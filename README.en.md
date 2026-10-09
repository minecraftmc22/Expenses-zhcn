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

Producing the Base64 on Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Content keystore.txt
```

### Firebase configuration (optional)

`google-services.json` is ignored by git. The workflow prepares it like this:

1. if the `GOOGLE_SERVICES_JSON` secret is set, it is written to `app/google-services.json`;
2. otherwise the bundled placeholder `app/google-services.json.ci` is copied over.

The placeholder only exists so that the project compiles and the app can run in local-only mode
(Room database). APKs built with it cannot use "Continue with Google" sign-in or cloud sync.

To enable cloud sync, create a project in the [Firebase console](https://console.firebase.google.com/),
register the Android app `com.minecraftmc22.expenses` (the debug build is `com.minecraftmc22.expenses.dev`),
and store the whole downloaded `google-services.json` as the `GOOGLE_SERVICES_JSON` secret.

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
