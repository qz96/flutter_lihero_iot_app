package com.lh.lhble.UI;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.lh.lhble.Entity.LoginResult;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.Entity.User;
import com.lh.lhble.R;
import com.lh.lhble.Utils.AESUtil;
import com.lh.lhble.Utils.AppUtils;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.network.RetrofitUtils;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LoginActivity extends BaseActivity implements CommontClickListener {
    private EditText edUserName, edPassword;
    private ImageView ivPwdShow;
    private TextView tvCurrentProject, tvLogin, tvAppVersionInfo;
    private LoadingDialog loadingDialog = null;
    private int edPwdInputType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    protected void initView() {
        super.initView();
        edUserName = findViewById(R.id.ed_user_name);
        edPassword = findViewById(R.id.ed_password);
        ivPwdShow = findViewById(R.id.iv_pwd_show);
        tvCurrentProject = findViewById(R.id.tv_current_project);
        tvLogin = findViewById(R.id.tv_login);
        tvAppVersionInfo = findViewById(R.id.tv_app_version_info);
        setTitle(HJBleApplication.getAppContext().getString(R.string.login_title));
        topLeftBtn.setVisibility(View.GONE);
        ivPwdShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edPassword.getInputType() == edPwdInputType) {
                    edPassword.setInputType(EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    edPassword.setSelection(edPassword.getText().length());
                } else if (edPwdInputType != 0) {
                    edPassword.setInputType(edPwdInputType);
                    edPassword.setSelection(edPassword.getText().length());
                }
            }
        });
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             //   startActivity(LiherDXCYX2Activity.getIntent(LoginActivity.this,"na","0D:02:11:00:00:01",false));
                login();

            }
        });
        LoadingDialog.Builder loadBuilder = new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(HJBleApplication.getAppContext().getString(R.string.login_tips))
                .setCancelOutside(false)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {

                    }
                });
        loadingDialog = loadBuilder.create();
        User user = HJBleApplication.getUser();
        if (!StringUtils.isEmpty(user.getOkPwd())) {
            edUserName.setText(user.getUsername());
            //20250702 因陈备需求，重新放开登录密码记忆功能
            edPassword.setText(AESUtil.decrypt(user.getOkPwd(), Constant.LK));
        }
        try {
            tvAppVersionInfo.setText(HJBleApplication.getAppContext().getString(R.string.soft_info)+"V " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersionInfo.setText(HJBleApplication.getAppContext().getString(R.string.soft_info)+"V 0.0.0");
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        edPwdInputType = edPassword.getInputType();
    }

    private void login() {
        if (StringUtils.isEmpty(edUserName.getText().toString())) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_user_name));
        }else if (StringUtils.isEmpty(edPassword.getText().toString())) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_password));
        }else if ("LHBle".equals(edUserName.getText().toString().trim())&&!"LHBle@911456".equals(edPassword.getText().toString().trim())){
            showMsg(2, "LHBle,"+HJBleApplication.getAppContext().getString(R.string.login_err_2));
        }else {
            String xpp = AESUtil.encrypt(edUserName.getText().toString().trim(), Constant.LK);
            String xpu = AESUtil.encrypt(edPassword.getText().toString().trim(), Constant.LK);
            JSONObject joC = new JSONObject();
            joC.put("client_id", "123");
            joC.put("client_secret", "123456");
            joC.put("grant_type", "password");
            joC.put("scope", "all");
            String xpc = AESUtil.encrypt(joC.toJSONString(), Constant.LK);
            loadingDialog.show();
            RetrofitUtils.getApiService().loginToken(xpp, xpu, xpc)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<LoginResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(LoginResult loginResult) {
                            loadingDialog.dismiss();
                            HJBleApplication.getUser().setUserToken(loginResult.getAccess_token());
                            HJBleApplication.getUser().setUsername(loginResult.getUser_name());
                            HJBleApplication.getUser().setUserId(loginResult.getUser_id());
                            HJBleApplication.getUser().setOkPwd(AESUtil.encrypt(edPassword.getText().toString().trim(), Constant.LK));
                            getUserInfo();

                        }

                        @Override
                        public void onError(Throwable e) {
                            loadingDialog.dismiss();
                            String errMsg = HJBleApplication.getAppContext().getString(R.string.login_err_0);
                            if (e instanceof HttpException) {
                                try {
                                    String s = new String(((HttpException) e).response().errorBody().bytes());
                                    if (StringUtils.isEmpty(s)) {
                                        errMsg = HJBleApplication.getAppContext().getString(R.string.login_err_1);
                                    } else if (s.contains("Bad credentials|")) {
                                        errMsg = HJBleApplication.getAppContext().getString(R.string.login_err_2);
                                    } else if (s.contains("username or password invalid")) {
                                        errMsg =HJBleApplication.getAppContext().getString(R.string.login_err_3);
                                    } else if (s.contains("User account is locked|")) {
                                        errMsg = HJBleApplication.getAppContext().getString(R.string.login_err_4);
                                    } else if (s.contains("User is disabled")) {
                                        errMsg = HJBleApplication.getAppContext().getString(R.string.login_err_5);
                                    } else {
                                        errMsg = s;
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            showMsg(1, errMsg);
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }

    }

    public void getUserInfo() {
        loadingDialog.show();
        RetrofitUtils.getApiService().getUserInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Result>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Result result) {
                        loadingDialog.dismiss();
                        if (result.getCode() == Constant.SuccessCode) {
                            JSONObject jo = JSONObject.parseObject(JSON.toJSONString(result.getData()));
                            HJBleApplication.getUser().setRealName(jo.getString("realName"));
                            HJBleApplication.getUser().setPhone(jo.getString("phone"));
                            HJBleApplication.getUser().setDeptCode(jo.getString("deptCode"));
                            HJBleApplication.getUser().setDeptName(jo.getString("deptName"));
                            //  HJBleApplication.getUser().setDefaultProject(jo.getString("defaultProject"));
                            HJBleApplication.getUser().setDefaultProject("277072394999566336");//默认设为采测溯测试项目
                            HJBleApplication.getUser().setGuid(jo.getString("guid"));
                            HJBleApplication.getUser().setStrPermissions(jo.getJSONArray("permissions").toJSONString());
                            if (StringUtils.isEmpty(HJBleApplication.getUser().getStrPermissions())) {
                                showMsg(2, HJBleApplication.getAppContext().getString(R.string.request_permission_err));
                                return;
                            } else if (HJBleApplication.getUser().getStrPermissions().contains("PRO_MANAGEMENT")||HJBleApplication.getUser().getStrPermissions().contains("PRO_Qualification")||HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {
                                String guid=AppUtils.getUUID(LoginActivity.this);
                                String deviceBuildUUID=AppUtils.getDeviceBuildUUID(LoginActivity.this);
                                if (!HJBleApplication.getUser().getUsername().startsWith("lhtest")&&!StringUtils.isEmpty(HJBleApplication.getUser().getGuid())&&!HJBleApplication.getUser().getGuid().equals(deviceBuildUUID)&&!HJBleApplication.getUser().getGuid().equals(guid)&&HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){

                                    new HitiStringDailog(LoginActivity.this,Constant.HitiStrTargetDialog_PRO_BLE_FIX_Disable,"",HJBleApplication.getAppContext().getString(R.string.bind_device_dlg_title),deviceBuildUUID+"\n"+HJBleApplication.getAppContext().getString(R.string.reset_bind_device),LoginActivity.this);
                                    return;
                                }
                                HJBleApplication.syscUserInfo();
                                startActivity(new Intent(LoginActivity.this, ScanBleActivity.class));
                               if (!StringUtils.isEmpty(deviceBuildUUID)&&HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")&&!deviceBuildUUID.equals(HJBleApplication.getUser().getGuid()))bingPhoneGuid(deviceBuildUUID);//20230403 陈翔 因为APP UUID会卸载重装后变动，故改成硬件ID替换
                            } else {
                                showMsg(2, HJBleApplication.getAppContext().getString(R.string.not_pro_permission));
                                return;
                            }

                        } else {
                            showMsg(2, HJBleApplication.getAppContext().getString(R.string.request_role_empty));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        loadingDialog.dismiss();
                        showMsg(2, HJBleApplication.getAppContext().getString(R.string.request_role_empty));

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void bingPhoneGuid(String devID) {
        loadingDialog.show();
        Map<String,Object> params=new HashMap<>();
        params.put("guid",devID);
        RetrofitUtils.getApiService().bingPhoneGuid(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Result>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Result result) {
                        loadingDialog.dismiss();
                        System.out.println("bingPhoneGuid-->"+ result.getCode());
                        if (result.getCode() == Constant.SuccessCode) {


                        } else {

                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        loadingDialog.dismiss();
                       // Toast.makeText(LoginActivity.this, "未获取到用户角色信息", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position){
            case Constant.HitiStrTargetDialog_PRO_BLE_FIX_Disable:

                break;
        }
    }
}
