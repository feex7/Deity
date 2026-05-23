package com.blog.service;

import com.blog.entity.MarketData;
import com.blog.repository.MarketDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private static final String YHOO = "https://query1.finance.yahoo.com/v8/finance/chart/%s?range=%s&interval=1d";

    private static final String[] STOCKS = {"AAPL", "GOOGL", "MSFT", "AMZN", "NVDA"};
    private static final String GOLD_SYMBOL = "GLD";

    private final MarketDataRepository repo;
    private final RestTemplate rest;
    private final ObjectMapper mapper;
    private final Random rng = new Random();

    public MarketDataService(MarketDataRepository repo, RestTemplateBuilder builder) {
        this.repo = repo;
        this.rest = builder.connectTimeout(Duration.ofSeconds(8)).readTimeout(Duration.ofSeconds(12)).build();
        this.mapper = new ObjectMapper();
    }

    @Transactional
    public List<Map<String, Object>> getStockData(String symbol, int days) {
        List<MarketData> cached = repo.findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(
                symbol, LocalDate.now().minusDays(days + 5), LocalDate.now());
        if (cached.size() >= days - 2) {
            return toChartData(cached);
        }
        try {
            fetchAndSave(symbol, days < 60 ? "1mo" : "3mo");
        } catch (Exception e) {
            log.warn("Yahoo API 失败 [{}], 使用模拟数据: {}", symbol, e.getMessage());
            generateMock(symbol, days);
        }
        cached = repo.findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(
                symbol, LocalDate.now().minusDays(days + 5), LocalDate.now());
        return toChartData(cached);
    }

    @Transactional
    public List<Map<String, Object>> getGoldData(int days) {
        List<MarketData> cached = repo.findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(
                GOLD_SYMBOL, LocalDate.now().minusDays(days + 5), LocalDate.now());
        if (cached.size() >= days - 5) {
            return toChartData(cached);
        }
        try {
            fetchAndSave(GOLD_SYMBOL, "3mo");
        } catch (Exception e) {
            log.warn("Yahoo API 失败 [GLD], 使用模拟数据: {}", e.getMessage());
            generateGoldMock(days);
        }
        cached = repo.findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(
                GOLD_SYMBOL, LocalDate.now().minusDays(days + 5), LocalDate.now());
        return toChartData(cached);
    }

    @Transactional
    public Map<String, Object> refreshAll() {
        int stockCount = 0, goldCount = 0;
        for (String sym : STOCKS) {
            try {
                fetchAndSave(sym, "1mo");
                stockCount++;
            } catch (Exception ignored) {}
        }
        try {
            fetchAndSave(GOLD_SYMBOL, "3mo");
            goldCount = 1;
        } catch (Exception ignored) {}
        Map<String, Object> result = new HashMap<>();
        result.put("stocks", stockCount);
        result.put("gold", goldCount);
        result.put("message", "已刷新 " + stockCount + " 支股票 + " + (goldCount > 0 ? "黄金数据" : ""));
        return result;
    }

    private void fetchAndSave(String symbol, String range) throws Exception {
        String url = String.format(YHOO, symbol, range);
        String json = rest.getForObject(url, String.class);
        JsonNode result = mapper.readTree(json).path("chart").path("result").get(0);
        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = result.path("indicators").path("quote").get(0);
        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");

        repo.deleteBySymbol(symbol);
        List<MarketData> list = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            if (opens.get(i).isNull() && closes.get(i).isNull()) continue;
            MarketData d = new MarketData();
            d.setSymbol(symbol);
            d.setType(symbol.equals(GOLD_SYMBOL) ? "GOLD" : "STOCK");
            d.setTradeDate(Instant.ofEpochSecond(timestamps.get(i).asLong())
                    .atZone(ZoneId.systemDefault()).toLocalDate());
            d.setOpen(opens.get(i).isNull() ? null : opens.get(i).asDouble());
            d.setHigh(highs.get(i).isNull() ? null : highs.get(i).asDouble());
            d.setLow(lows.get(i).isNull() ? null : lows.get(i).asDouble());
            d.setClose(closes.get(i).isNull() ? null : closes.get(i).asDouble());
            d.setVolume(volumes.get(i).isNull() ? null : volumes.get(i).asLong());
            list.add(d);
        }
        repo.saveAll(list);
        log.info("Yahoo API: {} 获取 {} 条", symbol, list.size());
    }

    private void generateMock(String symbol, int days) {
        repo.deleteBySymbol(symbol);
        double basePrice = switch (symbol) {
            case "AAPL" -> 190; case "GOOGL" -> 175; case "MSFT" -> 420;
            case "AMZN" -> 185; case "NVDA" -> 900;
            default -> 100;
        };
        List<MarketData> list = new ArrayList<>();
        double price = basePrice;
        for (int i = days; i >= 0; i--) {
            double change = (rng.nextDouble() - 0.48) * basePrice * 0.03;
            double open = price;
            double close = price + change;
            double high = Math.max(open, close) + rng.nextDouble() * basePrice * 0.01;
            double low = Math.min(open, close) - rng.nextDouble() * basePrice * 0.01;
            MarketData d = new MarketData();
            d.setSymbol(symbol);
            d.setType("STOCK");
            d.setTradeDate(LocalDate.now().minusDays(i));
            d.setOpen(Math.round(open * 100.0) / 100.0);
            d.setHigh(Math.round(high * 100.0) / 100.0);
            d.setLow(Math.round(low * 100.0) / 100.0);
            d.setClose(Math.round(close * 100.0) / 100.0);
            d.setVolume(10_000_000L + rng.nextLong(50_000_000));
            list.add(d);
            price = close;
        }
        repo.saveAll(list);
    }

    private void generateGoldMock(int days) {
        repo.deleteBySymbol(GOLD_SYMBOL);
        double basePrice = 220;
        List<MarketData> list = new ArrayList<>();
        double price = basePrice;
        for (int i = days; i >= 0; i--) {
            double change = (rng.nextDouble() - 0.5) * 1.5;
            double open = price;
            double close = price + change;
            double high = Math.max(open, close) + rng.nextDouble() * 1.0;
            double low = Math.min(open, close) - rng.nextDouble() * 1.0;
            MarketData d = new MarketData();
            d.setSymbol(GOLD_SYMBOL);
            d.setType("GOLD");
            d.setTradeDate(LocalDate.now().minusDays(i));
            d.setOpen(Math.round(open * 100.0) / 100.0);
            d.setHigh(Math.round(high * 100.0) / 100.0);
            d.setLow(Math.round(low * 100.0) / 100.0);
            d.setClose(Math.round(close * 100.0) / 100.0);
            d.setVolume(null);
            list.add(d);
            price = close;
        }
        repo.saveAll(list);
    }

    private List<Map<String, Object>> toChartData(List<MarketData> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MarketData d : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.getTradeDate().toString());
            row.put("open", d.getOpen());
            row.put("high", d.getHigh());
            row.put("low", d.getLow());
            row.put("close", d.getClose());
            row.put("volume", d.getVolume());
            result.add(row);
        }
        return result;
    }
}