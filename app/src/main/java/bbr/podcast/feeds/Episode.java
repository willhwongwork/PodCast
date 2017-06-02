package bbr.podcast.feeds;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Me on 4/18/2017.
 */

public class Episode implements Parcelable{
    public final String title;
    public final String link;
    public final String pubDate;
    public final String description;
    public final String enclosure;
    public final String duration;
    public final String explicit;
    public final String episodeImage;


    private static final String ns = null;

    public Episode(String title, String link, String pubDate, String description, String enclosure, String duration, String explicit, String episodeImage) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.description = description;
        this.enclosure = enclosure;
        this.duration = duration;
        this.explicit = explicit;
        this.episodeImage = episodeImage;
    }

    private Episode(Parcel in) {
        title = in.readString();
        link = in.readString();
        pubDate = in.readString();
        description = in.readString();
        enclosure = in.readString();
        duration = in.readString();
        explicit = in.readString();
        episodeImage = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(title);
        parcel.writeString(link);
        parcel.writeString(pubDate);
        parcel.writeString(description);
        parcel.writeString(enclosure);
        parcel.writeString(duration);
        parcel.writeString(explicit);
        parcel.writeString(episodeImage);
    }

    public static final Parcelable.Creator<Episode> CREATOR = new Parcelable.Creator<Episode>() {
        @Override
        public Episode createFromParcel(Parcel parcel) {
            return new Episode(parcel);
        }

        @Override
        public Episode[] newArray(int i) {
            return new Episode[i];
        }
    };
}
