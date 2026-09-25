package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.ports.in.DispatchOutboundMessagesUseCase;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.automation.domain.ports.out.CustomerServiceWindowPort;
import com.jclinical.automation.domain.ports.out.OutboundDispatchRepositoryPort;
import com.jclinical.automation.domain.ports.out.WhatsAppSenderPort;

import java.time.Clock;

public class OutboundDispatcher implements DispatchOutboundMessagesUseCase {

    static final int MAX_ATTEMPTS = 5;

    public OutboundDispatcher(OutboundDispatchRepositoryPort queue, ChannelSettingsRepositoryPort settings,
                              CustomerServiceWindowPort window, WhatsAppSenderPort sender, Clock clock) {
    }

    @Override
    public boolean dispatchNext() {
        throw new UnsupportedOperationException("pendiente");
    }
}
