package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.exception.InvalidJobStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobStateMachineTest {

    private final JobStateMachine jobStateMachine = new JobStateMachine();

    @Test
    void allowsCancellingPendingJob() {
        assertDoesNotThrow(() -> jobStateMachine.assertCanBeCancelled(JobStatus.PENDING));
    }

    @Test
    void allowsCancellingRunningJob() {
        assertDoesNotThrow(() -> jobStateMachine.assertCanBeCancelled(JobStatus.RUNNING));
    }

    @Test
    void rejectsCancellingSucceededJob() {
        assertThrows(InvalidJobStateException.class,
                () -> jobStateMachine.assertCanBeCancelled(JobStatus.SUCCEEDED));
    }

    @Test
    void rejectsCancellingFailedJob() {
        assertThrows(InvalidJobStateException.class,
                () -> jobStateMachine.assertCanBeCancelled(JobStatus.FAILED));
    }

    @Test
    void rejectsCancellingAlreadyCancelledJob() {
        assertThrows(InvalidJobStateException.class,
                () -> jobStateMachine.assertCanBeCancelled(JobStatus.CANCELLED));
    }

    @Test
    void allowsRetryingFailedJob() {
        assertDoesNotThrow(() -> jobStateMachine.assertCanBeRetried(JobStatus.FAILED));
    }

    @Test
    void rejectsRetryingPendingJob() {
        assertThrows(InvalidJobStateException.class,
                () -> jobStateMachine.assertCanBeRetried(JobStatus.PENDING));
    }

    @Test
    void rejectsRetryingRunningJob() {
        assertThrows(InvalidJobStateException.class,
                () -> jobStateMachine.assertCanBeRetried(JobStatus.RUNNING));
    }

    @Test
    void rejectsRetryingSucceededJob() {
        assertThrows(InvalidJobStateException.class,
                () -> jobStateMachine.assertCanBeRetried(JobStatus.SUCCEEDED));
    }

    @Test
    void rejectsRetryingCancelledJob() {
        assertThrows(InvalidJobStateException.class,
                () -> jobStateMachine.assertCanBeRetried(JobStatus.CANCELLED));
    }
}