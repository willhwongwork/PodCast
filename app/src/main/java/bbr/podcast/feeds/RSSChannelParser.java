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

import bbr.podcast.activity.PodcastEpsActivity;

/**
 * Created by Me on 4/19/2017.
 */

public class RSSChannelParser {

    private static final String ns = null;

    protected static PodcastChannel readChannel(XmlPullParser parser, PodcastEpsActivity.DownloadXmlTask task) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "channel");
        String title = null;
        String description = null;
        String language = null;
        String copyright = null;
        String managingEditor = null;
        String itunesImage = null;
        String imageUrl = null;
        List<Episode> episodes = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if(task != null && task.isCancelled() == true) {
                Log.d("XMLChannel", "task is being cancelled");
                return null;
            }
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            Log.d("XMLChannel", name);
            if (name.equals("title")) {
                title = readTitle(parser);
            } else if (name.equals("description")) {
                description = readDescription(parser);
            } else if (name.equals("language")) {
                language = readLanguage(parser);
            } else if (name.equals("copyright")) {
                copyright = readCopyright(parser);
            } else if (name.equals("managingEditor")) {
                managingEditor = readManagingEditor(parser);
            } else if (name.equals("itunes:image")) {
                itunesImage = readItunesImage(parser);
            } else if (name.equals("image")) {
                imageUrl = readImageUrl(parser);
            } else if (name.equals("item")){
                episodes.add(RSSParser.readEpisode(parser, itunesImage));
            } else {
                skip(parser);
            }
        }
        return new PodcastChannel(title, description, language, copyright, managingEditor, itunesImage, imageUrl, episodes);
    }

    private static String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    private static String readDescription(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "description");
        String descriptionStr = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "description");

        HtmlToPlainText formatter = new HtmlToPlainText();
        Document descriptionDoc = Jsoup.parse(descriptionStr);
        String description = StringUtils.trim(formatter.getPlainText(descriptionDoc));
        Log.d("XMLFormatChannelDescrip", description);

        return description;
    }

    private static String readLanguage(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "language");
        String language = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "language");
        return language;
    }

    private static String readCopyright(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "copyright");
        String copyright = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "copyright");
        return copyright;
    }

    private static String readManagingEditor(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "managingEditor");
        String managingEditor = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "managingEditor");
        return managingEditor;
    }

    private static String readItunesImage(XmlPullParser parser) throws IOException, XmlPullParserException {
        String itunesImage = "";
        parser.require(XmlPullParser.START_TAG, ns, "itunes:image");
        itunesImage = parser.getAttributeValue(null, "href");

        parser.nextTag();

        Log.d("XMLitunesImage", itunesImage);

        parser.require(XmlPullParser.END_TAG, ns, "itunes:image");
        return itunesImage;
    }

    private static String readImageUrl(XmlPullParser parser)throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "image");

        String imageUrl = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            Log.d("XMLChannelImageUrl", name);
            if (name.equals("url")) {
                parser.require(XmlPullParser.START_TAG, ns, "url");
                imageUrl = readText(parser);
                parser.require(XmlPullParser.END_TAG, ns, "url");

            } else {
                skip(parser);
            }
        }
        return imageUrl;
    }

    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            Log.d("XMLChannel", result);
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
