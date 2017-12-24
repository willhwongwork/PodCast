package bbr.podcast.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import bbr.podcast.service.MediaPlaybackService;
import bbr.podcast.R;
import bbr.podcast.feeds.Episode;

/**
 * Created by Me on 4/26/2017.
 */

public class MediaPlayerActivity extends AppCompatActivity {
    private static final String LOG_TAG = MediaPlayerActivity.class.getSimpleName();
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private static final String KEY_POSITION_VALUE = "position";

    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mPlayPause;
    private TextView mStart;
    private TextView mEnd;
    private SeekBar mSeekbar;
    private TextView mLine1;
    private TextView mLine2;
    private TextView mLine3;
    private ProgressBar mLoading;
    private View mControllers;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private ImageView mBackgroundImage;
    private Target loadtarget;
    private String itunesImage;
    private Bitmap itunesBitmap;

    private Toolbar mToolbar;

    private final Handler mHandler = new Handler();
    private MediaBrowserCompat mMediaBrowser;

    private Uri streamUrl = null;
    private Episode episode;

    private List<Episode> playlist;
    private int position;
    private boolean subs = false;

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;
    private PlaybackStateCompat mLastPlaybackState;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");
        setContentView(R.layout.activity_player);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        mBackgroundImage = (ImageView) findViewById(R.id.background_image);
        mPauseDrawable = ContextCompat.getDrawable(this, R.drawable.ic_pause_circle_filled_white_48dp);
        mPlayDrawable = ContextCompat.getDrawable(this, R.drawable.ic_play_circle_filled_white_48dp);
        mPlayPause = (ImageView) findViewById(R.id.play_pause);
        mSkipNext = (ImageView) findViewById(R.id.next);
        mSkipPrev = (ImageView) findViewById(R.id.prev);
        mStart = (TextView) findViewById(R.id.startText);
        mEnd = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        mLine1 = (TextView) findViewById(R.id.line1);
        mLine2 = (TextView) findViewById(R.id.line2);
        mLine3 = (TextView) findViewById(R.id.line3);
        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        mControllers = findViewById(R.id.controllers);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                        MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls();
                controls.skipToNext();
            }
        });

        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().skipToPrevious();
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStateCompat state = MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getPlaybackState();
                if (state != null) {
                    MediaControllerCompat.TransportControls controls = MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls();
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING:
                        case PlaybackStateCompat.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        default:
                            Log.d(LOG_TAG, "onClick with state " + state.getState());
                    }
                }
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(LOG_TAG, "seekbar onProgressChanged " + "progress: " + progress);
                mStart.setText(DateUtils.formatElapsedTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(LOG_TAG, "seekbar onStartTrackingTouch");
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(LOG_TAG, "seekbar onStopTrackingTouch");
                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().seekTo(seekBar.getProgress() * 1000);
                scheduleSeekbarUpdate();
            }
        });

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null) {
            updateFromParams(getIntent());
        } else {
            //long resumedPosition = savedInstanceState.getLong(KEY_POSITION_VALUE);
            //mSeekbar.setProgress((int) (resumedPosition / 1000));
        }


        episode = getIntent().getParcelableExtra(getResources().getString(R.string.intent_pass_episode));
        playlist = getIntent().getParcelableArrayListExtra(getResources().getString(R.string.intent_pass_episodes_list));
        position = getIntent().getIntExtra(getResources().getString(R.string.intent_pass_episodes_position), -1);
        subs = getIntent().getBooleanExtra(getResources().getString(R.string.intent_pass_subscription), false);
        //thumbnail = getIntent().getStringExtra(getResources().getString(R.string.intent_pass_thumbnail));
        //itunesImage = getIntent().getStringExtra(getResources().getString(R.string.intent_pass_itunesimage));

        // Create MediaBrowserServiceCompat
        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaPlaybackService.class),
                mConnectionCallbacks,
                null); // optional Bundle
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        Log.d(LOG_TAG, "conectToSession");
        MediaControllerCompat mediaController = new MediaControllerCompat(
                MediaPlayerActivity.this, token);
        if (mediaController.getMetadata() == null) {
            finish();
            return;
        }
        setSupportMediaController(mediaController);
        mediaController.registerCallback(controllerCallback);
        PlaybackStateCompat state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadataCompat metadata = mediaController.getMetadata();
        if (metadata != null) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING || state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        //long currentPosition = mLastPlaybackState.getPosition();
        //outState.putLong(KEY_POSITION_VALUE, currentPosition);
    }

    private void updateFromParams(Intent intent) {

    }

    private void scheduleSeekbarUpdate() {
        Log.d(LOG_TAG, "scheduleSeekbarUpdate");
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        Log.d(LOG_TAG, "stopSeekbarUpdate");
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(MediaPlayerActivity.this) != null) {
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).unregisterCallback(controllerCallback);
        }
        if(mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        stopSeekbarUpdate();
        mExecutorService.shutdown();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }

        return super.onOptionsItemSelected(item);
    }

    private void fetchImage(MediaDescriptionCompat description) {
        if (description.getIconUri() == null) {
            Log.d(LOG_TAG, "getIconUri is null");
        }else {
            Log.d(LOG_TAG, "getIconUri is " + description.getIconUri());
            itunesImage = description.getIconUri().toString();
        }
        //loadBitmap(itunesImage);
        //mBackgroundImage.setImageBitmap(itunesBitmap);
        Picasso.with(this).load(itunesImage).into(mBackgroundImage);
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
        itunesBitmap = b;
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }
        Log.d(LOG_TAG, "updateMediaDescription called");
        mLine1.setText(description.getTitle());
        mLine2.setText(description.getSubtitle());
        fetchImage(description);
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            Log.d(LOG_TAG,"upateDurration metadata is null");
            return;
        }
        Log.d(LOG_TAG, "seekbar updateDuration called");
        long duration = (long) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        Log.d(LOG_TAG, "seekbar duration " + duration);
        mSeekbar.setMax((int)duration/1000);
        mEnd.setText(DateUtils.formatElapsedTime(duration/1000));
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        Log.d(LOG_TAG, "updatePlaybackState");
        if (state == null) {
            Log.d(LOG_TAG, "state is null");
            return;
        }
        mLastPlaybackState = state;

        Log.d(LOG_TAG, "State is " + state + " " + state.getState());

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(mPauseDrawable);
                mControllers.setVisibility(View.VISIBLE);
                mLine3.setVisibility(View.INVISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mControllers.setVisibility(View.VISIBLE);
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                mLine3.setVisibility(View.INVISIBLE);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                mLine3.setVisibility(View.INVISIBLE);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                mPlayPause.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.VISIBLE);
                mControllers.setVisibility(View.INVISIBLE);
                mLine3.setText(R.string.loading);
                stopSeekbarUpdate();
                break;
            default:
                Log.d(LOG_TAG, "Unhandled state" + state.getState());
        }

        mSkipNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
                ? View.INVISIBLE : View.VISIBLE );
        mSkipPrev.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
                ? View.INVISIBLE : View.VISIBLE );
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        Log.d(LOG_TAG, "seekbar updateProgress " + "currentPosition: " + currentPosition);
        if (mLastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
            Log.d(LOG_TAG, "seekbar updateProgress " + "timeDelta: " + timeDelta + " playbackSpeed: " + mLastPlaybackState.getPlaybackSpeed());
            Log.d(LOG_TAG, "seekbar updateProgress " + "latest position " + currentPosition);
        }
        mSeekbar.setProgress((int) (currentPosition / 1000));
    }

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(LOG_TAG, "onConnected");
                    try {
                        connectToSession(mMediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, "could not connect media controller");
                        e.printStackTrace();
                    }

                    // Finish building the UI
                    playFromUri();

                    sendPlaylist();
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                }

                @Override
                public void onConnectionFailed() {
                    // The Service has refused our connection
                }
            };

    private void playFromUri() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MediaPlayerActivity.this);
        int state = mediaController.getPlaybackState().getState();

        if (getCallingActivity() != null) {
            Log.d(LOG_TAG, getCallingActivity().getClassName());
            if (getCallingActivity().getClassName().equals("bbr.podcast.activity.PodcastEpsActivity")) {
                Log.d(LOG_TAG, "playFromUri");
                Bundle extra = new Bundle();
                extra.putParcelable(getResources().getString(R.string.intent_pass_episode), episode);
                extra.putBoolean(getResources().getString(R.string.intent_pass_subscription), subs);
                //extra.putString(getResources().getString(R.string.intent_pass_thumbnail), thumbnail);
                streamUrl = Uri.parse(episode.enclosure);

                mediaController.getTransportControls().playFromUri(streamUrl, extra);
            }
        }

        // Register a Callback to stay in sync
        //mediaController.registerCallback(controllerCallback);
    }

    private void sendPlaylist() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MediaPlayerActivity.this);

        if (getCallingActivity() != null) {
            Log.d(LOG_TAG, getCallingActivity().getClassName());
            if (getCallingActivity().getClassName().equals("bbr.podcast.activity.PlaylistEpsActivity")) {
                subs = true;
                Log.d(LOG_TAG, "sendPlaylist");
                Bundle extra = new Bundle();
                extra.putInt(getResources().getString(R.string.intent_pass_episodes_position), position);
                extra.putBoolean(getResources().getString(R.string.intent_pass_subscription), subs);
                extra.putParcelableArrayList(getResources().getString(R.string.intent_pass_episodes_list), (ArrayList<Episode>)playlist);

                mediaController.getTransportControls().sendCustomAction("sendPlaylist", extra);
            }
        }


    }

    MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if(metadata != null) {
                Log.d(LOG_TAG, "onMetadataChanged");
                Log.d(LOG_TAG, "Received metadata state change to episode=" +
                        metadata.getDescription().getTitle());
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            Log.d(LOG_TAG, "onPlaybackStateChanged " + state);
            Log.d(LOG_TAG, "Received playback state change to state " + state.getState());
            updatePlaybackState(state);
        }
    };

}
