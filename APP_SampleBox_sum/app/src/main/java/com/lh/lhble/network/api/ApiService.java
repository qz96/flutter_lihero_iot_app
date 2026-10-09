package com.lh.lhble.network.api;




import com.lh.lhble.Entity.LoginResult;
import com.lh.lhble.Entity.Result;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.QueryMap;

/**
 * @class describe api请求在这里配置
 * @anthor admin
 * @change
 * @chang time
 * @class describe
 */

public interface ApiService {


    /**
     * 登录验证
     *
     * @return
     */

    @POST("oauth/token")
    Observable<LoginResult> loginToken(@Header("X-Pack-P") String xpp, @Header("X-Pack-U") String xpu, @Header("X-Pack-C") String xpc);

    /**
     * 获取用户基础信息
     * @return
     */

    @GET("basic/sysmng/user/info")
    Observable<Result> getUserInfo();

    /**
     * APP在线升级
     * @return
     */

    @GET("basic/common/app/getUpgrade?")
    Observable<Result> appUpgrade(@QueryMap Map<String, Object> params);
    /**
     * 获取用户界面菜单
     * @return
     */

    @GET("basic/sysmng/user/getUserMenus?appType=1")
    Observable<Result> getUserMenus();

    /**
     * 获取用户所有业务项目
     * @return
     */

    @GET("basic/sysmng/project/listUserProjects")
    Observable<Result> listUserProjects();


    /**
     * 获取项目采测配置
     * @return
     */

    @GET("basic/sysmng/project/getProjectSampleConfig?")
    Observable<Result> getProjectSampleConfig(@QueryMap Map<String, String> params);


    /**
     * 上传蓝牙瓶操作日志
     */
    @POST("sample/bottle/log/insertLog")
    @Headers({"Content-Type: application/json;charset=UTF-8"})
    Observable<Result> insertLog(@Body Object object);


    /**
     * 根据蓝牙地址获取采样时间最新的一条样品信息
     * @return
     */

    @GET("sample/info/getLatestSampleInfoByBlueMac?")
    Observable<Result> getLatestSampleInfoByBlueMac(@QueryMap Map<String,Object> params);

    /**
     * 上传蓝牙瓶操作日志
     */
    @POST("basic/sysmng/user/bingPhoneGuid")
    @Headers({"Content-Type: application/json;charset=UTF-8"})
    Observable<Result> bingPhoneGuid(@QueryMap Map<String, Object> params);

}