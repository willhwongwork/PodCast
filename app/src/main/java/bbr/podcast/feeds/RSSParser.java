package bbr.podcast.feeds;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bbr.podcast.PodcastEpsActivity;

/**
 * Created by Me on 4/19/2017.
 */

public class RSSParser {

    private static final String ns = null;

    protected static PodcastChannel readFeed(XmlPullParser parser, PodcastEpsActivity.DownloadXmlTask task) throws XmlPullParserException, IOException {
        List episodes = new ArrayList();
        PodcastChannel channel = null;

        parser.require(XmlPullParser.START_TAG, ns, "rss");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String prefix = parser.getPrefix();
            String name = parser.getName();
            Log.d("XML", name);
            if (name.equals("channel")) {
                channel = RSSChannelParser.readChannel(parser, task);
/*                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String nameNum2 = parser.getName();
                    Log.d("XML2", nameNum2);
                    if (nameNum2.equals("item")) {
                        episodes.add(readEpisode(parser));
                    } else {
                        skip(parser);
                    }
                }*/
                Log.d("XMLSizeOfEpisodes", Integer.toString(episodes.size()));
                return channel;
            }
        }
        return null;
    }

    protected static Episode readEpisode(XmlPullParser parser, String channelImage) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");
        String title = null;
        String link = null;
        String pubDate = null;
        String description = null;
        String enclosure = null;
        String duration = null;
        String explicit = null;
        String episodeImage = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            Log.d("XML3", name);
            if (name.equals("title")) {
                title = readTitle(parser);
            } else if (name.equals("link")) {
                link = readLink(parser);
            } else if (name.equals("pubDate")) {
                pubDate = readPubDate(parser);
            } else if (name.equals("description")) {
                description = readDescription(parser);
            } else if (name.equals("enclosure")) {
                enclosure = readEnclosure(parser);
            } else if (name.equals("itunes:duration")) {
                Log.d("XMLDuration", "name equals itunes:duration");
                duration = readDuration(parser);
            } else if (name.equals("itunes:explicit")) {
                explicit = readExplicit(parser);
            } else if (name.equals("itunes:image")) {
                episodeImage = readEpisodeImage(parser);
            } else if (name.equals("media:thumbnail")) {
                episodeImage = readMediaThumbnail(parser);
            } else {
                skip(parser);
            }
        }
        Log.d("XMLEpisode","title: " + title + " link: " + link + " pubDate: " + pubDate + " description: " + description + " enclosure: " + enclosure + " duration: " + duration + " explicit: " + explicit + " episodeImage: " + episodeImage);
        if(episodeImage == null) {
            episodeImage = channelImage;
        }
        return new Episode(title, link, pubDate, description, enclosure, duration, explicit, episodeImage);
    }

    // Processes title tags in the feed.
    private static String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    private static String readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "link");
        String link = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "link");
        return link;
    }

    private static String readPubDate(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "pubDate");
        String pubDate = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "pubDate");
        pubDate = pubDate.substring(0, 17);
        return pubDate;
    }

    private static String readDescription(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "description");
        String descriptionStr = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "description");

        HtmlToPlainText formatter = new HtmlToPlainText();
        Document descriptionDoc = Jsoup.parse(descriptionStr);
        String description = StringUtils.trim(formatter.getPlainText(descriptionDoc));
        Log.d("XMLFormattedDescription", description);

        return description;
    }

    private static String readEnclosure(XmlPullParser parser) throws IOException, XmlPullParserException {
        String enclosure = "";
        parser.require(XmlPullParser.START_TAG, ns, "enclosure");
        enclosure = parser.getAttributeValue(null, "url");

        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, ns, "enclosure");
        return enclosure;
    }

    private static String readDuration(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "itunes:duration");
        String duration = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "itunes:duration");
        return duration;
    }

    private static String readExplicit(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "itunes:explicit");
        String explicit = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "itunes:explicit");
        return explicit;
    }

    private static String readEpisodeImage(XmlPullParser parser) throws IOException, XmlPullParserException {
        String itunesImage = "";
        parser.require(XmlPullParser.START_TAG, ns, "itunes:image");
        itunesImage = parser.getAttributeValue(null, "href");

        parser.nextTag();

        Log.d("XMLitunesImage", itunesImage);

        parser.require(XmlPullParser.END_TAG, ns, "itunes:image");
        return itunesImage;
    }

    private static String readMediaThumbnail(XmlPullParser parser) throws IOException, XmlPullParserException {
        String mediaThumbnail = "";
        parser.require(XmlPullParser.START_TAG, ns, "media:thumbnail");
        mediaThumbnail = parser.getAttributeValue(null, "url");

        parser.nextTag();

        Log.d("XMLiunesMediaThumbnail", mediaThumbnail);

        parser.require(XmlPullParser.END_TAG, ns, "media:thumbnail");
        return mediaThumbnail;
    }

    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            Log.d("XML4", result);
            parser.nextTag();
        }
        return result;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
