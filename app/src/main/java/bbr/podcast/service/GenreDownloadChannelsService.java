package bbr.podcast.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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

import bbr.podcast.R;
import bbr.podcast.feeds.ItunesPodcast;
import bbr.podcast.utils.Constants;

/**
 * Created by Me on 6/4/2017.
 */

public class GenreDownloadChannelsService extends IntentService {
    private final static String LOG_TAG = GenreDownloadChannelsService.class.getSimpleName();

    public GenreDownloadChannelsService(){
        super("GenreDownloadChannelsService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        int genreId = intent.getIntExtra(getString(R.string.intent_pass_discover_genre_id), -1);

        ArrayList<ItunesPodcast> itunesPodcasts;

        if(genreId != -1) {
            itunesPodcasts = readItunesPodcastsFromNetwork(genreId);
            if(itunesPodcasts != null) {
                Log.d(LOG_TAG, "genre itunesPodcast size" + itunesPodcasts.size());
                String status = "done";
                Intent localIntent = new Intent(Constants.GENRE_BROADCAST_ACTION)
                        // Puts the status into the Intent
                        .putParcelableArrayListExtra(getString(R.string.intent_pass_itunes_podcast), (ArrayList<ItunesPodcast>) itunesPodcasts)
                        .putExtra(Constants.GENRE_EXTENDED_DATA_STATUS, status);

                // Broadcasts the Intent to receivers in this app.
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
            }
        }
    }

    private ArrayList<ItunesPodcast> readItunesPodcastsFromNetwork(int genreId) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String podcastJsonStr = null;
        try {
            String API_Url = "https://itunes.apple.com/search?term=podcast&limit=50&genreId=%s";
            String formattedUrl = String.format(API_Url, genreId);
            URL url = new URL(formattedUrl);
            Log.d(LOG_TAG, url.toString());
            // Create the request, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the inputStream into a string
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
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
            return getItunesPodcastsDataFromJson(podcastJsonStr);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        // This will only happen if there was an error getting or parsing the data.
        return null;

    }

    private ArrayList<ItunesPodcast> getItunesPodcastsDataFromJson(String JsonStr) throws JSONException {
        final String MDB_RESULTS = "results";
        final String MDB_FEED_URL = "feedUrl";
        final String MDB_COLLECTION_NAME = "collectionName";
        final String MDB_ARTWORK_URL_100 = "artworkUrl100";
        final String MDB_ARTWORK_URL_600 = "artworkUrl600";
        final String MDB_PRIMARY_GENRE_NAME = "primaryGenreName";

        JSONObject podcastsJson = new JSONObject(JsonStr);
        JSONArray results = podcastsJson.getJSONArray(MDB_RESULTS);

        final ArrayList<ItunesPodcast> resultPodcasts = new ArrayList<>();

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
            resultPodcasts.add(podcast);
        }
        return resultPodcasts;
    }
}
