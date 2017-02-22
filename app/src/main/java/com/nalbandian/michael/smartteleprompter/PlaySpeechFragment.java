package com.nalbandian.michael.smartteleprompter;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nalbandianm on 2/14/2017.
 */

public class PlaySpeechFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String SPEECH_URI = "URI";
    private boolean mRun = false;
    private boolean mPause = false;
    private View mRootView;
    @BindView(R.id.play_speech_display) TextView speech_view;
    @BindView(R.id.countdowntimer) TextView countdown_timer;
    @BindView(R.id.play_speech_top_overlay) View top_overlay;
    @BindView(R.id.play_speech_bottom_overlay) View bottom_overlay;
    @BindView(R.id.play_speech_scroll_view) ScrollView scroll_view;
    @BindView(R.id.play_speech_frame) FrameLayout frame_layout;
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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


        speech_view.setBackgroundColor(mBackgroundColor);
        speech_view.setTextSize(mFontSize);
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(SPEECH_LOADER, null, this);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mDisplaymetrics);
        super.onActivityCreated(savedInstanceState);
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
        TypedValue outValue = new TypedValue();
        getResources().getValue(R.dimen.line_height, outValue, true);
        float line_height = outValue.getFloat();

        float bottom_height = mFontSize * line_height * 15;

        ViewGroup.LayoutParams bottom_params = bottom_overlay.getLayoutParams();
        bottom_params.height = (int) (bottom_overlay.getBottom() - bottom_height);
        bottom_overlay.setLayoutParams(bottom_params);
        speech_view.setText(data.getString(COL_SPEECH) + "\n");
        mLines = speech_view.getLineCount();
        speechText = data.getString(COL_SPEECH) + "\n";
        speechTextStyled = new SpannableString(speechText);
        startStop(true);
        handler.post(runnableCode);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
    public void startStop(boolean value){
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
                if(mPause){
                    handler.postDelayed(runnableCode, 100);
                } else {
                    highlightText();
                    speech_view.setText(speechTextStyled);
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
                        int line_start = highlightText();
                        speech_view.setText(speechTextStyled);
                        // Check we have not reached the end.
                        if (bottom_params.height > 0 && mRun && (line_start != speech_view.getLayout().getLineStart(mLines - 1))) {
                            handler.postDelayed(runnableCode, mSpeed);
                        }
                    }
                }
            }
        }
    };

    private int highlightText() {
        Rect bounds = new Rect();
        int line_start = 0;
        int line_end = 0;
        for (int index = 0; index < mLines; index++) {
            speech_view.getLineBounds(index, bounds);
            if (bounds.top < top_overlay.getBottom() + scroll_view.getScrollY()) {
                line_start = speech_view.getLayout().getLineStart(index);
            }
            if (bounds.bottom < bottom_overlay.getTop() + scroll_view.getScrollY()) {
                line_end = speech_view.getLayout().getLineEnd(index);
            }

        }
        // Validate there are more lines off screen to force scrollView to scroll
        if ((top_overlay.getBottom() <= mDisplaymetrics.heightPixels / 2 && bottom_overlay.getTop() > mDisplaymetrics.heightPixels / 2) &&
                bounds.bottom > (scroll_view.getBottom() + scroll_view.getScrollY())) {
            mMoreLines = true;
        } else {
            mMoreLines = false;
        }
        // Emphasize the text in the prompter window
        if (line_start < line_end) {
            if (line_start != 0) {
                speechTextStyled.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, line_start, 0);
            }
            speechTextStyled.setSpan(new ForegroundColorSpan(mTextColor), line_start, line_end, 0);
            if (line_end != speechText.length() - 1) {
                speechTextStyled.setSpan(new ForegroundColorSpan(Color.LTGRAY), line_end, speechText.length(), 0);
            }
        }
        return line_start;
    }

    private void showCountDown(){
        countdown_timer.setText(""+mCountDown);
    }


}
