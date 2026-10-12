# shared

Código común de las apps Kotlin de `android/`. Cualquier app lo incluye así:

```kotlin
// settings.gradle.kts de la app
include(":shared")
project(":shared").projectDir = file("../shared")
// app/build.gradle.kts
implementation(project(":shared"))
```

| Pieza | Para qué |
|---|---|
| `ui/SegmentedChoice` | Selector en fila con icono opcional (sin check, sin solapamientos). |
| `ui/ChoiceDropdown` | Desplegable de una elección (idioma, moneda…). |
| `ui/ConfirmDialog` | Confirmación antes de borrar o de cualquier acción irreversible. |
| `ui/Selection` + `SelectionActions` | Selección múltiple: pulsación larga, seleccionar todo, borrar. |
| `ui/SubScreen`, `SectionTitle` | Barra superior de pantalla secundaria y títulos de sección. |
| `ui/SharedIcons` | Iconos extra no incluidos en material-icons-core. |
| `reverseText` | Invierte texto por puntos de código (no rompe emojis). |

Regla: lo que es **UX o comportamiento** (confirmar borrados, selección múltiple, selectores, idioma)
vive aquí y es igual en todas las apps. Lo que es **identidad** (colores, icono, nombre, textos propios)
vive en cada app. Un cambio en `android/shared/` hace que CI recompile todas las apps.
