package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.lh.lhble.base.Constant;
import com.lh.lhble.R;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.listener.CommontClickListener;


public class HitiInputStrDailog {

    private Activity mActivity;
    private ImageView ivScan;
    private TextView tvTitle, tvOk, tvCancel,tvField,tvTips;
    private EditText edContent;
    protected Dialog dialog;

    public HitiInputStrDailog(Activity activity, final int dailogType, final String dlg_title, String field,String content, final CommontClickListener mListener) {
        this.mActivity = activity;

        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_hiti_input_str, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvField = (TextView) view.findViewById(R.id.tv_field_name);
        edContent=view.findViewById(R.id.ed_dlg_content);
        ivScan=view.findViewById(R.id.iv_scan);
        tvTitle.setText(dlg_title);
        tvField.setText(field);
        if (!StringUtils.isEmpty(content))edContent.setText(content.trim());
        if (Constant.Dailog_Input_SampleNo==dailogType)ivScan.setVisibility(View.VISIBLE);
        ivScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onViewClick(Constant.RequestCode_Scan_Add_SampleNo,"",v);

            }
        });
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edContent.getText()!=null&& !StringUtils.isEmpty(edContent.getText().toString())){
                    mListener.onViewClick(dailogType,edContent.getText().toString().trim(),view);
                    dialog.dismiss();
                }else if (Constant.Dailog_BLE_Search==dailogType){
                    mListener.onViewClick(dailogType,"".trim(),view);
                    dialog.dismiss();
                }

            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        if (!mActivity.isFinishing())dialog.show();
       /* if (Constant.Dailog_Input_SampleNo==dailogType){
            mListener.onViewClick(Constant.RequestCode_Scan_Add_SampleNo,"",null);
        }*/
    }
    public HitiInputStrDailog(Activity activity, final int dailogType, final String dlg_title, String field,String content,String tips, CommontClickListener mListener) {
        this.mActivity = activity;

        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_hiti_input_str, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvField = (TextView) view.findViewById(R.id.tv_field_name);
        edContent=view.findViewById(R.id.ed_dlg_content);
        ivScan=view.findViewById(R.id.iv_scan);
        tvTips=view.findViewById(R.id.tv_tips);
        tvTitle.setText(dlg_title);
        tvField.setText(field);
        if (!StringUtils.isEmpty(tips)){
            tvTips.setVisibility(View.VISIBLE);
            tvTips.setText(tips);
        }else {
            tvTips.setVisibility(View.GONE);
        }
        if (!StringUtils.isEmpty(content))edContent.setText(content.trim());
        if (Constant.Dailog_Input_SampleNo==dailogType)ivScan.setVisibility(View.VISIBLE);
        ivScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onViewClick(Constant.RequestCode_Scan_Add_SampleNo,"",v);

            }
        });
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edContent.getText()!=null&& !StringUtils.isEmpty(edContent.getText().toString())){
                    mListener.onViewClick(dailogType,edContent.getText().toString().trim(),view);
                    dialog.dismiss();
                }else if (Constant.Dailog_BLE_Search==dailogType){
                    mListener.onViewClick(dailogType,"".trim(),view);
                    dialog.dismiss();
                }

            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        if (!mActivity.isFinishing())dialog.show();
        if (Constant.Dailog_Input_SampleNo==dailogType){
            mListener.onViewClick(Constant.RequestCode_Scan_Add_SampleNo,"",null);
        }
    }
    public void setContent(String content){
        if (dialog!=null&&dialog.isShowing()){
            edContent.setText(content);
        }
    }
}
