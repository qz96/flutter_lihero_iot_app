---
name: lhble-project-guide
description: LHBle Android项目指南 - 提供项目结构、模块说明、工具类使用、依赖库和开发规范。用于新项目上手、代码复用、查找特定功能实现、了解BLE采样盒设备通信架构。当需要理解项目组织、复用现有工具、或开发新功能时使用。
---

# LHBle 采样盒项目指南

## 项目概述

**LHBle** 是一个Android蓝牙采样盒控制应用，用于管理和控制多种类型的采样设备（DX系列、CYX系列、RTU等）。

**技术栈**:
- Android (minSdk: 23, targetSdk: 29)
- Java 8
- BLE蓝牙通信
- Retrofit + RxJava2网络框架
- FastJSON数据处理
- Excel导出功能

## 项目结构

```
APP_SampleBox/
├── app/                          # 主应用模块
│   ├── src/main/java/com/lh/lhble/
│   │   ├── Entity/               # 数据实体类 (32个)
│   │   ├── UI/                   # Activity和自定义View
│   │   ├── Utils/                # 工具类 (16个)
│   │   ├── adapter/              # RecyclerView适配器 (10个)
│   │   ├── base/                 # 基础类和常量
│   │   ├── network/              # 网络请求封装
│   │   ├── listener/             # 回调接口
│   │   └── google/zxing/         # 二维码扫描
│   └── libs/                     # 本地AAR/JAR库
└── wiseKit/                      # 通用UI组件库模块
    └── src/main/java/com/wise/wisekit/
        ├── activity/             # 基础Activity
        ├── dialog/               # 对话框组件
        ├── widget/               # 自定义控件
        └── utils/                # 通用工具类
```

## 核心模块说明

### 1. Entity (数据实体层)

**位置**: `app/src/main/java/com/lh/lhble/Entity/`

**主要实体类**:

| 实体类 | 用途 | 说明 |
|--------|------|------|
| `SampleBox.java` | 采样盒主实体 | 11.8KB，核心数据结构 |
| `DXSampleBox.java` | DX系列采样盒 | 6.3KB |
| `DX2SampleBox.java` | DX2系列采样盒 | 8.2KB |
| `GGCYBox.java` | 高温采样盒 | 5.8KB |
| `TransportBox.java` | 运输盒 | 3.2KB |
| `SampleBottle.java` | 采样瓶 | 1.5KB |
| `T_BottleState.java` | 瓶子状态 | 4.6KB |
| `User.java` | 用户信息 | 5.9KB |
| `Project.java` | 项目信息 | 1.6KB |
| `ModBusParam.java` | ModBus参数 | 10.7KB，最大实体类 |
| `RTUInfo.java` | RTU设备信息 | 4.7KB |
| `LhBleBean.java` | BLE设备数据 | 2.0KB |
| `AppUpgrade.java` | APP升级信息 | 3.4KB |

**使用示例**:
```java
// 创建采样盒对象
SampleBox sampleBox = new SampleBox();
sampleBox.setBoxId("BOX001");
sampleBox.setStatus(1);

// ModBus参数配置
ModBusParam modBusParam = new ModBusParam();
modBusParam.setAddress(0x01);
modBusParam.setDataLength(8);
```

### 2. UI层 (Activity)

**位置**: `app/src/main/java/com/lh/lhble/UI/`

**主要Activity分类**:

#### 启动和登录
- `LaunchActivity.java` - 启动页
- `LoginActivity.java` - 登录界面
- `SetActivity.java` - 设置页面

#### BLE设备管理
- `ScanBleActivity.java` - 扫描BLE设备 (54.5KB)
- `BluetoothControlActivity.java` - 蓝牙控制 (35.1KB)
- `BlueOTAActivity.java` - BLE固件升级 (43.9KB)
- `DYBleActivity.java` - 多源BLE设备 (30.2KB)

#### 采样盒控制 (按设备类型)
- **DX系列**:
  - `LiherDXCYXActivity.java` - DX基础版 (91.8KB)
  - `LiherDXCYX2Activity.java` - DX二代 (120.5KB)
  - `LiherDXCYX3Activity.java` - DX三代 (124.4KB)

- **CYX系列**:
  - `LiherCYXActivity.java` - CYX基础版 (93.4KB)
  - `LiherCYX4Activity.java` ~ `LiherCYX7Activity.java` - CYX 4-7代 (100-173KB)
  - `LiherCYXParamSetActivity.java` - CYX参数设置 (62.4KB)
  - `LiherCYXRTUActivity.java` - CYX RTU模式 (119.6KB)

- **其他类型**:
  - `LiherRTUActivity.java` - RTU设备 (119.8KB)
  - `LiherGGCYActivity.java` - 高温采样 (60.1KB)
  - `LiherSWJ1Activity.java` - 水位计 (96.7KB)
  - `LiherYSXActivity.java` - 雨水箱 (48.3KB)
  - `LiherZKYActivity.java` - 控制器 (112.3KB)

#### 订单和数据管理
- `LhBleListOrderActivity.java` - 订单列表 (44.6KB)
- `LiherActivity.java` - 主功能页 (45.3KB)
- `LiherAdvancedBottleActivity.java` - 高级瓶管理 (45.3KB)
- `LiherShortBottleActivity.java` - 短瓶管理 (39.3KB)

**Activity基类使用**:
```java
public class YourActivity extends WiseBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.your_layout);
        
        // 使用ToastUtils显示提示
        ToastUtils.showSuccess("操作成功");
        
        // 使用SPUtils存储数据
        SPUtils.putString("key", "value");
    }
}
```

### 3. Utils (工具类层)

**位置**: `app/src/main/java/com/lh/lhble/Utils/`

**核心工具类**:

#### 数据处理
- **StringUtils.java** (56.3KB) - 字符串处理工具集
  ```java
  // 常用方法
  StringUtils.isEmpty(str);
  StringUtils.toHexString(byteArray);
  StringUtils.hexStringToBytes(hexStr);
  StringUtils.formatDouble(value, 2); // 保留2位小数
  ```

- **DateUtil.java** (10.8KB) - 日期时间处理
  ```java
  DateUtil.getCurrentTime(); // 获取当前时间字符串
  DateUtil.timeStamp2Date(timestamp, format); // 时间戳转日期
  DateUtil.dateDiff(startTime, endTime, format); // 计算时间差
  ```

- **DoubleUtils.java** (5.0KB) - 浮点数精度处理
  ```java
  DoubleUtils.add(v1, v2); // 精确加法
  DoubleUtils.mul(v1, v2); // 精确乘法
  DoubleUtils.round(value, scale); // 四舍五入
  ```

#### 文件和Excel
- **ExcelUtils.java** (8.1KB) - Excel文件生成
  ```java
  // 创建Excel并写入数据
  WritableWorkbook workbook = ExcelUtils.createExcel(filePath, sheetName);
  ExcelUtils.writeData(workbook, dataList, headers);
  ExcelUtils.closeWorkbook(workbook);
  ```

- **FileUtil.java** (9.7KB) - 文件操作
  ```java
  FileUtil.createFile(path); // 创建文件
  FileUtil.deleteFile(file); // 删除文件
  FileUtil.readTxtFile(file); // 读取文本文件
  FileUtil.getFileSize(file); // 获取文件大小
  ```

#### 协议和加密
- **ProtocolUtil.java** (13.9KB) - 通信协议解析
  ```java
  // 解析BLE数据包
  ProtocolUtil.parseProtocol(data);
  ProtocolUtil.buildCommand(cmd, params); // 构建命令
  ```

- **CRC16ModBusUtils.java** (3.9KB) - CRC校验
  ```java
  CRC16ModBusUtils.calculateCRC(data); // 计算CRC16
  CRC16ModBusUtils.verifyCRC(data, crc); // 验证CRC
  ```

- **AESUtil.java** (2.8KB) - AES加密解密
  ```java
  AESUtil.encrypt(key, plaintext); // 加密
  AESUtil.decrypt(key, ciphertext); // 解密
  ```

#### 系统工具
- **AppUtils.java** (21.8KB) - APP信息工具
  ```java
  AppUtils.getVerCode(context); // 获取版本号
  AppUtils.getVerName(context); // 获取版本名称
  AppUtils.installApk(context, file); // 安装APK
  AppUtils.isServiceRunning(context, className); // 检查服务状态
  ```

- **ScreenUtils.java** (3.5KB) - 屏幕适配
  ```java
  ScreenUtils.getScreenWidth(context); // 获取屏幕宽度
  ScreenUtils.getScreenHeight(context); // 获取屏幕高度
  ScreenUtils.dp2px(context, dp); // DP转PX
  ```

- **SharedPreferencesUtils.java** (7.2KB) - 偏好设置封装
  ```java
  SharedPreferencesUtils.putString(context, key, value);
  SharedPreferencesUtils.getString(context, key, defaultValue);
  SharedPreferencesUtils.putInt(context, key, value);
  SharedPreferencesUtils.getBoolean(context, key, defaultValue);
  ```

#### 其他工具
- **RxTimerUtil.java** (2.9KB) - RxJava定时器
  ```java
  RxTimerUtil.delay(1000, () -> {
      // 延迟1秒执行
  });
  
  RxTimerUtil.interval(1000, time -> {
      // 每隔1秒执行
  });
  ```

- **BitmpCover.java** (8.5KB) - 图片压缩转换
  ```java
  BitmpCover.compressBitmap(bitmap, quality);
  BitmpCover.bitmapToBase64(bitmap);
  ```

- **DlgTime.java** (2.0KB) - 时间选择对话框
- **UmengHelper.java** (2.0KB) - 友盟统计集成
- **Utilty.java** (5.5KB) - 通用辅助方法

### 4. Adapter (适配器层)

**位置**: `app/src/main/java/com/lh/lhble/adapter/`

**适配器列表**:

| 适配器 | 用途 | 数据源 |
|--------|------|--------|
| `SampleBottleAdapter.java` | 采样瓶列表 | SampleBottle |
| `SampleBottle1Adapter.java` | 采样瓶列表变体 | SampleBottle |
| `YSXSampleBottleAdapter.java` | 雨水箱采样瓶 | SampleBottle |
| `BoxSamplePlanAdapter.java` | 采样计划列表 | BoxSamplePlan |
| `SamplePlanAdapter.java` | 采样计划 | SamplePlan |
| `DbParamDataAdapter.java` | 数据库参数 | DbParamData |
| `LhBleOrderDevAdapter.java` | BLE订单设备 | LhBleBean |
| `UpgradeLhBleDevAdapter.java` | BLE升级设备 | LhBleBean |
| `JsonObjectSelecterAdapter.java` | JSON对象选择器 | JsonObject |
| `JsonObjectsSelecterAdapter.java` | JSON数组选择器 | JsonArray |

**适配器使用模板**:
```java
// 创建适配器
SampleBottleAdapter adapter = new SampleBottleAdapter(context, bottleList);

// 设置点击事件
adapter.setOnItemClickListener((position, item) -> {
    // 处理点击
    SampleBottle bottle = bottleList.get(position);
    // ...
});

// 绑定到RecyclerView
recyclerView.setLayoutManager(new LinearLayoutManager(context));
recyclerView.setAdapter(adapter);

// 更新数据
adapter.notifyDataSetChanged();
```

### 5. Network (网络层)

**位置**: `app/src/main/java/com/lh/lhble/network/`

**网络架构**: Retrofit2 + OkHttp3 + RxJava2

**核心类**:

- **RetrofitUtils.java** (7.5KB) - Retrofit单例封装
  ```java
  // 获取API服务
  ApiService apiService = RetrofitUtils.getInstance().create(ApiService.class);
  
  // 发起请求
  apiService.getData(params)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(new MyObserver<ResponseData>() {
          @Override
          public void onSuccess(ResponseData data) {
              // 成功处理
          }
          
          @Override
          public void onError(String error) {
              // 错误处理
          }
      });
  ```

- **FastJsonConverterFactory.java** - FastJSON转换器工厂
- **MyObserver.java** - RxJava观察者基类
- **SslContextFactory.java** (4.2KB) - SSL证书配置

**API定义示例**:
```java
public interface ApiService {
    @POST("api/login")
    Observable<LoginResult> login(@Body User user);
    
    @GET("api/sample/{id}")
    Observable<SampleBox> getSampleBox(@Path("id") String id);
    
    @POST("api/upload")
    Observable<Result> uploadData(@Body List<SampleData> data);
}
```

### 6. Base (基础层)

**位置**: `app/src/main/java/com/lh/lhble/base/`

**核心类**:

- **Constant.java** (55.6KB) - 全局常量定义
  ```java
  // BLE相关常量
  public static final String BLE_UUID_SERVICE = "0000xxxx-0000-1000-8000-00805f9b34fb";
  public static final String BLE_UUID_CHARACTERISTIC = "0000yyyy-0000-1000-8000-00805f9b34fb";
  
  // 设备类型常量
  public static final int DEVICE_TYPE_DX = 1;
  public static final int DEVICE_TYPE_CYX = 2;
  public static final int DEVICE_TYPE_RTU = 3;
  
  // 命令码常量
  public static final byte CMD_READ_DATA = 0x01;
  public static final byte CMD_WRITE_DATA = 0x02;
  ```

- **HJBleApplication.java** (4.1KB) - Application类
  ```java
  // 在Application中初始化
  public class HJBleApplication extends Application {
      @Override
      public void onCreate() {
          super.onCreate();
          // 初始化友盟
          UmengHelper.init(this);
          // 初始化日志
          Logger.init("LHBLE").showThreadInfo(true);
      }
  }
  ```

- **ApService.java** (3.0KB) - 后台服务

### 7. wiseKit (通用组件库)

**位置**: `wiseKit/src/main/java/com/wise/wisekit/`

**组件分类**:

#### Utils
- **ToastUtils.java** - Toast提示
  ```java
  ToastUtils.showSuccess("成功");
  ToastUtils.showError("失败");
  ToastUtils.showWarning("警告");
  ToastUtils.showInfo("提示");
  ```

- **SPUtils.java** - SharedPreferences封装
  ```java
  SPUtils.putString("key", "value");
  SPUtils.getString("key", "default");
  SPUtils.putBoolean("key", true);
  SPUtils.remove("key");
  SPUtils.clear();
  ```

- **DateUtils.java** (11.3KB) - 日期工具

#### Widget (自定义控件)
- 进度条、圆形按钮、自定义TextView等8个控件
- 位于 `wiseKit/src/main/java/com/wise/wisekit/widget/`

#### Dialog
- 通用对话框组件

## 依赖库清单

### 核心依赖
```gradle
// AndroidX
implementation 'androidx.appcompat:appcompat:1.2.0'
implementation 'androidx.constraintlayout:constraintlayout:1.1.3'

// 日志
implementation 'com.orhanobut:logger:2.2.0'

// 权限管理
implementation 'pub.devrel:easypermissions:3.0.0'

// 状态栏兼容
implementation 'com.githang:status-bar-compat:0.7'
```

### 网络框架
```gradle
// Retrofit2
implementation 'com.squareup.retrofit2:retrofit:2.3.0'
implementation 'com.squareup.retrofit2:converter-scalars:2.1.0'
implementation 'com.squareup.retrofit2:converter-gson:2.1.0'
implementation 'com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0'

// OkHttp
implementation 'com.squareup.okhttp3:logging-interceptor:3.3.1'
implementation 'com.zhy:okhttputils:2.6.2'

// 数据解析
implementation 'com.alibaba:fastjson:1.1.70.android'

// RxJava2
implementation 'io.reactivex.rxjava2:rxjava:2.0.7'
implementation 'io.reactivex.rxjava2:rxandroid:2.0.1'
```

### UI组件
```gradle
// 时间选择器
implementation 'com.contrarywind:Android-PickerView:4.1.6'
implementation 'com.jzxiang.pickerview:TimePickerDialog:1.0.1'

// 二维码
implementation 'com.google.zxing:core:3.3.0'
```

### 数据统计
```gradle
// 友盟统计
implementation 'com.umeng.umsdk:common:9.4.7'
implementation 'com.umeng.umsdk:asms:1.4.1'
implementation 'com.umeng.umsdk:apm:1.5.2'
implementation 'com.umeng.umsdk:abtest:1.0.0'
```

### 文件处理
```gradle
// Excel生成
implementation 'net.sourceforge.jexcelapi:jxl:2.6.12'
```

### 本地库 (libs/)
- `libble-0.5.aar` - BLE通信库
- `libdfu-1.5.aar` - DFU固件升级库
- `libfastdfu-0.5.aar` - 快速DFU库
- `libcomx-0.5.jar` - 通信扩展库
- `app_update_component-release.aar` - APP更新组件
- `tbs_sdk_thirdapp_v2.5.0.1037...jar` - 腾讯浏览服务

## BLE通信架构

### BLE设备类型
1. **DX系列** - DX、DX2、DX3采样盒
2. **CYX系列** - CYX、CYX4-CYX7采样仪
3. **RTU系列** - RTU远程终端单元
4. **专用类型** - GGCY(高温)、YSX(雨水箱)、SWJ(水位计)、ZKY(控制器)

### 通信流程
```
1. 扫描设备 (ScanBleActivity)
   ↓
2. 连接设备 (BluetoothLe / WiseBluetoothLe)
   ↓
3. 发现服务和特征值
   ↓
4. 发送命令 (ProtocolUtil.buildCommand)
   ↓
5. 接收响应 (WiseCharacteristic回调)
   ↓
6. 解析数据 (ProtocolUtil.parseProtocol)
   ↓
7. 更新UI
```

### 关键类
- `wise.ble.BluetoothLe.java` - BLE核心类
- `wise.ble.WiseBluetoothLe.java` - BLE封装类
- `wise.ble.WiseCharacteristic.java` - 特征值封装
- `wise.ble.scan.*` - 扫描相关类

## 开发规范和最佳实践

### 1. 命名规范
- **Activity**: 以`Activity`结尾，如`LiherCYXActivity`
- **Entity**: 驼峰命名，如`SampleBox`、`DXSampleBox`
- **Utils**: 以`Utils`或`Util`结尾，如`DateUtil`、`ExcelUtils`
- **Adapter**: 以`Adapter`结尾，如`SampleBottleAdapter`
- **常量**: 全大写+下划线，如`DEVICE_TYPE_DX`

### 2. 代码复用
```java
// ✅ 使用现有工具类
String hex = StringUtils.toHexString(data);
double result = DoubleUtils.add(val1, val2);
ToastUtils.showSuccess("操作成功");

// ❌ 避免重复实现
// 不要自己写hex转换、浮点运算、Toast显示
```

### 3. 网络请求
```java
// ✅ 标准网络请求写法
apiService.getData(params)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(new MyObserver<ResponseData>() {
        @Override
        public void onSuccess(ResponseData data) {
            // 处理成功
        }
        
        @Override
        public void onError(String error) {
            ToastUtils.showError(error);
        }
    });
```

### 4. 数据存储
```java
// ✅ 使用SharedPreferencesUtils
SharedPreferencesUtils.putString(context, "user_id", userId);
String userId = SharedPreferencesUtils.getString(context, "user_id", "");

// ✅ 或使用SPUtils (wiseKit)
SPUtils.putString("user_id", userId);
String userId = SPUtils.getString("user_id", "");
```

### 5. Excel导出
```java
// ✅ 标准Excel导出流程
try {
    String filePath = FileUtil.getSDPath() + "/export/data.xls";
    FileUtil.createFile(filePath);
    
    WritableWorkbook workbook = ExcelUtils.createExcel(filePath, "采样数据");
    
    // 写入表头
    String[] headers = {"编号", "时间", "数值"};
    ExcelUtils.writeHeaders(workbook, headers);
    
    // 写入数据
    List<String[]> dataList = getDataList();
    ExcelUtils.writeData(workbook, dataList);
    
    ExcelUtils.closeWorkbook(workbook);
    ToastUtils.showSuccess("导出成功");
} catch (Exception e) {
    e.printStackTrace();
    ToastUtils.showError("导出失败: " + e.getMessage());
}
```

### 6. 权限处理
```java
// ✅ 使用EasyPermissions
@AfterPermissionGranted(REQUEST_CODE)
private void onPermissionGranted() {
    // 权限获取成功，执行操作
    startScan();
}

private void requestPermissions() {
    String[] perms = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    };
    EasyPermissions.requestPermissions(this, "需要权限", REQUEST_CODE, perms);
}
```

## 常见任务指引

### 添加新设备类型
1. 在`Entity/`创建数据实体类
2. 在`UI/`创建对应Activity（继承现有基类）
3. 在`Constant.java`添加设备类型常量
4. 在`ProtocolUtil.java`添加协议解析逻辑
5. 在`adapter/`创建适配器（如需要）

### 添加新API接口
1. 在`network/api/`定义接口方法
2. 创建对应的Entity实体类
3. 使用RetrofitUtils调用接口
4. 处理响应和错误

### 添加新工具方法
1. 优先在现有Utils类中添加（如StringUtils、DateUtil）
2. 如果功能独立，创建新的Utils类
3. 确保方法是static的
4. 添加清晰的注释说明

### 修改UI样式
1. 主题色定义在`res/values/colors.xml`
2. 背景shape在`res/drawable/`目录
3. 布局文件在`res/layout/`目录
4. 遵循现有设计风格（圆角、间距等）

## 调试技巧

### 日志输出
```java
// 使用Logger库
Logger.d("调试信息");
Logger.e("错误信息");
Logger.json(jsonString); // 格式化JSON
Logger.xml(xmlString); // 格式化XML
```

### BLE调试
```java
// 启用BLE日志
Logger.init("BLE_DEBUG").showThreadInfo(true);

// 打印十六进制数据
Logger.d("发送数据: " + StringUtils.toHexString(sendData));
Logger.d("接收数据: " + StringUtils.toHexString(receiveData));
```

## 注意事项

⚠️ **重要提醒**:

1. **编译环境**: 
   - Gradle Plugin 3.4.2
   - Java 8
   - JVM堆内存: `-Xmx1536m`

2. **AndroidX兼容**: 
   - 已启用AndroidX
   - Jetifier自动转换第三方库

3. **签名配置**: 
   - Debug和Release使用相同签名
   - 签名文件: `app/LHBLE.jks`

4. **ABI支持**: 
   - armeabi, armeabi-v7a, arm64-v8a, x86, mips

5. **版本管理**: 
   - versionCode: 118
   - versionName: "5.1.8"

6. **混淆配置**: 
   - Release未启用混淆 (`minifyEnabled false`)
   - 可在`proguard-rules.pro`配置规则

## 相关资源

- **BLE文档**: 参考`wise.ble`包下的蓝牙通信实现
- **协议文档**: 查看`ProtocolUtil.java`中的协议解析逻辑
- **UI组件**: wiseKit模块提供通用UI组件
- **示例代码**: 各Activity中包含具体使用示例

## 快速开始

```bash
# 克隆项目后
cd APP_SampleBox

# 同步Gradle依赖
./gradlew build

# 清理构建
./gradlew clean

# 运行测试
./gradlew test
```

---

**最后更新**: 2026-06-22
**维护者**: 项目开发团队
