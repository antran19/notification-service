package com.nexus.notification.application.service;

// Debounces raw probe results into UP/DOWN: a single failed check is not an outage (SRS 3.2.2
// "retry the check ... upon confirmed failure"), so the broker only goes DOWN after
// failureThreshold consecutive failures. Not thread-safe; one scheduled job drives it.
public class BrokerHealthTracker {

    public enum Transition { NONE, WENT_DOWN, RECOVERED }

    private final int failureThreshold;
    private int consecutiveFailures;
    private boolean up = true;

    public BrokerHealthTracker(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public Transition report(boolean healthy) {
        if (healthy) {
            consecutiveFailures = 0;
            if (!up) {
                up = true;
                return Transition.RECOVERED;
            }
            return Transition.NONE;
        }
        consecutiveFailures++;
        if (up && consecutiveFailures >= failureThreshold) {
            up = false;
            return Transition.WENT_DOWN;
        }
        return Transition.NONE;
    }

    public boolean isUp() {
        return up;
    }
}
