package com.blog.service;

import com.blog.entity.HotTopic;
import com.blog.repository.HotTopicRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class HotTopicFetcher {

    private static final Logger log = LoggerFactory.getLogger(HotTopicFetcher.class);

    private static final String HN_TOP = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String HN_ITEM = "https://hacker-news.firebaseio.com/v0/item/%d.json";
    private static final String GITHUB_SEARCH = "https://api.github.com/search/repositories?q=stars:>50+pushed:>%s&sort=stars&order=desc&per_page=10";
    private static final String ARXIV_AI = "http://export.arxiv.org/api/query?search_query=cat:cs.AI+OR+cat:cs.LG+OR+cat:cs.CL&sortBy=submittedDate&sortOrder=descending&max_results=10";
    private static final String ARXIV_SEC = "http://export.arxiv.org/api/query?search_query=cat:cs.CR&sortBy=submittedDate&sortOrder=descending&max_results=5";

    private final HotTopicRepository repo;
    private final RestTemplate rest;
    private final ObjectMapper mapper;

    public HotTopicFetcher(HotTopicRepository repo, RestTemplateBuilder builder) {
        this.repo = repo;
        this.rest = builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(15))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Scheduled(initialDelay = 5000, fixedRate = 3600000)
    public void scheduledFetch() {
        log.info("========== 多平台热点抓取开始 ==========");
        repo.deleteAll();
        int total = 0;
        total += safeFetch("Hacker News", () -> fetchHackerNews());
        total += safeFetch("GitHub Trending", () -> fetchGitHub());
        total += safeFetch("ArXiv AI", () -> fetchArxivAI());
        total += safeFetch("ArXiv 网络安全", () -> fetchArxivSecurity());
        log.info("========== 抓取完成，共入库 {} 条 ==========", total);
    }

    private int safeFetch(String name, Fetcher fetcher) {
        try {
            int count = fetcher.run();
            log.info("[{}] 成功: {} 条", name, count);
            return count;
        } catch (Exception e) {
            log.warn("[{}] 失败: {}", name, e.getMessage());
            return 0;
        }
    }

    @FunctionalInterface
    private interface Fetcher {
        int run() throws Exception;
    }

    private int fetchHackerNews() throws Exception {
        String idsJson = rest.getForObject(HN_TOP, String.class);
        JsonNode ids = mapper.readTree(idsJson);
        List<HotTopic> topics = new ArrayList<>();
        int limit = Math.min(ids.size(), 15);
        for (int i = 0; i < limit; i++) {
            try {
                long id = ids.get(i).asLong();
                String itemJson = rest.getForObject(String.format(HN_ITEM, id), String.class);
                JsonNode item = mapper.readTree(itemJson);
                if (item == null || item.isNull() || !"story".equals(item.path("type").asText())) continue;
                HotTopic t = new HotTopic();
                t.setTitle(item.path("title").asText());
                String text = item.path("text").asText("");
                t.setSummary(text.isEmpty() ? "来自 Hacker News 的热门讨论" : cleanHtml(text, 140));
                t.setHeatScore(item.path("score").asInt(100));
                t.setSource("Hacker News");
                t.setPlatform("HackerNews");
                t.setCategory("科技");
                String url = item.path("url").asText("");
                t.setSourceUrl(url.isEmpty() ? "https://news.ycombinator.com/item?id=" + id : url);
                long epoch = item.path("time").asLong();
                t.setDate(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toLocalDate());
                topics.add(t);
            } catch (Exception ignored) {}
        }
        repo.saveAll(topics);
        return topics.size();
    }

    private int fetchGitHub() throws Exception {
        String since = LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String url = String.format(GITHUB_SEARCH, since);
        String json = rest.getForObject(url, String.class);
        JsonNode items = mapper.readTree(json).path("items");
        List<HotTopic> topics = new ArrayList<>();
        for (JsonNode repo : items) {
            HotTopic t = new HotTopic();
            String fullName = repo.path("full_name").asText();
            String desc = repo.path("description").asText("");
            t.setTitle(fullName);
            String lang = repo.path("language").asText("");
            StringBuilder summary = new StringBuilder();
            if (!lang.isEmpty()) summary.append("[").append(lang).append("] ");
            summary.append(desc.isEmpty() ? "热门开源项目" : desc);
            t.setSummary(summary.length() > 140 ? summary.substring(0, 140) + "..." : summary.toString());
            t.setHeatScore(Math.min(repo.path("stargazers_count").asInt(100), 10000));
            t.setSource("GitHub");
            t.setPlatform("GitHub");
            t.setCategory("开发");
            t.setSourceUrl(repo.path("html_url").asText());
            String created = repo.path("created_at").asText();
            t.setDate(created.isEmpty() ? LocalDate.now() : LocalDate.parse(created.substring(0, 10)));
            topics.add(t);
        }
        repo.saveAll(topics);
        return topics.size();
    }

    private int fetchArxivAI() throws Exception {
        return fetchArxiv(ARXIV_AI, "AI");
    }

    private int fetchArxivSecurity() throws Exception {
        return fetchArxiv(ARXIV_SEC, "安全");
    }

    private int fetchArxiv(String apiUrl, String category) throws Exception {
        String xml = rest.getForObject(apiUrl, String.class);
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList entries = doc.getElementsByTagName("entry");
        List<HotTopic> topics = new ArrayList<>();
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            HotTopic t = new HotTopic();
            t.setTitle(getText(entry, "title"));
            String summary = getText(entry, "summary");
            t.setSummary(summary.length() > 140 ? summary.substring(0, 140) + "..." : summary);
            t.setHeatScore(2000 + (entries.getLength() - i) * 100);
            t.setSource("ArXiv");
            t.setPlatform("ArXiv");
            t.setCategory(category);
            t.setSourceUrl(getText(entry, "id"));
            String published = getText(entry, "published");
            if (!published.isEmpty()) {
                t.setDate(LocalDate.parse(published.substring(0, 10)));
            } else {
                t.setDate(LocalDate.now());
            }
            topics.add(t);
        }
        repo.saveAll(topics);
        return topics.size();
    }

    private String getText(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return "";
        return nodes.item(0).getTextContent().trim();
    }

    private String cleanHtml(String text, int maxLen) {
        String cleaned = text.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        if (cleaned.length() > maxLen) cleaned = cleaned.substring(0, maxLen) + "...";
        return cleaned;
    }
}