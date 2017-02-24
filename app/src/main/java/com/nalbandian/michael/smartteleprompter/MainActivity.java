package com.nalbandian.michael.smartteleprompter;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;
import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;
import com.nalbandian.michael.smartteleprompter.data.SpeechProvider;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>{

    private static final String  TAG = "EditSpeechFragment";
    private static final int SPEECH_LOADER = 86;
    private static final String[] SPEECH_COLUMNS = {
            SpeechColumns._ID,
            SpeechColumns.TITLE,
            SpeechColumns.SPEECH
    };
    static final String SPEECH_URI = "URI";
    static final int COL_ID = 0;
    static final int COL_TITLE = 1;
    static final int COL_SPEECH = 2;

    private RecyclerView mRecyclerView;
    private Toolbar mToolbar;
    private boolean mTwoPane = false;
    private Tracker mTracker;
    private AdView mAdView;
    private DataCastManager mDataCastManager;
    private Parcelable mListState;
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SmartTeleprompterApplication application = (SmartTeleprompterApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFab = (FloatingActionButton) findViewById(R.id.add_fab);
        getLoaderManager().initLoader(SPEECH_LOADER, null, this);
        mAdView = (AdView) findViewById(R.id.adView);

        mTwoPane = getResources().getBoolean(R.bool.isTablet);
        if(mTwoPane) {
            if(savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.speech_container, new SpeechFragment(), TAG)
                        .commit();
            }
        } else {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                    if (dy > 0)
                        mFab.hide();
                    else if (dy < 0)
                        mFab.show();
                }
            });
        }

        DataCastManager.checkGooglePlayServices(this);
        mDataCastManager = DataCastManager.getInstance();
        mDataCastManager.reconnectSessionIfPossible();

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mListState = savedInstanceState.getParcelable("scroll_pos");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(outState != null && mRecyclerView !=null && mRecyclerView.getLayoutManager() != null) {
            outState.putParcelable("scroll_pos",mRecyclerView.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    protected void onPause() {
        mDataCastManager.decrementUiCounter();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "Setting screen name: " + MainActivity.class.getSimpleName());
        mTracker.setScreenName(MainActivity.class.getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        mDataCastManager.incrementUiCounter();
        if(mListState != null && mRecyclerView !=null && mRecyclerView.getLayoutManager() != null) {
            mRecyclerView.getLayoutManager().onRestoreInstanceState(mListState);
        }
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        mDataCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.settings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String sortOrder = SpeechColumns.TITLE + " ASC";
        Uri speechesUri = SpeechProvider.Speeches.CONTENT_URI;

        return new CursorLoader(this, speechesUri, SPEECH_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Adapter adapter = new Adapter(this, cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        if(mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mTwoPane){
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.speech_container, new EditSpeechFragment(), TAG)
                            .commit();
                } else {
                    Bundle bundle = null;
                    Intent intent = new Intent(getApplicationContext(),
                            EditSpeechActivity.class);
                    startActivity(intent, bundle);
                }
            }
        });



    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;
        private Activity mActivity;

        public Adapter(Activity activity, Cursor cursor) {
            mActivity = activity;
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(COL_ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_speech, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mTwoPane) {
                        Bundle args = new Bundle();
                        args.putParcelable(SPEECH_URI, SpeechProvider.Speeches.withId(getItemId(vh.getAdapterPosition())));
                        SpeechFragment fragment = new SpeechFragment();
                        fragment.setArguments(args);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.speech_container, fragment, TAG)
                                .commit();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), SpeechActivity.class).setData(SpeechProvider.Speeches.withId(getItemId(vh.getAdapterPosition())));
                        startActivity(intent);
                    }
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            String title = mCursor.getString(COL_TITLE);
            holder.titleView.setText(title);
            holder.titleView.setContentDescription(title);
            holder.speechView.setText(mCursor.getString(COL_SPEECH));
            holder.speechView.setContentDescription("\u00A0");

        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView titleView;
        public TextView speechView;

        public ViewHolder(View view) {
            super(view);
            titleView = (TextView) view.findViewById(R.id.title);
            speechView = (TextView) view.findViewById(R.id.speech);
        }
    }
}
