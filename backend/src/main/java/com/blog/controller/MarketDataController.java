package com.blog.controller;

import com.blog.service.MarketDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    private final MarketDataService service;

    public MarketDataController(MarketDataService service) {
        this.service = service;
    }

    @GetMapping("/stocks")
    public Map<String, Object> getStocks(
            @RequestParam(defaultValue = "AAPL,GOOGL,MSFT,AMZN,NVDA") String symbols,
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (String sym : symbols.split(",")) {
            String s = sym.trim().toUpperCase();
            result.put(s, service.getStockData(s, days));
        }
        return result;
    }

    @GetMapping("/gold")
    public List<Map<String, Object>> getGold(@RequestParam(defaultValue = "90") int days) {
        return service.getGoldData(days);
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        return service.refreshAll();
    }
}