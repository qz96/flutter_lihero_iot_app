package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.R;
import com.lh.lhble.listener.CommontClickListener;


public class HitiStringDailog {

    private Context mContext;
    private TextView tvTitle, tvOk, tvCancel,tvContent;
    protected Dialog dialog;
    private int cancelCount;//点击退出按钮的次数
    /**
     * 构造方法
     *
     * @param context 上下文
     * @param dailogType 对话框类型
     * @param target 目标对象
     * @param dlg_title 标题
     * @param content 内容
     * @param mListener 点击监听
     */
    public HitiStringDailog(Context context, final int dailogType, final Object target, final String dlg_title, String content, final CommontClickListener mListener) {
        this.mContext = context;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_hiti_str, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvContent = (TextView) view.findViewById(R.id.tv_dlg_content);
        tvTitle.setText(dlg_title);
        tvContent.setText(content);
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,target,view);
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

    /**
     * 构造方法
     *
     * @param context 上下文
     * @param dailogType 对话框类型
     * @param myCancelCount 点击退出按钮的次数
     * @param target 目标对象
     * @param dlg_title 标题
     * @param content 内容
     * @param mListener 点击监听
     */
    public HitiStringDailog(Context context, final int dailogType,int myCancelCount, final Object target, final String dlg_title, String content, final CommontClickListener mListener) {
        this.mContext = context;
        if (myCancelCount>0)cancelCount=myCancelCount;
        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_hiti_str, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvContent = (TextView) view.findViewById(R.id.tv_dlg_content);
        tvTitle.setText(dlg_title);
        tvContent.setText(content);
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,target,view);
                dialog.dismiss();
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cancelCount>0){
                    Toast.makeText(context,"再按"+cancelCount+"次，确认退出",Toast.LENGTH_LONG).show();
                    cancelCount--;
                }else {
                    dialog.dismiss();
                }

            }
        });
        dialog.setContentView(view);
        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }


    public HitiStringDailog(Context context, final int dailogType, final Object target, final String dlg_title, String content,int contentColor, final CommontClickListener mListener) {
        this.mContext = context;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_hiti_str, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvContent = (TextView) view.findViewById(R.id.tv_dlg_content);
        tvTitle.setText(dlg_title);
        tvContent.setText(content);
        tvContent.setTextColor(context.getResources().getColor(contentColor));
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,target,view);
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
