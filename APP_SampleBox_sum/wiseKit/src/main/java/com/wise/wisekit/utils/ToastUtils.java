package com.wise.wisekit.utils;

import android.content.Context;
import android.widget.Toast;

import es.dmoral.toasty.Toasty;


/**
 * Created by Vincent on 2017/8/29.
 */

public class ToastUtils {

    private static Toast toast;
    public static void showSuccessSingleMsg(Context context, String msg){
        if(toast!=null){
            toast.cancel();
        }

       // ViseLog.d("showSuccessSingleMsg:"+msg);
        toast= Toasty.success(context,msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showErrorSingleMsg(Context context, String msg){
        if(toast!=null){
            toast.cancel();
        }

       // ViseLog.d("showErrorSingleMsg:"+msg);

        toast = Toasty.error(context,msg, Toast.LENGTH_LONG);
        toast.show();

    }

    public static void showInfoSingleMsg(Context context, String msg){
       // ViseLog.d("showInfoSingleMsg:"+msg);
        if(toast!=null){
            toast.cancel();
        }

        toast = Toasty.info(context,msg, Toast.LENGTH_LONG);
        toast.show();
    }
}
