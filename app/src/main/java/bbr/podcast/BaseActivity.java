package bbr.podcast;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import bbr.podcast.utils.NetworkHelper;

/**
 * Created by Me on 5/5/2017.
 */

public class BaseActivity extends AppCompatActivity {

    private static final String LOG_TAG = BaseActivity.class.getSimpleName();

    private MediaBrowserCompat mediaBrowser;
    private PlaybackControlsFragment controlsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Connect a media browser just to get the media session token. There are other ways
        // this can be done, for example by sharing the session token directly.
        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaPlaybackService.class), connectionCallback, null);
    }

    @Override
    protected void onStart() {
        super.onStart();

        controlsFragment = (PlaybackControlsFragment) getFragmentManager().findFragmentById(R.id.fragment_playback_controls);
        if (controlsFragment == null) {
            throw new IllegalStateException("Missing fragment with id 'controls'.");
        }

        hidePlaybackControls();

        mediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (getSupportMediaController() != null) {
            getSupportMediaController().unregisterCallback(mediaControllerCallback);
        }
        mediaBrowser.disconnect();
    }

    public MediaBrowserCompat getMediaBrowser() {
        return mediaBrowser;
    }

    protected void onMediaControllerConnected() {
        // empty implementation, can be overridden by clients.
    }

    protected void showPlaybackControls() {
        Log.d(LOG_TAG, "showPlaybackControls");
        if (NetworkHelper.isOnline(this)) {
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom,
                            R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
                    .show(controlsFragment)
                    .commitAllowingStateLoss();
        }
    }

    protected void hidePlaybackControls() {
        Log.d(LOG_TAG, "hidePlaybackControls");
        getFragmentManager().beginTransaction()
                .hide(controlsFragment)
                .commit();
    }

    /**
     * Check if the MediaSession is active and in a "playback-able" state
     * (not NONE and not STOPPED).
     *
     * @return true if the MediaSession's state requires playback controls to be visible.
     */
    protected boolean shouldShowControls() {
        MediaControllerCompat mediaController = getSupportMediaController();
        if (mediaController == null ||
                mediaController.getMetadata() == null ||
                mediaController.getPlaybackState() == null) {
            return false;
        }
        int state = mediaController.getPlaybackState().getState();
        Log.d(LOG_TAG, "shouldShowControls ? state: " + state);
        switch (state) {
            case PlaybackStateCompat.STATE_ERROR:
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                return false;
            default:
                return true;
        }
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
        setSupportMediaController(mediaController);
        mediaController.registerCallback(mediaControllerCallback);

        if (shouldShowControls()) {
            showPlaybackControls();
        } else {
            Log.d(LOG_TAG, "connectionCallback.onConnected: " +
                    "hiding controls because metadata is null");
            hidePlaybackControls();
        }

        if (controlsFragment != null) {
            controlsFragment.onConnected();
        }

        onMediaControllerConnected();
    }

    // Callback that ensures that we are showing the controls
    private final MediaControllerCompat.Callback mediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                    if (shouldShowControls()) {
                        showPlaybackControls();
                    } else {
                        Log.d(LOG_TAG, "mediaControllerCallback.onPlaybackStateChanged: " +
                                "hiding controls because state is " + state.getState());
                        hidePlaybackControls();
                    }
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    if (shouldShowControls()) {
                        showPlaybackControls();
                    } else {
                        Log.d(LOG_TAG, "mediaControllerCallback.onMetadataChanged: " +
                                "hiding controls because metadata is null");
                        hidePlaybackControls();
                    }
                }
            };

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(LOG_TAG, "onConnected");
                    try {
                        connectToSession(mediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, e + "could not connect media controller");
                        hidePlaybackControls();
                    }
                }
            };





}
