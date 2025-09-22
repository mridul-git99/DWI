package com.leucine.streem.dto.projection;

import com.leucine.streem.constant.TaskPauseReason;

public interface TaskPauseResumeAuditView {
    String getId();
    Long getPausedAt();
    Long getResumedAt();
    String getTaskExecutionId();
    TaskPauseReason getReason();
    String getComment();

    String getPausedByFirstName();
    String getPausedByLastName();
    String getPausedByEmployeeId();
    String getPausedByUserId();
    String getResumedByFirstName();
    String getResumedByLastName();
    String getResumedByEmployeeId();
    String getResumedByUserId();
}
