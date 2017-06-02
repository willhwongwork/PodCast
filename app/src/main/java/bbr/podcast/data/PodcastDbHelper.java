package bbr.podcast.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Me on 4/29/2017.
 */

public class PodcastDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "podcast.db";

    public PodcastDbHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_CHANNEL_TABLE = "CREATE TABLE " + PodcastContract.ChannelEntry.TABLE_NAME + " (" +
                PodcastContract.ChannelEntry._ID + " INTEGER PRIMARY KEY, " +
                PodcastContract.ChannelEntry.COLUMN_TITLE + " TEXT, " +
                PodcastContract.ChannelEntry.COLUMN_DESCRIPTION + " TEXT, " +
                PodcastContract.ChannelEntry.COLUMN_LANGUAGE + " TEXT, " +
                PodcastContract.ChannelEntry.COLUMN_COPYRIGHT + " TEXT, " +
                PodcastContract.ChannelEntry.COLUMN_EDITOR + " TEXT, " +
                PodcastContract.ChannelEntry.COLUMN_ITUNES_IMAGE + " TEXT, " +
                PodcastContract.ChannelEntry.COLUMN_IMAGE_URL + " TEXT, " +
                PodcastContract.ChannelEntry.COLUMN_THUMBNAIL + " TEXT, " +
                PodcastContract.ChannelEntry.COLUMN_FEED_URL + " TEXT " +
                " );";

        final String SQL_CREATE_EPISODE_TABLE = "CREATE TABLE " + PodcastContract.EpisodeEntry.TABLE_NAME + " (" +
                PodcastContract.EpisodeEntry._ID + " INTEGER PRIMARY KEY, " +
                PodcastContract.EpisodeEntry.COLUMN_TITLE + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_LINK + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_PUB_DATE + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_ENCLOSURE + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_DURATION + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_EXPLICIT + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_EPISODE_IMAGE + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_AUDIO_URI + " TEXT, " +
                PodcastContract.EpisodeEntry.COLUMN_CHANNEL_ID + " INTEGER, " +
                PodcastContract.EpisodeEntry.COLUMN_LAST_PLAYBACK_SECONDS + " INTEGER, " +

                " FOREIGN KEY (" + PodcastContract.EpisodeEntry.COLUMN_CHANNEL_ID + ") REFERENCES " +
                PodcastContract.ChannelEntry.TABLE_NAME + " (" + PodcastContract.ChannelEntry._ID + ") );";

        final  String SQL_CREATE_PLAYLIST_TABLE = "CREATE TABLE " + PodcastContract.PlaylistEntry.TABLE_NAME + " (" +
                PodcastContract.PlaylistEntry._ID + " INTEGER PRIMARY KEY, " +
                //PodcastContract.PlaylistEntry.COLUMN_BITWISE_NUMBER + " INTEGER, " +
                PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME + " TEXT " +
                ");";

        final String SQL_CREATE_PLAYLIST_EPISODE_TABLE = "CREATE TABLE " + PodcastContract.PlaylistEpisodeEntry.TABLE_NAME + " (" +
                PodcastContract.PlaylistEpisodeEntry._ID + " INTEGER PRIMARY KEY, " +
                PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME + " TEXT, " +
                PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME + " TEXT, " +
                "FOREIGN KEY (" + PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME + ") REFERENCES " +
                PodcastContract.EpisodeEntry.TABLE_NAME + " (" + PodcastContract.EpisodeEntry.COLUMN_TITLE + "), " +
                "FOREIGN KEY (" + PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME + ") REFERENCES " +
                PodcastContract.PlaylistEntry.TABLE_NAME + " (" + PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME + ") );";

        db.execSQL(SQL_CREATE_CHANNEL_TABLE);
        db.execSQL(SQL_CREATE_EPISODE_TABLE);
        db.execSQL(SQL_CREATE_PLAYLIST_TABLE);
        db.execSQL(SQL_CREATE_PLAYLIST_EPISODE_TABLE);

        ContentValues donwloadedValues = new ContentValues();
        //donwloadedValues.put(PodcastContract.PlaylistEntry.COLUMN_BITWISE_NUMBER, 1);
        donwloadedValues.put(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME, "downloaded");
        db.insert(PodcastContract.PlaylistEntry.TABLE_NAME, null, donwloadedValues);

        ContentValues favoriteValues = new ContentValues();
        //favoriteValues.put(PodcastContract.PlaylistEntry.COLUMN_BITWISE_NUMBER, 2);
        favoriteValues.put(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME, "favorite");
        db.insert(PodcastContract.PlaylistEntry.TABLE_NAME, null, favoriteValues);

        ContentValues recentValues = new ContentValues();
        //recentValues.put(PodcastContract.PlaylistEntry.COLUMN_BITWISE_NUMBER, 4);
        recentValues.put(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME, "recent");
        db.insert(PodcastContract.PlaylistEntry.TABLE_NAME, null, recentValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PodcastContract.ChannelEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PodcastContract.EpisodeEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PodcastContract.PlaylistEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PodcastContract.PlaylistEpisodeEntry.TABLE_NAME);
        onCreate(db);
    }
}
