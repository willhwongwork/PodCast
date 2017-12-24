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

import bbr.podcast.R;
import bbr.podcast.utils.Constants;

/**
 * Created by Me on 6/3/2017.
 */

public class DownloadDiscoverThumbnailsService extends IntentService {
    private final static String LOG_TAG = DownloadDiscoverThumbnailsService.class.getSimpleName();

    public DownloadDiscoverThumbnailsService() {
        super("DownloadDiscoverThumbnailsService");
    }

    @Override
    public void onHandleIntent(@Nullable Intent intent) {
        String[] genreNames = getResources().getStringArray(R.array.genre_name);
        int[] genreIds = getResources().getIntArray(R.array.genre_id);

        //List<String[]> thumbArtUrlsForAllGenres = new ArrayList<>();

        for(int i = 0; i < genreIds.length; i++) {
            Log.d(LOG_TAG, "discover reading urls " + genreNames[i]);
            String[] thumbArtUrls = readThumbArtUrlsFromNetwork(genreIds[i]);

            String status = "progress";
            Intent localIntent =
                    new Intent(Constants.DISCOVER_BROADCAST_ACTION)
                            // Puts the status into the Intent
                            .putExtra(Constants.DISCOVER_EXTENDED_DATA_STATUS, status)
                            .putExtra(getString(R.string.intent_pass_dicover_genre_name), genreNames[i])
                            .putExtra(getString(R.string.intent_pass_discover_genre_id), genreIds[i])
                            .putExtra(getString(R.string.intent_pass_discover_thumbs_array), thumbArtUrls);
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
            //thumbArtUrlsForAllGenres.add(thumbArtUrls);
        }

        String status = "done";
        Intent localIntent = new Intent(Constants.DISCOVER_BROADCAST_ACTION)
                // Puts the status into the Intent
                .putExtra(Constants.DISCOVER_EXTENDED_DATA_STATUS, status);

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private String[] readThumbArtUrlsFromNetwork(int genreId) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String podcastJsonStr = null;
            try {
                String API_Url = "https://itunes.apple.com/search?term=podcast&limit=4&genreId=%s";
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
                return getItunesThumbsDataFromJson(podcastJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the data.
            return null;

        }

        private String[] getItunesThumbsDataFromJson(String JsonStr) throws JSONException {
            final String MDB_RESULTS = "results";
            final String MDB_ARTWORK_URL_600 = "artworkUrl600";
            JSONObject podcastsJson = new JSONObject(JsonStr);
            JSONArray results = podcastsJson.getJSONArray(MDB_RESULTS);

            final String[] resultThumbUrls = new String[results.length()];

            for (int i = 0; i < results.length(); i++) {
                String artworkUrl600;

                JSONObject aPodcast = results.getJSONObject(i);
                artworkUrl600 = aPodcast.getString(MDB_ARTWORK_URL_600);
                resultThumbUrls[i] = artworkUrl600;
            }
            return resultThumbUrls;
        }
}
