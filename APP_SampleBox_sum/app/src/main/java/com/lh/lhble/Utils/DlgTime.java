package com.lh.lhble.Utils;

import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.lh.lhble.R;

import java.util.Calendar;
import java.util.Date;

import io.reactivex.annotations.NonNull;

public class DlgTime {

    public static void showYMDDialog(@NonNull FragmentActivity activity, Calendar defaultDate, Calendar startDate, Calendar endDate,
                                     Boolean year, Boolean month, Boolean day, Boolean hours, Boolean mintues, Boolean secondes,
                                     final OnDateSelectedFinishCallback callback) {
        TimePickerView pvTime = (new TimePickerBuilder(activity, new OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {
                if (callback != null) {
                    callback.onDateSelected(date);
                }

            }
        })).setType(new boolean[]{year, month, day, hours, mintues, secondes}).setRangDate(startDate, endDate).setDate(defaultDate).isDialog(true).build();
        Dialog mDialog = pvTime.getDialog();
        if (null != mDialog) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2, 150);
            params.leftMargin = 0;
            params.rightMargin = 0;
            pvTime.getDialogContainerLayout().setLayoutParams(params);
            Window dialogWindow = mDialog.getWindow();

            if (dialogWindow != null) {
                dialogWindow.setWindowAnimations(R.style.picker_view_slide_anim);
                dialogWindow.setGravity(150);
            }

            pvTime.show();
        }
    }

    public interface OnDateSelectedFinishCallback {
        void onDateSelected(Date var1);
    }
}
