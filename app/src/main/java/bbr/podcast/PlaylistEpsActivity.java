package bbr.podcast;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import bbr.podcast.data.PodcastContract;
import bbr.podcast.feeds.Episode;

/**
 * Created by Me on 5/19/2017.
 */

public class PlaylistEpsActivity extends BaseActivity {
    private static final String LOG_TAG = PlaylistEpsActivity.class.getSimpleName();

    private PlaylistEpsAdapter playlistEpsAdpater;
    private String playlistName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_eps);
        Toolbar podcastToobar = (Toolbar) findViewById(R.id.playlist_toobar);
        setSupportActionBar(podcastToobar);

        playlistName = getIntent().getStringExtra(getResources().getString(R.string.intent_pass_playlistname));
        List<String> episodesImages = getIntent().getStringArrayListExtra(getResources().getString(R.string.intent_pass_thumbs_list));
        final List<Episode> episodes = getIntent().getParcelableArrayListExtra(getResources().getString(R.string.intent_pass_episodes_list));

        ImageView epsImage = (ImageView) findViewById(R.id.playlist_eps_image);
        ImageView epsImage1 = (ImageView) findViewById(R.id.playlist_eps1_image);
        ImageView epsImage2 = (ImageView) findViewById(R.id.playlist_eps2_image);
        ImageView epsImage3 = (ImageView) findViewById(R.id.playlist_eps3_image);

        if (episodesImages.size() > 0) {
            Log.d(LOG_TAG, " episodesImages size " + episodesImages.size());

            int[] indexes = getEpisodeImageIndexes(episodesImages);

            Picasso.with(this).load(episodesImages.get(indexes[0])).fit().into(epsImage);
            Picasso.with(this).load(episodesImages.get(indexes[1])).fit().into(epsImage1);
            Picasso.with(this).load(episodesImages.get(indexes[2])).fit().into(epsImage2);
            Picasso.with(this).load(episodesImages.get(indexes[3])).fit().into(epsImage3);
        }

        RecyclerView playlistEpsRv = (RecyclerView) findViewById(R.id.playlist_episode_recycler_view);
        playlistEpsRv.setLayoutManager(new LinearLayoutManager(playlistEpsRv.getContext()));
        playlistEpsAdpater = new PlaylistEpsAdapter(this, episodes);
        playlistEpsRv.setAdapter(playlistEpsAdpater);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String title = episodes.get(position).title;

                episodes.remove(position);
                playlistEpsAdpater.swapEpisodes(episodes);

                getContentResolver().delete(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI,
                        PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME + " = ?" + " AND " +
                                PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME + " = ?",
                        new String[]{playlistName, title});

                if(playlistName.equals("downloaded")) {
                    deleteDownload(title);
                }
            }
        }).attachToRecyclerView(playlistEpsRv);
    }

    private void deleteDownload(String title) {
        Cursor episodeCursor = getContentResolver().query(PodcastContract.EpisodeEntry.CONTENT_URI,
                new String[]{PodcastContract.EpisodeEntry.COLUMN_AUDIO_URI},
                PodcastContract.EpisodeEntry.COLUMN_TITLE + " = ?",
                new String[]{title},
                null);
        episodeCursor.moveToFirst();

        String fileName = episodeCursor.getString(episodeCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_AUDIO_URI));
        File file;
        try {
            file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
            file.delete();
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, e.toString());
        }
        episodeCursor.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }

        return super.onOptionsItemSelected(item);
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
}
