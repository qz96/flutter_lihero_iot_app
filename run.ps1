# run.ps1 —— 一键启动模拟器并运行 LIHERO 物联网 App
# 用法: powershell -ExecutionPolicy Bypass -File .\run.ps1
# 可选参数:
#   -Avd <名称>   指定模拟器（默认 lihero_pixel）
#   -NoHot        关闭自动热重载（改为手动按 r）

param(
    [string]$Avd = "lihero_pixel",
    [switch]$NoHot
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Write-Step($msg) { Write-Host $msg -ForegroundColor Cyan }
function Write-Warn($msg) { Write-Host $msg -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host $msg -ForegroundColor Red }

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  LIHERO 物联网 App 一键启动" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# ---------- 0. 环境检查 ----------
Write-Step "[0/4] 检查环境 ..."

if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) {
    Write-Err "未找到 flutter 命令。"
    Write-Warn "请新开一个终端（环境变量为用户级，需重新加载），或执行："
    Write-Warn '  $env:Path = "D:\rtu_build_tools\flutter\bin;" + $env:Path'
    exit 1
}
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Err "未找到 adb 命令，请确认 Android SDK platform-tools 已加入 PATH。"
    exit 1
}

Write-Host "  flutter : $((Get-Command flutter).Source)"
Write-Host "  JDK     : $env:JAVA_HOME"

# ---------- 1. 配置文件 ----------
Write-Step "[1/4] 检查 configs.json ..."

if (-not (Test-Path "configs.json")) {
    Copy-Item "configs.json.example" "configs.json"
    Write-Warn "已从模板生成 configs.json。"
    Write-Warn "请填写 thingsboardAndroidAppSecret 等配置后重新运行本脚本。"
    exit 1
}

$config = Get-Content "configs.json" -Raw | ConvertFrom-Json
if ($config.thingsboardAndroidAppSecret -match "REPLACE_WITH") {
    Write-Warn "警告：configs.json 中 thingsboardAndroidAppSecret 仍为占位符，"
    Write-Warn "      OAuth2 方式登录将失败。是否继续？(Y/N)"
    if ((Read-Host) -notmatch "^[Yy]") { exit 1 }
}
Write-Host "  服务端地址: $($config.thingsboardApiEndpoint)"

# ---------- 2. 依赖 ----------
Write-Step "[2/4] 同步依赖 (flutter pub get) ..."
flutter pub get

# ---------- 3. 启动模拟器 ----------
Write-Step "[3/4] 启动模拟器 $Avd ..."

$booted = adb devices | Select-String "emulator-.*device$"
if ($booted) {
    Write-Host "  检测到模拟器已在运行，跳过启动。"
} else {
    $avds = (flutter emulators 2>&1 | Select-String "^\s*${Avd}\s")
    if (-not $avds) {
        Write-Err "未找到名为 '$Avd' 的模拟器。可用列表："
        flutter emulators
        exit 1
    }
    flutter emulators --launch $Avd

    Write-Host "  等待模拟器就绪（首次冷启动可能需 30-60 秒）..." -NoNewline
    adb wait-for-device
    # 等待系统启动完成
    do {
        Start-Sleep -Seconds 2
        Write-Host "." -NoNewline
        $isBooted = (adb shell getprop sys.boot_completed 2>$null) -match "1"
    } while (-not $isBooted)
    Write-Host " 就绪"
}

# ---------- 4. 运行 App ----------
Write-Step "[4/4] 运行 App（调试模式）..."
Write-Host ""
if ($NoHot) {
    Write-Host "  模式: 手动热重载" -ForegroundColor DarkGray
} else {
    Write-Host "  模式: 自动热重载（保存文件即生效）" -ForegroundColor DarkGray
}
Write-Host "  快捷键:  r = 手动热重载   R = 热重启   h = 全部帮助   q = 退出" -ForegroundColor DarkGray
Write-Host ""

$runArgs = @("run", "--dart-define-from-file", "configs.json")
if (-not $NoHot) { $runArgs += "--hot" }

flutter @runArgs
