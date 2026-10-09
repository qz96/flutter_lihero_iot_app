package com.lh.lhble.UI.View;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * 解决与SwifRefreshLayout 下拉刷新冲突的问题
 * Created by tom on 2018/8/20.
 */

public class MyWebView extends WebView {
    private OnScrllChanged mOnScrollChanged;

    public MyWebView(Context context) {
        super(context);
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChanged != null) {
            mOnScrollChanged.onScroll(l, t, oldl, oldt);
        }
    }

    public void setOnMyScrollChanged(OnScrllChanged onScrllChanged) {
        this.mOnScrollChanged = onScrllChanged;
    }

    public interface OnScrllChanged {
        void onScroll(int l, int t, int oldl, int oldt);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(this.getScrollY() <= 0)
                    this.scrollTo(0,1);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_DOWN: {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            break;
        }
        return super.dispatchTouchEvent(ev);
    }

    //实例化WebViwe后，调用此方法可滚动到底部

    public void scrollToBottom() {
        scrollTo(0, computeVerticalScrollRange());
    }
}
