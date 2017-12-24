package bbr.podcast.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import bbr.podcast.R;
import bbr.podcast.adapter.GenreDetailAdapter;
import bbr.podcast.feeds.ItunesPodcast;
import bbr.podcast.service.GenreDownloadChannelsService;
import bbr.podcast.utils.Constants;
import bbr.podcast.utils.SpacesItemDecoration;
import bbr.podcast.utils.Utility;

/**
 * Created by Me on 6/4/2017.
 */

public class GenreDetailActivity extends BaseActivity {
    private static final String LOG_TAG = GenreDetailActivity.class.getSimpleName();

    private List<ItunesPodcast> itunesPodcasts = new ArrayList<ItunesPodcast>();
    private GenreDetailAdapter genreDetailAdapter = new GenreDetailAdapter(itunesPodcasts, this);
    private GenreDetailReceiver genreDetailReceiver;

    private ProgressDialog pd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_detail);

        RecyclerView genreRv = (RecyclerView) findViewById(R.id.genre_recycler_view);

        GridLayoutManager layoutManager = new GridLayoutManager(this, Utility.calculateNoOfColumns(this, 160));
        layoutManager.setSmoothScrollbarEnabled (true);

        genreRv.setLayoutManager(layoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
        genreRv.addItemDecoration(new SpacesItemDecoration(Utility.calculateNoOfColumns(this, 160), spacingInPixels, true));
        genreRv.setAdapter(genreDetailAdapter);

        int genreId = getIntent().getIntExtra(getString(R.string.intent_pass_discover_genre_id), -1);

        IntentFilter intentFilter = new IntentFilter(Constants.GENRE_BROADCAST_ACTION);
        genreDetailReceiver = new GenreDetailReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(genreDetailReceiver, intentFilter);

        Intent intent = new Intent(this, GenreDownloadChannelsService.class);
        intent.putExtra(getString(R.string.intent_pass_discover_genre_id), genreId);
        startService(intent);

        showProgressDialog();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(genreDetailReceiver);
    }

    private void showProgressDialog(){
        pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.please_wait));
        pd.setIndeterminate(false);
        pd.setCancelable(false);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();
    }


    private class GenreDetailReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int curSize = genreDetailAdapter.getItemCount();
            ArrayList<ItunesPodcast> result = intent.getParcelableArrayListExtra(getString(R.string.intent_pass_itunes_podcast));
            itunesPodcasts.addAll(result);
            Log.d(LOG_TAG, "genre result size " + result.size() +  " curSize " + curSize + " itunesPodcasts size " + itunesPodcasts.size());
            pd.dismiss();
            genreDetailAdapter.notifyItemRangeInserted(curSize, itunesPodcasts.size());
        }
    }
}
