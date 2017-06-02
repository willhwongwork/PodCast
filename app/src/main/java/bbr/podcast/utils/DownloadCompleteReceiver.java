package bbr.podcast.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Created by Me on 5/28/2017.
 */

public class DownloadCompleteReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = DownloadCompleteReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "receiving download complete");
        DownloadManager dMgr = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        Bundle extras = intent.getExtras();
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
        Cursor c = dMgr.query(q);

        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                // process download
                String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
                // get other required data by changing the constant passed to getColumnIndex
                String fileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
                PlaylistHelper.addDownloadToDb(fileName, title, context);
            }
        }

        c.close();
    }
}
