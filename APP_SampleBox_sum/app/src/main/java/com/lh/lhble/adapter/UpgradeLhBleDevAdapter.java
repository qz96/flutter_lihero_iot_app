package com.lh.lhble.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lh.lhble.Entity.LhBleBean;
import com.lh.lhble.Entity.SampleBottle;
import com.lh.lhble.Entity.T_BottleState;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpgradeLhBleDevAdapter extends BaseAdapter {
    private List<LhBleBean> datas;
    private LayoutInflater mInflator;
    private Context mContext;
    public UpgradeLhBleDevAdapter(Context context)
    {
        super();
        mContext=context;
        datas = new ArrayList<>();
        mInflator = LayoutInflater.from(context);
    }


    public void setDatas(List<LhBleBean> sampleBottles){
        if (sampleBottles!=null){
            datas=sampleBottles;
        }else {
            datas=new ArrayList<>();
        }
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
            view = mInflator.inflate(R.layout.item_upgrade_bottle, null);
            viewHolder = new ViewHolder();
            viewHolder.tvBleNum = view.findViewById(R.id.tv_ble_num);
            viewHolder.tvBlueMac =  view.findViewById(R.id.tv_blue_mac);
            viewHolder.tvResultState =  view.findViewById(R.id.tv_result_state);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        LhBleBean bottleState = datas.get(i);

        viewHolder.tvBleNum.setText(bottleState.getBottle()+"");
        viewHolder.tvBlueMac.setText(bottleState.getBlueMac());
        switch (bottleState.getBleUpgradeState()){
            case 0:
                viewHolder.tvResultState.setText("");
                break;
            case 1:
                viewHolder.tvResultState.setText("进行中");
                viewHolder.tvResultState.setTextColor(Color.parseColor("#3F90F2"));
                break;
            case 2:
                viewHolder.tvResultState.setText("升级完成");
                viewHolder.tvResultState.setTextColor(Color.parseColor("#24ccaa"));
                break;
            case 3:
                viewHolder.tvResultState.setText("已是最新版本");
                viewHolder.tvResultState.setTextColor(Color.parseColor("#24ccaa"));
                break;
            case 4:
                viewHolder.tvResultState.setText("连接失败,请点击重试");
                viewHolder.tvResultState.setTextColor(Color.parseColor("#FF8C25"));
                break;
            case 5:
                viewHolder.tvResultState.setText("升级失败,请点击重试");
                viewHolder.tvResultState.setTextColor(Color.parseColor("#FF8C25"));
                break;
            default:
                if (bottleState.getBleUpgradeState()>200000){
                    viewHolder.tvResultState.setText("原版本V"+bottleState.getBleUpgradeState());
                    viewHolder.tvResultState.setTextColor(Color.parseColor("#3F90F2"));
                }else {
                    viewHolder.tvResultState.setText("错误"+bottleState.getBleUpgradeState()+",请点击重试");
                    viewHolder.tvResultState.setTextColor(Color.parseColor("#FF8C25"));
                }

                break;

        }


        return view;
    }

    static class ViewHolder
    {
        TextView tvBleNum;
        TextView tvBlueMac;
        TextView tvResultState;

    }
}
