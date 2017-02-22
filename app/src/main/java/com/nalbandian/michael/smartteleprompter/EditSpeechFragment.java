package com.nalbandian.michael.smartteleprompter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by nalbandianm on 2/14/2017.
 */

public class EditSpeechFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private View mRootView;
    @BindView(R.id.title) TextView speech_title;
    @BindView(R.id.speech) TextView speech_view;

    private Uri mUri = null;
    public static final String SPEECH_URI = "URI";
    private static final int SPEECH_LOADER = 86;
    private static final String[] SPEECH_COLUMNS = {
            SpeechColumns._ID,
            SpeechColumns.TITLE,
            SpeechColumns.SPEECH
    };

    static final int COL_ID = 0;
    static final int COL_TITLE = 1;
    static final int COL_SPEECH = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.edit_speech_fragment, container, false);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(SPEECH_URI);
        }
        ButterKnife.bind(this, mRootView);
        return mRootView;
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
        speech_title.setText(data.getString(COL_TITLE));
        speech_view.setText(data.getString(COL_SPEECH));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
