# Rental Scam Checker

A Spring Boot API that scores a rental listing for scam risk **before a tenant pays anything**. Paste in the ad (plus whatever the "landlord" has asked for) and get back a 0–100 risk score, a risk band, every red flag with the evidence that triggered it, and concrete next steps.

Built around the scams that are common in the Irish rental market: the landlord who is "working abroad" and will post the keys, deposits "held by Airbnb", look-alike Daft links, Western Union / crypto payments and cloned listings.

## How it works

```
POST /api/v1/analyses
        │
        ▼
ListingContext  ── normalises text, email, phone; fingerprints the description
        │
        ▼
ScamRule × 10   ── each independent rule returns zero or more Signals (points + evidence + advice)
        │
        ▼
RiskScorer      ── score = 100 · (1 − Π(1 − pointsᵢ/100))
        │
        ▼
ListingAnalysis ── persisted (fingerprint + contacts, not the ad text) → JSON response
```

Signals combine as independent evidence rather than a plain sum: the score never exceeds 100, one critical flag dominates, and several weak flags add up without any single minor one tipping a listing into the red. Adding a rule is just adding a `@Component` that implements `ScamRule`; a rule that throws is logged and skipped instead of failing the request.

### Rules

| Rule | Flags | Max points |
|---|---|---|
| `PaymentRule` | Western Union, MoneyGram, crypto, gift cards (field or text); PayPal Friends & Family; international wire; money before viewing | 60 |
| `SuspiciousLinkRule` | Look-alike platform domains, e.g. `daft-ie-secure.com` (token-based, so `artbooks.com` ≠ `rtb`) | 55 |
| `ViewingRule` | Absent-landlord story, keys by post, refused viewing, fake "secure"/Airbnb escrow | 45 |
| `PriceAnomalyRule` | Rent below 75% / 55% of typical for area + size (configurable benchmarks) | 40 |
| `DuplicateListingRule` | Same ad text previously checked with different contact details | 40 |
| `CommunityReportRule` | Email/phone reported by other users (critical at 3+ reports) | 75 |
| `ContactDetailsRule` | Non-Irish phone, free webmail only, platform-impersonating email | 35 |
| `DepositRule` | Deposit above one month's rent (the usual Irish cap) | 30 |
| `LanguageRule` | Pressure tactics, ID documents requested up front, off-platform contact | 20 |
| `CompletenessRule` | Very thin ad or no location | 6 |

Risk bands: `LOW` < 20 ≤ `MEDIUM` < 45 ≤ `HIGH` < 70 ≤ `CRITICAL`.

## Running it

Requires Java 21 and Maven.

```bash
mvn spring-boot:run          # H2 in-memory DB on :8080
mvn test                     # unit + MockMvc integration tests
```

Swagger UI: http://localhost:8080/swagger-ui.html · Health: http://localhost:8080/actuator/health

With PostgreSQL via Docker:

```bash
docker compose up --build
```

## API

### Analyse a listing

```bash
curl -s -X POST localhost:8080/api/v1/analyses \
  -H 'Content-Type: application/json' -d @examples/scam-listing.json
```

Only `description` is required; every other field lets more rules run.

| Field | Type | Notes |
|---|---|---|
| `title`, `description` | string | description required |
| `monthlyRent`, `depositAmount` | number | EUR |
| `location` | string | free text, e.g. `"Rathmines, Dublin 6"` |
| `bedrooms` | int | 0 = studio |
| `contactEmail`, `contactPhone` | string | normalised (`087…` ≡ `+35387…`, Gmail dots ignored) |
| `listingUrl` | string | |
| `requestedPaymentMethods` | enum[] | `BANK_TRANSFER`, `INTERNATIONAL_WIRE`, `CARD`, `CASH`, `REVOLUT`, `PAYPAL_GOODS_AND_SERVICES`, `PAYPAL_FRIENDS_AND_FAMILY`, `WESTERN_UNION`, `MONEYGRAM`, `CRYPTO`, `GIFT_CARD`, `OTHER` |
| `viewingOffered`, `paymentRequestedBeforeViewing` | boolean | |

Response (abridged):

```json
{
  "id": "6f1c…",
  "score": 100,
  "riskLevel": "CRITICAL",
  "summary": "This matches well-known rental scam patterns. Do not pay, and consider reporting it.",
  "signals": [
    {
      "code": "PAYMENT_UNTRACEABLE_METHOD",
      "category": "PAYMENT",
      "severity": "CRITICAL",
      "points": 60,
      "title": "Asks for an untraceable payment method",
      "detail": "Payment via: western union",
      "advice": "Never pay rent or a deposit by money transfer service, crypto or gift cards — the money cannot be recovered."
    },
    {
      "code": "VIEWING_ABSENT_LANDLORD",
      "points": 45,
      "detail": "Found: \"i am currently working abroad\", \"keys will be sent to you\", \"oil rig\"",
      "…": "…"
    }
  ],
  "recommendations": ["…signal-specific advice first, then baseline checks…"],
  "disclaimer": "Automated, rule-based assessment. …"
}
```

`GET /api/v1/analyses/{id}` returns a stored analysis.

### Community reports

```bash
# Report a contact
curl -s -X POST localhost:8080/api/v1/contacts/reports -H 'Content-Type: application/json' \
  -d '{"type":"PHONE","value":"086 000 1234","reason":"Took deposit, flat did not exist"}'

# Check a contact
curl -s 'localhost:8080/api/v1/contacts/check?type=PHONE&value=%2B353860001234'
```

Reported contacts feed straight back into `CommunityReportRule` for later analyses.

## Configuration

`scam-checker.rent-benchmarks` in `application.yml` holds the typical 1-bed rents per location keyword and the bedroom multipliers. **The defaults are illustrative placeholders** — replace them with current figures (for example from the RTB Rent Index) before relying on the price rule. Keys are matched in order on word boundaries, so list specific areas (`dublin 4`) before broader ones (`dublin`).

| Env var | Default |
|---|---|
| `DB_URL` | `jdbc:h2:mem:scamchecker` |
| `DB_USER` / `DB_PASSWORD` | `sa` / empty |

## Design notes

- **Privacy:** the ad text is never stored, only a SHA-256 fingerprint of it (letters only, so re-formatting or price edits don't defeat clone detection), plus normalised contact details.
- **Explainability:** every signal carries the exact phrase or computation that triggered it, so users can judge it themselves.
- **Limitations:** phrase rules are English-only and evadable by rewording; the community blocklist has no abuse protection (rate limiting, auth, report verification would be needed before going public); price benchmarks are static config; a shared room entered as `bedrooms: 1` is compared with 1-bed apartment rents.

## Ideas for next steps

- Pull live median rents from the RTB Rent Index instead of static config.
- Perceptual hashing of listing photos to catch images reused across ads.
- An ML classifier trained on labelled listings, using these rule signals as features.
- Rate limiting and auth on the report endpoint.
