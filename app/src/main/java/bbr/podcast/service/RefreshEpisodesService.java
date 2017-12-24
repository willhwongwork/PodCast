package bbr.podcast.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import bbr.podcast.R;
import bbr.podcast.data.PodcastContract;
import bbr.podcast.feeds.Episode;
import bbr.podcast.feeds.PodcastChannel;
import bbr.podcast.feeds.PodcastFeedParser;
import bbr.podcast.utils.Constants;

/**
 * Created by Me on 5/23/2017.
 */

public class RefreshEpisodesService extends IntentService {
    private static final String LOG_TAG = RefreshEpisodesService.class.getSimpleName();

    String Error;

    public RefreshEpisodesService() {
        super("RefreshEpisodesService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //List<Episode> oldEpisodes = intent.getParcelableArrayListExtra(getResources().getString(R.string.intent_pass_episodes_list));
        String subscription = intent.getStringExtra(getResources().getString(R.string.intent_pass_subscription));
        if (subscription != null) {
            String feedUrl = intent.getStringExtra(getResources().getString(R.string.intent_pass_feedUrl));
            PodcastChannel channel = loadPoadcastChannelFromNetwork(feedUrl);
            insertData(this, channel);
        }

        /*
     * Creates a new Intent containing a Uri object
     * BROADCAST_ACTION is a custom Intent action
     */
        String status = "done";
        Intent localIntent =
                new Intent(Constants.BROADCAST_ACTION)
                        // Puts the status into the Intent
                        .putExtra(Constants.EXTENDED_DATA_STATUS, status);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }


    private PodcastChannel loadPoadcastChannelFromNetwork(String url) {
        try {
            return loadXmlFromNetwork(url);
        } catch (IOException e) {
            Error = getResources().getString(R.string.connection_error);
            Log.e(LOG_TAG, getResources().getString(R.string.connection_error), e);
        } catch (XmlPullParserException e) {
            Error = getResources().getString(R.string.xml_error);
            Log.e(LOG_TAG, getResources().getString(R.string.xml_error), e);
        }
        return null;
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
                podcastChannel = podcastFeedParser.parse(stream, null);
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
        conn.connect();
        return conn.getInputStream();
    }

    private void insertData(Context context, PodcastChannel channel) {
        if(context == null) return;
        long channelId;

        // First, check if the channel with the title exists
        Cursor channelCursor = context.getContentResolver().query(
                PodcastContract.ChannelEntry.CONTENT_URI,
                new String[]{PodcastContract.ChannelEntry._ID},
                PodcastContract.ChannelEntry.COLUMN_TITLE + "=?",
                new String[]{channel.title},
                null);

        channelCursor.moveToFirst();
        int channelIdIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry._ID);
        channelId = channelCursor.getLong(channelIdIndex);

        List<Episode> episodes = channel.episodes;
        Vector<ContentValues> contentValuesVector = new Vector<ContentValues>(episodes.size());
        int episodesCount = episodes.size();
        for(int i = episodesCount - 1; i >= 0; i--) {
            ContentValues episodeValues = new ContentValues();
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_TITLE, episodes.get(i).title);
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_LINK, episodes.get(i).link);
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_PUB_DATE, episodes.get(i).pubDate);
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION, episodes.get(i).description);
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_ENCLOSURE, episodes.get(i).enclosure);
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_DURATION, episodes.get(i).duration);
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_EXPLICIT, episodes.get(i).explicit);
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_EPISODE_IMAGE, episodes.get(i).episodeImage);
            episodeValues.put(PodcastContract.EpisodeEntry.COLUMN_CHANNEL_ID, channelId);

            contentValuesVector.add(episodeValues);
        }

        // add to database
        if(contentValuesVector.size() > 0) {
            ContentValues[] contentValuesArray = new ContentValues[contentValuesVector.size()];
            contentValuesVector.toArray(contentValuesArray);
            context.getContentResolver().bulkInsert(PodcastContract.EpisodeEntry.CONTENT_URI, contentValuesArray);
            Log.d(LOG_TAG, "inserted data");

            // delete old data so we don't build up an endless history
        }

        channelCursor.close();
    }
}
