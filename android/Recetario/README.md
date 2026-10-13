# Recetario

Minimal, offline personal recipe book. Kotlin Multiplatform + Compose Multiplatform; Android today,
Windows/Linux prepared. Spanish, English, French, German, Portuguese and Italian (device language by default,
or chosen in Settings).

- Library grid with a search bar (title, tags, ingredients; accents ignored), a "show only favourites" button
  and a filter panel (sort by last edit, name or rating; minimum stars; tags). The add button offers
  *Add manually* or *Import from URL (web scraper)*. A gear button opens Settings.
- Create, view, edit, duplicate and delete recipes (deletion asks first). Optional photo per recipe.
- Favourites and 1–5 star ratings, set from the recipe page (tapping the current star clears it).
- Structured ingredients (quantity, unit, name, note) and ordered steps; add, edit, reorder, remove.
- Adjust quantity / yield: by servings (when the recipe states them), preset multipliers (×½ to ×3) or any
  other one (1.5, 1,5, 1/2, ½); text-only ingredients ("salt to taste") never change.
- Settings: theme, language, and every library-wide action (import file or URL, export JSON, backup, cookbook PDF).
- Cooking mode: one step at a time, progress, previous/next, ingredients on demand, screen kept on.
- Import: the app's own JSON and full backups, **RecipeSage** exports (JSON-LD), **My RecipeBox** (RecetteTek)
  `.rtk` archives, and web pages with schema.org/Recipe JSON-LD (shown as an editable preview; nothing is saved
  until you tap Save). The file type is recognised by content.
- Free-text notes per recipe (also where imported descriptions, comments and extra fields go).
- Export: one recipe as PDF; the whole library as JSON, as a full backup ZIP with photos, or as a cookbook PDF.
- Light, dark or system theme. No accounts, no network except the explicit "Import from web page".

Sample data: [`sample-recipes/`](sample-recipes/) has 112 ready-made recipes with photos to import
on a first run.

## Structure

```
Recetario/
├── sample-recipes/               starter recipe collection, importable as-is (see its README)
├── app/                          Android packaging only: id, version, signing, ABI splits, icon, manifest
└── shared/                       all code (Kotlin Multiplatform library)
    └── src/
        ├── commonMain/
        │   ├── kotlin/…/model/       Recipe, Ingredient (also the JSON shape)
        │   ├── kotlin/…/logic/       pure logic, no platform or UI code:
        │   │     Quantities (parse/format/scale) · IngredientParser · RecipeDraft (editor form + validation)
        │   │     RecipeRules (limits shared by editor and import) · LibraryQuery (search/filter/sort)
        │   │     RecipeJson (file format) · Backup (archive layout + validation) · ImportPlan (conflicts)
        │   │     RecipePage + Durations (schema.org JSON-LD) · Text (accents, entities, Markdown, dates)
        │   │     ForeignFormats (RecipeSage, My RecipeBox → recipes + photo sources)
        │   ├── kotlin/…/data/        RecipeRepository (SQLDelight + image files, write ordering)
        │   ├── kotlin/…/platform/    Services: ImageStore, ImageCodec, ZipCodec, WebClient, PdfExporter interfaces
        │   ├── kotlin/…/ui/          AppState (navigation + actions), Transfer (import/export), screens,
        │   │                         Strings (base + es), Translations (fr, de, pt, it, language list), Theme, AppIcons
        │   └── sqldelight/…/Recipe.sq schema and queries
        ├── androidMain/…/platform/   Android implementations, RecetarioApplication, MainActivity, pickers
        ├── commonTest/               logic tests (run on the JVM through the Android unit-test task)
        └── androidUnitTest/          repository and backup tests against real SQLite (JDBC driver) and ZIP
```

Dependency direction: `ui → data → logic → model`; `platform` interfaces are implemented per target.
`commonMain` has no Android (or JVM-only) API; everything platform-specific sits behind an interface in
`platform/Services.kt` or in `PlatformUi` (pickers, saving files, opening links).

## Dependencies

Versions are pinned in [`android/gradle/libs.versions.toml`](../gradle/libs.versions.toml), shared
with FuelLog, VozReves and `android/shared`.

| Dependency | Version | Why |
|---|---|---|
| Kotlin (multiplatform, compose, serialization plugins) | 2.4.21 | Latest stable |
| Compose Multiplatform runtime / foundation / ui | 1.11.1 | Shared UI. 1.12.1 is newer but its Android artifacts require compileSdk 37, which AGP 8 cannot build (verified in CI) |
| Compose Multiplatform Material 3 | 1.9.0 | Last stable multiplatform Material 3 (1.10+ are alphas) |
| Android Gradle Plugin | 8.13.2 | Last 8.x; AGP 9 requires a different KMP module layout (see "Next steps") |
| Gradle | 8.14.3 | Required by AGP 8.13 |
| SQLDelight (runtime, android-driver) | 2.4.1 | Typed SQL on SQLite for every target |
| kotlinx.serialization JSON | 1.11.0 | Export format and JSON-LD parsing |
| kotlinx.coroutines (core, android) | 1.11.0 | Background work |
| androidx.activity:activity-compose | 1.11.0 | `setContent`, back handling, system pickers (Android only) |
| *test:* kotlin-test, SQLDelight sqlite-driver | — | Tests only |

Not used on purpose: no image loader (images are local; decoding is ~40 lines per platform), no HTTP client
(one GET per import with `HttpURLConnection`), no navigation library (a small screen stack in `AppState`),
no icon library (~20 Material paths in `AppIcons`), no resources library (two languages in `Strings`),
no DI framework.

## Data

SQLite database `recetario.db` (schema version 3, `Recipe.sq`): `recipe`, `ingredient`, `step`, `tag`
(children ordered by `position`) and `setting` (theme). Migrations live next to it (`1.sqm`: 1 → 2 adds
`recipe.notes`; `2.sqm`: 2 → 3 adds `rating` and `favorite`). New columns go last, in migration order, because
`SELECT *` reads by position; `MigrationTest` upgrades a real v1 schema through both steps. Settings rows: theme and
language. Photos are JPEG files in the app's private
`files/images/`, referenced by opaque name (`<random>.jpg`), never stored as Base64.

Write ordering (`RecipeRepository`): new image files first → rows in one transaction → old image files
deleted only after commit. A failed transaction deletes the new files. A crash can at worst leave an
unreferenced file, removed at the next start (`deleteOrphanImages`). Rows never point to missing files.
Picked photos are rotated upright (EXIF), scaled to 1600 px and re-encoded (JPEG 85) before saving.

## Formats

**JSON** (`recetario-YYYY-MM-DD.json` or `<recipe>.json`):

```json
{ "format": "recetario", "version": 3, "exportedAt": 1760227200000, "app": "Recetario",
  "recipes": [ { "id": "…", "title": "…", "ingredients": [ {"name": "harina", "quantity": 200.0, "unit": "g"} ],
                 "steps": ["…"], "servings": 4, "prepMinutes": 10, "cookMinutes": 20, "tags": ["…"],
                 "sourceUrl": "https://…", "notes": "…", "rating": 4, "favorite": true,
                 "createdAt": 0, "updatedAt": 0 } ] }
```

Plain JSON carries no photos (no `image` field). Version 2 added `notes`, version 3 `rating` and `favorite`;
older files still import.
Newer `version`s are rejected (an older app would otherwise drop fields silently); unknown fields are ignored.

**Backup ZIP** (`recetario-backup-YYYY-MM-DD.zip`): `manifest.json` (format `recetario-backup`, version, counts,
image list), `recipes.json` (the JSON above with `image` names) and `images/<name>`.

**Import and restore** validate the whole file before writing anything: format and version, every recipe
against `RecipeRules`, unique ids, entry names (no paths outside `images/`), every referenced photo present
and with a JPEG/PNG/WebP signature, size limits (300 MB unpacked, 20 MB per photo). Then, per recipe id:
new → added; identical content → skipped; different content → the user chooses *keep mine*, *import as copies*
(new ids) or *replace mine*. Nothing is overwritten silently. All writes happen in one transaction.

**Other apps** (`logic/ForeignFormats.kt`). Both go through the same review, conflict choice and single
transaction as backups; the source id is kept, so importing the same export twice finds nothing new.

| | RecipeSage (`.json` / `.jsonld`) | My RecipeBox (`.rtk` ZIP) |
|---|---|---|
| Recipes | `recipes[]` of schema.org `Recipe` | `recipes_N.json` arrays |
| Id | `identifier` | `uuid` |
| Ingredients | `recipeIngredient[]` | `ingredients` (one per line; `1count X` → 1 X) |
| Steps | `HowToStep` / `HowToSection` text | `instructions` paragraphs; `**bold**`/links unwrapped |
| Servings | first number of `recipeYield` ("Para 4 Personas") | first number of `quantity` |
| Times | ISO 8601, tolerant of `PT1HM` | "30", "2 h 40", "45 min", "1:30" |
| Tags | `recipeCategory`, emoji prefixes kept | `categories` + `tags` titles |
| Notes | `description` + `comment[].text` | `description`, `notes`, nutrition, cookware, video |
| Photo | first `image` URL, downloaded | file named in `pictures`, else `originalPicture` URL |
| Dates | `dateModified` / `datePublished` | `lastModifiedDate` |
| Rating / favourite | `aggregateRating.ratingValue` (rounded) | `rating` (rounded), `favorite` |

Times with no field of their own (a total that is not prep + cook, resting time) or not understood are added
to the notes with a label in the user's language. Not imported: RecipeBox meal plans and shopping lists. Entries without a title are skipped and counted; repeated ids get a new id. Photos are fetched
(4 at a time, with progress and cancel) and resized only for the recipes that will be written, after the user
confirms; a photo that fails leaves that recipe without one and is reported.

**Web pages**: the page's `<script type="application/ld+json">` blocks are searched for a schema.org `Recipe`
(also inside `@graph` and arrays). Title, ingredients (split into quantity/unit/name/note only when the line
starts with a plain quantity; otherwise kept verbatim), instructions (strings, `HowToStep`, `HowToSection`),
yield, ISO 8601 times, category/cuisine as tags and the photo are taken. Missing parts are listed in the
preview and left empty. Pages without JSON-LD, blocked sites (HTTP errors), network errors and oversized pages
(5 MB) show a message. Only `https`/`http` (Android blocks cleartext by default, so in practice `https`).

## Build and test

CI (`.github/workflows/android.yml`, shared by every app in `android/`) runs, from this folder:

```
./gradlew assembleRelease -Pabi=<arm64-v8a|armeabi-v7a|universal> --no-daemon
./gradlew testReleaseUnitTest --no-daemon
```

Locally the same commands need JDK 17+ and an Android SDK with platform 36. The APKs end up in
*Releases → `android-Recetario-latest`*.

Tests (`shared/src/commonTest`, `shared/src/androidUnitTest`) cover quantity parsing/formatting/scaling,
ingredient line parsing, editor validation, the JSON format (round trip, versions, duplicates, invalid data),
JSON-LD extraction and durations, RecipeSage and My RecipeBox mapping (including the owner's real 108-recipe
RecipeSage export and a real `.rtk`, in `androidUnitTest/resources`), backup layout and validation, conflict
planning, search/filter/sort, the v1 → v2 migration, and the repository and full backup → ZIP → restore path
against real SQLite.

## Desktop (Windows and Linux)

Same code and same UI as Android: `commonMain` is plain Compose. Compose Multiplatform draws the UI
the same way on every OS (Material 3 style, no native system widgets).

- `shared/src/jvmCommonMain`: zip, web downloads and image storage (JVM, shared with Android).
- `shared/src/desktopMain`: `DesktopMain.kt` (window and startup), `DesktopImageCodec` (Skia), `DesktopPlatformUi`
  (native AWT dialogs and links), `WindowsTitleBar.kt` (native title bar dark/light, Windows only), SQLite over JDBC.
  No PDF for now (`Services.pdf = null`, the option is hidden).
- Data: `%APPDATA%\Recetario` on Windows, `~/.recetario` on Linux.
- Build locally: `./gradlew :shared:run` (runs the app; `:shared:desktopRun` exists but is the
  generic task with no `mainClass` configured and fails with "No main class specified"), `:shared:desktopTest` (tests).
  Installers: `:shared:packageMsi` (Windows), `:shared:packageDmg` (macOS), `:shared:createDistributable` (Linux: an app folder, packaged into a tar.gz).
- CI: `.github/workflows/desktop.yml`: `.msi` on `windows-latest` (x64), `.tar.gz` on `ubuntu-latest` (x64) and
  `.dmg` on `macos-14` (Apple Silicon). Publishes all three to the `desktop-recetario-latest` release.
  Left out for now: Linux arm64 (uncommon) and iOS (needs an Apple Developer account to install on an iPhone).

Desktop pending work:
- EXIF rotation for phone photos (photos can come out sideways when imported).
- Keyboard shortcuts (Escape / Alt+←) to go back.
- PDF export (needs a PDF library for the JVM, e.g. PDFBox).
- Desktop database schema migrations (today only a fresh database is created).

Updating Compose Multiplatform to 1.12+ requires AGP 9: `shared` will move from `com.android.library` +
`androidTarget()` to `com.android.kotlin.multiplatform.library`, and the Android unit tests change name.

## Known limitations

- Only one photo per recipe; no cropping.
- Ingredient splitting from web pages is heuristic (English and Spanish units); unparsed lines stay as text.
- A recipe's identity is its id: two different files with the same recipe written independently are separate recipes.
- Restoring a recipe whose content matches an existing one is skipped even if only the photo differs.
- PDF uses the platform's default fonts; no table of contents.
- If the activity is recreated (e.g. rotation) while a system file picker is open, that pick is ignored.
