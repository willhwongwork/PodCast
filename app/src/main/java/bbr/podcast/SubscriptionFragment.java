package bbr.podcast;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import bbr.podcast.data.PodcastContract;
import bbr.podcast.utils.SpacesItemDecoration;

/**
 * Created by Me on 5/2/2017.
 */

public class SubscriptionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_SUBSCRIPTION = "ARG_SUBSCRIPTION";

    private int mPage;
    private SubscriptionAdapter subscriptionAdapter;

    public static SubscriptionFragment newInstance(int tab) {
        Bundle args = new Bundle();
        args.putInt(ARG_SUBSCRIPTION, tab);
        SubscriptionFragment fragment = new SubscriptionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_SUBSCRIPTION);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_subscription, container, false);
        RecyclerView rvSubs = (RecyclerView) rootView.findViewById(R.id.subs_recycler_view);
        rvSubs.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        subscriptionAdapter = new SubscriptionAdapter(getActivity());
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
        rvSubs.addItemDecoration(new SpacesItemDecoration(2, spacingInPixels, true));
        rvSubs.setAdapter(subscriptionAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                PodcastContract.ChannelEntry.CONTENT_URI,
                null,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        subscriptionAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        subscriptionAdapter.swapCursor(null);
    }
}
