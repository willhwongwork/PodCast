package bbr.podcast.feeds;

/**
 * Created by Me on 4/16/2017.
 */

public class ItunesPodcast {
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


}
