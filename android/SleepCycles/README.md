# SleepCycles

Sleep cycle calculator: pick when you want to wake up or go to bed and get six times aligned to 90-minute
cycles, each rated against the recommended sleep for your age range. Native port of the PWA
[sleep-cycles-calc](https://github.com/David7ce/sleep-cycles-calc). Kotlin Multiplatform + Compose
Multiplatform: Android, Windows, Linux and macOS. Spanish, English, French, German, Portuguese and Italian
(device language by default, or chosen in Settings).

- Two modes: *go to bed at* (gives wake-up times) and *wake up at* (gives bedtimes). Results are live:
  they update as the time changes.
- *Sleep now* (also a launcher shortcut on Android): bed mode at the current time.
- Android: the time is set with the system time picker (clock dial with its own keyboard toggle, or scrolling
  numbers, chosen in Settings as *Type of clock*), and tapping a result opens the system clock app on a new
  alarm at that time (permission SET_ALARM, no prompt). Desktop has neither: it keeps the typed hour and minute fields.
- Settings is a dialog with a Done button, and "How does it work?" carries the PWA full text, in six languages.
- Six suggestions (6 to 1 cycles). Each cycle is 90 min; your time to fall asleep (0–120 min, default 15) is added.
- Rating by age range, shown as a face and its description: child 9–11 h, teen 8–10 h, adult 7–9 h,
  senior 7–8 h; "ok" within one cycle of the range and at 4.5 h.
- 24-hour or 12-hour (AM/PM), light/dark/system theme (dark draws the PWA night sky). No accounts,
  no network, no permissions; settings stay on the device.

## Structure

```
SleepCycles/
├── app/        Android packaging only: id, version, signing, ABI splits, icon, manifest, shortcut
└── shared/     Kotlin Multiplatform library (androidTarget + jvm("desktop"))
    └── src/
        ├── commonMain/    logic/ (calculation, time, settings: pure) · platform/ (interfaces) · ui/
        ├── jvmCommonMain/ clock shared by Android and desktop
        ├── androidMain/   SharedPreferences store, Application, MainActivity
        ├── desktopMain/   java.util.prefs store, window, Windows title bar
        └── commonTest/    logic, state and strings tests (run on the Android unit-test task and on desktopTest)
```

Dependency direction: `ui → logic`; `platform` interfaces are implemented per target.

## Run and test

- Android + unit tests: `.\scripts\build-and-test.ps1 -App SleepCycles` (needs the Android SDK).
- Desktop: `./gradlew.bat :shared:run`, tests with `./gradlew.bat :shared:desktopTest`, installer with `:shared:packageMsi`.
- Downloads: *Releases → `sleepcycles-latest`* (APKs, `.msi`, `.tar.gz`, `.dmg`).

## Not tested on real hardware

Verified by CI (unit tests and builds). Not exercised on a physical Android device or on macOS and Linux desktops.
