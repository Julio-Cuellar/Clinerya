package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;

import java.util.List;

public interface HandleInboundMessageUseCase {
    List<OutboundReply> handle(InboundMessage message);
}
