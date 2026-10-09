package com.lh.lhble.network;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.lh.lhble.BuildConfig;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.network.api.ApiService;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Retrofit;

/**
 * description ：
 * project name：MyAppProject
 * author : Vincent
 * creation date: 2017/2/23 10:12
 *
 * @version 1.0
 */

public class RetrofitUtils {

    private static Retrofit retrofit;
    private static final long NETWORK_TIME_OUT =30 * 1000L;//网络超时30秒
    private static OkHttpClient okHttpClient;
    /**
     * 获取一个默认配置BaseUrl地址的
     *
     * @return
     */
    private static Retrofit getRetrofit() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(NETWORK_TIME_OUT, TimeUnit.MILLISECONDS)
                    .readTimeout(NETWORK_TIME_OUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(3*NETWORK_TIME_OUT, TimeUnit.MILLISECONDS)
                    .retryOnConnectionFailure(false)
                    .connectionPool(new ConnectionPool(25,NETWORK_TIME_OUT,TimeUnit.MILLISECONDS))
                    .addInterceptor(setInterceptor()).build();
        }
        return new Retrofit.Builder()
                .baseUrl(Constant.SERVICE_API_ADDRESS)
                .addConverterFactory(FastJsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build();
    }

    /**
     * 动态配置BaseUrl
     *
     * @param baseUrl
     * @return
     */
    private static Retrofit getRetrofit(String baseUrl) {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(NETWORK_TIME_OUT, TimeUnit.MILLISECONDS)
                    .readTimeout(NETWORK_TIME_OUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(3*NETWORK_TIME_OUT, TimeUnit.MILLISECONDS)
                    .retryOnConnectionFailure(false)
                    .connectionPool(new ConnectionPool(25,NETWORK_TIME_OUT,TimeUnit.MILLISECONDS))
                    .addInterceptor(setInterceptor()).build();
        }
        return retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(FastJsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build();
    }


    /**
     * 拦截器配置和添加基础头部信息
     *
     * @return
     */
    private static Interceptor setInterceptor() {
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response;
                if (request.url().encodedPath().contains("basic/sysmng/project/getUserProjectsByUserName")){
                     response = chain.proceed(request);
                }else {
                    Request request1 = request.newBuilder()
                            .addHeader("Authorization","Bearer "+ HJBleApplication.getUser().getUserToken())
                            .method(request.method(), request.body())
                            .build();
                    try {
                        response = chain.proceed(request1);
                    }catch (SocketException e){
                        e.printStackTrace();
                        try {
                            okHttpClient.dispatcher().executorService().shutdown();
                            okHttpClient.connectionPool().evictAll();
                            if (okHttpClient.cache()!=null)okHttpClient.cache().close();
                            okHttpClient=null;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        throw e;
                    }catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        try {
                            okHttpClient.dispatcher().executorService().shutdown();
                            okHttpClient.connectionPool().evictAll();
                            if (okHttpClient.cache()!=null) okHttpClient.cache().close();
                            okHttpClient=null;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        throw e;
                    }catch (UnknownHostException e) {
                        e.printStackTrace();
                        try {
                            okHttpClient.dispatcher().executorService().shutdown();
                            okHttpClient.connectionPool().evictAll();
                            if (okHttpClient.cache()!=null) okHttpClient.cache().close();
                            okHttpClient=null;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        throw e;
                    } catch (IOException e) {
                        throw e;
                    }
                }


                if (BuildConfig.DEBUG) {
                    MediaType contentType = null;
                    String bodyString = null;
                    String requestBodyString=null;
                    if (response.body() != null) {
                        contentType = response.body().contentType();
                        bodyString = response.body().string();

                    }
                    if (request.body()!=null){
                        contentType=request.body().contentType();
                    }


                    if (response.body() != null) {


                        ResponseBody body = ResponseBody.create(contentType, bodyString);
                        return response.newBuilder().body(body).build();
                    } else {

                        return response;
                    }
                }

                return response;
            }
        };
        return interceptor;
    }

    /**
     * 请求的参数转String
     * @param request
     * @return
     */
    private static String getRequestBodyToString(Request request) {
        if (request.body() != null) {
            try {
                final Request copy = request.newBuilder().build();
                final Buffer buffer = new Buffer();
                copy.body().writeTo(buffer);
                return buffer.readUtf8();
            } catch (final IOException e) {
                return "";
            }
        }
        return "";
    }

    public static ApiService getApiService() {
        HJBleApplication.getUser();
        return getRetrofit().create(ApiService.class);
    }

    public static ApiService getApiService(String baseUrl) {
        return getRetrofit(baseUrl).create(ApiService.class);
    }
}
