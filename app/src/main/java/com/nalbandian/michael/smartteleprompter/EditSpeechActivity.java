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


public class EditSpeechActivity extends AppCompatActivity {

    private long mId = 0;
    private Menu mMenu = null;
    private Uri mUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_speech_activity);
        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            mUri = getIntent().getData();
            EditSpeechFragment fragment = new EditSpeechFragment();
            if(mUri != null) {
                arguments.putParcelable(EditSpeechFragment.SPEECH_URI, mUri);
                fragment.setArguments(arguments);
                mId = ContentUris.parseId(mUri);
            }
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.edit_speech_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_speech, menu);
        mMenu = menu;
        if(mUri != null){
            mMenu.findItem(R.id.delete).setEnabled(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.save) {
            SpeechesValuesBuilder speechesValues = new SpeechesValuesBuilder();
            View view = getSupportFragmentManager().getFragments().get(0).getView();
            if(view != null) {
                speechesValues.title(((EditText)view.findViewById(R.id.title)).getText().toString());
                speechesValues.speech(((EditText)view.findViewById(R.id.speech)).getText().toString());
                if(mId == 0){
                    Uri speechUri = getApplicationContext().getContentResolver().
                            insert(SpeechProvider.Speeches.CONTENT_URI, speechesValues.values());
                    mId = ContentUris.parseId(speechUri);
                    Toast.makeText(this, getString(R.string.changes_saved), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getApplicationContext(), SpeechActivity.class).setData(speechUri);
                    startActivity(intent);
                } else {
                    getApplicationContext().getContentResolver().
                            update(SpeechProvider.Speeches.CONTENT_URI, speechesValues.values(), SpeechColumns._ID + "= ?", new String[]{""+mId});

                    Toast.makeText(this, getString(R.string.changes_saved), Toast.LENGTH_LONG).show();
                    this.finish();
                }
            }
            return true;
        } else if (id == R.id.delete){
            getApplicationContext().getContentResolver().delete(SpeechProvider.Speeches.CONTENT_URI, SpeechColumns._ID + "= ?", new String[]{""+mId});
            this.finish();
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
