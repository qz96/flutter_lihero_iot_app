---
name: lhble-device-integration
description: 力合蓝牙设备接入开发指南 - 提供Liher系列Activity的标准开发模式、BLE通信流程、数据解析模板、参数读写规范、UI交互模式和错误处理机制。用于快速接入新力合蓝牙设备、复用现有代码架构、保持代码风格一致。当需要开发新设备控制页面、理解BLE通信协议、或实现设备参数配置时使用。
---

# 力合蓝牙设备接入开发指南

## 概述

本指南基于项目中19个Liher系列Activity（如`LiherSWJ1Activity`、`LiherCYX7Activity`等）的代码实践，总结了接入力合蓝牙设备的标准化开发模式。

**适用场景**:
- 开发新的力合蓝牙设备控制页面
- 接入新型号采样器/传感器设备
- 实现BLE设备参数读取和配置
- 统一设备控制页面的代码风格

## 标准架构模式

### 1. Activity继承结构

```java
public class LiherXXXActivity extends BaseActivity 
    implements CommontClickListener, View.OnClickListener {
    
    private static final String TAG = LiherXXXActivity.class.getSimpleName();
    
    // 设备信息
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_IS_CONFIG = "DEVICE_IS_CONFIG";
    
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean isConfig;
    
    // BLE核心对象
    private WiseBluetoothLe mble;
    private Context context;
    private Handler mHandler;
    
    // 数据接收标识
    private static final int MSG_DATA_CHANGE = 0x11;
    private int currentSendType, currentReceiveType;
}
```

**关键点**:
- ✅ 继承`BaseActivity`（来自wiseKit）
- ✅ 实现`CommontClickListener`（对话框回调）
- ✅ 实现`View.OnClickListener`（点击事件）
- ✅ 定义设备信息传递常量
- ✅ 使用`WiseBluetoothLe`作为BLE核心类

### 2. Intent创建方法

```java
public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
    Intent intent = new Intent(context, LiherXXXActivity.class);
    intent.putExtra(LiherXXXActivity.EXTRAS_DEVICE_NAME, name);
    intent.putExtra(LiherXXXActivity.EXTRAS_DEVICE_ADDRESS, mac);
    intent.putExtra(LiherXXXActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
    return intent;
}
```

**调用示例**:
```java
// 从扫描页面跳转
startActivity(LiherXXXActivity.getInstance(
    this, 
    deviceName,      // 设备名称
    deviceMac,       // MAC地址
    false            // 是否配置模式
));
```

## BLE通信核心流程

### 3. BLE服务定义

在`Constant.java`中定义BLE服务和特征值：

```java
// 数据发送服务
public static final WiseCharacteristic Ble_Data_Send_Service = 
    new WiseCharacteristic(
        "6c696865-726f-aabb-eeff-10736c696865",  // Service UUID
        "6c696865-726f-aabb-eeff-12776c696865"   // Characteristic UUID
    );

// 数据接收服务
public static final WiseCharacteristic Ble_Data_Receive_Service = 
    new WiseCharacteristic(
        "6c696865-726f-aabb-eeff-10736c696865",  // Service UUID
        "6c696865-726f-aabb-eeff-11726c696865"   // Characteristic UUID
    );

// 日志接收服务（可选）
public static final WiseCharacteristic Ble_LOG_Receive_Service = 
    new WiseCharacteristic(
        "6c696865-726f-aabb-eeff-10736c696865",
        "a6ed0104-d344-460a-8075-b9e8ec90d71b"
    );
```

**注意**: 
- ⚠️ 所有力合设备使用相同的Service UUID
- ⚠️ 不同Characteristic UUID区分发送/接收/日志通道

### 4. 初始化流程

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // 1. 获取Intent参数
    final Intent intent = getIntent();
    mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
    mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
    isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);
    
    // 2. 获取BLE实例并设置回调
    mble = WiseBluetoothLe.getInstance(getApplicationContext());
    mble.mBleCallBack = bleCallBack;
    
    // 3. 创建Handler处理消息
    mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_DATA_CHANGE:
                    String strData = (String) msg.obj;
                    analysisMsg(msg.arg1, msg.arg2, msg.obj);
                    break;
            }
            return false;
        }
    });
    
    // 4. 延迟初始化数据（等待连接稳定）
    RxTimerUtil.cancel();
    RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
        @Override
        public void doNext(long number) {
            sendLHBleData("Read Detail", Constant.Param_Read_Detail);
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.init_data));
        }
    });
}
```

**时序说明**:
```
Activity启动 → 获取参数 → 初始化BLE → 设置回调 → 延迟500ms → 发送查询命令
```

### 5. 发送数据标准方法

```java
/**
 * 发送BLE数据
 * @param sendString 发送的指令字符串（明文）
 * @param sendType 发送类型标识（用于区分响应处理）
 */
public void sendLHBleData(String sendString, int sendType) {
    // 1. 验证发送内容
    if (sendString == null || sendString.isEmpty()) {
        showMsg(2, HJBleApplication.getAppContext().getString(R.string.empty_send_msg));
        return;
    }
    
    // 2. 检查连接状态
    if (mble == null || !mble.isConnect()) {
        showMsg(2, HJBleApplication.getAppContext().getString(R.string.ble_close_connect_again));
        finish();
        return;
    }
    
    // 3. 防止Activity销毁时发送
    if (LiherXXXActivity.this.isFinishing()) {
        return;
    }
    
    // 4. 显示加载对话框
    loadingDialog.show();
    
    // 5. 记录日志
    addLogText(sendString, 0, sendType);
    insertLog(mDeviceAddress, sampleId, sendString);
    
    // 6. 协议封装（关键步骤）
    byte[] bytes = ProtocolUtil.getBlueToothMsgForSend(
        HJBleApplication.shareInstance().getDesID(), 
        sendString
    );
    
    if (bytes == null) {
        return;
    }
    
    // 7. 记录当前发送类型
    currentSendType = sendType;
    
    // 8. 异步发送（子线程）
    new Thread(new Runnable() {
        @Override
        public void run() {
            // 发送数据
            if (!mble.sendData(mSendCharact, bytes)) {
                addLogText(HJBleApplication.getAppContext().getString(R.string.send_fail) + "！", 
                    0, Constant.Return_Ble_SendFail);
                currentSendType = 0;
            } 
            // 等待响应（超时15秒）
            else if (!mble.waitReturn(mReceiveCharact, 15*1000)) {
                addLogText(getResources().getString(R.string.ble_rtn_timeout), 
                    1, Constant.Return_BLE_False);
                currentSendType = 0;
            }
        }
    }).start();
}
```

**流程图**:
```
验证输入 → 检查连接 → 协议封装 → 显示Loading → 子线程发送 → 等待响应(15s)
```

### 6. BLE回调处理

```java
WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
    
    @Override
    public void OnWiseBluetoothState(int state) {
        if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
            if (loadingDialog != null) loadingDialog.dismiss();
            addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close), 
                Color.BLACK, 0);
            finish(); // 断开连接后关闭页面
        }
    }
    
    @Override
    public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {
        // 判断接收通道
        if (characteristic.equals(mReceiveCharact)) {
            if (loadingDialog != null) loadingDialog.dismiss();
            
            // 数据解析（根据配置选择解析方式）
            String str = "";
            if (HJBleApplication.shareInstance().isBleHex()) {
                // HEX模式
                str = ConvertData.bytesToHexString(recvData, false);
            } else if (HJBleApplication.shareInstance().isBleConfig() == 0) {
                // 协议模式（常用）
                str = ProtocolUtil.getReturnMsgForReceive(recvData);
            } else {
                // 字符串模式
                str = new String(recvData);
            }
            
            if (!StringUtils.isEmpty(str)) {
                // ========== 关键步骤：处理非正常返回数据 ==========
                // 必须在 switch 之前检查，避免将错误信息当作正常数据处理
                
                if (Constant.ReceiveFalse.equals(str)) {
                    // 情况1：蓝牙返回数据不符合力合协议格式
                    addLogText("蓝牙返回数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                    
                } else if (Constant.ReceiveDataListFalse.equals(str)) {
                    // 情况2：蓝牙返回的分包数据不符合力合协议格式
                    addLogText("蓝牙返回分包数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                    
                } else if (Constant.ReceiveContinue.equals(str)) {
                    // 情况3：设备正在返回数据，需要等待后续数据包
                    addLogText("蓝牙消息返回中", 1, Constant.Return_BLE_Continue);
                    
                } else if (str.startsWith(Constant.SendDataListFalse)) {
                    // 情况4：发送的分包数据有误，提取错误信息
                    String errorMsg = str.substring(Constant.SendDataListFalse.length());
                    addLogText(errorMsg, 1, Constant.Return_Ble_ReceiveFalse);
                    
                } else {
                    // ========== 正常数据：根据发送类型确定接收类型 ==========
                    switch (currentSendType) {
                        case Constant.PARAM_LIHER_ORDER:
                            currentReceiveType = Constant.RETURN_LIHER_OREDER;
                            break;
                        case Constant.Param_Read_Detail:
                            currentReceiveType = Constant.Return_Read_Detail;
                            break;
                        case Constant.Param_Write_SWJ_DEVMN:
                            currentReceiveType = Constant.Return_Write_SWJ_DEVMN;
                            break;
                        // ... 更多类型映射
                        default:
                            currentReceiveType = 0;
                            return;
                    }
                    
                    // 通过Handler转发到主线程处理
                    addLogText(str, 1, currentReceiveType);
                }
            }
            
        } else if (characteristic.equals(mLogCharact)) {
            // 日志通道处理
            String logStr = ConvertData.bytesToHexString(recvData, false);
            addLogText(HJBleApplication.getAppContext().getString(R.string.note) + logStr, 
                Color.rgb(139, 0, 255), logStr.length());
        }
    }
    
    @Override
    public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
        if (!StringUtils.isEmpty(stateMsg)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (loadingDialog != null) loadingDialog.dismiss();
                    
                    // 根据消息内容显示提示
                    int type = 2; // 默认类型
                    if (stateMsg.contains("成功")) {
                        type = 0;
                    } else if (stateMsg.contains("失败")) {
                        type = 1;
                    }
                    showMsg(type, stateMsg);
                }
            });
        }
    }
};
```

**回调类型**:
1. **OnWiseBluetoothState** - 连接状态变化
2. **OnReceiveData** - 接收到设备数据
3. **OnWiseBluetoothConnectStateCode** - 连接详细状态码

## 数据解析模式

### 7. 消息分发机制

```java
void addLogText(final String log, int type, int dataType) {
    Message message = new Message();
    message.what = MSG_DATA_CHANGE;
    message.arg1 = type;       // 0=发送, 1=接收, 2=日志
    message.arg2 = dataType;   // 数据类型标识
    message.obj = log;         // 数据内容
    mHandler.sendMessage(message);
}
```

**type参数说明**:
- `0` - 发送的消息（显示为蓝色）
- `1` - 接收的消息（显示为紫色）
- `2` - 日志信息

### 8. 数据解析核心方法

```java
private void analysisMsg(int arg1, int arg2, Object obj) {
    if (arg1 == 0) {
        // ========== 发送的消息 ==========
        String msgContext = (String) obj;
        if (arg2 == Constant.PARAM_LIHER_ORDER || llOrderMode.getVisibility() == View.VISIBLE) {
            // 订单模式：显示在ScrollView中
            SpannableStringBuilder builder = new SpannableStringBuilder(
                HJBleApplication.getAppContext().getString(R.string.send) + msgContext);
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
            builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            TextView tView = new TextView(context);
            tView.setText(builder);
            llOrders.addView(tView);
            sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
        }
        
    } else if (arg1 == 1) {
        // ========== 接收的消息 ==========
        String msgContext = (String) obj;
        SpannableStringBuilder builder = new SpannableStringBuilder(
            HJBleApplication.getAppContext().getString(R.string.receive) + msgContext);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
        builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // 根据数据类型分发处理
        switch (arg2) {
            case Constant.Return_Read_Detail:
                parseDetailData(msgContext);
                break;
            case Constant.Return_Write_SWJ_DEVMN:
                handleWriteResult(msgContext, "Write DevMN=", R.string.swj_set_mn_success);
                refreshDetails();
                break;
            case Constant.Return_SWJ_TimeData:
                parseTimeData(msgContext);
                break;
            // ... 更多case分支
        }
        
    } else if (arg1 == 2) {
        // ========== 日志信息 ==========
        // 可选：记录到文件或数据库
    }
}
```

### 9. 数据解析模板

#### 模板1: #分隔符解析（最常用）

```java
private void parseDetailData(String msgContext) {
    try {
        // 1. 按#分割字段
        String[] details = msgContext.split("#", -1);
        
        // 2. 版本号校验
        String version = details[0];
        if (StringUtils.isInt(version) && Integer.parseInt(version) < 260423) {
            showMsg(1, "请尽快升级设备蓝牙程序到260423及以上版本");
        }
        
        // 3. 时间字段解析
        if (details[1] != null && details[1].length() == 12 && details[1].startsWith("2")) {
            sysTime = DateUtil.format2miao("20" + details[1], "yyyyMMddHHmmss");
        }
        
        // 4. 数值字段解析（带空值和类型校验）
        if (details[7] != null && StringUtils.isInt(details[7])) {
            devEv = Integer.parseInt(details[7]);
        }
        
        if (details[8] != null && StringUtils.isInt(details[8])) {
            devInstallHigh = Integer.parseInt(details[8]);
        }
        
        // 5. 特殊处理：有符号数转换
        if (details[10] != null && StringUtils.isInt(details[10])) {
            int value = Integer.parseInt(details[10]);
            devCalibration = value < 128 ? value : (value - 256); // 转有符号
        }
        
        // 6. 浮点数转换（定点数）
        if (details[17] != null && StringUtils.isInt(details[17])) {
            curveA = DoubleUtils.div(Integer.parseInt(details[17]), 100d, 2);
        }
        
        // 7. IP地址解析（12位数字转点分格式）
        if (details[6] != null && details[6].length() == 12 && StringUtils.isLong(details[6])) {
            String ip = Integer.parseInt(details[6].substring(0,3)) + "." +
                       Integer.parseInt(details[6].substring(3,6)) + "." +
                       Integer.parseInt(details[6].substring(6,9)) + "." +
                       Integer.parseInt(details[6].substring(9,12));
            tvDevIp.setText(ip);
        }
        
        // 8. 更新UI
        updateUI();
        
    } catch (Exception e) {
        e.printStackTrace();
        showMsg(2, getString(R.string.swj_version_too_low));
    }
}
```

**关键字段处理**:
```java
// 整数转浮点（毫米→米）
double valueInMeters = DoubleUtils.div(integerValue, 1000d, 3);

// 有符号数转换（0-255 → -128~127）
int signedValue = unsignedValue < 128 ? unsignedValue : (unsignedValue - 256);

// 百分比计算
if (devEv <= 9800) {
    percentage = 0;
} else if (devEv >= 10800) {
    percentage = 100;
} else {
    percentage = (devEv - 9800) / 10;
}

// 时间戳格式化
String timeStr = sysTime > 0 ? DateUtil.long2time4(sysTime) : "";
```

#### 模板2: =赋值解析（写入确认）

```java
case Constant.Return_Write_SWJ_DEVMN:
    if (msgContext != null && msgContext.contains("Write DevMN=")) {
        showMsg(0, HJBleApplication.getAppContext().getString(R.string.swj_set_mn_success));
        // 提取返回值
        String mn = msgContext.substring(msgContext.indexOf("=") + 1);
        tvDevMN.setText(mn.trim());
    } else {
        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail) + ",\n" + msgContext);
    }
    refreshDetails(); // 刷新详情
    break;
```

#### 模板3: 多条数据解析（曲线系数）

```java
private void parseCurveData(String msgContext) {
    String[] details = msgContext.split("#", -1);
    
    if (details.length >= 26) {
        // 5组曲线系数（每组4个字段：A,B,C,阈值）
        curveA = DoubleUtils.div(Integer.parseInt(details[5]), 100d, 2);
        curveB = DoubleUtils.div(Integer.parseInt(details[6]), 100d, 2);
        curveC = DoubleUtils.div(Integer.parseInt(details[7]), 100d, 2);
        curveSWYZ = Integer.parseInt(details[8]);
        
        curve1A = DoubleUtils.div(Integer.parseInt(details[9]), 100d, 2);
        curve1B = DoubleUtils.div(Integer.parseInt(details[10]), 100d, 2);
        curve1C = DoubleUtils.div(Integer.parseInt(details[11]), 100d, 2);
        curve1SWYZ = Integer.parseInt(details[12]);
        
        // ... 更多曲线
    }
}
```

## 参数读写规范

### 10. 读取参数流程

```java
// 步骤1: 定义参数常量（Constant.java）
public static final int Param_SWJ_TimeData = 5189;
public static final int Return_SWJ_TimeData = 5190;

// 步骤2: 点击触发读取
case R.id.tv_time_reads:
    sendLHBleData("Read TimeData", Constant.Param_SWJ_TimeData);
    break;

// 步骤3: 处理返回数据
case Constant.Return_SWJ_TimeData:
    String[] timeData = msgContext.split("#", -1);
    if (timeData != null && timeData.length >= 6) {
        waterLev = Integer.parseInt(timeData[0]);
        devDistance = Integer.parseInt(timeData[1]);
        // ... 更新UI
    }
    break;
```

### 11. 写入参数流程

```java
// 步骤1: 定义参数常量
public static final int Param_Write_SWJ_DEVMN = 5159;
public static final int Return_Write_SWJ_DEVMN = 5160;

// 步骤2: 弹出输入对话框
case R.id.tv_dev_mn:
    new HitiInputStrDailog(
        LiherXXXActivity.this, 
        Constant.Dialog_SWJ_DevMN,           // 对话框ID
        HJBleApplication.getAppContext().getString(R.string.swj_set_mn),  // 标题
        HJBleApplication.getAppContext().getString(R.string.swj_mn),      // 提示
        StringUtils.isEmpty(devMN) ? "" : devMN,  // 默认值
        LiherXXXActivity.this                // 回调监听
    );
    break;

// 步骤3: 验证并发送（onViewClick回调）
case Constant.Dialog_SWJ_DevMN:
    String MN = (String) target;
    if (StringUtils.isEmpty(MN) || MN.trim().length() != 12) {
        showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_mn));
    } else {
        // 格式化：固定长度补零
        String formattedMN = StringUtils.formatString(MN.trim(), 12);
        sendLHBleData("Write DevMN=" + formattedMN, Constant.Param_Write_SWJ_DEVMN);
    }
    break;

// 步骤4: 处理写入结果
case Constant.Return_Write_SWJ_DEVMN:
    if (msgContext != null && msgContext.contains("Write DevMN=")) {
        showMsg(0, HJBleApplication.getAppContext().getString(R.string.swj_set_mn_success));
    }
    refreshDetails(); // 重新读取确认
    break;
```

**数据格式化示例**:
```java
// 整数补零（4位）
String interval = StringUtils.formatString(Integer.parseInt(value), 4);
// 输入: "5" → 输出: "0005"

// IP地址格式化（每段3位）
String[] ips = strIP.split("\\.");
String devIP = StringUtils.formatString(Integer.parseInt(ips[0]), 3) +
               StringUtils.formatString(Integer.parseInt(ips[1]), 3) +
               StringUtils.formatString(Integer.parseInt(ips[2]), 3) +
               StringUtils.formatString(Integer.parseInt(ips[3]), 3);
// 输入: "192.168.1.12" → 输出: "192168001012"

// 浮点转整数（保留精度）
int huanghai = (int) (DoubleUtils.round(Double.parseDouble(value), 3) * 1000);
String formatted = StringUtils.formatString(huanghai, 8);
// 输入: "12.345" → 输出: "00012345"
```

### 12. 常用参数类型对照表

| 参数类型 | 写入前缀 | 示例 | 格式要求 |
|---------|---------|------|---------|
| 设备编号 | `Write DevNum=` | `Write DevNum=1234567890` | 10位数字 |
| 设备MN | `Write DevMN=` | `Write DevMN=ST00000001` | 12位字符 |
| IP地址 | `Write DevIP=` | `Write DevIP=192168001012` | 12位数字 |
| 端口号 | `Write NetPort=` | `Write NetPort=08080` | 5位数字 |
| 时间间隔 | `Write SapInterval=` | `Write SapInterval=0005` | 4位数字（分钟） |
| 量程 | `Write Range=` | `Write Range=10` | 2位数字（米） |
| 安装高度 | `Write Installhigh=` | `Write Installhigh=05000` | 5位数字（毫米） |
| 校准值 | `Write Calibration=` | `Write Calibration=128` | 3位数字（-128~127） |
| 系统时间 | `Write SysTime=` | `Write SysTime=230815150220` | 12位（YYMMDDHHmmss） |
| 曲线系数 | `Write Curve=` | `Write Curve=A1#B1#C1#S1#...` | 多组#分隔 |

## UI交互模式

### 13. 双模式设计（UI模式 + 窗口模式）⭐ **首界面必备功能**

> **重要说明**：所有力合蓝牙设备的首界面必须支持交互模式和窗口模式的切换功能。窗口模式用于查看原始BLE通信日志和发送自定义指令，便于调试和问题排查。

#### 13.1 布局结构

```xml
<!-- 交互模式容器：参数展示和编辑 -->
<LinearLayout
    android:id="@+id/ll_ui_mode"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
    
    <TextView android:id="@+id/tv_dev_num" />
    <TextView android:id="@+id/tv_dev_mn" />
    <!-- ... 更多参数控件 -->
</LinearLayout>

<!-- 窗口模式容器：原始指令收发和日志显示 -->
<LinearLayout
    android:id="@+id/ll_order_mode"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:visibility="gone"
    android:padding="10dp">

    <!-- 日志显示区域 -->
    <ScrollView
        android:id="@+id/scl_orders"
        android:layout_width="match_parent"
        android:layout_height="300dp"
        android:background="@drawable/shape_background_circle_blue_10dp"
        android:scrollbars="vertical">
        
        <LinearLayout
            android:id="@+id/ll_orders"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="10dp">
            <!-- 动态添加日志TextView -->
        </LinearLayout>
    </ScrollView>

    <!-- 清屏按钮和输入区域 -->
    <LinearLayout
        android:id="@+id/bottom_layout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingTop="10dp">

        <!-- 清屏按钮 -->
        <TextView
            android:id="@+id/tv_clear_screen"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@drawable/button_background"
            android:gravity="center"
            android:padding="10dp"
            android:text="清屏"
            android:textColor="@android:color/white"
            android:layout_marginRight="5dp"/>

        <!-- 指令输入框 -->
        <EditText
            android:id="@+id/sendData"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:background="@drawable/edit_background"
            android:hint="请输入指令内容"
            android:textSize="14sp"
            android:padding="10dp"/>

        <!-- 发送按钮 -->
        <TextView
            android:id="@+id/send"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@drawable/button_background"
            android:gravity="center"
            android:padding="10dp"
            android:text="发送"
            android:textColor="@android:color/white"
            android:layout_marginLeft="5dp"/>
    </LinearLayout>
</LinearLayout>
```

#### 13.2 Java代码实现

**步骤1: 声明成员变量**
```java
public class LiherXXXActivity extends BaseActivity 
    implements CommontClickListener, View.OnClickListener {
    
    // UI模式切换相关控件
    private LinearLayout llUIMode, llOrderMode;
    private ScrollView sclOrders;
    private LinearLayout llOrders;
    private EditText sendData;
    private TextView send, tvClearScreen;
    
    // 当前发送类型（用于区分响应处理）
    private int currentSendType, currentReceiveType;
}
```

**步骤2: 初始化控件**
```java
@Override
protected void initView() {
    super.initView();
    
    // 初始化窗口模式控件
    sclOrders = findViewById(R.id.scl_orders);
    llOrders = findViewById(R.id.ll_orders);
    sendData = findViewById(R.id.sendData);
    send = findViewById(R.id.send);
    tvClearScreen = findViewById(R.id.tv_clear_screen);
    
    // 设置点击事件
    send.setOnClickListener(this);
    tvClearScreen.setOnClickListener(this);
}
```

**步骤3: 设置右上角菜单**
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // 根据权限决定是否显示模式切换功能
    if (HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {
        setRightText("交互模式");
        rightTitleTxt.setOnClickListener(v -> showSelectUIModeMenu());
        rightTitleTxt.setVisibility(View.VISIBLE);
    } else {
        rightTitleTxt.setVisibility(View.GONE);
    }
}
```

**步骤4: 实现模式切换方法**
```java
/**
 * 显示UI模式选择菜单
 * - 交互模式：可视化参数配置界面
 * - 窗口模式：原始BLE通信日志 + 自定义指令发送
 */
private void showSelectUIModeMenu() {
    PopupMenu popupMenu = new PopupMenu(this, rightTitleTxt);
    popupMenu.inflate(R.menu.menu_select_ui_mode);
    
    // 隐藏不需要的菜单项
    popupMenu.getMenu().findItem(R.id.version_upgrade).setVisible(false);
    
    // 根据权限控制菜单项显示
    if (!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {
        popupMenu.getMenu().findItem(R.id.order_mode).setVisible(false);
    }
    
    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.ui_mode:
                    // 切换到交互模式
                    llUIMode.setVisibility(View.VISIBLE);
                    llOrderMode.setVisibility(View.GONE);
                    rightTitleTxt.setText("交互模式");
                    refreshDetails(); // 刷新设备详情
                    return true;
                    
                case R.id.order_mode:
                    // 切换到窗口模式
                    llUIMode.setVisibility(View.GONE);
                    llOrderMode.setVisibility(View.VISIBLE);
                    rightTitleTxt.setText("窗口模式");
                    return true;
            }
            return false;
        }
    });
    popupMenu.show();
}
```

**步骤5: 处理返回键逻辑**
```java
/**
 * 处理返回键
 * - 窗口模式下：切换回交互模式（不退出页面）
 * - 交互模式下：正常退出页面
 */
@Override
public void onBackPressed() {
    if (llOrderMode != null && llOrderMode.getVisibility() == View.VISIBLE) {
        // 当前是窗口模式，切换回交互模式
        llUIMode.setVisibility(View.VISIBLE);
        llOrderMode.setVisibility(View.GONE);
        rightTitleTxt.setText("交互模式");
        showMsg(0, "已切换到交互模式");
        // 不调用 super.onBackPressed()，阻止退出
    } else {
        // 当前是交互模式，正常退出
        super.onBackPressed();
    }
}
```

**步骤6: 处理窗口模式的发送和清屏**
```java
@Override
public void onClick(View v) {
    switch (v.getId()) {
        case R.id.send:
            // 发送自定义指令
            String command = sendData.getText().toString().trim();
            if (StringUtils.isEmpty(command)) {
                showMsg(2, "请输入指令内容");
                return;
            }
            // 使用 PARAM_LIHER_ORDER 类型发送
            sendLHBleData(command, Constant.PARAM_LIHER_ORDER);
            sendData.setText(""); // 清空输入框
            break;
            
        case R.id.tv_clear_screen:
            // 清屏：移除所有日志视图
            llOrders.removeAllViews();
            showMsg(0, "日志已清空");
            break;
    }
}
```

**步骤7: 修改 analysisMsg 支持所有模式的日志记录**
```java
private void analysisMsg(int arg1, int arg2, Object obj) {
    if (arg1 == 0) { // 发送的消息
        String msgContext = (String) obj;
        
        // ✅ 所有发送的指令都显示在窗口日志中（无论当前模式）
        if (arg2 == Constant.PARAM_LIHER_ORDER || arg2 == Constant.Param_Read_Detail) {
            SpannableStringBuilder builder = new SpannableStringBuilder(
                "发送:" + msgContext);
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
            builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            TextView tView = new TextView(context);
            tView.setText(builder);
            llOrders.addView(tView);
            sclOrders.fullScroll(ScrollView.FOCUS_DOWN); // 自动滚动到底部
        }
        
        // 错误信息显示为红色
        if (arg2 == Constant.Return_BLE_False) {
            SpannableStringBuilder builder = new SpannableStringBuilder(msgContext);
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
            builder.setSpan(colorSpan, 0, msgContext.length() > 3 ? 3 : msgContext.length(), 
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            TextView tView = new TextView(context);
            tView.setText(builder);
            llOrders.addView(tView);
            sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
        }
        
    } else if (arg1 == 1) { // 接收的消息
        String msgContext = (String) obj;
        
        switch (arg2) {
            case Constant.RETURN_LIHER_OREDER:
                // ✅ 窗口模式自定义指令的返回数据显示为紫色
                SpannableStringBuilder builder = new SpannableStringBuilder(
                    "接收:" + msgContext);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                break;
                
            case Constant.Return_Read_Detail:
                // ✅ 设备详情的返回数据也显示在日志中
                SpannableStringBuilder builder2 = new SpannableStringBuilder(
                    "接收:" + msgContext);
                ForegroundColorSpan colorSpan2 = new ForegroundColorSpan(Color.rgb(139, 0, 255));
                builder2.setSpan(colorSpan2, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                TextView tView2 = new TextView(context);
                tView2.setText(builder2);
                llOrders.addView(tView2);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                
                // 再解析数据更新UI（不影响原有业务逻辑）
                parseDetailData(msgContext);
                break;
        }
    }
}
```

**步骤8: BLE回调中添加 PARAM_LIHER_ORDER 处理**
```java
WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
    @Override
    public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {
        if (characteristic.equals(mReceiveCharact)) {
            if (loadingDialog != null) loadingDialog.dismiss();
            
            // 解析数据
            String str = ProtocolUtil.getReturnMsgForReceive(recvData);
            
            if (!StringUtils.isEmpty(str)) {
                // 检查非正常返回（必须在switch之前）
                if (Constant.ReceiveFalse.equals(str)) {
                    addLogText("蓝牙返回数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                } else if (Constant.ReceiveContinue.equals(str)) {
                    addLogText("蓝牙消息返回中", 1, Constant.Return_BLE_Continue);
                } else {
                    // ✅ 关键：根据发送类型确定接收类型
                    switch (currentSendType) {
                        case Constant.PARAM_LIHER_ORDER:  // ⭐ 新增，支持窗口模式
                            currentReceiveType = Constant.RETURN_LIHER_OREDER;
                            break;
                        case Constant.Param_Read_Detail:
                            currentReceiveType = Constant.Return_Read_Detail;
                            break;
                        default:
                            currentReceiveType = 0;
                            return;
                    }
                    
                    // 通过Handler转发到主线程处理
                    addLogText(str, 1, currentReceiveType);
                }
            }
        }
    }
};
```

#### 13.3 常量定义（Constant.java）

```java
// ========== 窗口模式专用常量 ==========

// 窗口模式自定义指令发送类型
public static final int PARAM_LIHER_ORDER = 5000;
public static final int RETURN_LIHER_OREDER = 5001;

// 对话框ID（可选）
public static final int Dialog_Clear_Screen_Confirm = 8000;
```

#### 13.4 完整工作流程

```
用户操作流程：
1. 进入设备控制页面 → 默认显示交互模式
2. 点击右上角"交互模式" → 弹出菜单
3. 选择"窗口模式" → 切换到窗口模式界面
4. 查看所有BLE通信历史日志（从启动开始的所有收发记录）
5. 在输入框输入自定义指令 → 点击"发送"
6. 查看指令发送（蓝色）和设备返回（紫色）
7. 点击"清屏" → 清空所有日志
8. 按返回键 → 切换回交互模式（不退出页面）
9. 再次按返回键 → 退出页面
```

#### 13.5 关键技术点

✅ **日志颜色规范**:
- 🔵 **蓝色** (#0000FF) - 发送的指令
- 🟣 **紫色** (RGB: 139, 0, 255) - 接收的数据
- 🔴 **红色** (#FF0000) - 错误信息

✅ **日志记录时机**:
- 所有 `sendLHBleData()` 调用都会触发日志记录
- 所有 BLE 回调数据都会触发日志记录
- 不受当前UI模式影响（始终记录）

✅ **清屏功能**:
- 仅清空UI显示（`llOrders.removeAllViews()`）
- 不影响内存中的历史记录
- 给用户明确提示

✅ **返回键处理**:
- 窗口模式按返回 → 切换回交互模式
- 交互模式按返回 → 退出页面
- 提升用户体验，避免误操作退出

#### 13.6 注意事项

⚠️ **重要提醒**:
1. **必须在BLE回调中处理 `PARAM_LIHER_ORDER`**，否则窗口模式的返回数据不会显示
2. **日志记录不应受当前模式影响**，这样切换到窗口模式时能看到完整的通信历史
3. **清屏功能要谨慎使用**，建议添加二次确认对话框
4. **返回键逻辑必须正确**，窗口模式下不应直接退出页面
5. **编译检查**：每次修改后必须执行 `gradlew.bat compileDebugJavaWithJavac`

❌ **常见错误**:
```java
// ❌ 错误1：忘记在switch中添加 PARAM_LIHER_ORDER
case Constant.Param_Read_Detail:
    currentReceiveType = Constant.Return_Read_Detail;
    break;
default:
    return; // 窗口模式的返回数据被丢弃！

// ✅ 正确写法：
case Constant.PARAM_LIHER_ORDER:  // ⭐ 必须添加
    currentReceiveType = Constant.RETURN_LIHER_OREDER;
    break;
```

```java
// ❌ 错误2：只在窗口模式下记录日志
if (llOrderMode.getVisibility() == View.VISIBLE) {
    // 记录日志
}
// 这样交互模式的通信不会被记录！

// ✅ 正确做法：始终记录日志，与模式无关
if (arg2 == Constant.PARAM_LIHER_ORDER || arg2 == Constant.Param_Read_Detail) {
    // 记录日志（无论当前是什么模式）
}
```

#### 13.7 实际案例参考

完整实现请参考以下文件：
- [LiherADCPActivity.java](file://E:\AndroidWorkSpace\LHBle\APP_SampleBox\app\src\main\java\com\lh\lhble\UI\LiherADCPActivity.java)
- [LiherCYX7Activity.java](file://E:\AndroidWorkSpace\LHBle\APP_SampleBox\app\src\main\java\com\lh\lhble\UI\LiherCYX7Activity.java)
- [activity_liher_lhadcp.xml](file://E:\AndroidWorkSpace\LHBle\APP_SampleBox\app\src\main\res\layout\activity_liher_lhadcp.xml)

#### 13.8 系统时间自动校验与用户修改

> **重要说明**：所有力合蓝牙设备的首界面在读取Read Detail后，必须自动校验设备系统时间与手机时间的差异，超过1分钟时弹出对话框引导用户校准。同时支持用户手动点击系统时间字段进行修改。

##### 13.8.1 实现流程

```
Read Detail返回 → 解析sysTime字段 → 判断是否合法 → 与手机时间比对
    ├── 时间差 > 1分钟 → 弹出“设置系统时间”对话框
    ├── 时间不合法(空/格式错误) → 弹出“设置系统时间”对话框
    └── 时间差 ≤ 1分钟 → 正常显示，不弹窗

用户点击系统时间字段 → 弹出“设置系统时间”对话框 → 用户输入正确时间 → 发送Write SysTime指令 → 更新UI
```

##### 13.8.2 所需常量（Constant.java中已定义）

```java
// 写入系统时间的发送/接收类型
public static final int Param_Wirte_SysTime = 5030;
public static final int Return_Wirte_SysTime = 5031;

// 对话框ID
public static final int Dailog_BLE_SYSDateTime = -6;
```

##### 13.8.3 Activity需实现的接口

Activity必须实现 `CommontClickListener` 接口，用于接收 `HitiInputStrDailog` 的回调：

```java
public class LiherXXXActivity extends BaseActivity 
    implements CommontClickListener, View.OnClickListener {
    // ...
}
```

##### 13.8.4 解析Read Detail后自动校验系统时间

在解析完Read Detail数据并更新UI后，添加系统时间校验逻辑：

```java
private void parseDetailData(String msgContext) {
    // ... 解析所有字段 ...
    
    // 更新UI
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            updateUI();
            // ⭐ 在更新UI后校验系统时间
            checkAndCorrectSysTime();
        }
    });
}

/**
 * 校验设备系统时间，与手机时间差异超过1分钟则弹窗提示用户校准
 * 参考实现：LiherCYX7Activity 第1854-1862行
 */
private void checkAndCorrectSysTime() {
    String deviceSysTime = sysInfo.getSysTime(); // 12位 yyMMddHHmmss
    
    if (!StringUtils.isEmpty(deviceSysTime) && deviceSysTime.startsWith("2")) {
        // 系统时间格式正常，显示并比较
        tvSysTime.setText(DateUtil.long2time4(
            DateUtil.format2miao("20" + deviceSysTime, "yyyyMMddHHmmss")));
        
        // 比较设备时间与手机时间，差异超过1分钟则弹窗
        long deviceTimeMillis = DateUtil.format2miao("20" + deviceSysTime, "yyyyMMddHHmmss");
        if (Math.abs(System.currentTimeMillis() - deviceTimeMillis) > 1 * 60 * 1000) {
            new HitiInputStrDailog(this, Constant.Dailog_BLE_SYSDateTime, 
                "设置系统时间", "当前正确时间：", 
                DateUtils.long2time(System.currentTimeMillis()), this);
        }
    } else {
        // 系统时间不合法（空或不以2开头），直接弹窗要求设置
        new HitiInputStrDailog(this, Constant.Dailog_BLE_SYSDateTime, 
            "设置系统时间", "当前正确时间：", 
            DateUtils.long2time(System.currentTimeMillis()), this);
    }
}
```

##### 13.8.5 用户手动点击修改系统时间

系统时间字段应设置点击监听，用户点击后弹出设置对话框：

```java
// initView() 中设置点击监听
tvSysTime.setOnClickListener(this);

// onClick() 中处理点击
case R.id.tv_sys_time:
    new HitiInputStrDailog(this, Constant.Dailog_BLE_SYSDateTime, 
        "设置系统时间", "当前正确时间：", 
        DateUtils.long2time(System.currentTimeMillis()), this);
    break;
```

##### 13.8.6 CommontClickListener回调处理

在 `onViewClick` 方法中处理对话框返回的用户输入：

```java
@Override
public void onViewClick(Object target, int type) {
    switch (type) {
        case Constant.Dailog_BLE_SYSDateTime:
            String sysTime = (String) target;
            if (sysTime != null && sysTime.length() == 19) {
                // 用户输入格式：yyyy-MM-dd HH:mm:ss (19位)
                // 转换为 yyMMddHHmmss (12位) 后发送
                String formattedTime = DateUtils.formatTime(sysTime, 
                    DateUtils.DATE_TIME_PATTERN, 
                    DateUtils.METHOD_DATE_TIME_PATTERN).substring(2);
                sendLHBleData("Write SysTime=" + formattedTime, 
                    Constant.Param_Wirte_SysTime);
            } else {
                showMsg(2, "请录入正确时间，" + sysTime);
            }
            break;
    }
}
```

##### 13.8.7 BLE回调中处理写入系统时间的响应

**步骤1**: 在BLE回调的 switch(currentSendType) 中添加映射：

```java
case Constant.Param_Wirte_SysTime:
    currentReceiveType = Constant.Return_Wirte_SysTime;
    break;
```

**步骤2**: 在 analysisMsg 的 switch(arg2) 中处理返回：

```java
case Constant.Return_Wirte_SysTime:
    if (msgContext != null && msgContext.contains("Write SysTime=")) {
        // 更新本地存储的系统时间
        if (sysInfo != null) {
            sysInfo.setSysTime(msgContext.substring(14).trim());
        }
        // 更新UI显示
        tvSysTime.setText(DateUtil.long2time4(
            DateUtil.format2miao("20" + sysInfo.getSysTime(), "yyyyMMddHHmmss")));
        showMsg(0, "系统时间设置成功");
    } else {
        showMsg(1, "设置系统时间失败,\n" + msgContext);
    }
    break;
```

##### 13.8.8 所需import

```java
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.wise.wisekit.utils.DateUtils;
```

##### 13.8.9 关键注意事项

⚠️ **时间格式转换说明**:
- 设备使用 `yyMMddHHmmss`（12位），如 `260720153000`
- 用户输入使用 `yyyy-MM-dd HH:mm:ss`（19位），如 `2026-07-20 15:30:00`
- `DateUtils.formatTime().substring(2)` 将19位转为12位（去掉前2位"20"）
- `DateUtil.format2miao("20" + sysTime, "yyyyMMddHHmmss")` 将12位转为时间戳

⚠️ **校验时机**:
- 仅在 Read Detail 返回后自动校验一次，不要在每次刷新时重复校验
- 用户手动点击时始终弹窗，不受时间差限制

⚠️ **避免死循环**: 写入系统时间成功后不要再次触发 Read Detail，否则可能循环弹窗

### 14. 权限控制

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    // 根据用户权限显示/隐藏功能
    if (HJBleApplication.getUser().getStrPermissions().contains("PRO_Qualification") 
        || HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {
        
        setRightText(HJBleApplication.getAppContext().getString(R.string.ui_mode));
        rightTitleTxt.setOnClickListener(v -> showSelectUIModeMenu());
        rightTitleTxt.setVisibility(View.VISIBLE);
        tvSetMacAddress.setVisibility(View.VISIBLE);
        
    } else {
        rightTitleTxt.setVisibility(View.GONE);
        tvSetMacAddress.setVisibility(View.INVISIBLE);
    }
}
```

### 15. 自定义对话框

#### 输入对话框
```java
new HitiInputStrDailog(
    context,                              // 上下文
    Constant.Dialog_SWJ_DevMN,            // 对话框ID（用于回调识别）
    "标题",                                // 对话框标题
    "提示信息",                            // 输入框提示
    "默认值",                              // 输入框默认值
    this                                  // CommontClickListener回调
);
```

#### 确认对话框
```java
new HitiStringDailog(
    context,
    Constant.Dialog_SWJ_ResetNet,         // 对话框ID
    null,                                  // 目标数据（可选）
    "确认重置网络？",                       // 标题
    "此操作将清空网络配置",                  // 内容
    this                                   // 回调
);
```

#### 曲线编辑对话框
```java
new RangeCurveEditDailog(
    context,
    Constant.Dialog_SWJ_RangeCurve,       // 对话框ID
    0,                                     // 曲线序号（0-4）
    curveA, curveB, curveC, curveSWYZ,    // 当前值
    this                                   // 回调
);
```

### 16. 下拉菜单选择

```java
private void showSelectUploadTypeMenu() {
    PopupMenu popupMenu = new PopupMenu(this, tvUploadType);
    popupMenu.inflate(R.menu.menu_select_upload_type);
    
    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.type_212:
                    sendLHBleData("Write Protocol=0", Constant.Param_Write_SWJ_UploadType);
                    return true;
                case R.id.type_mgtt:
                    sendLHBleData("Write Protocol=1", Constant.Param_Write_SWJ_UploadType);
                    return true;
            }
            return false;
        }
    });
    popupMenu.show();
}
```

## 高级功能

### 17. 定时轮询

```java
private int isMeaSure = 0;

case R.id.tv_time_reads:
    RxTimerUtil.cancel();
    if (isMeaSure == 0) {
        isMeaSure = 1;
        tvTimeReads.setText(HJBleApplication.getAppContext().getString(R.string.stop_circle));
        
        // 每10秒读取一次
        RxTimerUtil.interval(10000L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read TimeData", Constant.Param_SWJ_Measure);
            }
        });
    } else {
        isMeaSure = 0;
        tvTimeReads.setText(HJBleApplication.getAppContext().getString(R.string.circle_read));
    }
    break;
```

**注意事项**:
- ⚠️ 发送其他命令时需停止轮询
- ⚠️ Activity销毁时必须取消定时器

```java
public void sendLHBleData(String sendString, int sendType) {
    if (isMeaSure != 0 && sendType != Constant.Param_SWJ_Measure) {
        RxTimerUtil.cancel();
        isMeaSure = 0;
        tvTimeReads.setText(HJBleApplication.getAppContext().getString(R.string.circle_read));
    }
    // ... 发送逻辑
}

@Override
protected void onDestroy() {
    super.onDestroy();
    RxTimerUtil.cancel(); // 必须取消
    mble.disconnectDevice();
}
```

### 18. 设备忙检测

```java
// 状态位解析（16位状态字）
long moduleSTA16 = Long.parseLong(details[8]);

// 检测上传中状态（第3位）
if (((byte) ((moduleSTA16 >> 2 & 0xff) % 2)) == 1) {
    // 设备正在上传数据，禁止修改参数
    if (v.getId() != R.id.tv_refresh) {
        showMsg(2, "设备正在上传数据，请刷新后重试");
        return;
    }
}

// 显示上传进度
if (((byte) ((moduleSTA16 >> 2 & 0xff) % 2)) == 1) {
    showMsg(2, "设备正在上传数据，进行到'" + StringUtils.getLotStep(lotStep) + "'");
    playAlarmSoundAndVibrate(); // 播放提示音和震动
    
    // 3秒后自动刷新
    RxTimerUtil.timer(3000L, number -> {
        sendLHBleData("Read Detail", Constant.Param_Read_Detail);
    });
    return;
}
```

### 19. 二维码扫描

```java
case R.id.tv_set_mac_address:
    startActivityForResult(
        new Intent(LiherXXXActivity.this, CaptureActivity.class), 
        Constant.RequestCode_Scan_Box_MAC_Address
    );
    break;

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK) {
        switch (requestCode) {
            case Constant.RequestCode_Scan_Box_MAC_Address:
                String boxQcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                
                if (StringUtils.isEmpty(boxQcode)) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.not_qcode_info));
                } else if (boxQcode.length() != 12) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.not_lh_qcode));
                } else if (boxQcode.startsWith("12")) {
                    // MAC地址转换
                    String macAds = boxQcode.trim();
                    String newMac = StringUtils.getNewBlueToothMacAddress(macAds);
                    
                    new HitiStringDailog(
                        LiherXXXActivity.this,
                        Constant.Dailog_BLE_MACAddress,
                        macAds,
                        "确认MAC地址",
                        "新MAC地址：" + newMac,
                        LiherXXXActivity.this
                    );
                } else {
                    showMsg(2, boxQcode + "不是水位计二维码");
                }
                break;
        }
    }
}
```

### 20. 日志上报

```java
private void insertLog(String blueMac, String sampleId, String logContent) {
    if (StringUtils.isEmpty(blueMac)) {
        return;
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("blueMac", blueMac);
    params.put("sampleId", sampleId);
    params.put("operator", HJBleApplication.getUser().getUserId());
    params.put("logContent", logContent);
    
    RetrofitUtils.getApiService().insertLog(JSON.toJSON(params))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Result>() {
            @Override
            public void onSubscribe(Disposable d) {}
            
            @Override
            public void onNext(Result result) {}
            
            @Override
            public void onError(Throwable e) {}
            
            @Override
            public void onComplete() {}
        });
}
```

## 生命周期管理

### 21. 完整的生命周期

```java
@Override
protected void onResume() {
    super.onResume();
    
    // 根据配置模式选择服务通道
    if (HJBleApplication.shareInstance().isBleConfig() == 2) {
        mSendCharact = Constant.Ble_Config_Send_Service;
        mReceiveCharact = Constant.Ble_Config_Receive_Service;
    } else {
        mSendCharact = Constant.Ble_Data_Send_Service;
        mReceiveCharact = Constant.Ble_Data_Receive_Service;
    }
    
    setTitle(mDeviceName);
    
    if (!StringUtils.isEmpty(mDeviceAddress)) {
        tvMacAddress.setText(mDeviceAddress);
    }
}

@Override
protected void onPause() {
    super.onPause();
    // 可选：暂停轮询
}

@Override
protected void onDestroy() {
    super.onDestroy();
    
    // 1. 取消定时器
    RxTimerUtil.cancel();
    
    // 2. 断开BLE连接
    mble.disconnectDevice();
    Log.d(TAG, "destroy");
    
    // 3. 断开本地连接
    mble.disconnectLocalDevice();
    Log.d(TAG, "销毁");
}
```

## 错误处理和提示

### 22. 统一的提示方式

```java
// type: 0=成功(绿色), 1=失败(红色), 2=提示(黄色)
showMsg(0, "操作成功");
showMsg(1, "操作失败");
showMsg(2, "提示信息");
```

### 23. BLE非正常返回数据处理

在`OnReceiveData`回调中，**必须在switch之前**检查以下4种非正常返回情况：

#### 常量定义（Constant.java）

```java
// ========== 蓝牙通信异常标识 ==========

// 1. 接收数据不符合力合协议格式
public static final String ReceiveFalse = "ReceiveFalse";
public static final int Return_Ble_ReceiveFalse = -1;

// 2. 接收分包数据不符合力合协议格式
public static final String ReceiveDataListFalse = "ReceiveDataListFalse";

// 3. 设备正在返回数据（需要等待后续包）
public static final String ReceiveContinue = "ReceiveContinue";
public static final int Return_BLE_Continue = -2;

// 4. 发送分包数据错误（前缀）
public static final String SendDataListFalse = "SendDataListFalse:";
```

#### 处理逻辑模板

```java
@Override
public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {
    if (characteristic.equals(mReceiveCharact)) {
        if (loadingDialog != null) loadingDialog.dismiss();
        
        // 解析数据
        String str = ProtocolUtil.getReturnMsgForReceive(recvData);
        
        if (!StringUtils.isEmpty(str)) {
            // ⚠️ 关键：必须先检查非正常返回，再处理正常数据
            
            if (Constant.ReceiveFalse.equals(str)) {
                // 情况1：协议解析失败
                // 原因：设备返回的数据不符合力合蓝牙协议格式
                // 处理：记录日志，不继续处理
                addLogText("蓝牙返回数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                
            } else if (Constant.ReceiveDataListFalse.equals(str)) {
                // 情况2：分包重组失败
                // 原因：多包数据拼接后仍不符合协议格式
                // 处理：记录日志，建议重新发送命令
                addLogText("蓝牙返回分包数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                
            } else if (Constant.ReceiveContinue.equals(str)) {
                // 情况3：数据分片传输中
                // 原因：设备数据量大，分多次返回
                // 处理：记录日志，等待后续数据包（不要关闭loading）
                addLogText("蓝牙消息返回中", 1, Constant.Return_BLE_Continue);
                
            } else if (str.startsWith(Constant.SendDataListFalse)) {
                // 情况4：发送端分包错误
                // 原因：APP发送的分包数据有误
                // 处理：提取错误信息并显示
                String errorMsg = str.substring(Constant.SendDataListFalse.length());
                addLogText(errorMsg, 1, Constant.Return_Ble_ReceiveFalse);
                
            } else {
                // ✅ 正常数据：进入业务逻辑处理
                switch (currentSendType) {
                    case Constant.Param_Read_Detail:
                        currentReceiveType = Constant.Return_Read_Detail;
                        break;
                    // ... 其他类型
                    default:
                        currentReceiveType = 0;
                        return;
                }
                addLogText(str, 1, currentReceiveType);
            }
        }
    }
}
```

#### 四种异常情况详解

| 异常类型 | 触发条件 | 可能原因 | 处理建议 |
|---------|---------|---------|----------|
| `ReceiveFalse` | 单包数据解析失败 | 1. 设备固件版本过低<br>2. 数据通道配置错误<br>3. 设备未响应 | 1. 检查设备版本<br>2. 确认使用正确的Service UUID<br>3. 重试或重启设备 |
| `ReceiveDataListFalse` | 多包数据重组失败 | 1. 网络干扰导致丢包<br>2. 设备返回数据不完整<br>3. 超时时间设置过短 | 1. 靠近设备重试<br>2. 增加waitReturn超时时间<br>3. 检查设备状态 |
| `ReceiveContinue` | 设备返回"继续等待"标识 | 1. 数据量大需要分批<br>2. 设备正在查询数据库<br>3. 设备处理耗时操作 | 1. **不要关闭loading**<br>2. 等待后续数据包<br>3. 适当延长超时时间 |
| `SendDataListFalse` | APP发送分包错误 | 1. 指令过长未正确分包<br>2. MTU设置不合理<br>3. 协议封装错误 | 1. 检查ProtocolUtil封装逻辑<br>2. 调整MTU大小<br>3. 查看错误详情 |

#### 实际案例

**案例1：处理历史数据读取（大数据量）**
```java
case Constant.Param_LHADCP_Read_History:
    // 历史数据可能很大，会触发ReceiveContinue
    if (Constant.ReceiveContinue.equals(str)) {
        // 保持loading显示，等待后续数据
        addLogText("蓝牙消息返回中", 1, Constant.Return_BLE_Continue);
        // 注意：不要return，让waitReturn继续等待
    } else {
        currentReceiveType = Constant.Return_LHADCP_Read_History;
        addLogText(str, 1, currentReceiveType);
    }
    break;
```

**案例2：处理协议版本不兼容**
```java
if (Constant.ReceiveFalse.equals(str)) {
    // 可能是旧版设备，尝试降级处理
    showMsg(2, "设备协议版本过低，请升级固件");
    addLogText("蓝牙返回数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
    
    // 可选：尝试用旧版协议解析
    // String oldStr = ConvertData.bytesToHexString(recvData, false);
    // parseOldData(oldStr);
}
```

**案例3：调试分包错误**
```java
else if (str.startsWith(Constant.SendDataListFalse)) {
    String errorMsg = str.substring(Constant.SendDataListFalse.length());
    
    // 记录详细错误信息用于调试
    Logger.e("分包发送错误: " + errorMsg);
    addLogText(errorMsg, 1, Constant.Return_Ble_ReceiveFalse);
    
    // 提示用户
    showMsg(1, "数据发送失败：" + errorMsg);
}
```

#### 注意事项

⚠️ **重要提醒**:
1. **顺序很重要**：必须在switch之前检查这4种情况，否则会将错误标识当作正常数据处理
2. **ReceiveContinue特殊处理**：收到此标识时不要关闭loading对话框，应继续等待
3. **空值检查**：使用`!StringUtils.isEmpty(str)`而非`str != null`，避免空字符串进入逻辑
4. **日志颜色**：非正常返回统一使用type=1（红色），便于识别
5. **不要静默失败**：所有异常情况都应记录日志，方便排查问题

✅ **最佳实践**:
```java
// ❌ 错误写法：先switch再检查异常
switch (currentSendType) {
    case Constant.Param_Read_Detail:
        // 如果str是"ReceiveFalse"，会进入这里造成混乱
        parseDetailData(str);
        break;
}
if (Constant.ReceiveFalse.equals(str)) { ... } // 太晚了！

// ✅ 正确写法：先检查异常再switch
if (Constant.ReceiveFalse.equals(str)) {
    addLogText("协议错误", 1, Constant.Return_Ble_ReceiveFalse);
} else {
    switch (currentSendType) {
        case Constant.Param_Read_Detail:
            parseDetailData(str); // 确保str是正常数据
            break;
    }
}
```

### 24. 常见错误处理

```java
// 1. 连接断开
if (mble == null || !mble.isConnect()) {
    showMsg(2, HJBleApplication.getAppContext().getString(R.string.ble_close_connect_again));
    finish();
    return;
}

// 2. 超时处理
else if (!mble.waitReturn(mReceiveCharact, 15*1000)) {
    addLogText(getResources().getString(R.string.ble_rtn_timeout), 
        1, Constant.Return_BLE_False);
    currentSendType = 0;
}

// 3. 数据格式异常
try {
    String[] details = msgContext.split("#", -1);
    // 解析逻辑
} catch (Exception e) {
    e.printStackTrace();
    showMsg(2, getString(R.string.unknown_error) + "\n" + msgContext);
}

// 4. 版本过低
if (StringUtils.isInt(version) && Integer.parseInt(version) < 260423) {
    showMsg(1, "请尽快升级设备蓝牙程序到260423及以上版本");
}
```

## 快速开始模板

### 24. 新设备接入清单

创建新设备Activity时，按以下步骤操作：

**步骤1: 复制模板文件**
```bash
# 基于LiherSWJ1Activity.java创建新文件
cp LiherSWJ1Activity.java LiherNewDeviceActivity.java
```

**步骤2: 修改类名和布局**
```java
public class LiherNewDeviceActivity extends BaseActivity 
    implements CommontClickListener, View.OnClickListener {
    
    private final static String TAG = LiherNewDeviceActivity.class.getSimpleName();
    
    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_new_device; // 修改布局ID
    }
}
```

**步骤3: 定义设备专属常量**
```java
// Constant.java中添加
public static final int Param_NewDevice_Read = 6000;
public static final int Return_NewDevice_Read = 6001;
public static final int Param_Write_NewDevice_Param = 6002;
public static final int Return_Write_NewDevice_Param = 6003;

public static final int Dialog_NewDevice_Param1 = 7000;
public static final int Dialog_NewDevice_Param2 = 7001;
```

**步骤4: 修改数据解析逻辑**
```java
case Constant.Return_NewDevice_Read:
    String[] details = msgContext.split("#", -1);
    if (details.length >= 期望字段数) {
        // 解析各字段
        param1 = Integer.parseInt(details[0]);
        param2 = DoubleUtils.div(Integer.parseInt(details[1]), 100d, 2);
        // ... 更新UI
    }
    break;
```

**步骤5: 添加参数读写**
```java
// 读取
case R.id.tv_param1:
    sendLHBleData("Read Param1", Constant.Param_NewDevice_Read);
    break;

// 写入
case Constant.Dialog_NewDevice_Param1:
    String value = (String) target;
    if (StringUtils.isInt(value)) {
        String formatted = StringUtils.formatString(Integer.parseInt(value), 4);
        sendLHBleData("Write Param1=" + formatted, Constant.Param_Write_NewDevice_Param);
    }
    break;
```

**步骤6: 更新UI绑定**
```java
@Override
protected void initView() {
    super.initView();
    
    // 绑定UI控件
    tvParam1 = findViewById(R.id.tv_param1);
    tvParam2 = findViewById(R.id.tv_param2);
    
    // 设置点击事件
    tvParam1.setOnClickListener(this);
    tvParam2.setOnClickListener(this);
}
```

### 25. 最小可用示例

```java
public class LiherSimpleDeviceActivity extends BaseActivity 
    implements CommontClickListener, View.OnClickListener {
    
    private String mDeviceName;
    private String mDeviceAddress;
    private WiseBluetoothLe mble;
    private Handler mHandler;
    private static final int MSG_DATA_CHANGE = 0x11;
    private int currentSendType;
    
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;
    private LoadingDialog loadingDialog;
    
    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_simple_device;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        
        mble = WiseBluetoothLe.getInstance(getApplicationContext());
        mble.mBleCallBack = bleCallBack;
        
        initHandler();
        initDialog();
        
        // 延迟查询
        RxTimerUtil.timer(500L, number -> {
            sendLHBleData("Read Info", Constant.Param_Read_Detail);
        });
    }
    
    private void initHandler() {
        mHandler = new Handler(msg -> {
            if (msg.what == MSG_DATA_CHANGE) {
                analysisMsg(msg.arg1, msg.arg2, (String) msg.obj);
            }
            return false;
        });
    }
    
    private void initDialog() {
        loadingDialog = new LoadingDialog.Builder(this)
            .setCancelable(true)
            .setMessage("加载中...")
            .create();
    }
    
    public void sendLHBleData(String sendString, int sendType) {
        if (StringUtils.isEmpty(sendString)) {
            showMsg(2, "发送内容为空");
            return;
        }
        
        if (mble == null || !mble.isConnect()) {
            showMsg(2, "蓝牙未连接");
            finish();
            return;
        }
        
        loadingDialog.show();
        addLogText(sendString, 0, sendType);
        
        byte[] bytes = ProtocolUtil.getBlueToothMsgForSend(
            HJBleApplication.shareInstance().getDesID(), 
            sendString
        );
        
        if (bytes == null) return;
        
        currentSendType = sendType;
        
        new Thread(() -> {
            if (!mble.sendData(mSendCharact, bytes)) {
                addLogText("发送失败！", 0, Constant.Return_Ble_SendFail);
            } else if (!mble.waitReturn(mReceiveCharact, 15000)) {
                addLogText("响应超时", 1, Constant.Return_BLE_False);
            }
        }).start();
    }
    
    WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
        @Override
        public void OnWiseBluetoothState(int state) {
            if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
                if (loadingDialog != null) loadingDialog.dismiss();
                finish();
            }
        }
        
        @Override
        public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {
            if (characteristic.equals(mReceiveCharact)) {
                if (loadingDialog != null) loadingDialog.dismiss();
                
                String str = ProtocolUtil.getReturnMsgForReceive(recvData);
                
                // 根据currentSendType确定接收类型
                int receiveType = 0;
                switch (currentSendType) {
                    case Constant.Param_Read_Detail:
                        receiveType = Constant.Return_Read_Detail;
                        break;
                    default:
                        return;
                }
                
                addLogText(str, 1, receiveType);
            }
        }
        
        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {}
    };
    
    void addLogText(final String log, int type, int dataType) {
        Message message = new Message();
        message.what = MSG_DATA_CHANGE;
        message.arg1 = type;
        message.arg2 = dataType;
        message.obj = log;
        mHandler.sendMessage(message);
    }
    
    private void analysisMsg(int arg1, int arg2, Object obj) {
        if (arg1 == 1) { // 接收消息
            String msgContext = (String) obj;
            
            switch (arg2) {
                case Constant.Return_Read_Detail:
                    String[] details = msgContext.split("#", -1);
                    if (details.length >= 3) {
                        // 解析并更新UI
                        String version = details[0];
                        String time = details[1];
                        String status = details[2];
                        
                        runOnUiThread(() -> {
                            tvVersion.setText(version);
                            tvTime.setText(time);
                            tvStatus.setText(status);
                        });
                    }
                    break;
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxTimerUtil.cancel();
        mble.disconnectDevice();
    }
}
```

## 常见问题FAQ

### Q1: 如何处理分包数据？
**A**: 使用`ProtocolUtil.getReturnMsgForReceive()`自动处理分包重组。

### Q2: 如何调试BLE通信？
**A**: 
```java
// 启用HEX模式查看原始数据
Logger.d("发送: " + StringUtils.toHexString(sendBytes));
Logger.d("接收: " + StringUtils.toHexString(recvBytes));

// 或使用订单模式直接发送原始指令
```

### Q3: 如何适配不同版本的设备固件？
**A**: 
```java
String version = details[0];
if (StringUtils.isInt(version) && Integer.parseInt(version) >= 新版本号) {
    // 新版解析逻辑
} else {
    // 旧版解析逻辑
}
```

### Q4: 如何处理中文乱码？
**A**: 确保使用`ProtocolUtil.getReturnMsgForReceive()`进行GBK解码。

### Q5: 如何实现批量参数配置？
**A**: 
```java
// 依次发送多个写入命令
sendLHBleData("Write Param1=" + value1, Constant.Param_Write_Param1);
RxTimerUtil.timer(500L, number -> {
    sendLHBleData("Write Param2=" + value2, Constant.Param_Write_Param2);
});
```

## 最佳实践总结

✅ **DO - 推荐做法**:
1. 始终使用`ProtocolUtil`进行协议封装和解析
2. 发送前验证连接状态
3. 使用`RxTimerUtil`管理定时任务
4. 在`onDestroy`中清理资源
5. 所有UI更新在主线程执行
6. **每次代码修改后必须执行编译检查**（见下方详细说明）

---

## 自动编译检查规范

### 重要：每次修改后的强制编译验证

**规则**: 智能体在执行完任何代码修改后，必须自动运行Gradle编译检查一轮，解决编译错误或尝试解决编译错误三轮，方能结束任务。

#### 为什么需要编译检查？

1. **避免遗留问题**：移除控件时可能遗漏相关引用
2. **及时发现错误**：变量名拼写、类型不匹配、方法不存在等
3. **保证代码质量**：确保后续开发基于可编译的代码基础
4. **提升开发效率**：立即发现问题比后续调试更快

#### 编译检查流程

**步骤1: 执行Gradle编译**
```bash
# Windows环境（CMD）
cd E:\AndroidWorkSpace\LHBle\APP_SampleBox

# 推荐：只编译Java代码（更快，约30-60秒）
gradlew.bat compileDebugJavaWithJavac --warning-mode all

# 备选：完整打包（较慢，约2-5分钟，用于最终验证）
gradlew.bat assembleDebug --warning-mode all
```

**步骤2: 分析编译结果**

✅ **编译成功**：
```
BUILD SUCCESSFUL in XXs
XX actionable tasks: XX executed, XX up-to-date
```
→ 任务完成，可以提交代码

❌ **编译失败**：
```
BUILD FAILED in XXs
XX errors found
```
→ 必须修复所有错误后重新编译

**步骤3: 处理常见编译错误**

##### 错误类型1: 找不到符号（Cannot find symbol）
```java
// 错误示例
etElevation.setText("123"); // error: cannot find symbol etElevation

// 原因：控件已删除但代码仍在使用
// 解决：删除或注释掉相关代码
```

##### 错误类型2: 方法未定义（Method undefined）
```java
// 错误示例
adapter.updateColumnVisibility(map); // error: method undefined

// 原因：方法签名错误或方法不存在
// 解决：检查方法名、参数类型是否匹配
```

##### 错误类型3: 类型不匹配（Type mismatch）
```java
// 错误示例
int value = getString(); // error: incompatible types

// 原因：返回类型与接收类型不匹配
// 解决：修正变量类型或转换返回值
```

##### 错误类型4: 资源找不到（Resource not found）
```xml
<!-- 错误示例 -->
android:background="@drawable/shape_not_exist"
<!-- error: resource drawable/shape_not_exist not found -->

<!-- 解决：创建缺失的资源或使用已有资源 -->
```

#### 实际案例演示

**场景**：移除单点测量页面的河底高程、安装高度、曲线系数字段

**修改前检查清单**：
```java
// ❌ 残留代码示例
private EditText etElevation;      // 声明
private EditText etInstallHeight;  // 声明
private EditText etCoeffA, etCoeffB, etCoeffC; // 声明

etElevation = findViewById(R.id.et_elevation);  // 初始化
etElevation.setText(...);  // 使用
```

**修改步骤**：
1. ✅ 删除布局文件中的控件定义
2. ✅ 删除Activity中的成员变量声明
3. ✅ 删除initView()中的findViewById
4. ✅ 删除loadSysInfoToUI()中的数据加载
5. ✅ 删除saveParameters()中的数据保存
6. ✅ **执行编译检查** ← 关键步骤

**编译检查命令**：
```bash
cd E:\AndroidWorkSpace\LHBle\APP_SampleBox
gradlew.bat compileDebugJavaWithJavac
```

**如果编译失败**：
```bash
# 查看错误详情
grep "error:" build_output.txt

# 定位到具体文件和行号
# LiherADCPSingleMeasureActivity.java:275: error: cannot find symbol
#     etElevation.setText(...);
#     ^

# 修复后重新编译
gradlew.bat compileDebugJavaWithJavac --rerun-tasks
```

#### 自动化脚本（可选）

创建 `check_compile.bat` 快速检查：
```batch
@echo off
echo ========================================
echo   Android编译检查
echo ========================================
cd /d E:\AndroidWorkSpace\LHBle\APP_SampleBox
echo 正在编译...
gradlew.bat compileDebugJavaWithJavac --warning-mode all
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ 编译成功！无错误。
) else (
    echo.
    echo ✗ 编译失败！请查看上方错误信息。
)
pause
```

使用方法：
```bash
check_compile.bat
```

#### 编译检查时机

**必须执行编译检查的场景**：
- ✅ 删除了UI控件后
- ✅ 修改了方法签名后
- ✅ 添加了新的依赖后
- ✅ 修改了资源文件后
- ✅ 重构了代码结构后
- ✅ 准备提交代码前

**可选执行编译检查的场景**：
- ⭕ 小幅度的逻辑修改
- ⭕ 仅修改注释或日志
- ⭕ 临时测试代码

#### 注意事项

⚠️ **警告**：
1. 编译检查不能替代功能测试
2. 编译通过不代表逻辑正确
3. 仍需手动测试业务流程
4. 建议在真机/模拟器上验证关键功能

💡 **提示**：
- 首次编译较慢（需下载依赖），后续编译会利用缓存
- 使用 `--warning-mode all` 可以看到更多潜在问题
- 定期执行 `gradlew.bat clean` 清理构建缓存

---

### 完整工作流程示例

```
1. 理解需求 → 2. 分析影响范围 → 3. 执行代码修改 → 
4. 自动编译检查 → 5a. 编译失败 → 修复错误 → 回到步骤4
                                  → 5b. 编译成功或已尝试回到步骤4有3轮依然编译失败 → 6. 完成任务
```

**记住**：编译检查是质量保证的最后一道防线，绝不能省略！