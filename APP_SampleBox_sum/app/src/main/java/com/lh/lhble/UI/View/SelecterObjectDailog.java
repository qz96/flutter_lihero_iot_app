package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.R;
import com.lh.lhble.adapter.JsonObjectSelecterAdapter;
import com.lh.lhble.listener.CommonItemOnClickListener3;

import java.util.List;


public class SelecterObjectDailog {
    private Context mContext;
    private List<JSONObject> datas;
    private TextView tvTitle, tvOk, tvCancel;
    protected Dialog dialog;
    private FrameLayout frlMnMap;
    private ListView lvData;
    private JsonObjectSelecterAdapter mAdapter;
    private CommonItemOnClickListener3 mListener;
    public SelecterObjectDailog(Context context, String dlg_title, List<JSONObject> jat, int selecterType, CommonItemOnClickListener3 listener) {
        this.mContext = context;
        this.mListener=listener;
        this.datas = jat;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_select_jat, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        lvData = (ListView) view.findViewById(R.id.lv_data);
        mAdapter=new JsonObjectSelecterAdapter(mContext);
        lvData.setAdapter(mAdapter);
        tvTitle.setText(dlg_title);
        if (datas != null && datas.size() > 0) {
           mAdapter.setDatas(datas);
        }
        tvOk.setVisibility(View.GONE);
        lvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (datas.size()>i){
                    mListener.onItemClick3(view,i,datas.get(i),selecterType);
                    dialog.dismiss();
                }

            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.setContentView(view);
        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }



}
