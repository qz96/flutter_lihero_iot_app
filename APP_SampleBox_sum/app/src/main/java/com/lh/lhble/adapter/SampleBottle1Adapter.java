package com.lh.lhble.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lh.lhble.Entity.SampleBottle;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SampleBottle1Adapter extends BaseAdapter {
    private List<SampleBottle> datas;
    private LayoutInflater mInflator;
    private Context mContext;
    public SampleBottle1Adapter(Context context)
    {
        super();
        mContext=context;
        datas = new ArrayList<>();
        mInflator = LayoutInflater.from(context);
    }

    public void sort() {
        Collections.sort(datas);
    }

    public void setDatas(List<SampleBottle> sampleBottles){
        if (sampleBottles!=null){
            datas=sampleBottles;
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
            view = mInflator.inflate(R.layout.item_sample_bottle_1_state, null);
            viewHolder = new ViewHolder();
            viewHolder.tvSampleBottleNum = view.findViewById(R.id.tv_sample_bottle_num);
            viewHolder.tvTime =  view.findViewById(R.id.tv_time);
            viewHolder.tvWorkState =  view.findViewById(R.id.tv_work_state);
            viewHolder.ivRedLock=view.findViewById(R.id.iv_red_lock);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

       SampleBottle sampleBottle = datas.get(i);

        viewHolder.tvSampleBottleNum.setText(sampleBottle.getBottleNo()+"");
        if (sampleBottle.getIsLock()==1) {
            viewHolder.ivRedLock.setImageDrawable(mContext.getResources().getDrawable(R.mipmap.ic_red_lock));
            viewHolder.ivRedLock.setVisibility(View.VISIBLE);
        }else if (sampleBottle.getIsLock()==2){
            viewHolder.ivRedLock.setImageDrawable(mContext.getResources().getDrawable(R.mipmap.ic_sample_save));
            viewHolder.ivRedLock.setVisibility(View.VISIBLE);
        }else {
            viewHolder.ivRedLock.setVisibility(View.INVISIBLE);
        }
        if (sampleBottle.getDateTime()!=null&&sampleBottle.getDateTime().startsWith("2")){
            viewHolder.tvTime.setText( "采样时间："+DateUtil.long2time4(DateUtil.format2miao("20"+sampleBottle.getDateTime(),"yyyyMMddHHmmss")));
        }else {
            viewHolder.tvTime.setText("待采样");
        }
        if (sampleBottle.getBlueMac()==null||"000000000000".equals( sampleBottle.getBlueMac().trim())||sampleBottle.getBlueMac().trim().length()!=12){
            viewHolder.tvWorkState.setText("空位待装瓶\n");
        }else {
            String strInfo = StringUtils.getNewBlueToothMacAddress(sampleBottle.getBlueMac().trim())+"\n样品编号:" +StringUtils.getTargetSpField(sampleBottle.getSampleID().trim(),"_") ;
            viewHolder.tvWorkState.setText(strInfo);
        }


        return view;
    }

    static class ViewHolder
    {
        TextView tvSampleBottleNum;
        ImageView ivRedLock;
        TextView tvTime;
        TextView tvWorkState;

    }
}
