package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.NetInfo;
import com.lh.lhble.R;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.listener.CommontClickListener;

import java.text.DecimalFormat;
import java.text.Format;


public class NetInfoInputDailog {

    private Context mContext;
    private EditText edNetInfoIP,edNetInfoPort ;
    private LinearLayout llProtocolType;
    private TextView tvTitle, tvOk, tvCancel,tvContent;
    private CheckBox cb212,cbMqtt;
    protected Dialog dialog;
    private NetInfo mNetInfo;
    public NetInfoInputDailog(Context context,   int dailogType,  String dlg_title,NetInfo netInfo,  CommontClickListener mListener) {
        this.mContext = context;
        if (netInfo==null||netInfo.getNo()<0){
            Toast.makeText(mContext,"通信服务序号丢失",Toast.LENGTH_LONG).show();
            return;
        }
        this.mNetInfo=netInfo;
        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_input_netinfo, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        edNetInfoIP=view.findViewById(R.id.ed_net_info_ip);
        edNetInfoPort=view.findViewById(R.id.ed_net_info_port);
        cb212=view.findViewById(R.id.cb_212);
        cbMqtt=view.findViewById(R.id.cb_mqtt);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        tvContent = (TextView) view.findViewById(R.id.tv_dlg_content);
        llProtocolType=view.findViewById(R.id.ll_protocol_type);
        if (mNetInfo.getProtocolType()==-1){//-1表示不考虑协议类型的采样箱0D07采样通道IP端口配置
            llProtocolType.setVisibility(View.GONE);
        }else {
            llProtocolType.setVisibility(View.VISIBLE);
        }
        tvTitle.setText(dlg_title);


        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNetInfo.getProtocolType()==-1){
                    if (!StringUtils.isIPAddress(edNetInfoIP.getText().toString())){
                        Toast.makeText(mContext,"请设置正确IP地址，例如192.107.8.26",Toast.LENGTH_LONG).show();
                    }else if (!StringUtils.isInt(edNetInfoPort.getText().toString())){
                        Toast.makeText(mContext,"请设置正确端口号",Toast.LENGTH_SHORT).show();
                    }else {
                        String[]ips=edNetInfoIP.getText().toString().trim().split("\\.");
                        String devIP=StringUtils.formatString(Integer.parseInt(ips[0]),3)+StringUtils.formatString(Integer.parseInt(ips[1]),3)+StringUtils.formatString(Integer.parseInt(ips[2]),3)+StringUtils.formatString(Integer.parseInt(ips[3]),3);
                        mNetInfo.setStrIp12(devIP);
                        mNetInfo.setStrPort5(StringUtils.formatString(Integer.parseInt(edNetInfoPort.getText().toString()),5));
                        mListener.onViewClick(dailogType,mNetInfo,view);
                        dialog.dismiss();
                    }
                }else {
                    if (!StringUtils.isIPAddress(edNetInfoIP.getText().toString())){
                        Toast.makeText(mContext,"请设置正确IP地址，例如192.107.8.26",Toast.LENGTH_LONG).show();
                    }else if (!StringUtils.isInt(edNetInfoPort.getText().toString())){
                        Toast.makeText(mContext,"请设置正确端口号",Toast.LENGTH_SHORT).show();
                    }else if (!cb212.isChecked()&&!cbMqtt.isChecked()){
                        Toast.makeText(mContext,"请选择上传协议",Toast.LENGTH_SHORT).show();
                    }else if (cb212.isChecked()&&cbMqtt.isChecked()){
                        Toast.makeText(mContext,"上传协议目前只支持单选",Toast.LENGTH_SHORT).show();
                    }else {
                        String[]ips=edNetInfoIP.getText().toString().trim().split("\\.");
                        String devIP=StringUtils.formatString(Integer.parseInt(ips[0]),3)+StringUtils.formatString(Integer.parseInt(ips[1]),3)+StringUtils.formatString(Integer.parseInt(ips[2]),3)+StringUtils.formatString(Integer.parseInt(ips[3]),3);
                        mNetInfo.setStrIp12(devIP);
                        mNetInfo.setStrPort5(StringUtils.formatString(Integer.parseInt(edNetInfoPort.getText().toString()),5));
                        mNetInfo.setProtocolType(cbMqtt.isChecked()?1:0);
                        mNetInfo.setEn(1);
                        mListener.onViewClick(dailogType,mNetInfo,view);
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
        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }


}
