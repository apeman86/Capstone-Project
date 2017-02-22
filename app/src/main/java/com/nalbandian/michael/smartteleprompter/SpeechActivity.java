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
import android.widget.Toast;

import com.nalbandian.michael.smartteleprompter.data.SpeechColumns;
import com.nalbandian.michael.smartteleprompter.data.SpeechProvider;
import com.nalbandian.michael.smartteleprompter.data.generated.values.SpeechesValuesBuilder;


public class SpeechActivity extends AppCompatActivity {

    private long mId = 0;
    private Menu mMenu = null;
    private Uri mUri = null;



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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.speech, menu);
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.delete){
            getApplicationContext().getContentResolver().delete(SpeechProvider.Speeches.CONTENT_URI, SpeechColumns._ID + "= ?", new String[]{""+mId});
            this.finish();
        } else if (id == R.id.edit) {
            Intent intent = new Intent(getApplicationContext(), EditSpeechActivity.class).setData(mUri);
            startActivity(intent);
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.start_teleprompter) {
            Intent intent = new Intent(getApplicationContext(), PlaySpeechActivity.class).setData(mUri);
            startActivity(intent);
        } else if (id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
