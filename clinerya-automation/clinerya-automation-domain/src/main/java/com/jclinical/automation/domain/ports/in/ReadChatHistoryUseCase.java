package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.ChatAccess;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Historial de WhatsApp (D9): lo ven medicos y recepcion (VIEW_PATIENTS); cada lectura de un chat se
 * audita; la auditoria la consulta el administrador (MANAGE_CLINIC).
 */
public interface ReadChatHistoryUseCase {

    List<ChatSummary> listChats(UUID actingUserId, UUID clinicId);

    List<ChatMessage> readMessages(UUID actingUserId, UUID clinicId, String phone, LocalDateTime before, Integer limit);

    List<ChatAccess> accessLog(UUID actingUserId, UUID clinicId, String phone, UUID userId, LocalDateTime from, LocalDateTime to);
}
