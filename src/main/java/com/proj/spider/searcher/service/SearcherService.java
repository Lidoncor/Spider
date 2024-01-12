package com.proj.spider.searcher.service;

import com.proj.spider.crawler.model.Word;
import com.proj.spider.repository.PageRankRepository;
import com.proj.spider.repository.URLRepository;
import com.proj.spider.repository.WordRepository;
import com.proj.spider.searcher.dto.MatchedRow;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Service
public class SearcherService {

    WordRepository wordRepository;
    PageRankRepository pageRankRepository;
    URLRepository urlRepository;

    public SearcherService(WordRepository wordRepository,
                           PageRankRepository pageRankRepository,
                           URLRepository urlRepository) {
        this.wordRepository = wordRepository;
        this.pageRankRepository = pageRankRepository;
        this.urlRepository = urlRepository;
    }

    public String search(Model model, @RequestParam String query) {
        List<Word> words = getWordsIds(query);

        List<MatchedRow> rows = getMatchingRows(words);

        drawMatchingTable(words, rows);

        Map<Integer, Double> m1Scores = distanceScore(rows, words.size());

        Map<Integer, Double> m2Scores = pageRankScore(m1Scores.keySet());

        drawResultScoreTable(m1Scores, m2Scores);

        Map<String, List<String>> pageWords = new LinkedHashMap<>();
        for (var entry : m1Scores.entrySet()) {
            pageWords.put(getUrlName(entry.getKey()), getPageWords(entry.getKey()));
        }

        return markHtml(pageWords, words, model);
    }

    private String getUrlName(int id) {
        return urlRepository.findById(id).getValue();
    }

    private List<String> getPageWords(int id) {
        return wordRepository.getPageWords(id);
    }

    private String markHtml(Map<String, List<String>> pageWords, List<Word> queryWords, Model model) {
        Map<String, String> pageContent = new LinkedHashMap<>();
        for (var entry : pageWords.entrySet()) {
            StringBuilder content = new StringBuilder();
            for (String str : entry.getValue()) {
                if (Objects.equals(str, queryWords.get(0).getValue())) {
                    content.append("<mark>").append(str).append("</mark>");
                } else if (Objects.equals(str, queryWords.get(1).getValue())) {
                    content.append("<mark style=\"background-color: aqua;\">").append(str).append("</mark>");
                } else {
                    content.append(str).append(" ");
                }
            }
            pageContent.put(entry.getKey(), content.toString());
        }

        model.addAttribute("pageContent", pageContent);

        return "index";
    }

    private List<Word> getWordsIds(String query) {
        String[] words = query.split(" ");
        return List.of(wordRepository.findFirstByValue(words[0]), wordRepository.findFirstByValue(words[1]));
    }

    private List<MatchedRow> getMatchingRows(List<Word> words) {
        return wordRepository.getMatchingRows(words.get(0).getId(), words.get(1).getId());
    }

    private Map<Integer, Double> distanceScore(List<MatchedRow> matchedRows, Integer wordsQuantity) {
        Map<Integer, Double> result = new HashMap<>();

        if (wordsQuantity <= 1) {
            for (MatchedRow matchedRow : matchedRows) {
                result.put(matchedRow.getUrlId(), 1.0);
            }
        } else {
            Map<Integer, List<MatchedRow>> temp = new HashMap<>();
            for (MatchedRow matchedRow : matchedRows) {
                if (!temp.containsKey(matchedRow.getUrlId())) {
                    temp.put(matchedRow.getUrlId(), new ArrayList<>());
                }
                temp.get(matchedRow.getUrlId()).add(matchedRow);
            }

            for (var entry : temp.entrySet()) {
                double score = Double.MAX_VALUE;

                for (MatchedRow i : entry.getValue()) {
                    double scoreTemp = 0.0;
                    for (MatchedRow k : entry.getValue()) {
                        scoreTemp += Math.abs(i.getWordPos1() - k.getWordPos2());
                    }
                    score = Math.min(scoreTemp, score);
                }

                result.put(entry.getKey(), score);
            }

        }

        return normalizeScores(result, false);
    }

    private Map<Integer, Double> normalizeScores(Map<Integer, Double> scores, boolean smallIsBetter) {
        Map<Integer, Double> result = new HashMap<>();

        double vSmall = 0.00001;
        double minScore = Collections.min(scores.values());
        double maxScore = Collections.max(scores.values());

        for (var entry : scores.entrySet()) {
            if (smallIsBetter) {
                result.put(entry.getKey(), minScore / Math.max(vSmall, entry.getValue()));
            } else {
                result.put(entry.getKey(), entry.getValue() / maxScore);
            }
        }

        List<Map.Entry<Integer, Double>> list = new ArrayList<>(result.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);

        Map<Integer, Double> sortedRes = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : list) {
            sortedRes.put(entry.getKey(), entry.getValue());
        }

        return sortedRes;
    }

    private Map<Integer, Double> pageRankScore(Set<Integer> urls) {
        Map<Integer, Double> res = new HashMap<>();

        for (Integer urlId : urls) {
            double score = pageRankRepository.getScore(urlId);
            res.put(urlId, score);
        }

        return normalizeScores(res, false);
    }

    private void drawMatchingTable(List<Word> words, List<MatchedRow> rows) {
        AsciiTable asciiTable = new AsciiTable();
        asciiTable.addRule();
        asciiTable.addRow("Url Id", "Location '%s'".formatted(words.get(0).getValue()), "Location '%s'".formatted(words.get(1).getValue()));
        asciiTable.addRule();
        for (MatchedRow matchedRow : rows) {
            asciiTable.addRow(matchedRow.getUrlId(), matchedRow.getWordPos1(), matchedRow.getWordPos2());
            asciiTable.addRule();
        }
        asciiTable.setTextAlignment(TextAlignment.CENTER);
        String render = asciiTable.render();
        System.out.println(render);
    }

    private void drawResultScoreTable(Map<Integer, Double> m1Scores, Map<Integer, Double> m2Scores) {
        AsciiTable asciiTable = new AsciiTable();
        asciiTable.addRule();
        asciiTable.addRow("Score M1", "Score M2", "Score M3", "URL id", "URL name");
        asciiTable.addRule();
        for (var entry : m1Scores.entrySet()) {
            asciiTable.addRow(
                    entry.getValue(),
                    m2Scores.get(entry.getKey()),
                    (entry.getValue() + m2Scores.get(entry.getKey())) / 2,
                    entry.getKey(),
                    getUrlName(entry.getKey())
            );
            asciiTable.addRule();
        }
        asciiTable.setTextAlignment(TextAlignment.CENTER);
        String render = asciiTable.render();
        System.out.println(render);
    }
}
