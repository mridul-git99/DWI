package com.leucine.streem.dto.projection;

/**
 * Projection interface for job pending tasks count.
 * Used to get the total count of pending tasks for each job.
 */
public interface JobPendingTaskCountProjection {
    Long getJobId();
    Long getPendingCount();
}
