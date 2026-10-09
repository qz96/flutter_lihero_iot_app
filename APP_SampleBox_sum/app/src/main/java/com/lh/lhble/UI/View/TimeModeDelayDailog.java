package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.listener.CommontClickListener;


public class TimeModeDelayDailog {
    private Context mContext;
    private TextView tvTitle, tvOk, tvCancel;
    private EditText edHour,edMin;
    protected Dialog dialog;
    private CommontClickListener mListener;
    private int dialogType;
    public TimeModeDelayDailog(Context context, String dlg_title, int currentType, CommontClickListener listener) {
        this.mContext = context;
        this.mListener=listener;
        this.dialogType=currentType;
        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_box_timemode_delay, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);

        edHour=view.findViewById(R.id.ed_hour);
        edMin=view.findViewById(R.id.ed_min);
        tvTitle.setText(dlg_title);

        tvOk.setVisibility(View.VISIBLE);
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StringUtils.isEmpty(edHour.getText().toString())||Integer.parseInt(edHour.getText().toString().trim())>23
                ||StringUtils.isEmpty(edMin.getText().toString())||Integer.parseInt(edMin.getText().toString().trim())>59){
                    Toast.makeText(mContext,"请输入正确生效时间",Toast.LENGTH_LONG).show();
                    return;
                }else {
                    int min0=Integer.parseInt(DateUtil.long2time11(System.currentTimeMillis()).substring(2,4))*60+Integer.parseInt(DateUtil.long2time11(System.currentTimeMillis()).substring(4,6));
                    int min1=Integer.parseInt(edHour.getText().toString().trim())*60+Integer.parseInt(edMin.getText().toString().trim());

                    if (min0>=min1){
                        if ((min1+1440-min0)<=800){
                            mListener.onViewClick(dialogType,min1+1440-min0,v);
                            dialog.dismiss();
                        }else {
                            Toast.makeText(mContext,"请输入大于当前时间",Toast.LENGTH_LONG).show();
                            return;
                        }

                    }else {
                        mListener.onViewClick(dialogType,min1-min0,v);
                        dialog.dismiss();
                    }
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


}
