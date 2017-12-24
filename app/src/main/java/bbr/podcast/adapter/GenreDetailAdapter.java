package bbr.podcast.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import bbr.podcast.R;
import bbr.podcast.activity.PodcastEpsActivity;
import bbr.podcast.feeds.ItunesPodcast;

/**
 * Created by Me on 6/4/2017.
 */

public class GenreDetailAdapter extends RecyclerView.Adapter<GenreDetailAdapter.ViewHolder> {
    private static final String LOG_TAG = GenreDetailAdapter.class.getSimpleName();

    private List<ItunesPodcast> itunesPodcasts;
    private Context context;

    public GenreDetailAdapter(List<ItunesPodcast> itunesPodcasts, Context context) {
        this.itunesPodcasts = itunesPodcasts;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ItunesPodcast podcast = itunesPodcasts.get(position);
        holder.mTextView.setText(podcast.collectionName);
        Log.d(LOG_TAG, "genre collectionName " + podcast.collectionName);

        Picasso.with(context).setLoggingEnabled(true);
        Picasso.with(context)
                .load(podcast.artworkUrl600)
                .fit()
                .into(holder.mImageView);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context c = v.getContext();
                Intent intent = new Intent(context, PodcastEpsActivity.class)
                        .putExtra(c.getResources().getString(R.string.intent_pass_thumbnail), podcast.artworkUrl600)
                        .putExtra(c.getResources().getString(R.string.intent_pass_feedUrl), podcast.feedUrl);

                Log.d(LOG_TAG, "debuging clicks");
                c.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        Log.d(LOG_TAG, "genre item count " + itunesPodcasts.size());

        return itunesPodcasts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mImageView;
        public final TextView mTextView;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.item_search_imageView);
            mTextView = (TextView) view.findViewById(R.id.item_search_textView);
        }
    }
}
