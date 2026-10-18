package com.eventra.service;

import com.eventra.dto.CatalogDtos.*;
import com.eventra.entity.City;
import com.eventra.exception.ApiException;
import com.eventra.mapper.CatalogMapper;
import com.eventra.repository.CinemaRepository;
import com.eventra.repository.CinemaScreenRepository;
import com.eventra.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CinemaService {
    private final CinemaRepository cinemaRepository;
    private final CinemaScreenRepository screenRepository;
    private final VenueRepository venueRepository;
    private final CatalogService catalogService;
    private final CatalogMapper mapper;

    public List<CinemaSummary> list(String citySlug, Double lat, Double lng) {
        City city = catalogService.resolveCity(citySlug);
        if (city == null) throw new ApiException(com.eventra.exception.ErrorCode.BAD_REQUEST, "city is required");
        return cinemaRepository.findByCityIdAndActiveTrueOrderByName(city.getId()).stream()
                .map(c -> mapper.cinema(c, CatalogMapper.distanceKm(lat, lng, c.getLatitude(), c.getLongitude())))
                .sorted(Comparator.comparing((CinemaSummary c) -> c.distanceKm() == null ? Double.MAX_VALUE : c.distanceKm()).thenComparing(CinemaSummary::name))
                .toList();
    }

    public CinemaDetailResponse detail(Long id) {
        var c = cinemaRepository.findById(id).filter(x -> x.isActive()).orElseThrow(() -> ApiException.notFound("Cinema"));
        return new CinemaDetailResponse(mapper.cinema(c), screenRepository.findByCinemaIdOrderByName(id).stream()
                .filter(s -> s.isActive()).map(mapper::screen).toList());
    }

    public VenueSummary venue(Long id) {
        return venueRepository.findById(id).filter(v -> v.isActive()).map(mapper::venue).orElseThrow(() -> ApiException.notFound("Venue"));
    }

    public List<VenueSummary> venues(String citySlug) {
        City city = catalogService.resolveCity(citySlug);
        if (city == null) throw new ApiException(com.eventra.exception.ErrorCode.BAD_REQUEST, "city is required");
        return venueRepository.findByCityIdAndActiveTrueOrderByName(city.getId()).stream().map(mapper::venue).toList();
    }
}
