// com/master/mosaique_capital/controller/PortfolioController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final AssetService assetService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary() {
        BigDecimal totalPatrimony = assetService.getTotalPatrimony();

        Map<String, Object> summary = Map.of(
                "totalPatrimony", totalPatrimony
                // D'autres métriques pourront être ajoutées ici
        );

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/distribution")
    public ResponseEntity<List<Map<String, Object>>> getPortfolioDistribution() {
        return ResponseEntity.ok(assetService.getAssetDistribution());
    }
}