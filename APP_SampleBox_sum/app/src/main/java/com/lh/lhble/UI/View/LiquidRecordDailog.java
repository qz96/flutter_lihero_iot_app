package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.base.Constant;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.listener.CommontClickListener;


public class LiquidRecordDailog {
    private Context mContext;
    private TextView tvTitle, tvOk, tvCancel;
    private TextView tvLiquidResetTime,tvLiquidOutTime;
    protected Dialog dialog;
    private CommontClickListener mListener;
    private int dialogType;
    private long currentResetTime,currentOutTime,tempResetTime,tempOutTime;
    public LiquidRecordDailog(Context context, String dlg_title, int currentType,long  currentResetTime,long currentOutTime,CommontClickListener listener) {
        this.mContext = context;
        this.mListener=listener;
        this.dialogType=currentType;
        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_liquid_record, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);

        tvLiquidOutTime=view.findViewById(R.id.tv_liquid_out_time);
        tvLiquidResetTime=view.findViewById(R.id.tv_liquid_reset_time);
        tvTitle.setText(dlg_title);
        this.currentResetTime=currentResetTime>0?currentResetTime:(System.currentTimeMillis()-60*60*1000L);
        this.currentOutTime=currentOutTime>0?currentOutTime:(System.currentTimeMillis()+60*60*1000L);
        tempOutTime=this.currentOutTime;
        tempResetTime= this.currentResetTime;
        if (this.currentResetTime>0){
            tvLiquidResetTime.setText(DateUtil.long2time(this.currentResetTime));
        }
        if (this.currentOutTime>0){
            tvLiquidOutTime.setText(DateUtil.long2time(this.currentOutTime));
        }
        tvLiquidResetTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onViewClick(Constant.Dialog_Liquid_Reset_Time,LiquidRecordDailog.this,v);
            }
        });
        tvLiquidOutTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onViewClick(Constant.Dialog_Liquid_Out_Time,LiquidRecordDailog.this,v);
            }
        });
        tvOk.setVisibility(View.VISIBLE);
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((tempOutTime==LiquidRecordDailog.this.currentOutTime
                        &&tempResetTime==LiquidRecordDailog.this.currentResetTime)
                        ||tempResetTime<=0 ||tempOutTime<=0){
                    Toast.makeText(mContext,"请输入母液更换时间和过期时间",Toast.LENGTH_LONG).show();
                    return;
                }else if (LiquidRecordDailog.this.tempResetTime>=LiquidRecordDailog.this.tempOutTime){
                    Toast.makeText(mContext,"请输入正确母液更换时间和过期时间",Toast.LENGTH_LONG).show();
                    return;
                }else {
                    mListener.onViewClick(dialogType,tempResetTime+"#"+tempOutTime,v);
                    dialog.cancel();
                }
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.setContentView(view);
        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }

    public long getTempResetTime() {
        return tempResetTime;
    }

    public void setTempResetTime(long tempResetTime) {
        this.tempResetTime = tempResetTime;
    }

    public long getTempOutTime() {
        return tempOutTime;
    }

    public void setTempOutTime(long tempOutTime) {
        this.tempOutTime = tempOutTime;
    }

    public void notifyDataSetChanged(long tempResetTime, long tempOutTime){
        if (tempResetTime>0){
            this.tempResetTime=tempResetTime;
            tvLiquidResetTime.setText(DateUtil.long2time(tempResetTime));
        }else if (tempOutTime>0){
            this.tempOutTime=tempOutTime;
            tvLiquidOutTime.setText(DateUtil.long2time(tempOutTime));
        }
    }

}
