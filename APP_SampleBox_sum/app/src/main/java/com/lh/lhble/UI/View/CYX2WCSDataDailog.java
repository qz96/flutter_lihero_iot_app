package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.Entity.DX3SWJInfo;
import com.lh.lhble.Entity.DX3WcsInfo;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ScreenUtils;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.listener.CommontClickListener;


public class CYX2WCSDataDailog {

    private Context mContext;
    private TextView tvDevStatus,tvTEM,tvDP,tvPH,tvDO,tvEC,tvTb,tvOil,tvNH,tvCOD,tvORP,tvYLS,tvLLZ;
    private TextView tvTitle, tvOk, tvCancel;
    protected Dialog dialog;
    private DX3WcsInfo wcsInfo;
    public CYX2WCSDataDailog(Context context, int dailogType, String dlg_title, DX3WcsInfo wcsData, CommontClickListener mListener) {
        this.mContext = context;
        if (wcsData==null){
            Toast.makeText(mContext,"参数数据读取为空",Toast.LENGTH_LONG).show();
            return;
        }
        this.wcsInfo=wcsData;
        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_cyx_wcs_data, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle =  view.findViewById(R.id.tv_title);
        tvDevStatus=view.findViewById(R.id.tv_dev_status);
        tvTEM=view.findViewById(R.id.tv_tem);
        tvDP=view.findViewById(R.id.tv_dp);
        tvPH=view.findViewById(R.id.tv_ph);
        tvDO=view.findViewById(R.id.tv_do);
        tvEC=view.findViewById(R.id.tv_ec);
        tvTb=view.findViewById(R.id.tv_tb);
        tvOil=view.findViewById(R.id.tv_oil);
        tvNH=view.findViewById(R.id.tv_nh);
        tvCOD=view.findViewById(R.id.tv_cod);
        tvORP=view.findViewById(R.id.tv_orp);
        tvYLS=view.findViewById(R.id.tv_yls);
        tvLLZ=view.findViewById(R.id.tv_llz);
        tvCancel =  view.findViewById(R.id.tv_cancel);
        tvOk =  view.findViewById(R.id.tv_ok);
        tvTitle.setText(dlg_title);
        tvDevStatus.setText(StringUtils.getDX3WCSDevStatus(wcsData.status));
        byte[] devSTABytes=StringUtils.getDX3WCSDevStatuByte(wcsData.status);
        tvTEM.setText(devSTABytes[2]==0?"":DoubleUtils.round(wcsInfo.tem,1)+"");
        tvDP.setText(devSTABytes[0]==0?"":DoubleUtils.round(wcsInfo.dp,1)+"");
        tvPH.setText(devSTABytes[1]==0?"":DoubleUtils.round(wcsInfo.pH,2)+"");
        tvDO.setText(devSTABytes[2]==0?"":DoubleUtils.round(wcsInfo.Do,2)+"");
        tvEC.setText(devSTABytes[3]==0?"":DoubleUtils.round(wcsInfo.EC,1)+"");
        tvTb.setText(devSTABytes[4]==0?"":DoubleUtils.round(wcsInfo.Tb,1)+"");
        tvOil.setText(devSTABytes[5]==0?"":DoubleUtils.round(wcsInfo.Oil,1)+"");
        tvNH.setText(devSTABytes[6]==0?"":DoubleUtils.round(wcsInfo.NH,1)+"");
        tvCOD.setText(devSTABytes[7]==0?"":DoubleUtils.round(wcsInfo.COD,1)+"");
        tvORP.setText(devSTABytes[8]==0?"":DoubleUtils.round(wcsInfo.ORP,1)+"");
        tvYLS.setText(devSTABytes[9]==0?"":DoubleUtils.round(wcsInfo.YLS,1)+"");
        tvLLZ.setText(devSTABytes[10]==0?"":DoubleUtils.round(wcsInfo.LLZ,1)+"");
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onViewClick(dailogType,wcsInfo,view);
                dialog.dismiss();
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.weight = ScreenUtils.getScreenWidth(context) / 10 * 9;
        dialog.setContentView(view, layoutParams);
        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }


}
