package com.nalbandian.michael.smartteleprompter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumerImpl;
import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;
import com.nalbandian.michael.smartteleprompter.services.AccessibilityHelperAsyncTask;
import com.nalbandian.michael.smartteleprompter.services.ScrollingAsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN;

/**
 * Created by nalbandianm on 2/14/2017.
 */

public class PlaySpeechFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String SPEECH_URI = "URI";
    public static final String ACTION = "action";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String SETUP = "setup";
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String STOP = "stop";

    public static final String TAG = PlaySpeechFragment.class.getSimpleName();

    private boolean mTwoPane = false;
    private boolean mRun = false;
    private boolean mPause = false;
    private View mRootView;
    @BindView(R.id.play_speech_display) TextView speech_view;
    @BindView(R.id.countdowntimer) TextView countdown_timer;
    @BindView(R.id.play_speech_top_overlay) View top_overlay;
    @BindView(R.id.play_speech_bottom_overlay) View bottom_overlay;
    @BindView(R.id.play_speech_scroll_view) ScrollView scroll_view;
    private String[] lines;

    Toolbar toolbar;
    private Uri mUri;

    private static final int SPEECH_LOADER = 86;
    private static final String[] SPEECH_COLUMNS = {
            SpeechColumns._ID,
            SpeechColumns.TITLE,
            SpeechColumns.SPEECH
    };

    static final int COL_ID = 0;
    static final int COL_TITLE = 1;
    static final int COL_SPEECH = 2;

    private int mCountDown = 5;
    private int mLines = 0;
    private SpannableString speechTextStyled = null;
    private String speechText = null;
    private boolean mMoreLines = false;
    private DisplayMetrics mDisplaymetrics = new DisplayMetrics();
    private int mBackgroundColor = 0x000000;
    private int mTextColor = 0x000000;
    private int mSpeed = 4;
    private int mFontSize = 20;
    private boolean mLoaded = false;
    private Menu mMenu;
    private DataCastManager mDataCastManager;
    private static SpeechCastChannel mChannel;
    private boolean mAccessibility = false;
    private ScrollingAsyncTask mScrollingAsyncTask;
    AccessibilityManager mAccessibilityManager;
    private TextToSpeech mTTS;
    private boolean notInitialized = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTwoPane = getResources().getBoolean(R.bool.isTablet);
        //DataCastManager.checkGooglePlayServices(getActivity());
        mChannel = new SpeechCastChannel();
        mDataCastManager = DataCastManager.getInstance();
        mDataCastManager.reconnectSessionIfPossible();
        mDataCastManager.addDataCastConsumer(mChannel);
        AccessibilityManager mAccessibilityManager = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        mAccessibility = mAccessibilityManager.getEnabledAccessibilityServiceList(FEEDBACK_SPOKEN).size() > 0;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.play_speech_fragment, container, false);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(SPEECH_URI);
        }

        ButterKnife.bind(this, mRootView);
        mBackgroundColor = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.pref_background_color), getResources().getInteger(R.integer.COLOR_WHITE));
        mTextColor = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.pref_text_color), getResources().getInteger(R.integer.COLOR_BLACK));
        mSpeed = 1000 / (PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.pref_scroll_speed), getResources().getInteger(R.integer.SCROLL_DEFAULT)));
        mFontSize = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.pref_font_size), getResources().getInteger(R.integer.FONT_SIZE_DEFAULT));
        if(mDataCastManager.isConnected() || mAccessibility){
            top_overlay.setVisibility(View.GONE);
            bottom_overlay.setVisibility(View.GONE);
        } else {
            speech_view.setBackgroundColor(mBackgroundColor);
        }
        speech_view.setTextSize(mFontSize);
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(SPEECH_LOADER, null, this);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mDisplaymetrics);
        lockOrientation(getActivity());
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        mDataCastManager.incrementUiCounter();
        if(!mDataCastManager.isConnected() && mScrollingAsyncTask != null){
            mScrollingAsyncTask.pauseResume(false);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        mDataCastManager.incrementUiCounter();
        if(!mDataCastManager.isConnected() && mScrollingAsyncTask != null){
            mScrollingAsyncTask.pauseResume(true);
        }

        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(!mTwoPane) {
            menu.clear();
            inflater.inflate(R.menu.play_speech, menu);
            mDataCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
            mMenu = menu;

        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home || id == R.id.stop) {
            if(mScrollingAsyncTask != null) {
                mScrollingAsyncTask.startStop(false);
            }
            if(mDataCastManager.isConnected()) {
                sendDataMessage(mChannel, ACTION, STOP);
            }
            mTTS.shutdown();
            getActivity().onBackPressed();
            return true;
        }  else if (id == R.id.resume){
            if(mScrollingAsyncTask != null){
                mScrollingAsyncTask.pauseResume(false);
            }
            mMenu.findItem(R.id.resume).setVisible(false);
            mMenu.findItem(R.id.pause).setVisible(true);
            if(mDataCastManager.isConnected()) {
                sendDataMessage(mChannel, ACTION, PLAY);
            }
        } else if (id == R.id.pause){
            if(mScrollingAsyncTask != null) {
                mScrollingAsyncTask. pauseResume(true);
            }
            mMenu.findItem(R.id.pause).setVisible(false);
            mMenu.findItem(R.id.resume).setVisible(true);
            if(mDataCastManager.isConnected()) {
                sendDataMessage(mChannel, ACTION, PAUSE);
            }
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(mUri != null){
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    SPEECH_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        speech_view.setText(data.getString(COL_SPEECH) + "\n");
        mLines = speech_view.getLineCount();
        speechText = data.getString(COL_SPEECH) + "\n";
        if(mAccessibility){
            speech_view.setContentDescription("\u00A0");
        }
        if(!mTwoPane) {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(data.getString(COL_TITLE));

        } else {
            Toolbar toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
            toolbar.setTitle(data.getString(COL_TITLE));
            toolbar.setTitleTextColor(getResources().getColor(R.color.titleColor));
            toolbar.inflateMenu(R.menu.play_speech);
            if(mAccessibility) {
                toolbar.setContentDescription("\u00A0");
            }
            mMenu = toolbar.getMenu();
            mDataCastManager.addMediaRouterButton(mMenu, R.id.media_route_menu_item);

            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int id = item.getItemId();
                    if (id == android.R.id.home || id == R.id.stop) {
                        if(mDataCastManager.isConnected()) {
                            sendDataMessage(mChannel, ACTION, STOP);
                        }
                        mScrollingAsyncTask.startStop(false);
                        getActivity().finish();
                        return true;
                    }
                    else if (id == R.id.resume){
                        mScrollingAsyncTask.pauseResume(false);
                        mMenu.findItem(R.id.resume).setVisible(false);
                        mMenu.findItem(R.id.pause).setVisible(true);
                        if(mDataCastManager.isConnected()) {
                            sendDataMessage(mChannel, ACTION, PLAY);
                        }
                    } else if (id == R.id.pause){
                        mScrollingAsyncTask.pauseResume(true);
                        mMenu.findItem(R.id.pause).setVisible(false);
                        mMenu.findItem(R.id.resume).setVisible(true);
                        if(mDataCastManager.isConnected()) {
                            sendDataMessage(mChannel, ACTION, PAUSE);
                        }
                    }

                    return true;
                }
            });
        }

        speechTextStyled = new SpannableString(speechText);
        if(mDataCastManager.isConnected()) {
            sendDataMessage(mChannel, SETUP, speechText);
            speech_view.setText(speechTextStyled);
        } else {
            if (mAccessibility) {
                new AccessibilityHelperAsyncTask(getActivity(), speech_view, speechText, mRun).execute();
            }else{
                mScrollingAsyncTask = new ScrollingAsyncTask(getActivity(), speech_view, scroll_view, top_overlay, bottom_overlay, countdown_timer, speechText, mTextColor, mFontSize, mSpeed, mDisplaymetrics);
                mScrollingAsyncTask.startStop(true);
                mScrollingAsyncTask.execute();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }




    private static void lockOrientation(Activity activity) {
        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int tempOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
        switch(tempOrientation)
        {
            case Configuration.ORIENTATION_LANDSCAPE:
                if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
        activity.setRequestedOrientation(orientation);
    }

    public void sendDataMessage(SpeechCastChannel channel, String type, String value){
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE, type);
            json.put(VALUE, value);

            mDataCastManager.sendDataMessage(json.toString(), channel.getNamespace());

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * Custom message channel
     */
    class SpeechCastChannel extends DataCastConsumerImpl {

        private final String TAG = SpeechCastChannel.class.getSimpleName();

        /**
         * @return custom namespace
         */
        public String getNamespace() {
            return getString(R.string.namespace);
        }

        @Override
        public void onApplicationStatusChanged(String appStatus) {
            super.onApplicationStatusChanged(appStatus);
            if("".equals(appStatus) && getActivity() != null) {
                Intent intent = new Intent(getActivity(), PlaySpeechActivity.class).setData(mUri);
                startActivity(intent);
                getActivity().finish();
            }
        }

        /*
                         * Receive message from the receiver app
                         */
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            Log.d(TAG, "onMessageReceived: " + message);
        }

        @Override
        public void onMessageSendFailed(Status status) {
            Log.d(TAG, "onMessageSendFailed: " + status);
        }

        @Override
        public void onDisconnected() {
            super.onDisconnected();
        }

        @Override
        public void onDisconnectionReason(int reason) {
            if(getActivity() != null){
                Intent intent = new Intent(getActivity(), PlaySpeechActivity.class).setData(mUri);
                startActivity(intent);
                getActivity().finish();
            }
            super.onDisconnectionReason(reason);
        }
    }

}
