package com.jclinical.collaboration.infra.adapters.out.persistence;

import com.jclinical.collaboration.domain.model.ExternalAccessGrant;

public interface ExternalAccessGrantMapper {

    ExternalAccessGrantEntity toEntity(ExternalAccessGrant domain);

    ExternalAccessGrant toDomain(ExternalAccessGrantEntity entity);
}
