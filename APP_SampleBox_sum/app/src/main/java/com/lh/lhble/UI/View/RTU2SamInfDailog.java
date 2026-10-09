package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.Entity.DX3WcsInfo;
import com.lh.lhble.Entity.RtuLljInfo;
import com.lh.lhble.Entity.SampleBox;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ScreenUtils;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.listener.CommontClickListener;


public class RTU2SamInfDailog {

    private Context mContext;
    private LinearLayout llWaterLev,llDP,llRateFlow,llCumulativeFlow,llPH,llDo,llEC,llTb,llORP,llRain,llFZKGState;
    private TextView  tvWaterLev,tvDP,tvRateFlow,tvCumulativeFlow,tvPH,tvDO,tvEC,tvTb, tvORP,tvRain,tvFZKGState;
    private TextView tvTitle, tvOk, tvCancel;
    protected Dialog dialog;
    private RtuLljInfo rtuLljInfo;
    public RTU2SamInfDailog(Context context, int dailogType, String dlg_title, SampleBox sampleBox,RtuLljInfo lljInfo, CommontClickListener mListener) {
        this.mContext = context;
        if (lljInfo==null){
            Toast.makeText(mContext,"参数数据读取为空",Toast.LENGTH_LONG).show();
            return;
        }
        this.rtuLljInfo=lljInfo;
        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_rtu_saminf, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle =  view.findViewById(R.id.tv_title);
        tvWaterLev=view.findViewById(R.id.tv_water_lev);
        tvRateFlow=view.findViewById(R.id.tv_rate_flow);
        tvDP=view.findViewById(R.id.tv_depth);
        tvPH=view.findViewById(R.id.tv_ph);
        tvDO=view.findViewById(R.id.tv_do);
        tvEC=view.findViewById(R.id.tv_ec);
        tvTb=view.findViewById(R.id.tv_tb);
        tvCumulativeFlow=view.findViewById(R.id.tv_cumulative_flow);
        tvORP=view.findViewById(R.id.tv_orp);
        tvRain=view.findViewById(R.id.tv_rain);
        llWaterLev=view.findViewById(R.id.ll_water_lev);
        llDP=view.findViewById(R.id.ll_water_depth);
        llRateFlow=view.findViewById(R.id.ll_rate_flow);
        llCumulativeFlow=view.findViewById(R.id.ll_cumulative_flow);
        llPH=view.findViewById(R.id.ll_ph);
        llDo=view.findViewById(R.id.ll_do);
        llEC=view.findViewById(R.id.ll_ec);
        llTb=view.findViewById(R.id.ll_tb);
        llORP=view.findViewById(R.id.ll_orp);
        llRain=view.findViewById(R.id.ll_rain);
        llFZKGState=view.findViewById(R.id.ll_fzkg_state);
        tvFZKGState=view.findViewById(R.id.tv_fzkg_state);
        tvCancel =  view.findViewById(R.id.tv_cancel);
        tvOk =  view.findViewById(R.id.tv_ok);
        tvTitle.setText(dlg_title);
        if (rtuLljInfo.waterLev>Constant.DefaultBigValue){
            llWaterLev.setVisibility(View.VISIBLE);
            tvWaterLev.setText( DoubleUtils.round(rtuLljInfo.waterLev, 3) + ""  );
        }else {
            llWaterLev.setVisibility(View.GONE);
        }
        if (rtuLljInfo.depth>Constant.DefaultBigValue){
            llDP.setVisibility(View.VISIBLE);
            tvDP.setText(DoubleUtils.round(rtuLljInfo.depth, 3) + "");
        }else {
            llDP.setVisibility(View.GONE);
        }
        if (rtuLljInfo.rateFlow>Constant.DefaultBigValue){
            llRateFlow.setVisibility(View.VISIBLE);
            tvRateFlow.setText(DoubleUtils.round(rtuLljInfo.rateFlow, 3) + "" );
        }else {
            llRateFlow.setVisibility(View.GONE);
        }
        if (rtuLljInfo.cumulativeFlow>Constant.DefaultBigValue){
            llCumulativeFlow.setVisibility(View.VISIBLE);
            String cumulativeFlow = (rtuLljInfo.cumuFlowNum > 0 ? rtuLljInfo.cumuFlowNum + "*10^7+" : "") + (rtuLljInfo.cumulativeFlow > Constant.DefaultValue ? String.format("%.3f",DoubleUtils.round(rtuLljInfo.cumulativeFlow, 3)) : "");
            tvCumulativeFlow.setText(cumulativeFlow);
        }else {
            llCumulativeFlow.setVisibility(View.GONE);
        }

        if (rtuLljInfo.ph>Constant.DefaultBigValue){
            llPH.setVisibility(View.VISIBLE);
            tvPH.setText(rtuLljInfo.ph > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.ph, 2) + "" : "");
        }else {
            llPH.setVisibility(View.GONE);
        }
        if (rtuLljInfo.oxygen>Constant.DefaultBigValue){
            llDo.setVisibility(View.VISIBLE);
            tvDO.setText(DoubleUtils.round(rtuLljInfo.oxygen, 2) + "" );
        }else {
            llDo.setVisibility(View.GONE);
        }
        if (rtuLljInfo.conductivity>Constant.DefaultBigValue){
            llEC.setVisibility(View.VISIBLE);
            tvEC.setText(rtuLljInfo.conductivity > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.conductivity, 2) + "" : "");
        }else {
            llEC.setVisibility(View.GONE);
        }
        if (rtuLljInfo.turbidity>Constant.DefaultBigValue){
            llTb.setVisibility(View.VISIBLE);
            tvTb.setText(DoubleUtils.round(rtuLljInfo.turbidity, 2) + "");
        }else {
            llTb.setVisibility(View.GONE);
        }
        if (rtuLljInfo.orp>Constant.DefaultBigValue){
            llORP.setVisibility(View.VISIBLE);
            tvORP.setText(DoubleUtils.round(rtuLljInfo.orp, 2) + "");
        }else {
            llORP.setVisibility(View.GONE);
        }
        if (rtuLljInfo.rain>Constant.DefaultBigValue){
            llRain.setVisibility(View.VISIBLE);
            tvRain.setText(DoubleUtils.round(rtuLljInfo.rain,1)+"");
        }else {
            llRain.setVisibility(View.GONE);
        }
        if (sampleBox!=null&&sampleBox.getFzkgState()>0){
            tvFZKGState.setText("有");
            tvFZKGState.setTextColor(context.getResources().getColor(R.color.color_green_24ccaa));
        }else if (sampleBox!=null&&sampleBox.getFzkgState()==0){
            tvFZKGState.setText("无");
            tvFZKGState.setTextColor(context.getResources().getColor(R.color.color_black_999999));
        }else {
            tvFZKGState.setText("未知");
            tvFZKGState.setTextColor(context.getResources().getColor(R.color.color_black_999999));
        }

        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,rtuLljInfo,view);
                dialog.dismiss();
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.weight = ScreenUtils.getScreenWidth(context) / 10 * 9;
        dialog.setContentView(view, layoutParams);
        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }


}
