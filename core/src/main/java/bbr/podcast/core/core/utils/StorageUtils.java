package bbr.podcast.core.core.utils;

import android.app.Activity;
import android.util.Log;

import java.io.File;

import bbr.podcast.core.core.ClientConfig;
import bbr.podcast.core.core.preferences.UserPreferences;

/**
 * Created by Me on 4/13/2017. Utility functions for handling storage errors
 */

public class StorageUtils {
    private static final String TAG = "StorageUtils";

    public static boolean storageAvailable() {
        File dir = UserPreferences.getDataFolder(null);
        if (dir != null) {
            return dir.exists() && dir.canRead() && dir.canWrite();
        } else {
            Log.d(TAG, "Storage not available: data folder is null");
            return false;
        }
    }

    /**
     * Checks if external storage is available. If external storage isn't
     * available, the current activity is finsished and an error activity is
     * launched.
     *
     * @param activity the activity which would be finished if no storage is
     *                 available
     * @return true if external storage is available
     */
    public static boolean checkStorageAvailability(Activity activity) {
        boolean storageAvailable = storageAvailable();
        if (!storageAvailable) {
            activity.finish();
            activity.startActivity(ClientConfig.applicationCallbacks.getStorageErrorActivity(activity));
        }
        return storageAvailable;
    }

}
