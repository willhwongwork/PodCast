package bbr.podcast;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by Me on 5/29/2017.
 */

public class MyApplication extends Application {
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
}