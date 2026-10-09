package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.R;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.listener.CommontClickListener;


public class CYX4JYTimeInputDailog {

    private Activity mActivity;
    private TextView tvTitle, tvOk, tvCancel,tvJYTime;
    private EditText edContent;
    protected Dialog dialog;

    public CYX4JYTimeInputDailog(Activity activity, final int dailogType, final String dlg_title, String strJYTime, final CommontClickListener mListener) {
        this.mActivity = activity;

        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_cyx4_jytime_input, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvJYTime = (TextView) view.findViewById(R.id.tv_jy_time);
        edContent=view.findViewById(R.id.ed_dlg_content);
        tvTitle.setText(dlg_title);
       // if (!StringUtils.isEmpty(strJYTime)&&StringUtils.isInt(strJYTime))tvJYTime.setText(strJYTime.trim());
        edContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (StringUtils.isEmpty(s.toString())){
                    tvJYTime.setText("");
                }else if (!StringUtils.isDouble(s.toString().trim())){
                    Toast.makeText(mActivity,"请输入正确目标采样高度，0~10米",Toast.LENGTH_SHORT).show();
                }else {
                    double h=DoubleUtils.round(Double.parseDouble(s.toString().trim()),2);
                    if (h<0||h>10){
                        Toast.makeText(mActivity,"请输入正确目标采样高度，0~10米",Toast.LENGTH_SHORT).show();
                    }else if(h<=6){
                        int jytime= (int) DoubleUtils.round(28*h+34,0);
                        tvJYTime.setText(jytime+"");
                    }else {
                        int jytime= (int) DoubleUtils.round(5*h+172,0);
                        tvJYTime.setText(jytime+"");
                    }


                }
            }
        });
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tvJYTime.getText()!=null&& !StringUtils.isEmpty(tvJYTime.getText().toString())){
                    mListener.onViewClick(dailogType,tvJYTime.getText().toString().trim(),view);
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
    public void setContent(String content){
        if (dialog!=null&&dialog.isShowing()){
            edContent.setText(content);
        }
    }
}
