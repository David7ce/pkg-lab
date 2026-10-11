# Voz del Revés

App Android mínima (Kotlin + Jetpack Compose + Material 3) para grabarte y escucharte al revés.

- Grabar con el micro (hasta 60 s), escuchar normal o al revés, compartir el audio invertido como WAV.
- Sin librerías de audio: `AudioRecord` → invertir el array de muestras → `AudioTrack`.
- Un solo permiso: `RECORD_AUDIO` (se pide al pulsar Grabar).
- Colores dinámicos (Material You) en Android 12+, modo oscuro automático. minSdk 26.

## Compilar

Abre la carpeta en Android Studio y dale a Run, o desde terminal (necesita el Android SDK):

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/
./gradlew assembleRelease    # APK minificada (~1–3 MB), firmada con la clave debug
```

Antes de publicar, crea tu propio keystore y cambia `signingConfig` en `app/build.gradle.kts`.

## Archivos

- `AudioEngine.kt` – grabar, reproducir (normal/invertido) y exportar WAV.
- `MainViewModel.kt` – estado de la app (sobrevive a rotaciones).
- `MainActivity.kt` – la pantalla en Compose.
