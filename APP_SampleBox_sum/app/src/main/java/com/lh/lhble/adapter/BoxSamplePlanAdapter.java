package com.lh.lhble.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.lh.lhble.Entity.BoxSamplePlan;
import com.lh.lhble.R;
import com.lh.lhble.listener.CommonItemOnClickListener3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoxSamplePlanAdapter extends BaseAdapter {
    private List<BoxSamplePlan> datas;
    private LayoutInflater mInflator;
    private CommonItemOnClickListener3 mListener;
    private int selectType;
    public BoxSamplePlanAdapter(Context context, int selectType, CommonItemOnClickListener3 mListener)
    {
        super();
        this.selectType=selectType;
        datas = new ArrayList<>();
        this.mListener=mListener;
        mInflator = LayoutInflater.from(context);
    }

    public void sort() {
        Collections.sort(datas);
    }

    public void setDatas(List<BoxSamplePlan> plans){
        if (plans!=null){
            datas=plans;
        }else {
            datas=new ArrayList<>();
        }
        sort();
        notifyDataSetChanged();
    }


    public void clear() {
        datas.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return datas.size();
    }

    @Override
    public Object getItem(int i)
    {
        return datas.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
       ViewHolder viewHolder;
        if (view == null){
//                view = mInflator.inflate(R.layout.activity_main, null);
            view = mInflator.inflate(R.layout.item_box_sample_plan, null);
            viewHolder = new ViewHolder();
            viewHolder.tvTime =  view.findViewById(R.id.tv_time);
            viewHolder.ivDel=view.findViewById(R.id.iv_del);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        BoxSamplePlan samplePlan = datas.get(i);
        if (samplePlan.getStatus().equals("01")){
            viewHolder.tvTime.setText(samplePlan.getHHmm().substring(0,2)+":"+samplePlan.getHHmm().substring(2));
            viewHolder.ivDel.setVisibility(View.VISIBLE);
        }else if (samplePlan.getStatus().equals("00")){
            viewHolder.tvTime.setText("暂未开启");
            viewHolder.ivDel.setVisibility(View.INVISIBLE);
        }else {
            viewHolder.tvTime.setText("未知数据"+samplePlan.getStatus()+samplePlan.getHHmm());
            viewHolder.ivDel.setVisibility(View.VISIBLE);
        }

        viewHolder.ivDel.setTag(i);
        viewHolder.ivDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position= (int) v.getTag();
                datas.get(position).setStatus("00");
                datas.get(position).setHHmm("0000");
                mListener.onItemClick3(v,position,datas,selectType);
            }
        });
        return view;
    }

    static class ViewHolder
    {
        TextView tvTime;
        ImageView ivDel;

    }
}
