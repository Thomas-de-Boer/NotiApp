# Noti App: instructions for Claude Code

Private Android app (Kotlin + Jetpack Compose) for one family: tap a button and every family phone gets a
notification, optionally ringing like a call. Messages go through Firebase Cloud Messaging, sent directly from
the phone (no server). See README.md for how it works and how to set it up.

## Secrets: never in git, never on GitHub (the repo is PUBLIC)
These files contain secrets and must **never** be committed, pushed, uploaded, pasted into issues/PRs, or attached
to a GitHub release:

- `app/src/main/assets/fcm_service_account.json`: service-account key that can send notifications to the family
- `app/google-services.json`: Firebase project config
- any `*.apk` / `*.aab`: the built app **contains the service-account key**, so APKs are shared privately within
  the family only, never via GitHub releases
- signing keystores (`*.jks`, `*.keystore`, `debug.keystore`)

Rules:
- They are listed in `.gitignore`. Never remove those lines, and never use `git add -f` on them.
- Before every commit, check `git status` / `git diff --cached --name-only` and make sure none of these files
  (or their contents, e.g. `"private_key"`, `BEGIN PRIVATE KEY`) are staged.
- Never print the contents of these files in output. Showing only `project_id` or `type` is fine.
- If a secret is ever committed or pushed: stop, tell the user immediately, and explain how to revoke the key
  (Google Cloud, then the service account, then **Keys**, then delete it and create a new one) before rewriting history.

## Conventions
- App UI text: **Dutch** (`app/src/main/res/values/strings.xml`). README and docs: **English**.
- Commit messages: **English**. One commit per feature or fix, then push to `origin main`.
- Bump `versionCode` (and usually `versionName`) in `app/build.gradle.kts` for every release the family installs.

## Testing
- Build: `.\gradlew assembleDebug` / `assembleRelease` with `JAVA_HOME` set to
  `C:\Program Files\Android\Android Studio\jbr`.
- The real family phones use the same Firebase project. In emulator tests **never send to "Iedereen"**
  (the `gezin` topic): it reaches and can ring the real phones. Only send with "Aan:" set to an emulator.
- Uninstall the app from emulators after testing, so their test members disappear from the real phones.
