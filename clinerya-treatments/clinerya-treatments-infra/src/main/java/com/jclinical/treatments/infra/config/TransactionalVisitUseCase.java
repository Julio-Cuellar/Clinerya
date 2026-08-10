package com.jclinical.treatments.infra.config;

import com.jclinical.treatments.domain.model.Visit;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase;
import com.jclinical.treatments.domain.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalVisitUseCase implements ManageVisitsUseCase {

    private final VisitService visitService;

    @Override
    @Transactional
    public Visit registerVisit(UUID patientId, UUID quotationId, UUID clinicId, RegisterVisitCommand command) {
        return visitService.registerVisit(patientId, quotationId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Visit> getVisitsByQuotation(UUID quotationId, UUID patientId, UUID clinicId) {
        return visitService.getVisitsByQuotation(quotationId, patientId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public Visit getVisitDetails(UUID visitId, UUID patientId, UUID clinicId) {
        return visitService.getVisitDetails(visitId, patientId, clinicId);
    }
}
