package com.jclinical.accounting.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataJournalEntryRepository extends JpaRepository<JournalEntryEntity, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);

    List<JournalEntryEntity> findByClinicIdOrderByCreatedAtDesc(UUID clinicId);

    List<JournalEntryEntity> findByClinicIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
            UUID clinicId,
            LocalDate from,
            LocalDate to);

    @Query("""
            select distinct e
            from JournalEntryEntity e
            join fetch e.lines l
            where e.clinicId = :clinicId
              and l.bankAccountId = :bankAccountId
            order by e.entryDate asc, e.createdAt asc
            """)
    List<JournalEntryEntity> findByClinicIdAndBankAccountId(
            @Param("clinicId") UUID clinicId,
            @Param("bankAccountId") UUID bankAccountId);

    @Query(value = """
            select distinct e
            from JournalEntryEntity e
            left join e.lines l
            where e.clinicId = :clinicId
              and e.entryDate >= :from
              and e.entryDate <= :to
              and (:sourceEventType = '' or e.sourceEventType = :sourceEventType)
              and (:search = ''
                   or lower(coalesce(e.description, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(e.sourceEventType, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(l.accountCode, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(l.accountName, '')) like lower(concat('%', :search, '%')))
            order by e.entryDate desc, e.createdAt desc
            """,
            countQuery = """
            select count(distinct e.id)
            from JournalEntryEntity e
            left join e.lines l
            where e.clinicId = :clinicId
              and e.entryDate >= :from
              and e.entryDate <= :to
              and (:sourceEventType = '' or e.sourceEventType = :sourceEventType)
              and (:search = ''
                   or lower(coalesce(e.description, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(e.sourceEventType, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(l.accountCode, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(l.accountName, '')) like lower(concat('%', :search, '%')))
            """)
    Page<JournalEntryEntity> search(
            @Param("clinicId") UUID clinicId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("search") String search,
            @Param("sourceEventType") String sourceEventType,
            Pageable pageable);

    @Query("""
            select coalesce(sum(l.debit), 0), coalesce(sum(l.credit), 0)
            from JournalEntryEntity e
            join e.lines l
            where e.clinicId = :clinicId
              and e.entryDate >= :from
              and e.entryDate <= :to
              and (:sourceEventType = '' or e.sourceEventType = :sourceEventType)
              and (:search = ''
                   or lower(coalesce(e.description, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(e.sourceEventType, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(l.accountCode, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(l.accountName, '')) like lower(concat('%', :search, '%')))
            """)
    List<Object[]> totals(
            @Param("clinicId") UUID clinicId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("search") String search,
            @Param("sourceEventType") String sourceEventType);
}
