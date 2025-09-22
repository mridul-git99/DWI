package com.leucine.streem.model.helper.parameter;

import lombok.Data;

@Data
public class CalculationParameterVariableChoices {
    private String taskId;
    private String taskExecutionId;
    private String parameterId;
    private String parameterValueId;
    private String value;
}
