package bbr.podcast.fragment;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import bbr.podcast.R;

/**
 * Created by Me on 5/31/2017.
 */

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_podcast);

    }
}
