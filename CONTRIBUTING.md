# Contributing to SelfControl

Thanks for your interest! Bug reports, feature ideas, device-compatibility notes and pull requests
are all welcome. This is a small, focused project — keep changes simple and well-scoped.

## Ways to contribute

- **Report a bug** — open an issue with your device model, Android/OneUI version, and clear steps to
  reproduce. Logcat output (`adb logcat | grep SelfControl`) helps a lot.
- **Request a feature** — open an issue describing the use case before writing code, so we can
  agree on the approach.
- **Improve docs** — typo fixes, clarifications and translations are very welcome.
- **Submit code** — see the workflow below.

## Development setup

Requirements: **JDK 17** and the **Android SDK** (API 34). No rooted device needed.

```bash
git clone <your-fork-url>
cd selfcontrol
./gradlew assembleDebug          # Windows: .\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Create a `local.properties` at the root if Gradle can't find your SDK:
`sdk.dir=/path/to/Android/Sdk`. It is git-ignored — never commit it.

Read [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) first to understand how the two services
(`AppWatcherService` + `LimitService`) fit together.

## Pull request workflow

1. Fork the repo and create a branch off `main` (e.g. `fix/curfew-midnight`).
2. Make your change. Keep the diff focused — one concern per PR.
3. Make sure it builds: `./gradlew assembleDebug`.
4. If you can, test on a real device (the accessibility behavior is hard to unit-test).
5. Open the PR with a clear description of *what* changed and *why*.

## Coding conventions

- **Kotlin**, matching the style already in the codebase (4-space indent, descriptive names).
- Code and comments are in **English**. The one deliberate exception: the keyword sets in
  `AppWatcherService` (`CANCEL_KEYWORDS`, `DANGER_KEYWORDS`) contain localized strings because they
  match the on-screen text of the system Settings UI. If you add support for another language's
  Settings, add the localized labels there.
- Don't break the **anti-cheat delay**: loosening a limit must stay gated behind `DelayManager`;
  tightening stays immediate.
- Don't add network calls, analytics or trackers. The app is fully offline by design, and the
  on-device event log stays OFF by default (`EVENT_LOG_ENABLED = false`).

## Scope & philosophy

SelfControl is a **self-discipline tool**, not tamper-proof control software. It runs with no root
and no special privileges, and that's intentional — keep contributions within that boundary. Power
features that require Device Owner / root are out of scope for this edition.

## License

By contributing, you agree that your contributions are licensed under the project's
[MIT License](LICENSE).
