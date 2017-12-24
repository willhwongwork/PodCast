package bbr.podcast.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

import bbr.podcast.R;
import bbr.podcast.adapter.DiscoverAdapter;
import bbr.podcast.service.DownloadDiscoverThumbnailsService;
import bbr.podcast.utils.Constants;
import bbr.podcast.utils.SpacesItemDecoration;
import bbr.podcast.utils.Utility;

/**
 * Created by Me on 6/3/2017.
 */

public class DiscoverFragment extends Fragment {
    private static final String LOG_TAG = DiscoverFragment.class.getSimpleName();
    public static final String ARG_DISCOVER = "ARG_DISCOVER";

    private DiscoverThumbsReceiver discoverThumbsReceiver;
    private String discoverThumbsState;
    private ProgressBar progressBar;

    private List<String[]> thumbArtUrlsForAllGenres = new ArrayList<>();
    private List<String> genreNames = new ArrayList<>();
    private List<Integer> genreIds = new ArrayList<>();
    private DiscoverAdapter discoverAdapter;

    private AdView mAdView;

    public static DiscoverFragment newInstance(int tab) {
        Bundle args = new Bundle();
        args.putInt(ARG_DISCOVER, tab);
        DiscoverFragment discoverFragment = new DiscoverFragment();
        discoverFragment.setArguments(args);
        return discoverFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter intentFilter = new IntentFilter(Constants.DISCOVER_BROADCAST_ACTION);
        discoverThumbsReceiver = new DiscoverThumbsReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(discoverThumbsReceiver, intentFilter);
        if(genreNames.size() == 0){
            Intent intent = new Intent(getActivity(), DownloadDiscoverThumbnailsService.class);
            getActivity().startService(intent);
            Log.d(ARG_DISCOVER, "start discover service");
        }

    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        discoverAdapter = new DiscoverAdapter(thumbArtUrlsForAllGenres, genreNames, genreIds, context);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(discoverThumbsReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover, container, false);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar_discover);
        if(genreNames.size() == 0) {
            progressBar.setVisibility(View.VISIBLE);
        }
        RecyclerView discoverRv = (RecyclerView) rootView.findViewById(R.id.discover_recycler_view);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), Utility.calculateNoOfColumns(getContext(), 160));
        layoutManager.setSmoothScrollbarEnabled (true);

        discoverRv.setLayoutManager(layoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
        discoverRv.addItemDecoration(new SpacesItemDecoration(Utility.calculateNoOfColumns(getContext(), 160), spacingInPixels, true));
        discoverRv.setAdapter(discoverAdapter);

        mAdView = (AdView) rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice("6634CA7A33498E1A2B7DCBD2575C14F3")
                .build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.d(LOG_TAG, " ad failed to load " + errorCode);
                // Code to be executed when an ad request fails.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
            }
        });

        return rootView;
    }

    private class DiscoverThumbsReceiver extends BroadcastReceiver {
        // Prevents instantiation
        private DiscoverThumbsReceiver() {}

        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
            discoverThumbsState = intent.getStringExtra(Constants.DISCOVER_EXTENDED_DATA_STATUS);
            if(discoverThumbsState.equals("progress")) {
                genreIds.add(intent.getIntExtra(getString(R.string.intent_pass_discover_genre_id), -1));
                genreNames.add(intent.getStringExtra(getString(R.string.intent_pass_dicover_genre_name)));
                thumbArtUrlsForAllGenres.add(intent.getStringArrayExtra(getString(R.string.intent_pass_discover_thumbs_array)));
            }

            if(discoverThumbsState.equals("done")) {
                if(genreNames.size() == 16) {
                    progressBar.setVisibility(View.INVISIBLE);
                    //discoverAdapter.notifyItemRangeInserted(0, genreNames.size());
                    discoverAdapter.notifyDataSetChanged();

                }
            }
        }
    }

}
