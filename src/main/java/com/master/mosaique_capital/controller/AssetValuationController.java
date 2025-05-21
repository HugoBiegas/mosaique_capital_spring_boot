// com/master/mosaique_capital/controller/AssetValuationController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.dto.asset.AssetValuationDto;
import com.master.mosaique_capital.service.AssetValuationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/valuations")
@RequiredArgsConstructor
public class AssetValuationController {

    private final AssetValuationService valuationService;

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<AssetValuationDto>> getValuationsByAssetId(@PathVariable Long assetId) {
        return ResponseEntity.ok(valuationService.getValuationsByAssetId(assetId));
    }

    @GetMapping("/asset/{assetId}/range")
    public ResponseEntity<List<AssetValuationDto>> getValuationsByDateRange(
            @PathVariable Long assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(valuationService.getValuationsByAssetIdAndDateRange(assetId, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetValuationDto> getValuationById(@PathVariable Long id) {
        return ResponseEntity.ok(valuationService.getValuationById(id));
    }

    @PostMapping
    public ResponseEntity<AssetValuationDto> createValuation(@Valid @RequestBody AssetValuationDto valuationDto) {
        AssetValuationDto createdValuation = valuationService.createValuation(valuationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdValuation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteValuation(@PathVariable Long id) {
        valuationService.deleteValuation(id);
        return ResponseEntity.noContent().build();
    }
}