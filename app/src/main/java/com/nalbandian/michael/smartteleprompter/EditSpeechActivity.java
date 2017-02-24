package com.nalbandian.michael.smartteleprompter;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;
import com.nalbandian.michael.smartteleprompter.data.SpeechProvider;
import com.nalbandian.michael.smartteleprompter.data.generated.values.SpeechesValuesBuilder;


public class EditSpeechActivity extends AppCompatActivity {

    private long mId = 0;
    private Uri mUri = null;
    EditSpeechFragment mFragment;
    public static final String ACTION_DATA_UPDATED = "com.nalbandian.michael.smartteleprompter.ACTION_DATA_UPDATED";
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_speech_activity);
        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            mUri = getIntent().getData();
            if(mUri != null){
                mFragment = (EditSpeechFragment) getSupportFragmentManager().findFragmentById(R.id.edit_speech_container);
                mFragment.setSpeechUri(mUri);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.edit_speech_container, mFragment)
                        .commit();
            }


        }
        mAdView = (AdView) findViewById(R.id.speech_adView);
        if(mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }
}
