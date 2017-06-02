package bbr.podcast.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Me on 4/28/2017.
 */

public class PodcastContract {

    public static final String CONTENT_AUTHORITY = "bbr.podcast";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private PodcastContract() {}

    public static final class ChannelEntry implements BaseColumns {

        public static final String TABLE_NAME = "channel";

        public static final String COLUMN_TITLE = "title";

        public static final String COLUMN_DESCRIPTION = "description";

        public static final String COLUMN_LANGUAGE = "language";

        public static final String COLUMN_COPYRIGHT = "copyright";

        public static final String COLUMN_EDITOR = "managing_editor";

        public static final String COLUMN_ITUNES_IMAGE = "itunes_image";

        public static final String COLUMN_IMAGE_URL = "image_url";

        public static final String COLUMN_THUMBNAIL = "thumbnail";

        public static final String COLUMN_FEED_URL = "feed_url";

        // create content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_NAME).build();
        // create cursor of base type directory for multiple entries
        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + TABLE_NAME;
        // create cursor of base type item for single entry
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +"/" + CONTENT_AUTHORITY + "/" + TABLE_NAME;

        // for building URIs on insertion
        public static Uri buildChannelUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class EpisodeEntry implements BaseColumns {

        public static final String TABLE_NAME = "episode";

        public static final String COLUMN_TITLE = "title";

        public static final String COLUMN_LINK = "link";

        public static final String COLUMN_PUB_DATE = "pub_date";

        public static final String COLUMN_DESCRIPTION = "description";

        public static final String COLUMN_ENCLOSURE = "enclosure";

        public static final String COLUMN_DURATION = "duration";

        public static final String COLUMN_EXPLICIT = "explicit";

        public static final String COLUMN_EPISODE_IMAGE = "episode_image";

        public static final String COLUMN_AUDIO_URI = "audio_uri";

        public static final String COLUMN_CHANNEL_ID = "channel_id";

        // the sum of the bitwise numbers which represent the playlists that this episode belongs to
        //public static final String COLUMN_PLAYLISTS_SUM = "playlists_sum";

        public static final String COLUMN_LAST_PLAYBACK_SECONDS = "last_playback_seconds";


        // create content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_NAME).build();
        // create cursor of base type directory for multiple entries
        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + TABLE_NAME;
        // create cursor of base type item for single entry
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +"/" + CONTENT_AUTHORITY + "/" + TABLE_NAME;

        // for building URIs on insertion
        public static Uri buildEpisodeUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

    }

    public static final class PlaylistEntry implements BaseColumns {

        public static final String TABLE_NAME = "playlist";


        // the number representing the playlist, starts from 1, 2, 4, 8...
        // used for a bitwise AND operation to determine whether an episode belongs to a certain playlist.
        //public static final String COLUMN_BITWISE_NUMBER = "bitwise_num";

        public static final String COLUMN_PLAYLIST_NAME = "playlist_name";

        // create content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_NAME).build();
        // create cursor of base type directory for multiple entries
        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + TABLE_NAME;
        // create cursor of base type item for single entry
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +"/" + CONTENT_AUTHORITY + "/" + TABLE_NAME;

        // for building URIs on insertion
        public static Uri buildEpisodeUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class PlaylistEpisodeEntry implements BaseColumns {

        public static final String TABLE_NAME = "playlist_episode";

        public static final String COLUMN_PLAYLIST_NAME = "playlist_episode_name";

        public static final String COLUMN_EPISODE_NAME = "episode_name";

        // create content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_NAME).build();
        // create cursor of base type directory for multiple entries
        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + TABLE_NAME;
        // create cursor of base type item for single entry
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +"/" + CONTENT_AUTHORITY + "/" + TABLE_NAME;

        // for building URIs on insertion
        public static Uri buildEpisodeUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildEpisodeWithPlaylistUri() {
            return CONTENT_URI.buildUpon().appendPath("playlist_episode").build();
        }
    }
}
