package bbr.podcast;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bbr.podcast.feeds.ItunesPodcast;
import bbr.podcast.utils.SpacesItemDecoration;

/**
 * Created by Me on 4/16/2017.
 */

public class SearchResultsActivity extends BaseActivity {
    private final static String LOG_TAG = SearchResultsActivity.class.getSimpleName();

    private List<ItunesPodcast> itunesPodcasts = new ArrayList<ItunesPodcast>();
    private SearchResultsAdapter searchResultsAdapter = new SearchResultsAdapter(itunesPodcasts, this);
    ProgressDialog pd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        RecyclerView rvSearch = (RecyclerView) findViewById(R.id.search_recycler_view);
        rvSearch.setLayoutManager(new GridLayoutManager(this,2));
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
        rvSearch.addItemDecoration(new SpacesItemDecoration(2, spacingInPixels, true));
        rvSearch.setAdapter(searchResultsAdapter);

        handleIntent(getIntent());
        Log.d(LOG_TAG, "handleIntent");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
            if(itunesPodcasts.size() == 0) {
                final FetchItunesPodcastTask podcastTask = new FetchItunesPodcastTask(itunesPodcasts, searchResultsAdapter);

                pd = new ProgressDialog(this);
                pd.setMessage("Please Wait");
                pd.setIndeterminate(false);
                pd.setCancelable(false);
                pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        podcastTask.cancel(true);
                    }
                });
                podcastTask.execute(query);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }

        return super.onOptionsItemSelected(item);
    }

    public class FetchItunesPodcastTask extends AsyncTask<String, Void, List<ItunesPodcast>> {

        private final String LOG_TAG = FetchItunesPodcastTask.class.getCanonicalName();
        private List<ItunesPodcast> itunesPodcasts;
        private SearchResultsAdapter searchResultsAdapter;

        public FetchItunesPodcastTask(List<ItunesPodcast> p, SearchResultsAdapter adapter) {
            itunesPodcasts = p;
            searchResultsAdapter = adapter;
        }

        private List<ItunesPodcast> getItunesPodcastDataFromJson(String podcastJsonStr) throws JSONException {

            final String MDB_RESULTS = "results";
            final String MDB_FEED_URL = "feedUrl";
            final String MDB_COLLECTION_NAME = "collectionName";
            final String MDB_ARTWORK_URL_100 = "artworkUrl100";
            final String MDB_ARTWORK_URL_600 = "artworkUrl600";
            final String MDB_PRIMARY_GENRE_NAME = "primaryGenreName";

            JSONObject podcastsJson = new JSONObject(podcastJsonStr);
            JSONArray results = podcastsJson.getJSONArray(MDB_RESULTS);

            final ItunesPodcast[] resultPodcasts = new ItunesPodcast[results.length()];

            for (int i = 0; i < results.length(); i++) {
                String feedUrl;
                String collectionName;
                String artworkUrl100;
                String artworkUrl600;
                String primaryGenreName;

                JSONObject aPodcast = results.getJSONObject(i);
                feedUrl = aPodcast.getString(MDB_FEED_URL);
                collectionName = aPodcast.getString(MDB_COLLECTION_NAME);
                artworkUrl100 = aPodcast.getString(MDB_ARTWORK_URL_100);
                artworkUrl600 = aPodcast.getString(MDB_ARTWORK_URL_600);
                primaryGenreName = aPodcast.getString(MDB_PRIMARY_GENRE_NAME);

                ItunesPodcast podcast = new ItunesPodcast(feedUrl, collectionName, artworkUrl100, artworkUrl600, primaryGenreName);
                resultPodcasts[i] = podcast;
            }
            List resultPodcastsList = Arrays.asList(resultPodcasts);
            return resultPodcastsList;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.show();

        }

        @Override
        protected List<ItunesPodcast> doInBackground(String... params) {
            String query = params[0];
            Log.d(LOG_TAG, query);
            if (query.contains(" ")) {
                query = query.replace(" ", "+");
                Log.d(LOG_TAG, "query has empty space");
                Log.d(LOG_TAG, query);
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String podcastJsonStr = null;

            try{
                final String API_URL = "https://itunes.apple.com/search?media=podcast&term=%s";
                String formattedUrl = String.format(API_URL, query).replace(' ', '+');
                URL url = new URL(formattedUrl);
                Log.d(LOG_TAG, url.toString());
                // Create the request, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        Log.d(LOG_TAG, "is cancelled");
                        reader.close();
                        return null;
                    }
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");

                }

                if(buffer.length() == 0) {
                    // Stream was empty. No point in parsing.
                    return null;
                }
                podcastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getItunesPodcastDataFromJson(podcastJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the data.
            return null;
        }

        @Override
        protected void onPostExecute(List<ItunesPodcast> result) {
            pd.dismiss();
            if (result != null) {
                int curSize = searchResultsAdapter.getItemCount();
                itunesPodcasts.addAll(result);
                searchResultsAdapter.notifyItemRangeInserted(curSize, result.size());
            } else {
                Toast.makeText(SearchResultsActivity.this, "Error downloading data", Toast.LENGTH_SHORT);
            }
        }
    }
}