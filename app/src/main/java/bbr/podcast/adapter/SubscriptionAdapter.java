package bbr.podcast.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import bbr.podcast.R;
import bbr.podcast.activity.PodcastEpsActivity;
import bbr.podcast.data.PodcastContract;

import static android.R.attr.thumbnail;

/**
 * Created by Me on 5/3/2017.
 */

public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.ViewHolder> {

    private static final String LOG_TAG = SubscriptionAdapter.class.getSimpleName();

    private Cursor cursor;
    private Context context;

    public SubscriptionAdapter (Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        Log.d(LOG_TAG, "position of cursor: " + position);

        int titleIndex = cursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_TITLE);
        String title = cursor.getString(titleIndex);
        holder.mTextView.setText(title);

        String loadImageUrl;

        int itunesImageColumnIndex = cursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_ITUNES_IMAGE);
        final String itunesImage = cursor.getString(itunesImageColumnIndex);
        loadImageUrl = itunesImage;
        if (itunesImage == null) {
            int thumbnailIndex = cursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_THUMBNAIL);
            final String thumbnail = cursor.getString(thumbnailIndex);
            loadImageUrl = thumbnail;
        }

        int feedIndex = cursor.getColumnIndex(PodcastContract.ChannelEntry.COLUMN_FEED_URL);
        final String feedUrl = cursor.getString(feedIndex);
        Log.d(LOG_TAG, "feedUrl " + feedUrl);

        Picasso.with(context).setLoggingEnabled(true);
        Picasso.with(context)
                .load(loadImageUrl)
                .fit()
                .into(holder.mImageView);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, PodcastEpsActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, "subscription")
                        .putExtra(context.getResources().getString(R.string.intent_pass_feedUrl),feedUrl)
                        .putExtra(context.getResources().getString(R.string.intent_pass_thumbnail), thumbnail);

                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (cursor == null) {
            return 0;
        }else {
            return cursor.getCount();
        }
    }

    public void swapCursor(Cursor newCursor) {
        cursor = newCursor;
        // After the new Cursor is set, call notifyDataSetChanged
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
