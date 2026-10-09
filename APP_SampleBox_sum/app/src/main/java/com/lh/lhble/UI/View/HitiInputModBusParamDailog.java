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


public class HitiInputModBusParamDailog {

    private Activity mActivity;
    private ImageView ivScan;
    private TextView tvTitle, tvOk, tvCancel,tvField,tvTips,tvFieldMenu;
    private EditText edContent;
    protected Dialog dialog;
    ModBusParam myParam;
    public HitiInputModBusParamDailog(Activity activity, final int dailogType, final String dlg_title,  ModBusParam modBusParam,  String tips, CommontClickListener mListener) {
        this.mActivity = activity;
        if (modBusParam==null)return;
        this.myParam=modBusParam;
        // 首先得到整个View
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dlg_hiti_input_modbus_param, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(mActivity, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvField = (TextView) view.findViewById(R.id.tv_field_name);
        tvFieldMenu=view.findViewById(R.id.tv_field_menu);
        edContent=view.findViewById(R.id.ed_dlg_content);
        ivScan=view.findViewById(R.id.iv_scan);
        tvTips=view.findViewById(R.id.tv_tips);
        tvTitle.setText(dlg_title);
        tvField.setText(myParam.getParamName()+(StringUtils.isEmpty(myParam.getUnit())?"":("("+myParam.getUnit()+")"))+":");
        if (!StringUtils.isEmpty(tips)){
            tvTips.setVisibility(View.VISIBLE);
            tvTips.setText(tips);
        }else {
            tvTips.setVisibility(View.GONE);
        }
        if (myParam.getDataType()==3){
            tvFieldMenu.setVisibility(View.VISIBLE);
            edContent.setVisibility(View.GONE);
            tvFieldMenu.setText(myParam.getDataValue());
        }else {
            tvFieldMenu.setVisibility(View.GONE);
            edContent.setVisibility(View.VISIBLE);
            if (!StringUtils.isEmpty(myParam.getDataValue()))edContent.setText(myParam.getDataValue().trim());
        }

        if (Constant.Dailog_Input_SampleNo==dailogType)ivScan.setVisibility(View.VISIBLE);
        ivScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onViewClick(Constant.RequestCode_Scan_Add_SampleNo,"",v);

            }
        });
        tvFieldMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(mActivity, tvFieldMenu);
                Menu menu = popupMenu.getMenu();
                for (Map.Entry<Integer, String> entry : myParam.getDataMenu().entrySet()) {
                    menu.add(Menu.NONE, entry.getKey(), entry.getKey(), entry.getValue());
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (myParam.getDataMenu().containsKey(menuItem.getItemId())){
                            tvFieldMenu.setText(myParam.getDataMenu().get(menuItem.getItemId()));
                            myParam.setWriteRegisters(new ArrayList<>(Arrays.asList(new MRegister(menuItem.getItemId()))));
                            return true;
                        }

                        return false;
                    }
                });
                popupMenu.show();
            }
        });
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (myParam.getDataType()!=3&&(edContent.getText()==null||StringUtils.isEmpty(edContent.getText().toString()))){
                        ToastUtils.showInfoSingleMsg(mActivity,"请录入参数信息");
                        return;
                    }else if (myParam.getDataType()==3&&(myParam.getWriteRegisters()==null||myParam.getWriteRegisters().size()!=1)){
                        ToastUtils.showInfoSingleMsg(mActivity,"请选择参数信息");
                        return;
                    }else if (myParam.getDataType()==3){
                        mListener.onViewClick(dailogType,myParam,view);
                        dialog.dismiss();
                    }else {
                        List<MRegister> mRegisters=new ArrayList<>();
                        String strContent=edContent.getText().toString().trim();
                        if (myParam.getDm()>0&&DoubleUtils.hasDecimalPart(myParam.getDm())){//根据浮点型放大倍数调整
                            switch (myParam.getDataType()){
                                case 0://无符号整形
                                    if (StringUtils.isDouble(strContent)){
                                        float fv = Float.parseFloat(edContent.getText().toString().trim());
                                        int iv= (int) DoubleUtils.div(fv,myParam.getDm(),0);
                                        strContent=iv+"";
                                    }else {
                                        ToastUtils.showInfoSingleMsg(mActivity,edContent.getText().toString()+"，请输入正数值");
                                        return;
                                    }
                                    break;
                                case 1://浮点型
                                    if (StringUtils.isDouble(strContent)){
                                        float fv = Float.parseFloat(edContent.getText().toString().trim());
                                        double dv=  DoubleUtils.div(fv,myParam.getDm(),(myParam.getScale()>0?myParam.getScale():0)+(DoubleUtils.getIntegerDigitCount(myParam.getDm())-1));
                                        strContent=dv+"";
                                    }else {
                                        ToastUtils.showInfoSingleMsg(mActivity,edContent.getText().toString()+"，请输入数值");
                                        return;
                                    }
                                    break;
                                case 2://有符号整型
                                    if (StringUtils.isDouble(strContent)){
                                        float fv = Float.parseFloat(edContent.getText().toString().trim());
                                        int iv= (int) DoubleUtils.div(fv,myParam.getDm(),0);
                                        strContent=iv+"";
                                    }else {
                                        ToastUtils.showInfoSingleMsg(mActivity,edContent.getText().toString()+"，请输入数值");
                                        return;
                                    }
                                    break;
                                case 5://浮点型
                                    if (StringUtils.isDouble(strContent)){
                                        float fv = Float.parseFloat(edContent.getText().toString().trim());
                                        double dv=  DoubleUtils.div(fv,myParam.getDm(),(myParam.getScale()>0?myParam.getScale():0)+(DoubleUtils.getIntegerDigitCount(myParam.getDm())-1));
                                        strContent=dv+"";
                                    }else {
                                        ToastUtils.showInfoSingleMsg(mActivity,edContent.getText().toString()+"，请输入数值");
                                        return;
                                    }
                                    break;
                            }
                        }else if (myParam.getDm()>0){//根据整数放大倍数调整
                            switch (myParam.getDataType()){
                                case 0://无符号整形
                                    if (StringUtils.isInt(strContent)){
                                        int iv=Integer.parseInt(strContent.trim());
                                        iv= (int) DoubleUtils.div(iv,myParam.getDm(),0);
                                        strContent=iv+"";
                                    }else {
                                        ToastUtils.showInfoSingleMsg(mActivity,edContent.getText().toString()+"，请输入正整值");
                                        return;
                                    }
                                    break;
                                case 1://浮点型
                                    if (StringUtils.isDouble(strContent)){
                                        float fv = Float.parseFloat(edContent.getText().toString().trim());
                                        double dv=  DoubleUtils.div(fv,myParam.getDm(),(myParam.getScale()>0?myParam.getScale():0)+(DoubleUtils.getIntegerDigitCount(myParam.getDm())-1));
                                        strContent=dv+"";
                                    }else {
                                        ToastUtils.showInfoSingleMsg(mActivity,edContent.getText().toString()+"，请输入数值");
                                        return;
                                    }
                                    break;
                                case 2://有符号整型
                                    if (StringUtils.isInt(strContent)){
                                        int iv=Integer.parseInt(strContent.trim());
                                        iv= (int) DoubleUtils.div(iv,myParam.getDm(),0);
                                        strContent=iv+"";
                                    }else {
                                        ToastUtils.showInfoSingleMsg(mActivity,edContent.getText().toString()+"，请输入整值");
                                        return;
                                    }
                                    break;
                                case 5://浮点型
                                    if (StringUtils.isDouble(strContent)){
                                        float fv = Float.parseFloat(edContent.getText().toString().trim());
                                        double dv=  DoubleUtils.div(fv,myParam.getDm(),(myParam.getScale()>0?myParam.getScale():0)+(DoubleUtils.getIntegerDigitCount(myParam.getDm())-1));
                                        strContent=dv+"";
                                    }else {
                                        ToastUtils.showInfoSingleMsg(mActivity,edContent.getText().toString()+"，请输入数值");
                                        return;
                                    }
                                    break;
                            }
                        }


                        switch (myParam.getDataType()){
                            case 0:
                                if (StringUtils.isInt(strContent)) {
                                    int iv = Integer.parseInt(strContent);
                                    if (iv<0){
                                        ToastUtils.showInfoSingleMsg(mActivity,iv+"，请输入正整数");
                                        return;
                                    }else {
                                        mRegisters.add(new MRegister(iv));
                                    }
                                } else {
                                    ToastUtils.showInfoSingleMsg(mActivity,strContent+"，请输入正整数");
                                    return;
                                }
                                break;
                            case 1:
                                if (StringUtils.isDouble(strContent)) {
                                    float fv = Float.parseFloat(strContent);
                                    int bits=Float.floatToIntBits(fv);
                                    mRegisters.add(new MRegister(bits & 0xFFFF));
                                    mRegisters.add(new MRegister((bits >> 16) & 0xFFFF));
                                } else {
                                    ToastUtils.showInfoSingleMsg(mActivity,strContent+"，请输入浮点数");
                                    return;
                                }
                                break;
                            case 2:
                                if (StringUtils.isInt(strContent)) {
                                    int iv = Integer.parseInt(strContent);
                                    mRegisters.add(new MRegister((iv >> 16) & 0xFFFF));
                                    mRegisters.add(new MRegister(iv & 0xFFFF));
                                } else {
                                    ToastUtils.showInfoSingleMsg(mActivity,strContent+"，请输入整数");
                                    return;
                                }
                                break;

                            case 4:
                                if (StringUtils.isDouble(strContent)) {
                                    double dv= Double.parseDouble(strContent);
                                    int big= (int) DoubleUtils.div(dv,Math.pow(10,7),0);
                                    if (big>0){
                                        int bits=Float.floatToIntBits((float) (dv-(big*Math.pow(10,7))));
                                        mRegisters.add(new MRegister(bits & 0xFFFF));
                                        mRegisters.add(new MRegister((bits >> 16) & 0xFFFF));
                                        mRegisters.add(new MRegister(big));
                                    }else {
                                        int bits=Float.floatToIntBits((float) dv);
                                        mRegisters.add(new MRegister(bits & 0xFFFF));
                                        mRegisters.add(new MRegister((bits >> 16) & 0xFFFF));
                                        mRegisters.add(new MRegister(0));
                                    }



                                } else {
                                    ToastUtils.showInfoSingleMsg(mActivity,strContent+"，请输入数值");
                                    return;
                                }
                                break;
                            case 5:
                                if (StringUtils.isDouble(strContent)) {
                                    float fv = Float.parseFloat(strContent);
                                    int bits=Float.floatToIntBits(fv);
                                    mRegisters.add(new MRegister((bits >> 16) & 0xFFFF));
                                    mRegisters.add(new MRegister(bits & 0xFFFF));
                                } else {
                                    ToastUtils.showInfoSingleMsg(mActivity,strContent+"，请输入浮点数");
                                    return;
                                }
                                break;
                        }

                        if (mRegisters!=null&&mRegisters.size()>0&&myParam!=null){
                            myParam.setWriteRegisters(mRegisters);
                            mListener.onViewClick(dailogType,myParam,view);
                        }else {
                            ToastUtils.showInfoSingleMsg(mActivity,strContent+"，请输入正确数据");
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
    public void setContent(String content){
        if (dialog!=null&&dialog.isShowing()){
            edContent.setText(content);
        }
    }
}
