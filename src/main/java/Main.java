import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

public class Main {

    static List<Match> matches = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        Document doc;

        doc = getDoc("https://lite.betbonanza.com/sports/");

        Elements sportsHref = doc.select("a[href*=https://lite.betbonanza.com/sports/categories?sport=]");
        ExecutorService executorService = Executors.newFixedThreadPool(12);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (Element sportEL : sportsHref) {
            tasks.add(() -> {
                Document document = getDoc(sportEL.attr("href"));
                Elements countriesHref = document.select("a[href*=https://lite.betbonanza.com/sports/leagues/]");
                for (Element countryEl : countriesHref) {
                    document = getDoc(countryEl.attr("href"));
                    Elements leaguesHref = document.select("a[href*=https://lite.betbonanza.com/competition/open/]");
                    for (Element leagueEl : leaguesHref) {
                        parse(getDoc(leagueEl.attr("href")));
                    }
                }
                return null;
            });
        }
        executorService.invokeAll(tasks);
        executorService.shutdown();
        createHTML();


    }

    public static void parse(Document doc) {

        Elements elements = doc.select("table.highlights--item");

        for (Element e : elements) {

            Elements urlA = e.select("table > tbody > tr > td > a");
            String url = urlA.attr("href");

            Elements clubTd = e.select("td.clubs");
            Elements clubSpans = clubTd.select("span");
            List<String> teams = new ArrayList<>();
            for (Element el : clubSpans) {
                teams.add(el.text());
            }

            try {
                if (teams.get(0).contains("Winner") || teams.get(1).isEmpty())
                    continue;
            } catch (Exception ex) {
                continue;
            }

            Elements tdWhen = e.select("td.time");
            String tdStr = tdWhen.text();
            Elements spanWhen = e.select("span.play-time");
            String spanStr = spanWhen.text();
            String whenStr = tdStr.replace(spanStr, "").trim();
//            whenStr = LocalDate.now().getYear() + " " + whenStr.toUpperCase();



            String sportAndLeague = e.select("td.meta > span").text();
            String[] split = sportAndLeague.split("/");

            try {
                matches.add(
                        Match.builder()
                                .url(url)
                                .team1(teams.get(0))
                                .team2(teams.get(1))
                                .start(whenStr)
                                .sport(split[0])
                                .tournament(split[1] + " " + split[2])
                                .build());
            } catch (Exception ignored) {
            }
        }
    }

    public static void createHTML() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");

        var context = new Context();
        context.setVariable("data", matches);

        var templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        var result = templateEngine.process("index", context);

        Path path = Paths.get("src/main/resources/output.html");

        try {
            Files.writeString(path, result, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.out.print("Invalid Path");
        }
        System.out.println(result);
    }

    public static Document getDoc(String url) {
        try {
            return Jsoup
                    .connect(url)
                    .timeout(10 * 1000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .header("Accept-Language", "*")
                    .get();
        } catch (IOException ex) {
            return null;
        }

    }
}
