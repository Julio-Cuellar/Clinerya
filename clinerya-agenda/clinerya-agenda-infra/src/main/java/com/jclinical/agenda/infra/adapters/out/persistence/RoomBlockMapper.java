package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.RoomBlock;
import com.jclinical.agenda.infra.adapters.in.web.dto.RoomBlockResponse;
public interface RoomBlockMapper {
    RoomBlockEntity toEntity(RoomBlock domain);
    RoomBlock toDomain(RoomBlockEntity entity);
    RoomBlockResponse toResponse(RoomBlock domain);
}
