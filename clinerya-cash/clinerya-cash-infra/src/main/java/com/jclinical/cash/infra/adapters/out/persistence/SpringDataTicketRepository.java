package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.PaymentMethod;
import com.jclinical.cash.domain.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTicketRepository extends JpaRepository<TicketEntity, UUID> {

    Optional<TicketEntity> findByIdAndClinicId(UUID id, UUID clinicId);

    List<TicketEntity> findByCashSessionIdAndClinicId(UUID cashSessionId, UUID clinicId);

    @Query("select t from TicketEntity t where t.clinicId = :clinicId "
            + "and t.createdAt >= :from and t.createdAt < :to "
            + "order by t.createdAt asc")
    List<TicketEntity> findByClinicIdAndRange(
            @Param("clinicId") UUID clinicId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<TicketEntity> findByQuotationIdAndClinicId(UUID quotationId, UUID clinicId);

    List<TicketEntity> findByPatientIdAndClinicIdOrderByCreatedAtDesc(UUID patientId, UUID clinicId);

    @Query("select coalesce(sum(pl.amount), 0) from TicketEntity t join t.paymentLines pl "
            + "where t.cashSessionId = :cashSessionId and t.status = :status and pl.method = :method")
    BigDecimal sumActiveCashAmountBySession(
            @Param("cashSessionId") UUID cashSessionId,
            @Param("status") TicketStatus status,
            @Param("method") PaymentMethod method);

    @Query("select coalesce(sum(t.totalAmount + coalesce(t.discountAmount, 0)), 0) from TicketEntity t "
            + "where t.quotationId = :quotationId and t.clinicId = :clinicId and t.status = :status")
    BigDecimal sumActiveAmountByQuotation(
            @Param("quotationId") UUID quotationId,
            @Param("clinicId") UUID clinicId,
            @Param("status") TicketStatus status);
}
