package bbr.podcast.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import bbr.podcast.service.MediaPlaybackService;
import bbr.podcast.R;
import bbr.podcast.feeds.Episode;

/**
 * Created by Me on 6/13/2017.
 */

public class PodcastWidgetProvider extends AppWidgetProvider {
    private static final String LOG_TAG = PodcastWidgetProvider.class.getSimpleName();

    private static Bitmap thumbBitmap;
    private static Target loadtarget;
    private Episode episode;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        updateWidgets(context, appWidgetManager, appWidgetIds, episode);


    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        episode = intent.getParcelableExtra(context.getString(R.string.intent_pass_episode));
        if(MediaPlaybackService.ACTION_METADATA_UPDATED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            updateWidgets(context, appWidgetManager, appWidgetIds, episode);
        }
    }

    public static void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Episode episode) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {

            Log.d(LOG_TAG, "updating");

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget);

            if(episode == null) {
                rv.setTextViewText(R.id.title_widget, context.getString(R.string.no_podcast_playing));
            } else {
                rv.setTextViewText(R.id.title_widget, episode.title);
                rv.setTextViewText(R.id.author_widget, episode.pubDate);
                loadBitmap(context, episode.episodeImage);
                if(thumbBitmap != null)  {
                    rv.setImageViewBitmap(R.id.thumb_nail_widget, thumbBitmap);
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
    }

    private static void loadBitmap(Context context, String url) {

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

        Picasso.with(context).load(url).into(loadtarget);
    }

    private static void handleLoadedBitmap(Bitmap b) {
        thumbBitmap = b;
    }

}
