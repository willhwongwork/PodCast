package bbr.podcast.utils;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import bbr.podcast.R;
import bbr.podcast.fragment.DiscoverFragment;
import bbr.podcast.fragment.PlaylistFragment;
import bbr.podcast.fragment.SubscriptionFragment;

/**
 * Created by Me on 5/2/2017.
 */

public class MainFragmentPagerAdapter extends FragmentPagerAdapter {
    final int PAGE_COUNT = 3;
    private String[] tabTitles ;
    private Context context;

    public MainFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        tabTitles = this.context.getResources().getStringArray(R.array.tab_title);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return SubscriptionFragment.newInstance(position + 1);
            case 1:
                return PlaylistFragment.newInstance(position + 1);
            case 2:
                return DiscoverFragment.newInstance(position + 1);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

/*    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }*/
}
