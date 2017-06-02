package bbr.podcast;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by Me on 5/5/2017.
 */

public class PlaybackControlsFragment extends Fragment {

    private static final String LOG_TAG = PlaybackControlsFragment.class.getSimpleName();

    private ViewSwitcher viewSwitcher;
    private LinearLayout playPauseLl;
    private LinearLayout progressBarLl;
    private ImageButton playPause;
    private ProgressBar progressBar;
    private TextView title;
    private TextView subtitle;
    private TextView extraInfo;
    private ImageView thumbnailView;
    private String thumbUrl;
    private Bitmap thumbBitmap;

    private Target loadtarget;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            Log.d(LOG_TAG, "Received playback state change to state " + state.getState());
            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            Log.d(LOG_TAG, "Received metadata state change to episode=" +
                    metadata.getDescription().getTitle());
            PlaybackControlsFragment.this.onMetadataChanged(metadata);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        viewSwitcher = (ViewSwitcher) rootView.findViewById(R.id.viewSwitcher1);
        playPauseLl = (LinearLayout) rootView.findViewById(R.id.play_pause_layout);
        progressBarLl = (LinearLayout) rootView.findViewById(R.id.progressbar_layout);

        playPause = (ImageButton) rootView.findViewById(R.id.play_pause);
        playPause.setEnabled(true);
        playPause.setOnClickListener(buttonListener);

        title = (TextView) rootView.findViewById(R.id.title);
        subtitle = (TextView) rootView.findViewById(R.id.author);
        extraInfo = (TextView) rootView.findViewById(R.id.extra_info);
        thumbnailView = (ImageView) rootView.findViewById(R.id.thumb_nail);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }

        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "fragment onStart");

        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            onConnected();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "fragment.onStop");
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.unregisterCallback(mCallback);
        }
    }

    public void onConnected() {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        Log.d(LOG_TAG, "onConnected, mediaController");
        if (controller != null) {
            onMetadataChanged(controller.getMetadata());
            onPlaybackStateChanged(controller.getPlaybackState());
            controller.registerCallback(mCallback);
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        Log.d(LOG_TAG, "onMetadataChanged");
        if (getActivity() == null) {
            //onMetadataChanged called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring
            return;
        }
        if (metadata == null) {
            return;
        }

        title.setText(metadata.getDescription().getTitle());
        subtitle.setText(metadata.getDescription().getSubtitle());

        String iconUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            iconUrl = metadata.getDescription().getIconUri().toString();
        }
        if(!TextUtils.equals(iconUrl, thumbUrl)) {
            Log.d(LOG_TAG, "loading thumbnail " + thumbUrl + " iconUrl " + iconUrl);
            thumbUrl = iconUrl;
            Picasso.with(getActivity()).setLoggingEnabled(true);
            Picasso.with(getActivity())
                    .load(thumbUrl)
                    .fit()
                    .into(thumbnailView);
        }
    }

    public void setExtraInfo(String eInfo) {
        if (extraInfo == null) {
            extraInfo.setVisibility(View.GONE);
        } else {
            extraInfo.setText(eInfo);
            extraInfo.setVisibility(View.VISIBLE);
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        Log.d(LOG_TAG, "onPlaybackStateChanged");
        if (getActivity() == null) {
            //onPlaybackStateChanged called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.
            return;
        }
        if (state == null) {
            return;
        }

        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                if(viewSwitcher.getCurrentView() == progressBarLl) {
                    viewSwitcher.showNext();
                }
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                Log.e(LOG_TAG, "error playbackstate: " + state.getErrorMessage());
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                if(viewSwitcher.getCurrentView() == playPauseLl) {
                    viewSwitcher.showNext();
                }
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                if(viewSwitcher.getCurrentView() == progressBarLl) {
                    viewSwitcher.showNext();
                }
                break;

        }

        if (enablePlay) {
            playPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_36dp));
        } else {
            playPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_36dp));
        }
    }

    private final View.OnClickListener buttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaControllerCompat controller = ((FragmentActivity) getActivity())
                    .getSupportMediaController();
            PlaybackStateCompat stateObj = controller.getPlaybackState();
            final int state = stateObj == null ?
                    PlaybackStateCompat.STATE_NONE : stateObj.getState();

            Log.d(LOG_TAG, "Button pressed, in state" + state);
            switch (v.getId()) {
                case R.id.play_pause:
                    Log.d(LOG_TAG, "Play button pressed, in state " + state);
                    if (state == PlaybackStateCompat.STATE_PAUSED ||
                            state == PlaybackStateCompat.STATE_STOPPED ||
                            state == PlaybackStateCompat.STATE_NONE) {
                        playMedia();
                    }  else if (state == PlaybackStateCompat.STATE_PLAYING ||
                            state == PlaybackStateCompat.STATE_BUFFERING ||
                            state == PlaybackStateCompat.STATE_CONNECTING) {
                        pauseMedia();
                    }
                    break;
            }
        }
    };

    private void playMedia() {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia() {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }



}
