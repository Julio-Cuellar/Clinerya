package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataIcd10CatalogRepository extends JpaRepository<Icd10CodeEntity, String> {

    @Query("SELECT c FROM Icd10CodeEntity c "
            + "WHERE c.billable = true "
            + "AND (LOWER(c.code) LIKE LOWER(CONCAT(:term, '%')) "
            + "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :term, '%'))) "
            + "ORDER BY c.code")
    List<Icd10CodeEntity> search(@Param("term") String term, Pageable pageable);
}
