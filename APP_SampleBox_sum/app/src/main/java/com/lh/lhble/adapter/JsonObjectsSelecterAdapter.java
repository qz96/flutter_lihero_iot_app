package com.lh.lhble.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.R;

import java.util.ArrayList;
import java.util.List;

public class JsonObjectsSelecterAdapter extends BaseAdapter {

    private Context context; // 运行上下文
    private LayoutInflater listContainer; // 视图容器
    private List<JSONObject> datas;
    public JsonObjectsSelecterAdapter(Context context) {
        this.setContext(context);
        this.context = context;
        datas=new ArrayList<>();
        listContainer = LayoutInflater.from(context);
    }

    public void setDatas(List<JSONObject> datas) {
        this.datas = datas;
        notifyDataSetChanged();
    }

    public List<JSONObject> getDatas() {
        return datas;
    }

    public final class ListItemView {
        public ImageView ivCheck;
        public TextView tv_0;

        public ListItemView() {

        }
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return datas.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return datas.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }

    @SuppressLint("InflateParams")
    public View getView(final int position, View convertView, ViewGroup parent) {
        ListItemView listItemView = null;
        JSONObject data=datas.get(position);
        if (convertView == null) {
            listItemView = new ListItemView();
            // 获取list_item布局文件的视图
            convertView = listContainer
                    .inflate(R.layout.item_json_object_checked, null);

            // 获取控件对象
            listItemView.tv_0 = (TextView) convertView
                    .findViewById(R.id.tv_0);
            listItemView.ivCheck=convertView.findViewById(R.id.iv_check);
            // 设置控件集到convertView
            convertView.setTag(listItemView);

        } else {
            listItemView = (ListItemView) convertView.getTag();
        }
        listItemView.tv_0.setText(data.getString("MyName"));
        if (data.getBooleanValue("isChecked")){
            listItemView.ivCheck.setImageDrawable(context.getResources().getDrawable(R.mipmap.ic_blue_checked));
        }else {
            listItemView.ivCheck.setImageDrawable(context.getResources().getDrawable(R.mipmap.ic_un_checked));
        }

        return convertView;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

}