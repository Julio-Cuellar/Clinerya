package com.jclinical.auth.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SpringDataRevokedTokenRepository extends JpaRepository<RevokedTokenEntity, String> {

    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    @Modifying
    @Query("delete from RevokedTokenEntity t where t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
