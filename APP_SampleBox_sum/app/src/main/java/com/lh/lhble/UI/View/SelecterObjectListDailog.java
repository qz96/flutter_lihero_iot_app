package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.R;
import com.lh.lhble.Utils.ScreenUtils;
import com.lh.lhble.adapter.JsonObjectsSelecterAdapter;
import com.lh.lhble.listener.CommontClickListener;

import java.util.ArrayList;
import java.util.List;


public class SelecterObjectListDailog {
    private Context mContext;
    private List<JSONObject> datas;
    private TextView tvTitle, tvOk, tvCancel;
    protected Dialog dialog;
    private FrameLayout frlMnMap;
    private ListView lvData;
    private JsonObjectsSelecterAdapter mAdapter;
    private CommontClickListener mListener;
    public SelecterObjectListDailog(Context context, String dlg_title, List<JSONObject> jat, int selecterType, CommontClickListener listener) {
        this.mContext = context;
        this.mListener=listener;
        this.datas = jat;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_select_jats, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        lvData = (ListView) view.findViewById(R.id.lv_data);
        mAdapter=new JsonObjectsSelecterAdapter(mContext);
        lvData.setAdapter(mAdapter);
        tvTitle.setText(dlg_title);
        if (datas != null && datas.size() > 0) {
           mAdapter.setDatas(datas);
        }

        lvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (datas.size()>i){
                    boolean isCheckedI=datas.get(i).getBooleanValue("isChecked");
                    datas.get(i).put("isChecked",!isCheckedI);
                    mAdapter.notifyDataSetChanged();
                }

            }
        });
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<JSONObject> mDatas=new ArrayList<>();
                for (int i=0;i<datas.size();i++){
                    if (datas.get(i).getBooleanValue("isChecked"))mDatas.add(datas.get(i));
                }
                mListener.onViewClick(selecterType,mDatas,v);
                dialog.cancel();
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        if (datas.size()>15){
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParams.height = ScreenUtils.getScreenHeight(context) / 7 * 6;
            dialog.setContentView(view, layoutParams);
        }else {
            dialog.setContentView(view);
        }
        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }



}
