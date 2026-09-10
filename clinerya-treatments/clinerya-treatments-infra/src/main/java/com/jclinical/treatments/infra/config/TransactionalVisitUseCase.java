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
    public Visit registerVisit(UUID actingUserId, UUID patientId, UUID quotationId, UUID clinicId, RegisterVisitCommand command) {
        return visitService.registerVisit(actingUserId, patientId, quotationId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Visit> getVisitsByQuotation(UUID actingUserId, UUID quotationId, UUID patientId, UUID clinicId) {
        return visitService.getVisitsByQuotation(actingUserId, quotationId, patientId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public Visit getVisitDetails(UUID actingUserId, UUID visitId, UUID patientId, UUID clinicId) {
        return visitService.getVisitDetails(actingUserId, visitId, patientId, clinicId);
    }
}
