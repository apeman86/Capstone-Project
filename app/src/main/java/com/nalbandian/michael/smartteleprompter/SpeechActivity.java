package com.nalbandian.michael.smartteleprompter;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.libraries.cast.companionlibrary.cast.DataCastManager;
import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;
import com.nalbandian.michael.smartteleprompter.data.SpeechProvider;
import com.nalbandian.michael.smartteleprompter.data.generated.values.SpeechesValuesBuilder;


public class SpeechActivity extends AppCompatActivity {

    public static final String SPEECH_URI = "URI";
    private long mId = 0;
    private Uri mUri = null;
    private boolean mTwoPane;
    private DataCastManager mDataCastManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speech_activity);
        if (savedInstanceState == null) {

            Bundle arguments = new Bundle();
            if(mUri == null) {
                mUri = getIntent().getData();
                mId = ContentUris.parseId(mUri);
            }
            arguments.putParcelable(SpeechFragment.SPEECH_URI, mUri);
            SpeechFragment fragment = new SpeechFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.speech_container, fragment)
                    .commit();
        }
    }


    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        super.addContentView(view, params);
    }

}
