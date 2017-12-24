package bbr.podcast.feeds;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Me on 4/16/2017.
 */

public class ItunesPodcast implements Parcelable {
    public String feedUrl;
    public String collectionName;
    public String artworkUrl100;
    public String artworkUrl600;
    public String primaryGenreName;

    public ItunesPodcast(String feedUrl, String collectionName, String artworkUrl100, String artworkUrl600, String primaryGenreName) {
        this.feedUrl = feedUrl;
        this.collectionName = collectionName;
        this.artworkUrl100 = artworkUrl100;
        this.artworkUrl600 = artworkUrl600;
        this.primaryGenreName = primaryGenreName;
    }

    private ItunesPodcast(Parcel in) {
        feedUrl = in.readString();
        collectionName = in.readString();
        artworkUrl100 = in.readString();
        artworkUrl600 = in.readString();
        primaryGenreName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(feedUrl);
        parcel.writeString(collectionName);
        parcel.writeString(artworkUrl100);
        parcel.writeString(artworkUrl600);
        parcel.writeString(primaryGenreName);
    }

    public static final Parcelable.Creator<ItunesPodcast> CREATOR = new Parcelable.Creator<ItunesPodcast>() {
        @Override
        public ItunesPodcast createFromParcel(Parcel parcel) {
            return new ItunesPodcast(parcel);
        }

        @Override
        public ItunesPodcast[] newArray(int i) {
            return new ItunesPodcast[i];
        }
    };
}
