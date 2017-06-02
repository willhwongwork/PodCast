package bbr.podcast.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

/**
 * Created by Me on 4/25/2017.
 */

public class MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     * @param context Context used to construct the notification.
     * @param mediaSession Media session to get information.
            * @return A pre-built notification with information from the given media session.
     */
    public static NotificationCompat.Builder from(
            Context context, MediaSessionCompat mediaSession, Bitmap bitmap) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        if(bitmap != null) {
            builder.setLargeIcon(bitmap);
            Log.d("Log helper", "bitmap is not null from MediaPlaybackService");
        }
        builder
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                //.setLargeIcon(description.getIconBitmap())
                .setContentIntent(controller.getSessionActivity())
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        return builder;
    }

}
