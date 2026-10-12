# shared (`:pkgshared`)

Código común de las apps Kotlin de `android/`: **multiplataforma** (commonMain = Compose puro).
Lo usan VozReves, FuelLog y Recetario. No tiene recursos de Android: el texto se pasa desde cada app.

## Cómo se incluye

```kotlin
// settings.gradle.kts de la app (la ruta es relativa a la carpeta de la app)
include(":pkgshared")
project(":pkgshared").projectDir = file("../shared")

// app/build.gradle.kts (Android) o shared/build.gradle.kts (KMP)
implementation(project(":pkgshared"))
```

Requisitos: Kotlin **2.4.21**, Compose Multiplatform **1.11.1**, Material 3 **1.9.0**, AGP **8.13.x**,
`compileSdk` 36, Gradle 8.14.x. Las versiones están fijadas en `build.gradle.kts` de este módulo.

## Textos de cada app

En la raíz de cada app, envolver el contenido:

```kotlin
CompositionLocalProvider(LocalSharedStrings provides SharedStrings(
    cancel = stringResource(R.string.cancel), back = …, close = …, delete = …, selectAll = …
)) { … }
```

## Piezas

| Pieza | Para qué |
|---|---|
| `ui/SegmentedChoice` | Selector en fila con icono opcional (sin check, sin solapamientos). |
| `ui/ChoiceDropdown` | Desplegable de una elección (idioma, moneda, unidades…). |
| `ui/ConfirmDialog` | Confirmación antes de borrar o de cualquier acción irreversible. |
| `ui/Selection` + `SelectionActions` | Selección múltiple: pulsación larga, seleccionar todo, borrar. |
| `ui/SubScreen`, `SectionTitle` | Barra superior de pantalla secundaria y títulos de sección. |
| `ui/SharedIcons` | Iconos Material propios (sin material-icons, que no es multiplataforma). |
| `ui/SharedStrings` | Textos comunes que rellena cada app. |
| `reverseText` | Invierte texto por puntos de código (no rompe emojis). |

Regla: lo que es **UX o comportamiento** (confirmar borrados, selección múltiple, selectores, idioma)
vive aquí y es igual en todas las apps. Lo que es **identidad** (colores, icono, nombre, textos propios)
vive en cada app. Un cambio en `android/shared/` hace que CI recompile todas las apps.
