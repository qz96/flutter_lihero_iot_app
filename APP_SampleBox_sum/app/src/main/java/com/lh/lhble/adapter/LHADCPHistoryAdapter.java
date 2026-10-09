package com.lh.lhble.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lh.lhble.Entity.LHADCPHistoryData;
import com.lh.lhble.R;

import java.util.List;
import java.util.Map;

/**
 * LHADCP历史数据适配器
 */
public class LHADCPHistoryAdapter extends BaseAdapter {
    private Context context;
    private List<LHADCPHistoryData> dataList;
    private Map<String, Boolean> columnVisibilityMap;

    public LHADCPHistoryAdapter(Context context, List<LHADCPHistoryData> dataList, Map<String, Boolean> columnVisibilityMap) {
        this.context = context;
        this.dataList = dataList;
        this.columnVisibilityMap = columnVisibilityMap;
    }
    
    /**
     * 更新列字段可见性配置
     */
    public void updateColumnVisibility(Map<String, Boolean> visibilityMap) {
        this.columnVisibilityMap = visibilityMap;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataList == null ? 0 : dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList == null ? null : dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history_data, parent, false);
            holder = new ViewHolder();
            // 初始化所有控件
            holder.llRow1 = convertView.findViewById(R.id.ll_row1);
            holder.llRow2 = convertView.findViewById(R.id.ll_row2);
            holder.llRow3 = convertView.findViewById(R.id.ll_row3);
            holder.llRow4 = convertView.findViewById(R.id.ll_row4);
            holder.llRow5 = convertView.findViewById(R.id.ll_row5);
            holder.llRow6 = convertView.findViewById(R.id.ll_row6);
            
            holder.tvDataFlag = convertView.findViewById(R.id.tv_data_flag);
            holder.tvDataTime = convertView.findViewById(R.id.tv_data_time);
            holder.tvMnNumber = convertView.findViewById(R.id.tv_mn_number);
            holder.tvOperatorName = convertView.findViewById(R.id.tv_operator_name);
            holder.tvVelocity = convertView.findViewById(R.id.tv_velocity);
            holder.tvFlow = convertView.findViewById(R.id.tv_flow);
            holder.tvWaterDepth = convertView.findViewById(R.id.tv_water_depth);
            holder.tvWaterTemp = convertView.findViewById(R.id.tv_water_temp);
            holder.tvArea = convertView.findViewById(R.id.tv_area);
            holder.tvWaterLevel = convertView.findViewById(R.id.tv_water_level);
            holder.tvPitchAngle = convertView.findViewById(R.id.tv_pitch_angle);
            holder.tvRollAngle = convertView.findViewById(R.id.tv_roll_angle);
            holder.tvInstallHeight = convertView.findViewById(R.id.tv_install_height);
            holder.tvRiverbedElevation = convertView.findViewById(R.id.tv_riverbed_elevation);
            holder.tvBatteryVoltage = convertView.findViewById(R.id.tv_battery_voltage);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LHADCPHistoryData data = dataList.get(position);
        
        // 设置数据
        setDataToViews(data, holder);
        
        // 根据配置显示/隐藏字段
        updateVisibility(holder);

        return convertView;
    }
    
    /**
     * 设置数据到视图
     */
    private void setDataToViews(LHADCPHistoryData data, ViewHolder holder) {
        // 数据标识
        holder.tvDataFlag.setText(data.getDataFlagText());
        if ("已上传".equals(data.getDataFlagText())) {
            holder.tvDataFlag.setBackgroundResource(R.drawable.shape_background_green_radiu_10dp);
        } else {
            holder.tvDataFlag.setBackgroundResource(R.drawable.shape_background_blue_55_radiu_5dp);
        }
        
        // 数据时间
        holder.tvDataTime.setText(data.getFormattedTime());
        
        // MN号
        holder.tvMnNumber.setText(data.getMnNumber() != null ? data.getMnNumber() : "--");
        
        // 操作账号
        holder.tvOperatorName.setText(data.getOperatorName() != null ? data.getOperatorName() : "--");
        
        // 流速
        holder.tvVelocity.setText(String.format("%.3f m/s", data.getVelocity()));
        
        // 瞬时流量
        holder.tvFlow.setText(String.format("%.3f m³/s", data.getInstantFlow()));
        
        // 水深
        holder.tvWaterDepth.setText(String.format("%.3f m", data.getWaterDepth()));
        
        // 水温
        holder.tvWaterTemp.setText(String.format("%.1f ℃", data.getWaterTemp()));
        
        // 过水面积
        holder.tvArea.setText(String.format("%.3f m²", data.getArea()));
        
        // 水位
        holder.tvWaterLevel.setText(String.format("%.3f m", data.getWaterLevel()));
        
        // 俯仰角
        holder.tvPitchAngle.setText(String.format("%.1f °", data.getPitchAngle()));
        
        // 横滚角
        holder.tvRollAngle.setText(String.format("%.1f °", data.getRollAngle()));
        
        // 安装高度
        holder.tvInstallHeight.setText(String.format("%.3f m", data.getInstallHeight()));
        
        // 河底高程
        holder.tvRiverbedElevation.setText(String.format("%d mm", data.getRiverbedElevation()));
        
        // 电源电压
        holder.tvBatteryVoltage.setText(String.format("%.2f V", data.getBatteryVoltage()));
    }
    
    /**
     * 根据配置更新字段可见性
     */
    private void updateVisibility(ViewHolder holder) {
        if (columnVisibilityMap == null) {
            return;
        }
        
        // 第一行：数据标识、数据时间
        boolean showFlag = columnVisibilityMap.getOrDefault("数据标识", false);
        boolean showTime = columnVisibilityMap.getOrDefault("数据时间", true);
        holder.tvDataFlag.setVisibility(showFlag ? View.VISIBLE : View.GONE);
        holder.tvDataTime.setVisibility(showTime ? View.VISIBLE : View.GONE);
        holder.llRow1.setVisibility((showFlag || showTime) ? View.VISIBLE : View.GONE);
        
        // 第二行：MN号、操作账号
        boolean showMn = columnVisibilityMap.getOrDefault("MN号", false);
        boolean showOperator = columnVisibilityMap.getOrDefault("操作账号", false);
        holder.tvMnNumber.setVisibility(showMn ? View.VISIBLE : View.GONE);
        holder.tvOperatorName.setVisibility(showOperator ? View.VISIBLE : View.GONE);
        holder.llRow2.setVisibility((showMn || showOperator) ? View.VISIBLE : View.GONE);
        
        // 第三行：流速、瞬时流量、水深
        boolean showVelocity = columnVisibilityMap.getOrDefault("流速(m/s)", true);
        boolean showFlow = columnVisibilityMap.getOrDefault("瞬时流量(m³/s)", true);
        boolean showDepth = columnVisibilityMap.getOrDefault("水深(m)", false);
        holder.tvVelocity.setVisibility(showVelocity ? View.VISIBLE : View.GONE);
        holder.tvFlow.setVisibility(showFlow ? View.VISIBLE : View.GONE);
        holder.tvWaterDepth.setVisibility(showDepth ? View.VISIBLE : View.GONE);
        holder.llRow3.setVisibility((showVelocity || showFlow || showDepth) ? View.VISIBLE : View.GONE);
        
        // 第四行：水温、过水面积、水位
        boolean showTemp = columnVisibilityMap.getOrDefault("水温(℃)", false);
        boolean showArea = columnVisibilityMap.getOrDefault("过水面积(m²)", false);
        boolean showLevel = columnVisibilityMap.getOrDefault("水位(m)", false);
        holder.tvWaterTemp.setVisibility(showTemp ? View.VISIBLE : View.GONE);
        holder.tvArea.setVisibility(showArea ? View.VISIBLE : View.GONE);
        holder.tvWaterLevel.setVisibility(showLevel ? View.VISIBLE : View.GONE);
        holder.llRow4.setVisibility((showTemp || showArea || showLevel) ? View.VISIBLE : View.GONE);
        
        // 第五行：俯仰角、横滚角、安装高度
        boolean showPitch = columnVisibilityMap.getOrDefault("俯仰角(°)", false);
        boolean showRoll = columnVisibilityMap.getOrDefault("横滚角(°)", false);
        boolean showInstall = columnVisibilityMap.getOrDefault("安装高度(m)", false);
        holder.tvPitchAngle.setVisibility(showPitch ? View.VISIBLE : View.GONE);
        holder.tvRollAngle.setVisibility(showRoll ? View.VISIBLE : View.GONE);
        holder.tvInstallHeight.setVisibility(showInstall ? View.VISIBLE : View.GONE);
        holder.llRow5.setVisibility((showPitch || showRoll || showInstall) ? View.VISIBLE : View.GONE);
        
        // 第六行：河底高程、电源电压
        boolean showElevation = columnVisibilityMap.getOrDefault("河底高程(mm)", false);
        boolean showVoltage = columnVisibilityMap.getOrDefault("电源电压(V)", false);
        holder.tvRiverbedElevation.setVisibility(showElevation ? View.VISIBLE : View.GONE);
        holder.tvBatteryVoltage.setVisibility(showVoltage ? View.VISIBLE : View.GONE);
        holder.llRow6.setVisibility((showElevation || showVoltage) ? View.VISIBLE : View.GONE);
    }

    static class ViewHolder {
        // 行容器
        LinearLayout llRow1, llRow2, llRow3, llRow4, llRow5, llRow6;
        
        // 字段控件
        TextView tvDataFlag;
        TextView tvDataTime;
        TextView tvMnNumber;
        TextView tvOperatorName;
        TextView tvVelocity;
        TextView tvFlow;
        TextView tvWaterDepth;
        TextView tvWaterTemp;
        TextView tvArea;
        TextView tvWaterLevel;
        TextView tvPitchAngle;
        TextView tvRollAngle;
        TextView tvInstallHeight;
        TextView tvRiverbedElevation;
        TextView tvBatteryVoltage;
    }
}
