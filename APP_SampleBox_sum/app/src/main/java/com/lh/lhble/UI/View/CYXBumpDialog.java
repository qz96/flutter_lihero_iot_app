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


public class CYXBumpDialog {

    private Context mContext;
    private TextView tvTitle, tvDevBump0, tvDevBump1, tvDevBump2,tvCancel;
    protected Dialog dialog;

    public CYXBumpDialog(Context context, final int dailogType, final CommontClickListener mListener) {
        this.mContext = context;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_cyx_bump, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);

        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);

        tvDevBump0 = (TextView) view.findViewById(R.id.tv_dev_bump0);
        tvDevBump1 = (TextView) view.findViewById(R.id.tv_dev_bump1);
        tvDevBump2 = (TextView) view.findViewById(R.id.tv_dev_bump2);
        tvDevBump0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,0,view);
                dialog.dismiss();
            }
        });
        tvDevBump1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,1,view);
                dialog.dismiss();
            }
        });
        tvDevBump2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,2,view);
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
