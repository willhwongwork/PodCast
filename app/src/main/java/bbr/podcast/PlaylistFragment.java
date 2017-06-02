package bbr.podcast;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import bbr.podcast.data.PodcastContract;
import bbr.podcast.utils.SpacesItemDecoration;

/**
 * Created by Me on 5/2/2017.
 */

public class PlaylistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = PlaylistFragment.class.getSimpleName();
    public static final String ARG_PLAYLIST = "ARG_PLAYLIST";

    private PlaylistAdpater playlistAdpater;

    public static PlaylistFragment newInstance(int tab) {
        Bundle args = new Bundle();
        args.putInt(ARG_PLAYLIST, tab);
        PlaylistFragment fragment = new PlaylistFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().initLoader(1, null, this);

        //IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        //LocalBroadcastManager.getInstance(getActivity()).registerReceiver(onDownloadComplete, intentFilter);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(onDownloadComplete);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlists, container, false);
        RecyclerView rvPlaylists = (RecyclerView) rootView.findViewById(R.id.playlist_recycler_view);
        rvPlaylists.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
        rvPlaylists.addItemDecoration(new SpacesItemDecoration(2, spacingInPixels, true));
        playlistAdpater = new PlaylistAdpater(getActivity());
        rvPlaylists.setAdapter(playlistAdpater);


        return rootView;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader;
        switch (id) {
            case 0:
                loader =  new CursorLoader(getActivity(),
                        PodcastContract.PlaylistEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null);
                break;
            case 1:
                Uri episodesWithPlaylistUri = PodcastContract.PlaylistEpisodeEntry.buildEpisodeWithPlaylistUri();
                loader = new CursorLoader(getActivity(),
                        episodesWithPlaylistUri,
                        null,
                        null,
                        null,
                        PodcastContract.PlaylistEpisodeEntry._ID + " DESC");
                break;
            default:
                loader = null;
                break;

        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "onLoadFinished id " + loader.getId());
        playlistAdpater.swapCursor(data, loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        playlistAdpater.swapCursor(null, loader.getId());
    }
}
