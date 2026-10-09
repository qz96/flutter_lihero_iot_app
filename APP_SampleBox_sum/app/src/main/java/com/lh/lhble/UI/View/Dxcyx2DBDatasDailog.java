package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.lh.lhble.BuildConfig;
import com.lh.lhble.Entity.DbParamData;
import com.lh.lhble.Entity.ExcelInfoBean;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.ExcelUtils;
import com.lh.lhble.adapter.DbParamDataAdapter;
import com.lh.lhble.listener.CommontClickListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class Dxcyx2DBDatasDailog {
    private Context mContext;
    private List<DbParamData> datas;
    private TextView tvTitle, tvOk, tvCancel;
    protected Dialog dialog;
    private FrameLayout frlMnMap;
    private ListView lvData;
    private DbParamDataAdapter mAdapter;
    private CommontClickListener mListener;
    public Dxcyx2DBDatasDailog(Context context, String dlg_title, List<DbParamData> dbParamDataList, int selecterType, CommontClickListener listener) {
        this.mContext = context;
        this.mListener=listener;
        this.datas = dbParamDataList;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_dxcyx2_db_datas, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, R.style.MyDialogStyle);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvOk = (TextView) view.findViewById(R.id.tv_ok);
        lvData = (ListView) view.findViewById(R.id.lv_data);
        mAdapter=new DbParamDataAdapter(mContext);
        lvData.setAdapter(mAdapter);
        tvTitle.setText(dlg_title);
        if (datas != null && datas.size() > 0) {
            Collections.sort(datas);
           mAdapter.setDatas(datas);
        }
        lvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

              /*  if (datas.size()>i){
                    mListener.onItemClick3(view,i,datas.get(i),selecterType);
                    dialog.dismiss();
                }*/

            }
        });
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File dfile = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath());
                if (!dfile.exists()) {
                    dfile.mkdirs(); // 创建文件夹
                }
               /* String storePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "LHBleExcels";
                File appDir = new File(storePath);
                if (!appDir.exists()) {
                    appDir.mkdir();
                }*/
                String fileName= dlg_title+DateUtil.long2time10(System.currentTimeMillis())+"Excel.xls";
                List<HashMap<String,String>> hashMap01=new ArrayList<>();
                List<String> excelKeys=new ArrayList<>();
                excelKeys.add(datas.get(0).getParamName().replaceAll("\n",""));
                excelKeys.add(datas.get(0).getData1Value());
                excelKeys.add(datas.get(0).getData2Value());
                excelKeys.add(datas.get(0).getData3Value());
                for (int i=1;i<datas.size();i++){
                    HashMap<String,String> params=new HashMap<>();
                    params.put(datas.get(0).getParamName().replaceAll("\n",""),datas.get(i).getParamName().replaceAll("\n",""));
                    params.put(datas.get(0).getData1Value(),datas.get(i).getData1Value());
                    params.put(datas.get(0).getData2Value(),datas.get(i).getData2Value());
                    params.put(datas.get(0).getData3Value(),datas.get(i).getData3Value());
                    hashMap01.add(params);
                }
                if ( ExcelUtils.getInstance().exportData(dfile.getAbsolutePath(),fileName,new ExcelInfoBean(dlg_title+"表",excelKeys,hashMap01))){
                    Toast.makeText(mContext,"生成"+fileName+"的Excel表，保存成功！",Toast.LENGTH_LONG).show();
                    Uri fileUri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileprovider", new File(dfile.getAbsolutePath()+ File.separator+fileName));
                    Intent mIntent = new Intent("android.intent.action.VIEW");
                    mIntent.setDataAndType(fileUri, "application/vnd.ms-excel");
                    mIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(mIntent);
                }else {
                    Toast.makeText(mContext,"生成"+fileName+"的Excel表失败！",Toast.LENGTH_LONG).show();
                }
                mListener.onViewClick(selecterType,"",v);
                dialog.cancel();
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dialog.setContentView(view, layoutParams);

        if (mContext!=null&&mContext instanceof Activity){
            if (!((Activity) mContext).isFinishing())dialog.show();
        }
    }



}
