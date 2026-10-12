# FuelLog

Minimal personal fuel tracker for Android: several vehicles, refuellings, prices, consumption and expenses.
Data is local only; the optional map loads OpenStreetMap tiles. 8 languages (en, es, fr, de, pt, zh, hi, ar).

## Structure

Shared UI (selectors, confirmations, multi-select, dropdowns) comes from `android/shared/` (see its README).


```
app/src/main/java/com/deivon/fuellog/
├── data/    Models.kt (Vehicle, Refuel) · FuelDatabase.kt (SQLiteOpenHelper)
├── logic/   Decimals · FuelStats (+ Extremes) · Period · RefuelForm (validation, warnings, pay-by-amount)
│            Units (metric / UK / US) · Backup (JSON/CSV) · GeoPoint (+ Mercator)
│            ModelCatalog (brand/model list from assets/models_es.csv)
└── ui/      MainActivity (3 tabs) · Overview · History · RefuelEditor · VehicleEditor · SettingsScreen
             AppViewModel (+ UiEvent: saved / deleted with undo) · AppSettings (language, theme, units, currency)
             AppIcons · Theme · Formats (locale + units) · Widgets (DropdownField, ConsumptionChart)
    ui/map/  TileMap (OSM tiles on a Canvas) · MapPicker · DeviceLocation (one-shot, platform LocationManager)
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

No third-party runtime libraries. SQLite is used directly instead of Room: two tables and a few queries
do not justify annotation processing plus a runtime library. The map is ~150 lines of Compose drawing
OpenStreetMap tiles, cached in memory (LRU) and on disk (platform `HttpResponseCache`), instead of a map SDK
(osmdroid/MapLibre add megabytes; Google Maps needs Play services and an API key). Vehicle and UI icons are
~20 Material paths copied into `AppIcons` instead of `material-icons-extended`.

## Database (version 3)

```sql
vehicle(id PK AUTOINCREMENT, name, type, registration, fuel_type, notes,  -- type: car, motorcycle, van, truck, bus, plane, boat, other
        ref_consumption_dl INTEGER)   -- optional reference consumption, tenths of L/100 km (54 = 5.4)
refuel(id PK AUTOINCREMENT, vehicle_id FK → vehicle ON DELETE CASCADE,
       timestamp INTEGER,             -- epoch millis
       station,
       odometer_km INTEGER,
       volume_ml INTEGER,             -- millilitres
       price_per_litre_milli INTEGER, -- thousandths of currency unit per litre (1.459 → 1459)
       total_cents INTEGER,           -- always litres × price
       full_tank INTEGER,
       missed_previous INTEGER,       -- 1 = an earlier refuel was never logged
       notes,
       latitude REAL, longitude REAL) -- optional, display only
INDEX refuel(vehicle_id, timestamp)
```

Money and fuel quantities are integers: no floating point in stored values or money arithmetic.

**Migration 1 → 2**: vehicles get `type = 'car'`; the refuel table is rebuilt (older Android SQLite cannot
drop columns) without `location` text and `total_manual`; any old location text is appended to the notes and
stored totals are kept.
**Migration 2 → 3**: adds `vehicle.ref_consumption_dl` and `refuel.missed_previous` (skipped if the column
already exists, as on a v1 database rebuilt in the same upgrade). Schema changes: bump `VERSION` in `FuelDatabase` and add an `if (oldVersion < N)` step.

## Calculations (`logic/FuelStats.kt`)

- **Total** = litres × price, rounded half-up to the cent: `(mL × milli + 5000) / 10000`.
- **Consumption** (full-tank-to-full-tank): between two consecutive full tanks,
  `L/100 km = fuel added after the first fill up to and including the second / km × 100`.
  Partial refuellings in between are added to the interval. Records before the first full tank are ignored.
  If the odometer does not strictly increase, or a record says the previous refuel was not logged, the pending
  interval is discarded (never a misleading value).
- **Versus reference**: `(real / reference − 1) × 100`, shown on the overview when the vehicle has a reference.
- **Cost per km** = cost of the fuel in the interval / km.
- **Average price** = total paid / total litres (volume-weighted).
- **Highlights**: highest/lowest price per litre and best/worst consumption, only when there are at least
  two different values to compare.
- With no valid interval the UI shows "—" with the reason, never 0.
- **Pay by amount**: volume = amount ÷ price, rounded to the mL; the stored total is then volume × price, which
  equals the amount typed for normal prices (tested).
- **Warnings** (record is valid but asked to confirm): > 2 500 km since the previous refuel, an interval outside
  2–30 L/100 km, or a price more than 40 % away from the vehicle's average.

## Units

Storage is always metric. `Units` converts input and display: UK = miles, litres, imperial mpg; US = miles,
US gallons, US mpg. A price typed per US gallon is stored per litre to the thousandth, so a US total can differ
from the receipt by about a cent.

## Backup

- **JSON** (`fuellog-backup`, version 2: list of vehicles with their refuellings; version 1 files still restore):
  lossless, used for restore. Restore validates the whole file first,
  skips malformed records (and says how many), asks for confirmation, and replaces data in one transaction.
- Optional fields added without a version bump: vehicle `referenceDl100`, refuel `missedPrevious`.
- **CSV**: comma-separated, dot decimals, ISO dates — for spreadsheets. Export only.

## Map

OpenStreetMap tiles (`tile.openstreetmap.org`) with an identifying User-Agent, at most 4 downloads at once,
disk-cached, and the required "© OpenStreetMap contributors" attribution. Fine for personal use; heavy use
should switch to a tile provider with its own terms. Location permission is requested only for "My location".

## Model catalogue (Spain)

`assets/models_es.csv` lists the 2025 best-selling car models in Spain (ANFAC registrations, top 50, minus
electric-only, plug-in-led and the Mini range). The `l100` column is an **approximate** WLTP combined figure for a
common version, not verified against IDAE's database; it only pre-fills the editable reference consumption.
To extend it, add `brand,model,fuel,l100` lines; `ModelCatalogTest` checks the shipped file parses fully.
