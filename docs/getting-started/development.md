# 开发与构建

## 环境

| 项目      | 当前要求                                 |
|---------|--------------------------------------|
| JDK     | 21                                   |
| Android | minSdk 31、targetSdk 36、compileSdk 36 |
| Gradle  | 使用仓库内 Wrapper                        |
| 模块      | `app` + domain/core/runtime 基础模块       |

依赖版本以 `gradle/libs.versions.toml` 为唯一来源，不在文档复制完整版本表。

## 常用命令

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :domain:test :runtime:session:test
.\gradlew.bat :core:android:testDebugUnitTest :core:ui:testDebugUnitTest
.\gradlew.bat verifyModuleBoundaries
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleDebugAndroidTest
git diff --check
```

有可用设备时再执行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Release 签名

签名值按“环境变量优先、本地 `keystore.properties` 次之”读取。四项必须同时存在：store file、store
password、key alias、key password。仓库不会在凭据缺失时使用 Debug 签名生成 Release 包。

## 本地数据兼容性

当前重构以清除应用数据后的全新安装为验收基线，不承诺旧
SharedPreferences、旧信封、旧数据库和旧备份格式的迁移。数据库打不开时不得自动删除用户数据。

