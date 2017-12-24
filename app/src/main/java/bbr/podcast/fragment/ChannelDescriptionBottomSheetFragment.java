package bbr.podcast.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;
import android.widget.TextView;

import bbr.podcast.R;

/**
 * Created by Me on 6/1/2017.
 */

public class ChannelDescriptionBottomSheetFragment extends BottomSheetDialogFragment {

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        View contentView = View.inflate(getActivity(), R.layout.fragment_channel_description, null);

        TextView titleTextView = (TextView) contentView.findViewById(R.id.channel_title);
        TextView descriptionTextVIew = (TextView) contentView.findViewById(R.id.channel_description);

        Bundle bundle = getArguments();
        String title = bundle.getString(getResources().getString(R.string.intent_pass_channel_title));
        String description = bundle.getString(getResources().getString(R.string.intent_pass_channel_description));

        titleTextView.setText(title);
        descriptionTextVIew.setText(description);

        dialog.setContentView(contentView);
    }
}
