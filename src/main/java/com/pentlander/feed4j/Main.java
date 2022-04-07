package com.pentlander.feed4j;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import gg.jte.resolve.ResourceCodeResolver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

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

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args == null || args.length < 1) {
            System.err.println("Must provide filename with URLs.");
            System.exit(1);
        }
        var path = Path.of(args[0]);
        var client = HttpClient.newHttpClient();

        var items = new ArrayList<FeedItem>();
        for (String url : Files.readAllLines(path)) {
            for (int tries = 0; tries < MAX_TRIES; tries++) {
                try {
                    tries++;
                    var req = HttpRequest.newBuilder(URI.create(url)).GET().build();
                    var resp = client.send(req, BodyHandlers.ofInputStream());

                    var feedInput = new SyndFeedInput();
                    feedInput.setXmlHealerOn(true);
                    feedInput.build(new InputStreamReader(resp.body())).getEntries().stream()
                            .map(FeedItem::fromEntry).sorted().limit(10).forEach(items::add);
                } catch (IOException e) {
                    if (tries < MAX_TRIES) continue;
                    System.err.printf("Failed to fetch url after %s tries: %s%n", tries, url);
                    e.printStackTrace();
                } catch (IllegalArgumentException | FeedException e) {
                    System.err.printf("Failed to process url '%s': %s%n", url, e.getMessage());
                    break;
                }
            }
        }
        items.sort(FeedItem::compareTo);

        var resolver = new ResourceCodeResolver("templates");
        var templateEngine = TemplateEngine.create(resolver, ContentType.Html);
        var output = new StringOutput();
        templateEngine.render("index.jte", items, output);

        System.out.println(output);
    }
}
