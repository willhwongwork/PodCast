package bbr.podcast.core.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import bbr.podcast.core.R;

/**
 * Created by Me on 4/13/2017.
 */
/**
 * Provides access to preferences set by the user in the settings screen. A
 * private instance of this class must first be instantiated via
 * init() or otherwise every public method will throw an Exception
 * when called.
 */

public class UserPreferences {

    public static final String IMPORT_DIR = "import/";

    private static final String TAG = "UserPreferences";

    //User Interface
    public static final String PREF_THEME = "prefTheme";

    // Other
    public static final String PREF_DATA_FOLDER = "prefDataFolder";

    private static Context context;
    private static SharedPreferences prefs;

    /**
     * Sets up the UserPreferences class.
     *
     * @throws IllegalArgumentException if context is null
     */
    public static void init(@NonNull Context context) {
        Log.d(TAG, "Creating new instance of UserPreferences");

        UserPreferences.context = context.getApplicationContext();
        UserPreferences.prefs = PreferenceManager.getDefaultSharedPreferences(context);

        createImportDirectory();
        createNoMediaFile();
    }

    public static int getTheme() {
        return readThemeValue(prefs.getString(PREF_THEME, "0"));
    }

    public static int getNoTitleTheme() {
        int theme = getTheme();
        if (theme == R.style.Theme_Podcast_Dark) {
            return R.style.Theme_Podcast_Dark_NoTitle;
        } else {
            return R.style.Theme_Podcast_Light_NoTitle;
        }
    }

    private static int readThemeValue(String valueFromPrefs) {
        switch (Integer.parseInt(valueFromPrefs)) {
            case 0:
                return R.style.Theme_Podcast_Light;
            case 1:
                return R.style.Theme_Podcast_Dark;
            default:
                return R.style.Theme_Podcast_Light;
        }
    }

    /**
     * Return the folder where the app stores all of its data. This method will
     * return the standard data folder if none has been set by the user.
     *
     * @param type The name of the folder inside the data folder. May be null
     *             when accessing the root of the data folder.
     * @return The data folder that has been requested or null if the folder
     * could not be created.
     */
    public static File getDataFolder(String type) {
        String strDir = prefs.getString(PREF_DATA_FOLDER, null);
        if (strDir == null) {
            Log.d(TAG, "Using default data folder");
            return context.getExternalFilesDir(type);
        } else {
            File dataDir = new File(strDir);
            if (!dataDir.exists()) {
                if (!dataDir.mkdir()) {
                    Log.w(TAG, "Could not create data folder");
                    return null;
                }
            }

            if (type == null) {
                return dataDir;
            } else {
                // handle path separators
                String[] dirs = type.split("/");
                for (int i = 0; i < dirs.length; i++) {
                    if (dirs.length > 0) {
                        if (i < dirs.length - 1) {
                            dataDir = getDataFolder(dirs[i]);
                            if (dataDir == null) {
                                return null;
                            }
                        }
                        type = dirs[i];
                    }
                }
                File typeDir = new File(dataDir, type);
                if (!typeDir.exists()) {
                    if (dataDir.canWrite()) {
                        if (!typeDir.mkdir()) {
                            Log.e(TAG, "Could not create data folder named " + type);
                            return null;
                        }
                    }
                }
                return typeDir;
            }
        }
    }

    /**
     * Create a .nomedia file to prevent scanning by the media scanner.
     */
    private static void createNoMediaFile() {
        File f = new File(context.getExternalFilesDir(null), ".nomedia");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Could not create .nomedia file");
                e.printStackTrace();
            }
            Log.d(TAG, ".nomedia file created");
        }
    }


    /**
     * Creates the import directory if it doesn't exist and if storage is
     * available
     */
    private static void createImportDirectory() {
        File importDir = getDataFolder(IMPORT_DIR);
        if (importDir != null) {
            if (importDir.exists()) {
                Log.d(TAG, "Import directory already exists");
            } else {
                Log.d(TAG, "Creating import directory");
                importDir.mkdir();
            }
        } else {
            Log.d(TAG, "Could not access external storage.");
        }
    }
}
