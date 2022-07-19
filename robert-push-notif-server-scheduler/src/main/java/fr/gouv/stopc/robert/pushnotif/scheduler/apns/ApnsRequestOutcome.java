package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

/**
 * Represents the possible outcomes of a request to an APNS. ACCEPTED and
 * REJECTED represent the possible outcomes when the app managed to get the
 * response from the server. ERROR represents the outcome when the app did not
 * manage to reach the server.
 */
public enum ApnsRequestOutcome {
    ACCEPTED,
    REJECTED,
    ERROR
}
