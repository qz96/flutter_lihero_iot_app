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


public class HitiInputParamPointDailog {

    private Activity mActivity;
    private TextView tvTitle, tvOk, tvCancel,tvValue,tvRiss,tvTips;
    private EditText edValueContent,edRissContent;
    protected Dialog dialog;
    ModBusParam myParam;
    private int paramModBusAdr;
    public HitiInputParamPointDailog(Activity activity, final int dailogType, final String dlg_title, ModBusParam modBusParam,int paramModBusAdr, String tips, CommontClickListener mListener) {
        this.mActivity = activity;
        if (modBusParam==null)return;
        this.myParam=modBusParam;
        this.paramModBusAdr=paramModBusAdr;
        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_hiti_input_param_point, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvValue = (TextView) view.findViewById(R.id.tv_param_value);
        tvRiss=view.findViewById(R.id.tv_param_riss);

        edValueContent=view.findViewById(R.id.ed_param_value);
        edRissContent=view.findViewById(R.id.ed_param_riss);

        tvTips=view.findViewById(R.id.tv_tips);
        tvTitle.setText(dlg_title);
        tvRiss.setText(myParam.getChildrenParams().get(0).getParamName()+(StringUtils.isEmpty(myParam.getChildrenParams().get(0).getUnit())?"":("("+myParam.getChildrenParams().get(0).getUnit()+")"))+":");
        tvValue.setText(myParam.getChildrenParams().get(1).getParamName()+":");
        if (!StringUtils.isEmpty(tips)){
            tvTips.setVisibility(View.VISIBLE);
            tvTips.setText(tips);
        }else {
            tvTips.setVisibility(View.GONE);
        }

        if (!StringUtils.isEmpty(myParam.getChildrenParams().get(0).getDataValue()))edRissContent.setText(myParam.getChildrenParams().get(0).getDataValue().trim());
        if (!StringUtils.isEmpty(myParam.getChildrenParams().get(1).getDataValue()))edValueContent.setText(myParam.getChildrenParams().get(1).getDataValue().trim());

        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (edValueContent.getText()==null||StringUtils.isEmpty(edValueContent.getText().toString())){
                        ToastUtils.showInfoSingleMsg(mActivity,"请录入参数值");
                        return;
                    }else if (edRissContent.getText()==null||StringUtils.isEmpty(edRissContent.getText().toString())){
                        ToastUtils.showInfoSingleMsg(mActivity,"请选择信号值");
                        return;
                    }else {
                        List<MRegister> mRegisters=new ArrayList<>();

                        String strSSParamContent=edRissContent.getText().toString().trim();

                        if (StringUtils.isDouble(strSSParamContent)) {
                            float fv = Float.parseFloat(strSSParamContent);
                            int bits=Float.floatToIntBits(fv);
                            mRegisters.add(new MRegister((bits >> 16) & 0xFFFF));
                            mRegisters.add(new MRegister(bits & 0xFFFF));

                        } else {
                            ToastUtils.showInfoSingleMsg(mActivity,strSSParamContent+"，请输入浮点数");
                            return;
                        }

                        String strValueContent=edValueContent.getText().toString().trim();
                        if (myParam.getChildrenParams().get(1).getDataType()==0&&myParam.getChildrenParams().get(1).getDm()>0){
                            if (StringUtils.isDouble(strValueContent)) {

                                float fv = Float.parseFloat(strValueContent);
                                int iv= (int) DoubleUtils.div(fv,myParam.getChildrenParams().get(1).getDm(),0);
                                if (iv<0){
                                    ToastUtils.showInfoSingleMsg(mActivity,strValueContent+"，当前协议暂不支持负数值设置");
                                    return;
                                }
                                mRegisters.add(new MRegister(iv));

                            } else {
                                ToastUtils.showInfoSingleMsg(mActivity,strValueContent+"，请输入浮点数");
                                return;
                            }
                        }else if (myParam.getChildrenParams().get(1).getDataType()==5){
                            if (StringUtils.isDouble(strValueContent)) {

                                float fv = Float.parseFloat(strValueContent);
                                int bits=Float.floatToIntBits(fv);
                                mRegisters.add(new MRegister((bits >> 16) & 0xFFFF));
                                mRegisters.add(new MRegister(bits & 0xFFFF));
                            } else {
                                ToastUtils.showInfoSingleMsg(mActivity,strValueContent+"，请输入浮点数");
                                return;
                            }
                        }



                        if (mRegisters!=null&&mRegisters.size()>0&&myParam!=null){
                            myParam.setWriteRegisters(mRegisters);
                            myParam.setData(mRegisters);
                            mListener.onViewClick(dailogType,myParam,view);
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
        if (Constant.Dailog_Input_SampleNo==dailogType){
            mListener.onViewClick(Constant.RequestCode_Scan_Add_SampleNo,"",null);
        }
    }

}
