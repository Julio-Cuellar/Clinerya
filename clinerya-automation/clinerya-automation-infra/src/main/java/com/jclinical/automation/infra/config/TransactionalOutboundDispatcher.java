package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.ports.in.DispatchOutboundMessagesUseCase;
import com.jclinical.automation.domain.service.OutboundDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** El bloqueo del mensaje (SKIP LOCKED) dura lo que su envio y el registro del resultado. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalOutboundDispatcher implements DispatchOutboundMessagesUseCase {

    private final OutboundDispatcher dispatcher;

    @Override
    @Transactional
    public boolean dispatchNext() {
        return dispatcher.dispatchNext();
    }
}
