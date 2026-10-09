# Expenses (Chinese edition)

A free and open-source app for tracking your daily expenses in a beautifully simple way.

![Screens](resources/screens.png)

[中文说明](README.md)

## Download

Download the newest APK from the [releases page](https://github.com/minecraftmc22/expenses-zhcn/releases).

> This is a Chinese-localised fork of [nominalista/expenses](https://github.com/nominalista/expenses).
> The application id is `com.minecraftmc22.expenses` and the version numbering restarts at `0.3`.

## Features

- Expenses with amount, currency, date, title, notes and tags
- Summary for today / this week / this month / all time
- Filter by tag, sort by date or amount
- More than 160 currencies with search
- Light / dark / system default theme
- Export to Excel (.xls) and import from Excel
- Optional Google sign-in with cloud sync; without sign-in everything stays in the local Room database
- Simplified Chinese, Traditional Chinese, English, Español and Polski UI

## Languages

| Language | Resource directory |
| --- | --- |
| Simplified Chinese | `app/src/main/res/values-zh-rCN/strings.xml` |
| Traditional Chinese | `app/src/main/res/values-zh-rTW/strings.xml` |
| English | `app/src/main/res/values/strings.xml` |
| English (AU) | `app/src/main/res/values-en-rAU/strings.xml` |
| Español | `app/src/main/res/values-es/strings.xml` |
| Polski | `app/src/main/res/values-pl-rPL/strings.xml` |

To add a language, copy `values/strings.xml` into `values-<language code>/strings.xml` and translate it.

## Building with GitHub Actions

The repository ships with [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml).

It runs:

- on every push to `master` / `main` and on every pull request — builds and uploads the APKs
- on demand from the **Actions** tab with **Run workflow**
- on `v*` tags (for example `v0.3`) — also attaches the APKs to that GitHub release

Artifacts (downloadable from the workflow run):

- `app-prod-release.apk` — release build
- `app-prod-debug.apk` — debug build

> Note: Gradle 5.4.1 / AGP 3.5 is a 2019-era toolchain and must run on JDK 8, so the workflow
> installs the Android SDK command line tools with JDK 17 first and then switches back to JDK 8
> for the build. If you hit a toolchain related failure, try changing `runs-on: ubuntu-latest`
> to `ubuntu-22.04`.

### Signing the release APK (optional)

Without configuration the release APK is produced **unsigned** (fine to archive, but it cannot be
installed or distributed as is). Configure the secrets below and the workflow signs it for you.

Add these in **Settings → Secrets and variables → Actions**:

| Secret | Meaning |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Base64 of the keystore (.jks) file |
| `RELEASE_STORE_PASSWORD` | keystore password |
| `RELEASE_KEY_ALIAS` | key alias |
| `RELEASE_KEY_PASSWORD` | key password |

**No keystore yet?** Create one first (`keytool` ships with the JRE and is usually not on PATH on Windows):

```powershell
& "C:\Program Files\Java\jre1.8.0_421\bin\keytool.exe" -genkeypair -v `
  -keystore "$env:USERPROFILE\expenses-release.jks" `
  -alias expenses -keyalg RSA -keysize 2048 -validity 10000 -storetype JKS
```

It asks for the keystore password, the key password (press Enter to reuse the same one) and the
CN/OU/O/L/ST/C fields (anything works, they only show up in the certificate name).

Produce the Base64 and copy it to the clipboard:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\expenses-release.jks")) `
  | Set-Content "$env:USERPROFILE\keystore.txt"
Get-Content "$env:USERPROFILE\keystore.txt" | Set-Clipboard
```

All four secrets are required, and they must go under
**Settings → Secrets and variables → Actions → the `Secrets` tab** (not `Variables`; do not create them as
environment secrets either — those need an `environment:` declaration in the workflow, which this project
does not use).

> ⚠️ **Back up the `.jks` file and both passwords.** If they are lost you can never update the app with the
> same signature again and users have to uninstall and reinstall. `.jks`, `.keystore` and `keystore.txt`
> are all in `.gitignore`; never force-add them.
>
> **Why an APK can come out unsigned**: the log prints `Signing secrets are incomplete, missing: ...` and
> names the missing secret, and the artifact is called `app-prod-release-unsigned.apk`. Add the secrets and
> re-run — they are read at run time, so a new run picks them up. If only `RELEASE_KEYSTORE_BASE64` is set
> while the other three are missing, the build fails at the end with a clear error instead of silently
> producing an unsigned APK.

### Firebase configuration (optional)

`google-services.json` is ignored by git. The workflow prepares it like this:

1. if the `GOOGLE_SERVICES_JSON` secret is set, it is written to `app/google-services.json`;
2. otherwise the bundled placeholder `app/google-services.json.ci` is copied over.

The placeholder only exists so that the project compiles and the app can run in local-only mode
(Room database). APKs built with it cannot use "Continue with Google" sign-in or cloud sync.
Its API key and client id are deliberately obvious dummies (`PLACEHOLDER-NOT-A-REAL-...`) and contain
no real credentials, so GitHub secret scanning neither reports nor should report them.

To enable cloud sync, create a project in the [Firebase console](https://console.firebase.google.com/),
register the Android app `com.minecraftmc22.expenses` (the debug build is `com.minecraftmc22.expenses.dev`),
and store the whole downloaded `google-services.json` as the `GOOGLE_SERVICES_JSON` secret.

### When Google sign-in fails

Since 0.3 a failed sign-in is no longer swallowed — the app shows the error code, for example
"Google sign-in failed (10)". An incomplete configuration no longer breaks the build either; it is
reported when you tap sign-in.

| What you see | Meaning | What to do |
| --- | --- | --- |
| "Google sign-in is not configured for this build" | `oauth_client` in `google-services.json` is an empty array | Enable the **Google** provider under Firebase → Authentication, then **download `google-services.json` again** |
| `10` | Signing certificate or OAuth client ID does not match the project | Register the SHA-1 of the keystore you actually sign with |
| `12500` | Google sign-in is not enabled | Enable the Google sign-in provider in the Firebase console |
| `12501` | The user cancelled the account picker | Normal, just try again |
| `7` | Network error | Check the network or proxy |
| starts with `ERROR_` | Rejected by Firebase | Usually a project/API key problem — first check you are not on the placeholder config |
| a plain English sentence | Google returned no ID token | `default_web_client_id` does not belong to this app's project, or the signing certificate is not registered |

If **Google itself** shows "Access blocked / access_denied / 403" after picking an account, the OAuth
consent screen is still in **Testing**: publish it in Google Cloud Console → **OAuth consent screen**,
or add your Google account under **Test users**.

> **Looking up which SHA-1 is registered**: in `google-services.json`, the `certificate_hash` of every
> `client_type: 1` entry is a SHA-1 without the colons. Compare it with the `SHA1:` line printed by the
> Keystore fingerprint workflow — if they differ, that is the cause of error `10`.
>
> The `client_type: 3` entry is what produces `default_web_client_id`; when no such entry exists, the
> Google sign-in provider has not been enabled yet.

> **An APK built by Actions cannot sign in by default**: without the `GOOGLE_SERVICES_JSON` secret it is
> built with the bundled placeholder `app/google-services.json.ci`, which matches no real project.
> That is not a code problem.

To make sign-in actually work:

> Direct links are the quickest way to the right pages (replace `<PROJECT_ID>` with your project id):
>
> - enable Google sign-in: `https://console.firebase.google.com/project/<PROJECT_ID>/authentication/providers`
> - add the SHA-1 fingerprint / download `google-services.json`:
>   `https://console.firebase.google.com/project/<PROJECT_ID>/settings/general`

1. Create a Firebase project.
2. Add the Android app `com.minecraftmc22.expenses` (and `com.minecraftmc22.expenses.dev` for debug builds).
3. Enable **Google** under **Authentication → Sign-in method**.
4. **Register the signing certificate SHA-1** (the step people forget):
   - **Easiest**: Actions → **Keystore fingerprint** → *Run workflow*; the log prints the
     `SHA1:` / `SHA256:` lines (see [`.github/workflows/keystore-fingerprint.yml`](.github/workflows/keystore-fingerprint.yml))
   - If you have the APK: `keytool -printcert -jarfile app-prod-release.apk`
   - If you have the keystore: `keytool -list -v -keystore release.jks -alias <your alias>`
   - On Windows `keytool` is usually not on PATH, so use the full path, for example
     `"C:\Program Files\Java\jre1.8.0_421\bin\keytool.exe" -printcert -jarfile app-prod-release.apk`
5. Download the new `google-services.json` and store it as the `GOOGLE_SERVICES_JSON` secret.
6. Re-run the workflow and test the new APK.

> ℹ️ **Google sign-in validates against the SHA-1 (40 hex digits). SHA-256 neither replaces it nor can
> it be derived from it**; SHA-256 is an optional extra fingerprint.
>
> With the signing secrets configured, **debug builds are signed with the same release keystore**, so
> `app-prod-release.apk` and `app-prod-debug.apk` share one fingerprint and registering it once covers
> both. Without the secrets the debug APK is signed with the GitHub runner's own debug key, whose
> fingerprint cannot be registered, so sign-in will not work there.
>
> Also: **adding a SHA-1 does not require downloading `google-services.json` again** (fingerprints
> are not part of that file), but **enabling the Google provider does**, because that adds the
> `oauth_client` entries.

Whenever the signing key changes (for example from the debug key to the release key) the SHA-1 has to be
registered again, otherwise the code goes back to `10`.


## Local build

Requires JDK 8 and the Android SDK (`platforms;android-29`, `build-tools;29.0.2`).

```bash
./gradlew assembleProdRelease
```

The four signing properties can live in `~/.gradle/gradle.properties`
(without them the release APK is simply unsigned):

```properties
EXPENSES_STORE_FILE=/absolute/path/to/release.jks
EXPENSES_STORE_PASSWORD=your-password
EXPENSES_KEY_ALIAS=your-alias
EXPENSES_KEY_PASSWORD=your-password
```

## Contact

This fork is maintained by [Minecraftmc22](https://github.com/minecraftmc22);
the original app is developed by [nominalista](https://github.com/nominalista).
Feel free to reach out to [i.am.minecraftmc22@gmail.com](mailto:i.am.minecraftmc22@gmail.com).

## Copyright

    Copyright 2019 Mc22's Studio. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
