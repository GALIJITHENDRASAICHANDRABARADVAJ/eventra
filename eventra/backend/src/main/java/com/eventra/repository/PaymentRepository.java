package com.eventra.repository;

import com.eventra.entity.Payment;
import com.eventra.entity.enums.PaymentProviderType;
import com.eventra.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findForUpdate(@Param("id") Long id);

    Optional<Payment> findByProviderAndProviderOrderId(PaymentProviderType provider, String providerOrderId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    List<Payment> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Payment> findFirstByBookingIdAndStatusOrderByCreatedAtDesc(Long bookingId, PaymentStatus status);

    Optional<Payment> findFirstByBookingIdAndStatusInOrderByCreatedAtDesc(Long bookingId, Collection<PaymentStatus> statuses);
}
