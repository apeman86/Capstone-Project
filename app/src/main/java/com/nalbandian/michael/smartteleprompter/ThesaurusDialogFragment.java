package com.nalbandian.michael.smartteleprompter;

import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by nalbandianm on 2/24/2017.
 */

public class ThesaurusDialogFragment extends AppCompatDialogFragment {

    private static final String SEARCHTEXT = "search_text";
    private static Tracker mTracker;
    private ListView mListView;
    private static final String API_KEY = "key";
    private static final String WORD = "word";
    private static final String OUTPUT = "output";
    private static final String LANG = "language";
    private String KEY = BuildConfig.THESAURUS_API_KEY;

    public interface ThesaurusListener {
        public void onResult(String synonym);
    }

    public static ThesaurusDialogFragment newInstance(String searchText){
        ThesaurusDialogFragment fragment = new ThesaurusDialogFragment();
        Bundle args = new Bundle();
        args.putString(SEARCHTEXT, searchText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.thesaurus_dialog, container);
        getDialog().setTitle(getString(R.string.thesaurus));

        SmartTeleprompterApplication application = (SmartTeleprompterApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        mListView = (ListView) rootView.findViewById(R.id.search_results);
        SearchView searchView = (SearchView) rootView.findViewById(R.id.search_view);
        searchView.setActivated(true);
        searchView.onActionViewExpanded();
        searchView.setIconified(false);
        searchView.setQueryHint("Search for Synonym");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ThesaurusAsyncTask asyncTask = new ThesaurusAsyncTask();
                asyncTask.execute(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ThesaurusListener listener = (ThesaurusListener) getTargetFragment();
                listener.onResult(((TextView)view).getText().toString());
                dismiss();
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        mTracker.setScreenName(ThesaurusDialogFragment.class.getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        super.onResume();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String searchText = getArguments().getString(SEARCHTEXT);
        SearchView searchView = (SearchView) view.findViewById(R.id.search_view);
        if(searchText.equals("")) {
            searchView.requestFocus();
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            searchView.setQuery(searchText, true);
        }
    }

    public class ThesaurusAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(String... strings) {
            HttpURLConnection urlConnection = null;
            String thesaurusJSON = null;
            BufferedReader reader = null;
            ArrayList<String> synonyms = null;
            Uri.Builder uriBuilder = Uri.parse(getString(R.string.thesaurus_url)).buildUpon();
            uriBuilder.appendQueryParameter(API_KEY, KEY);
            uriBuilder.appendQueryParameter(WORD, strings[0]);
            uriBuilder.appendQueryParameter(LANG, "en_US");
            uriBuilder.appendQueryParameter(OUTPUT, "json");

            try {
                URL url = new URL(uriBuilder.build().toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) !=null){
                    buffer.append(line+"\n");
                }
                thesaurusJSON = buffer.toString();
                synonyms = parseJSON(thesaurusJSON);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if(urlConnection != null) {
                    urlConnection.disconnect();
                }
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return synonyms;
        }

        private ArrayList<String> parseJSON(String JSONStr) throws JSONException, ParseException {

            ArrayList<String> synonyms = new ArrayList<>();
            final String RESPONSE = "response";
            final String LIST = "list";
            final String SYNONYMS = "synonyms";
            final String DIVIDER = "\\|";

            JSONObject thesaurusJSON = new JSONObject(JSONStr);
            JSONArray lists = thesaurusJSON.getJSONArray(RESPONSE);

            for (int index = 0; index < lists.length(); index++) {
                JSONObject listObject = lists.getJSONObject(index);
                JSONObject list = listObject.getJSONObject(LIST);

                String synonymsStr = list.getString(SYNONYMS);
                String[] strings = synonymsStr.split(DIVIDER);
                for (String synonym: strings ) {
                    if(synonym.contains("(")) {
                        synonyms.add(synonym.substring(0, synonym.indexOf('(')-1));
                    } else {
                        synonyms.add(synonym);
                    }
                }
            }

            return synonyms;


        }

        @Override
        protected void onPostExecute(ArrayList<String> list) {
            ArrayAdapter adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, list);
            mListView.setAdapter(adapter);
            super.onPostExecute(list);
        }
    }
}
