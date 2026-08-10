package com.jclinical.cash.infra.adapters.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "cash_folio_counters", schema = "cash")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFolioCounterEntity {

    @Id
    private UUID clinicId;

    private int lastFolio;
}
