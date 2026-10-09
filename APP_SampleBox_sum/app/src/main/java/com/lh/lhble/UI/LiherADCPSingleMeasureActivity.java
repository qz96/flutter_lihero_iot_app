package com.lh.lhble.UI;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;

import com.lh.lhble.Entity.LHADCPSysInfo;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.LHADCPCountdownDialog;
import com.lh.lhble.R;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * LHADCP单点测量页面
 */
public class LiherADCPSingleMeasureActivity extends BaseActivity implements View.OnClickListener, CommontClickListener {
    private final static String TAG = LiherADCPSingleMeasureActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_SYS_INFO = "SYS_INFO";

    private String mDeviceName;
    private String mDeviceAddress;
    private LHADCPSysInfo sysInfo;

    private WiseBluetoothLe mble;
    private Context context;
    private Handler mHandler;
    private static final int MSG_DATA_CHANGE = 0x11;
    private static final int MSG_TIMER_UPDATE = 0x12;

    private LoadingDialog loadingDialog = null;
    private LHADCPCountdownDialog countdownDialog = null;

    // BLE服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;
    private int currentSendType, currentReceiveType;

    // UI控件 - 运行模式
    private RadioGroup rgRunMode;
    private RadioButton rbAutoMode, rbManualMode, rbOnlineMode, rbMaintenanceMode;
    
    // 自动模式配置
    private LinearLayout llAutoConfig;
    private TextView tvAutoTime,tvAutoStart;
    
    // 手动模式控制
    private LinearLayout llManualControl;
    private Button btnStartMeasure, btnStopMeasure;
    
    // 计时器
    private TextView tvTimer;
    private long manualStartTime = 0;
    private Handler timerHandler;
    
    // 在线模式配置
    private LinearLayout llOnlineConfig;
    private TextView tvSamWait, tvSamTime, tvUpTime, tvStartTime;
    private Button btnViewRealtimeData;
    
    // 参数设置（仅保留断面MN和操作账号）
    private EditText etOperatorName, etSectionMN;
    private Spinner spinnerSectionShape;
    private EditText etSectionParam1, etSectionParam2, etSectionParam3;
    private TextView tvParam1Label, tvParam2Label, tvParam3Label;
    private LinearLayout llSectionParams;
    private Button btnSaveParams;

    // 断面形状选项
    private String[] sectionShapes = {"矩形", "梯形", "V形", "U形", "圆形"};
    private int[] sectionShapeValues = {0, 1, 2, 3, 4};

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_lhadcp_single_measure;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        sysInfo = (LHADCPSysInfo) intent.getParcelableExtra(EXTRAS_SYS_INFO);

        context = this;

        // 初始化BLE
        mble = WiseBluetoothLe.getInstance(getApplicationContext());
        mble.mBleCallBack = bleCallBack;

        // 初始化Handler
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        analysisMsg(msg.arg1, msg.arg2, (String) msg.obj);
                        break;
                    case MSG_TIMER_UPDATE:
                        updateManualTimer();
                        break;
                }
                return false;
            }
        });

        timerHandler = new Handler();

        // 加载设备参数
        if (sysInfo != null) {
            loadSysInfoToUI();
        }
    }

    public static Intent getIntent(Context context, String name, String mac, LHADCPSysInfo info) {
        Intent intent = new Intent(context, LiherADCPSingleMeasureActivity.class);
        intent.putExtra(LiherADCPSingleMeasureActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherADCPSingleMeasureActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherADCPSingleMeasureActivity.EXTRAS_SYS_INFO, info);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();

        // 初始化运行模式控件
        rgRunMode = findViewById(R.id.rg_run_mode);
        rbAutoMode = findViewById(R.id.rb_auto_mode);
        rbManualMode = findViewById(R.id.rb_manual_mode);
        rbOnlineMode = findViewById(R.id.rb_online_mode);
        rbMaintenanceMode = findViewById(R.id.rb_maintenance_mode);
        
        // 自动模式配置
        llAutoConfig = findViewById(R.id.ll_auto_config);
        tvAutoTime = findViewById(R.id.tv_auto_time);
        tvAutoTime.setOnClickListener(this);
        tvAutoStart=findViewById(R.id.tv_auto_start);
        tvAutoStart.setOnClickListener(this);
        // 手动模式控制
        llManualControl = findViewById(R.id.ll_manual_control);
        btnStartMeasure = findViewById(R.id.btn_start_measure);
        btnStopMeasure = findViewById(R.id.btn_stop_measure);
        
        // 计时器
        tvTimer = findViewById(R.id.tv_timer);
        
        // 在线模式配置
        llOnlineConfig = findViewById(R.id.ll_online_config);
        tvSamWait = findViewById(R.id.et_sam_wait);
        tvSamTime = findViewById(R.id.et_sam_time);
        tvUpTime = findViewById(R.id.et_up_time);
        tvStartTime = findViewById(R.id.et_start_time);
        btnViewRealtimeData = findViewById(R.id.btn_view_realtime_data);
        
        // 参数设置（仅保留断面MN和操作账号）
        etOperatorName = findViewById(R.id.et_operator_name);
        etSectionMN = findViewById(R.id.et_section_mn);
        spinnerSectionShape = findViewById(R.id.spinner_section_shape);
        etSectionParam1 = findViewById(R.id.et_section_param1);
        etSectionParam2 = findViewById(R.id.et_section_param2);
        etSectionParam3 = findViewById(R.id.et_section_param3);
        tvParam1Label = findViewById(R.id.tv_param1_label);
        tvParam2Label = findViewById(R.id.tv_param2_label);
        tvParam3Label = findViewById(R.id.tv_param3_label);
        llSectionParams = findViewById(R.id.ll_section_params);
        btnSaveParams = findViewById(R.id.btn_save_params);

        // 设置Spinner适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, sectionShapes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSectionShape.setAdapter(adapter);

        // 设置点击事件（BaseActivity 已提供左上角返回按钮，无需额外设置）
        btnStartMeasure.setOnClickListener(this);
        btnStopMeasure.setOnClickListener(this);
        btnViewRealtimeData.setOnClickListener(this);
        btnSaveParams.setOnClickListener(this);
        
        // 在线模式参数点击事件
        tvSamWait.setOnClickListener(this);
        tvSamTime.setOnClickListener(this);
        tvUpTime.setOnClickListener(this);
        tvStartTime.setOnClickListener(this);

        // 运行模式切换监听
        rgRunMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // 先恢复默认UI状态（隐藏所有模式专属控件）
                updateModeUI(checkedId);
                
                // 根据选中的模式发送对应的BLE指令
                if (checkedId == R.id.rb_auto_mode) {
                    // 自动模式：弹出输入框让用户填写持续时间
                    String lastAutoTime = tvAutoTime.getText().toString().trim();
                    if (TextUtils.isEmpty(lastAutoTime)) {
                        lastAutoTime = "60"; // 默认60分钟
                    }
                    showAutoTimeInputDialog(lastAutoTime);
                    
                } else if (checkedId == R.id.rb_manual_mode) {
                    // 手动模式：直接切换
                    sendLHBleData("Write RunMode=1", Constant.Param_Write_RunMode);
                    
                } else if (checkedId == R.id.rb_online_mode) {
                    // 在线模式：只验证参数完整性，不自动发送指令
                    String samWait = tvSamWait.getText().toString().trim();
                    String samTime = tvSamTime.getText().toString().trim();
                    String upTime = tvUpTime.getText().toString().trim();
                    String startTime = tvStartTime.getText().toString().trim();
                    
                    // 验证所有参数都已填写
                    if (TextUtils.isEmpty(samWait) || TextUtils.isEmpty(samTime) || 
                        TextUtils.isEmpty(upTime) || TextUtils.isEmpty(startTime)) {
                        showMsg(2, "请填写完整的在线模式参数");
                        return;
                    }
                    
                    // 验证数值合法性
                    try {
                        int samWaitValue = Integer.parseInt(samWait);
                        int samTimeValue = Integer.parseInt(samTime);
                        int upTimeValue = Integer.parseInt(upTime);
                        
                        if (samWaitValue <= 0 || samTimeValue <= 0 || upTimeValue <= 0) {
                            showMsg(2, "采样等待时间、采数间隔、上传间隔必须大于0");
                            return;
                        }
                        
                        // 验证首次测试时间格式和是否大于当前时间
                        if (startTime.length() != 12) {
                            showMsg(2, "首次测试时间格式不正确");
                            return;
                        }
                        
                        // 解析时间字符串 yyMMddHHmmss
                        int year = Integer.parseInt(startTime.substring(0, 2)) + 2000;
                        int month = Integer.parseInt(startTime.substring(2, 4)) - 1; // Calendar月份从0开始
                        int day = Integer.parseInt(startTime.substring(4, 6));
                        int hour = Integer.parseInt(startTime.substring(6, 8));
                        int minute = Integer.parseInt(startTime.substring(8, 10));
                        int second = Integer.parseInt(startTime.substring(10, 12));
                        
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(year, month, day, hour, minute, second);
                        
                        if (selectedCal.getTimeInMillis() <= System.currentTimeMillis()) {
                            showMsg(2, "首次测试时间必须大于当前时间");
                            return;
                        }
                        
                        // 所有验证通过，显示提示
                        showMsg(0, "在线模式参数验证通过，请点击各项参数进行写入");
                        
                    } catch (NumberFormatException e) {
                        showMsg(2, "参数格式不正确");
                        return;
                    }
                    
                } else if (checkedId == R.id.rb_maintenance_mode) {
                    // 维护模式
                    sendLHBleData("Write RunMode=3", Constant.Param_Write_RunMode);
                }
            }
        });

        // 断面形状选择监听
        spinnerSectionShape.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSectionShapeUI(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 初始化Loading对话框
        LoadingDialog.Builder loadBuilder = new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(HJBleApplication.getAppContext().getString(R.string.loading_normal))
                .setCancelOutside(false);
        loadingDialog = loadBuilder.create();
    }

    /**
     * 加载系统信息到UI
     */
    private void loadSysInfoToUI() {
        // 运行模式
        switch (sysInfo.getRunMode()) {
            case 0: rbAutoMode.setChecked(true); break;
            case 1: rbManualMode.setChecked(true); break;
            case 2: rbOnlineMode.setChecked(true); break;
            case 3: rbMaintenanceMode.setChecked(true); break;
        }
        
        // 自动测试时间
        if (sysInfo.getAutoTestDuration() > 0) {
            tvAutoTime.setText(String.valueOf(sysInfo.getAutoTestDuration()));
        }
        
        // 在线模式参数
        if (sysInfo.getOnlineSampleInterval() > 0) {
            tvSamTime.setText(String.valueOf(sysInfo.getOnlineSampleInterval() / 60)); // 秒转分钟
        }
        if (sysInfo.getOnlineUploadInterval() > 0) {
            tvUpTime.setText(String.valueOf(sysInfo.getOnlineUploadInterval() / 60)); // 秒转分钟
        }
        if (sysInfo.getSamplingWaitTime() > 0) {
            tvSamWait.setText(String.valueOf(sysInfo.getSamplingWaitTime()));
        }
        
        // 参数设置（仅保留断面MN和操作账号）
        if (!StringUtils.isEmpty(sysInfo.getOperatorName())) {
            etOperatorName.setText(sysInfo.getOperatorName());
        }
        if (!StringUtils.isEmpty(sysInfo.getDeviceMN())) {
            etSectionMN.setText(sysInfo.getDeviceMN());
        }
        
        // 断面形状
        spinnerSectionShape.setSelection(sysInfo.getCrossSectionShape());
        updateSectionShapeUI(sysInfo.getCrossSectionShape());
        
        // 断面参数
        if (sysInfo.getSectionParam1() > 0) {
            etSectionParam1.setText(String.valueOf(sysInfo.getSectionParam1()));
        }
        if (sysInfo.getSectionParam2() > 0) {
            etSectionParam2.setText(String.valueOf(sysInfo.getSectionParam2()));
        }
        if (sysInfo.getSectionParam3() > 0) {
            etSectionParam3.setText(String.valueOf(sysInfo.getSectionParam3()));
        }
    }

    /**
     * 更新模式UI显示
     */
    private void updateModeUI(int checkedId) {
        llAutoConfig.setVisibility(View.GONE);
        llManualControl.setVisibility(View.GONE);
        llOnlineConfig.setVisibility(View.GONE);
        btnViewRealtimeData.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);
        
        // 停止计时器
        timerHandler.removeCallbacksAndMessages(null);
        
        if (checkedId == R.id.rb_auto_mode) {
            llAutoConfig.setVisibility(View.VISIBLE);
        } else if (checkedId == R.id.rb_manual_mode) {
            llManualControl.setVisibility(View.VISIBLE);
            btnStartMeasure.setEnabled(true);
            btnStopMeasure.setEnabled(false);
        } else if (checkedId == R.id.rb_online_mode) {
            llOnlineConfig.setVisibility(View.VISIBLE);
            btnViewRealtimeData.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 更新断面形状UI
     */
    private void updateSectionShapeUI(int shapeIndex) {
        // 重置所有参数可见性
        tvParam2Label.setVisibility(View.GONE);
        etSectionParam2.setVisibility(View.GONE);
        tvParam3Label.setVisibility(View.GONE);
        etSectionParam3.setVisibility(View.GONE);
        
        switch (shapeIndex) {
            case 0: // 矩形
                tvParam1Label.setText("宽度（m）");
                break;
            case 1: // 梯形
                tvParam1Label.setText("顶部宽度（m）");
                tvParam2Label.setText("底部宽度（m）");
                tvParam3Label.setText("高度（m）");
                tvParam2Label.setVisibility(View.VISIBLE);
                etSectionParam2.setVisibility(View.VISIBLE);
                tvParam3Label.setVisibility(View.VISIBLE);
                etSectionParam3.setVisibility(View.VISIBLE);
                break;
            case 2: // V形
                tvParam1Label.setText("顶角角度（°）");
                break;
            case 3: // U形
                tvParam1Label.setText("底圆半径（m）");
                break;
            case 4: // 圆形
                tvParam1Label.setText("半径（m）");
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_auto_time:
                String lastAutoTime = tvAutoTime.getText().toString().trim();
                if (TextUtils.isEmpty(lastAutoTime)) {
                    lastAutoTime = "60"; // 默认60分钟
                }
                showAutoTimeInputDialog(lastAutoTime);
                break;
            case R.id.tv_auto_start:
                String mAutoTime = tvAutoTime.getText().toString().trim();
                if (TextUtils.isEmpty(mAutoTime)) {
                    mAutoTime = "60"; // 默认60分钟
                    showAutoTimeInputDialog(mAutoTime);
                }else {
                    new HitiStringDailog(this, Constant.HitiStrTargetDialog_LHADCP_Auto_Start, null, "自动模式", mAutoTime+"秒后，开始测量", LiherADCPSingleMeasureActivity.this);
                }

                 break;
            case R.id.btn_start_measure:
                startManualMeasure();
                break;
            case R.id.btn_stop_measure:
                stopManualMeasure();
                break;
            case R.id.btn_view_realtime_data:
                viewRealtimeData();
                break;
            case R.id.btn_save_params:
                saveParameters();
                break;
            case R.id.et_sam_wait:
                // 采样等待时间输入框
                showNumberInputDialog(Constant.Dailog_Input_SamWait, 
                    tvSamWait.getText().toString(), "采样等待时间（秒）");
                break;
            case R.id.et_sam_time:
                // 采数间隔时间输入框
                showNumberInputDialog(Constant.Dailog_Input_SamTime, 
                    tvSamTime.getText().toString(), "采数间隔时间（分钟）");
                break;
            case R.id.et_up_time:
                // 上传间隔时间输入框
                showNumberInputDialog(Constant.Dailog_Input_UpTime, 
                    tvUpTime.getText().toString(), "上传间隔时间（分钟）");
                break;
            case R.id.et_start_time:
                // 首次测试时间日期选择器
                showDateTimePickerDialog();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 设置标题栏（使用 BaseActivity 的标题栏）
        setTitle("单点测量");
        
        if (!StringUtils.isEmpty(mDeviceAddress)) {
            // 可以在此显示设备地址等信息
        }
    }

    /**
     * 开始手动测量
     */
    private void startManualMeasure() {
        sendLHBleData("Set Start", Constant.Param_Set_Start);
    }

    /**
     * 停止手动测量
     */
    private void stopManualMeasure() {
        sendLHBleData("Set Stop", Constant.Param_Set_Stop);
    }

    /**
     * 查看实时数据
     */
    private void viewRealtimeData() {
        Intent intent = LiherADCPRealtimeDataActivity.getIntent(
            this, mDeviceName, mDeviceAddress, sysInfo);
        startActivity(intent);
    }

    /**
     * 保存参数
     */
    private void saveParameters() {
        // 保存其他通用参数（断面MN、操作账号、断面形状等）
        saveCommonParameters();

    }

    /**
     * 显示自动测试持续时间输入对话框
     * @param defaultValue 默认值（上次填写的值）
     */
    private void showAutoTimeInputDialog(String defaultValue) {
        new HitiInputStrDailog(this, Constant.Dailog_Input_AutoTime, 
            "自动测试持续时间", "持续时间（秒）", defaultValue,
            new com.lh.lhble.listener.CommontClickListener() {
                @Override
                public void onViewClick(int type, Object target, View view) {
                    if (type == Constant.Dailog_Input_AutoTime) {
                        String content = (String) target;
                        // 验证时间值必须大于0
                        try {
                            int autoTime = Integer.parseInt(content.trim());
                            if (autoTime <= 0) {
                                showMsg(2, "自动测试持续时间必须大于0");
                                return;
                            }
                            
                            // 更新UI显示
                            tvAutoTime.setText(String.valueOf(autoTime));
                            
                            // 发送BLE指令设置持续时间，然后切换到自动模式
                            sendLHBleData("Write AutoTime=" + autoTime, Constant.Param_Write_AutoTime);
                        } catch (NumberFormatException e) {
                            showMsg(2, "请输入有效的数字");
                        }
                    }
                }
            });
    }

    /**
     * 显示数字输入对话框（通用）
     * @param dialogType 对话框类型
     * @param defaultValue 默认值
     * @param fieldName 字段名称
     */
    private void showNumberInputDialog(int dialogType, String defaultValue, String fieldName) {
        if (TextUtils.isEmpty(defaultValue)) {
            defaultValue = "";
        }
        new HitiInputStrDailog(this, dialogType, 
            "输入" + fieldName, fieldName, defaultValue, 
            new com.lh.lhble.listener.CommontClickListener() {
                @Override
                public void onViewClick(int type, Object target, View view) {
                    if (type == dialogType) {
                        String content = (String) target;
                        // 验证值必须大于0
                        try {
                            int value = Integer.parseInt(content.trim());
                            if (value <= 0) {
                                showMsg(2, fieldName + "必须大于0");
                                return;
                            }
                            
                            // 根据类型更新对应的TextView并发送BLE指令
                            switch (dialogType) {
                                case Constant.Dailog_Input_SamWait:
                                    tvSamWait.setText(String.valueOf(value));
                                    // 立即发送BLE指令
                                    sendLHBleData("Write SamWait=" + value, Constant.Param_Write_SamWait);
                                    break;
                                case Constant.Dailog_Input_SamTime:
                                    tvSamTime.setText(String.valueOf(value));
                                    // 分钟转秒后发送
                                    int samTimeSeconds = value * 60;
                                    sendLHBleData("Write SamTime=" + samTimeSeconds, Constant.Param_Write_SamTime);
                                    break;
                                case Constant.Dailog_Input_UpTime:
                                    tvUpTime.setText(String.valueOf(value));
                                    // 分钟转秒后发送
                                    int upTimeSeconds = value * 60;
                                    sendLHBleData("Write UpTime=" + upTimeSeconds, Constant.Param_Write_UpTime);
                                    break;
                            }
                        } catch (NumberFormatException e) {
                            showMsg(2, "请输入有效的数字");
                        }
                    }
                }
            });
    }

    /**
     * 显示日期时间选择器对话框
     */
    private void showDateTimePickerDialog() {
        // 使用DatePickerDialog和TimePickerDialog组合
        Calendar calendar = Calendar.getInstance();
        // 默认设置为当前时间加1小时
        calendar.add(Calendar.HOUR, 1);
        
        final int[] year = {calendar.get(Calendar.YEAR)};
        final int[] month = {calendar.get(Calendar.MONTH)};
        final int[] day = {calendar.get(Calendar.DAY_OF_MONTH)};
        final int[] hour = {calendar.get(Calendar.HOUR_OF_DAY)};
        final int[] minute = {calendar.get(Calendar.MINUTE)};
        
        // 先显示日期选择器
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                    year[0] = selectedYear;
                    month[0] = selectedMonth;
                    day[0] = selectedDay;
                    
                    // 日期选择完成后，显示时间选择器
                    TimePickerDialog timePickerDialog = new TimePickerDialog(LiherADCPSingleMeasureActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
                                hour[0] = selectedHour;
                                minute[0] = selectedMinute;
                                
                                // 格式化时间为 yyMMddHHmmss
                                String dateTime = String.format(Locale.getDefault(), 
                                    "%02d%02d%02d%02d%02d%02d",
                                    year[0] % 100, // 取年份后两位
                                    month[0] + 1,  // 月份从0开始，需要+1
                                    day[0],
                                    hour[0],
                                    minute[0],
                                    0); // 秒数固定为0
                                
                                // 验证时间必须大于当前时间
                                Calendar selectedCal = Calendar.getInstance();
                                selectedCal.set(year[0], month[0], day[0], hour[0], minute[0], 0);
                                
                                if (selectedCal.getTimeInMillis() <= System.currentTimeMillis()) {
                                    showMsg(2, "首次测试时间必须大于当前时间");
                                    return;
                                }
                                
                                // 在写入首次测试时间前，检查其他3项时间值是否都大于0
                                String samWait = tvSamWait.getText().toString().trim();
                                String samTime = tvSamTime.getText().toString().trim();
                                String upTime = tvUpTime.getText().toString().trim();
                                
                                if (TextUtils.isEmpty(samWait) || TextUtils.isEmpty(samTime) || TextUtils.isEmpty(upTime)) {
                                    showMsg(2, "请先填写采样等待时间、采数间隔和上传间隔");
                                    return;
                                }
                                
                                try {
                                    int samWaitValue = Integer.parseInt(samWait);
                                    int samTimeValue = Integer.parseInt(samTime);
                                    int upTimeValue = Integer.parseInt(upTime);
                                    
                                    if (samWaitValue <= 0 || samTimeValue <= 0 || upTimeValue <= 0) {
                                        showMsg(2, "采样等待时间、采数间隔、上传间隔必须大于0");
                                        return;
                                    }
                                } catch (NumberFormatException e) {
                                    showMsg(2, "参数格式不正确，请重新输入");
                                    return;
                                }
                                
                                // 更新UI显示并发送BLE指令
                                tvStartTime.setText(dateTime);
                                sendLHBleData("Write StartTime=" + dateTime, Constant.Param_Write_StartTime);
                            }
                        }, hour[0], minute[0], true); // 24小时制
                    timePickerDialog.setTitle("选择时间");
                    timePickerDialog.show();
                }
            }, year[0], month[0], day[0]);
        datePickerDialog.setTitle("选择日期");
        // 设置最小日期为今天
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    /**
     * 保存通用参数
     * 根据新需求文档2.3节：仅保留断面MN和操作账号
     * （河底高程、安装高度、安装方向、曲线系数ABC已移至参数设置页面）
     * 
     * ⚠️ 注意：sendLHBleData()不支持并发，必须等上一个指令返回后才能发送下一个
     */
    private void saveCommonParameters() {
        // 构建待发送的参数列表（按顺序）
        List<SaveParamTask> taskList = new ArrayList<>();
        
        // 1. 测量人员
        String operatorName = etOperatorName.getText().toString().trim();
        if (!TextUtils.isEmpty(operatorName)) {
            taskList.add(new SaveParamTask("Write UsrName=" + operatorName, Constant.Param_Write_UsrName));
        }
        
        // 2. 断面MN
        String sectionMN = etSectionMN.getText().toString().trim();
        if (!TextUtils.isEmpty(sectionMN)) {
            taskList.add(new SaveParamTask("Write DevMN=" + sectionMN, Constant.Param_Write_DevMN));
        }
        
        // 3. 断面形状和参数（只要有任意一个参数就添加）
        int shapeIndex = spinnerSectionShape.getSelectedItemPosition();
        String param1 = etSectionParam1.getText().toString().trim();
        String param2 = etSectionParam2.getText().toString().trim();
        String param3 = etSectionParam3.getText().toString().trim();
        
        // 只有当至少有一个参数不为空时，才添加到任务列表
        if (!TextUtils.isEmpty(param1) || !TextUtils.isEmpty(param2) || !TextUtils.isEmpty(param3)) {
            StringBuilder shapeStr = new StringBuilder(shapeIndex + "");
            if (!TextUtils.isEmpty(param1)) {
                shapeStr.append("-").append(param1);
            }
            if (!TextUtils.isEmpty(param2)) {
                shapeStr.append("-").append(param2);
            }
            if (!TextUtils.isEmpty(param3)) {
                shapeStr.append("-").append(param3);
            }
            taskList.add(new SaveParamTask("Write Shape=" + shapeStr.toString(), Constant.Param_Write_Shape));
        }
        
        // 开始链式发送
        if (taskList.isEmpty()) {
            showMsg(0, "没有需要保存的参数");
            return;
        }
        
        // 启动第一个任务
        currentSaveTaskIndex = 0;
        saveParamTasks = taskList;
        sendNextSaveParam();
    }
    
    // 保存参数任务队列
    private List<SaveParamTask> saveParamTasks;
    private int currentSaveTaskIndex;

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position){
            case Constant.HitiStrTargetDialog_LHADCP_Auto_Start:
                // 自动模式：设置时间成功后，开始测量
                sendLHBleData("Write RunMode=0", Constant.Param_Write_RunMode);
                break;
        }
    }

    /**
     * 保存参数任务
     */
    private static class SaveParamTask {
        String command;
        int sendType;
        
        SaveParamTask(String command, int sendType) {
            this.command = command;
            this.sendType = sendType;
        }
    }
    
    /**
     * 发送下一个保存参数任务
     */
    private void sendNextSaveParam() {
        if (currentSaveTaskIndex >= saveParamTasks.size()) {
            // 所有参数保存完成
            showMsg(0, "参数保存成功");
            saveParamTasks = null;
            currentSaveTaskIndex = 0;
            return;
        }
        
        SaveParamTask task = saveParamTasks.get(currentSaveTaskIndex);
        sendLHBleData(task.command, task.sendType);
    }

    /**
     * 发送BLE数据
     */
    public void sendLHBleData(String sendString, int sendType) {
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.empty_send_msg));
            return;
        }

        if (mble == null || !mble.isConnect()) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.ble_close_connect_again));
            finish();
            return;
        }

        loadingDialog.show();
        addLogText(sendString, 0, sendType);

        byte[] bytes = ProtocolUtil.getBlueToothMsgForSend(
            HJBleApplication.shareInstance().getDesID(), 
            sendString
        );

        if (bytes == null) {
            return;
        }

        currentSendType = sendType;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes)) {
                    addLogText(HJBleApplication.getAppContext().getString(R.string.send_fail) + "！", 
                        0, Constant.Return_Ble_SendFail);
                    currentSendType = 0;
                } else if (!mble.waitReturn(mReceiveCharact, 15 * 1000)) {
                    addLogText(getResources().getString(R.string.ble_rtn_timeout), 
                        1, Constant.Return_BLE_False);
                    currentSendType = 0;
                }
            }
        }).start();
    }

    /**
     * BLE回调
     */
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

                String str = "";
                if (HJBleApplication.shareInstance().isBleHex()) {
                    str = ConvertData.bytesToHexString(recvData, false);
                } else if (HJBleApplication.shareInstance().isBleConfig() == 0) {
                    str = ProtocolUtil.getReturnMsgForReceive(recvData);
                } else {
                    str = new String(recvData);
                }

                if (!StringUtils.isEmpty(str)) {
                    // ========== 关键步骤：处理非正常返回数据 ==========
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
                            case Constant.Param_Read_Detail:
                                currentReceiveType = Constant.Return_Read_Detail;
                                break;
                            case Constant.Param_Set_Start:
                            case Constant.Param_Set_Stop:
                            case Constant.Param_Write_RunMode:
                            case Constant.Param_Write_AutoTime:
                            case Constant.Param_Write_SamWait:
                            case Constant.Param_Write_SamTime:  // ⭐ 新增
                            case Constant.Param_Write_UpTime:   // ⭐ 新增
                            case Constant.Param_Write_StartTime: // ⭐ 新增
                            case Constant.Param_Write_UsrName:
                            case Constant.Param_Write_DevMN:
                            case Constant.Param_Write_Shape:
                                currentReceiveType = currentSendType+1; // Write指令返回相同内容表示成功
                                break;
                            default:
                                currentReceiveType = 0;
                                return;
                        }
                        addLogText(str, 1, currentReceiveType);
                    }
                }
            }
        }

        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
            if (!StringUtils.isEmpty(stateMsg)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        
                        int type = 2;
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

    /**
     * 添加日志
     */
    void addLogText(final String log, int type, int dataType) {
        Message message = new Message();
        message.what = MSG_DATA_CHANGE;
        message.arg1 = type;
        message.arg2 = dataType;
        message.obj = log;
        mHandler.sendMessage(message);
    }

    /**
     * 解析消息
     */
    private void analysisMsg(int arg1, int arg2, Object obj) {
        if (arg1 == 0) {
            // 发送的消息
            String msgContext = (String) obj;
            System.out.println("SendString:" + msgContext);
            
        } else if (arg1 == 1) {
            // 接收的消息
            String msgContext = (String) obj;
            
            // Write指令：返回相同内容表示成功
            if (arg2 == Constant.Return_Write_RunMode ||
                arg2 == Constant.Return_Write_AutoTime ||
                arg2 == Constant.Return_Set_Start ||
                arg2 == Constant.Return_Set_Stop) {
                
                if (msgContext.contains("Write RunMode") || msgContext.contains("Set Start") || 
                    msgContext.contains("Set Stop") || msgContext.contains("Write AutoTime")) {
                    handleWriteSuccess(arg2);
                } else {
                    showMsg(1, "操作失败");
                }
            } else if (arg2 == Constant.Return_Write_UsrName ||
                       arg2 == Constant.Return_Write_DevMN ||
                       arg2 == Constant.Return_Write_Shape) {
                // ⭐ 保存通用参数的链式调用
                // Write指令返回相同内容表示成功，直接调用handleWriteSuccess继续下一个任务
                handleWriteSuccess(arg2);
            }
        }
    }

    /**
     * 处理写入成功
     */
    private void handleWriteSuccess(int receiveType) {
        if (receiveType == Constant.Return_Set_Start) {
            // 开始手动测量成功
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnStartMeasure.setEnabled(false);
                    btnStopMeasure.setEnabled(true);
                    tvTimer.setVisibility(View.VISIBLE);
                    manualStartTime = System.currentTimeMillis();
                    startManualTimer();
                    showMsg(0, "开始测量成功");
                }
            });
        } else if (receiveType == Constant.Return_Set_Stop) {
            // 停止手动测量成功
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnStartMeasure.setEnabled(true);
                    btnStopMeasure.setEnabled(false);
                    tvTimer.setVisibility(View.GONE);
                    timerHandler.removeCallbacksAndMessages(null);
                    
                    // 跳转到实时数据面板
                    viewRealtimeData();
                    showMsg(0, "停止测量成功");
                }
            });
        } else if (receiveType == Constant.Return_Write_RunMode) {
            // 模式切换成功，显示提示
            showMsg(0, "模式切换成功");
            
            // 检查是否是切换到自动模式
            if (rbAutoMode.isChecked()) {
                String autoTimeStr = tvAutoTime.getText().toString().trim();
                if (!TextUtils.isEmpty(autoTimeStr)) {
                    int duration = Integer.parseInt(autoTimeStr);
                    showCountdownDialog(duration);
                }
            }
        } else if (receiveType == Constant.Return_Write_AutoTime) {
            new HitiStringDailog(this, Constant.HitiStrTargetDialog_LHADCP_Auto_Start, null, "自动模式", "设置时间成功后，开始测量", LiherADCPSingleMeasureActivity.this);

        } else if (receiveType == Constant.Return_Write_SamWait) {
            // 在线模式：采样等待时间设置成功后，继续设置其他参数
            String samTime = tvSamTime.getText().toString().trim();
            String upTime = tvUpTime.getText().toString().trim();
            String startTime = tvStartTime.getText().toString().trim();
            
            // 分钟转秒
            int samTimeSeconds = Integer.parseInt(samTime) * 60;
            int upTimeSeconds = Integer.parseInt(upTime) * 60;
            
            // 继续发送下一个参数
            sendLHBleData("Write SamTime=" + samTimeSeconds, Constant.Param_Write_SamTime);
        } else if (receiveType == Constant.Return_Write_SamTime) {
            // 在线模式：采数间隔设置成功后，继续设置上传间隔
            String upTime = tvUpTime.getText().toString().trim();
            int upTimeSeconds = Integer.parseInt(upTime) * 60;
            
            sendLHBleData("Write UpTime=" + upTimeSeconds, Constant.Param_Write_UpTime);
        } else if (receiveType == Constant.Return_Write_UpTime) {
            // 在线模式：上传间隔设置成功后，不自动继续，等待用户手动设置首次测试时间
            showMsg(0, "上传间隔设置成功，请设置首次测试时间");
        } else if (receiveType == Constant.Return_Write_StartTime) {
            // 在线模式：首次测试时间设置成功后，自动切换到在线模式
            sendLHBleData("Write RunMode=2", Constant.Param_Write_RunMode);
        } else if (receiveType == Constant.Return_Write_UsrName ||
                receiveType == Constant.Return_Write_DevMN ||
                receiveType == Constant.Return_Write_Shape) {
            // ⭐ 保存通用参数的链式调用
            if (saveParamTasks != null && currentSaveTaskIndex < saveParamTasks.size()) {
                // 当前任务完成，发送下一个
                currentSaveTaskIndex++;
                sendNextSaveParam();
            }
        }
    }

    /**
     * 显示倒计时对话框（自动模式）
     */
    private void showCountdownDialog(int duration) {
        countdownDialog = new LHADCPCountdownDialog(this, duration, new LHADCPCountdownDialog.OnCountdownFinishListener() {
            @Override
            public void onFinish() {
                // 倒计时结束，跳转到实时数据面板
                viewRealtimeData();
            }
        });
    }

    /**
     * 启动手动计时器
     */
    private void startManualTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(MSG_TIMER_UPDATE);
                timerHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    /**
     * 更新手动计时器
     */
    private void updateManualTimer() {
        long elapsed = System.currentTimeMillis() - manualStartTime;
        int seconds = (int) (elapsed / 1000);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        tvTimer.setText(timeStr);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxTimerUtil.cancel();
        timerHandler.removeCallbacksAndMessages(null);
        if (countdownDialog != null && countdownDialog.isShowing()) {
            countdownDialog.dismiss();
        }

    }
}
