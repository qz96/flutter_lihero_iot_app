package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;
import com.lh.lhble.R;
import com.lh.lhble.google.zxing.encoding.EncodingHandler;
import com.lh.lhble.listener.CommontClickListener;


public class HitiVersionDailog {

    private Context mContext;
    private TextView tvTitle, tvOk, tvCancel,tvContent;
    protected Dialog dialog;
    private ImageView ivQcode;

    public HitiVersionDailog(Context context, final int dailogType, final Object target, final String dlg_title, String content,String appUrl, final CommontClickListener mListener) {
        this.mContext = context;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_apk_qcode, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvContent = (TextView) view.findViewById(R.id.tv_app_version_info);
        ivQcode=view.findViewById(R.id.iv_app_qcode);
        tvTitle.setText(dlg_title);
        tvContent.setText(content);
        try {
            ivQcode.setImageBitmap(EncodingHandler.createQRCode(appUrl,360));
        } catch (WriterException e) {
            tvContent.setText("生成二维码失败");
            e.printStackTrace();
        }

        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,null,view);
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
