package com.lh.lhble.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lh.lhble.Entity.DbParamData;
import com.lh.lhble.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DbParamDataAdapter extends BaseAdapter {
    private Context mContext;
    private List<DbParamData> datas;
    private LayoutInflater mInflator;

    public DbParamDataAdapter(Context context) {
        super();
        datas = new ArrayList<>();
        mContext = context;
        mInflator = LayoutInflater.from(context);
    }

    public void sort() {
        Collections.sort(datas);
    }

    public void setDatas(List<DbParamData> dbParamDatas) {
        if (dbParamDatas != null) {
            datas = dbParamDatas;
        } else {
            datas = new ArrayList<>();
        }
        sort();
        notifyDataSetChanged();
    }


    public void clear() {
        datas.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public Object getItem(int i) {
        return datas.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
//                view = mInflator.inflate(R.layout.activity_main, null);
            view = mInflator.inflate(R.layout.item_db_param_data, null);
            viewHolder = new ViewHolder();
            viewHolder.tvParamName = view.findViewById(R.id.tv_param_name);
            viewHolder.tvData1Value = view.findViewById(R.id.tv_data1_value);
            viewHolder.tvData2Value = view.findViewById(R.id.tv_data2_value);
            viewHolder.tvData3Value = view.findViewById(R.id.tv_data3_value);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        DbParamData itemData = datas.get(i);
     //   viewHolder.llContent.setBackgroundColor(i % 2 == 0 ? mContext.getColor(R.color.white) : mContext.getColor(R.color.color_55_app_light));
        viewHolder.tvParamName.setText(itemData.getParamName());
        viewHolder.tvData1Value.setText(itemData.getData1Value());
        viewHolder.tvData2Value.setText(itemData.getData2Value());
        viewHolder.tvData3Value.setText(itemData.getData3Value());
        if (itemData.getIsDb()==1){
            viewHolder.tvData1Value.setBackgroundColor(mContext.getColor(R.color.colorPrimary));
            viewHolder.tvData2Value.setBackgroundColor(mContext.getColor(R.color.colorPrimary));
            viewHolder.tvData3Value.setBackgroundColor(mContext.getColor(R.color.colorPrimary));
        }else if (itemData.getIsDb()==2){
            viewHolder.tvData1Value.setBackgroundColor(mContext.getColor(R.color.color_red));
            viewHolder.tvData2Value.setBackgroundColor(mContext.getColor(R.color.color_red));
            viewHolder.tvData3Value.setBackgroundColor(mContext.getColor(R.color.color_red));
        }else {
            viewHolder.tvData1Value.setBackgroundColor(mContext.getColor(R.color.white));
            viewHolder.tvData2Value.setBackgroundColor(mContext.getColor(R.color.white));
            viewHolder.tvData3Value.setBackgroundColor(mContext.getColor(R.color.white));
        }

        return view;
    }

    static class ViewHolder {
        TextView tvParamName;
        TextView tvData1Value;
        TextView tvData2Value;
        TextView tvData3Value;

    }
}
