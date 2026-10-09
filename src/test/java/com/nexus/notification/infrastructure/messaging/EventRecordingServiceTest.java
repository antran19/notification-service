package com.nexus.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.DomainEvent;
import com.nexus.common.events.UserRegisteredEvent;
import com.nexus.notification.application.service.NotificationMessageResolver;
import com.nexus.notification.application.usecase.CacheUserEmailUseCase;
import com.nexus.notification.application.usecase.RecordNotificationUseCase;
import com.nexus.notification.application.usecase.SendNotificationEmailUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventRecordingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();
    private final RecordNotificationUseCase recordNotificationUseCase = mock(RecordNotificationUseCase.class);
    private final CacheUserEmailUseCase cacheUserEmailUseCase = mock(CacheUserEmailUseCase.class);
    private final SendNotificationEmailUseCase sendNotificationEmailUseCase = mock(SendNotificationEmailUseCase.class);
    private final EventRecordingService service = new EventRecordingService(objectMapper, recordNotificationUseCase,
            cacheUserEmailUseCase, sendNotificationEmailUseCase);

    @Test
    void process_malformedJson_doesNotThrow_andDoesNotCallUseCase() {
        assertThatCode(() -> service.process("{not valid json", Map.of()))
                .doesNotThrowAnyException();

        verify(recordNotificationUseCase, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void process_validJsonMissingEventId_doesNotThrow_andDoesNotCallUseCase() {
        String payload = "{\"eventType\":\"SomethingUnmapped\",\"occurredAt\":\"2026-01-01T00:00:00Z\"}";

        assertThatCode(() -> service.process(payload, Map.of()))
                .doesNotThrowAnyException();

        verify(recordNotificationUseCase, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void process_validUnmappedEvent_callsUseCaseWithNullTypedEvent() {
        String payload = "{\"eventId\":\"e-1\",\"eventType\":\"ProductCreated\","
                + "\"aggregateId\":\"product-1\",\"occurredAt\":\"2026-01-01T00:00:00Z\"}";
        when(recordNotificationUseCase.record(any(), any(), any(), any(), any(), any()))
                .thenReturn(NotificationMessageResolver.Resolution.UNMAPPED);

        service.process(payload, Map.of());

        ArgumentCaptor<DomainEvent> typedEventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(recordNotificationUseCase).record(eq("e-1"), eq("ProductCreated"), eq("product-1"),
                eq(Instant.parse("2026-01-01T00:00:00Z")), eq(payload), typedEventCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(typedEventCaptor.getValue()).isNull();
        verify(sendNotificationEmailUseCase, never()).send(any(), any());
    }

    @Test
    void process_useCaseThrowsDataAccessException_propagatesOutOfProcess() {
        String payload = "{\"eventId\":\"e-2\",\"eventType\":\"ProductCreated\","
                + "\"aggregateId\":\"product-2\",\"occurredAt\":\"2026-01-01T00:00:00Z\"}";
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(recordNotificationUseCase).record(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.process(payload, Map.of()))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void process_resolvedEventWithRecipient_sendsNotificationEmail() {
        String payload = "{\"eventId\":\"e-3\",\"eventType\":\"AuctionWon\","
                + "\"aggregateId\":\"auction-1\",\"occurredAt\":\"2026-01-01T00:00:00Z\"}";
        when(recordNotificationUseCase.record(any(), any(), any(), any(), any(), any()))
                .thenReturn(new NotificationMessageResolver.Resolution("winner-1", "You won!"));

        service.process(payload, Map.of());

        verify(sendNotificationEmailUseCase).send("winner-1", "You won!");
    }

    @Test
    void process_userRegisteredEvent_cachesTheEmail() {
        String payload = "{\"eventId\":\"e-4\",\"eventType\":\"UserRegistered\",\"aggregateId\":\"user-1\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"userId\":\"user-1\","
                + "\"email\":\"alice@example.com\",\"fullName\":\"Alice\"}";
        when(recordNotificationUseCase.record(any(), any(), any(), any(), any(), any()))
                .thenReturn(NotificationMessageResolver.Resolution.UNMAPPED);

        service.process(payload, Map.of("UserRegistered", UserRegisteredEvent.class));

        verify(cacheUserEmailUseCase).cache("user-1", "alice@example.com");
    }
}
