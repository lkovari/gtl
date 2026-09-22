# Prompt: add Firebase Crashlytics to GTL

Implement Firebase Crashlytics in this repository. This slice reports uncaught crashes, ANRs, and every caught failure in application code. It does not change the database and it does not read SQLite to build a report.

The track already lives in Room (`gps_events`). Crashlytics receives a stack trace and short keys. It does not receive the track. There is no incident table, no copy of `gps_events`, and no query added for diagnostics.

## Hard limits

- Do not add a table, column, entity, DAO, migration, or schema version. Do not edit `Entities.kt`, `Daos.kt`, `GtlDatabase.kt`, `TrackRepository.kt`, or `docs/DBSTRUCT-en.md`.
- Do not query Room or SQLite from the crash reporter. A session id is taken only from a variable the call site already holds in memory.
- Do not write latitude, longitude, altitude, speed, bearing, accuracy, temperature, pressure, satellite counts, accelerometer samples, a file path that embeds coordinates, or a `GpsEventEntity` into a Crashlytics key, log, or a new exception message. Pass the original throwable through. Do not wrap it in a message you build from a fix.
- Do not call `setUserId`. There is no account.
- Do not add `firebase-analytics` or `firebase-crashlytics-ndk`.
- Do not install a custom `UncaughtExceptionHandler` and do not send the report over the network from a crash handler. The SDK writes a file and uploads it on the next cold start.
- Do not instrument tests (`app/src/test`, `app/src/androidTest`, `engine/src/test`). Those catches are part of the test, not production failures.
- Do not add source comments.
- `google-services.json` is already listed in `.gitignore`. Leave it ignored. Do not commit it.

## What the developer should see

Uncaught exceptions and ANRs are recorded by the SDK with no call site.

Every other caught `Throwable` in `app/src/main` is recorded through `recordException`. `CancellationException` is not a failure: rethrow it and do not record it. That matters in `OsmDownloadWorker` and `TuhuDownloadWorker`, where `catch (Exception)` currently swallows cancellation. Rethrow first, then record any other exception, then keep the existing cleanup and `Result.failure()`.

`Result.failure()` branches that have no throwable stay as they are. Do not invent an exception for them.

Each non-fatal report carries an `op` key set immediately before `recordException`. Set `session_id` only when that call site already has a session id in memory. Omit the key otherwise. Do not send `0` as a stand-in.

| Key | Value |
|---|---|
| `op` | stable name from the table below |
| `session_id` | open session id already in memory (`Long`), only for the two recording inserts |

App version, device model, and OS version are attached by the SDK. Do not duplicate them as custom keys.

After `insert_event`, keep today’s behaviour: `loggingError = true` and `stopRecording()`. That already stops further inserts, so one recording emits one `insert_event` report. After `insert_stop`, still call `stopSession` so the session does not stay open. Do not drop any other `op` with a once-per-session guard.

Bind every caught exception to a name. Do not discard it as `_`.

`GtlApplication.onCreate` stays free of Crashlytics keys and location. The Google Services content provider installs the crash handler before `onCreate`.

## Where to call it

| File | Catch | `op` | `session_id` |
|---|---|---|---|
| `service/TrackingForegroundService.kt` | `SQLException` around `insertEvent` while recording (sets `loggingError`) | `insert_event` | that insert’s `sessionId` |
| `service/TrackingForegroundService.kt` | empty `SQLException` around the STOP `insertEvent` inside `stopRecording` | `insert_stop` | the `sessionId` closed there |
| `data/location/LocationClient.kt` | `SecurityException` on `requestLocationUpdates` in `gpsProviderLocations` | `gps_request_updates` | omit |
| `data/location/LocationClient.kt` | `SecurityException` on `removeUpdates` in `gpsProviderLocations` | `gps_remove_updates` | omit |
| `data/location/LocationClient.kt` | `SecurityException` on the GPS `requestLocationUpdates` in `fusedLocations` | `fused_gps_request_updates` | omit |
| `data/location/LocationClient.kt` | `SecurityException` on fused `requestLocationUpdates` | `fused_request_updates` | omit |
| `data/location/LocationClient.kt` | `SecurityException` on fused `removeLocationUpdates` | `fused_remove_updates` | omit |
| `data/location/LocationClient.kt` | `SecurityException` on `removeUpdates` of the GNSS listener in `fusedLocations` | `fused_gps_remove_updates` | omit |
| `data/gnss/GnssStatusSource.kt` | `SecurityException` on `registerGnssStatusCallback` | `gnss_register` | omit |
| `data/gnss/GnssStatusSource.kt` | `SecurityException` on `unregisterGnssStatusCallback` | `gnss_unregister` | omit |
| `ui/screens/MapPane.kt` | `IllegalStateException` in `animateToTrackBounds` | `map_fit_bounds` | omit |
| `ui/screens/MapPane.kt` | `Throwable` in the OSM `AndroidView` factory (`attachOsmLayers`) | `osm_attach` | omit |
| `ui/screens/MapPane.kt` | `Throwable` in OSM `onRelease` (`destroyAll`) | `osm_destroy` | omit |
| `ui/screens/MapPane.kt` | `Throwable` in `applyOsmXmlTheme` | `osm_theme` | omit |
| `ui/screens/MapPane.kt` | `RuntimeException` in `fitOsmToBounds` | `osm_fit_bounds` | omit |
| `data/maps/OsmRenderTheme.kt` | `Throwable` in `create` | `osm_theme_create` | omit |
| `tuhu/TuhuRenderTheme.kt` | `Throwable` in `create` | `tuhu_theme_create` | omit |
| `data/maps/OsmDownloadWorker.kt` | `Exception` in `doWork`, after rethrowing `CancellationException` | `osm_download` | omit |
| `tuhu/TuhuDownloadWorker.kt` | `Exception` in `doWork`, after rethrowing `CancellationException` | `tuhu_download` | omit |

Keep each catch’s existing recovery (close the flow, fall back to the default theme, delete the partial download, post `onOsmFailed`, zoom to the bounds center, stop recording, close the session). Recording is added. Recovery is not removed.

If a new `catch` appears in `app/src/main` while you implement this, record it too, with a new stable `op`. Do not leave a production catch that discards the throwable.

## Build

Catalog: `gradle/libs.versions.toml`. Root plugins: `build.gradle.kts`. App module: `app/build.gradle.kts`. AGP is 9.2.1, Kotlin is 2.2.10. Pin the current stable `firebase-bom`, `com.google.gms.google-services`, and `com.google.firebase.crashlytics` versions that support this AGP. Do not use a floating version. Do not guess a version from memory; read the current Firebase Android release notes when implementing.

Dependencies, only these:

- `implementation(platform(...firebase-bom...))`
- `implementation(...firebase-crashlytics...)`

Apply `com.google.gms.google-services` and `com.google.firebase.crashlytics` in the app module only when `app/google-services.json` exists. A checkout without that file must still compile and assemble. `BuildConfig` is already enabled. Add a boolean `CRASHLYTICS_ENABLED` that is true only for the release build type when the json file exists, and false otherwise.

Put one type in the app module, `CrashReport`, with `record(op: String, sessionId: Long?, error: Throwable)`. When `CRASHLYTICS_ENABLED` is false, return before any `FirebaseCrashlytics` call. When it is true, set `op`. Set `session_id` only when `sessionId` is non-null. Then call `recordException(error)`.

Debug collection stays off so local crashes do not reach the console. Add `app/src/debug/AndroidManifest.xml` (manifest merger) with:

```xml
<meta-data
    android:name="firebase_crashlytics_collection_enabled"
    android:value="false" />
```

Release keeps the SDK default, which sends reports. The Crashlytics Gradle plugin uploads the R8 `mapping.txt` as part of the release bundle when the plugin is applied. `isMinifyEnabled` is already true. Do not turn minify off. Leave `ndk.debugSymbolLevel = SYMBOL_TABLE` as it is; that line is for Play, not for this SDK.

The human creates the Firebase project and downloads `google-services.json` for `applicationId` `com.lkovari.mobile.apps.gtl` into `app/google-services.json`. Code does not embed that file.

## Privacy policy

Update `docs/play-console/privacy-policy.html` in this repo before any Play release that contains the SDK. The in-app Help screen only opens `https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html`. Do not add a second policy screen or change `strings.xml` for this. Publishing the live KLHome page is a manual step for the publisher; the local HTML and the live page must match before rollout. Set both “Last updated” lines to the day the policy text changes.

The page is bilingual (`data-lang="en"` and `data-lang="hu"`). Change both languages in the same file.

### Summary

Replace the English paragraph with:

> GTL is a GPS track logger. It records your route, satellite status, and optional ambient temperature on this phone. There is no account. We do not operate a tracking server. The app uses the internet for Google Maps tiles (if you set an API key), for OpenStreetMap region file downloads that you start, and to send a crash report to Google Firebase Crashlytics the next time you open the app after a crash or a caught error.

Replace the Hungarian paragraph with:

> A GTL GPS útvonalnapló. Az útvonalat, a műholdállapotot és — ha van szenzor — a környezeti hőmérsékletet ezen a telefonon rögzíti. Nincs fiók. Nem üzemeltetünk követőszervert. Internetre a Google Térkép csempéihez (ha beállítasz API-kulcsot), az általad indított OpenStreetMap-letöltésekhez, és ahhoz van szükség, hogy egy összeomlás vagy egy elkapott hiba után a következő megnyitáskor hibajelentés menjen a Google Firebase Crashlyticsnek.

### Data on the device

Append one sentence to the English paragraph:

> A crash report may remain in app-private storage until the next time you open the app, and is then sent as described under Crash reports.

Append one sentence to the Hungarian paragraph:

> A hibajelentés az app saját tárhelyén maradhat a következő megnyitásig, és utána a Hibajelentések szakasz szerint megy el.

### New section, after Google Maps and before OpenStreetMap downloads

English:

> ## Crash reports
>
> If the app crashes, stops responding, or catches an error, GTL stores a crash report on the device and sends it to Google Firebase Crashlytics the next time you open the app. The report contains the stack trace, app version, device model, Android version, an operation name for the code path, and, when a recording is open, a numeric session id. It does not contain latitude, longitude, altitude, speed, temperature, pressure, satellite details, or the track. Google processes the report, including the IP address used at upload time. Google’s policy: https://policies.google.com/privacy

Hungarian:

> ## Hibajelentések
>
> Ha az alkalmazás összeomlik, nem válaszol, vagy hibát kap el, a GTL hibajelentést tárol a készüléken, és a következő megnyitáskor elküldi a Google Firebase Crashlyticsnek. A jelentésben stack trace, appverzió, készülékmodell, Android-verzió, a kódútvonal műveletneve, és nyitott rögzítéskor egy numerikus menetazonosító van. Nincs benne szélesség, hosszúság, magasság, sebesség, hőmérséklet, nyomás, műholdadat vagy az útvonal. A Google feldolgozza a jelentést, a feltöltéskori IP-címmel együtt. A Google tájékoztatója: https://policies.google.com/privacy

Keep the existing Google Maps sentences that say the track database is not sent to Google. Crash reports are this new section, not the map tiles.

### What we do not do

Replace the English bullet `No ads or third-party analytics SDKs` with:

> No ads, no product-analytics SDK, and no advertising identifier. Firebase Crashlytics is used only for the crash reports described above.

Replace the Hungarian bullet `Nincs reklám és külső analitikai SDK` with:

> Nincs reklám, nincs termékanalitikai SDK, és nincs hirdetésazonosító. A Firebase Crashlytics csak a fent leírt hibajelentésre szolgál.

Leave the other bullets (no account, no IMEI, no live-location upload, no sale of personal data).

## Play Console Data safety

This is a manual form in Play Console, not a code change. Fill it before the release that contains the SDK. There is no in-app opt-out in this slice, so collection is required for everyone who installs that version.

Follow the current Firebase page “Prepare for Google Play’s Data safety section” at implementation time. The minimum that must match this code:

- Crash logs: collected. Purpose is the one Firebase lists for Crashlytics (diagnostics, not advertising).
- Device or other IDs, only if that page still lists the Crashlytics installation UUID for this SDK version.
- Location, precise location, and track contents: not collected by this feature.
- Data is processed by Google as the Crashlytics service provider. It is not sold and not used for ads.

Play Android vitals stays as it is. Crashlytics does not replace it.

## Check

- A debug build does not create a Crashlytics issue for a thrown exception or for `recordException`.
- A release build with `google-services.json` present, after a process restart, shows a non-fatal issue for a caught failure. The stack names the class that caught it, `op` is one of the names above, and the report text has no latitude or longitude. `session_id` is present for `insert_event` and `insert_stop`, and absent for the other ops.
- The same release build shows a fatal issue for an uncaught test exception only after the next cold start.
- Cancelling an OSM or Túristautak download does not create a Crashlytics issue and does not become `Result.failure()` from a swallowed `CancellationException`.
- A failed insert still leaves the points already stored in `gps_events` on the device. The Tracks inspect screen still opens that session.
- `./gradlew :app:assembleDebug` succeeds when `app/google-services.json` is absent.
- Room schema version is unchanged.
