# Voz del Revés

App Android (Kotlin + Jetpack Compose + Material 3) para grabarte y escucharte al revés.

- Grabar con el micro (hasta 60 s), escuchar normal o al revés, con play/pausa rápido y control de tramo.
- Guardar el audio invertido como WAV en la carpeta Música/VozReves (Android 10+) o en la carpeta que elijas (Android 8–9).
- Frases: lista editable de frases que la app lee con voz del sistema (TTS), también al revés.
- Historial: reproducir, renombrar, compartir, borrar con confirmación y selección múltiple.
- Idiomas: español, inglés, chino, hindi y árabe, o el del sistema. Colores dinámicos (Material You) en Android 12+.
- minSdk 26, compileSdk 36.

## Estructura

```
app/src/main/java/com/deivon/vozreves/
├── AudioEngine.kt    – grabar y reproducir PCM 16 bits (normal/invertido), pausa y tramos
├── Wav.kt            – escribir/leer WAV, decodificar y remuestrear
├── History.kt        – grabaciones guardadas (un WAV por grabación, títulos en titles.properties)
├── Phrases.kt        – lista de frases (archivo de texto en la carpeta privada)
├── Tts.kt            – convertir una frase en audio con la voz del sistema
├── Names.kt          – nombres de archivo (reverse_voice_AAAA_MM_DD_HH-mm-ss.wav)
├── Settings.kt       – preferencias (idioma, tema, duración máxima de grabación)
├── Waveform.kt       – dibujo de la forma de onda
├── TimeFormat.kt     – formato de tiempos (mm:ss)
├── MainViewModel.kt  – estado de la app (sobrevive a rotaciones)
├── MainActivity.kt   – pantallas en Compose: Principal, Frases, Historial, Ajustes
└── AppIcons.kt       – iconos propios de la app
```

Usa el módulo compartido `android/shared` (`:pkgshared`) para los componentes de UI comunes:
`SegmentedChoice` (normal/invertido), `ChoiceDropdown` (idioma), `ConfirmDialog`, `Selection`
(selección múltiple), `SubScreen`, `SectionTitle` y `LocalSharedStrings` (textos de botones comunes).
Ver `android/shared/README.md`.

## Tests

En `app/src/test`: WAV (ida y vuelta), historial, nombres de archivo, frases, decodificación y
`reverseText`/`Selection` del módulo compartido. Se ejecutan en cada build de CI.

## Compilar

Abre la carpeta `android/VozReves` en Android Studio (incluye `:pkgshared` por `settings.gradle.kts`) y dale a Run,
o desde terminal (necesita el Android SDK):

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/
./gradlew assembleRelease    # APK minificada, firmada con la clave común del repo
./gradlew testDebugUnitTest  # tests unitarios
```

Requisitos: Gradle 8.14.3, AGP 8.13.2, Kotlin 2.4.21, JDK 17.

## Firma

`app/build.gradle.kts` lee la firma de las variables `SIGNING_*` (keystore y contraseña). En CI se descifra
`.github/signing/release.jks.enc` con el secret `ANDROID_KEYSTORE_PASSWORD`. Sin esas variables la build usa la
clave debug. Para publicar con tu propia clave, cambia `signingConfig` en `app/build.gradle.kts`.

## Notas

- Probado solo en CI (compilación y tests). La reproducción en dispositivo real (play/pausa, iconos, selección)
  debe verificarse en un móvil antes de dar la app por buena.
