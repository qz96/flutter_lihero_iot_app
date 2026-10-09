package com.lh.lhble.UI;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;

import com.lh.lhble.Entity.BinData;
import com.lh.lhble.R;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.wise.wisekit.activity.BaseActivity;

public class SetActivity extends BaseActivity implements CommontClickListener {
    //力合蓝牙发送是否加密
    private TextView tvEncrypt;
    // 是否支持配置
    public static final String EXTRAS_SET_IS_CONFIG = "DEVICE_IS_CONFIG";
    //选择力合蓝牙发送是否加密
    private LinearLayout selectEncrypt;
    // 选择模式
    private LinearLayout selectModeBtn;
    // 选择字符
    private LinearLayout selectCharBtn;
    // 选择是否添加回车
    private LinearLayout selectAddReturnBtn;

    // 选择模式
    private TextView modeTxt;
    // 选择字符
    private TextView charTxt;
    // 选择是否添加回车
    private TextView returnTxt;

    private boolean isConfig;
    public static final int RequestCode_System_Doc=1002;
    private TextView tvAppVersionInfo,tvCheckNewApk,tvApService,tvLhbleListOrder;
    private BinData binData;

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_set;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView() {
        super.initView();

        Intent intent = getIntent();
        isConfig = intent.getBooleanExtra(EXTRAS_SET_IS_CONFIG, false);

        setTitle("设置");
        tvEncrypt=findViewById(R.id.encrypt_txt);
        modeTxt = findViewById(R.id.mode_txt);
        charTxt = findViewById(R.id.char_txt);
        returnTxt = findViewById(R.id.return_txt);
        tvApService=findViewById(R.id.tv_app_ap_service);
        selectEncrypt=findViewById(R.id.select_encrypt);
        selectEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectEncryptPopupMenu();
            }
        });
        selectCharBtn = findViewById(R.id.select_char);
        selectCharBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectCharPopupMenu();
            }
        });

        selectModeBtn = findViewById(R.id.select_mode);
        selectModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectModePopupMenu();
            }
        });

        selectAddReturnBtn = findViewById(R.id.select_return);
        selectAddReturnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectReturnPopupMenu();
            }
        });

        if (HJBleApplication.shareInstance().isBleConfig()==2) {
            modeTxt.setText("配置模式");
        }else if (HJBleApplication.shareInstance().isBleConfig()==1){
            modeTxt.setText("数据模式");
        }else {
            modeTxt.setText("力合模式");
        }
        if (HJBleApplication.shareInstance().isEncrypt()){
            tvEncrypt.setText("加密");
        }else {
            tvEncrypt.setText("明文");
        }

        if (HJBleApplication.shareInstance().isBleHex()) {
            charTxt.setText("Hex");
        } else {
            charTxt.setText("ASCII");
        }

        if (HJBleApplication.shareInstance().isAddReturn()) {
            returnTxt.setText("是");
        } else {
            returnTxt.setText("否");
        }

        // 不支持配置
        if (!isConfig) {
            findViewById(R.id.config_layout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.config_layout).setVisibility(View.VISIBLE);
        }
        tvAppVersionInfo=findViewById(R.id.tv_app_version_info);
        tvCheckNewApk=findViewById(R.id.tv_check_new_apk);
        tvCheckNewApk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setResult(RESULT_OK,null);
                finish();
            }
        });
        tvLhbleListOrder=findViewById(R.id.tv_check_order);
        tvLhbleListOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK,new Intent());
                finish();
            }
        });
        tvApService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setAction("android.intent.action.MAIN");
                ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity");
                intent.setComponent(cn);
                startActivity(intent);
               // startService(new Intent(SetActivity.this, ApService.class));
            }
        });
    }




    private void showSelectModePopupMenu(){
        PopupMenu popupMenu = new PopupMenu(this,selectModeBtn);
        popupMenu.inflate(R.menu.menu_select_mode);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.lh_mode:
                        modeTxt.setText("力合模式");
                        HJBleApplication.shareInstance().setBleConfig(0);
                        break;
                    case R.id.data_mode:
                        modeTxt.setText("数据模式");
                        HJBleApplication.shareInstance().setBleConfig(1);
                        return true;
                    case R.id.config_mode:
                        modeTxt.setText("配置模式");
                        HJBleApplication.shareInstance().setBleConfig(2);
                        return true;

                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void showSelectEncryptPopupMenu(){
        PopupMenu popupMenu = new PopupMenu(this,selectEncrypt);
        popupMenu.inflate(R.menu.menu_select_encrypt);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.yes:
                        tvEncrypt.setText("加密");
                        HJBleApplication.shareInstance().setIsEncrypt(true);
                        return true;
                    case R.id.no:
                        tvEncrypt.setText("明文");
                        HJBleApplication.shareInstance().setIsEncrypt(false);
                        return true;

                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void showSelectCharPopupMenu(){
        PopupMenu popupMenu = new PopupMenu(this,selectCharBtn);
        popupMenu.inflate(R.menu.menu_select_char);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.ascii:
                        charTxt.setText("ASCII");
                        HJBleApplication.shareInstance().setBleHex(false);
                        return true;
                    case R.id.hex:
                        charTxt.setText("Hex");
                        HJBleApplication.shareInstance().setBleHex(true);
                        return true;

                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void showSelectReturnPopupMenu(){
        PopupMenu popupMenu = new PopupMenu(this,selectAddReturnBtn);
        popupMenu.inflate(R.menu.menu_select_return);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.yes:
                        returnTxt.setText("是");
                        HJBleApplication.shareInstance().setAddReturn(true);
                        return true;
                    case R.id.no:
                        returnTxt.setText("否");
                        HJBleApplication.shareInstance().setAddReturn(false);
                        return true;

                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onViewClick(int position, Object target, View view) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK){
            switch (requestCode){

            }
        }

    }
}
