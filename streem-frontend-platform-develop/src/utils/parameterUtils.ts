import {
  AutomationAction,
  AutomationActionActionType,
  AutomationActionActionTypeVisual,
  ParameterExceptionTypeEnum,
  TriggerTypeEnum,
} from '#PrototypeComposer/checklist.types';
import {
  Checklist,
  MandatoryParameter,
  NonMandatoryParameter,
  Parameter,
  ParameterCorrectionStatus,
  ParameterResponse,
  ParameterVariationType,
  Selections,
  Stage,
  TExecutedAutomation,
  Task,
  TaskExecution,
} from '#types';
import { labelByConstraint } from '#utils';
import { camelCase, startCase } from 'lodash';
import { ComparisonOperator, InputTypes } from './globalTypes';
import { generateShouldBeCriteria } from './stringUtils';
import { compareEpochs, formatDateTime } from './timeUtils';
import { ParameterLabelByType } from '#PrototypeComposer/constants';
import { ParameterExceptionState } from '#views/Jobs/ListView/types';

export const responseDetailsForChoiceBasedParameters = ({ data, response }: any) => {
  let detailList: any[] = [];
  data?.forEach((currData: any) => {
    if (response?.choices?.[currData.id] === 'SELECTED') {
      return detailList.push(
        `${currData.name}${response.reason ? ` : Remarks - ${response.reason}` : ''}`,
      );
    }
  });
  return detailList.join(', ');
};

export const DEFAULT_VALUE = 'N/A ';

export const getParameterContent = (parameter: any, format?: string, timezone?: string) => {
  let parameterContent;

  switch (parameter.type) {
    case MandatoryParameter.SHOULD_BE:
    case MandatoryParameter.MULTI_LINE:
    case MandatoryParameter.SINGLE_LINE:
    case MandatoryParameter.NUMBER:
      parameterContent = parameter.response.value;
      break;
    case MandatoryParameter.DATE:
    case MandatoryParameter.DATE_TIME:
      parameterContent = parameter.response?.value
        ? formatDateTime({
            value: parameter.response.value,
            type:
              parameter.type === MandatoryParameter.DATE ? InputTypes.DATE : InputTypes.DATE_TIME,
            format,
            timezone,
          })
        : DEFAULT_VALUE;
      break;
    case MandatoryParameter.YES_NO:
    case MandatoryParameter.CHECKLIST:
    case MandatoryParameter.SINGLE_SELECT:
    case MandatoryParameter.MULTISELECT:
      parameterContent = responseDetailsForChoiceBasedParameters(parameter);
      break;
    case MandatoryParameter.RESOURCE:
    case MandatoryParameter.MULTI_RESOURCE:
      parameterContent = (parameter.response.choices || [])
        .map((c: any) => `${c.objectDisplayName} (ID: ${c.objectExternalId})`)
        .join(', ');
      break;
    case MandatoryParameter.FILE_UPLOAD:
    case MandatoryParameter.MEDIA:
    case MandatoryParameter.SIGNATURE:
      parameterContent = parameter.response?.medias
        ?.map((media: any) => media.name || media.filename)
        .join(', ');
      break;
    default:
      return;
  }

  return parameterContent ? parameterContent : DEFAULT_VALUE;
};

export const ObjectIdsDataFromChoices = (choices: any) => {
  let data: string[] = [];
  if (choices?.length > 0) {
    choices?.forEach((choice: any) => {
      data.push(choice.objectId);
    });
    return data;
  } else {
    return null;
  }
};

export const getAutomationActionTexts = ({
  automation,
  forNotify,
  parameterRefData,
  executedAutomationObject,
  isExecuted,
  parameter,
}: {
  automation: AutomationAction;
  forNotify: 'success' | 'error' | 'warning' | null;
  parameterRefData: any;
  executedAutomationObject?: TExecutedAutomation;
  isExecuted?: boolean;
  parameter?: any;
}) => {
  const objectTypeDisplayName =
    automation?.actionDetails?.objectTypeDisplayName ||
    parameterRefData?.data?.objectTypeDisplayName;
  const selectorType = automation?.actionDetails?.selector?.toLowerCase() || 'parameter';
  const automationActionType = AutomationActionActionTypeVisual[automation.actionType];
  const automationPropertyName = automation?.actionDetails?.propertyDisplayName || '';
  const targetEntityTypeDisplayName = startCase(
    camelCase(automation?.targetEntityType?.split('_').join(' ')),
  );
  const parameterRefLabel = parameterRefData?.label || '';
  const parameterLabel = parameter?.label || '';
  const displayValue =
    selectorType === 'constant' ? automation?.actionDetails?.value : parameterLabel;

  if (forNotify === 'success') {
    if (automation.actionType === AutomationActionActionType.CREATE_OBJECT) {
      return `Triggered "${automationActionType} ${automationPropertyName} of the selected ${objectTypeDisplayName} with ${executedAutomationObject?.propertyDisplayName}: ${executedAutomationObject?.createObjectAutomationResponseDto?.[0]?.createdObjectId}"`;
    }
    if (automation.actionType === AutomationActionActionType.BULK_CREATE_OBJECT) {
      const bulkCount = automation?.actionDetails?.bulkCount || 'multiple';
      const countText = selectorType === 'parameter' ? 'based on parameter value' : bulkCount;
      return `Triggered "Bulk Create ${countText} ${objectTypeDisplayName}(s)"`;
    }
    return `Triggered "${automationActionType} ${automationPropertyName} of the selected ${objectTypeDisplayName}"`;
  } else if (forNotify === 'error') {
    return `Not able to trigger "${automationActionType} ${automationPropertyName} of the selected ${objectTypeDisplayName}"`;
  } else if (forNotify === 'warning') {
    return `Skipped "${automationActionType} ${automationPropertyName} of the selected ${objectTypeDisplayName}"`;
  }
  if (isExecuted) {
    switch (automation.actionType) {
      case AutomationActionActionType.CREATE_OBJECT:
        return `Create Object : A new ${objectTypeDisplayName} was added with ID: ${automation?.actionDetails?.objectTypeExternalId}`;
      case AutomationActionActionType.BULK_CREATE_OBJECT:
        const bulkCount = automation?.actionDetails?.bulkCount || 'multiple';
        const countText = selectorType === 'parameter' ? 'based on parameter value' : bulkCount;
        return `Bulk Create Objects : ${countText} new ${objectTypeDisplayName}(s) were created`;
      case AutomationActionActionType.INCREASE_PROPERTY:
        return `Increase ${targetEntityTypeDisplayName} : Increase ${objectTypeDisplayName} of ${parameterRefLabel} was increased by the value in the ${displayValue} ${selectorType}`;
      case AutomationActionActionType.DECREASE_PROPERTY:
        return `Decrease ${targetEntityTypeDisplayName} : Decrease ${objectTypeDisplayName} of ${parameterRefLabel} was decreased by the value in the ${displayValue} ${selectorType}`;
      case AutomationActionActionType.ARCHIVE_OBJECT:
        return `Archive a ${targetEntityTypeDisplayName} : A new ${objectTypeDisplayName} with ID: ${automation?.actionDetails?.objectTypeExternalId} was archived.`;
      case AutomationActionActionType.SET_PROPERTY:
        return `Update ${startCase(
          camelCase(automation.actionDetails?.captureProperty?.split('_').join(' ')),
        )} : Set ${objectTypeDisplayName} of ${parameterRefLabel} to ${automationPropertyName}`;
      case AutomationActionActionType.SET_RELATION:
        return `Set ${targetEntityTypeDisplayName} : ${automation?.actionDetails?.relationDisplayName} of ${parameterRefLabel} was set as the resource selected in the ${parameterLabel}`;
    }
  } else {
    switch (automation.actionType) {
      case AutomationActionActionType.CREATE_OBJECT:
        return `Create Object : Create/Add a new ${objectTypeDisplayName}`;
      case AutomationActionActionType.BULK_CREATE_OBJECT:
        const bulkCount = automation?.actionDetails?.bulkCount || 'multiple';
        const countText = selectorType === 'parameter' ? 'based on parameter value' : bulkCount;
        return `Bulk Create Objects : Create ${countText} new ${objectTypeDisplayName}(s)`;
      case AutomationActionActionType.INCREASE_PROPERTY:
        return `Increase ${targetEntityTypeDisplayName} : Increase ${objectTypeDisplayName} of ${parameterRefLabel} by the value in the ${displayValue} ${selectorType}`;
      case AutomationActionActionType.DECREASE_PROPERTY:
        return `Decrease ${targetEntityTypeDisplayName} : Decrease ${objectTypeDisplayName} of ${parameterRefLabel} by the value in the ${displayValue} ${selectorType}`;
      case AutomationActionActionType.ARCHIVE_OBJECT:
        return `Archive a ${targetEntityTypeDisplayName} : Archive an existing ${objectTypeDisplayName}`;
      case AutomationActionActionType.SET_PROPERTY:
        return `Update ${startCase(
          camelCase(automation.actionDetails?.captureProperty?.split('_').join(' ')),
        )} : ${objectTypeDisplayName} of ${parameterRefLabel} was set to ${automationPropertyName}`;
      case AutomationActionActionType.SET_RELATION:
        return `Set ${targetEntityTypeDisplayName} : Set relation ${automation?.actionDetails?.relationDisplayName} of ${parameterRefLabel} as the resource selected in the ${parameterLabel}`;
    }
  }
};

export const getParameters = ({
  checklist,
  parameterValues,
}: {
  checklist: any;
  parameterValues: any;
}) => {
  const parameters = {};
  const correctionOrder: any = {};
  let correctionCounter: number = 0;
  const variationDetails = {};
  checklist?.stages?.map((stage: any) => {
    stage?.tasks?.map((task: any) => {
      task?.parameters?.map((parameter: any) => {
        if (parameter?.response?.length > 0) {
          parameter.response
            .sort((a, b) => a.taskExecutionOrderTree - b.taskExecutionOrderTree)
            .forEach((res: any, index: number) => {
              if (res.variations?.length > 0) {
                variationDetails[res.id] = {
                  data: res.variations,
                  location:
                    res.taskExecutionOrderTree === 1
                      ? `Task ${stage?.orderTree}.${task?.orderTree}`
                      : `Task ${stage?.orderTree}.${task?.orderTree}.${
                          res.taskExecutionOrderTree - 1
                        }`,
                  parameterId: parameter.id,
                };
              }
              if (res.correction) {
                index === 0 && ++correctionCounter;
                correctionOrder[parameter.id] = correctionCounter;
              }
            });
        }
        parameters[parameter.id] = parameter;
      });
    });
  });

  // Add ParameterValues
  (parameterValues || []).map((parameter) => {
    parameters[parameter.id] = parameter;
  });

  return { parameters, variationDetails, correctionOrder };
};

export const getTransformedTasks = (
  checklist: Checklist,
  correctionOrderResponse: Record<string, any>,
) => {
  const transformedTasks = new Map();
  const hiddenIds: Record<string, boolean> = {};
  const hiddenParams: Record<string, boolean> = {};

  checklist.stages.forEach((stage: Stage) => {
    let visibleTasksCount = 0;
    stage.tasks.forEach((task: Task) => {
      let visibleTaskExecutionsCount = 0;
      task.parameters.forEach((parameter: any) => {
        parameter.response.forEach((res) => {
          let taskExecution = {
            stageId: stage.id,
            ...task,
            parameters: [
              ...(transformedTasks.get(res.taskExecutionId)?.parameters || []),
              {
                ...parameter,
                response: { ...res, correction: correctionOrderResponse?.[res.id] },
              },
            ],
            taskExecutions: undefined,
            visibleParametersCount:
              transformedTasks.get(res.taskExecutionId)?.visibleParametersCount || 0,
          };

          if (!res.hidden) {
            taskExecution.visibleParametersCount++;
          } else {
            hiddenParams[res.id] = true;
          }

          transformedTasks.set(res.taskExecutionId, taskExecution);
        });
      });

      task.taskExecutions.forEach((taskExecution: TaskExecution) => {
        let _taskExecution = transformedTasks.get(taskExecution.id) || {
          ...task,
          stageId: stage.id,
          taskExecution,
          taskExecutions: undefined,
        };

        if (_taskExecution.visibleParametersCount === 0) {
          hiddenIds[taskExecution.id] = true;
        } else {
          visibleTaskExecutionsCount++;
        }

        transformedTasks.set(taskExecution.id, {
          ..._taskExecution,
          taskExecution,
          taskExecutions: undefined,
        });
      });

      if (visibleTaskExecutionsCount !== 0) {
        visibleTasksCount++;
      } else {
        hiddenIds[task.id] = true;
      }
    });

    if (visibleTasksCount === 0) {
      hiddenIds[stage.id] = true;
    }
  });

  return { transformedTasks, hiddenIds, hiddenParams };
};

const getContentString = (
  details: any[],
  parameter: Parameter,
  isValidation: boolean = false,
  parameters: Parameter[],
  objectTypesList: [],
) => {
  switch (parameter.type) {
    case MandatoryParameter.NUMBER:
      return details
        ?.map((currDetail: any) => {
          const dependentParameter = parameters[currDetail.parameterId];
          return `Check if entered value ${
            labelByConstraint(parameter.type)[currDetail.constraint]
          } ${currDetail.propertyDisplayName} of selected ${dependentParameter?.label} value`;
        })
        .join(',');

    case MandatoryParameter.RESOURCE:
      return isValidation
        ? details
            ?.map((currDetail: any) => {
              const value = currDetail?.value
                ? currDetail.value
                : currDetail.options.map((currOption) => currOption.displayName).join(',');
              return `Check if ${currDetail.propertyDisplayName} of ${
                parameter.data.objectTypeDisplayName
              } ${labelByConstraint(parameter.type)[currDetail.constraint]} ${value}`;
            })
            .join(',')
        : details
            ?.map((currDetail: any) => {
              const dependentParameter = parameters[currDetail.referencedParameterId];
              const parameterObjectType = objectTypesList?.find(
                (currObjectType) => currObjectType.id === parameter?.data.objectTypeId,
              );
              const parameterObjectTypeProperty = [
                ...(parameterObjectType?.properties || []),
                ...(parameterObjectType?.relations || []),
              ].find((currProperty) => currProperty.id === currDetail?.field?.split('.')[1]);
              const value = dependentParameter
                ? `the selected ${dependentParameter.label} value`
                : currDetail?.displayName
                ? `${currDetail.displayName} ${currDetail?.externalId ? currDetail.externalId : ''}`
                : ` ${currDetail.values[0]}`;
              return `Check if ${parameter.data.objectTypeDisplayName} where ${
                parameterObjectTypeProperty?.displayName
              } ${labelByConstraint(parameter.type)[currDetail.op]} ${value}`;
            })
            .join(',');

    default:
      return '';
  }
};

const generateVariationDetailText = (
  type: string,
  parameterId: string,
  parameters,
  objectTypesList: [],
) => {
  return function (details: any[]) {
    const parameterData = parameters[parameterId];
    switch (type) {
      case ParameterVariationType.FILTER:
        return getContentString(details, parameterData, false, parameters, objectTypesList);
      case ParameterVariationType.VALIDATION:
        return getContentString(details, parameterData, true, parameters, objectTypesList);
      case ParameterVariationType.SHOULD_BE:
        const detail = Array.isArray(details) ? details[0] : details;
        const uom = detail?.uom || '';
        const value =
          detail.operator === 'BETWEEN'
            ? `${detail.lowerValue} ${uom} and ${detail.upperValue} ${uom}`
            : `${detail.value} ${uom}`;
        return `Check if entered value is ${generateShouldBeCriteria(detail)} ${value}`;
    }
  };
};

export const generateVariationData = (variationDetails, parameters, objectTypesList) => {
  let updatedData: {}[] = [];

  Object.keys(variationDetails)?.forEach((key) => {
    (variationDetails[key]?.data || [])?.forEach((obj) => {
      const parameterId = variationDetails[key]?.parameterId;
      const location = variationDetails[key]?.location;
      const generateVariationFn = generateVariationDetailText(
        obj.type,
        parameterId,
        parameters,
        objectTypesList,
      );
      const temp = {
        ...obj,
        location: location,
        oldVariationString: obj.oldVariation
          ? generateVariationFn(obj.oldVariation)
          : DEFAULT_VALUE,
        newVariationString: obj.newVariation
          ? generateVariationFn(obj.newVariation)
          : DEFAULT_VALUE,
      };
      updatedData.push(temp);
    });
  });
  return updatedData;
};

export const fileTypeCheck = (collectionOfTypes: string[] = [], type: string) => {
  return collectionOfTypes.includes(type);
};

export const logsResourceChoicesMapper = (list: any[]) => {
  return list.reduce((result, jobLog) => {
    const jobId = jobLog.id;
    result[jobId] = {};

    jobLog.logs.forEach((log: any) => {
      if (log.triggerType === TriggerTypeEnum.RESOURCE && log.resourceParameters) {
        for (const key in log.resourceParameters) {
          if (log.resourceParameters.hasOwnProperty(key)) {
            result[jobId][key] = { ...log.resourceParameters[key] };
          }
        }
      }
    });

    return result;
  }, {});
};

export const logsParser = (log: any, jobId: string, resourceParameterChoicesMap: any) => {
  switch (log.triggerType) {
    case TriggerTypeEnum.RESOURCE_PARAMETER:
      const selectedChoices = (
        resourceParameterChoicesMap?.[jobId]?.[log.entityId]?.choices || []
      ).reduce((acc: any[], c: any) => {
        acc.push(`${c?.objectDisplayName} (ID: ${c?.objectExternalId})`);
        return acc;
      }, []);

      if (!selectedChoices?.length) {
        return {
          ...log,
          value: '-',
        };
      }
      return {
        ...log,
        value: selectedChoices?.join(', '),
      };
    default:
      return log;
  }
};

export const filterByParameterType = (option: any, input: string) => {
  if (input && !ParameterLabelByType[option.value]?.toLowerCase().includes(input.toLowerCase())) {
    return false;
  }
  return true;
};

export const findTaskAndStage = (parameterOrderInTaskInStage: any, parameterId: string) => {
  for (const [stageId, stageData] of Object.entries(parameterOrderInTaskInStage)) {
    for (const [taskId, taskParameters] of Object.entries(stageData)) {
      if (taskParameters.includes(parameterId)) {
        return { stageId, taskId, taskParameters };
      }
    }
  }

  return { stageId: null, taskId: null };
};

export const getCorrectionStatusFlag = (
  status: string,
  isCorrectingError: boolean,
  parameterType: string | undefined,
  correction: any,
) => {
  const correctionStatusFlags = {
    isCorrectionInitiated: status === ParameterCorrectionStatus.INITIATED,
    isCorrectionCorrected: status === ParameterCorrectionStatus.CORRECTED,
    isCorrectionAccepted: status === ParameterCorrectionStatus.ACCEPTED,
    isCorrectionRejected: status === ParameterCorrectionStatus.REJECTED,
  };

  const {
    isCorrectionAccepted,
    isCorrectionRejected,
    isCorrectionInitiated,
    isCorrectionCorrected,
  } = correctionStatusFlags;

  const showCorrectionInitiatorIcon =
    isCorrectingError &&
    ![
      NonMandatoryParameter.INSTRUCTION,
      NonMandatoryParameter.MATERIAL,
      MandatoryParameter.CHECKLIST,
    ].includes(parameterType || '') &&
    (correction === null || isCorrectionRejected || isCorrectionAccepted);

  const showCorrectionActionButton =
    isCorrectingError && !!correction && (isCorrectionInitiated || isCorrectionCorrected);

  return { ...correctionStatusFlags, showCorrectionInitiatorIcon, showCorrectionActionButton };
};

export const getArrayOfSelectedIds = (choices: any) => {
  return Object.keys(choices).filter((id) => choices[id] === Selections.SELECTED);
};

export const getFilterValuesForResource = (
  referencedParameter: Parameter,
  latestResponse?: ParameterResponse,
) => {
  let value;

  if (
    referencedParameter?.type === MandatoryParameter.SINGLE_SELECT ||
    referencedParameter?.type === MandatoryParameter.MULTISELECT
  ) {
    const choices = latestResponse
      ? latestResponse?.choices
      : referencedParameter?.response?.choices;

    value = choices ? getArrayOfSelectedIds(choices) : null;
  } else {
    if (latestResponse) {
      value = latestResponse?.value ?? ObjectIdsDataFromChoices(latestResponse?.choices);
    } else {
      value =
        referencedParameter?.data?.input ??
        ObjectIdsDataFromChoices(referencedParameter?.data?.choices);
    }
  }

  if (
    value &&
    [MandatoryParameter.CALCULATION, MandatoryParameter.NUMBER].includes(referencedParameter?.type)
  ) {
    value = Number(value);
  }

  return value;
};
export const formatParameter = (parameter: Parameter) => {
  const { data, response, ...rest } = parameter;

  switch (parameter.type) {
    case MandatoryParameter.MULTISELECT:
    case MandatoryParameter.SINGLE_SELECT:
      return {
        ...rest,
        data: data.map((option) => ({
          ...option,
          state: response[0]?.choices?.[option.id] || 'NOT_SELECTED',
        })),
      };

    case MandatoryParameter.NUMBER:
    case MandatoryParameter.MULTI_LINE:
    case MandatoryParameter.DATE:
    case MandatoryParameter.SINGLE_LINE:
    case MandatoryParameter.DATE_TIME:
      return {
        ...rest,
        data: {
          ...data,
          input: parameter?.response?.[0]?.value,
        },
      };

    case MandatoryParameter.YES_NO:
      return {
        ...rest,
        data: data.map((option) => ({
          ...option,
          state: response[0]?.choices?.[option.id] || 'NOT_SELECTED',
          reason: option.type === 'no' ? response[0]?.reason : '',
        })),
      };

    case MandatoryParameter.RESOURCE:
    case MandatoryParameter.MULTI_RESOURCE:
      return {
        ...rest,
        data: {
          ...data,
          choices: parameter?.response?.[0]?.choices,
        },
      };

    default:
      return parameter;
  }
};

export const isCjfParameterValueValid = (parameter: Parameter) => {
  if (!parameter?.mandatory) return true;

  let isValid = false;

  if (
    [
      MandatoryParameter.NUMBER,
      MandatoryParameter.DATE,
      MandatoryParameter.DATE_TIME,
      MandatoryParameter.SINGLE_LINE,
      MandatoryParameter.MULTI_LINE,
    ].includes(parameter?.type)
  ) {
    const inputValue = parameter?.data?.input;

    if (inputValue && inputValue !== null) {
      isValid = true;
    }
  } else if (
    [
      MandatoryParameter.SINGLE_SELECT,
      MandatoryParameter.MULTISELECT,
      MandatoryParameter.YES_NO,
    ].includes(parameter?.type)
  ) {
    if (parameter?.data?.length > 0 && parameter?.data?.some((item) => item.state === 'SELECTED')) {
      isValid = true;
    }
  } else if (
    Object.keys(parameter?.data).length === 0 ||
    (parameter?.data?.choices &&
      parameter?.data?.choices !== null &&
      parameter?.data?.choices?.length > 0)
  ) {
    isValid = true;
  }

  return isValid;
};

export const getExceptionStatusAndDetails = (parameter: Parameter, source: string | undefined) => {
  if (!parameter || !parameter.response) return {};

  const {
    response,
    validations: {
      exceptionApprovalType,
      criteriaValidations,
      dateTimeParameterValidations,
      propertyValidations,
      resourceParameterValidations,
    },
  } = parameter;
  const { exception, audit } = response;
  if (!exception || !audit) return {};

  const { status, createdAt, createdBy, reviewer } = exception;
  const { modifiedAt } = audit;

  const exceptionErrorMsg =
    criteriaValidations?.[0]?.errorMessage ||
    dateTimeParameterValidations?.[0]?.errorMessage ||
    propertyValidations?.[0]?.errorMessage ||
    resourceParameterValidations?.[0]?.errorMessage;

  const isExceptionAcceptWithReasonFlow =
    exceptionApprovalType === ParameterExceptionTypeEnum.ACCEPT_WITH_REASON_FLOW;
  const isExceptionApprovalFlow =
    exceptionApprovalType === ParameterExceptionTypeEnum.APPROVER_REVIEWER_FLOW;

  const isExceptionEnabled = status === ParameterExceptionState.INITIATED;
  const isExceptionAccepted = status === ParameterExceptionState.ACCEPTED;
  const isExceptionRejected = status === ParameterExceptionState.REJECTED;
  const isExceptionAutoAccepted = status === ParameterExceptionState.AUTO_ACCEPTED;
  const isCorrectionModalEnabled = source === 'correction-modal';

  const showException =
    (isExceptionAccepted || isExceptionRejected) &&
    compareEpochs(modifiedAt, createdAt, ComparisonOperator.LESS_THAN_OR_EQUAL_TO);

  const showAutoApprovedException =
    isExceptionAutoAccepted &&
    compareEpochs(modifiedAt, createdAt, ComparisonOperator.LESS_THAN_OR_EQUAL_TO);

  let exceptionActionPerformer = null;
  if (isExceptionAutoAccepted) {
    exceptionActionPerformer = createdBy;
  } else if (isExceptionAccepted || isExceptionRejected) {
    exceptionActionPerformer = reviewer?.filter((el) => el.actionPerformed);
  }

  return {
    isCorrectionModalEnabled,
    isExceptionAcceptWithReasonFlow,
    isExceptionApprovalFlow,
    isExceptionEnabled,
    isExceptionAccepted,
    isExceptionRejected,
    isExceptionAutoAccepted,
    showException,
    showAutoApprovedException,
    exceptionActionPerformer,
    exceptionErrorMsg: exceptionErrorMsg ? `"${exceptionErrorMsg}"` : '',
    reviewersReason: exception?.reviewersReason ? exception.reviewersReason : '',
  };
};

export const getExceptionParameter = (parameter: any, validationRuleId: string) => {
  const response = parameter?.response
    ? Array.isArray(parameter.response)
      ? parameter.response?.[0]
      : parameter.response
    : {};

  const exception = response?.exception?.find((ex: any) => ex.ruleId === validationRuleId);
  const validationRule = parameter?.validations?.find(
    (validation: any) => validation.ruleId === validationRuleId,
  );

  return {
    ...parameter,
    response: {
      ...response,
      exception: exception || null,
    },
    validations: validationRule || {},
  };
};

export const getCurrentValidation = (validations: Record<string, any[]>) => {
  for (const key in validations) {
    if (Array.isArray(validations[key]) && validations[key].length > 0) {
      return validations[key][0];
    }
  }
  return null;
};
