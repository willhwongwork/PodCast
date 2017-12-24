package bbr.podcast.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
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

import java.util.ArrayList;
import java.util.List;

import bbr.podcast.R;
import bbr.podcast.activity.MediaPlayerActivity;
import bbr.podcast.activity.PlaylistEpsActivity;
import bbr.podcast.feeds.Episode;
import bbr.podcast.fragment.EpisodeBottomSheetFragment;

/**
 * Created by Me on 5/19/2017.
 */

public class PlaylistEpsAdapter extends RecyclerView.Adapter<PlaylistEpsAdapter.ViewHolder> {
    private static final String LOG_TAG = PlaylistEpsAdapter.class.getSimpleName();

    private Context context;
    private List<Episode> episodes;

    public PlaylistEpsAdapter(Context context, List<Episode> episodes) {
        this.context = context;
        this.episodes = episodes;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_episode, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        final Episode episode = episodes.get(position);
        holder.episodeTitleTextView.setText(episode.title);
        holder.episodeDateTextView.setText(episode.pubDate);
        holder.episodeDurationTextView.setText(DateUtils.formatElapsedTime(convertToSeconds(episode.duration)));

        Picasso.with(context).load(episode.episodeImage).resize(256, 256).into(holder.episodeThumbView);

        holder.episodeInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MediaPlayerActivity.class)
                        .putExtra(context.getResources().getString(R.string.intent_pass_episode), episode)
                        .putParcelableArrayListExtra(context.getResources().getString(R.string.intent_pass_episodes_list), (ArrayList<Episode>)episodes)
                        .putExtra(context.getResources().getString(R.string.intent_pass_episodes_position), position);


                ActivityCompat.startActivityForResult((Activity)context, intent, 0, null);
            }
        });

        holder.episodeInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("Subscribe", true);
                bundle.putString("Description", episode.description);
                bundle.putString("EpisodeTitle", episode.title);
                bundle.putString("Enclosure", episode.enclosure);
                EpisodeBottomSheetFragment episodeBottomSheetFragment = new EpisodeBottomSheetFragment();
                episodeBottomSheetFragment.setArguments(bundle);
                PlaylistEpsActivity playlistEpsActivity = (PlaylistEpsActivity) context;
                episodeBottomSheetFragment.show(playlistEpsActivity.getSupportFragmentManager(), "BottomSheet");
            }
        });

    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    public void swapEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
        notifyDataSetChanged();
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


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView episodeTitleTextView;
        public final TextView episodeDateTextView;
        public final ImageView episodeThumbView;
        public final TextView episodeDurationTextView;
        public final ImageButton episodeInfoButton;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            episodeTitleTextView = (TextView) mView.findViewById(R.id.episode_title);
            episodeDateTextView = (TextView) mView.findViewById(R.id.episode_pubDate);
            episodeThumbView = (ImageView) mView.findViewById(R.id.thumb);
            episodeDurationTextView = (TextView) mView.findViewById(R.id.episode_duration);
            episodeInfoButton = (ImageButton) mView.findViewById(R.id.episode_info);

        }
    }

}
