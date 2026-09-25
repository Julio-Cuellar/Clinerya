package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.WebhookPayload;

/** Lee el JSON de Meta. Un cuerpo ilegible devuelve un payload vacio. */
public interface WebhookPayloadParserPort {

    WebhookPayload parse(byte[] rawBody);
}
