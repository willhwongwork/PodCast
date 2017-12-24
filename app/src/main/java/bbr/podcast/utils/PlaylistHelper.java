package bbr.podcast.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import bbr.podcast.data.PodcastContract;

/**
 * Created by Me on 5/17/2017.
 */

public class PlaylistHelper {
    private static final String LOG_TAG = PlaylistHelper.class.getSimpleName();

    public static void addRecentToDb(String title, Context context) {
        Log.d(LOG_TAG, "addRecentToDb");
        ContentValues insertPlaylistEpisodeValues = new ContentValues();
        insertPlaylistEpisodeValues.put(PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME, title);
        insertPlaylistEpisodeValues.put(PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME, "recent");

        context.getContentResolver().insert(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI, insertPlaylistEpisodeValues);

    }

    public static void addDownloadToDb(String fileName, String title, Context context) {
        Log.d(LOG_TAG, "addDownloadToDb " + fileName);
        ContentValues mUpdateValues = new ContentValues();
        mUpdateValues.put(PodcastContract.EpisodeEntry.COLUMN_AUDIO_URI, fileName);

        context.getContentResolver().update(PodcastContract.EpisodeEntry.CONTENT_URI,
                mUpdateValues,
                PodcastContract.EpisodeEntry.COLUMN_TITLE + " = ?",
                new String[]{title});

        ContentValues insertPlaylistEpisodeValues = new ContentValues();
        insertPlaylistEpisodeValues.put(PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME, title);
        insertPlaylistEpisodeValues.put(PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME, "downloaded");

        context.getContentResolver().insert(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI, insertPlaylistEpisodeValues);
    }

    public static void deleteFailedDownloadFromDb(String fileName, String title, Context context) {
        Log.d(LOG_TAG, "deleteFailedDownloadFromDb " + fileName);
        Cursor cursor = context.getContentResolver().query(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI,
                null,null,null,null);
        if(cursor.moveToFirst()){
            for (int i = 0; i < cursor.getCount(); i++) {
                String episodeName = cursor.getString(cursor.getColumnIndex(PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME));
                String playlistEpsName = cursor.getString(cursor.getColumnIndex(PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME));
                Log.d(LOG_TAG, "title: " + episodeName + " playlistName: " + playlistEpsName);
            }
        }

        context.getContentResolver().delete(PodcastContract.PlaylistEpisodeEntry.CONTENT_URI,
                PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME + "=? " + " AND " +
                        PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME + " =?",
                new String[]{title, "downloaded"});

        cursor.close();
    }

    public static Cursor getEpisodeIdCursor(String title, Context context) {
        Log.d(LOG_TAG, "getNameCursor");
        return context.getContentResolver().query(PodcastContract.EpisodeEntry.CONTENT_URI,
                new String[]{PodcastContract.EpisodeEntry._ID},
                PodcastContract.EpisodeEntry.COLUMN_TITLE + "=?",
                new String[]{title},
                null);
    }

    public static Cursor getPlaylistEpisodesCursor(String title, Context context) {
        Log.d(LOG_TAG, "getPlaylistEpisodesCursor");
        Uri episodesWithPlaylistUri = PodcastContract.PlaylistEpisodeEntry.buildEpisodeWithPlaylistUri();
        return context.getContentResolver().query(episodesWithPlaylistUri,
                null,
                PodcastContract.EpisodeEntry.COLUMN_TITLE + "=?",
                new String[]{title},
                null);
    }


    public static String getEpsPlaylistName(Cursor cursor) {
        int epsPlaylistNameColumnIndex = cursor.getColumnIndex(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME);
        String epsPlaylistName = cursor.getString(epsPlaylistNameColumnIndex);
        return epsPlaylistName;
    }

    public static boolean isRecent(String epsPlaylistName) {
        Log.d(LOG_TAG, "is Recent " + epsPlaylistName.equals("recent"));
        if (!epsPlaylistName.equals("recent")) {
            return false;
        } else {
            return true;
        }
    }


    public static boolean isPlaylist(String epsPlaylistName, String playlistName) {
        Log.d(LOG_TAG, "is playlist " + epsPlaylistName.equals(playlistName));
        return epsPlaylistName.equals(playlistName);
    }


    public static boolean isDownloaded(String epsPlaylistName, Cursor cursor, Context context) {
        Log.d(LOG_TAG, "is Downloaded " + epsPlaylistName.equals("downloaded"));
        if(!epsPlaylistName.equals("downloaded")) {
            return false;
        }
        int fileNameColumnIndex = cursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_AUDIO_URI);
        String fileName = cursor.getString(fileNameColumnIndex);
        int titleColumnIndex = cursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_TITLE);
        String title = cursor.getString(titleColumnIndex);
        File file;
        try{
            file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        } catch (NullPointerException e) {
            return false;
        }
        if (file.exists()) {
            Log.d(LOG_TAG, " downloaded file " + fileName + " exists");
            return true;
        } else {
            deleteFailedDownloadFromDb(fileName, title, context);
            return false;
        }
    }

}
