package com.jclinical.records.domain.service;

import com.jclinical.records.domain.model.Icd10Code;
import com.jclinical.records.domain.ports.out.Icd10CatalogRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Icd10CatalogServiceTest {

    @Mock
    private Icd10CatalogRepositoryPort repository;

    private Icd10CatalogService service;

    @BeforeEach
    void setUp() {
        service = new Icd10CatalogService(repository);
    }

    @Test
    void searchReturnsEmptyForBlankTerm() {
        assertThat(service.search("  ", 20)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void searchReturnsEmptyForTermShorterThanTwoChars() {
        assertThat(service.search("k", 20)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void searchTrimsTermAndDelegatesToRepository() {
        Icd10Code pulpitis = Icd10Code.builder().code("K04.0").description("Pulpitis").billable(true).build();
        when(repository.search("pulpitis", 20)).thenReturn(List.of(pulpitis));

        List<Icd10Code> result = service.search("  pulpitis  ", 20);

        assertThat(result).containsExactly(pulpitis);
        verify(repository).search("pulpitis", 20);
    }

    @Test
    void searchClampsLimitToDefaultWhenOutOfRange() {
        when(repository.search(anyString(), anyInt())).thenReturn(List.of());

        service.search("caries", 0);
        service.search("caries", 500);

        verify(repository, never()).search(anyString(), org.mockito.ArgumentMatchers.eq(0));
        verify(repository, never()).search(anyString(), org.mockito.ArgumentMatchers.eq(500));
    }
}
