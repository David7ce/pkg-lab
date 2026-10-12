# FuelLog

Minimal personal fuel tracker for Android: refuellings, prices, consumption and expenses. Local only.

## Structure

```
app/src/main/java/com/deivon/fuellog/
├── data/    Models.kt (Vehicle, Refuel) · FuelDatabase.kt (SQLiteOpenHelper)
├── logic/   Decimals.kt · FuelStats.kt · Period.kt · RefuelForm.kt (validation) · Backup.kt (JSON/CSV)
└── ui/      MainActivity (3 tabs) · Overview · History · RefuelEditor · VehicleScreen · AppViewModel · Theme · Formats
app/src/test/…/logic/   Unit tests for everything in logic/
```

Dependency direction: `ui → logic → data`. `logic/` has no Android UI code and is tested on the JVM.

## Dependencies

| Dependency | Why |
|---|---|
| Compose BOM, material3, ui | UI toolkit (Material 3 includes the core icon set) |
| activity-compose | `setContent`, back handling, file picker launchers |
| lifecycle-viewmodel-compose | State that survives rotation |
| core-ktx | AndroidX baseline required by Compose |
| *test:* junit, org.json | Tests only; org.json replaces Android's stub on the JVM |

No third-party runtime libraries. SQLite is used directly instead of Room: two tables and ~6 queries
do not justify annotation processing plus a runtime library.

## Database (version 1)

```sql
vehicle(id PK, name, registration, fuel_type, notes)          -- one row, id = 1
refuel(id PK AUTOINCREMENT, vehicle_id FK → vehicle,
       timestamp INTEGER,            -- epoch millis
       station, location,
       odometer_km INTEGER,
       volume_ml INTEGER,            -- millilitres
       price_per_litre_milli INTEGER,-- thousandths of currency unit per litre (1.459 → 1459)
       total_cents INTEGER,
       total_manual INTEGER,         -- 1 = user overrode litres × price
       full_tank INTEGER, notes)
INDEX refuel(vehicle_id, timestamp)
```

All quantities are integers: no floating point in stored values or money arithmetic.
Schema changes: bump `VERSION` in `FuelDatabase` and add an `if (oldVersion < N)` migration step.

## Calculations (`logic/FuelStats.kt`)

- **Total** = litres × price, rounded half-up to the cent: `(mL × milli + 5000) / 10000`.
- **Consumption** (full-tank-to-full-tank): between two consecutive full tanks,
  `L/100 km = fuel added after the first fill up to and including the second / km × 100`.
  Partial refuellings in between are added to the interval. Records before the first full tank are ignored.
  If the odometer does not strictly increase, the pending interval is discarded (never a misleading value).
- **Cost per km** = cost of the fuel in the interval / km.
- **Average price** = total paid / total litres (volume-weighted).
- With no valid interval the UI shows "—" with the reason, never 0.

## Backup

- **JSON** (`fuellog-backup`, version 1): lossless, used for restore. Restore validates the whole file first,
  skips malformed records (and says how many), asks for confirmation, and replaces data in one transaction.
- **CSV**: comma-separated, dot decimals, ISO dates — for spreadsheets. Export only.
