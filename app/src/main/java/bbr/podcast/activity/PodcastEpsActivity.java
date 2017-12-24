package bbr.podcast.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import bbr.podcast.adapter.PodcastEpsAdapter;
import bbr.podcast.R;
import bbr.podcast.service.RefreshEpisodesService;
import bbr.podcast.data.PodcastContract;
import bbr.podcast.feeds.Episode;
import bbr.podcast.feeds.PodcastChannel;
import bbr.podcast.feeds.PodcastFeedParser;
import bbr.podcast.utils.Constants;

/**
 * Created by Me on 4/17/2017.
 */

public class PodcastEpsActivity extends BaseActivity {
    private static final String LOG_TAG = PodcastEpsActivity.class.getSimpleName();
    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    // Whether the display should be refreshed.
    public static boolean refreshDisplay = true;
    public static String sPref = null;

    private PodcastChannel channel;

    private String thumbnail;
    private String feedUrl;

    private String subscription;

    private String backgroundDlState;

    private PodcastEpsAdapter podcastEpsAdapter;
    ProgressDialog pd;
    String Error;

    private SwipeRefreshLayout swipeRefreshLayout;

    private DownloadStateReceiver mDownloadStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podcast);

        Toolbar podcastToolbar = (Toolbar) findViewById(R.id.podcast_toobar);
        setSupportActionBar(podcastToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        subscription = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        feedUrl = getIntent().getStringExtra(getResources().getString(R.string.intent_pass_feedUrl));
        thumbnail = getIntent().getStringExtra(getResources().getString(R.string.intent_pass_thumbnail));
        if (subscription != null) {
            readDataFromProvider();
        } else {
            loadPage();
        }

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                myUpdateOperation();
            }
        });

        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);

        // Instantiates a new DownloadStateReceiver
        mDownloadStateReceiver =
                new DownloadStateReceiver();
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDownloadStateReceiver,
                statusIntentFilter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDownloadStateReceiver);
    }

    /*
 * Listen for option item selections so that we receive a notification
 * when the user requests a refresh by selecting the refresh action bar item.
 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Check if user triggered a refresh:
            case R.id.menu_refresh:
                Log.i(LOG_TAG, "Refresh menu item selected");

                // Signal SwipeRefreshLayout to start the progress indicator
                swipeRefreshLayout.setRefreshing(true);

                // Start the refresh background task.
                // This method calls setRefreshing(false) when it's finished.
                myUpdateOperation();

                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
        }

        // User didn't trigger a refresh, let the superclass handle this action
        return super.onOptionsItemSelected(item);

    }

    private void myUpdateOperation() {
        Intent intent = new Intent(PodcastEpsActivity.this, RefreshEpisodesService.class);
        //intent.putParcelableArrayListExtra(context.getResources().getString(R.string.intent_pass_episodes_list), (ArrayList<Episode>)channel.episodes);
        intent.putExtra(getResources().getString(R.string.intent_pass_feedUrl), feedUrl).putExtra(getResources().getString(R.string.intent_pass_subscription), subscription);
        startService(intent);
    }



    public void readDataFromProvider() {
        String title = null;
        String description = null;
        String language = null;
        String copyright = null;
        String managingEditor = null;
        String itunesImage = null;
        String imageUrl = null;
        List<Episode> episodes = new ArrayList<>();
        Cursor channelCursor = getContentResolver().query(PodcastContract.ChannelEntry.CONTENT_URI,
                null,
                PodcastContract.ChannelEntry.COLUMN_FEED_URL + "=?",
                new String[]{feedUrl},
                null);

        channelCursor.moveToFirst();
        int titleIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_TITLE);
        title = channelCursor.getString(titleIndex);
        int descriptionIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_DESCRIPTION);
        description = channelCursor.getString(descriptionIndex);
        int languageIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_LANGUAGE);
        language = channelCursor.getString(languageIndex);
        int copyrightIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_COPYRIGHT);
        copyright = channelCursor.getString(copyrightIndex);
        int managingEditorIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_EDITOR);
        managingEditor = channelCursor.getString(managingEditorIndex);
        int itunesImageIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_ITUNES_IMAGE);
        itunesImage = channelCursor.getString(itunesImageIndex);
        int imageUrlIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_IMAGE_URL);
        imageUrl = channelCursor.getString(imageUrlIndex);

        int channelIdIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry._ID);
        int channelId = channelCursor.getInt(channelIdIndex);

        Cursor episodesCursor = getContentResolver().query(PodcastContract.EpisodeEntry.CONTENT_URI,
                null,
                PodcastContract.EpisodeEntry.COLUMN_CHANNEL_ID + "=?",
                new String[]{Integer.toString(channelId)},
                PodcastContract.EpisodeEntry._ID + " DESC");

        episodesCursor.moveToFirst();
        for (int i = 0; i < episodesCursor.getCount(); i++)  {
            int eTitleIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_TITLE);
            String eTitle = episodesCursor.getString(eTitleIndex);
            int linkIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_LINK);
            String link = episodesCursor.getString(linkIndex);
            int pubDateIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_PUB_DATE);
            String pubDate = episodesCursor.getString(pubDateIndex);
            int eDescriptionIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION);
            String eDescription = episodesCursor.getString(eDescriptionIndex);
            int enclosureIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_ENCLOSURE);
            String enclosure = episodesCursor.getString(enclosureIndex);
            int durationIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_DURATION);
            String duration = episodesCursor.getString(durationIndex);
            int explicitIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_EXPLICIT);
            String explicit = episodesCursor.getString(explicitIndex);
            int episodeImageIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_EPISODE_IMAGE);
            String episodeImage = episodesCursor.getString(episodeImageIndex);
            episodes.add(new Episode(eTitle, link, pubDate, eDescription, enclosure, duration, explicit, episodeImage));
            episodesCursor.moveToNext();
        }

        channel = new  PodcastChannel(title, description, language, copyright, managingEditor, itunesImage, imageUrl, episodes);

        podcastEpsAdapter = new PodcastEpsAdapter(channel, thumbnail, feedUrl, PodcastEpsActivity.this, true);

        RecyclerView episodeRv = (RecyclerView) findViewById(R.id.episode_recycler_view);
        episodeRv.setLayoutManager(new LinearLayoutManager(episodeRv.getContext()));
        episodeRv.setAdapter(podcastEpsAdapter);

        ImageView podcastImage = (ImageView) findViewById(R.id.podcast_image);
        loadPodcastImage(podcastImage, channel);

        channelCursor.close();
        episodesCursor.close();
    }



    // Uses AsyncTask to download the XML feed.
    public void loadPage() {
        final DownloadXmlTask downloadXmlTask = new DownloadXmlTask(this);
        pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.please_wait));
        pd.setIndeterminate(false);
        pd.setCancelable(true);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadXmlTask.cancel(true);
            }
        });

        downloadXmlTask.execute(feedUrl);

/*        if((sPref.equals(ANY)) && (wifiConnected || mobileConnected)) {
            new DownloadXmlTask().execute(URL);
        }
        else if ((sPref.equals(WIFI)) && (wifiConnected)) {
            new DownloadXmlTask().execute(URL);
        } else {
            // show error
        }*/
    }

    private void loadPodcastImage (ImageView podcastImage, PodcastChannel channel) {

        String downloadImage;
        if(channel.itunesImage != null) {
            downloadImage = channel.itunesImage;
        } else {
            downloadImage = channel.imageUrl;
        }

        Picasso.with(this).setLoggingEnabled(true);
        Picasso.with(this)
                .load(downloadImage)
                .error(R.drawable.placeholder)
                .fit()
                .into(podcastImage);
    }

    // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
    public class DownloadXmlTask extends AsyncTask<String, Integer, PodcastChannel> {

        private final String LOG_TAG = DownloadXmlTask.class.getSimpleName();

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadXmlTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            pd.show();

        }

        @Override
        protected PodcastChannel doInBackground(String... urls) {
            try {
                return loadXmlFromNetwork(urls[0]);
            } catch (IOException e) {
                Error = getResources().getString(R.string.connection_error);
                Log.e(LOG_TAG, getResources().getString(R.string.connection_error), e);
            } catch (XmlPullParserException e) {
                Error = getResources().getString(R.string.xml_error);
                Log.e(LOG_TAG, getResources().getString(R.string.xml_error), e);
            }
            return null;
        }

        @Override
        protected void onCancelled(PodcastChannel result) {
            Log.d(LOG_TAG, "cancelled");
            if (result != null) {
                Log.d(LOG_TAG, "cancelled");
            }

        }

        @Override
        protected void onPostExecute(PodcastChannel result) {
            channel = result;
            podcastEpsAdapter = new PodcastEpsAdapter(channel, thumbnail, feedUrl, PodcastEpsActivity.this, false);
            if(result != null) {
                Log.d(LOG_TAG, "result is not null");
                Log.d(LOG_TAG, Integer.toString(result.getEpisodes().size()));
                for (Episode ep : result.getEpisodes()) {
                    if(ep.enclosure != null) {
                        Log.d(LOG_TAG, ep.enclosure);
                    }
                }

                RecyclerView episodeRv = (RecyclerView) findViewById(R.id.episode_recycler_view);
                episodeRv.setLayoutManager(new LinearLayoutManager(episodeRv.getContext()));
                episodeRv.setAdapter(podcastEpsAdapter);

                pd.dismiss();

                //podcastEpsAdapter.notifyDataSetChanged();

                ImageView podcastImage = (ImageView) findViewById(R.id.podcast_image);
                loadPodcastImage(podcastImage, channel);

            } else {
                Log.d(LOG_TAG, "result is NULL");
                if(Error.equals(getResources().getString(R.string.connection_error))) {
                    Toast.makeText(PodcastEpsActivity.this, getResources().getString(R.string.connection_error), Toast.LENGTH_SHORT);
                }
            }
        }

        private PodcastChannel loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
            InputStream stream = null;
            // Instantiate the parser
            PodcastFeedParser podcastFeedParser = new PodcastFeedParser();
            PodcastChannel podcastChannel;

            try {
                stream = downloadUrl(urlString);
                if(stream != null){
                    Log.d(LOG_TAG, "stream is not null");
                    podcastChannel = podcastFeedParser.parse(stream, this);
                    return podcastChannel;
                }
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
            return null;
        }

        // Given a string representation of a URL, sets up a connection and gets
// an input stream.
        private InputStream downloadUrl(String urlString) throws IOException {
            URL url = new URL(urlString);
            Log.d(LOG_TAG, urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //conn.setReadTimeout(10000 /* milliseconds */);
            //conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            if (isCancelled() == true) {
                return null;
            }
            conn.connect();
            return conn.getInputStream();
        }
    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class DownloadStateReceiver extends BroadcastReceiver
    {
        // Prevents instantiation
        private DownloadStateReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
        /*
         * Handle Intents here.
         */

            backgroundDlState = intent.getStringExtra(Constants.EXTENDED_DATA_STATUS);
            if (backgroundDlState.equals("done")) {
                if(subscription != null) {
                    readDataFromProvider();
                }
            }
            swipeRefreshLayout.setRefreshing(false);

        }
    }
}
