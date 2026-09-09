# GTL renewal — end of work report

Date: 4 September 2026  
Repo: `src/mobile/gtl` (Android Studio, Kotlin + Compose)  
Old source: `src/mobile/gtl-e` (Eclipse, not copied)

## Verification

- `./gradlew :engine:test :app:assembleDebug` — BUILD SUCCESSFUL
- Engine unit tests: 10 tests, all passing
- Debug APK produced under `app/build/outputs/apk/debug/`
- This is a native Android app; UI was not exercised on a device/emulator in this session. Open the `gtl` folder in Android Studio and run on a phone or emulator with GPS.

## What was rebuilt

- Application id `com.lkovari.mobile.apps.gtl`, versionCode **18**, versionName **2.0.0**
- minSdk **24**, targetSdk **36**, compileSdk **36**
- Modules `:engine` (JVM domain) and `:app` (Compose, Room, location, maps)
- Signing: gitignored `keystore.properties` copied from sensors-s (`ekl-release-key_v36500` / `ekldroidapps`)
- Room SQLite: `TrackSession` + `GpsEvent`; map polyline is drawn from Room
- Foreground location service (no `ACCESS_BACKGROUND_LOCATION`)
- GNSS HUD: GPS L1/L5, Galileo, GLONASS, BeiDou, QZSS, NavIC via `GnssStatus.Callback`
- Ambient temperature logged when `TYPE_AMBIENT_TEMPERATURE` exists
- Google Maps Compose + OSM Mapsforge download with progress bar
- Material 3 EN/HU UI; every secondary screen has a back arrow to main
- KML export via system share sheet (no remote icon URLs)
- `RemoteTrackSync` no-op for a later cloud backend (Room needs no Google registration)

## Privacy policy

- Repo: `docs/play-console/privacy-policy.html`
- KLHome copy: `angular/KLHome/KLHome/src/assets/bigfiles/gtl-privacy-policy.html`
- Live URL after you publish KLHome gh-pages:  
  https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html

## Deleted on purpose (Play-removal causes)

These were **not** ported:

| Old behavior | Why it went |
|---|---|
| `TelephonyManager.getDeviceId()`, IMEI file, `READ_PHONE_STATE` | Device identifiers / SIM-adjacent |
| `ins.php` live lat/lng upload | Covert location to eklsofttrade |
| `phoneids.txt` IMEI whitelist | Remote unlock of “extra” features |
| `gtlfind.php` follow-me WebView / email | Location sharing via our server |
| Google Directions POST | Sent start/end to Google |
| Apache HttpClient, Maps API v1, TabActivity | Dead APIs |
| Wi-Fi toggle, mock location, `DEVICE_POWER`, boot autostart | Illegal or leftover permissions |
| Missing `GTLShutdownReceiver` / `MinMaxTemperatureValue` classes | Broken in gtl-e; temperature range is reimplemented in engine |

## Your follow-ups

1. **Maps API key** — add `MAPS_API_KEY=...` to `local.properties`. Restrict to `com.lkovari.mobile.apps.gtl` and the EKL keystore SHA-1. Until then the Map tab shows an on-device message; OSM downloads still work.
2. **KLHome deploy** — commit/push the new HTML on the gh-pages/master workflow you already use for Numbers.
3. **Play Console** — still shows `eklsofttrade@gmail.com` and the old HTTP developer site. Update contact, privacy URL, and Data safety (precise location, no sharing) when you republish.
4. **OSM file URLs** — catalog uses `https://download.mapsforge.org/maps/v5/...`. If a region 404s, the download row shows failure; pick another region or update `OsmCatalog`.
5. **Hardware** — L5, NavIC, and ambient temperature depend on the chipset. Missing constellations show `0/0`, not an error.

## Later (not in the 2.0.0 rewrite session)

- **2.0.3** — Kalman on stored points, Settings sliders, usage presets, map-simplify 1–20 m. Notes: [CHANGELOGS.md](../CHANGELOGS.md), [CHANGELOG-2026-09-08.md](CHANGELOG-2026-09-08.md).
- **Unreleased after 2.0.3** — **Use GNSS only**, runner smoothing off, 0.5 m pedestrian duplicate floor, heading from consecutive positions when GPS bearing is 0. How the map line is the stored log: [README.md — How logging works](../README.md#how-logging-works).

## Not rebuilt (by plan)

- Turn-by-turn Google Directions
- Live web tracking / follow-me page
- Boot-time auto restart
- App-driven GPS/Wi-Fi toggles (opens system Location settings instead)
