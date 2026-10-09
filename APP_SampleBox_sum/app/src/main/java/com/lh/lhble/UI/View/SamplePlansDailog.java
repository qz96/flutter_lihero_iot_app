package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.lhble.Entity.SamplePlan;
import com.lh.lhble.R;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.adapter.SamplePlanAdapter;
import com.lh.lhble.listener.CommonItemOnClickListener3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SamplePlansDailog {
    private Context mContext;
    private List<SamplePlan> datas;
    private TextView tvTitle, tvOk, tvCancel,tvPlanAdd;
    private LinearLayout llPlanAdd;
    private EditText edHour,edMin;
    protected Dialog dialog;
    private FrameLayout frlMnMap;
    private ListView lvData;
    private SamplePlanAdapter mAdapter;
    private CommonItemOnClickListener3 mListener;
    public SamplePlansDailog(Context context, String dlg_title, List<SamplePlan> samplePlans, int selecterType, CommonItemOnClickListener3 listener) {
        this.mContext = context;
        this.mListener=listener;
        this.datas = samplePlans;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_sample_plans, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        llPlanAdd=view.findViewById(R.id.ll_plan_add);
        tvPlanAdd=view.findViewById(R.id.tv_plan_add);
        edHour=view.findViewById(R.id.ed_hour);
        edMin=view.findViewById(R.id.ed_min);
        lvData = (ListView) view.findViewById(R.id.lv_plan);
        mAdapter=new SamplePlanAdapter(mContext,selecterType,mListener);
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
        tvPlanAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StringUtils.isEmpty(edHour.getText().toString())||Integer.parseInt(edHour.getText().toString().trim())>23
                ||StringUtils.isEmpty(edMin.getText().toString())||Integer.parseInt(edMin.getText().toString().trim())>59){
                    Toast.makeText(mContext,"请输入正确定时采样时间",Toast.LENGTH_LONG).show();
                    return;
                }
                if (datas!=null&&datas.size()<6){

                    SamplePlan addSamplePlan=new SamplePlan();
                    String aHour=(edHour.getText().toString().trim().length()==1?"0":"")+edHour.getText().toString().trim();
                    String aMin=(edMin.getText().toString().trim().length()==1?"0":"")+edMin.getText().toString().trim();
                    addSamplePlan.setDateTime(aHour+":"+aMin);
                    List<SamplePlan>changePlans=new ArrayList<>();
                    for (SamplePlan samplePlan:datas){
                        String sTime=samplePlan.getDateTime().replace(":","");
                        String addTime=addSamplePlan.getDateTime().replace(":","");
                        if (Integer.parseInt(sTime)>Integer.parseInt(addTime)){
                            samplePlan.setNo(samplePlan.getNo()+1);
                            changePlans.add(samplePlan);
                        }
                    }
                    if (changePlans.size()>0){
                        addSamplePlan.setNo(changePlans.get(0).getNo());
                    }else if (datas.size()<=0){
                        addSamplePlan.setNo(1);
                    }else {
                        addSamplePlan.setNo(datas.get(datas.size()-1).getNo()+1);
                    }
                    changePlans.add(addSamplePlan);
                    Collections.sort(changePlans);
                    mListener.onItemClick3(v,7,changePlans,selecterType);
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
