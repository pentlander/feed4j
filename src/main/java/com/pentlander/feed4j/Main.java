package com.pentlander.feed4j;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import gg.jte.resolve.ResourceCodeResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

import static java.net.http.HttpResponse.BodyHandlers;
import static java.util.Comparator.comparing;

public class Main {
    private static final int MAX_TRIES = 3;

    private static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : Instant.MIN;
    }

    public record FeedItem(String title, URI uri, Instant publishedAt, Instant updatedAt) implements Comparable<FeedItem> {
        static FeedItem fromEntry(SyndEntry entry) {
            try {
                var uri = new URI(entry.getLink());
                return new FeedItem(
                        entry.getTitle(),
                        uri,
                        toInstant(entry.getPublishedDate()),
                        toInstant(entry.getUpdatedDate())
                );
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public String host() {
            return uri().getHost();
        }

        @Override
        public int compareTo(FeedItem o) {
            if (o == null) return -1;
            return comparing(FeedItem::publishedAt).reversed()
                    .thenComparing(comparing(FeedItem::publishedAt).reversed())
                    .compare(this, o);
        }
    }

    public static HttpResponse<InputStream> getUrl(HttpClient client, String url) throws InterruptedException, IOException {
        var req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
        IOException exception = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                return client.send(req, BodyHandlers.ofInputStream());
            } catch (HttpTimeoutException e) {
                throw e;
            } catch (IOException e) {
                exception = e;
            }
        }
        throw exception;
    }

    public static void main(String[] args) throws IOException {
        if (args == null || args.length < 1) {
            System.err.println("Must provide filename with URLs.");
            System.exit(1);
        }
        var path = Path.of(args[0]);
        var client = HttpClient.newHttpClient();

        var items = Files.lines(path).parallel().flatMap(url -> {
            try {
                System.err.println("Fetching url: " + url);
                var resp = getUrl(client, url);
                System.err.println("Got url: " + url);
                var feedInput = new SyndFeedInput();
                feedInput.setXmlHealerOn(true);
                return feedInput.build(new InputStreamReader(resp.body())).getEntries().stream()
                        .map(FeedItem::fromEntry).sorted().limit(10);
            } catch (IOException e) {
                System.err.printf("Failed to fetch url '%s' after %s tries: %s%n", url, MAX_TRIES, e.getMessage());
            } catch (IllegalArgumentException | FeedException e) {
                System.err.printf("Failed to process url '%s': %s%n", url, e.getMessage());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return Stream.empty();
        }).sorted(FeedItem::compareTo).toList();

        var resolver = new ResourceCodeResolver("templates");
        var templateEngine = TemplateEngine.create(resolver, ContentType.Html);
        var output = new StringOutput();
        templateEngine.render("index.jte", items, output);

        System.out.println(output);
    }
}
