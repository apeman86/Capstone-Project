package com.nalbandian.michael.smartteleprompter;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;
import com.nalbandian.michael.smartteleprompter.data.SpeechProvider;

/**
 * Created by nalbandianm on 2/16/2017.
 */

public class PlaySpeechActivity extends AppCompatActivity {

    private Uri mUri = null;
    private static PlaySpeechFragment mFragment = null;
    private Menu mMenu = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_speech_activity);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        if (savedInstanceState == null) {

            Bundle arguments = new Bundle();
            if(mUri == null) {
                mUri = getIntent().getData();
            }
            arguments.putParcelable(PlaySpeechFragment.SPEECH_URI, mUri);
            mFragment = new PlaySpeechFragment();
            mFragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.play_speech_container, mFragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.play_speech, menu);
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home || id == R.id.stop) {
            mFragment.startStop(false);
            onBackPressed();
            return true;
        }
        else if (id == R.id.resume){
            mFragment.pauseResume(false);
            mMenu.findItem(R.id.resume).setVisible(false);
            mMenu.findItem(R.id.pause).setVisible(true);
        } else if (id == R.id.pause){
            mFragment.pauseResume(true);
            mMenu.findItem(R.id.pause).setVisible(false);
            mMenu.findItem(R.id.resume).setVisible(true);
        }

        return super.onOptionsItemSelected(item);
    }

}
