package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.BrokerHealthProbePort;
import com.nexus.notification.application.port.out.MessageConsumptionControlPort;
import com.nexus.notification.application.service.BrokerHealthTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CheckBrokerHealthUseCaseTest {

    private BrokerHealthProbePort probe;
    private MessageConsumptionControlPort consumption;
    private RecordNotificationUseCase record;
    private CheckBrokerHealthUseCase useCase;

    @BeforeEach
    void setUp() {
        probe = mock(BrokerHealthProbePort.class);
        consumption = mock(MessageConsumptionControlPort.class);
        record = mock(RecordNotificationUseCase.class);
        useCase = new CheckBrokerHealthUseCase(probe, new BrokerHealthTracker(2), record, consumption);
    }

    @Test
    void healthyBrokerChangesNothing() {
        when(probe.isHealthy()).thenReturn(true);

        useCase.check();

        verifyNoInteractions(consumption, record);
        assertThat(useCase.isBrokerUp()).isTrue();
    }

    @Test
    void aSingleFailureIsNotAnOutage() {
        when(probe.isHealthy()).thenReturn(false);

        useCase.check();

        verifyNoInteractions(consumption, record);
        assertThat(useCase.isBrokerUp()).isTrue();
    }

    @Test
    void confirmedFailurePausesConsumptionAndRecordsAnEvent() {
        when(probe.isHealthy()).thenReturn(false);

        useCase.check();
        useCase.check();

        verify(consumption).pause();
        verify(record).record(any(), eq("MessageBrokerUnavailable"), eq("kafka"), any(), any(), eq(null));
        assertThat(useCase.isBrokerUp()).isFalse();
    }

    @Test
    void recoveryResumesConsumptionAndRecordsAnEvent() {
        when(probe.isHealthy()).thenReturn(false, false, true);

        useCase.check();
        useCase.check();
        useCase.check();

        verify(consumption).resume();
        verify(record).record(any(), eq("MessageBrokerRecovered"), eq("kafka"), any(), any(), eq(null));
        assertThat(useCase.isBrokerUp()).isTrue();
    }
}
