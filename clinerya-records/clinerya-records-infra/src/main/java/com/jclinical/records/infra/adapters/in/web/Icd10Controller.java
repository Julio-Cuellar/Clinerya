package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.Icd10Code;
import com.jclinical.records.domain.ports.in.ManageIcd10CatalogUseCase;
import com.jclinical.records.infra.adapters.in.web.dto.Icd10CodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/icd10")
@RequiredArgsConstructor
public class Icd10Controller {

    private final ManageIcd10CatalogUseCase icd10CatalogUseCase;

    @GetMapping
    public ResponseEntity<List<Icd10CodeResponse>> search(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        List<Icd10Code> results = icd10CatalogUseCase.search(query, limit);
        return ResponseEntity.ok(results.stream().map(Icd10CodeResponse::from).toList());
    }
}
