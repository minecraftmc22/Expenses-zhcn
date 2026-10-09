# 记账 Expenses（中文版）

一款免费、开源的记账应用，用简洁优雅的方式记录你的日常支出。

![界面预览](resources/screens.png)

[English README](README.en.md)

## 下载

最新版 APK 请到 [Releases 页面](https://github.com/minecraftmc22/expenses-zhcn/releases) 下载。

> 本项目是 [nominalista/expenses](https://github.com/nominalista/expenses) 的中文本地化分支：
> 已内置简体 / 繁体中文界面，应用包名为 `com.minecraftmc22.expenses`，版本号从 `0.3` 重新开始。

## 功能

- 记录支出：金额、货币、日期、标题、备注和标签
- 汇总区间：今天 / 本周 / 本月 / 全部时间
- 按标签筛选，可按日期或金额排序
- 160 多种货币，支持搜索
- 浅色 / 深色 / 跟随系统主题
- 导出为 Excel（.xls）、从 Excel 导入
- 可选登录 Google 账号把数据同步到云端；未登录时数据保存在本机 Room 数据库
- 简体中文、繁體中文、English、Español、Polski 界面

## 语言资源

| 语言 | 资源目录 |
| --- | --- |
| 简体中文 | `app/src/main/res/values-zh-rCN/strings.xml` |
| 繁體中文 | `app/src/main/res/values-zh-rTW/strings.xml` |
| English | `app/src/main/res/values/strings.xml` |
| English (AU) | `app/src/main/res/values-en-rAU/strings.xml` |
| Español | `app/src/main/res/values-es/strings.xml` |
| Polski | `app/src/main/res/values-pl-rPL/strings.xml` |

想补充其他语言，复制 `values/strings.xml` 到对应的 `values-<语言代码>/strings.xml` 翻译即可。

## 用 GitHub Actions 打包

仓库已内置工作流 [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml)。

触发方式：

- 推送到 `master` / `main` 分支，或发起 Pull Request —— 自动构建并上传 APK
- 在仓库 **Actions** 页面手动点击 **Run workflow**
- 推送 `v*` 形式的标签（例如 `v0.3`）—— 构建后自动把 APK 附加到对应的 Release

构建产物（在 Actions 运行的 Artifacts 里下载）：

- `app-prod-release.apk` —— 正式版
- `app-prod-debug.apk` —— 调试版

> 说明：Gradle 5.4.1 / AGP 3.5 这套 2019 年的工具链必须跑在 JDK 8 上，
> 所以工作流先用 JDK 17 装 Android SDK 命令行工具，再切回 JDK 8 构建。
> 万一遇到工具链相关的报错，可以把 `runs-on: ubuntu-latest` 改成 `ubuntu-22.04` 再试。

### 配置自己的签名（可选）

不配置时，release APK 会以**未签名**方式产出（可以留档，但无法直接安装分发）。
配置后工作流会自动签名，产物就是可直接安装的 `app-prod-release.apk`。

在仓库 **Settings → Secrets and variables → Actions** 中添加：

| Secret | 说明 |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | keystore（.jks）文件的 Base64 内容 |
| `RELEASE_STORE_PASSWORD` | keystore 密码 |
| `RELEASE_KEY_ALIAS` | 密钥别名 |
| `RELEASE_KEY_PASSWORD` | 密钥密码 |

生成 Base64（Windows PowerShell）：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Content keystore.txt
```

### 配置 Firebase（可选）

`google-services.json` 已被 `.gitignore` 忽略，工作流按下面的顺序准备它：

1. 配置了 `GOOGLE_SERVICES_JSON` secret —— 用它生成 `app/google-services.json`；
2. 没有配置 —— 使用仓库自带的占位配置 `app/google-services.json.ci`。

占位配置只是让项目能编译通过、应用能以纯本地模式（Room 数据库）运行；
用它打出的 APK 里，「使用 Google 账号继续」登录和云端同步不可用。
里面的 API key 和 client id 都是**明显的假值**（`PLACEHOLDER-NOT-A-REAL-...`），不含任何真实凭据，
所以 GitHub 的密钥扫描不会（也不应该）对它报警。

要启用云同步：到 [Firebase 控制台](https://console.firebase.google.com/) 新建项目，
添加包名为 `com.minecraftmc22.expenses` 的 Android 应用（调试版包名是 `com.minecraftmc22.expenses.dev`），
把下载到的 `google-services.json` 内容整份存成 `GOOGLE_SERVICES_JSON` secret 即可。

### Google 登录失败怎么查

从 0.3 起，登录失败不再被静默吞掉 —— 会弹窗显示错误码，例如「Google 登录失败（10）」。
配置不全也不会再让编译失败，只会在点击登录时告诉你原因。

| 提示 | 含义 | 处理 |
| --- | --- | --- |
| 「还没有配置 Google 登录」 | `google-services.json` 里 `oauth_client` 是空数组 | 到 Firebase → Authentication 启用 **Google** 登录方式，然后**重新下载** `google-services.json` |
| `10` | 签名证书或 OAuth 客户端 ID 与项目不匹配 | 把**实际签名所用** keystore 的 SHA-1 登记到 Firebase 项目 |
| `12500` | Google 登录未启用 | 到 Firebase 控制台启用 Google 登录方式 |
| `12501` | 用户取消了账号选择 | 正常现象，重新点一次 |
| `7` | 网络错误 | 检查网络或代理 |
| `ERROR_` 开头 | Firebase 侧拒绝 | 多半是项目配置或 API key 问题，先确认没有在用占位配置 |
| 一句话英文提示 | Google 没有返回 ID token | `default_web_client_id` 不属于本应用的项目，或签名证书未登记 |

选完账号后如果是 **Google 自己**弹出「Access blocked / access_denied / 403」，那是 OAuth 同意屏幕还在
**「测试」**状态：到 Google Cloud Console → **「OAuth 同意屏幕」** 发布应用，或把测试用的 Google 账号
加进「测试用户」。

> **反查已登记的 SHA-1**：`google-services.json` 里每个 `client_type: 1` 条目的 `certificate_hash`
> 就是去掉冒号的 SHA-1。把它和 `Keystore fingerprint` 打出来的 `SHA1:` 对比，就能立刻确认签名密钥
> 有没有登记 —— 不相等就是错误码 `10` 的根因。
>
> 另外 `client_type: 3` 的那一条就是 `default_web_client_id` 的来源；如果 `oauth_client` 里没有
> `client_type: 3`，说明 Google 登录方式还没启用。

> **用 Actions 打的 APK 默认登录不了**：没配置 `GOOGLE_SERVICES_JSON` secret 时用的是仓库里的占位配置
> `app/google-services.json.ci`，它不对应任何真实项目，登录必然失败。这不是代码问题。

要让登录真正可用，按顺序做：

> **控制台是中文界面时**：`Authentication` 就是左侧「构建」分组下的**「身份验证」**
>（和 Firestore、Storage 并列），里面的 `Sign-in method` 是**「登录方法」**；
> `Project settings → Your apps` 是**「项目设置 → 您的应用」**。
> 不想找菜单就直接用链接（把 `<项目ID>` 换成你的项目 ID）：
>
> - 启用 Google 登录：`https://console.firebase.google.com/project/<项目ID>/authentication/providers`
> - 添加 SHA-1 指纹 / 下载 `google-services.json`：`https://console.firebase.google.com/project/<项目ID>/settings/general`

1. 新建 Firebase 项目；
2. 添加 Android 应用，包名 `com.minecraftmc22.expenses`（调试包名 `com.minecraftmc22.expenses.dev` 也加一个）；
3. 在 **Authentication → Sign-in method** 里启用 **Google**；
4. **登记签名证书的 SHA-1**（最容易漏的一步）：
   - **最省事**：Actions → **Keystore fingerprint** → *Run workflow*，日志里会直接打出
     `SHA1:` / `SHA256:`（工作流见 [`.github/workflows/keystore-fingerprint.yml`](.github/workflows/keystore-fingerprint.yml)）
   - 手上有 APK 文件：`keytool -printcert -jarfile app-prod-release.apk`
   - 手上有 keystore 文件：`keytool -list -v -keystore release.jks -alias <你的别名>`
   - Windows 上 `keytool` 不在 PATH 时用全路径，例如
     `"C:\Program Files\Java\jre1.8.0_421\bin\keytool.exe" -printcert -jarfile app-prod-release.apk`
5. 下载新的 `google-services.json`，整份存成 `GOOGLE_SERVICES_JSON` secret；
6. 重新跑一次 Actions，用新 APK 测试。

> ℹ️ **Google 登录校验用的是 SHA-1（40 位十六进制），不能用 SHA-256 代替，也无法由 SHA-256 推算出来。**
> SHA-256 是可选的附加指纹。
>
> 配了签名 Secret 之后，**调试包也用同一把 release keystore 签名**，所以 `app-prod-release.apk` 和
> `app-prod-debug.apk` 指纹相同，注册一次两个都能用 Google 登录。没配签名 Secret 时调试包由
> GitHub runner 自己的调试密钥签名，那个指纹无法登记，登录不了。
>
> 另外：**添加 SHA-1 之后不需要重新下载 `google-services.json`**（指纹不存在于该文件里）；
> 但**启用 Google 登录方式之后需要**重新下载，因为 `oauth_client` 会新增内容。

换了签名密钥（例如从调试密钥换到正式密钥）就要重新登记 SHA-1，否则错误码会再次变成 `10`。


## 本地构建

需要 JDK 8 与 Android SDK（`platforms;android-29`、`build-tools;29.0.2`）。

```bash
./gradlew assembleProdRelease
```

签名用的四个属性可以写在 `~/.gradle/gradle.properties` 里（不写就产出未签名 APK）：

```properties
EXPENSES_STORE_FILE=/absolute/path/to/release.jks
EXPENSES_STORE_PASSWORD=你的密码
EXPENSES_KEY_ALIAS=你的别名
EXPENSES_KEY_PASSWORD=你的密码
```

## 联系我

本分支由 [Minecraftmc22](https://github.com/minecraftmc22) 维护，
原版由 [nominalista](https://github.com/nominalista) 开发。
有任何问题或建议，欢迎发邮件到 [i.am.minecraftmc22@gmail.com](mailto:i.am.minecraftmc22@gmail.com)。

## 版权

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
