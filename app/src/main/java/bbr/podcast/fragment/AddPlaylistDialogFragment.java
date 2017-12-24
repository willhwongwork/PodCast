package bbr.podcast.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import bbr.podcast.R;
import bbr.podcast.data.PodcastContract;

/**
 * Created by Me on 5/27/2017.
 */

public class AddPlaylistDialogFragment extends DialogFragment {
    private static final String LOG_TAG = AddPlaylistDialogFragment.class.getSimpleName();

    /* The activity that creates an instance of this dialog fragment must
    * implement this interface in order to receive event callbacks.
    * Each method passes the DialogFragment in case the host needs to query it. */
    public interface AddPlaylistDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    AddPlaylistDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the Listener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (AddPlaylistDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement AddPlaylistDialogListener");
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        final View addplaylistView = inflater.inflate(R.layout.fragment_dialog_addplaylist, null);
        final EditText editText = (EditText) addplaylistView.findViewById(R.id.add_playlist);

        builder.setView(addplaylistView)
                .setPositiveButton(R.string.add_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String playlistName = editText.getText().toString();
                        if (playlistName != null) {
                            insertNewPlaylist(playlistName);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void insertNewPlaylist(String playlistName) {
        ContentValues values = new ContentValues();
        values.put(PodcastContract.PlaylistEntry.COLUMN_PLAYLIST_NAME, playlistName);
        getActivity().getContentResolver().insert(PodcastContract.PlaylistEntry.CONTENT_URI, values);
    }
}
