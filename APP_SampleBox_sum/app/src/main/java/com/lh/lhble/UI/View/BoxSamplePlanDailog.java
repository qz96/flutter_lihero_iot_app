package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.lh.lhble.Entity.BoxSamplePlan;
import com.lh.lhble.Entity.SampleBox;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.adapter.BoxSamplePlanAdapter;
import com.lh.lhble.listener.CommonItemOnClickListener3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BoxSamplePlanDailog {
    private Context mContext;
    private List<BoxSamplePlan> datas;
    private TextView tvTitle, tvOk, tvCancel,tvPlanAdd;
    private LinearLayout llPlanAdd;
    private EditText edHour,edMin;
    protected Dialog dialog;
    private FrameLayout frlMnMap;
    private ListView lvData;
    private BoxSamplePlanAdapter mAdapter;
    private CommonItemOnClickListener3 mListener;
    private List<Long> cycleSampleTimes;
    private long sysTimeTime;

    public BoxSamplePlanDailog(Context context, String dlg_title, List<BoxSamplePlan> samplePlans, SampleBox sampleBox, int selecterType, CommonItemOnClickListener3 listener) {
        this.mContext = context;
        this.mListener=listener;
        this.datas = samplePlans;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_box_sample_plan, null);
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
        mAdapter=new BoxSamplePlanAdapter(mContext,selecterType,mListener);
        lvData.setAdapter(mAdapter);
        tvTitle.setText(dlg_title);
        if (sampleBox!=null&&"13".equals(sampleBox.getSampleMode())&&sampleBox.getNextSampleTime()>0&&sampleBox.getCycleTime()>0){
            cycleSampleTimes = new ArrayList<>();
            sysTimeTime = DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss");
            long nextTime= DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss")+sampleBox.getNextSampleTime()*1000L;
            for (int i=0;sampleBox.getCycleTime()*i<=7*24*60;i++){
                cycleSampleTimes.add(nextTime);
                nextTime=nextTime+sampleBox.getCycleTime()*60*1000L;

            }

        }
        if (datas != null && datas.size() > 0) {
           mAdapter.setDatas(datas);
        }
        tvOk.setVisibility(View.GONE);
        tvPlanAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StringUtils.isEmpty(edHour.getText().toString())||Integer.parseInt(edHour.getText().toString().trim())>23
                ||StringUtils.isEmpty(edMin.getText().toString())||Integer.parseInt(edMin.getText().toString().trim())>59){
                    Toast.makeText(mContext,"请输入正确定时采样时间",Toast.LENGTH_LONG).show();
                    return;
                }else if (datas==null||datas.size()<6){
                    Toast.makeText(context,"未获取到当前定时采样计划，请返回重新加载",Toast.LENGTH_SHORT).show();
                    return;
                }else if (datas.get(5).getStatus().equals("01")){
                    Toast.makeText(context,"定时采样计划最多制定6个，请先删除再新增",Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    BoxSamplePlan addSamplePlan=new BoxSamplePlan();
                    String aHour=(edHour.getText().toString().trim().length()==1?"0":"")+edHour.getText().toString().trim();
                    String aMin=(edMin.getText().toString().trim().length()==1?"0":"")+edMin.getText().toString().trim();
                    //疾控模式设置下层定时采样时需要和上层等比例采样间隔10分钟以上
                    if (cycleSampleTimes!=null&&cycleSampleTimes.size()>0&&sysTimeTime>0){
                        long planTime=DateUtil.format2miao(DateUtil.long2time2(System.currentTimeMillis())+aHour+aMin+"00", "yyyyMMddHHmmss");
                        if (sysTimeTime-planTime>24*60*60*1000L){
                            Toast.makeText(context,DateUtil.long2time(sysTimeTime)+"设备系统时间异常，请返回设置正确系统时间！",Toast.LENGTH_LONG).show();
                            return;
                        }else if (sysTimeTime-planTime>0){
                            planTime=planTime+24*60*60*1000L;
                        }
                        for (Long cycleSampleTime:cycleSampleTimes){
                            if (Math.abs(cycleSampleTime-planTime)<10*60*1000L){
                                Toast.makeText(context,DateUtil.long2time(cycleSampleTime)+"有等比例采样，请保证间隔10分钟以上！",Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }
                    addSamplePlan.setHHmm(aHour+aMin);
                    addSamplePlan.setStatus("01");
                    for (BoxSamplePlan samplePlan:datas){
                        if ("01".equals(samplePlan.getStatus())&&addSamplePlan.getHHmm().equals(samplePlan.getHHmm())){
                            Toast.makeText(context,"请勿指定相同时间的定时采样计划",Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    datas.add(addSamplePlan);
                    Collections.sort(datas);
                    datas.remove(6);
                    mListener.onItemClick3(v,7,datas,selecterType);
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

    public void notifyDataSetChanged(List<BoxSamplePlan> datas){
        this.datas=datas;
        if (mAdapter!=null){
            mAdapter.setDatas(datas);
        }

    }

    public boolean isShow(){
        if (dialog!=null){
          return   dialog.isShowing();
        }else {
            return false;
        }
    }

}
