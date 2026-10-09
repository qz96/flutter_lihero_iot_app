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

import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.R;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.listener.CommontClickListener;

import java.text.DecimalFormat;
import java.text.Format;


public class BottleSampleInfoDailog {

    private Context mContext;
    private EditText edSampleHour,edSampleMinute,edSampleWater,edSampleDDL;
    private TextView tvTitle, tvOk, tvCancel,tvContent;
    private CheckBox cbSamplePlan;
    protected Dialog dialog;

    public BottleSampleInfoDailog(Context context, final int dailogType, final String dlg_title, String samplePlan,int sampleWater,int sampleDDL, final CommontClickListener mListener) {
        this.mContext = context;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_bottle_sample_info, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        edSampleHour=view.findViewById(R.id.ed_sample_hour);
        edSampleMinute=view.findViewById(R.id.ed_sample_minute);
        edSampleWater=view.findViewById(R.id.ed_sample_water);
        edSampleDDL=view.findViewById(R.id.ed_sample_ddl);
        cbSamplePlan=view.findViewById(R.id.cb_sample_plan);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvContent = (TextView) view.findViewById(R.id.tv_dlg_content);
        tvTitle.setText(dlg_title);
        if (samplePlan==null||samplePlan.length()!=6||!"01".equals(samplePlan.substring(4,6))){
            edSampleHour.setText("");
            edSampleMinute.setText("");
            cbSamplePlan.setChecked(false);
        }else {
            edSampleHour.setText(samplePlan.substring(0,2));
            edSampleMinute.setText(samplePlan.substring(2,4));
            cbSamplePlan.setChecked(true);
        }
        edSampleWater.setText(sampleWater==0?"":(sampleWater+""));
        edSampleDDL.setText(sampleDDL==0?"":(sampleDDL+""));
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jo=new JSONObject();
                if (cbSamplePlan.isChecked()){
                    if (StringUtils.isEmpty(edSampleHour.getText().toString())||Integer.parseInt(edSampleHour.getText().toString())>23 ||
                        StringUtils.isEmpty(edSampleMinute.getText().toString())||Integer.parseInt(edSampleMinute.getText().toString())>59){
                        Toast.makeText(mContext,"请设置正确定时采样时间",Toast.LENGTH_LONG).show();
                        return;
                    }
                    Format f2 = new DecimalFormat("00");
                    String currentSamplePlan=f2.format(Integer.parseInt(edSampleHour.getText().toString()))+f2.format(Integer.parseInt(edSampleMinute.getText().toString()))+"01";
                    if (!currentSamplePlan.equals(samplePlan))jo.put("SamplePlan",currentSamplePlan);
                }else {
                    if (!"000000".equals(samplePlan))jo.put("SamplePlan","000000");
                }
                if (StringUtils.isEmpty(edSampleWater.getText().toString())||Integer.parseInt(edSampleWater.getText().toString())==0){
                   if (sampleWater!=0)jo.put("SampleWater",0);
                }else if (Integer.parseInt(edSampleWater.getText().toString())>500){
                    Toast.makeText(mContext,"请设置<500cm的水位阀值",Toast.LENGTH_LONG).show();
                    return;
                }else {
                    if (sampleWater!=Integer.parseInt(edSampleWater.getText().toString()))jo.put("SampleWater",Integer.parseInt(edSampleWater.getText().toString()));
                }
                if (StringUtils.isEmpty(edSampleDDL.getText().toString())||Integer.parseInt(edSampleDDL.getText().toString())==0){
                    if (sampleDDL!=0)jo.put("SampleDDL",0);
                }else if (Integer.parseInt(edSampleDDL.getText().toString())>5000){
                    Toast.makeText(mContext,"请设置<5000uS的电导阀值",Toast.LENGTH_LONG).show();
                    return;
                }else {
                    if (sampleDDL!=Integer.parseInt(edSampleDDL.getText().toString()))jo.put("SampleDDL",Integer.parseInt(edSampleDDL.getText().toString()));
                }
                mListener.onViewClick(dailogType,jo,view);
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
