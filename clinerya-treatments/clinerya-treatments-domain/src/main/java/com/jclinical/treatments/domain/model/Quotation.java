package com.jclinical.treatments.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quotation {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private UUID createdByUserId;
    private LocalDate quotationDate;
    private QuotationStatus status;
    private String notes;
    private LocalDate validUntil;
    @Builder.Default
    private List<QuotationItem> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal grandTotal() {
        return items.stream()
                .map(QuotationItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void send() {
        if (status != QuotationStatus.DRAFT) {
            throw new IllegalStateException("Solo una cotización en borrador puede enviarse.");
        }
        status = QuotationStatus.SENT;
        updatedAt = LocalDateTime.now();
    }

    public void accept() {
        if (status != QuotationStatus.SENT) {
            throw new IllegalStateException("Solo una cotización enviada puede aceptarse.");
        }
        status = QuotationStatus.ACCEPTED;
        updatedAt = LocalDateTime.now();
    }

    public void reject() {
        if (status != QuotationStatus.SENT) {
            throw new IllegalStateException("Solo una cotización enviada puede rechazarse.");
        }
        status = QuotationStatus.REJECTED;
        updatedAt = LocalDateTime.now();
    }

    public void expire() {
        if (status != QuotationStatus.SENT && status != QuotationStatus.DRAFT) {
            throw new IllegalStateException("Solo una cotización en borrador o enviada puede marcarse como vencida.");
        }
        status = QuotationStatus.EXPIRED;
        updatedAt = LocalDateTime.now();
    }

    public void ensureEditable() {
        if (status != QuotationStatus.DRAFT) {
            throw new IllegalStateException("No se puede modificar una cotización que ya fue enviada.");
        }
    }

    public void replaceItems(List<QuotationItem> newItems) {
        ensureEditable();
        this.items = new ArrayList<>(newItems);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateItemProgress(UUID itemId, ItemProgressStatus targetStatus) {
        QuotationItem item = items.stream()
                .filter(existing -> existing.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La partida no existe en esta cotización."));
        item.setProgressStatus(targetStatus);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateHeader(String notes, LocalDate validUntil, LocalDate quotationDate) {
        ensureEditable();
        this.notes = notes;
        this.validUntil = validUntil;
        this.quotationDate = quotationDate;
        this.updatedAt = LocalDateTime.now();
    }
}
