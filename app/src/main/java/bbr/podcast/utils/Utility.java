package bbr.podcast.utils;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by Me on 10/19/2017.
 */

public class Utility {
    public static int calculateNoOfColumns(Context context, int columnWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / columnWidthDp);
        return noOfColumns;
    }
}
