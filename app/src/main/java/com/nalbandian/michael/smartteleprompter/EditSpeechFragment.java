package com.nalbandian.michael.smartteleprompter;

import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumerImpl;
import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;
import com.nalbandian.michael.smartteleprompter.data.SpeechProvider;
import com.nalbandian.michael.smartteleprompter.data.generated.values.SpeechesValuesBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import static com.nalbandian.michael.smartteleprompter.EditSpeechActivity.ACTION_DATA_UPDATED;

/**
 * Created by nalbandianm on 2/14/2017.
 */

public class EditSpeechFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ThesaurusDialogFragment.ThesaurusListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_CODE_OPENER = 1000;
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1001;
    private View mRootView;
    @BindView(R.id.title)
    TextView speech_title;
    @BindView(R.id.speech)
    TextView speech_view;
    @BindView(R.id.edit_speech_adView)
    AdView mAdView;
    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;
    @BindView(R.id.overlay)
    View mOverlay;
    private long mId = 0;
    private Menu mMenu = null;
    private Uri mUri = null;
    private String editedTitle = null;
    private String editedSpeech = null;
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
    private boolean mTwoPane;
    private Tracker mTracker;
    private GoogleApiClient mGoogleApiClient;
    private DriveId mSelectedFileDriveId;
    private DataCastManager mDataCastManager;
    private boolean mUserCancelImport = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SmartTeleprompterApplication application = (SmartTeleprompterApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        mTwoPane = getActivity().getResources().getBoolean(R.bool.isTablet);
//        DataCastManager.checkGooglePlayServices(getActivity());
        mDataCastManager = DataCastManager.getInstance();
//        mDataCastManager.reconnectSessionIfPossible();
        if (mSelectedFileDriveId != null) {
            mGoogleApiClient.connect();
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        mTracker.setScreenName(EditSpeechFragment.class.getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        mDataCastManager.incrementUiCounter();
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("title", speech_title.getText().toString());
        outState.putString("speech", speech_view.getText().toString());
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        mDataCastManager.decrementUiCounter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.edit_speech_fragment, container, false);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(SPEECH_URI);
            mId = ContentUris.parseId(mUri);
        }
        ButterKnife.bind(this, mRootView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(SPEECH_LOADER, null, this);
        if(savedInstanceState != null){
            editedTitle = savedInstanceState.getString("title");
            editedSpeech = savedInstanceState.getString("speech");
        }
        super.onActivityCreated(savedInstanceState);
    }

    public void setSpeechUri(Uri speechUri) {
        mUri = speechUri;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mUri != null) {
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
        mId = data.getLong(COL_ID);

        if(editedTitle != null) {
            speech_title.setText(editedTitle);
            editedTitle = null;
        } else {
            speech_title.setText(data.getString(COL_TITLE));
        }
        if(editedSpeech != null){
            speech_view.setText(editedSpeech);
            editedSpeech = null;
        } else {
            speech_view.setText(data.getString(COL_SPEECH));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        inflater.inflate(R.menu.edit_speech, menu);
        mDataCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        mMenu = menu;
        if (mUri != null) {
            mMenu.findItem(R.id.delete).setEnabled(true);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.save) {
            if(TextUtils.isEmpty(speech_title.getText())) {
                Toast.makeText(getActivity(), getString(R.string.speech_title_empty), Toast.LENGTH_LONG).show();

            } else {
                SpeechesValuesBuilder speechesValues = new SpeechesValuesBuilder();
                speechesValues.title(speech_title.getText().toString());
                speechesValues.speech(speech_view.getText().toString());
                if (mId == 0) {
                    Uri speechUri = getActivity().getContentResolver().
                            insert(SpeechProvider.Speeches.CONTENT_URI, speechesValues.values());
                    mId = ContentUris.parseId(speechUri);
                    Toast.makeText(getActivity(), getString(R.string.changes_saved), Toast.LENGTH_LONG).show();
                    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                            .setPackage(getActivity().getPackageName());
                    getActivity().sendBroadcast(dataUpdatedIntent);
                    if (mTwoPane) {
                        Bundle args = new Bundle();
                        args.putParcelable(SPEECH_URI, speechUri);
                        SpeechFragment fragment = new SpeechFragment();
                        fragment.setArguments(args);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.speech_container, fragment)
                                .commit();
                    } else {
                        Intent intent = new Intent(getActivity(), SpeechActivity.class).setData(speechUri);
                        getActivity().finish();
                        startActivity(intent);
                    }
                } else {
                    getActivity().getApplicationContext().getContentResolver().
                            update(SpeechProvider.Speeches.CONTENT_URI, speechesValues.values(), SpeechColumns._ID + "= ?", new String[]{"" + mId});
                    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                            .setPackage(getActivity().getPackageName());
                    getActivity().sendBroadcast(dataUpdatedIntent);
                    Toast.makeText(getActivity(), getString(R.string.changes_saved), Toast.LENGTH_LONG).show();
                    if (mTwoPane) {
                        Bundle args = new Bundle();
                        args.putParcelable(SPEECH_URI, mUri);
                        SpeechFragment fragment = new SpeechFragment();
                        fragment.setArguments(args);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.speech_container, fragment)
                                .commit();
                    } else {
                        getActivity().finish();
                    }
                }
                return true;
            }
        } else if (id == R.id.delete) {
            getActivity().getContentResolver().delete(SpeechProvider.Speeches.CONTENT_URI, SpeechColumns._ID + "= ?", new String[]{"" + mId});
            Toast.makeText(getActivity(), getString(R.string.speech_deleted), Toast.LENGTH_LONG).show();
            if (mTwoPane) {
                SpeechFragment fragment = new SpeechFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.speech_container, fragment)
                        .commit();
            } else {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                getActivity().finish();
                startActivity(intent);
            }

        } else if (id == R.id.cancel) {
            if(mTwoPane) {
                SpeechFragment fragment = new SpeechFragment();
                if(mUri !=null) {
                    Bundle args = new Bundle();
                    args.putParcelable(SPEECH_URI, mUri);
                    fragment.setArguments(args);
                }
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.speech_container, fragment)
                        .commit();
            } else {
                getActivity().finish();
            }
        } else if (id == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        } else if (id == R.id.thesaurus) {

            int startIndex = 0;
            int endIndex = 0;
            String selectedText = "";
            startIndex = speech_view.getSelectionStart();
            endIndex = speech_view.getSelectionEnd();
            if (startIndex != endIndex && startIndex >= 0 && endIndex >= 0) {
                selectedText = speech_view.getText().toString().substring(startIndex, endIndex);
            }
            ThesaurusDialogFragment fragment = ThesaurusDialogFragment.newInstance(selectedText);
            fragment.setTargetFragment(this, 300);

            fragment.show(getFragmentManager(), "thesaurus_dialog");

            return true;
        } else if (id == R.id.settings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        } else if (id == R.id.import_file) {
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addApi(Drive.API)
                        .addScope(Drive.SCOPE_FILE)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            }
            mGoogleApiClient.connect();
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResult(String synonym) {
        String text = speech_view.getText().toString();
        int startIndex = speech_view.getSelectionStart();
        int endIndex = speech_view.getSelectionEnd();
        text = text.substring(0, startIndex) + synonym + text.substring(endIndex);
        speech_view.setText(text);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mSelectedFileDriveId != null) {
            open();
            return;
        }
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[]{"text/plain"})
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0, null);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
            mTracker.send(new HitBuilders.ExceptionBuilder()
                    .setDescription(e.getCause() + ":" + e.getStackTrace())
                    .setFatal(false)
                    .build());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                if(!mUserCancelImport){
                    mUserCancelImport = true;
                    connectionResult.startResolutionForResult(getActivity(), RESOLVE_CONNECTION_REQUEST_CODE);
                } else {
                    mUserCancelImport = false;
                }
            } catch (IntentSender.SendIntentException e) {
                mTracker.send(new HitBuilders.ExceptionBuilder()
                        .setDescription(e.getCause() + ":" + e.getStackTrace())
                        .setFatal(false)
                        .build());
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), getActivity(), 0).show();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mUserCancelImport = false;
                    mGoogleApiClient.connect();
                }
                break;
            case REQUEST_CODE_OPENER:
                if (resultCode == RESULT_OK) {
                    mSelectedFileDriveId = (DriveId) data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                } else {
                    mGoogleApiClient = null;
                    return;
                }
                break;
        }
    }

    private void open() {
        // Reset progress dialog back to zero as we're
        // initiating an opening request.
        mOverlay.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        DriveFile file = Drive.DriveApi.getFile(mGoogleApiClient, mSelectedFileDriveId);
        file.getMetadata(mGoogleApiClient).setResultCallback(metadataRetrievedCallback);
        file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(contentsCallback);
        mSelectedFileDriveId = null;
    }

    ResultCallback<MetadataResult> metadataRetrievedCallback = new
            ResultCallback<MetadataResult>() {
                @Override
                public void onResult(MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {

                        return;
                    }
                    Metadata metadata = result.getMetadata();
                    speech_title.setText(metadata.getTitle());
                }
            };

    private ResultCallback<DriveApi.DriveContentsResult> contentsCallback = new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult result) {
                if (!result.getStatus().isSuccess()) {

                    return;
                }
                DriveContents contents = result.getDriveContents();
                InputStream in = contents.getInputStream();
                BufferedReader reader;
                reader = new BufferedReader(new InputStreamReader(in));
                StringBuffer buffer = new StringBuffer();

                String line;
                try {
                    while ((line = reader.readLine()) !=null){
                        buffer.append(line+"\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    contents.discard(mGoogleApiClient);
                }
                speech_view.setText(buffer.toString());
                mProgressBar.setVisibility(View.GONE);
                mOverlay.setVisibility(View.GONE);
                mGoogleApiClient.disconnect();
            }
        };
}
