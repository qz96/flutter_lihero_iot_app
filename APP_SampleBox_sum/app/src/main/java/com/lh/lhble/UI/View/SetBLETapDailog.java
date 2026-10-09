package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.lh.lhble.R;
import com.lh.lhble.listener.CommontClickListener;


public class SetBLETapDailog {

    private Context mContext;
    private TextView tvTitle, tvOk, tvCancel;
    private Switch swTapState;
    protected Dialog dialog;

    public SetBLETapDailog(Context context, final int dailogType, final String dlg_title, final CommontClickListener mListener) {
        this.mContext = context;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_set_ble_tap, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        swTapState=view.findViewById(R.id.sw_tap_state);
        tvTitle.setText(dlg_title);
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,swTapState.isChecked()?1:0,view);
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
