package bbr.podcast;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import bbr.podcast.data.PodcastContract;
import bbr.podcast.utils.PlaylistHelper;

/**
 * Created by Me on 5/15/2017.
 */

public class EpisodeBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String LOG_TAG = EpisodeBottomSheetFragment.class.getSimpleName();

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.fragment_bottomsheet_episode, null);

        final ImageButton addDownload = (ImageButton) contentView.findViewById(R.id.add_download);
        final ImageButton addFavorite = (ImageButton) contentView.findViewById(R.id.add_favorite);
        ImageButton addToPlaylist = (ImageButton) contentView.findViewById(R.id.add_to_playlist);

        LinearLayout actionContainer = (LinearLayout) contentView.findViewById(R.id.episode_actions_container);


        Bundle bundle = getArguments();
        String description = bundle.getString("Description");
        final String title = bundle.getString("EpisodeTitle");
        boolean subs = bundle.getBoolean("Subscribe");
        final String enclosure = bundle.getString("Enclosure");

        if (subs) {
            final Cursor cursor = PlaylistHelper.getPlaylistEpisodesCursor(title, getActivity());
            final List<String> playlistEpsNames = new ArrayList<String>();
            cursor.moveToFirst();
            int playlistNameColumnIndex = cursor.getColumnIndex(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME);
            for (int i = 0; i < cursor.getCount(); i++) {
                String playlistEpsName = cursor.getString(playlistNameColumnIndex);
                playlistEpsNames.add(playlistEpsName);
                Log.d(LOG_TAG, "playlistNames " + playlistEpsName);
            }
            final boolean isDled = isDownloaded(playlistEpsNames, addDownload);
            final boolean isFavs = isFavorite(playlistEpsNames, addFavorite);

            addDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isDled) {

                    } else {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(EpisodeBottomSheetFragment.this.getActivity());
                        ConnectivityManager cm =
                                (ConnectivityManager)EpisodeBottomSheetFragment.this.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        if(activeNetwork == null){
                            Toast.makeText(getActivity(), "no network", Toast.LENGTH_SHORT);
                            return;
                        }
                        boolean isConnected = activeNetwork != null &&
                                activeNetwork.isConnectedOrConnecting();
                        boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;

                        if(sharedPreferences.getBoolean(getString(R.string.pref_download_key), true) && isWiFi != true) {

                            Toast.makeText(getActivity(), "not on wifi", Toast.LENGTH_SHORT);
                        } else {
                            downloadEpisode(enclosure, title);
                        }
                    }
                }
            });

            addFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isFavs) {

                    } else {
                        addFavorite.getDrawable().setColorFilter(getActivity().getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
                        addFavoriteToDb(title);
                    }


                }
            });

            addToPlaylist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buildDialogForAddToPlaylist(title, playlistEpsNames);
                }
            });

        } else {
            actionContainer.setVisibility(View.INVISIBLE);
        }

        TextView descriptionTv = (TextView) contentView.findViewById(R.id.episode_description);
        descriptionTv.setText(description);

        TextView titleTv = (TextView) contentView.findViewById(R.id.episode_bottom_title);
        titleTv.setText(title);

        dialog.setContentView(contentView);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "EpisodeBottomSheetFragment onStop");
    }

    private void downloadEpisode(String enclosure, final String title) {
        Log.d(LOG_TAG, "download Episode" + title);
        URL url = null;
        try {
            url = new URL(enclosure);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        final String fileName = FilenameUtils.getName(url.getPath());

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(enclosure));
        request.setDescription(fileName);
        request.setTitle(title);

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        request.setDestinationInExternalFilesDir(getActivity(), Environment.DIRECTORY_DOWNLOADS, fileName);

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);

        // store fileName into the database
    }

    private void buildDialogForAddToPlaylist(final String title, final List<String> playlistEpsNames) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Light_Dialog);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }

        final String[] playlistNames = getPlaylistNames();
        builder.setTitle("Add to Playlist")
                .setItems(playlistNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if(!isInPlaylist(playlistEpsNames, playlistNames[which])) {
                            addToPlaylistToDb(title, playlistNames[which]);
                        }
                    }
                })
                /*.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })*/
                .setIcon(R.drawable.ic_playlist_add_black_24dp)
                .show();
    }

    private String[] getPlaylistNames() {
        Cursor cursor = getActivity().getContentResolver().query(PodcastContract.PlaylistEntry.CONTENT_URI,
                null,
                PodcastContract.PlaylistEntry._ID + " > ?",
                new String[]{String.valueOf(3)},
                null);

        cursor.moveToFirst();
        List<String> playlistNames = new ArrayList<>();
        for(int i = 0; i < cursor.getCount(); i++) {
            String name = cursor.getString(cursor.getColumnIndex(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME));
            playlistNames.add(name);
            cursor.moveToNext();
        }
        cursor.close();

        String [] names = new String[playlistNames.size()];
        playlistNames.toArray(names);
        return names;
    }

    private void addFavoriteToDb(String title) {
        ContentValues insertPlaylistEpisodeValues = new ContentValues();
        insertPlaylistEpisodeValues.put(PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME, title);
        insertPlaylistEpisodeValues.put(PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME, "favorite");

        getActivity().getContentResolver().insert(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI, insertPlaylistEpisodeValues);
    }

    private void addToPlaylistToDb(String title, String playlistName) {
        ContentValues insertPlaylistEpisodeValues = new ContentValues();
        insertPlaylistEpisodeValues.put(PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME, title);
        insertPlaylistEpisodeValues.put(PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME, playlistName);

        getActivity().getContentResolver().insert(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI, insertPlaylistEpisodeValues);
    }

    private boolean isInPlaylist(List<String> playlistNames, String name) {
        Log.d(LOG_TAG, "is " + name + playlistNames.contains(name));
        if(!playlistNames.contains(name)){
            return false;
        } else {
            return true;
        }
    }

    private boolean isDownloaded(List<String> playlistEpsNames, ImageButton addDownload) {
        Log.d(LOG_TAG, "is Downloaded " + playlistEpsNames.contains("downloaded"));
        if (!playlistEpsNames.contains("downloaded")) {
            addDownload.getDrawable().setColorFilter(getActivity().getResources().getColor(R.color.colorBlack), PorterDuff.Mode.SRC_ATOP);
            return false;
        } else {
            addDownload.getDrawable().setColorFilter(getActivity().getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            return true;
        }
    }

    private boolean isFavorite(List<String> playlistEpsNames, ImageButton addFavorite) {
        Log.d(LOG_TAG, "is Favorite " + playlistEpsNames.contains("favorite"));
        if (!playlistEpsNames.contains("favorite")) {
            addFavorite.getDrawable().setColorFilter(getActivity().getResources().getColor(R.color.colorBlack), PorterDuff.Mode.SRC_ATOP);
            return false;
        } else {
            addFavorite.getDrawable().setColorFilter(getActivity().getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            return true;
        }
    }


}
