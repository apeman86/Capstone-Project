package com.nalbandian.michael.smartteleprompter;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.WindowManager;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;

import static com.nalbandian.michael.smartteleprompter.PlaySpeechFragment.SPEECH_URI;
import com.nalbandian.michael.smartteleprompter.PlaySpeechFragment.SpeechCastChannel;

import java.util.Locale;

/**
 * Created by nalbandianm on 2/16/2017.
 */

public class PlaySpeechActivity extends AppCompatActivity {

    private Uri mUri = null;
    private static PlaySpeechFragment mFragment = null;
    private Menu mMenu = null;
    private boolean mTwoPane;
    Tracker mTracker;
    private DataCastManager mDataCastManager;
    private SpeechCastChannel mChannel;
    private TextToSpeech mTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_speech_activity);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        mTracker =  ((SmartTeleprompterApplication) getApplication()).getDefaultTracker();
        mTwoPane = getResources().getBoolean(R.bool.isTablet);
        if(mTwoPane){
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = dpToPx(700);
            getWindow().setAttributes(params);
        }
        if (savedInstanceState == null) {

            Bundle arguments = new Bundle();
            if(mUri == null) {
                mUri = getIntent().getData();
            }
            arguments.putParcelable(SPEECH_URI, mUri);
            mFragment = new PlaySpeechFragment();
            mFragment.setArguments(arguments);


            getSupportFragmentManager().beginTransaction()
                    .add(R.id.play_speech_container, mFragment)
                    .commit();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mTTS != null) {
            mTTS.shutdown();
        }
    }

    @Override
    protected void onResume() {
        mTracker.setScreenName(PlaySpeechActivity.class.getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        super.onResume();
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

}
