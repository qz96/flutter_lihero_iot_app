# flutter_lihero_iot_app

基于 [ThingsBoard CE](https://github.com/thingsboard/flutter_thingsboard_app) 官方 Flutter 移动端改造的物联网 App 基线版本。

> LIHERO 物联网平台移动端 —— 用于连接 ThingsBoard 服务端，查看设备、遥测、告警、仪表板等。

## ⚠️ 关于本仓库

本仓库是一个**干净的单一提交基线（baseline）**：

- 只保留一条 `baseline` 提交，不含 ThingsBoard 上游的完整 Git 历史。
- 标签 `baseline-v1.0` 指向该提交。
- 便于以此作为私有/内部项目的起点，避免暴露上游 commit 记录。

如需合入上游新特性，可另加 `thingsboard` 官方仓库为 remote，再做 cherry-pick / merge。

## 仓库结构说明

| 路径 | 说明 |
| --- | --- |
| `lib/` | Flutter 应用源码 |
| `.vendor/` | **本地 vendored 依赖**（见下文） |
| `configs.json.example` | 配置模板（复制为 `configs.json` 后填写） |
| `configs.json` | **本地配置，已被 `.gitignore` 排除，不会进仓库** |

## 构建环境要求

- Flutter **3.29.0** / Dart 3.7.0
- Android SDK Platform **34** + Build-Tools **34.0.0**
- JDK 17+（Android Studio 自带 JBR 即可）
- 环境变量：`JAVA_HOME`（Windows 风格路径）、`ANDROID_HOME`

## 构建步骤（Android release APK）

```bash
# 1. 安装依赖
flutter pub get

# 2. 准备配置：复制模板并填入你的服务端地址/密钥
cp configs.json.example configs.json
#   编辑 configs.json：thingsboardApiEndpoint、thingsboardAndroidAppSecret 等

# 3. 构建
flutter build apk --release --no-tree-shake-icons --dart-define-from-file configs.json
```

产物路径：`build/app/outputs/flutter-apk/app-release.apk`

## 关于 vendored 依赖（`.vendor/`）

由于构建环境无法直接访问 GitHub，以下两个官方依赖已从源码 vendored 到本地 `.vendor/`，并在 `pubspec.yaml` 中以 `path:` 引用：

- `plugin_wifi_connect`（原 `thingsboard/flutter_wifi_connect`）
- `esp_provisioning_softap`（原 `thingsboard/esp_provisioning_softap`，已移除旧 v1 嵌入 API `registerWith` 以兼容 Flutter 3.29）

### 如何换回官方 GitHub 依赖（可选）

若你的构建环境可访问 GitHub，可还原为官方源：

1. 编辑 `pubspec.yaml`，将两个依赖改回：
   ```yaml
   plugin_wifi_connect:
     git:
       url: https://github.com/thingsboard/flutter_wifi_connect.git
       ref: master
   esp_provisioning_softap:
     git:
       url: https://github.com/thingsboard/esp_provisioning_softap.git
       ref: master
   ```
2. 执行 `flutter pub get`
3. 如需保留离线构建能力，保留本地 `.vendor/` 目录即可（不影响使用官方依赖）。

## 应用语言行为

App 显示语言跟随**服务端保存的用户偏好**（`user.additionalInfo.lang`，如 `zh_CN` / `en_US`）：

- 已登录：使用服务端保存的语言。
- 未登录：回退到设备系统语言。

在「设置 → 语言」中切换后会写入服务端并即时生效。

## 分支与标签

- `main`：默认分支，基线代码。
- `baseline-v1.0`：基线发布标签。

## 版权与许可

基于 ThingsBoard CE（Apache License 2.0）改造，遵循其原始许可。
