# gtl

GTL GPS Track Logger — store route points in SQLite and export KML with driving data.

Kotlin + Jetpack Compose rewrite of the 2014 Eclipse app. Location stays on the device.

Privacy policy: https://lkovari.github.io/KLHome/assets/bigfiles/gtl-privacy-policy.html

Local copy: `docs/play-console/privacy-policy.html`

## Setup

1. Open this folder in Android Studio.
2. Copy `keystore.properties` from sensors-s (gitignored). Same EKL release keystore.
3. Add a Maps SDK key to `local.properties`:

```
MAPS_API_KEY=your_key_here
```

Restrict the key to `com.lkovari.mobile.apps.gtl` and the EKL keystore SHA-1.

## SDK

- minSdk 24
- targetSdk 36
- versionCode 18 / versionName 2.0.0
