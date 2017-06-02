package bbr.podcast.feeds;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

import bbr.podcast.PodcastEpsActivity;

/**
 * Created by Me on 4/18/2017.
 */

public class PodcastFeedParser {

    private static final String ns = null;

    public PodcastChannel parse(InputStream in, PodcastEpsActivity.DownloadXmlTask task) throws XmlPullParserException, IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            //parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return RSSParser.readFeed(parser, task);
        } finally {
            in.close();
        }
    }


}
