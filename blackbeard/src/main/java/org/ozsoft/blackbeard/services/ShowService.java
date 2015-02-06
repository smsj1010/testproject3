package org.ozsoft.blackbeard.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.xml.parsers.ParserConfigurationException;

import org.ozsoft.blackbeard.data.Configuration;
import org.ozsoft.blackbeard.domain.Episode;
import org.ozsoft.blackbeard.domain.Show;
import org.ozsoft.blackbeard.domain.Torrent;
import org.ozsoft.blackbeard.parsers.EpisodeListParser;
import org.ozsoft.blackbeard.parsers.ShowListParser;
import org.ozsoft.blackbeard.providers.AbstractSearchProvider;
import org.ozsoft.blackbeard.providers.KickAssSearchProvider;
import org.ozsoft.blackbeard.util.http.HttpClient;
import org.ozsoft.blackbeard.util.http.HttpRequest;
import org.ozsoft.blackbeard.util.http.HttpResponse;
import org.xml.sax.SAXException;

/**
 * TV show service.
 * 
 * @author Oscar Stigter
 */
@ManagedBean(name = "showService")
@ApplicationScoped
public class ShowService {

    private static final String TVRAGE_SHOW_SEARCH_URL = "http://services.tvrage.com/feeds/search.php?show=%s";

    private static final String TVRAGE_EPISODE_LIST_URL = "http://services.tvrage.com/feeds/episode_list.php?sid=%d";

    private static final Pattern EPISODE_PATTERN = Pattern.compile("^.*s(\\d+)e(\\d+).*$");

    // private static final String PROXY_HOST = "146.106.91.10";
    // private static final int PROXY_PORT = 8080;
    // private static final String PROXY_USERNAME = "";
    // private static final String PROXY_PASSWORD = "";

    private static final Set<AbstractSearchProvider> searchProviders;

    private final HttpClient httpClient;

    private final Configuration config;

    /**
     * Static initializer.
     */
    static {
        // Set torrent search providers.
        searchProviders = new HashSet<AbstractSearchProvider>();
        searchProviders.add(new KickAssSearchProvider());
        // searchProviders.add(new BitSnoopSearchProvider());
    }

    public ShowService() {
        config = new Configuration();
        config.load();

        httpClient = new HttpClient();
        // httpClient.setUseProxy(true);
        // httpClient.setProxyHost(PROXY_HOST);
        // httpClient.setProxyPort(PROXY_PORT);
        // httpClient.setProxyUsername(PROXY_USERNAME);
        // httpClient.setProxyPassword(PROXY_PASSWORD);
    }

    public Show[] getShows() {
        return config.getShows();
    }

    /**
     * Searches for shows by name.
     * 
     * @param text
     *            Part of the show's name.
     * @return
     */
    public List<Show> searchShows(String text) {
        List<Show> shows = new ArrayList<Show>();
        String uri = String.format(TVRAGE_SHOW_SEARCH_URL, text);
        try {
            HttpRequest httpRequest = httpClient.createGetRequest(uri);
            HttpResponse httpResponse = httpRequest.execute();
            int statusCode = httpResponse.getStatusCode();
            if (statusCode == 200) {
                shows = ShowListParser.parse(httpResponse.getBody());
            } else {
                System.err.format("ERROR: Could not retrieve list of shows from URI '%s' (HTTP status: %d)\n", uri, statusCode);
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            System.err.format("ERROR: Could not parse list of shows from URI '%s'\n", uri);
            e.printStackTrace();
        }

        return shows;
    }

    public void updateEpisodes(Show show) {
        String uri = String.format(TVRAGE_EPISODE_LIST_URL, show.getId());
        try {
            HttpRequest httpRequest = httpClient.createGetRequest(uri);
            HttpResponse httpResponse = httpRequest.execute();
            int statusCode = httpResponse.getStatusCode();
            if (statusCode == 200) {
                List<Episode> episodes = EpisodeListParser.parse(httpResponse.getBody());
                for (Episode episode : episodes) {
                    Episode existingEpisode = show.getEpisode(episode.getEpisodeNumber());
                    if (existingEpisode != null) {
                        episode.setStatus(existingEpisode.getStatus());
                    }
                    show.addEpisode(episode);
                }
            } else {
                System.err.format("ERROR: Could not retrieve episode list from URI '%s' (HTTP status: %d)\n", uri, statusCode);
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            System.err.format("ERROR: Could not parse episode list from URI '%s'\n", uri);
            e.printStackTrace();
        }
    }

    public List<Torrent> searchTorrents(String text) {
        Set<Torrent> torrents = new TreeSet<Torrent>();
        for (AbstractSearchProvider searchProvider : searchProviders) {
            torrents.addAll(searchProvider.search(text, httpClient));
        }

        filterTorrents(torrents);

        return new ArrayList<Torrent>(torrents);
    }

    public void filterTorrents(Set<Torrent> torrents) {
        Iterator<Torrent> it = torrents.iterator();
        while (it.hasNext()) {
            Torrent torrent = it.next();
            String name = torrent.title.replaceAll("\\.", " ").toLowerCase();
            Matcher m = EPISODE_PATTERN.matcher(name);
            if (m.matches()) {
                // int season = Integer.parseInt(m.group(1));
                // int episode = Integer.parseInt(m.group(2));
                // System.out.format("%s (season: %d, episode: %d)\n", name, season, episode);
            } else {
                it.remove();
            }
        }
    }

    public void downloadTorrent(Torrent torrent) {
        try {
            Runtime.getRuntime().exec("cmd /c start " + torrent.magnetUri);
        } catch (IOException e) {
            System.err.format("ERROR: Failed to download torrent '%s'\n", torrent.title);
        }
    }
}