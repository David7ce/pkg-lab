# pkg-lab

Laboratorio para compilar y probar apps en la nube con GitHub Actions, sin necesidad de PC.
Organizado por sistema operativo para poder crecer a otras plataformas.

```
pkg-lab/
├── .github/workflows/
│   └── android.yml        ← compila todo lo que hay en android/
├── android/
│   └── VozReves/          ← cada app es un proyecto Gradle independiente
├── linux/                 ← (futuro) con su propio linux.yml
├── windows/               ← (futuro)
└── web/                   ← (futuro)
```

## Android

- **Añadir una app:** crea `android/<NombreApp>/` con un proyecto Gradle (con `gradlew`). No hay que tocar el workflow.
- **Compilar:** cada push a `main` compila solo las apps que han cambiado. También a mano desde
  *Actions → Android → Run workflow* (escribe el nombre de la carpeta o déjalo vacío para todas).
- **Descargar:** *Releases → `android-<NombreApp>-latest`*. Cada app se compila en paralelo para
  `arm64-v8a` (casi todos los móviles), `armeabi-v7a` (32 bits antiguos) y `universal`.
  Para que una app genere APKs por arquitectura, su `app/build.gradle.kts` debe leer `-Pabi`
  (ver VozReves); si no, las tres salen iguales.

### Firma (recomendado)

Sin keystore, cada build se firma con una clave debug distinta y Android no deja actualizar encima:
hay que desinstalar antes. Para evitarlo, añade en *Settings → Secrets and variables → Actions*:

| Secret | Contenido |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | el archivo `.jks` en base64 |
| `ANDROID_KEYSTORE_PASSWORD` | contraseña del keystore |
| `ANDROID_KEY_ALIAS` | alias de la clave |
| `ANDROID_KEY_PASSWORD` | contraseña de la clave |

Las apps leen la firma de las variables `SIGNING_*` en su `app/build.gradle.kts` (ver VozReves como ejemplo).

## Otras plataformas

Para una plataforma nueva: carpeta `<os>/` + workflow `<os>.yml` con `paths: ["<os>/**"]`,
siguiendo el mismo patrón (detectar cambios → compilar en matriz → release `<os>-<App>-latest`).
