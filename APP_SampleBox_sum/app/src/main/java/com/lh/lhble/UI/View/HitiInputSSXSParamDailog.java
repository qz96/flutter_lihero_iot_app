package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;

import com.lh.lhble.Entity.MRegister;
import com.lh.lhble.Entity.ModBusParam;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.listener.CommontClickListener;
import com.wise.wisekit.utils.ToastUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class HitiInputSSXSParamDailog {

    private Activity mActivity;
    private TextView tvTitle, tvOk, tvCancel,tvSS,tvSSParam,tvTips;
    private EditText edSSContent,edSSParamContent;
    protected Dialog dialog;
    ModBusParam myParam;
    public HitiInputSSXSParamDailog(Activity activity, final int dailogType, final String dlg_title, ModBusParam modBusParam, String tips, CommontClickListener mListener) {
        this.mActivity = activity;
        if (modBusParam==null)return;
        this.myParam=modBusParam;
        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_hiti_input_ssxs_param, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvSS = (TextView) view.findViewById(R.id.tv_ss_name);
        tvSSParam=view.findViewById(R.id.tv_ss_param);

        edSSContent=view.findViewById(R.id.ed_ss_content);
        edSSParamContent=view.findViewById(R.id.ed_ss_param);

        tvTips=view.findViewById(R.id.tv_tips);
        tvTitle.setText(dlg_title);
        tvSS.setText(myParam.getChildrenParams().get(0).getParamName()+(StringUtils.isEmpty(myParam.getChildrenParams().get(0).getUnit())?"":("("+myParam.getChildrenParams().get(0).getUnit()+")"))+":");
        tvSSParam.setText(myParam.getChildrenParams().get(1).getParamName()+":");
        if (!StringUtils.isEmpty(tips)){
            tvTips.setVisibility(View.VISIBLE);
            tvTips.setText(tips);
        }else {
            tvTips.setVisibility(View.GONE);
        }

        if (!StringUtils.isEmpty(myParam.getChildrenParams().get(0).getDataValue()))edSSContent.setText(myParam.getChildrenParams().get(0).getDataValue().trim());
        if (!StringUtils.isEmpty(myParam.getChildrenParams().get(1).getDataValue()))edSSParamContent.setText(myParam.getChildrenParams().get(1).getDataValue().trim());

        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (edSSContent.getText()==null||StringUtils.isEmpty(edSSContent.getText().toString())){
                        ToastUtils.showInfoSingleMsg(mActivity,"请录入水深");
                        return;
                    }else if (edSSParamContent.getText()==null||StringUtils.isEmpty(edSSParamContent.getText().toString())){
                        ToastUtils.showInfoSingleMsg(mActivity,"请选择系数值");
                        return;
                    }else {
                        List<MRegister> mRegisters=new ArrayList<>();
                        String strSSContent=edSSContent.getText().toString().trim();

                        if (StringUtils.isDouble(strSSContent)) {
                            float fv = Float.parseFloat(strSSContent);
                            int bits=Float.floatToIntBits(fv);
                            mRegisters.add(new MRegister(bits & 0xFFFF));
                            mRegisters.add(new MRegister((bits >> 16) & 0xFFFF));
                        } else {
                            ToastUtils.showInfoSingleMsg(mActivity,strSSContent+"，请输入浮点数");
                            return;
                        }
                        String strSSParamContent=edSSParamContent.getText().toString().trim();

                        if (StringUtils.isDouble(strSSParamContent)) {
                            float fv = Float.parseFloat(strSSParamContent);
                            int bits=Float.floatToIntBits(fv);
                            mRegisters.add(new MRegister(bits & 0xFFFF));
                            mRegisters.add(new MRegister((bits >> 16) & 0xFFFF));
                        } else {
                            ToastUtils.showInfoSingleMsg(mActivity,strSSParamContent+"，请输入浮点数");
                            return;
                        }

                        if (mRegisters!=null&&mRegisters.size()>0&&myParam!=null){
                            myParam.setWriteRegisters(mRegisters);
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
