# Eventra

A full-stack movie, event and live-experience ticketing platform: city-aware discovery, real-time seat maps with
server-side seat locking, food & beverages, coupons, multi-gateway payments (Stripe, Razorpay, PhonePe, UPI apps,
pay-at-venue, and a mock gateway for development), QR tickets with gate validation, cancellations and refunds,
notifications, and an admin/partner console with a visual seat-layout designer and analytics.

All titles, cinemas, venues, people and artwork in this repository are original and fictional.

---

## Architecture

```
Browser (Next.js 16 · React 19 · TypeScript · Tailwind v4 · TanStack Query · Framer Motion · R3F)
   │  REST/JSON  (Bearer access token in memory · refresh token in HttpOnly cookie)
   ▼
Spring Boot 3.5 (Java 21)
   Spring Security (JWT, RBAC, rate limiting, secure headers)
   Controller ─► Service ─► @Transactional ─► Repository (Spring Data JPA / Hibernate)
   Scheduled jobs: seat-lock cleanup · booking expiry · refunds · reminders
   PaymentProvider ─► Mock | Stripe | Razorpay | PhonePe  ◄── signed, idempotent webhooks
   ▼
PostgreSQL 16 (Flyway migrations · row locks · exclusion constraint for show overlaps)
```

The backend is the only source of truth for seat availability, prices, discounts, payment success, booking and
refund status, and authorization. The frontend never sends a price; it sends seat labels, food item ids and a
coupon code, and the server recomputes everything.

Payment flow: `Next.js → Spring Boot (create order) → gateway → webhook → Spring Boot (verify signature, dedupe event id) → DB → CONFIRMED booking + QR ticket`.

More detail in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Repository layout

```
eventra/
├── backend/                 Spring Boot API (Maven)
│   └── src/main/java/com/eventra/
│       ├── config/          security, CORS, cache, OpenAPI, scheduling, demo-data seeder
│       ├── security/        JWT service/filter, rate limiting, request-id logging
│       ├── controller/      public REST controllers (thin)
│       ├── service/         catalog, auth, pricing, coupons, search, reviews, idempotency, audit
│       ├── seat/            SeatLockService (SELECT … FOR UPDATE) + cleanup job
│       ├── booking/         booking lifecycle, event inventory, cancellation, tickets/QR, expiry job
│       ├── payment/         PaymentProvider interface, router, PaymentService, RefundService, providers/
│       ├── notification/    in-app notifications, EmailService (console / SMTP)
│       ├── admin/           admin & partner APIs, seat configurator, analytics
│       ├── entity/ repository/ dto/ mapper/ exception/ common/
│   └── src/main/resources/db/migration   V1…V9 schema + reference data
│   └── src/main/resources/db/demo        V100 demo catalog (dev profile only)
├── frontend/                Next.js App Router app
│   ├── app/                 routes (customer, booking funnel, admin, staff)
│   ├── components/          ui/, layout/, movies/, booking/, events/, admin/
│   ├── features/            auth, location, query providers, booking helpers
│   ├── services/            the single API layer (authApi, movieApi, showApi, seatApi, bookingApi, paymentApi, …)
│   ├── lib/ types/ schemas/ hooks/
│   ├── tests/ (Vitest)  e2e/ (Playwright)
│   └── scripts/generate-media.mjs   draws the original demo posters/backdrops/food art
├── database/init/           creates the test database for docker compose
├── docs/                    architecture & API notes
├── postman/                 Eventra.postman_collection.json
├── docker-compose.yml
└── .env.example
```

## Quick start (local)

Prerequisites: Java 21, Maven 3.9+, Node 22+, Docker (or a local PostgreSQL 16).

```bash
cp .env.example .env                       # adjust if needed; defaults work for local dev
docker compose up -d postgres              # PostgreSQL on :5432 (also creates eventra_test)

# backend (dev profile: demo data + mock payments)
cd backend
mvn spring-boot:run                        # http://localhost:8080 · Swagger: http://localhost:8080/swagger-ui

# frontend
cd ../frontend
npm install
npm run dev                                # http://localhost:3000
```

Or run everything in containers: `docker compose --profile app up --build`.

On first start Flyway creates the schema, the dev profile loads the demo catalog (10 movies, 13 cinemas in 9 cities,
29 screens, 12 events/sports/plays/activities), creates the accounts below, schedules a rolling 7-day window of
shows, and places a few real sample bookings through the booking + mock-payment pipeline so seat maps and analytics
aren't empty.

| Role | Email | Password |
|------|-------|----------|
| Customer | demo@eventra.com | `DEMO_USER_PASSWORD` (default `Demo@12345`) |
| Admin | admin@eventra.com | same |
| Venue staff | staff@eventra.com | same |
| Partner (owns the Purple Square cinemas) | partner@eventra.com | same |

These accounts exist only with the `dev` profile. Never enable it in production.

## Environment variables

See [`.env.example`](.env.example) for the full list. The important ones:

| Variable | Purpose |
|---|---|
| `NEXT_PUBLIC_API_URL` | Backend base URL used by the browser |
| `NEXT_PUBLIC_APP_NAME` | Display name |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` (≥ 32 bytes), `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION` | Token signing and lifetimes (ISO-8601 durations) |
| `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe (cards) |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | Razorpay (UPI incl. Google Pay/PhonePe apps, cards, net banking, wallets) |
| `PHONEPE_CLIENT_ID`, `PHONEPE_CLIENT_SECRET`, `PHONEPE_CLIENT_VERSION`, `PHONEPE_ENVIRONMENT` | PhonePe PG |
| `EMAIL_PROVIDER` (`console`/`smtp`), `EMAIL_HOST`, `EMAIL_PORT`, `EMAIL_USERNAME`, `EMAIL_PASSWORD` | Email |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed browser origins |
| `SEAT_LOCK_DURATION`, `CANCELLATION_CUTOFF_MINUTES`, `CONVENIENCE_FEE_PER_TICKET`, `TAX_RATE_ON_FEE` | Business policy |

Secrets are read only by the backend. Nothing secret is ever sent to the browser.

## Database

* Migrations: `backend/src/main/resources/db/migration` (`V1__initial_schema.sql` … `V9__reference_data.sql`).
  Demo data lives separately in `db/demo` and is only on Flyway's path in the `dev` profile.
* Integrity rules are enforced in the database itself as well as in code. Examples: the `ex_shows_no_overlap`
  exclusion constraint (no overlapping shows on one screen), `CHECK (sold + held <= capacity)` on event inventory,
  a unique `(provider, event_id)` on webhook events, and one refund per captured payment.
* Never change a production schema by hand. Add a new `V{n}__*.sql` migration.

## Payments

`PaymentProvider` is the gateway abstraction. `PaymentRouter` maps each customer-facing method to the first
configured gateway that supports it:

| Method | Preferred gateway order |
|---|---|
| Card (Visa/Mastercard/RuPay/Amex where supported) | Stripe → Razorpay → PhonePe → Mock |
| UPI | Razorpay → PhonePe → Mock |
| Google Pay | Razorpay (UPI) → Mock. There is no standalone "Google Pay API"; GPay pays through UPI. |
| PhonePe | PhonePe PG → Razorpay (UPI) → Mock |
| Net banking / Wallets | Razorpay → (PhonePe) → Mock |
| Pay at venue | internal: seats held until a configurable cut-off, staff mark it PAID or CANCELLED |

* **Stripe**: the backend creates a PaymentIntent. The browser gets only the `client_secret` and publishable key
  and confirms with Stripe.js Payment Element. The final state comes from `/api/v1/payments/webhook/stripe`,
  which verifies the `Stripe-Signature` header (HMAC-SHA256 of `timestamp.payload`, 5-minute tolerance).
* **Razorpay**: the backend creates an Order and the browser opens Checkout. The handler response is verified
  server-side (`HMAC_SHA256(order_id|payment_id, key_secret)`), and `/webhook/razorpay` verifies
  `X-Razorpay-Signature`.
* **PhonePe**: Standard Checkout v2 with OAuth client credentials. Base URLs switch on `PHONEPE_ENVIRONMENT`.
  **Before going live, re-check the endpoints against PhonePe's current merchant documentation.**
* **Mock (development)**: enabled by the `dev` profile (`PAYMENTS_MOCK_ENABLED`). Checkout sends you to
  `/dev/payment-simulator`, where you choose SUCCESS / FAILED / CANCELLED. The result is delivered as a signed
  webhook through the same idempotent pipeline, and you can deliver it twice to see duplicate protection.

Card numbers and CVVs never touch Eventra. Only gateway references (order, payment and refund ids) are stored.

Webhook URLs to register with each gateway: `https://<api-host>/api/v1/payments/webhook/{stripe|razorpay|phonepe}`.

## Core guarantees and where they live

| Guarantee | Implementation |
|---|---|
| Two customers can't hold or buy the same seat | `SeatLockService.lockSeats`: `SELECT … FOR UPDATE` on the requested `show_seats` rows in id order, inside one transaction; 409 `SEAT_LOCKED` / `SEAT_ALREADY_BOOKED` |
| Holds expire | `SeatLockCleanupJob` (every 5 s) marks locks `EXPIRED` (rows kept for audit) and frees seats. Lapsed holds are also treated as free immediately at read/lock time. |
| Server-side pricing | `PricingService`: tickets and food from DB snapshots, configurable fee and GST, coupon discount capped at the order value |
| Price snapshot | `bookings` keeps ticket/food/fee/tax/discount/total; `booking_seats`, `food_order_items` and `booking_items` keep unit prices |
| No duplicate bookings/payments | `Idempotency-Key` on booking creation, payment initiation and cancellation (`IdempotencyService`); webhook event-id dedupe; payment row locks |
| Late payment after the seats were resold | the booking isn't confirmed and a full refund is created automatically |
| Uncertain payment status | `/payment/[id]` shows "Payment status is being verified" and the backend queries the gateway; nothing is marked failed on a missing callback |
| Refunds | `RefundService`: REQUESTED → PROCESSING → COMPLETED/FAILED (with retries). COMPLETED only when the gateway confirms. |
| QR tickets | `CV1.<ticketCode>.<HMAC>`: no payment data; `/tickets/validate` admits once (row-locked) |
| Authorization | URL rules plus `AdminAccess` (partners are scoped to their own cinemas); frontend guards are UX only |

## API

* Swagger UI: `http://localhost:8080/swagger-ui`, OpenAPI JSON: `/v3/api-docs`
* Postman: import `postman/Eventra.postman_collection.json`, then run the folders top to bottom.
* Response envelope: `{ "success": true, "data": …, "message": "Success" }` or
  `{ "success": false, "error": { "code": "SEAT_ALREADY_BOOKED", "message": "…", "fields": { … } } }`.
* Paging: `?page=0&size=20` returns `{ content, page, size, totalElements, totalPages }`.

## Testing

```bash
# backend: needs PostgreSQL with a eventra_test database (docker compose creates it)
cd backend && mvn test

# frontend unit tests
cd frontend && npm test

# end-to-end (backend + frontend running in dev mode)
cd frontend && npx playwright test
```

Backend integration tests (real PostgreSQL, clean-migrated per run):

| Test | Covers |
|---|---|
| `SeatConcurrencyTest` | §97: two users hit seat A1 at the same instant, and 12 users race for 3 seats. Exactly one wins; the others get 409. |
| `PaymentIdempotencyTest` | §98: a duplicate webhook yields one booking and one ticket; forged signatures are rejected; a failed payment can be retried; a late payment after resale is refunded |
| `SeatLockExpiryTest` | §99: after 5 minutes another customer can lock the seat, with or without the cleanup job having run |
| `CancellationTest` | §100: cancellation before the deadline releases seats and completes the refund; after the deadline it's rejected |
| `BookingFlowE2ETest` | §96 over HTTP: login → showtimes → seat map → lock → booking + food → coupon → mock payment (webhook ×2) → CONFIRMED + QR → My Bookings → notifications → gate scan once |
| `AdminFlowTest` | §101: admin creates movie → cinema → screen → seat layout → show, and a customer finds and locks it; overlap rejected; RBAC 403s |
| `AuthTest` | register/login/refresh rotation, reuse detection, logout, validation errors |
| `PricingServiceTest` | pure unit tests of the price engine |

## Deployment notes

* Run the backend with `SPRING_PROFILES_ACTIVE=prod`. This turns off demo data and the mock gateway and switches
  to structured JSON logs. Provide real `JWT_SECRET`, `QR_SIGNING_SECRET` and gateway keys, set
  `COOKIE_SECURE=true`, and serve both apps over HTTPS on the same site (for example `app.example.com` and
  `api.example.com`) so the refresh cookie works.
* Scale-out: seat locks are in PostgreSQL, so any number of API instances stay consistent. The in-memory rate
  limiter and caches are per instance; move them to Redis when you run more than one node (a `redis` service is
  already in `docker-compose.yml`).
* Scheduled jobs are idempotent and lock rows individually, so running them on several nodes is safe. You can
  still pin them to one node with `eventra.scheduling.enabled`.
