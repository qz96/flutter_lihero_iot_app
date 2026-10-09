# LHADCP设备接入说明

## 概述
为蓝牙地址16开头的手持式多普勒流量计(LHADCP)设备创建了完整的主界面接入方案。

## 创建的文件

### 1. Activity类
**文件路径**: `app/src/main/java/com/lh/lhble/UI/LiherLHADCPActivity.java`

**主要功能**:
- ✅ 基于LiherSWJ1Activity的标准架构模式
- ✅ BLE通信连接管理
- ✅ Read Detail数据解析（45个字段）
- ✅ 主界面UI更新
- ✅ 6个快捷功能入口

**核心特性**:
```java
// 1. 设备信息展示
- 电池电量（百分比 + 电压，低电红色告警）
- 固件版本号
- 网络信号强度
- 探头水温
- 存储空间
- 设备名称、MN号
- 设备状态（正常/异常）
- 工作模式、运行模式
- 蓝牙MAC地址

// 2. 常用功能入口
- 单点测量
- 历史数据
- 校准
- 参数设置
- 上传设置
- 多线多点测量

// 3. 数据解析
parseDetailData()方法解析Read Detail返回的45个字段：
- 固件版本（6位）
- 电池电压（5位，0.01V单位）
- 网络信号强度（2位）
- 水温（浮点数）
- 设备名称（12位）
- 设备MN（12位）
- 设备状态（状态字）
- 运行模式（3位）
- ... 更多字段
```

### 2. 布局文件
**文件路径**: `app/src/main/res/layout/activity_liher_lhadcp.xml`

**布局结构**:
```xml
ScrollView
├── 设备信息区域 (白色圆角卡片)
│   ├── 标题："设备信息"
│   ├── 电池电量
│   ├── 固件版本
│   ├── 网络信号
│   ├── 探头水温
│   ├── 存储空间
│   ├── 设备名称
│   ├── 设备MN
│   ├── 设备状态
│   ├── 工作模式
│   ├── 运行模式
│   ├── 蓝牙地址
│   └── 刷新按钮
│
└── 常用功能区域 (白色圆角卡片)
    ├── 标题："常用功能"
    └── GridLayout (2列3行)
        ├── 单点测量 (绿色)
        ├── 历史数据 (主题色)
        ├── 校准 (蓝色)
        ├── 参数设置 (橙色)
        ├── 上传设置 (粉色)
        └── 多线多点测量 (灰色)
```

**设计特点**:
- 📱 响应式布局，适配不同屏幕
- 🎨 彩色按钮区分功能类型
- 🔄 一键刷新数据
- ⚠️ 低电红色告警（<60%）
- ✅ 状态颜色标识（正常绿色/异常红色）

### 3. 常量定义
**文件路径**: `app/src/main/java/com/lh/lhble/base/Constant.java`

**新增常量**:
```java
// LHADCP手持式多普勒流量计相关常量（蓝牙地址16开头）
public static final int Param_LHADCP_Read_Detail=5401;
public static final int Return_LHADCP_Read_Detail=5402;
public static final int Param_LHADCP_Set_Start=5403;
public static final int Return_LHADCP_Set_Start=5404;
public static final int Param_LHADCP_Set_Stop=5405;
public static final int Return_LHADCP_Set_Stop=5406;
public static final int Param_LHADCP_Read_History=5407;
public static final int Return_LHADCP_Read_History=5408;
public static final int Param_LHADCP_Calibration=5409;
public static final int Return_LHADCP_Calibration=5410;
public static final int Param_LHADCP_Write_Param=5411;
public static final int Return_LHADCP_Write_Param=5412;
```

## 使用方法

### 1. 从扫描页面跳转
```java
// 检测到蓝牙地址16开头的设备
if (deviceMac.startsWith("16")) {
    Intent intent = LiherLHADCPActivity.getInstance(
        this,
        deviceName,  // 设备名称
        deviceMac,   // MAC地址（16开头）
        false        // 是否配置模式
    );
    startActivity(intent);
}
```

### 2. 数据流程
```
Activity启动
    ↓
获取Intent参数（设备名、MAC地址）
    ↓
初始化BLE连接
    ↓
延迟500ms发送"Read Detail"命令
    ↓
接收45个字段的数据
    ↓
parseDetailData()解析
    ↓
updateUI()更新界面
    ↓
显示设备信息和状态
```

### 3. 关键方法

#### 发送命令
```java
sendLHBleData("Read Detail", Constant.Param_LHADCP_Read_Detail);
```

#### 解析数据
```java
private void parseDetailData(String msgContext) {
    String[] details = msgContext.split("#", -1);
    
    // 固件版本
    firmwareVersion = details[1];
    
    // 电池电压
    batteryVoltage = Integer.parseInt(details[10]);
    
    // 网络信号
    signalStrength = Integer.parseInt(details[20]);
    
    // 水温
    waterTemp = Double.parseDouble(details[28]);
    
    // ... 更多字段解析
    
    updateUI();
}
```

#### 计算电量百分比
```java
private int calculateBatteryPercent(int voltageInCentiVolt) {
    double voltage = voltageInCentiVolt / 100.0;
    
    if (voltage <= 9.8) return 0;
    else if (voltage >= 10.6) return 100;
    else return (int) ((voltage - 9.8) / (10.6 - 9.8) * 100);
}
```

## 协议对应关系

### Read Detail返回字段映射

| 序号 | 协议字段 | Java变量 | UI显示 | 格式 |
|-----|---------|---------|--------|------|
| 2 | 固件版本 | firmwareVersion | tvFirmwareVersion | 6位字符串 |
| 11 | 电池电压 | batteryVoltage | tvBatteryInfo | 5位(0.01V) |
| 21 | 信号强度 | signalStrength | tvSignalStrength | 2位(dBm) |
| 29 | 水温 | waterTemp | tvWaterTemp | 浮点数(℃) |
| 3 | 设备名 | deviceName | tvDeviceName | 12位字符串 |
| 5 | 设备MN | deviceMN | tvDeviceMN | 12位字符串 |
| 9 | 设备状态 | deviceStatus | tvDeviceStatus | 状态字 |
| 13 | 运行模式 | runMode | tvRunMode | 3位(0/1/2) |

## 待完善功能

以下功能按钮已预留点击事件，需要后续开发对应页面：

1. **单点测量** (`btn_single_measure`)
   - 创建测量Activity
   - 实时数据显示
   - 测量结果保存

2. **历史数据** (`btn_history_data`)
   - 读取历史数据（Read History=n）
   - 列表展示
   - 数据导出

3. **校准** (`btn_calibration`)
   - 大气压校准（Set Pressure）
   - 其他校准项

4. **参数设置** (`btn_param_settings`)
   - Write指令实现
   - 参数编辑对话框

5. **上传设置** (`btn_upload_settings`)
   - 上传协议配置
   - IP和端口设置

6. **多线多点测量** (`btn_multi_measure`)
   - 复杂测量流程
   - 数据记录

## 技术要点

### 1. 遵循项目标准
- ✅ 继承BaseActivity
- ✅ 使用WiseBluetoothLe进行BLE通信
- ✅ ProtocolUtil协议封装
- ✅ RxTimerUtil定时任务
- ✅ LoadingDialog加载提示

### 2. 错误处理
```java
// 连接断开检测
if (mble == null || !mble.isConnect()) {
    showMsg(2, "蓝牙未连接");
    finish();
    return;
}

// 超时处理
else if (!mble.waitReturn(mReceiveCharact, 15000)) {
    addLogText("响应超时", 1, Constant.Return_BLE_False);
}

// 数据解析异常
try {
    parseDetailData(msgContext);
} catch (Exception e) {
    e.printStackTrace();
    showMsg(2, "数据解析异常：" + e.getMessage());
}
```

### 3. 生命周期管理
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    RxTimerUtil.cancel();  // 取消定时器
    mble.disconnectDevice();  // 断开BLE
    mble.disconnectLocalDevice();  // 断开本地连接
}
```

## 注意事项

⚠️ **重要提醒**:

1. **设备识别**: 仅适用于蓝牙地址16开头的LHADCP设备
2. **协议版本**: 基于《ADCP表头蓝牙通信协议》文档实现
3. **数据存储**: 存储空间字段需根据实际协议补充
4. **TODO标记**: 6个功能按钮需要后续开发对应页面
5. **权限控制**: 可根据需要添加用户权限判断

## 测试建议

1. **连接测试**: 验证BLE连接稳定性
2. **数据解析**: 对比实际设备返回数据验证解析正确性
3. **UI更新**: 检查所有字段显示是否正确
4. **低电告警**: 模拟低电压测试红色告警
5. **刷新功能**: 测试手动刷新数据
6. **异常处理**: 测试断线重连、超时等场景

## 相关文件

- 协议文档: `Source/ADCP表头蓝牙通信协议.md`
- 功能需求: `Source/手持式多普勒流量计LHADCP手机APP功能需求.md`
- Activity: `app/src/main/java/com/lh/lhble/UI/LiherLHADCPActivity.java`
- 布局文件: `app/src/main/res/layout/activity_liher_lhadcp.xml`
- 常量定义: `app/src/main/java/com/lh/lhble/base/Constant.java`

---

**创建时间**: 2026-06-22  
**适用设备**: LHADCP手持式多普勒流量计（蓝牙地址16开头）  
**协议版本**: v1.0
