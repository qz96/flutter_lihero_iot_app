package com.lh.lhble.UI;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lh.lhble.Entity.LHADCPHistoryData;
import com.lh.lhble.Entity.LHADCPSysInfo;
import com.lh.lhble.R;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.adapter.LHADCPHistoryAdapter;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LHADCP历史数据页面
 */
public class LiherADCPHistoryActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = LiherADCPHistoryActivity.class.getSimpleName();

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

    private LoadingDialog loadingDialog = null;

    // BLE服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;
    private int currentSendType, currentReceiveType;

    // UI控件
    private ImageButton fabColumnSelect;
    private ImageButton fabExportCsv;
    private SwipeRefreshLayout swipeRefresh;
    private ListView lvHistoryData;
    private TextView tvEmpty;
    private TextView tvLoadMore;

    // 数据列表
    private List<LHADCPHistoryData> historyDataList;
    private LHADCPHistoryAdapter adapter;
    
    // 分页参数
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    // 列字段配置
    private Map<String, Boolean> columnVisibilityMap;
    private static final String[] ALL_COLUMNS = {
        "数据标识", "数据时间", "MN号", "操作账号",
        "流速(m/s)", "瞬时流量(m³/s)", "水深(m)", "水位(m)",
        "过水面积(m²)", "水温(℃)", "俯仰角(°)", "横滚角(°)",
        "安装高度(m)", "河底高程(mm)", "电源电压(V)"
    };

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_lhadcp_history;
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
                }
                return false;
            }
        });

        // 初始化数据列表
        historyDataList = new ArrayList<>();
        
        // 初始化列字段可见性配置（默认只显示：数据时间、流速、瞬时流量）
        columnVisibilityMap = new HashMap<>();
        for (String column : ALL_COLUMNS) {
            columnVisibilityMap.put(column, false);
        }
        columnVisibilityMap.put("数据时间", true);
        columnVisibilityMap.put("流速(m/s)", true);
        columnVisibilityMap.put("瞬时流量(m³/s)", true);
        
        // 加载第一页数据
        loadHistoryData(1);
    }

    public static Intent getIntent(Context context, String name, String mac, LHADCPSysInfo info) {
        Intent intent = new Intent(context, LiherADCPHistoryActivity.class);
        intent.putExtra(LiherADCPHistoryActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherADCPHistoryActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherADCPHistoryActivity.EXTRAS_SYS_INFO, info);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();

        // 初始化UI控件
        fabColumnSelect = findViewById(R.id.fab_column_select);
        fabExportCsv = findViewById(R.id.fab_export_csv);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        lvHistoryData = findViewById(R.id.lv_history_data);
        tvEmpty = findViewById(R.id.tv_empty);
        tvLoadMore = findViewById(R.id.tv_load_more);

        // 设置点击事件（BaseActivity已提供左上角返回按钮）
        fabColumnSelect.setOnClickListener(this);
        fabExportCsv.setOnClickListener(this);

        // 设置适配器
        adapter = new LHADCPHistoryAdapter(this, historyDataList, columnVisibilityMap);
        lvHistoryData.setAdapter(adapter);

        // 下拉刷新
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });
        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        // 上拉加载更多
        lvHistoryData.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {
                if (scrollState == android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    // 滚动停止时，检查是否滚动到底部
                    if (view.getLastVisiblePosition() == view.getCount() - 1 && 
                        !isLoading && hasMoreData) {
                        loadMoreData();
                    }
                }
            }

            @Override
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem, 
                               int visibleItemCount, int totalItemCount) {
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
     * 加载历史数据
     */
    private void loadHistoryData(int page) {
        if (isLoading) {
            return;
        }
        
        isLoading = true;
        currentPage = page;
        
        if (page == 1) {
            swipeRefresh.setRefreshing(true);
        } else {
            tvLoadMore.setVisibility(View.VISIBLE);
            tvLoadMore.setText("加载中...");
        }
        
        sendLHBleData("Read History=" + page, Constant.Param_LHADCP_Read_History);
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        historyDataList.clear();
        hasMoreData = true;
        loadHistoryData(1);
    }

    /**
     * 加载更多数据
     */
    private void loadMoreData() {
        loadHistoryData(currentPage + 1);
    }

    /**
     * 显示列字段选择对话框
     */
    private void showColumnSelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_column_select, null);
        builder.setView(dialogView);
        
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        
        LinearLayout llCheckboxes = dialogView.findViewById(R.id.ll_checkboxes);
        Button btnSelectAll = dialogView.findViewById(R.id.btn_select_all);
        Button btnCancelAll = dialogView.findViewById(R.id.btn_cancel_all);
        
        // 动态创建复选框
        final Map<String, CheckBox> checkBoxMap = new HashMap<>();
        for (String column : ALL_COLUMNS) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(column);
            checkBox.setTextSize(14);
            checkBox.setPadding(20, 15, 20, 15);
            checkBox.setChecked(columnVisibilityMap.get(column));
            llCheckboxes.addView(checkBox);
            checkBoxMap.put(column, checkBox);
        }
        
        // 全选按钮
        btnSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (CheckBox checkBox : checkBoxMap.values()) {
                    checkBox.setChecked(true);
                }
            }
        });
        
        // 取消全选按钮
        btnCancelAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (CheckBox checkBox : checkBoxMap.values()) {
                    checkBox.setChecked(false);
                }
            }
        });
        
        // 确定按钮（点击对话框外部自动保存）
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                // 保存用户选择
                for (Map.Entry<String, CheckBox> entry : checkBoxMap.entrySet()) {
                    columnVisibilityMap.put(entry.getKey(), entry.getValue().isChecked());
                }
                
                // 刷新列表显示（通过适配器更新可见性）
                adapter.updateColumnVisibility(columnVisibilityMap);
                
                // 提示用户
                int visibleCount = 0;
                for (Boolean visible : columnVisibilityMap.values()) {
                    if (visible) visibleCount++;
                }
                showMsg(0, "已选择显示 " + visibleCount + " 个字段");
            }
        });
        
        if (!this.isFinishing()) {
            dialog.show();
        }
    }

    /**
     * 导出CSV文件
     */
    private void exportToCSV() {
        if (historyDataList.isEmpty()) {
            showMsg(2, "暂无数据可导出");
            return;
        }

        try {
            // 生成文件名：最旧数据时间#导出时间戳.csv
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String exportTime = sdf.format(new Date());
            
            String oldestDataTime = "";
            if (!historyDataList.isEmpty()) {
                oldestDataTime = historyDataList.get(historyDataList.size() - 1).getDataTime();
            }
            
            String fileName = oldestDataTime + "#" + exportTime + ".csv";
            
            // 创建保存目录
            File exportDir = new File(getExternalFilesDir(null), "LHADCP_Export");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            File csvFile = new File(exportDir, fileName);
            
            // 写入CSV文件
            FileWriter writer = new FileWriter(csvFile);
            
            // 写入表头
            writer.append("数据标识,数据时间,MN号,操作账号,流速(m/s),瞬时流量(m³/s),")
                  .append("水深(m),水位(m),过水面积(m²),水温(℃),俯仰角(°),横滚角(°),")
                  .append("安装高度(m),河底高程(mm),电源电压(V)\n");
            
            // 写入数据
            for (LHADCPHistoryData data : historyDataList) {
                writer.append(data.getDataFlagText()).append(",");
                writer.append(data.getFormattedTime()).append(",");
                writer.append(data.getMnNumber()).append(",");
                writer.append(data.getOperatorName()).append(",");
                writer.append(String.valueOf(data.getVelocity())).append(",");
                writer.append(String.valueOf(data.getInstantFlow())).append(",");
                writer.append(String.valueOf(data.getWaterDepth())).append(",");
                writer.append(String.valueOf(data.getWaterLevel())).append(",");
                writer.append(String.valueOf(data.getArea())).append(",");
                writer.append(String.valueOf(data.getWaterTemp())).append(",");
                writer.append(String.valueOf(data.getPitchAngle())).append(",");
                writer.append(String.valueOf(data.getRollAngle())).append(",");
                writer.append(String.valueOf(data.getInstallHeight())).append(",");
                writer.append(String.valueOf(data.getRiverbedElevation())).append(",");
                writer.append(String.valueOf(data.getBatteryVoltage()));
                writer.append("\n");
            }
            
            writer.flush();
            writer.close();
            
            showMsg(0, "导出成功！\n文件位置：" + csvFile.getAbsolutePath());
            
        } catch (IOException e) {
            e.printStackTrace();
            showMsg(2, "导出失败：" + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 设置标题栏（使用 BaseActivity 的标题栏）
        setTitle("历史数据");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_column_select:
                showColumnSelectDialog();
                break;
            case R.id.fab_export_csv:
                exportToCSV();
                break;
        }
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
                        // 情况3：设备正在返回数据，需要等待后续数据包（历史数据可能很大）
                        addLogText("蓝牙消息返回中", 1, Constant.Return_BLE_Continue);
                        
                    } else if (str.startsWith(Constant.SendDataListFalse)) {
                        // 情况4：发送的分包数据有误，提取错误信息
                        String errorMsg = str.substring(Constant.SendDataListFalse.length());
                        addLogText(errorMsg, 1, Constant.Return_Ble_ReceiveFalse);
                        
                    } else {
                        // ========== 正常数据：根据发送类型确定接收类型 ==========
                        switch (currentSendType) {
                            case Constant.Param_LHADCP_Read_History:
                                currentReceiveType = Constant.Return_LHADCP_Read_History;
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
    private void analysisMsg(int sendType, int receiveType, String msgContext) {
        if (receiveType == Constant.Return_LHADCP_Read_History) {
            parseHistoryData(msgContext);
        }
    }

    /**
     * 解析历史数据
     */
    private void parseHistoryData(String msgContext) {
        try {
            // 数据格式：不同数据组以"#"隔开，每组内以"-"分隔
            String[] groups = msgContext.split("#", -1);
            
            if (groups.length == 0 || TextUtils.isEmpty(groups[0])) {
                // 没有更多数据
                hasMoreData = false;
                showMsg(0, "已加载全部数据");
                return;
            }

            List<LHADCPHistoryData> newDataList = new ArrayList<>();
            
            for (String group : groups) {
                if (TextUtils.isEmpty(group)) {
                    continue;
                }
                
                String[] fields = group.split("-", -1);
                
                if (fields.length < 15) {
                    continue;
                }
                
                LHADCPHistoryData data = new LHADCPHistoryData();
                
                // （1）数据标识
                data.setDataFlag(fields[0]);
                
                // （2）数据时间
                data.setDataTime(fields[1]);
                
                // （3）MN号
                data.setMnNumber(fields[2]);
                
                // （4）操作的蓝牙账号
                data.setOperatorName(fields[3]);
                
                // （5）流速
                if (StringUtils.isDouble(fields[4])) {
                    data.setVelocity(Double.parseDouble(fields[4]));
                }
                
                // （6）瞬时流量
                if (StringUtils.isDouble(fields[5])) {
                    data.setInstantFlow(Double.parseDouble(fields[5]));
                }
                
                // （7）水深
                if (StringUtils.isDouble(fields[6])) {
                    data.setWaterDepth(Double.parseDouble(fields[6]));
                }
                
                // （8）水位
                if (StringUtils.isDouble(fields[7])) {
                    data.setWaterLevel(Double.parseDouble(fields[7]));
                }
                
                // （9）过水面积
                if (StringUtils.isDouble(fields[8])) {
                    data.setArea(Double.parseDouble(fields[8]));
                }
                
                // （10）水温
                if (StringUtils.isDouble(fields[9])) {
                    data.setWaterTemp(Double.parseDouble(fields[9]));
                }
                
                // （11）俯仰角
                if (StringUtils.isDouble(fields[10])) {
                    data.setPitchAngle(Double.parseDouble(fields[10]));
                }
                
                // （12）横滚角
                if (StringUtils.isDouble(fields[11])) {
                    data.setRollAngle(Double.parseDouble(fields[11]));
                }
                
                // （13）安装高度
                if (StringUtils.isDouble(fields[12])) {
                    data.setInstallHeight(Double.parseDouble(fields[12]));
                }
                
                // （14）河底高程
                if (StringUtils.isLong(fields[13])) {
                    data.setRiverbedElevation(Long.parseLong(fields[13]));
                }
                
                // （15）电源电压
                if (StringUtils.isDouble(fields[14])) {
                    data.setBatteryVoltage(Double.parseDouble(fields[14]));
                }
                
                newDataList.add(data);
            }
            
            // 更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentPage == 1) {
                        historyDataList.clear();
                    }
                    
                    historyDataList.addAll(newDataList);
                    
                    // 更新UI显示
                    if (historyDataList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        lvHistoryData.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        lvHistoryData.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                    
                    // 停止刷新/加载
                    swipeRefresh.setRefreshing(false);
                    tvLoadMore.setVisibility(View.GONE);
                    isLoading = false;
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    swipeRefresh.setRefreshing(false);
                    tvLoadMore.setVisibility(View.GONE);
                    isLoading = false;
                    showMsg(2, "数据解析异常：" + e.getMessage());
                }
            });
        }
    }
}
