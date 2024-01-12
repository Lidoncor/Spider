package com.proj.spider.crawler.service;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.SilentJavaScriptErrorListener;
import com.proj.spider.crawler.model.LinkBetweenURL;
import com.proj.spider.crawler.model.Location;
import com.proj.spider.crawler.model.URL;
import com.proj.spider.crawler.model.Word;
import com.proj.spider.repository.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

@Service
public class CrawlerService {
    WordRepository wordRepository;
    URLRepository urlRepository;
    LocationRepository locationRepository;
    LinkBetweenURLRepository linkBetweenURLRepository;
    PageRankRepository pageRankRepository;
    WebClient webClient;

    double pagesCount = 0;
    double linksCount = 0;
    double wordCount = 0;
    double linkBetweenUrlCount = 0;

    public CrawlerService(WordRepository wordRepository,
                          URLRepository urlRepository,
                          LocationRepository locationRepository,
                          LinkBetweenURLRepository linkBetweenURLRepository,
                          PageRankRepository pageRankRepository) {
        this.wordRepository = wordRepository;
        this.urlRepository = urlRepository;
        this.locationRepository = locationRepository;
        this.linkBetweenURLRepository = linkBetweenURLRepository;
        this.pageRankRepository = pageRankRepository;

        webClient = new WebClient();
        webClient.setJavaScriptErrorListener(new SilentJavaScriptErrorListener());
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(1000);
    }

    public void crawl(Set<String> urls, int depth) {

        XYSeries linksSeries = new XYSeries("Ссылки");
        XYSeries urlBetweenSeries = new XYSeries("Ссылки на внеш. стр.");
        XYSeries wordSeries = new XYSeries("Слова");

        Set<String> nextPages = new HashSet<>();

        System.out.println("Page traversal started");
        for (int i = 0; i < depth; i++) {
            System.out.println("DEPTH " + i);
            for (String url : urls) {
                System.out.println("NOW " + url);
                if (isIndexed(url)) {
                    System.out.println("INDEXED " + url);
                    continue;
                }

                pagesCount++;

                URL urlEntity = saveURL(url);

                Document document;
                try {
                    document = getPage(url);
                    nextPages.addAll(getLinks(document, urlEntity));

                    linksSeries.add(pagesCount, linksCount);
                    urlBetweenSeries.add(pagesCount, linkBetweenUrlCount);
                } catch (Exception ex) {
                    continue;
                }

                wordCount += indexPage(document, urlEntity);
                wordSeries.add(pagesCount, wordCount);

                if (pagesCount % 10 == 0) {
                    System.out.println("URL table: " + urlRepository.count());
                    System.out.println("Word table: " + wordRepository.count());
                    System.out.println("Location table: " + locationRepository.count());
                    System.out.println("LinkBetweenUrl table: " + linkBetweenURLRepository.count());
                }

            }

            urls.addAll(nextPages);
            nextPages.clear();

        }
        System.out.println("DONE");
        webClient.close();

        pageRank();

        JFreeChart wordChart = ChartFactory.createXYLineChart(
                "Слова",
                "Страницы",
                "Слова",
                createDataset(wordSeries),
                PlotOrientation.VERTICAL,
                true, // Наличие легенды
                false, // Инструменты масштабирования
                false // Инструменты выбора
        );
        drawChart(wordChart, "wordChart");

        JFreeChart linksChart = ChartFactory.createXYLineChart(
                "Ссылки",
                "Страницы",
                "Ссылки",
                createDataset(linksSeries),
                PlotOrientation.VERTICAL,
                true, // Наличие легенды
                false, // Инструменты масштабирования
                false // Инструменты выбора
        );
        drawChart(linksChart, "linksChart");

        JFreeChart linkBetweenChart = ChartFactory.createXYLineChart(
                "Ссылки на внеш. стр.",
                "Страницы",
                "Ссылки",
                createDataset(urlBetweenSeries),
                PlotOrientation.VERTICAL,
                true, // Наличие легенды
                false, // Инструменты масштабирования
                false // Инструменты выбора
        );
        drawChart(linkBetweenChart, "linksBetweenUrlChart");
    }

    private void pageRank() {
        pageRankRepository.truncate();
        pageRankRepository.defaultSettings();

        for (int i = 0; i < 20; i++) {
            System.out.println("PageRank Iter: " + i);

            List<Integer> urlIds = pageRankRepository.getAllUrlIds();
            for (Integer urlId : urlIds) {
                double pr = 1 - 0.85;

                List<Integer> linkedPages = pageRankRepository.getAllLinkedPagesIds(urlId);
                for (Integer linkedPage : linkedPages) {
                    double score = pageRankRepository.getScore(linkedPage);
                    int linkingCount = pageRankRepository.linkingCount(linkedPage);
                    pr += 0.85 * (score / linkingCount);
                }

                pageRankRepository.updateScore(pr, urlId);
            }
        }
    }

    public Document getPage(String url) throws IOException {
        HtmlPage myPage = webClient.getPage(url);
        return Jsoup.parse(myPage.asXml());
    }

    public Set<String> getLinks(Document document, URL url) throws URISyntaxException {
        URI uri = new URI(url.getValue());
        String domain = uri.getHost();

        Set<String> urls = new HashSet<>();
        Elements aTags = document.select("a");

        for (Element aTag : aTags) {
            String link;

            if (aTag.hasAttr("href") && !aTag.attr("href").isEmpty()) {
                link = aTag.attr("href");
            } else {
                continue;
            }

            if (link.charAt(0) == '/') {
                link = "https://" + domain + link;
            }

            if (isIndexed(link)) {
                continue;
            }

            URL url_test = saveURL(link);

            linkBetweenURLRepository.save(new LinkBetweenURL(url, url_test));
            linkBetweenUrlCount++;

            urls.add(link);

        }

        return urls;
    }

    public URL saveURL(String url) {
        URL urlEntity = new URL(url);
        urlRepository.save(urlEntity);
        linksCount++;
        return urlEntity;
    }

    public String getText(Document document) {
        String bodyText = document.select("body").text();
        bodyText = bodyText.replaceAll("[^\\p{IsCyrillic}+]", " ");
        return bodyText;
    }

    public double indexPage(Document document, URL url) {
        StringTokenizer stringTokenizer = new StringTokenizer(getText(document));
        double wrdCount = 0;
        for (int position = 0; position < stringTokenizer.countTokens(); position++) {
            String wordToken = stringTokenizer.nextToken();
            Word word = wordRepository.findByValue(wordToken);
            if (word == null) {
                word = new Word(wordToken);
                wrdCount++;
            }
            word.getLocation().add(new Location(url, word, position));
            wordRepository.save(word);
        }
        return wrdCount;
    }

    public boolean isIndexed(String url) {
        return locationRepository.existsByUrlValue(url);
    }

    private XYDataset createDataset(XYSeries series) {
        return new XYSeriesCollection(series);
    }

    private void drawChart(JFreeChart chart, String name) {
        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        File outputfile = new File(name + ".jpg");
        try {
            ChartUtilities.saveChartAsJPEG(outputfile, chart, 700, 700);
        } catch (Exception ignored) {}
    }

}
