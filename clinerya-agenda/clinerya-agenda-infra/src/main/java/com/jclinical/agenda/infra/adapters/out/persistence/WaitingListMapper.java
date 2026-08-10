package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.WaitingListEntry;
import com.jclinical.agenda.infra.adapters.in.web.dto.WaitingListResponse;
public interface WaitingListMapper {
    WaitingListEntity toEntity(WaitingListEntry domain);
    WaitingListEntry toDomain(WaitingListEntity entity);
    WaitingListResponse toResponse(WaitingListEntry domain);
}
