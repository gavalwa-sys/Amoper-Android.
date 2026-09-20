# AMOPER Android App — Setup Guide

This delivers two things:

1. **`AMOPER-API/`** — a JSON REST API layer added to your existing AMOPER PHP
   project (auth, marketplace, logistics, driver, notifications). Your
   uploaded zip didn't contain a working `api/` or `modules/` implementation,
   so this was built from scratch against your real database schema.
2. **`AmoperAndroid/`** — a native Kotlin + Jetpack Compose Android app that
   talks to that API: customer marketplace (browse, cart, checkout, orders)
   and logistics (get a quote, book a shipment, track it), plus a driver mode
   (view assigned deliveries, update status, share GPS location).

I could not compile/run either of these in this environment (no PHP or
Android SDK available here), so test both locally before relying on them.

---

## 1. Install the API on your PHP backend

1. Copy `AMOPER-API/api/` into the root of your AMOPER project (next to the
   existing `api.php`), and `AMOPER-API/database/migrations/013_api_tokens.sql`
   into your `database/migrations/` folder.
2. Run that migration against your database:
   ```
   mysql -u youruser -p yourdatabase < database/migrations/013_api_tokens.sql
   ```
3. (Optional, for clean `/api/...` URLs) merge the extra rewrite line from
   `AMOPER-API/htaccess-addition.txt` into your real `.htaccess`
   (a pre-merged copy is in `.htaccess.patched` for reference). This isn't
   required — the Android app talks to `api.php?r=...` directly, which
   already works with your existing `.htaccess`.
4. Sanity check from a browser or curl:
   ```
   curl "https://your-site/api.php?r=ping"
   # {"ok":true,"data":{"message":"AMOPER API is up",...}}
   ```
5. Try registering a test user:
   ```
   curl -X POST "https://your-site/api.php?r=auth/register" \
     -H "Content-Type: application/json" \
     -d '{"name":"Test User","email":"test@example.com","password":"secret123","role":"customer"}'
   ```
   You should get back `{"ok":true,"data":{"token":"...","user":{...}}}`.

**What this API covers:** register/login/logout, browsing categories &
products, cart, checkout → orders, logistics quotes → booked shipments →
tracking with event history, a driver's assigned-deliveries list with status
updates and GPS pings, and notifications. It reuses your real tables
(`users`, `products`, `orders`, `shipments`, `drivers`, etc.) — nothing was
invented. Things intentionally left out for a first pass: payment gateway
integration (orders are created `payment_status = unpaid` for your existing
payment flow to pick up), corporate/customs/warehouse modules, and
password-reset/OTP endpoints. All of the underlying tables already exist if
you want to extend the API later — `api/bootstrap.php`, `api/auth.php`,
`api/marketplace.php`, `api/logistics.php` are small, readable files to add
routes to.

---

## 2. Run the Android app

1. Install **Android Studio** (Koala/2024.1 or newer).
2. Open the `AmoperAndroid/` folder as a project (File → Open).
3. Android Studio will offer to add/repair the Gradle wrapper for this
   project on first open (it wasn't hand-generated here) — accept that, or
   if you have Gradle installed locally run `gradle wrapper --gradle-version 8.7`
   inside `AmoperAndroid/` first.
4. Open `app/build.gradle.kts` and set `API_BASE_URL` to your server:
   - Android **emulator** talking to a XAMPP/local server on the same
     machine: `http://10.0.2.2/YOUR-AMOPER-FOLDER/` (10.0.2.2 is the
     emulator's alias for your computer's localhost).
   - A **real device** on the same Wi-Fi as your dev machine: use your
     machine's LAN IP, e.g. `http://192.168.1.20/YOUR-AMOPER-FOLDER/`.
   - A **deployed** AMOPER site: its real `https://` URL.
   - This must end with a trailing slash.
5. Click Run. Pick an emulator or a plugged-in device.
6. Register a new account from the app (or reuse the curl test user above)
   and you're in.

### Project layout
- `data/model` — API response models
- `data/network` — Retrofit service, auth-token interceptor, DataStore token storage
- `data/repository` — one repository per domain (auth, marketplace, logistics, driver)
- `ui/auth`, `ui/marketplace`, `ui/orders`, `ui/logistics`, `ui/driver`, `ui/profile` —
  screens + view models, one folder per feature
- `ui/nav/AmoperNavGraph.kt` — all navigation, including the bottom nav bar
  (a "Driver" tab only appears for accounts registered with the driver role)

### Known limitations / good next steps
- Driver GPS sharing uses a simple in-app 20-second polling loop, not a
  foreground service — it stops if the app is backgrounded or killed. For
  production-grade background tracking, promote it to a proper foreground
  `Service` with a persistent notification.
- No push notifications (FCM) yet — the notifications screen/endpoint is
  pull-based only.
- No payment step in checkout — orders land as `unpaid`, matching how the
  rest of the AMOPER platform already expects to handle payment.
- Passwords are validated client-side lightly; add stronger checks if you're
  going to production.
- I haven't been able to build/run either half in this environment, so
  treat this as a strong first pass that needs a real compile-and-test pass
  on your machine, not a guaranteed zero-bug drop-in.
