package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.exception.InvalidJobStateException;
import org.springframework.stereotype.Component;

@Component
public class JobStateMachine {

    public void assertCanBeCancelled(JobStatus currentStatus) {
        if (currentStatus != JobStatus.PENDING && currentStatus != JobStatus.RUNNING) {
            throw new InvalidJobStateException(
                    "'" + currentStatus + "' durumundaki bir iş iptal edilemez");
        }
    }

    public void assertCanBeRetried(JobStatus currentStatus) {
        if (currentStatus != JobStatus.FAILED) {
            throw new InvalidJobStateException(
                    "'" + currentStatus + "' durumundaki bir iş tekrar çalıştırılamaz, yalnızca FAILED işler retry edilebilir");
        }
    }
}