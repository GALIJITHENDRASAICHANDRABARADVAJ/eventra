package com.eventra.admin;

import com.eventra.exception.ApiException;
import com.eventra.exception.ErrorCode;
import com.eventra.repository.CinemaRepository;
import com.eventra.security.SecurityUtils;
import com.eventra.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Backend authorization for the admin area. ADMIN can do everything; PARTNER is limited to the cinemas
 * linked to its partner account. The frontend route guard is only UX — this is the real boundary.
 */
@Component
@RequiredArgsConstructor
public class AdminAccess {
    private final CinemaRepository cinemaRepository;

    public UserPrincipal requireAdmin() {
        UserPrincipal u = SecurityUtils.requireUser();
        if (!u.isAdmin()) throw new ApiException(ErrorCode.FORBIDDEN);
        return u;
    }

    public void requireCinemaAccess(Long cinemaId) {
        UserPrincipal u = SecurityUtils.requireUser();
        if (u.isAdmin()) return;
        if (u.hasRole("PARTNER") && cinemaRepository.cinemaIdsForPartnerUser(u.id()).contains(cinemaId)) return;
        throw new ApiException(ErrorCode.FORBIDDEN, "You can only manage your own cinemas");
    }

    /** null = unrestricted (admin); otherwise the partner's cinema ids. */
    public List<Long> scopedCinemaIds() {
        UserPrincipal u = SecurityUtils.requireUser();
        return u.isAdmin() ? null : cinemaRepository.cinemaIdsForPartnerUser(u.id());
    }
}
