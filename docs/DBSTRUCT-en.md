# SQLite database structure

File: `gtl.db` (Room, schema version **1**).  
Package: `com.lkovari.mobile.apps.gtl.data.db`.

Two tables. A session is one logging run. Each stored GPS fix is one row in `gps_events`. Deleting a session **cascade-deletes** its points.

```mermaid
erDiagram
    track_sessions ||--o{ gps_events : "sessionId"

    track_sessions {
        INTEGER id PK "autoincrement"
        INTEGER startedAt "epoch ms"
        INTEGER stoppedAt "epoch ms, null while logging"
        TEXT usageType "UsageType name"
        TEXT measurementSystem "METRIC IMPERIAL ICAO"
    }

    gps_events {
        INTEGER id PK "autoincrement"
        INTEGER sessionId FK "index, ON DELETE CASCADE"
        INTEGER timestamp "epoch ms"
        REAL latitude
        REAL longitude
        REAL altitude "GPS m, Location.altitude"
        REAL speed "m/s"
        REAL bearing "degrees"
        REAL accuracy "GPS accuracy m"
        INTEGER satellitesInFix
        REAL ambientTemperature "C, nullable"
        REAL accelX "nullable"
        REAL accelY "nullable"
        REAL accelZ "nullable"
        INTEGER isPlacemark "1 = START PAUSE STOP"
        TEXT eventKind "START MOVE PAUSE STOP"
    }
```

## `track_sessions`

| Column | Meaning |
|---|---|
| `id` | Primary key |
| `startedAt` | When logging started |
| `stoppedAt` | When Stop ran; `NULL` = open session |
| `usageType` | `AIRCRAFT`, `WATERCRAFT`, `FOUR_WHEELERS`, `TWO_WHEELERS`, `RUNNER` (settings at start) |
| `measurementSystem` | `METRIC`, `IMPERIAL`, `ICAO` |

Queries: list by `startedAt` descending; find the open session (`stoppedAt IS NULL`).

## `gps_events`

One row = one accepted fix (or the Stop placemark). Polyline, Route totals, Help table, and KMZ all read this table.

| Column | Unit / notes |
|---|---|
| `id` | Primary key |
| `sessionId` | Parent session |
| `timestamp` | Fix time |
| `latitude` / `longitude` / `altitude` | WGS84; altitude from fused GPS |
| `speed` | metres per second |
| `bearing` | heading degrees |
| `accuracy` | horizontal accuracy metres |
| `satellitesInFix` | GNSS snapshot at insert |
| `ambientTemperature` | °C if `TYPE_AMBIENT_TEMPERATURE` exists |
| `accelX` / `accelY` / `accelZ` | last accelerometer sample |
| `isPlacemark` | `true` for START / PAUSE / STOP (KMZ icons) |
| `eventKind` | `START`, `MOVE`, `PAUSE`, `STOP` |

`MOVE` rows are the dense track. START / PAUSE / STOP are also stored as points and marked as placemarks.

## Not stored

- Barometric pressure / baro altitude (planned: aircraft + ICAO).
- Raw GNSS constellation mix (HUD only, in memory).
- Map / OSM settings (DataStore, not SQLite).

## Access

`TrackRepository` is the only app-layer API. `RemoteTrackSync.uploadSession` runs on stop and is a no-op.
