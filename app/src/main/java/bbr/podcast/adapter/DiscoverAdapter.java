package bbr.podcast.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import bbr.podcast.R;
import bbr.podcast.activity.GenreDetailActivity;

/**
 * Created by Me on 6/3/2017.
 */

public class DiscoverAdapter extends RecyclerView.Adapter<DiscoverAdapter.ViewHolder> {
    private List<String[]> thumbArtUrlsForAllGenres;
    private List<String> genreNames;
    private List<Integer> genreIds;
    private Context context;

    public DiscoverAdapter(List<String[]> thumbArtUrlsForAllGenres, List<String> genreNames, List<Integer> genreIds, Context context) {
        this.thumbArtUrlsForAllGenres = thumbArtUrlsForAllGenres;
        this.genreNames = genreNames;
        this.genreIds = genreIds;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discover, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String[] thumbArtUrls = thumbArtUrlsForAllGenres.get(position);
        if(thumbArtUrls == null) {return;}
        if(holder == null) {return;}

        final String genreName = genreNames.get(position);
        final int genreId = genreIds.get(position);

        holder.mTextView.setText(genreName);

        Picasso.with(context).load(thumbArtUrls[0]).fit().into(holder.mThumbImageView);
        Picasso.with(context).load(thumbArtUrls[1]).fit().into(holder.mThumb1ImageView);
        Picasso.with(context).load(thumbArtUrls[2]).fit().into(holder.mThumb2ImageView);
        Picasso.with(context).load(thumbArtUrls[3]).fit().into(holder.mThumb3ImageView);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, GenreDetailActivity.class)
                        .putExtra(context.getString(R.string.intent_pass_discover_genre_id), genreId)
                        .putExtra(context.getString(R.string.intent_pass_dicover_genre_name), genreName);

                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return genreNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTextView;
        public final ImageView mThumbImageView;
        public final ImageView mThumb1ImageView;
        public final ImageView mThumb2ImageView;
        public final ImageView mThumb3ImageView;
        public final ImageButton mMoreVertImageButton;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTextView = (TextView) view.findViewById(R.id.discover_name);
            mThumbImageView = (ImageView) view.findViewById(R.id.discover_thumb);
            mThumb1ImageView = (ImageView) view.findViewById(R.id.discover_thumb1);
            mThumb2ImageView = (ImageView) view.findViewById(R.id.discover_thumb2);
            mThumb3ImageView = (ImageView) view.findViewById(R.id.discover_thumb3);
            mMoreVertImageButton = (ImageButton) view.findViewById(R.id.more_discover_actions);
        }
    }

}
