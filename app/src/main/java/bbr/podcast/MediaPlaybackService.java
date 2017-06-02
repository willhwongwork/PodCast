package bbr.podcast;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import bbr.podcast.data.PodcastContract;
import bbr.podcast.feeds.Episode;
import bbr.podcast.utils.MediaStyleHelper;
import bbr.podcast.utils.PlaylistHelper;


/**
 * Created by Me on 4/24/2017.
 */

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {
    private static final String LOG_TAG = MediaPlaybackService.class.getSimpleName();
    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mMediaSessionCompat;
    private PlaybackStateCompat.Builder playbackstateBuilder;
    private NotificationCompat.Builder notificationBuilder;
    private MediaMetadataCompat.Builder metadataBuilder;
    private Episode episode;
    private String itunesImage;

    // make sure to set Target as strong reference
    private Target loadtarget;
    private Bitmap artBitmap;
    private Bitmap scaledDownBitmap;

    private List<Episode> playlist;
    private String uri;
    private int position;

    private int currentPlaybackState = -1;

    private boolean voluntaryPaused = false;
    private boolean downloaded = false;
    private boolean subs = false;

    private String fileName = null;

    private Runnable mDelayedStopRunnable = new Runnable() {
        @Override
        public void run() {
            mMediaPlayer.stop();
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            stopForeground(false);
        }
    };

    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( mMediaPlayer != null && mMediaPlayer.isPlaying() ) {
                mMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
        }
    };

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            super.onPlay();
            if( !successfullyRetrievedAudioFocus() ) {
                return;
            }
            mMediaSessionCompat.setActive(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

            showPlayingNotification();
            mMediaPlayer.start();

            Log.d(LOG_TAG, "onplay");

            // The service needs to continue running even after the bound client (usually a
            // MediaController) disconnects, otherwise the playback will stop.
            // Calling startService(Intent) will keep the service running until it is explicitly killed.
            startService(new Intent(getApplicationContext(), MediaPlaybackService.class));

        }

        @Override
        public void onPause() {
            super.onPause();

            Log.d(LOG_TAG, "onPause");

            if( mMediaPlayer.isPlaying() ) {
                mMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                showPausedNotification();
                voluntaryPaused = true;
                stopForeground(false);
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.d(LOG_TAG, "onSTOP");
            stopSelf();
        }

        @Override
        public void onSeekTo(long position) {
            super.onSeekTo(position);
            Log.d(LOG_TAG, "onSeekTo position: " + position);
            mMediaPlayer.seekTo((int) position);
            if (mMediaPlayer.isPlaying()) {
                //setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }

        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extra) {
            super.onPlayFromUri(uri, extra);
            playlist = null;

            Log.d(LOG_TAG, "onPlayFromUri");
            if (uri != null && extra != null) {
                extra.setClassLoader(getClassLoader());
                subs = extra.getBoolean(getResources().getString(R.string.intent_pass_subscription));
                episode = (Episode) extra.getParcelable(getResources().getString(R.string.intent_pass_episode));
                preparePlayback(uri.toString());
            }
        }

        @Override
        public void onCustomAction(String customAction, Bundle extra) {
            super.onCustomAction(customAction, extra);
            if (customAction.equals("sendPlaylist")) {
                extra.setClassLoader(getClassLoader());
                subs = extra.getBoolean(getResources().getString(R.string.intent_pass_subscription));
                playlist = extra.getParcelableArrayList(getResources().getString(R.string.intent_pass_episodes_list));
                position = extra.getInt(getResources().getString(R.string.intent_pass_episodes_position));
                episode = playlist.get(position);
                uri = episode.enclosure;
                preparePlayback(uri);

            }
        }

        @Override
        public void onSkipToNext() {
            if (playlist != null && position < playlist.size()-1) {
                position++;
                episode = playlist.get(position);
                uri = episode.enclosure;
                preparePlayback(uri);
            }
        }

        @Override
        public void onSkipToPrevious() {
            if (playlist != null && position > 0) {
                position--;
                episode = playlist.get(position);
                uri = episode.enclosure;
                preparePlayback(uri);
            }
        }

    };

    private void preparePlayback(String uri) {
        try {
            NotificationManagerCompat.from(this).cancel(1);
            setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
            mMediaPlayer.reset();
            Log.d(LOG_TAG, "reset player");

            episode = getEpisode(uri);
            changeMediaSessionMetadata(episode, itunesImage);
            Log.d(LOG_TAG, "episodeCursor title: " + episode.title);

            downloaded = false;
            checkDownload(uri);
            if(downloaded) {
                Log.d(LOG_TAG, "play from file");
                mMediaPlayer.setDataSource(Uri.fromFile(new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)).toString());
            }else {
                mMediaPlayer.setDataSource(uri);
            }

            Log.d(LOG_TAG, "buffer before prepare done");
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    if (episode != null) {
                        Log.d(LOG_TAG, "OnPrepared episodeCursor is not null");
                    }
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    showPlayingNotification();
                    checkRecent(episode.title);

                }
            });

            // The service needs to continue running even after the bound client (usually a
            // MediaController) disconnects, otherwise the playback will stop.
            // Calling startService(Intent) will keep the service running until it is explicitly killed.
            startService(new Intent(getApplicationContext(), MediaPlaybackService.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "onCreate");

        initMediaPlayer();
        initMediaSession();
        initNoisyReceiver();
        initMediaSessionMetadata();

        playbackstateBuilder = new PlaybackStateCompat.Builder();
        setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        unregisterReceiver(mNoisyReceiver);

        mMediaSessionCompat.release();
        NotificationManagerCompat.from(this).cancel(1);
        stopForeground(true);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(LOG_TAG, "onGetRoot");
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(LOG_TAG, "onLoadChildren");
        result.sendResult(null);
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setVolume(1.0f, 1.0f);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(playlist == null) {
                    mp.stop();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                    stopForeground(true);
                } else if(playlist != null && position < playlist.size()-1) {
                    position++;
                    episode = playlist.get(position);
                    uri = episode.enclosure;
                    preparePlayback(uri);
                }
            }
        });
        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.d(LOG_TAG, "onInfo: " + what);
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                        break;
                }
                return false;
            }
        });
    }

    private void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag", mediaButtonReceiver, null);

        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        mMediaSessionCompat.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mMediaSessionCompat.getSessionToken());
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);
    }

    private void initMediaSessionMetadata() {
        Log.d(LOG_TAG, "initMediaSessionMetaData");
        metadataBuilder = new MediaMetadataCompat.Builder();
        //Notification icon in card
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Display Title");
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Display Subtitle");
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    private void changeMediaSessionMetadata(Episode episode, String image) {
        Log.d(LOG_TAG, "changeMediaSessionMetadata");

        //lock screen icon for pre lollipop
        loadBitmap(image);
        //metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, image);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, image);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, episode.title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, episode.pubDate);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, convertToDuration(episode.duration));

        mMediaSessionCompat.setMetadata(metadataBuilder.build());


    }

    private long convertToDuration(String strDuration) {
        Log.d(LOG_TAG, "strDuration: " + strDuration);
        String[] tokens = strDuration.split(":");
        long duration;
        if (tokens.length == 3) {
            int hours = Integer.parseInt(tokens[0]);
            int minutes = Integer.parseInt(tokens[1]);
            int seconds = Integer.parseInt(tokens[2]);
            duration = (3600 * hours + 60 * minutes + seconds) * 1000;
        } else if (tokens.length == 2) {
            int minutes = Integer.parseInt(tokens[0]);
            int seconds = Integer.parseInt(tokens[1]);
            duration = (60 * minutes + seconds) * 1000;
        } else if (tokens.length == 1) {
            int seconds = Integer.parseInt(tokens[0]);
            duration = seconds * 1000;
        } else {
            duration = -1;
        }
        Log.d(LOG_TAG, "duration: " + duration);
        return duration;
    }

    private boolean successfullyRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    private void setMediaPlaybackState(int state) {
        Log.d(LOG_TAG, "setMediaPlaybackState " + state);
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        }
        playbackstateBuilder.setState(state, mMediaPlayer.getCurrentPosition(), 1.0f, SystemClock.elapsedRealtime());
        mMediaSessionCompat.setPlaybackState(playbackstateBuilder.build());

        currentPlaybackState = state;
    }

    private void showPlayingNotification() {
        notificationBuilder = MediaStyleHelper.from(this, mMediaSessionCompat, artBitmap);

        if( notificationBuilder == null ) {
            return;
        }

        Log.d(LOG_TAG,"showPlayingNotification");

        notificationBuilder.mActions.clear();

        notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_pause_black_36dp, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        notificationBuilder.setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setMediaSession(mMediaSessionCompat.getSessionToken())
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP)));
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        // Display the notification and place the service in the foreground
        NotificationManagerCompat.from(MediaPlaybackService.this).notify(1, notificationBuilder.build());
        startForeground(1, notificationBuilder.build());
    }

    private void showPausedNotification() {
        notificationBuilder = MediaStyleHelper.from(this, mMediaSessionCompat, artBitmap);

        if( notificationBuilder == null ) {
            return;
        }

        Log.d(LOG_TAG, "showPausedNotification");

        notificationBuilder.mActions.clear();

        notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_play_arrow_black_36dp, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        notificationBuilder.setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setMediaSession(mMediaSessionCompat.getSessionToken())
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP)));
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        NotificationManagerCompat.from(this).notify(1, notificationBuilder.build());
        stopForeground(false);
    }


    public void loadBitmap(String url) {

        if (loadtarget == null) loadtarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // do something with the Bitmap
                handleLoadedBitmap(bitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }

        };

        Picasso.with(this).load(url).into(loadtarget);
    }

    public void handleLoadedBitmap(Bitmap b) {
        artBitmap = b;
        //Notification icon in card
        scaledDownBitmap = ThumbnailUtils.extractThumbnail(b, 256, 256);
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, scaledDownBitmap);
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, scaledDownBitmap);

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, scaledDownBitmap);
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Handler mHandler = new Handler();
        switch( focusChange ) {
            case AudioManager.AUDIOFOCUS_LOSS: {
                if( mMediaPlayer.isPlaying() ) {
                    // Permanent loss of audio focus
                    // Pause playback immediately
                    mMediaPlayer.pause();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    // Wait 30 seconds before stopping playback
                    mHandler.postDelayed(mDelayedStopRunnable,
                            TimeUnit.SECONDS.toMillis(30));
                }
                audioManager.abandonAudioFocus(this);
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                mMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                voluntaryPaused = false;
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                if( mMediaPlayer != null && currentPlaybackState == PlaybackStateCompat.STATE_PLAYING) {
                    mMediaPlayer.setVolume(0.3f, 0.3f);
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                if( mMediaPlayer != null ) {
                    if(voluntaryPaused) {
                        mMediaPlayer.setVolume(1.0f, 1.0f);
                        break;
                    }
                    //if( !mMediaPlayer.isPlaying() && currentPlaybackState != PlaybackStateCompat.STATE_PAUSED) {
                    if(currentPlaybackState == PlaybackStateCompat.STATE_PAUSED && voluntaryPaused == false) {
                        mMediaPlayer.start();
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    }
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if( mMediaPlayer != null ) {
            mMediaPlayer.release();
        }
    }

    private Episode getEpisode (String url) {
        if (episode != null && episode.episodeImage != null) {
            itunesImage = episode.episodeImage;
            Log.d(LOG_TAG, "episodeImage: " + episode.episodeImage);
            return episode;
        }

/*        Cursor episodeCursor = getContentResolver().query(PodcastContract.EpisodeEntry.CONTENT_URI,
                null,
                PodcastContract.EpisodeEntry.COLUMN_ENCLOSURE + "=?",
                new String[]{url},
                null);

        episodeCursor.moveToFirst();

        int eTitleIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_TITLE);
        String eTitle = episodeCursor.getString(eTitleIndex);
        int linkIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_LINK);
        String link = episodeCursor.getString(linkIndex);
        int pubDateIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_PUB_DATE);
        String pubDate = episodeCursor.getString(pubDateIndex);
        int eDescriptionIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION);
        String eDescription = episodeCursor.getString(eDescriptionIndex);
        int enclosureIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_ENCLOSURE);
        String enclosure = episodeCursor.getString(enclosureIndex);
        int durationIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_DURATION);
        String duration = episodeCursor.getString(durationIndex);
        int explicitIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_EXPLICIT);
        String explicit = episodeCursor.getString(explicitIndex);
        int episodeImageIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_EPISODE_IMAGE);
        String episodeImage = episodeCursor.getString(episodeImageIndex);

        int channelIdIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_CHANNEL_ID);
        int channelId = episodeCursor.getInt(channelIdIndex);



        Log.d(LOG_TAG, "episodeImage: " + episode.episodeImage);
        if (episode.episodeImage != null) {
            itunesImage = episode.episodeImage;
        } else {
            itunesImage = getChannelImage(channelId);
        }*/

        return episode;

        //return new Episode(eTitle, link, pubDate, eDescription, enclosure, duration, explicit, episodeImage);*/
    }

    private String getChannelImage(int channelId) {
/*        String title = null;
        String description = null;
        String language = null;
        String copyright = null;
        String managingEditor = null;*/
        String itunesImage = null;
/*        String imageUrl = null;
        List<Episode> episodes = new ArrayList<>();;*/
        Cursor channelCursor = getContentResolver().query(PodcastContract.ChannelEntry.CONTENT_URI,
                null,
                PodcastContract.ChannelEntry._ID + "=?",
                new String[]{Integer.toString(channelId)},
                null);

        channelCursor.moveToFirst();
/*        int titleIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_TITLE);
        title = channelCursor.getString(titleIndex);
        int descriptionIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_DESCRIPTION);
        description = channelCursor.getString(descriptionIndex);
        int languageIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_LANGUAGE);
        language = channelCursor.getString(languageIndex);
        int copyrightIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_COPYRIGHT);
        copyright = channelCursor.getString(copyrightIndex);
        int managingEditorIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_EDITOR);
        managingEditor = channelCursor.getString(managingEditorIndex);*/
        int itunesImageIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_ITUNES_IMAGE);
        itunesImage = channelCursor.getString(itunesImageIndex);
/*        int imageUrlIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_IMAGE_URL);
        imageUrl = channelCursor.getString(imageUrlIndex);
        */
        return itunesImage;
    }

    private void checkDownload(String url) {
        Cursor episodeCursor = getContentResolver().query(PodcastContract.EpisodeEntry.CONTENT_URI,
                null,
                PodcastContract.EpisodeEntry.COLUMN_ENCLOSURE + "=?",
                new String[]{url},
                null);

        if (episodeCursor.getCount() == 0) {
            return;
        }
        episodeCursor.moveToFirst();

        int fileNameIndex = episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_AUDIO_URI);
        fileName = episodeCursor.getString(fileNameIndex);
        if (fileName == null){ return;}

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        if (file.exists()) {
            Log.d(LOG_TAG, " downloaded file " + fileName + " exists");
            downloaded = true;
        }

    }

    private void checkRecent(String title) {
        Log.d(LOG_TAG, "check Recent " + title);

        if(!subs) {return;}

        final Cursor cursor = PlaylistHelper.getPlaylistEpisodesCursor(title, this);
        final List<String> playlistEpsNames = new ArrayList<String>();
        cursor.moveToFirst();
        int playlistNameColumnIndex = cursor.getColumnIndex(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME);
        for (int i = 0; i < cursor.getCount(); i++) {
            String playlistEpsName = cursor.getString(playlistNameColumnIndex);
            playlistEpsNames.add(playlistEpsName);
            Log.d(LOG_TAG, "check Recent " + playlistEpsName);
            cursor.moveToNext();
        }

        boolean isRecent = isRecent(playlistEpsNames);

        if (isRecent) {

        } else {
            PlaylistHelper.addRecentToDb(title, this);
        }

        cursor.close();

    }

    private boolean isRecent(List<String> playlistEpsNames) {
        Log.d(LOG_TAG, "is recent " + playlistEpsNames.contains("recent"));
        if (!playlistEpsNames.contains("recent")) {
            return false;
        } else {
            return true;
        }
    }
}
