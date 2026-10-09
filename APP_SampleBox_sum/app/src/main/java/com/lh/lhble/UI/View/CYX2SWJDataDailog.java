package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.Entity.DX3SWJInfo;
import com.lh.lhble.Entity.NetInfo;
import com.lh.lhble.Entity.RtuSWJInfo;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.listener.CommontClickListener;


public class CYX2SWJDataDailog {

    private Context mContext;
    private TextView tvSWDevStatus,tvSwjWaterLev,tvSwjDepth,tvSwjArea,tvSwjSurfaceV,tvSwjSectionV,tvSwjRateFlow,tvSwjCumulativeFlow,tvSwjScumuFlow,tvSwjNcumuFlow,tvSwjIntervalFlow;
    private TextView tvTitle, tvOk, tvCancel;
    protected Dialog dialog;
    private DX3SWJInfo swjInfo;
    public CYX2SWJDataDailog(Context context, int dailogType, String dlg_title, DX3SWJInfo swjData, CommontClickListener mListener) {
        this.mContext = context;
        if (swjData==null){
            Toast.makeText(mContext,"水位数据读取为空",Toast.LENGTH_LONG).show();
            return;
        }
        this.swjInfo=swjData;
        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_cyx_swj_data, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle =  view.findViewById(R.id.tv_title);
        tvSWDevStatus=view.findViewById(R.id.tv_sw_dev_status);
        tvSwjWaterLev=view.findViewById(R.id.tv_swj_water_lev);
        tvSwjDepth=view.findViewById(R.id.tv_swj_depth);
        tvSwjArea=view.findViewById(R.id.tv_swj_area);
        tvSwjSurfaceV=view.findViewById(R.id.tv_swj_surfacev);
        tvSwjSectionV=view.findViewById(R.id.tv_swj_sectionv);
        tvSwjRateFlow=view.findViewById(R.id.tv_swj_rate_flow);
        tvSwjCumulativeFlow=view.findViewById(R.id.tv_swj_cumulative_flow);
        tvSwjScumuFlow=view.findViewById(R.id.tv_swj_scumu_flow);
        tvSwjNcumuFlow=view.findViewById(R.id.tv_swj_ncumu_flow);
        tvSwjIntervalFlow=view.findViewById(R.id.tv_swj_interval_flow);
        tvCancel =  view.findViewById(R.id.tv_cancel);
        tvOk =  view.findViewById(R.id.tv_ok);
        tvTitle.setText(dlg_title);
        tvSWDevStatus.setText(StringUtils.getDX3SWDevStatus(swjInfo.status));
        tvSwjWaterLev.setText(DoubleUtils.round(swjInfo.waterLev,3)+"");
        tvSwjDepth.setText(DoubleUtils.round(swjInfo.depth,3)+"");
        tvSwjArea.setText(DoubleUtils.round(swjInfo.area,3)+"");
        tvSwjSurfaceV.setText(DoubleUtils.round(swjInfo.surfaceV,3)+"");
        tvSwjSectionV.setText(DoubleUtils.round(swjInfo.sectionV,3)+"");
        tvSwjRateFlow.setText(DoubleUtils.round(swjInfo.rateFlow,3)+"");
        tvSwjIntervalFlow.setText(DoubleUtils.round(swjInfo.intervalFlow,3)+"");
        String cumulativeFlow = (swjInfo.cumuFlowNum > 0 ? swjInfo.cumuFlowNum + "*10^7+" : "") + (swjInfo.cumulativeFlow != Constant.DefaultValue ? DoubleUtils.round(swjInfo.cumulativeFlow, 3) + "" : "");
        tvSwjCumulativeFlow.setText(cumulativeFlow);
        String scumuFlow = (swjInfo.scumuFlowNum > 0 ? swjInfo.scumuFlowNum + "*10^7+" : "") + (swjInfo.scumuFlow != Constant.DefaultValue ? DoubleUtils.round(swjInfo.scumuFlow, 3) + "" : "");
        tvSwjScumuFlow.setText(scumuFlow);
        String ncumuFlow = (swjInfo.ncumuFlowNum > 0 ? "-"+swjInfo.ncumuFlowNum + "*10^7+" : "") + (swjInfo.ncumuFlow != Constant.DefaultValue ? DoubleUtils.round(swjInfo.ncumuFlow, 3) + "" : "");
        tvSwjNcumuFlow.setText(ncumuFlow);



        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,swjData,view);
                dialog.dismiss();
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }


}
