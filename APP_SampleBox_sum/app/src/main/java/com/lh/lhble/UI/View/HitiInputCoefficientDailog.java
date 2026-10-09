package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.lh.lhble.Entity.MRegister;
import com.lh.lhble.Entity.ModBusParam;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.listener.CommontClickListener;
import com.wise.wisekit.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;


public class HitiInputCoefficientDailog {

    private Activity mActivity;
    private TextView  tvOk, tvCancel,tvTips;
    private EditText edTopCoefficient,edBottomCoefficient;
    protected Dialog dialog;
    ModBusParam myParam;
    public HitiInputCoefficientDailog(Activity activity, final int dailogType,int topCoefficient100,int bottomCoefficient100, String tips, CommontClickListener mListener) {
        this.mActivity = activity;

        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_top_bottom_coefficient, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);

        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);


        edTopCoefficient=view.findViewById(R.id.ed_top_coefficient);
        edBottomCoefficient=view.findViewById(R.id.ed_bottom_coefficient);

        tvTips=view.findViewById(R.id.tv_tips);

        if (!StringUtils.isEmpty(tips)){
            tvTips.setVisibility(View.VISIBLE);
            tvTips.setText(tips);
        }else {
            tvTips.setVisibility(View.GONE);
        }

        if (topCoefficient100>0)edTopCoefficient.setText(DoubleUtils.div(topCoefficient100,100,2)+"");
        if (bottomCoefficient100>0)edBottomCoefficient.setText(DoubleUtils.div(bottomCoefficient100,100,2)+"");

        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (edTopCoefficient.getText()==null||StringUtils.isEmpty(edTopCoefficient.getText().toString())){
                        ToastUtils.showInfoSingleMsg(mActivity,"请录入上层系数");
                        return;
                    }else if (edBottomCoefficient.getText()==null||StringUtils.isEmpty(edBottomCoefficient.getText().toString())){
                        ToastUtils.showInfoSingleMsg(mActivity,"请录入下层系数");
                        return;
                    }else {
                        StringBuffer sb=new StringBuffer();

                        if (StringUtils.isDouble(edTopCoefficient.getText().toString())) {

                            int t100= (int) (DoubleUtils.round(Double.parseDouble(edTopCoefficient.getText().toString().trim()),2)*100);
                            sb.append(t100+"-");
                        } else {
                            ToastUtils.showInfoSingleMsg(mActivity,edTopCoefficient.getText().toString()+"，请输入小数");
                            return;
                        }
                        if (StringUtils.isDouble(edBottomCoefficient.getText().toString())) {

                            int b100= (int) (DoubleUtils.round(Double.parseDouble(edBottomCoefficient.getText().toString().trim()),2)*100);
                            sb.append(b100);
                        } else {
                            ToastUtils.showInfoSingleMsg(mActivity,edBottomCoefficient.getText().toString()+"，请输入小数");
                            return;
                        }

                        if (sb.length()>0){
                            mListener.onViewClick(dailogType,sb.toString(),view);
                        }else {
                            ToastUtils.showInfoSingleMsg(mActivity, "请输入正确数据");
                        }

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
