package com.nexus.notification.infrastructure.messaging;

import com.nexus.common.core.exception.DomainException;

// A record on the topic that isn't a valid event envelope. Extends DomainException so
// KafkaErrorHandlingConfig treats it as not retryable: it goes straight to the DLT.
public class MalformedEventException extends DomainException {
    public MalformedEventException(String rawPayload, Throwable cause) {
        super("MALFORMED_EVENT", "Cannot parse event payload: " + rawPayload);
        initCause(cause);
    }
}
