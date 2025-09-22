package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionDto implements Serializable {
    private static final long serialVersionUID = -3026728201574430753L;
    private String id;
    private String status;
    private String initiatorsReason;
    private String correctorsReason;
    private String reviewersReason;
    private List<CorrectorDto> corrector;
    private List<ReviewerDto> reviewer;
    private String oldValue;
    private String newValue;
    private JsonNode oldChoices;
    private JsonNode newChoices;
    private String code;
    private Long createdAt;
    private UserAuditDto createdBy;
    private String jobId;
    private String taskExecutionId;
    private String processName;
    private String parameterName;
    private String jobCode;
    private String taskName;
    private String parameterId;
    private List<MediaDto> oldMedias;
    private List<MediaDto> newMedias;

}
