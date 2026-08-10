package com.jclinical.attachments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAttachmentRepository extends JpaRepository<AttachmentEntity, UUID> {

    List<AttachmentEntity> findByPatientIdAndClinicIdAndElementIdOrderByCreatedAtAsc(UUID patientId, UUID clinicId, String elementId);

    List<AttachmentEntity> findByPatientIdAndElementIdOrderByCreatedAtAsc(UUID patientId, String elementId);

    Optional<AttachmentEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
