package com.eventra.controller;

import com.eventra.common.ApiResponse;
import com.eventra.dto.CatalogDtos.*;
import com.eventra.security.SecurityUtils;
import com.eventra.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Search")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    /** Autocomplete calls pass record=false; submitted searches pass record=true to feed recent/trending. */
    @GetMapping
    public ApiResponse<SearchResponse> search(@RequestParam String q, @RequestParam(required = false) String city,
                                              @RequestParam(defaultValue = "6") int limit, @RequestParam(defaultValue = "false") boolean record) {
        return ApiResponse.ok(searchService.search(q, city, limit, SecurityUtils.currentUserIdOrNull(), record));
    }

    @GetMapping("/suggestions")
    public ApiResponse<SearchSuggestions> suggestions() {
        return ApiResponse.ok(searchService.suggestions(SecurityUtils.currentUserIdOrNull()));
    }
}
