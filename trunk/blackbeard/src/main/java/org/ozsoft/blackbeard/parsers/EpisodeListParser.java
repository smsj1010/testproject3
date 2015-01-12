package org.ozsoft.blackbeard.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.ozsoft.blackbeard.domain.Episode;
import org.ozsoft.blackbeard.domain.Show;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX parser for TV show episode lists from TVRage.
 * 
 * @author Oscar Stigter
 */
public class EpisodeListParser {

    private static final SAXParserFactory SAX_PARSER_FACTORY;

    static {
        SAX_PARSER_FACTORY = SAXParserFactory.newInstance();
        SAX_PARSER_FACTORY.setNamespaceAware(true);
        SAX_PARSER_FACTORY.setValidating(false);
        SAX_PARSER_FACTORY.setXIncludeAware(false);
        try {
            SAX_PARSER_FACTORY.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception e) {
            System.err.println("Could not create SAXParserFactory");
            e.printStackTrace(System.err);
        }
    }

    public static List<Episode> parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = SAX_PARSER_FACTORY.newSAXParser();
        EpisodeListHandler handler = new EpisodeListHandler();
        parser.parse(new InputSource(is), handler);
        return handler.getEpisodes();
    }

    /**
     * SAX content handler for an episode lists for a specific TV show from TVRage. <br />
     * <br />
     * 
     * After parsing the list, the {@link getEpisodes} method can be used to retrieve the episodes. <br />
     * <br />
     * 
     * @author Oscar Stigter
     */
    private static class EpisodeListHandler extends DefaultHandler {

        private final List<Episode> episodes;

        private String nodePath = "";

        private final StringBuilder text = new StringBuilder();

        private Episode episode;

        /**
         * Constructor.
         */
        public EpisodeListHandler() {
            episodes = new ArrayList<Episode>();
        }

        /**
         * Returns the episodes specified in the XML document.
         * 
         * @return The {@link Show}s.
         */
        public List<Episode> getEpisodes() {
            return episodes;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            nodePath = String.format("%s/%s", nodePath, localName);
            text.setLength(0);

            if (nodePath.equals("/Show/Episodelist/Season/episode")) {
                // Start of episode.
                episode = new Episode();
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (nodePath.equals("/Show/Episodelist/Season/episode/epnum")) {
                episode.episode = Integer.parseInt(text.toString());
            } else if (nodePath.equals("/Show/Episodelist/Season/episode/seasonnum")) {
                episode.season = Integer.parseInt(text.toString());
            } else if (nodePath.equals("/Show/Episodelist/Season/episode")) {
                episodes.add(episode);
            }
            nodePath = nodePath.substring(0, nodePath.lastIndexOf('/'));
        }

        @Override
        public void characters(char[] buffer, int start, int length) {
            text.append(String.copyValueOf(buffer, start, length));
        }
    }
}
