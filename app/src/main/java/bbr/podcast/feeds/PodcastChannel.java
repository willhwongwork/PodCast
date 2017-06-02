package bbr.podcast.feeds;

import java.util.List;

/**
 * Created by Me on 4/17/2017.
 */

public class PodcastChannel {
    public final String title;
    public final String description;
    public final String language;
    public final String copyright;
    public final String managingEditor;
    public final String itunesImage;
    public final String imageUrl;
    public final List<Episode> episodes;

    public PodcastChannel(String title, String description, String language, String copyright, String managingEditor, String itunesImage, String imageUrl, List<Episode> episodes) {
        this.title = title;
        this.description = description;
        this.language = language;
        this.copyright = copyright;
        this.managingEditor = managingEditor;
        this.itunesImage = itunesImage;
        this.imageUrl = imageUrl;
        this.episodes = episodes;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }
}
