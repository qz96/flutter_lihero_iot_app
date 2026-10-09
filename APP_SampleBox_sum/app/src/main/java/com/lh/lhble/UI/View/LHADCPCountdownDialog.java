package com.lh.lhble.UI.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.lh.lhble.R;

/**
 * LHADCP倒计时对话框
 * 用于自动模式测量时的倒计时显示
 */
public class LHADCPCountdownDialog {

    private Context mContext;
    private TextView tvCountdown;
    protected Dialog dialog;
    private Handler countdownHandler;
    private OnCountdownFinishListener mListener;

    public interface OnCountdownFinishListener {
        void onFinish();
    }

    /**
     * 构造方法
     *
     * @param context 上下文
     * @param duration 倒计时时长（秒）
     * @param listener 倒计时结束监听
     */
    public LHADCPCountdownDialog(Context context, int duration, OnCountdownFinishListener listener) {
        this.mContext = context;
        this.mListener = listener;

        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(R.layout.dlg_countdown, null);
        // 创建自定义样式的Dialog
        dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        tvCountdown = (TextView) view.findViewById(R.id.tv_input_title);

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (mContext != null && mContext instanceof Activity) {
            if (!((Activity) mContext).isFinishing()) {
                dialog.show();
                startCountdown(duration);
            }
        }
    }

    /**
     * 开始倒计时
     */
    private void startCountdown(final int duration) {
        countdownHandler = new Handler();
        final int[] remaining = {duration};

        updateCountdownText(remaining[0]);

        countdownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                remaining[0]--;
                if (remaining[0] > 0) {
                    updateCountdownText(remaining[0]);
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    // 倒计时结束
                    updateCountdownText(0);
                    if (mListener != null) {
                        mListener.onFinish();
                    }
                    dismiss();
                }
            }
        }, 1000);
    }

    /**
     * 更新倒计时文本
     */
    private void updateCountdownText(int seconds) {
        if (tvCountdown != null) {
            String text = "测量中...\n" + seconds + "秒";
            tvCountdown.setText(text);
        }
    }

    /**
     * 关闭对话框
     */
    public void dismiss() {
        if (countdownHandler != null) {
            countdownHandler.removeCallbacksAndMessages(null);
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * 判断对话框是否正在显示
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
