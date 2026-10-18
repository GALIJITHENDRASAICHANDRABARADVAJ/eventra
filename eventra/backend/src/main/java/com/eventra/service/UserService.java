package com.eventra.service;

import com.eventra.dto.AuthDtos.UpdateProfileRequest;
import com.eventra.dto.AuthDtos.UserResponse;
import com.eventra.entity.Role;
import com.eventra.entity.User;
import com.eventra.exception.ApiException;
import com.eventra.exception.ErrorCode;
import com.eventra.mapper.CatalogMapper;
import com.eventra.repository.CityRepository;
import com.eventra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final CatalogMapper mapper;

    public UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getAvatarUrl(),
                mapper.city(u.getPreferredCity()), u.getRoles().stream().map(Role::getName).sorted().toList(), u.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return toResponse(get(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User u = get(userId);
        u.setFullName(req.fullName().trim());
        u.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        u.setAvatarUrl(req.avatarUrl() == null || req.avatarUrl().isBlank() ? null : req.avatarUrl().trim());
        if (req.preferredCityId() != null) {
            u.setPreferredCity(cityRepository.findById(req.preferredCityId()).orElseThrow(() -> ApiException.notFound("City")));
        }
        return toResponse(u);
    }

    @Transactional
    public UserResponse setPreferredCity(Long userId, Long cityId) {
        User u = get(userId);
        u.setPreferredCity(cityRepository.findById(cityId).orElseThrow(() -> ApiException.notFound("City")));
        return toResponse(u);
    }

    User get(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
