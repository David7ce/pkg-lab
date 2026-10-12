# pkg-lab

Laboratorio para compilar y probar apps en la nube con GitHub Actions, sin necesidad de PC.
Organizado por sistema operativo para poder crecer a otras plataformas.

```
pkg-lab/
├── .github/workflows/
│   └── android.yml        ← compila todo lo que hay en android/
├── android/
│   ├── VozReves/          ← cada app es un proyecto Gradle independiente
│   ├── FuelLog/           ← registro de repostajes y consumo
│   └── Recetario/         ← recetario personal (Kotlin Multiplatform: preparado para Windows/Linux)
├── linux/                 ← (futuro) con su propio linux.yml
├── windows/               ← (futuro)
└── web/                   ← (futuro)
```

## Android

- **Añadir una app:** crea `android/<NombreApp>/` con un proyecto Gradle (con `gradlew`). No hay que tocar el workflow.
- **Tests:** si la app tiene tests unitarios (`app/src/test`, o los de sus módulos), se ejecutan en cada build
  y el recuento aparece como aviso en la ejecución.
- **Errores:** si falla la compilación o un test, los errores de Kotlin/Gradle aparecen como anotaciones en la
  página de la ejecución, sin tener que abrir el log completo.
- **Apps multiplataforma:** una app Kotlin Multiplatform vive igual en `android/<App>/`, con su código común en un
  módulo `shared` y `app` solo para empaquetar (ver Recetario). El CI no cambia.
- **Compilar:** cada push a `main` compila solo las apps que han cambiado. También a mano desde
  *Actions → Android → Run workflow* (escribe el nombre de la carpeta o déjalo vacío para todas).
- **Descargar:** *Releases → `android-<NombreApp>-latest`*. Cada app se compila en paralelo para
  `arm64-v8a` (casi todos los móviles), `armeabi-v7a` (32 bits antiguos) y `universal`.
  Para que una app genere APKs por arquitectura, su `app/build.gradle.kts` debe leer `-Pabi`
  (ver VozReves); si no, las tres salen iguales.
- **Nombre de las APK:** `<App>-android-<versión>-<arquitectura>.apk` (la versión se lee de la propia APK).

### Firma (recomendado)

Sin keystore, cada build se firma con una clave debug distinta y Android no deja actualizar encima:
hay que desinstalar antes. Por eso todas las apps se firman con una clave común:

- `.github/signing/release.jks.enc`: keystore cifrado con AES-256 (alias `pkglab`). Es seguro
  tenerlo en un repo público; sin la contraseña no sirve de nada.
- Secret `ANDROID_KEYSTORE_PASSWORD` (*Settings → Secrets and variables → Actions*): la contraseña.
  **Guárdala también en un gestor de contraseñas**: si se pierde, las apps instaladas no podrán
  actualizarse con una clave nueva.

Para descifrarlo en local: `openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -in release.jks.enc -out release.jks`.
Las apps leen la firma de las variables `SIGNING_*` en su `app/build.gradle.kts` (ver VozReves como ejemplo).

## Otras plataformas

Para una plataforma nueva: carpeta `<os>/` + workflow `<os>.yml` con `paths: ["<os>/**"]`,
siguiendo el mismo patrón (detectar cambios → compilar en matriz → release `<os>-<App>-latest`).
