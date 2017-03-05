package com.nalbandian.michael.smartteleprompter.services;

import android.app.IntentService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nalbandian.michael.smartteleprompter.R;

/**
 * Created by nalbandianm on 3/4/2017.
 */

public class ScrollingAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = ScrollingAsyncTask.class.getSimpleName();
    private boolean mTwoPane = false;
    private boolean mRun = false;
    private boolean mPause = false;
    private TextView speech_view;
    private TextView countdown_timer;
    private View top_overlay;
    private View bottom_overlay;
    private ScrollView scroll_view;
    private int mCountDown = 5;
    private int mLines = 0;
    private SpannableString speechTextStyled = null;
    private String speechText = null;
    private boolean mMoreLines = false;
    private DisplayMetrics mDisplaymetrics = new DisplayMetrics();
    private int mTextColor = 0x000000;
    private int mSpeed = 4;
    private int mFontSize = 20;
    private boolean mLoaded = false;
    private Context mContext;

    public ScrollingAsyncTask(Context context, TextView speechview, ScrollView scrollView, View top, View bottom, TextView countdown, String speech, int textColor, int fontSize, int speed, DisplayMetrics displayMetrics) {
        super();
        mContext = context;
        speech_view = speechview;
        scroll_view = scrollView;
        top_overlay = top;
        bottom_overlay = bottom;
        countdown_timer = countdown;
        speechText = speech;
        mLines = speech_view.getLineCount();
        mTextColor = textColor;
        mFontSize = fontSize;
        mSpeed = speed;
        mDisplaymetrics = displayMetrics;
    }

    public ScrollingAsyncTask() {
        super();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        mTwoPane = mContext.getResources().getBoolean(R.bool.isTablet);
        speechTextStyled = new SpannableString(speechText);
        handler.post(runnableCode);
        return null;
    }

    public void startStop(boolean value){
        Log.d(TAG, "Shutdown!");
        mRun = value;
    }

    public void pauseResume(boolean value){
        mPause = value;
    }

    // Create the Handler object (on the main thread by default)
    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {

            if(mRun) {
                if(!mLoaded) {
                    if(speechText != null) {
                        TypedValue outValue = new TypedValue();
                        mContext.getResources().getValue(R.dimen.line_height, outValue, true);
                        float line_height = outValue.getFloat();

                        float bottom_height = mFontSize * line_height * 15;
                        ViewGroup.LayoutParams bottom_params = bottom_overlay.getLayoutParams();

                        bottom_params.height = (int) (bottom_overlay.getBottom() - bottom_height);

                        bottom_overlay.setLayoutParams(bottom_params);
                        highlightText(true);
                        mLoaded = true;
                    } else {
                        handler.postDelayed(runnableCode, 100);
                        return;
                    }
                }
                if(mPause){
                    handler.postDelayed(runnableCode, 100);
                } else {
                    if (mCountDown >= 0) {
                        if (countdown_timer.getVisibility() == View.GONE) {
                            countdown_timer.setVisibility(View.VISIBLE);
                        }
                        showCountDown();
                        mCountDown--;
                        if (mRun) {
                            handler.postDelayed(runnableCode, 1000);
                        }
                    }
                    if (mCountDown == -1) {
                        speech_view.requestFocus();
                        countdown_timer.setVisibility(View.GONE);
                        speechTextStyled = new SpannableString(speechText);

                        ViewGroup.LayoutParams top_params = top_overlay.getLayoutParams();
                        ViewGroup.LayoutParams bottom_params = bottom_overlay.getLayoutParams();
                        if (!mMoreLines) {
                            top_params.height += 2;
                            top_overlay.setLayoutParams(top_params);


                            if (bottom_params.height - 2 < 0) {
                                bottom_params.height = 0;
                            } else {
                                bottom_params.height -= 2;
                            }
                            bottom_overlay.setLayoutParams(bottom_params);
                        } else {
                            scroll_view.scrollTo(0, scroll_view.getScrollY() + 2);
                        }
                        highlightText(false);
                        
                    }
                }
            }
        }
    };

    private void highlightText(boolean initalLoad) {
        Rect bounds = new Rect();
        int line_start = 0;
        int line_end = 0;
        ViewGroup.LayoutParams bottom_params = bottom_overlay.getLayoutParams();
        if(mContext == null){
            startStop(false);
            return;
        }
        for (int index = 0; index < mLines; index++) {
            speech_view.getLineBounds(index, bounds);
            if ((mTwoPane? bounds.top + dpToPx(mContext.getResources().getDimension(R.dimen.speech_side_margin))
                    : bounds.top) < top_overlay.getBottom() + scroll_view.getScrollY()) {
                line_start = speech_view.getLayout().getLineStart(index);
            }
            if (bounds.bottom < (mTwoPane ? bottom_overlay.getTop() - dpToPx(mContext.getResources().getDimension(R.dimen.speech_side_margin))
                    : bottom_overlay.getTop()) + scroll_view.getScrollY()) {
                line_end = speech_view.getLayout().getLineEnd(index);
            }

        }
        // Validate there are more lines off screen to force scrollView to scroll
        if ((top_overlay.getBottom() <= mDisplaymetrics.heightPixels / 2 && (mTwoPane ? bottom_overlay.getTop() - dpToPx(mContext.getResources().getDimension(R.dimen.list_side_margin))
                : bottom_overlay.getTop()) > mDisplaymetrics.heightPixels / 2) &&
                (!mTwoPane ? bounds.bottom > (scroll_view.getBottom() + scroll_view.getScrollY()) :
                        (scroll_view.getMaxScrollAmount() >  scroll_view.getScrollY()))) {
            mMoreLines = true;
        } else {
            mMoreLines = false;
        }
        // Emphasize the text in the prompter window
        if (line_start < line_end || initalLoad) {
            if (line_start != 0) {
                speechTextStyled.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, line_start, 0);
            }
            if(line_start < line_end ) {
                speechTextStyled.setSpan(new ForegroundColorSpan(mTextColor), line_start, line_end, 0);
            }
            if (line_end != speechText.length() - 1) {
                speechTextStyled.setSpan(new ForegroundColorSpan(Color.LTGRAY), line_end, speechText.length(), 0);
            }
        }
        speech_view.setText(speechTextStyled);
        // Check we have not reached the end.
        if (bottom_params.height > 0 && mRun && !initalLoad) {
            handler.postDelayed(runnableCode, mSpeed);
        }
    }

    private int dpToPx(float dp) {
        return Math.round(dp * (mDisplaymetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void showCountDown(){
        countdown_timer.setContentDescription(""+mCountDown);
        countdown_timer.setText(""+mCountDown);
        countdown_timer.setFocusable(true);
        countdown_timer.requestFocus();
    }

}
