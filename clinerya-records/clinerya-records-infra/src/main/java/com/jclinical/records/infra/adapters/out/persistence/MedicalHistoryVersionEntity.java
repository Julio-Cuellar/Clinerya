package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.infra.adapters.out.persistence.security.AesCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "medical_history_versions", 
    schema = "records",
    uniqueConstraints = @UniqueConstraint(columnNames = { "medical_history_id", "version" })
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryVersionEntity {

    @Id
    private UUID id;

    @Column(name = "medical_history_id", nullable = false)
    private UUID medicalHistoryId;

    @Column(nullable = false)
    private int version;

    @Column(name = "answers_json", columnDefinition = "TEXT", nullable = false)
    @Convert(converter = AesCryptoConverter.class)
    private String answersJson;

    @Column(name = "changed_by_user_id", nullable = false)
    private UUID changedByUserId;

    @Column(name = "changed_by_user_name", nullable = false)
    private String changedByUserName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
