package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.R;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.listener.CommontClickListener;


public class JZParamPointInputDailog {

    private Activity mActivity;
    private TextView tvTitle, tvPointName,tvRissName,tvOk, tvCancel;
    private EditText edPointValue,edRissValue;
    protected Dialog dialog;

    public JZParamPointInputDailog(Activity activity, final int dailogType, final String dlg_title,int pointNo,String pointName, String rissName, final CommontClickListener mListener) {
        this.mActivity = activity;

        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_jz_param_point_input, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvPointName=view.findViewById(R.id.tv_point_name);
        tvRissName=view.findViewById(R.id.tv_riss_name);
        edPointValue=view.findViewById(R.id.ed_point_value);
        edRissValue=view.findViewById(R.id.ed_riss_value);
        tvTitle.setText(dlg_title);
        tvPointName.setText(pointName);
        tvRissName.setText(rissName);
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (StringUtils.isEmpty(edPointValue.getText().toString())||StringUtils.isEmpty(edPointValue.getText().toString())){
                    Toast.makeText(mActivity,"请输入校准数据值",Toast.LENGTH_SHORT).show();
                }else if (!StringUtils.isDouble(edPointValue.getText().toString().trim())){
                    Toast.makeText(mActivity,"请输入"+pointName+"，保留两位小数",Toast.LENGTH_SHORT).show();
                }else if (!StringUtils.isDouble(edRissValue.getText().toString().trim())){
                    Toast.makeText(mActivity,"请输入"+rissName+"，保留两位小数",Toast.LENGTH_SHORT).show();
                }else {
                    double pointValue=Double.parseDouble(edPointValue.getText().toString().trim());
                    double rissValue=Double.parseDouble(edRissValue.getText().toString().trim());
                    mListener.onViewClick(dailogType,(pointNo+"#"+(int)(pointValue*100))+"#"+((int)(rissValue*100)),view);
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

    }

}
