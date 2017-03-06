package com.nalbandian.michael.smartteleprompter.services;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nalbandian.michael.smartteleprompter.R;

import java.util.Locale;

/**
 * Created by nalbandianm on 3/4/2017.
 */

public class AccessibilityHelperAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = AccessibilityHelperAsyncTask.class.getSimpleName();
    private boolean mRun = false;
    private boolean mPause = false;
    private TextView speech_view;
    private int mCountDown = 5;
    private String speechText = null;
    private TextToSpeech mTTS;

    private Context mContext;

    public AccessibilityHelperAsyncTask(Context context, TextView speechview, String speech, boolean run) {
        super();
        mContext = context;
        speech_view = speechview;
        speechText = speech;
        mRun = run;
    }

    public AccessibilityHelperAsyncTask() {
        super();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        mTTS = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.US);
                    if (result == 1) {
                        mRun = true;
                        handler.postDelayed(runnableCode, 2000);
                    }
                }
            }
        });

        return null;
    }

    public void startStop(boolean value){
        Log.d(TAG, "Shutdown!");
        mRun = value;
    }

    public void pauseResume(boolean value){
        mPause = value;
    }

    Handler handler = new Handler();
    // Define the code block to be executed
    Runnable runnableCode = new Runnable() {
        boolean initialized = false;
        @Override
        public void run() {
            if(mRun) {
                if (mCountDown > 0) {
                    if(!initialized){
                        mTTS.speak(mContext.getString(R.string.begin_speech), TextToSpeech.QUEUE_ADD, null);
                        initialized = true;
                    }

                    while (mTTS.isSpeaking()) {
                    }
                    mTTS.speak("" + mCountDown--, TextToSpeech.QUEUE_FLUSH, null);
                    handler.postDelayed(runnableCode, 1000);
                } else {
                    mTTS.setSpeechRate(.75f);
                    mTTS.speak(speechText, TextToSpeech.QUEUE_FLUSH, null);
                }
            } else {
                mTTS.shutdown();
            }

        };

    };

}
