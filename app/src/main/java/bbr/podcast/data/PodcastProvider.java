package bbr.podcast.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Me on 4/30/2017.
 */

public class PodcastProvider extends ContentProvider {
    private static final String LOG_TAG = PodcastProvider.class.getSimpleName();

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private PodcastDbHelper mOpenHelper;

    private static final int CHANNEL = 100;
    private static final int CHANNEL_WITH_ID = 200;
    private static final int EPISODE = 300;
    private static final int EPISODE_WITH_ID = 400;
    private static final int PLAYLIST = 500;
    private static final int PLAYLIST_WITH_ID = 600;
    private static final int PLAYLIST_EPISODE = 700;
    private static final int PLAYLIST_EPISODE_WITH_ID = 800;
    private static final int EPISODE_WITH_PLAYLIST = 900;

    private static final SQLiteQueryBuilder sEpisodeWithPlaylistQueryBuilder;

    static {
        sEpisodeWithPlaylistQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join
        sEpisodeWithPlaylistQueryBuilder.setTables(
                PodcastContract.PlaylistEpisodeEntry.TABLE_NAME +
                " INNER JOIN " + PodcastContract.EpisodeEntry.TABLE_NAME + " ON " +
                PodcastContract.PlaylistEpisodeEntry.TABLE_NAME + "." + PodcastContract.PlaylistEpisodeEntry.COLUMN_EPISODE_NAME +
                " = " + PodcastContract.EpisodeEntry.TABLE_NAME + "." + PodcastContract.EpisodeEntry.COLUMN_TITLE +
                " INNER JOIN " + PodcastContract.PlaylistEntry.TABLE_NAME + " ON " +
                PodcastContract.PlaylistEpisodeEntry.TABLE_NAME + "." + PodcastContract.PlaylistEpisodeEntry.COLUMN_PLAYLIST_NAME +
                " = " + PodcastContract.PlaylistEntry.TABLE_NAME + "." + PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME);
    }

    static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PodcastContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, PodcastContract.ChannelEntry.TABLE_NAME, CHANNEL);
        matcher.addURI(authority, PodcastContract.ChannelEntry.TABLE_NAME + "/#", CHANNEL_WITH_ID);
        matcher.addURI(authority, PodcastContract.EpisodeEntry.TABLE_NAME, EPISODE);
        matcher.addURI(authority, PodcastContract.EpisodeEntry.TABLE_NAME + "/#", EPISODE_WITH_ID);
        matcher.addURI(authority, PodcastContract.PlaylistEntry.TABLE_NAME, PLAYLIST);
        matcher.addURI(authority, PodcastContract.PlaylistEntry.TABLE_NAME + "/#", PLAYLIST_WITH_ID);
        matcher.addURI(authority, PodcastContract.PlaylistEpisodeEntry.TABLE_NAME , PLAYLIST_EPISODE);
        matcher.addURI(authority, PodcastContract.PlaylistEpisodeEntry.TABLE_NAME + "/#", PLAYLIST_EPISODE_WITH_ID);
        matcher.addURI(authority, PodcastContract.PlaylistEpisodeEntry.TABLE_NAME + "/*", EPISODE_WITH_PLAYLIST);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new PodcastDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor retCursor;
        Log.d(LOG_TAG, "Querying data");
        switch (sUriMatcher.match(uri)) {
            // All channels selected
            case CHANNEL: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.ChannelEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            // Individual channel based on Id selected
            case CHANNEL_WITH_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.ChannelEntry.TABLE_NAME,
                        projection,
                        PodcastContract.ChannelEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))},
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            // All episodes selected
            case EPISODE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.EpisodeEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            // Individual episode based on Id selected
            case EPISODE_WITH_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.EpisodeEntry.TABLE_NAME,
                        projection,
                        PodcastContract.EpisodeEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))},
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            case PLAYLIST: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.PlaylistEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            case PLAYLIST_WITH_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.PlaylistEntry.TABLE_NAME,
                        projection,
                        PodcastContract.PlaylistEntry._ID + " =?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))},
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            case PLAYLIST_EPISODE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.PlaylistEpisodeEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            case PLAYLIST_EPISODE_WITH_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.PlaylistEpisodeEntry.TABLE_NAME,
                        projection,
                        PodcastContract.PlaylistEpisodeEntry._ID + " =?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))},
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            case EPISODE_WITH_PLAYLIST: {
                retCursor = sEpisodeWithPlaylistQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                retCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return retCursor;
            }
            default: {
                // By default, we assume a bad URI
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case CHANNEL: {
                return PodcastContract.ChannelEntry.CONTENT_DIR_TYPE;
            }
            case CHANNEL_WITH_ID: {
                return PodcastContract.ChannelEntry.CONTENT_ITEM_TYPE;
            }
            case EPISODE: {
                return PodcastContract.EpisodeEntry.CONTENT_DIR_TYPE;
            }
            case EPISODE_WITH_ID: {
                return PodcastContract.EpisodeEntry.CONTENT_ITEM_TYPE;
            }
            case PLAYLIST: {
                return PodcastContract.PlaylistEntry.CONTENT_DIR_TYPE;
            }
            case PLAYLIST_WITH_ID: {
                return PodcastContract.PlaylistEntry.CONTENT_ITEM_TYPE;
            }
            case PLAYLIST_EPISODE: {
                return PodcastContract.PlaylistEpisodeEntry.CONTENT_DIR_TYPE;
            }
            case PLAYLIST_EPISODE_WITH_ID: {
                return PodcastContract.PlaylistEpisodeEntry.CONTENT_ITEM_TYPE;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri returnUri;
        switch (sUriMatcher.match(uri)) {
            case CHANNEL: {
                long _id = db.insert(PodcastContract.ChannelEntry.TABLE_NAME, null, values);
                // insert unless it is already contained in the database
                if (_id > 0) {
                    returnUri = PodcastContract.ChannelEntry.buildChannelUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }
            case EPISODE: {
                long _id = db.insert(PodcastContract.EpisodeEntry.TABLE_NAME, null, values);
                // insert unless it is already contained in the database
                if (_id > 0) {
                    returnUri = PodcastContract.EpisodeEntry.buildEpisodeUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }
            case PLAYLIST: {
                long _id = db.insert(PodcastContract.PlaylistEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = PodcastContract.PlaylistEntry.buildEpisodeUri(_id);
                } else  {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }
            case PLAYLIST_EPISODE: {
                long _id = db.insert(PodcastContract.PlaylistEpisodeEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = PodcastContract.PlaylistEpisodeEntry.buildEpisodeUri(_id);
                } else  {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unkown uri: " + uri);
            }
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int numDeleted;
        switch (match) {
            case CHANNEL: {
                numDeleted = db.delete(
                        PodcastContract.ChannelEntry.TABLE_NAME, selection, selectionArgs);
                // reset _ID
                //db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                //        PodcastContract.ChannelEntry.TABLE_NAME + "'");
                break;
            }
            case CHANNEL_WITH_ID: {
                numDeleted = db.delete(PodcastContract.ChannelEntry.TABLE_NAME,
                        PodcastContract.ChannelEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                // reset _ID
                //db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                //        PodcastContract.ChannelEntry.TABLE_NAME + "'");
                break;
            }
            case EPISODE: {
                numDeleted = db.delete(
                        PodcastContract.EpisodeEntry.TABLE_NAME, selection, selectionArgs);
                // reset _ID
                //db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                //       PodcastContract.EpisodeEntry.TABLE_NAME + "'");
                break;
            }
            case EPISODE_WITH_ID: {
                numDeleted = db.delete(PodcastContract.EpisodeEntry.TABLE_NAME,
                        PodcastContract.EpisodeEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            }
            case PLAYLIST: {
                numDeleted = db.delete(
                        PodcastContract.PlaylistEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case PLAYLIST_WITH_ID: {
                numDeleted = db.delete(PodcastContract.PlaylistEntry.TABLE_NAME,
                        PodcastContract.PlaylistEntry._ID + " =?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});

                break;
            }
            case PLAYLIST_EPISODE: {
                numDeleted = db.delete(
                        PodcastContract.PlaylistEpisodeEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case PLAYLIST_EPISODE_WITH_ID: {
                numDeleted = db.delete(PodcastContract.PlaylistEpisodeEntry.TABLE_NAME,
                        PodcastContract.PlaylistEpisodeEntry._ID + " =?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
        if(numDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return numDeleted;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CHANNEL: {
                // allows for multiple transaction
                db.beginTransaction();

                // keep track of successful inserts
                int numInserted = 0;
                try {
                    for (ContentValues value : values) {
                        if (value == null) {
                            throw new IllegalArgumentException("Cannot have null content values");
                        }
                        long _id = -1;
                        try {
                            _id = db.insertOrThrow(PodcastContract.ChannelEntry.TABLE_NAME,
                                    null, value);
                        } catch (SQLiteConstraintException e) {
                            Log.w(LOG_TAG, "Attempting to insert " +
                                value.getAsString(
                                        PodcastContract.ChannelEntry.COLUMN_TITLE)
                                + " but value is already in database.");
                        }
                        if (_id != -1) {
                            numInserted++;
                        }
                    }
                    if (numInserted > 0) {
                        // If no errors, declare a successful transaction.
                        // database will not populate if this is not called
                        db.setTransactionSuccessful();
                    }
                } finally {
                    // all transactions occur at once
                    db.endTransaction();
                }
                if (numInserted > 0) {
                    // if there was successful insertion, notify the content resolver that there
                    // was a change
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return numInserted;
            }
            case EPISODE: {
                // allows for multiple transaction
                db.beginTransaction();

                // keep track of successful inserts
                int numInserted = 0;
                try {
                    for (ContentValues value : values) {
                        if (value == null) {
                            throw new IllegalArgumentException("Cannot have null content values");
                        }
                        long _id = -1;
                        try {
                            if(!doesEpisodeExist(value)){
                                _id = db.insertOrThrow(PodcastContract.EpisodeEntry.TABLE_NAME,
                                    null, value);
                            }
                        } catch (SQLiteConstraintException e) {
                            Log.w(LOG_TAG, "Attempting to insert " +
                                    value.getAsString(
                                            PodcastContract.EpisodeEntry.COLUMN_TITLE)
                                    + " but value is already in database.");
                        }
                        if (_id != -1) {
                            numInserted++;
                        }
                    }
                    if (numInserted > 0) {
                        // If no errors, declare a successful transaction.
                        // database will not populate if this is not called
                        db.setTransactionSuccessful();
                    }
                } finally {
                    // all transactions occur at once
                    db.endTransaction();
                }
                if (numInserted > 0) {
                    // if there was successful insertion, notify the content resolver that there
                    // was a change
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return numInserted;
            }
            case PLAYLIST_EPISODE: {
                // allows for multiple transaction
                db.beginTransaction();

                // keep track of successful inserts
                int numInserted = 0;
                try {
                    for (ContentValues value : values) {
                        if (value == null) {
                            throw new IllegalArgumentException("Cannot have null content values");
                        }
                        long _id = -1;
                        try {

                            _id = db.insertOrThrow(PodcastContract.PlaylistEpisodeEntry.TABLE_NAME,
                                        null, value);
                        } catch (SQLiteConstraintException e) {
                            Log.w(LOG_TAG, "Attempting to insert " +
                                    value.getAsString(
                                            PodcastContract.PlaylistEpisodeEntry.TABLE_NAME)
                                    + " but value is already in database.");
                        }
                        if (_id != -1) {
                            numInserted++;
                        }
                    }
                    if (numInserted > 0) {
                        // If no errors, declare a successful transaction.
                        // database will not populate if this is not called
                        db.setTransactionSuccessful();
                    }
                } finally {
                    // all transactions occur at once
                    db.endTransaction();
                }
                if (numInserted > 0) {
                    // if there was successful insertion, notify the content resolver that there
                    // was a change
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return numInserted;
            }
            default: {
                return  super.bulkInsert(uri, values);
            }
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int numUpdated = 0;

        if (values == null) {
            throw new IllegalArgumentException("Cannot have null content values");
        }

        switch (sUriMatcher.match(uri)) {
            case CHANNEL: {
                numUpdated = db.update(PodcastContract.ChannelEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            }
            case CHANNEL_WITH_ID: {
                numUpdated = db.update(PodcastContract.ChannelEntry.TABLE_NAME,
                        values,
                        PodcastContract.ChannelEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            }
            case EPISODE: {
                numUpdated = db.update(PodcastContract.EpisodeEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            }
            case EPISODE_WITH_ID: {
                numUpdated = db.update(PodcastContract.EpisodeEntry.TABLE_NAME,
                        values,
                        PodcastContract.EpisodeEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            }
            case PLAYLIST: {
                numUpdated = db.update(PodcastContract.PlaylistEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            }
            case PLAYLIST_WITH_ID: {
                numUpdated = db.update(PodcastContract.PlaylistEntry.TABLE_NAME,
                        values,
                        PodcastContract.PlaylistEntry._ID + " =?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            }
            case PLAYLIST_EPISODE: {
                numUpdated = db.update(PodcastContract.PlaylistEpisodeEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            }
            case PLAYLIST_EPISODE_WITH_ID: {
                numUpdated = db.update(PodcastContract.PlaylistEpisodeEntry.TABLE_NAME,
                        values,
                        PodcastContract.PlaylistEntry._ID + " =?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unkown uri: " + uri);
            }
        }
        if (numUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return numUpdated;
    }

    private boolean doesEpisodeExist(ContentValues values) {
        String title = values.getAsString(PodcastContract.EpisodeEntry.COLUMN_TITLE);
        Cursor retCursor = mOpenHelper.getReadableDatabase().query(
                PodcastContract.EpisodeEntry.TABLE_NAME,
                null,
                PodcastContract.EpisodeEntry.COLUMN_TITLE + " =?",
                new String[]{title},
                null,
                null,
                null);

        if(retCursor.moveToFirst()) {
            Log.d(LOG_TAG, "Episode" + title + " exist");
            retCursor.close();
            return true;
        } else {
            Log.d(LOG_TAG, "Episode" + title + " not exist");
            retCursor.close();
            return false;
        }


    }
}
