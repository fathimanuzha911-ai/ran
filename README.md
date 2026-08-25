# MNPOS Distribution (lean rebuild)

A dedicated Android app for the **distribution module only** — Branches,
Sales Reps, Customers, Routes, Stock Requests, Sales Orders, Sales,
Collections, Returns, Stock Transfer, Expenses, Daily Settlement, and the
two report screens (Order Summary / Daily Products). No POS, purchasing,
printer, or product-management screens — those were dropped since you said
you only need distribution.

It talks to the **same backend** your current app uses
(`/api/mobile/...`) — no server changes required. `applicationId` is
`com.mnpos.distribution` so it can be installed side-by-side with the
original app while you evaluate it.

## Why this should feel faster with 20 concurrent users

| Original app | This app |
|---|---|
| New `HttpURLConnection` + new `Thread` per request | One shared, connection-pooled `OkHttpClient` (keep-alive, async dispatcher) |
| Every list built by hand-nesting `LinearLayout`s in Java, all rows held in memory inside a `ScrollView` | Real `RecyclerView` everywhere a list appears — rows are recycled, pagination loads 30 at a time on scroll |
| One 1,053-line `DistributionActivity` switching on strings for ~15 screens | One generic `RecordListActivity` + `RecordAdapter`, configured per screen by a `RecordSpec` in `Catalog.java` |
| Every menu item shown to every user | Menu is filtered by role tier (`Session.roleTier()`) computed from the same permission strings your backend already returns at login |

## Role handling (12 reps / 6 managers / 2 admins)

`Session.roleTier()` classifies the logged-in user as `SALES_REP`,
`MANAGER`, or `ADMIN` from `role_name` and the permissions array returned
by `/api/mobile/login` (same contract as the original app). `Catalog.MENU`
then filters what each tier sees:

- **Sales reps** see: Customers, Sales Orders, Sales, Collections, Returns,
  Stock Requests.
- **Managers** additionally see: Branches, Sales Reps, Routes, Stock
  Transfer, Expenses, Daily Settlement, Order Summary, Daily Products.
- **Admins** see everything managers see. I didn't add an admin-only screen
  (e.g. user management) since it wasn't in the requested scope — see
  "Extending" below for how to add one.

If your backend scopes data by location/rep automatically based on the auth
token (which the original app's code suggests it does), you may not need to
change anything server-side. If reps should be *hard-blocked* from ever
seeing another rep's data (not just hidden in the UI), that check needs to
live server-side too — a client-side menu filter is a UX convenience, not a
security boundary.

## What you should verify before rollout

1. **Create-form field names.** I don't have your backend source, so the
   fields posted by Collections / Returns / Expenses / Daily Settlement
   (`location_id`, `sales_rep_id`, `amount`, `payment_method`, `reason`,
   `note`, `date`, `cash_amount`) are inferred from naming patterns in your
   existing Java code. Test each form against your actual API and adjust
   the `FieldSpec[]` in `Catalog.java` if a field name doesn't match.
2. **List field names.** `RecordSpec` uses fallback key chains (e.g. a row's
   title tries `name`, then `customer_name`, then `invoice_no`...) so it
   degrades gracefully if a field is missing, but you should open each
   screen once and confirm the right fields are showing.
3. **Pagination.** The list screens assume `?page=` is supported and that
   fewer than 30 rows returned means "last page." Confirm that matches your
   API — if it doesn't paginate server-side, everything still works, it'll
   just fetch everything on page 1 and stop.
4. **Sales Orders / Stock Requests creation.** These need multi-line
   product entry (like Stock Transfer), which doesn't fit the generic
   single-field form, so they're currently **view-only** in this build. Use
   `StockTransferActivity.java` as the template if you want to add order
   creation — it already has the search-and-add-line-item pattern.

## Extending

- **New read-only screen:** add one `RecordSpec` entry to `Catalog.RECORDS`
  and one `MenuItem` to `Catalog.MENU`. Nothing else needed.
- **New create form:** add a `FieldSpec[]` to an existing `RecordSpec`.
- **Admin-only screen:** add a `MenuItem` with `Session.TIER_ADMIN`.

## Building

Open in Android Studio (Ladybug or newer) or run:

```
./gradlew assembleDebug
```

`minSdk 23`, `compileSdk 35`, matching your original app's range.
