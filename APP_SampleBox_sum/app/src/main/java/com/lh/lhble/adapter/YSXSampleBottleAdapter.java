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

public class YSXSampleBottleAdapter extends BaseAdapter {
    private List<SampleBottle> datas;
    private LayoutInflater mInflator;

    public YSXSampleBottleAdapter(Context context)
    {
        super();
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
            view = mInflator.inflate(R.layout.item_ysx_sample_bottle_state, null);
            viewHolder = new ViewHolder();
            viewHolder.tvSampleBottleNum = view.findViewById(R.id.tv_sample_bottle_num);
            viewHolder.tvTime =  view.findViewById(R.id.tv_time);
            viewHolder.tvWorkState =  view.findViewById(R.id.tv_work_state);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

       SampleBottle sampleBottle = datas.get(i);

        viewHolder.tvSampleBottleNum.setText(sampleBottle.getBottleNo()+"");
        if (StringUtils.isEmpty(sampleBottle.getSampleID())||sampleBottle.getSampleID().length()!=16||"0000000000000000".equals(sampleBottle.getSampleID())){
            viewHolder.tvTime.setText("无装瓶记录\n");
            viewHolder.tvWorkState.setText("空位待装瓶");

        }else if (sampleBottle.getDateTime()==null||!sampleBottle.getDateTime().startsWith("2")||sampleBottle.getDateTime().length()!=12){
            viewHolder.tvWorkState.setText("样品编号："+StringUtils.getTargetSpField(sampleBottle.getSampleID().trim(),"_"));
            StringBuffer stringBuffer=new StringBuffer();
            stringBuffer.append("无采样记录");
            if (sampleBottle.getBlueMac()==null||"000000000000".equals( sampleBottle.getBlueMac().trim())||sampleBottle.getBlueMac().trim().length()!=12){
                stringBuffer.append("");
            }else {
                stringBuffer.append("\n蓝牙地址："+StringUtils.getNewBlueToothMacAddress(sampleBottle.getBlueMac().trim()));
            }
            viewHolder.tvTime.setText(stringBuffer);

        }else {
            viewHolder.tvWorkState.setText("样品编号："+StringUtils.getTargetSpField(sampleBottle.getSampleID().trim(),"_"));
            StringBuffer stringBuffer=new StringBuffer();
            stringBuffer.append("采样时间："+DateUtil.long2time(DateUtil.format2miao("20"+sampleBottle.getDateTime(),"yyyyMMddHHmmss")));

            if (sampleBottle.getBlueMac()==null||"000000000000".equals( sampleBottle.getBlueMac().trim())||sampleBottle.getBlueMac().trim().length()!=12){
                stringBuffer.append("");
            }else {
                stringBuffer.append("\n蓝牙地址："+StringUtils.getNewBlueToothMacAddress(sampleBottle.getBlueMac().trim()));
            }
            viewHolder.tvTime.setText(stringBuffer);
        }



        return view;
    }

    static class ViewHolder
    {
        ImageView ivSampleBottle;
        TextView tvSampleBottleNum;
        TextView tvTime;
        TextView tvWorkState;

    }
}
