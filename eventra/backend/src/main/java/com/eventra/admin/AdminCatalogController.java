package com.eventra.admin;

import com.eventra.admin.AdminDtos.*;
import com.eventra.common.ApiResponse;
import com.eventra.common.PageResponse;
import com.eventra.dto.CatalogDtos.VenueSummary;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Catalog administration. URL-level guard: ADMIN or PARTNER; finer checks in {@link AdminAccess}. */
@Tag(name = "Admin: catalog")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final AdminCatalogService service;

    @GetMapping("/movies")
    public ApiResponse<PageResponse<AdminMovieResponse>> movies(@RequestParam(required = false) String q,
                                                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(PageResponse.of(service.movies(q, page, size)));
    }

    @GetMapping("/movies/{id}")
    public ApiResponse<AdminMovieResponse> movie(@PathVariable Long id) {
        return ApiResponse.ok(service.movie(id));
    }

    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminMovieResponse> createMovie(@Valid @RequestBody MovieUpsertRequest req) {
        return ApiResponse.ok(service.createMovie(req), "Movie created");
    }

    @PutMapping("/movies/{id}")
    public ApiResponse<AdminMovieResponse> updateMovie(@PathVariable Long id, @Valid @RequestBody MovieUpsertRequest req) {
        return ApiResponse.ok(service.updateMovie(id, req), "Movie updated");
    }

    @DeleteMapping("/movies/{id}")
    public ApiResponse<Void> deleteMovie(@PathVariable Long id) {
        service.deactivateMovie(id);
        return ApiResponse.ok(null, "Movie deactivated");
    }

    @GetMapping("/cinemas")
    public ApiResponse<List<AdminCinemaResponse>> cinemas(@RequestParam(required = false) Long cityId) {
        return ApiResponse.ok(service.cinemas(cityId));
    }

    @PostMapping("/cinemas")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminCinemaResponse> createCinema(@Valid @RequestBody CinemaUpsertRequest req) {
        return ApiResponse.ok(service.createCinema(req), "Cinema created");
    }

    @PutMapping("/cinemas/{id}")
    public ApiResponse<AdminCinemaResponse> updateCinema(@PathVariable Long id, @Valid @RequestBody CinemaUpsertRequest req) {
        return ApiResponse.ok(service.updateCinema(id, req), "Cinema updated");
    }

    @DeleteMapping("/cinemas/{id}")
    public ApiResponse<Void> deleteCinema(@PathVariable Long id) {
        service.deactivateCinema(id);
        return ApiResponse.ok(null, "Cinema deactivated");
    }

    @GetMapping("/venues")
    public ApiResponse<List<VenueSummary>> venues() {
        return ApiResponse.ok(service.venues());
    }

    @PostMapping("/venues")
    public ApiResponse<Map<String, Long>> createVenue(@Valid @RequestBody VenueUpsertRequest req) {
        return ApiResponse.ok(Map.of("id", service.upsertVenue(null, req)), "Venue created");
    }

    @PutMapping("/venues/{id}")
    public ApiResponse<Map<String, Long>> updateVenue(@PathVariable Long id, @Valid @RequestBody VenueUpsertRequest req) {
        return ApiResponse.ok(Map.of("id", service.upsertVenue(id, req)), "Venue updated");
    }

    @GetMapping("/seat-types")
    public ApiResponse<List<SeatTypeResponse>> seatTypes() {
        return ApiResponse.ok(service.seatTypes());
    }

    @GetMapping("/screens")
    public ApiResponse<List<AdminScreenResponse>> screens(@RequestParam Long cinemaId) {
        return ApiResponse.ok(service.screens(cinemaId));
    }

    @GetMapping("/screens/{id}")
    public ApiResponse<AdminScreenResponse> screen(@PathVariable Long id) {
        return ApiResponse.ok(service.screen(id));
    }

    @PostMapping("/screens")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminScreenResponse> createScreen(@Valid @RequestBody ScreenUpsertRequest req) {
        return ApiResponse.ok(service.createScreen(req), "Screen created");
    }

    @PutMapping("/screens/{id}")
    public ApiResponse<AdminScreenResponse> updateScreen(@PathVariable Long id, @Valid @RequestBody ScreenUpsertRequest req) {
        return ApiResponse.ok(service.updateScreen(id, req), "Screen updated");
    }

    @PutMapping("/screens/{id}/layout")
    public ApiResponse<AdminScreenResponse> layout(@PathVariable Long id, @Valid @RequestBody SeatLayoutRequest req) {
        return ApiResponse.ok(service.saveLayout(id, req), "Seat layout saved");
    }

    @GetMapping("/shows")
    public ApiResponse<PageResponse<AdminShowResponse>> shows(@RequestParam(required = false) Long cinemaId, @RequestParam(required = false) Long movieId,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.ok(PageResponse.of(service.shows(cinemaId, movieId, date, page, size)));
    }

    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminShowResponse> createShow(@Valid @RequestBody ShowCreateRequest req) {
        return ApiResponse.ok(service.createShow(req), "Show scheduled");
    }

    @PatchMapping("/shows/{id}")
    public ApiResponse<AdminShowResponse> updateShow(@PathVariable Long id, @Valid @RequestBody ShowUpdateRequest req) {
        return ApiResponse.ok(service.updateShow(id, req), "Show updated");
    }

    @GetMapping("/events")
    public ApiResponse<PageResponse<AdminEventResponse>> events(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(PageResponse.of(service.events(page, size)));
    }

    @GetMapping("/events/{id}")
    public ApiResponse<AdminEventResponse> event(@PathVariable Long id) {
        return ApiResponse.ok(service.event(id));
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminEventResponse> createEvent(@Valid @RequestBody EventUpsertRequest req) {
        return ApiResponse.ok(service.createEvent(req), "Event created");
    }

    @PutMapping("/events/{id}")
    public ApiResponse<AdminEventResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody EventUpsertRequest req) {
        return ApiResponse.ok(service.updateEvent(id, req), "Event updated");
    }
}
