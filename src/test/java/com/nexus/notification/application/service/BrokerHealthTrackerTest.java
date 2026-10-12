package com.nexus.notification.application.service;

import com.nexus.notification.application.service.BrokerHealthTracker.Transition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrokerHealthTrackerTest {

    @Test
    void startsUp() {
        assertThat(new BrokerHealthTracker(3).isUp()).isTrue();
    }

    @Test
    void staysUpBelowTheFailureThreshold() {
        BrokerHealthTracker tracker = new BrokerHealthTracker(3);

        assertThat(tracker.report(false)).isEqualTo(Transition.NONE);
        assertThat(tracker.report(false)).isEqualTo(Transition.NONE);
        assertThat(tracker.isUp()).isTrue();
    }

    @Test
    void goesDownOnceWhenThresholdIsReached() {
        BrokerHealthTracker tracker = new BrokerHealthTracker(3);
        tracker.report(false);
        tracker.report(false);

        assertThat(tracker.report(false)).isEqualTo(Transition.WENT_DOWN);
        assertThat(tracker.isUp()).isFalse();
        assertThat(tracker.report(false)).isEqualTo(Transition.NONE);
    }

    @Test
    void aSuccessfulCheckResetsTheFailureCount() {
        BrokerHealthTracker tracker = new BrokerHealthTracker(3);
        tracker.report(false);
        tracker.report(false);
        tracker.report(true);

        assertThat(tracker.report(false)).isEqualTo(Transition.NONE);
        assertThat(tracker.isUp()).isTrue();
    }

    @Test
    void recoversOnceWhenABrokerComesBack() {
        BrokerHealthTracker tracker = new BrokerHealthTracker(1);
        tracker.report(false);

        assertThat(tracker.report(true)).isEqualTo(Transition.RECOVERED);
        assertThat(tracker.isUp()).isTrue();
        assertThat(tracker.report(true)).isEqualTo(Transition.NONE);
    }
}
