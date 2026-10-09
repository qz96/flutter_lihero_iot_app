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
import com.lh.lhble.base.Constant;
import com.lh.lhble.listener.CommontClickListener;


public class RangeCurveEditDailog {

    private Activity mActivity;
    private TextView tvTitle, tvOk, tvCancel,tvField;
    private EditText edCurveA,edCurveB,edCurveC,edCurveSWYZ;
    protected Dialog dialog;

    public RangeCurveEditDailog(Activity activity, final int dailogType,int curveNo,double curveA,double curveB,double curveC,int curveSWYZ,CommontClickListener mListener) {
        this.mActivity = activity;

        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_range_curve_edit, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvField = (TextView) view.findViewById(R.id.tv_field_name);
        edCurveA=view.findViewById(R.id.ed_curve_a);
        edCurveB=view.findViewById(R.id.ed_curve_b);
        edCurveC=view.findViewById(R.id.ed_curve_c);
        edCurveSWYZ=view.findViewById(R.id.ed_curve_swyz);
        if (curveA!= Constant.DefaultValue)edCurveA.setText(DoubleUtils.round(curveA,2)+"");
        if (curveB!=Constant.DefaultValue)edCurveB.setText(DoubleUtils.round(curveB,2)+"");
        if (curveC!=Constant.DefaultValue)edCurveC.setText(DoubleUtils.round(curveC,2)+"");
        if (curveSWYZ>0)edCurveSWYZ.setText(curveSWYZ+"");

        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edCurveA.getText()==null||StringUtils.isEmpty(edCurveA.getText().toString())||!StringUtils.isDouble(edCurveA.getText().toString())||Double.parseDouble(edCurveA.getText().toString())>=1000){
                    Toast.makeText(mActivity,"请输入正确量程曲线方程系数a值，-99.99~999.99",Toast.LENGTH_SHORT).show();
                }else if (edCurveB.getText()==null||StringUtils.isEmpty(edCurveB.getText().toString())||!StringUtils.isDouble(edCurveB.getText().toString())||Double.parseDouble(edCurveB.getText().toString())>=1000){
                    Toast.makeText(mActivity,"请输入正确量程曲线方程系数b值，-99.99~999.99",Toast.LENGTH_SHORT).show();
                }else if (edCurveC.getText()==null||StringUtils.isEmpty(edCurveC.getText().toString())||!StringUtils.isDouble(edCurveC.getText().toString())||Double.parseDouble(edCurveC.getText().toString())>=1000){
                    Toast.makeText(mActivity,"请输入正确量程曲线方程系数c值，-99.99~999.99",Toast.LENGTH_SHORT).show();
                }else if (edCurveSWYZ.getText()==null||StringUtils.isEmpty(edCurveSWYZ.getText().toString())||!StringUtils.isInt(edCurveSWYZ.getText().toString())||Integer.parseInt(edCurveSWYZ.getText().toString())<=0){
                    Toast.makeText(mActivity,"请输入正确量程曲线水位阈值",Toast.LENGTH_SHORT).show();
                }else{
                    mListener.onViewClick(dailogType,(curveNo+"#"+StringUtils.formatString(((int)(DoubleUtils.round(Double.parseDouble(edCurveA.getText().toString()),2)*100)),5)
                            +"#"+StringUtils.formatString(((int)(DoubleUtils.round(Double.parseDouble(edCurveB.getText().toString()),2)*100)),5)
                            +"#"+StringUtils.formatString(((int)(DoubleUtils.round(Double.parseDouble(edCurveC.getText().toString()),2)*100)),5)
                            +"#"+StringUtils.formatString(Integer.parseInt(edCurveSWYZ.getText().toString()),5)),view);
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
        if (!mActivity.isFinishing()){
            dialog.show();
        }
    }


}
