package com.nalbandian.michael.smartteleprompter;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;
import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;
import com.nalbandian.michael.smartteleprompter.data.SpeechProvider;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nalbandianm on 2/14/2017.
 */

public class SpeechFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String SPEECH_URI = "URI";
    private View mRootView;
    private TextView speech_title;
    @BindView(R.id.speech_view) TextView speech_view;
    @BindView(R.id.speech_adView) AdView mAdView;
    private Uri mUri;
    private Menu mMenu;
    private boolean mTwoPane = false;

    private static final int SPEECH_LOADER = 86;
    private static final String[] SPEECH_COLUMNS = {
            SpeechColumns._ID,
            SpeechColumns.TITLE,
            SpeechColumns.SPEECH
    };

    static final int COL_ID = 0;
    static final int COL_TITLE = 1;
    static final int COL_SPEECH = 2;
    private int mFontSize;
    private long mId;
    private Tracker mTracker;
    private DataCastManager mDataCastManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTwoPane = getResources().getBoolean(R.bool.isTablet);
        SmartTeleprompterApplication application = (SmartTeleprompterApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        mDataCastManager = DataCastManager.getInstance();
        mDataCastManager.reconnectSessionIfPossible();
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        mTracker.setScreenName(SpeechFragment.class.getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        mDataCastManager.incrementUiCounter();
        super.onResume();
    }

    @Override
    public void onPause() {
        mDataCastManager.decrementUiCounter();
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.speech_fragment, container, false);
        if(mRootView.findViewById(R.id.speech_title)!=null){
            speech_title = (TextView) mRootView.findViewById(R.id.speech_title);
        }
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(SPEECH_URI);
            mId = ContentUris.parseId(mUri);
        }
        ButterKnife.bind(this, mRootView);
        if(mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
        mFontSize = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.pref_font_size), getResources().getInteger(R.integer.FONT_SIZE_DEFAULT));
        speech_view.setTextSize(mFontSize);
        return mRootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.

        if(mUri != null) {
            menu.clear();
            inflater.inflate(R.menu.speech, menu);
            mDataCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.delete){
            getActivity().getContentResolver().delete(SpeechProvider.Speeches.CONTENT_URI, SpeechColumns._ID + "= ?", new String[]{""+mId});
            if(mTwoPane){
                SpeechFragment fragment = new SpeechFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.speech_container, fragment)
                        .commit();
            } else {
                Toast.makeText(getActivity(), getString(R.string.speech_deleted), Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
        } else if (id == R.id.edit) {
            if(mTwoPane) {
                Bundle args = new Bundle();
                args.putParcelable(SPEECH_URI, mUri);
                EditSpeechFragment fragment = new EditSpeechFragment();
                fragment.setArguments(args);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.speech_container, fragment)
                        .commit();
            } else {
                Intent intent = new Intent(getActivity(), EditSpeechActivity.class).setData(mUri);
                startActivity(intent);
            }
        } else if (id == android.R.id.home) {
            getActivity().finish();
        } else if (id == R.id.start_teleprompter) {
            Intent intent = new Intent(getActivity(), PlaySpeechActivity.class).setData(mUri);
            startActivity(intent);
        } else if (id == R.id.settings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(SPEECH_LOADER, null, this);

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
        if(!mTwoPane && getActivity() != null && ((AppCompatActivity)getActivity()).getSupportActionBar() != null){
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(data.getString(COL_TITLE));
        } else {
            speech_title.setText(data.getString(COL_TITLE));
        }
        speech_view.setText(data.getString(COL_SPEECH));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
