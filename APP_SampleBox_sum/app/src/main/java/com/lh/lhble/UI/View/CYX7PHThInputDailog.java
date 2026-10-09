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


public class CYX7PHThInputDailog {

    private Activity mActivity;
    private TextView tvTitle, tvOk, tvCancel;
    private EditText edPHThMax,edPHThMin;
    protected Dialog dialog;

    public CYX7PHThInputDailog(Activity activity, final int dailogType, final String dlg_title, double phThMax,double phThMin, final CommontClickListener mListener) {
        this.mActivity = activity;

        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_cyx7_phth_input, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        edPHThMax=view.findViewById(R.id.ed_phth_max);
        edPHThMin=view.findViewById(R.id.ed_phth_min);
        tvTitle.setText(dlg_title);
        if (phThMin>0)edPHThMin.setText(DoubleUtils.round(phThMin,2)+"");
        if (phThMax>0&&phThMax>phThMin)edPHThMax.setText(DoubleUtils.round(phThMax,2)+"");

        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (StringUtils.isEmpty(edPHThMin.getText().toString())||StringUtils.isEmpty(edPHThMax.getText().toString())){
                    Toast.makeText(mActivity,"请输入正确pH阈值上下限，0~14",Toast.LENGTH_SHORT).show();
                }else if (!StringUtils.isDouble(edPHThMin.getText().toString().trim())){
                    Toast.makeText(mActivity,"请输入pH阈值下限，保留两位小数",Toast.LENGTH_SHORT).show();
                }else if (!StringUtils.isDouble(edPHThMax.getText().toString().trim())){
                    Toast.makeText(mActivity,"请输入pH阈值上限，保留两位小数",Toast.LENGTH_SHORT).show();
                }else {
                    double phThMin=Double.parseDouble(edPHThMin.getText().toString().trim());
                    double phThMax=Double.parseDouble(edPHThMax.getText().toString().trim());
                    if (DoubleUtils.round(phThMin,2)<=0||DoubleUtils.round(phThMin,2)>=14){
                        Toast.makeText(mActivity,"请输入pH阈值下限，0~14",Toast.LENGTH_SHORT).show();
                        return;
                    }else if (DoubleUtils.round(phThMax,2)<=0||DoubleUtils.round(phThMax,2)>=14){
                        Toast.makeText(mActivity,"请输入pH阈值上限，0~14",Toast.LENGTH_SHORT).show();
                        return;
                    }else  if (phThMax<=phThMin){
                        Toast.makeText(mActivity,"请输入正确pH阈值上下限，上限必须高于下限值",Toast.LENGTH_SHORT).show();
                    }else {

                        mListener.onViewClick(dailogType,((int)(phThMin*100))+"#"+((int)(phThMax*100)),view);
                        dialog.dismiss();
                    }
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
