package bbr.podcast;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;
import java.util.Vector;

import bbr.podcast.data.PodcastContract;
import bbr.podcast.feeds.Episode;
import bbr.podcast.feeds.PodcastChannel;

/**
 * Created by Me on 4/21/2017.
 */

public class PodcastEpsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String LOG_TAG = PodcastEpsAdapter.class.getSimpleName();

    PodcastChannel channel;
    String thumbnail;
    String feedUrl;
    Context context;
    private final int DESCRIPTION = 0;
    private final int SUBSCRIPTION = 1;

    private Bitmap thumbBitmap;

    private boolean subs = false;

    public PodcastEpsAdapter (PodcastChannel channel, String thumbnail, String feedUrl, Context context, boolean subs) {
        this.channel = channel;
        this.thumbnail = thumbnail;
        this.feedUrl = feedUrl;
        this.context = context;
        this.subs = subs;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case DESCRIPTION:
                View v1 = inflater.inflate(R.layout.item_description, parent, false);
                viewHolder = new DescriptionViewHolder(v1);
                break;
            case SUBSCRIPTION:
                View v2 = inflater.inflate(R.layout.item_subscription, parent, false);
                viewHolder = new SubscriptionViewHolder(v2);
                break;
            default:
                View v3 = inflater.inflate(R.layout.item_episode, parent, false);
                viewHolder = new EpisodeViewHolder(v3);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder (RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case DESCRIPTION:
                DescriptionViewHolder holder1 = (DescriptionViewHolder) holder;
                configureDescriptionViewHolder(holder1, position);
                break;
            case SUBSCRIPTION:
                SubscriptionViewHolder holder2 = (SubscriptionViewHolder) holder;
                configureSubscriptionViewHolder(holder2, position);
                break;
            default:
                EpisodeViewHolder holder3 = (EpisodeViewHolder) holder;
                configureEpisodeViewHolder(holder3, position);
                break;
        }

    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return channel.episodes.size() + 2;
    }

    private void configureDescriptionViewHolder (final DescriptionViewHolder holder, int position) {
        holder.titleTextView.setText(channel.title);
        holder.descriptionTextView.setText(channel.description);
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(context.getResources().getString(R.string.intent_pass_channel_title), channel.title);
                bundle.putString(context.getResources().getString(R.string.intent_pass_channel_description), channel.description);
                ChannelDescriptionBottomSheetFragment channelFragment = new ChannelDescriptionBottomSheetFragment();
                channelFragment.setArguments(bundle);
                PodcastEpsActivity podcastEpsActivity = (PodcastEpsActivity) context;
                channelFragment.show(podcastEpsActivity.getSupportFragmentManager(), "channelBottomSheet");
            }
        });
    }

    private void configureSubscriptionViewHolder (final SubscriptionViewHolder holder, int position) {



        if(subs == true) {
            holder.subButton.getDrawable().setColorFilter(context.getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.subButton.getDrawable().setColorFilter(context.getResources().getColor(R.color.colorBlack), PorterDuff.Mode.SRC_ATOP);
        }

        holder.subButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(subs) {
                    buildAlertDialogForDeleteSubs(channel, context, holder);

                } else {
                    insertData(context, channel, thumbnail, feedUrl);;
                    holder.subButton.getDrawable().setColorFilter(context.getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
                    subs = true;
                }
            }
        });
        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, channel.title);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, channel.description);
                context.startActivity(Intent.createChooser(sharingIntent, "Share via"));

            }
        });

       //channelCursor.close();

    }

    private void buildAlertDialogForDeleteSubs(final PodcastChannel channel, final Context context, final SubscriptionViewHolder holder) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        builder.setTitle("Unsubscribe channel")
                .setMessage("Are you sure you want to unsubscribe this channel?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        deleteData(channel, context);
                        holder.subButton.getDrawable().setColorFilter(context.getResources().getColor(R.color.colorBlack), PorterDuff.Mode.SRC_ATOP);
                        subs = false;
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void configureEpisodeViewHolder (EpisodeViewHolder holder, int position) {
        final Episode episode = channel.episodes.get(position - 2);
        Log.d(LOG_TAG, "episode: " + episode.title);
        holder.episodeTitleTextView.setText(episode.title);
        holder.episodeDateTextView.setText(episode.pubDate);
        //holder.episodeDurationTextView.setText(episode.duration);
        holder.episodeDurationTextView.setText(DateUtils.formatElapsedTime(convertToSeconds(episode.duration)));

        if (episode.episodeImage != null) {
            //loadBitmap(episode.episodeImage);
            Picasso.with(context).load(episode.episodeImage).resize(128, 128).into(holder.episodeThumbView);

            Log.d(LOG_TAG, "load episodeImage not null " + episode.episodeImage);
        } else {
            Picasso.with(context).load(channel.itunesImage).fit().into(holder.episodeThumbView);
        }

        holder.episodeInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("Subscribe", subs);
                bundle.putString("Description", episode.description);
                bundle.putString("EpisodeTitle", episode.title);
                bundle.putString("Enclosure", episode.enclosure);
                EpisodeBottomSheetFragment episodeBottomSheetFragment = new EpisodeBottomSheetFragment();
                episodeBottomSheetFragment.setArguments(bundle);
                PodcastEpsActivity podcastEpsActivity = (PodcastEpsActivity) context;
                episodeBottomSheetFragment.show(podcastEpsActivity.getSupportFragmentManager(), "BottomSheet");
            }
        });

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context c = v.getContext();
                Intent intent = new Intent(v.getContext(), MediaPlayerActivity.class)
                        .putExtra(c.getResources().getString(R.string.intent_pass_episode), episode)
                        .putExtra(c.getResources().getString(R.string.intent_pass_subscription), subs);

                        //.putExtra(c.getResources().getString(R.string.intent_pass_thumbnail), thumbnail)
                        //.putExtra(c.getResources().getString(R.string.intent_pass_itunesimage), channel.itunesImage);

                //c.startActivity(intent);
                ActivityCompat.startActivityForResult((Activity)c, intent, 0, null);
            }
        });

    }

    public static class DescriptionViewHolder extends RecyclerView.ViewHolder {

        public final View mView;
        public final TextView titleTextView;
        public final TextView descriptionTextView;

        public DescriptionViewHolder (View view) {
            super(view);
            mView = view;
            titleTextView = (TextView) mView.findViewById(R.id.podcast_title);
            descriptionTextView = (TextView) mView.findViewById(R.id.podcast_description);
        }
    }

    public static class SubscriptionViewHolder extends RecyclerView.ViewHolder {

        public final View mView;
        public final ImageButton subButton;
        public final ImageButton shareButton;

        public SubscriptionViewHolder (View view) {
            super(view);
            mView = view;
            subButton = (ImageButton) mView.findViewById(R.id.subscription);
            shareButton = (ImageButton) mView.findViewById(R.id.share);
        }
    }

    public static class EpisodeViewHolder extends RecyclerView.ViewHolder {

        public final View mView;
        public final TextView episodeTitleTextView;
        public final TextView episodeDateTextView;
        public final ImageView episodeThumbView;
        public final TextView episodeDurationTextView;
        public final ImageButton episodeInfoButton;

        public EpisodeViewHolder (View view) {
            super(view);
            mView = view;
            episodeTitleTextView = (TextView) mView.findViewById(R.id.episode_title);
            episodeDateTextView = (TextView) mView.findViewById(R.id.episode_pubDate);
            episodeThumbView = (ImageView) mView.findViewById(R.id.thumb);
            episodeDurationTextView = (TextView) mView.findViewById(R.id.episode_duration);
            episodeInfoButton = (ImageButton) mView.findViewById(R.id.episode_info);
        }
    }

    private void insertData(Context context, PodcastChannel channel, String thumbnail, String feedUrl) {
        long channelId = insertChannel(channel, thumbnail, feedUrl, context);

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

        int inserted = 0;
        // add to database
        if(contentValuesVector.size() > 0) {
            ContentValues[] contentValuesArray = new ContentValues[contentValuesVector.size()];
            contentValuesVector.toArray(contentValuesArray);
            context.getContentResolver().bulkInsert(PodcastContract.EpisodeEntry.CONTENT_URI, contentValuesArray);
            Log.d(LOG_TAG, "inserted data");

            // delete old data so we don't build up an endless history
        }
    }

    private long insertChannel(PodcastChannel channel, String thumbnail, String feedUrl, Context context) {
        long channelId;

        // First, check if the channel with the title exists
        Cursor channelCursor = context.getContentResolver().query(
                PodcastContract.ChannelEntry.CONTENT_URI,
                new String[]{PodcastContract.ChannelEntry._ID},
                PodcastContract.ChannelEntry.COLUMN_TITLE + "=?",
                new String[]{channel.title},
                null);

        if(channelCursor.moveToFirst()) {
            int channelIdIndex = channelCursor.getColumnIndex(PodcastContract.ChannelEntry._ID);
            channelId = channelCursor.getLong(channelIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues channelValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_TITLE, channel.title);
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_DESCRIPTION, channel.description);
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_LANGUAGE, channel.language);
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_COPYRIGHT, channel.copyright);
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_EDITOR, channel.managingEditor);
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_ITUNES_IMAGE, channel.itunesImage);
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_IMAGE_URL, channel.imageUrl);
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_THUMBNAIL, thumbnail);
            channelValues.put(PodcastContract.ChannelEntry.COLUMN_FEED_URL, feedUrl);

            // Finally, insert location data into the database.
            Uri insertedUri = context.getContentResolver().insert(PodcastContract.ChannelEntry.CONTENT_URI, channelValues);

            // The resulting URI contains the ID for the row.  Extract the channelId from the Uri.
            channelId = ContentUris.parseId(insertedUri);
        }

        channelCursor.close();

        return channelId;
    }

    private void deleteData(PodcastChannel channel, Context context) {
        String channelSelection = PodcastContract.ChannelEntry.COLUMN_TITLE + " = ?";
        String[] channelSelectionArgs = {channel.title};
        Cursor channelCursor = context.getContentResolver().query(PodcastContract.ChannelEntry.CONTENT_URI,
                new String[]{PodcastContract.ChannelEntry._ID},
                channelSelection,
                channelSelectionArgs,
                null);
        channelCursor.moveToFirst();
        int channelId = channelCursor.getInt(channelCursor.getColumnIndex(PodcastContract.ChannelEntry._ID));
        Log.d(LOG_TAG, "delete channel Id " + channelId);

        Uri episodesWithPlaylistUri = PodcastContract.PlaylistEpisodeEntry.buildEpisodeWithPlaylistUri();
        Cursor cursor = context.getContentResolver().query(episodesWithPlaylistUri,
                        null,
                        PodcastContract.EpisodeEntry.COLUMN_CHANNEL_ID + "=?",
                        new String[]{String.valueOf(channelId)},
                        null);
        cursor.moveToFirst();
        int episodeNameColumnIndex = cursor.getColumnIndex(PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME);
        int episodePlaylistNameColumnIndex = cursor.getColumnIndex(PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME);
        int episodeFileNameColumnIndex = cursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_AUDIO_URI);
        Log.d(LOG_TAG, "delete cursor count: " + cursor.getCount());
        int playlistEpsRowsDeleted = 0;

        for (int i = 0; i < cursor.getCount(); i++) {
            String episodeName = cursor.getString(episodeNameColumnIndex);
            String episodePlaylistName = cursor.getString(episodePlaylistNameColumnIndex);
            int rowDeleted = context.getContentResolver().delete(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI,
                    PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME + " =? ",
                    new String[]{episodeName});
            Log.d(LOG_TAG, "delete episodeName " + episodeName);

            if (episodePlaylistName.equals("downloaded")) {
                String fileName = cursor.getString(episodeFileNameColumnIndex);
                File file;
                try {
                    file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                    file.delete();
                } catch (NullPointerException e) {
                    Log.e(LOG_TAG, e.toString());
                }
            }
            cursor.moveToNext();
            playlistEpsRowsDeleted += rowDeleted;
        }
        cursor.close();
        channelCursor.close();

        int playlistRowsDeleted = context.getContentResolver().delete(PodcastContract.ChannelEntry.CONTENT_URI, channelSelection, channelSelectionArgs);

        int episodesRowsDeleted = context.getContentResolver().delete(PodcastContract.EpisodeEntry.CONTENT_URI,
                PodcastContract.EpisodeEntry.COLUMN_CHANNEL_ID + " = ?",
                new String[]{String.valueOf(channelId)});

        Log.d(LOG_TAG, "deleted data " + playlistEpsRowsDeleted + " " + playlistRowsDeleted + " " + episodesRowsDeleted);
    }

    private long convertToSeconds(String strDuration) {
        Log.d(LOG_TAG, "strDuration: " + strDuration);
        String[] tokens = strDuration.split(":");
        long duration;
        if (tokens.length == 3) {
            int hours = Integer.parseInt(tokens[0]);
            int minutes = Integer.parseInt(tokens[1]);
            int seconds = Integer.parseInt(tokens[2]);
            duration = (3600 * hours + 60 * minutes + seconds);
        } else if (tokens.length == 2) {
            int minutes = Integer.parseInt(tokens[0]);
            int seconds = Integer.parseInt(tokens[1]);
            duration = (60 * minutes + seconds);
        } else if (tokens.length == 1) {
            int seconds = Integer.parseInt(tokens[0]);
            duration = seconds;
        } else {
            duration = -1;
        }
        Log.d(LOG_TAG, "duration: " + duration);
        return duration;
    }


}
