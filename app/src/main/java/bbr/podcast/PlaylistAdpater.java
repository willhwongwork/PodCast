package bbr.podcast;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import bbr.podcast.data.PodcastContract;
import bbr.podcast.feeds.Episode;
import bbr.podcast.utils.PlaylistHelper;

/**
 * Created by Me on 5/14/2017.
 */

public class PlaylistAdpater extends RecyclerView.Adapter<PlaylistAdpater.ViewHolder> {
    private static final String LOG_TAG = PlaylistAdpater.class.getSimpleName();

    private Cursor playlistCursor;
    private Cursor episodesCursor;
    private Context context;


    public PlaylistAdpater(Context context) {
        this.context = context;
        //playlistCursor = getPlaylistCursor(context);
        //episodesCursor = getPlaylistEpisodesCursor(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        playlistCursor.moveToPosition(position);
        Log.d(LOG_TAG, "position of playlistCursor: " + position);
        int playlistNameColumnIndex = playlistCursor.getColumnIndex(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME);
        final String playlistName = playlistCursor.getString(playlistNameColumnIndex);

        //final List<String> episodesImages = getPlaylistEpisodesThumbs(playlistNum);
        final List<String> episodesImages = new ArrayList<String>();
        final List<Episode> episodes = new ArrayList<Episode>();
        getEpisodesAndThumbs(playlistName, episodesImages, episodes);

        if (episodesImages.size() > 0) {
            Log.d(LOG_TAG, " episodesImages size " + episodesImages.size());

            int[] indexes = getEpisodeImageIndexes(episodesImages);

            Picasso.with(context).load(episodesImages.get(indexes[0])).fit().into(holder.mThumbImageView);
            Picasso.with(context).load(episodesImages.get(indexes[1])).fit().into(holder.mThumb1ImageView);
            Picasso.with(context).load(episodesImages.get(indexes[2])).fit().into(holder.mThumb2ImageView);
            Picasso.with(context).load(episodesImages.get(indexes[3])).fit().into(holder.mThumb3ImageView);
        }

        holder.mTextView.setText(playlistName);


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PlaylistEpsActivity.class)
                        .putExtra(context.getResources().getString(R.string.intent_pass_playlistname), playlistName)
                        .putStringArrayListExtra(context.getResources().getString(R.string.intent_pass_thumbs_list), (ArrayList<String>)episodesImages)
                        .putParcelableArrayListExtra(context.getResources().getString(R.string.intent_pass_episodes_list), (ArrayList<Episode>) episodes);

                context.startActivity(intent);
            }
        });

        holder.mMoreVertImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playlistName.equals("downloaded") || playlistName.equals("favorite") || playlistName.equals("recent")) {
                    return;
                }

                PopupMenu popup = new PopupMenu(context, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.playlist_popup_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_delete:
                                deletePlaylist(playlistName);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
            }
        });

    }

    @Override
    public int getItemCount() {
        if (playlistCursor == null) {
            Log.d(LOG_TAG, "itemCount is 0");
            return 0;
        }else {
            return playlistCursor.getCount();
        }
    }

    void swapCursor(Cursor newCursor, int id) {
        Log.d(LOG_TAG, "swapCursor id " + id);
        switch (id) {
            case 0:
                Log.d(LOG_TAG, "swap playlistCursor");
                playlistCursor = newCursor;
                break;
            case 1:
                episodesCursor = newCursor;
            default:
                break;

        }

        // After the new Cursor is set, call notifyDataSetChanged
        notifyDataSetChanged();
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
            mTextView = (TextView) view.findViewById(R.id.playlist);
            mThumbImageView = (ImageView) view.findViewById(R.id.playlist_thumb);
            mThumb1ImageView = (ImageView) view.findViewById(R.id.playlist_thumb1);
            mThumb2ImageView = (ImageView) view.findViewById(R.id.playlist_thumb2);
            mThumb3ImageView = (ImageView) view.findViewById(R.id.playlist_thumb3);
            mMoreVertImageButton = (ImageButton) view.findViewById(R.id.more_playlist_actions);
        }
    }

    private void getEpisodesAndThumbs(String playlistName, List<String> episodesImages, List<Episode> episodes) {
        if(episodesCursor == null){return;};
        episodesCursor.moveToFirst();

        for (int i = 0; i < episodesCursor.getCount(); i++){
            String epsPlaylistName = PlaylistHelper.getEpsPlaylistName(episodesCursor);
            if(playlistName.equals("downloaded") == false) {
                if (PlaylistHelper.isPlaylist(epsPlaylistName, playlistName)) {
                    getEpsAndThumbs(episodesCursor, episodesImages, episodes);

                }
                episodesCursor.moveToNext();
            } else {
                if (PlaylistHelper.isDownloaded(epsPlaylistName, episodesCursor, context)) {
                    getEpsAndThumbs(episodesCursor, episodesImages, episodes);
                }
                episodesCursor.moveToNext();
            }
        }


    }

    private void getEpsAndThumbs(Cursor episodesCursor, List<String> episodesImages, List<Episode> episodes) {
        int episodeImageColumnIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_EPISODE_IMAGE);
        String episodeImage = episodesCursor.getString(episodeImageColumnIndex);
        episodesImages.add(episodeImage);

        int titleColumnIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_TITLE);
        String episodeTitle = episodesCursor.getString(titleColumnIndex);
        int linkColumnIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_LINK);
        String episodeLink = episodesCursor.getString(linkColumnIndex);
        int pubDateColumnIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_PUB_DATE);
        String episodePubDate = episodesCursor.getString(pubDateColumnIndex);
        int descriptionColumnIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION);
        String description = episodesCursor.getString(descriptionColumnIndex);
        int enclosureColumnIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_ENCLOSURE);
        String enclosure = episodesCursor.getString(enclosureColumnIndex);
        int durationColumnIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_DURATION);
        String duration = episodesCursor.getString(durationColumnIndex);
        int explicitColumnIndex = episodesCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_EXPLICIT);
        String explicit = episodesCursor.getString(explicitColumnIndex);

        episodes.add(new Episode(episodeTitle, episodeLink, episodePubDate, description, enclosure, duration, explicit, episodeImage));
    }


    private int[] getEpisodeImageIndexes(List<String> episodesImages) {
        int[] indexes = new int[4];
        int size = episodesImages.size();
        int multiple = size / 4;
        if (size >= 4) {
            for (int i = 0; i < 4; i++) {
                indexes[i] = i * multiple;
                Log.d(LOG_TAG, "index value " + indexes[i]);
            }
        }



        if (size == 1) {
            for (int i = 0; i < 4; i++) {
                indexes[i] = 0;
                Log.d(LOG_TAG, "index value " + indexes[i]);
            }
        }

        if (size == 2) {
            for (int i = 0; i < 4; i++) {
                if (i % 2 == 0) {
                    indexes[i] = 0;
                } else {
                    indexes[i] = 1;
                }
                Log.d(LOG_TAG, "index value " + indexes[i]);
            }
        }

        if (size == 3) {
            for (int i = 0; i < 4; i++) {
                if (i % 2 == 0) {
                    indexes[0] = 0;
                    indexes[2] = 2;
                } else {
                    indexes[i] = 1;
                }
                Log.d(LOG_TAG, "index value " + indexes[i]);
            }
        }

        return indexes;
    }

    private void deletePlaylist(final String playlistName) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        builder.setTitle("Delete playlist")
                .setMessage("Are you sure you want to delete this playlist?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        deletePlaylistFromDb(playlistName);
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

    private void deletePlaylistFromDb(String playlistName) {
        context.getContentResolver().delete(PodcastContract.PlaylistEntry.CONTENT_URI,
                PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME + "=?",
                new String[]{playlistName});
        context.getContentResolver().delete(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI,
                PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME + "=?",
                new String[]{playlistName});
    }
}
