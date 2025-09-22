import { BaseModal, Button, Checkbox } from '#components';
import { NotificationType } from '#components/Notification/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import AlertBar from '#components/shared/AlertBar';
import React, { FC, useEffect, useMemo, useReducer } from 'react';
import styled from 'styled-components';
import SelectApproversField from '../components/Task/Parameters/Exceptions/SelectApproversField';
import ParameterException from '../components/Task/Parameters/Exceptions/ParameterException';
import { apiGetAllJobAssignees } from '#utils/apiUrls';
import { FilterOperators, SsoStates } from '#utils/globalTypes';
import useRequest from '#hooks/useRequest';
import { uiPermissions } from '#services/uiPermissions';
import { ParameterExceptionTypeEnum } from '#PrototypeComposer/checklist.types';
import PasswordInputSection from '#components/shared/PasswordInputSection';
import { ssoSigningRedirect } from '#utils/request';
import { useDispatch } from 'react-redux';
import { useTypedSelector } from '#store';
import { getExceptionParameter } from '#utils/parameterUtils';
import { ParameterState, TargetEntityType } from '#types';
import { jobActions } from '../jobStore';
import { useLocation } from '@reach/router';
import { ParameterExceptionState } from '#views/Jobs/ListView/types';

type TBulkCjfExceptionProps = {
  jobId: string;
  errors: any[];
  isCjfException?: boolean;
};

const Wrapper = styled.div`
  .modal {
    .modal-body {
      padding: 0px !important;

      .section {
        display: flex;
        flex-direction: column;
        gap: 24px;
        padding: 24px;
        border-bottom: 1px solid #d0d5dd;
      }

      .approvers-field {
        display: flex;
        gap: 16px;
        align-items: center;
      }

      .apply-to-all {
        margin-top: 24px;
      }

      .title {
        font-size: 16px;
        color: #161616;
        font-weight: 700;
      }

      .header {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .info {
          font-size: 14px;
        }
      }

      .footer {
        display: flex;
        justify-content: space-between;
        padding: 16px 24px;
        position: sticky;
        bottom: 0;
        background: white;
        border-top: 1px solid #d0d5dd;

        div {
          display: flex;
        }
      }

      .review-summary {
        padding: 24px 24px 0px 24px;
      }

      p {
        margin: 0px;
      }
    }
  }
`;

type TExceptionParameter = {
  id: string;
  label: string;
  reason: string;
  approvers: any[];
  exceptionApprovalType: ParameterExceptionTypeEnum;
  validationRuleId: string;
  validations: any;
};

type TExceptionState = {
  globalApprovers: any[];
  applyApproversToAll: boolean;
  step: number;
  parametersWithException: Record<string, TExceptionParameter>;
  showPasswordInput: boolean;
  password: string;
  allParametersReadOnly: boolean;
};

type Action =
  | { type: 'SET_GLOBAL_APPROVERS'; payload: any[] }
  | { type: 'SET_APPLY_TO_ALL'; payload: boolean }
  | { type: 'SET_STEP'; payload: number }
  | { type: 'SET_PARAMETERS_WITH_EXCEPTION'; payload: TExceptionParameter[] }
  | { type: 'SET_REASON'; payload: string; id: string }
  | { type: 'SET_APPROVERS'; payload: any[]; id: string }
  | { type: 'SHOW_PASSWORD_INPUT'; payload: boolean }
  | { type: 'SET_PASSWORD'; payload: string }
  | { type: 'HANDLE_BACK'; payload: any[] };

const initialState: TExceptionState = {
  globalApprovers: [],
  applyApproversToAll: false,
  step: 1,
  parametersWithException: {},
  showPasswordInput: false,
  password: '',
  allParametersReadOnly: false,
};

const reducer = (state: TExceptionState, action: Action): TExceptionState => {
  switch (action.type) {
    case 'SET_GLOBAL_APPROVERS':
      return {
        ...state,
        globalApprovers: action.payload,
      };

    case 'SET_APPLY_TO_ALL':
      return {
        ...state,
        applyApproversToAll: action.payload,
        parametersWithException: {
          ...state.parametersWithException,
          ...Object.keys(state.parametersWithException).reduce((acc, id) => {
            const param = state.parametersWithException[id];

            acc[id] = {
              ...param,
              approvers:
                action.payload &&
                param.exceptionApprovalType === ParameterExceptionTypeEnum.APPROVER_REVIEWER_FLOW &&
                !param?.response?.[0]?.exception
                  ? state.globalApprovers
                  : param.approvers,
            };

            return acc;
          }, {} as Record<string, TExceptionParameter>),
        },
      };

    case 'SET_STEP':
      return {
        ...state,
        step: action.payload,
      };

    case 'SET_PARAMETERS_WITH_EXCEPTION':
      const parametersWithException = {};
      let allParametersReadOnly = true;

      for (const param of action.payload) {
        const { ruleId, exceptionApprovalType } = param.validations || {};

        parametersWithException[ruleId] = {
          ...param,
          reason: '',
          approvers: [],
          exceptionApprovalType,
          validationRuleId: ruleId,
        };

        if (
          !param?.response?.exception ||
          param?.targetEntityType === TargetEntityType.TASK ||
          param?.response?.state !== ParameterState.PENDING_FOR_APPROVAL
        ) {
          allParametersReadOnly = false;
        }
      }

      return {
        ...state,
        parametersWithException,
        allParametersReadOnly,
      };

    case 'SET_REASON':
      return {
        ...state,
        parametersWithException: {
          ...state.parametersWithException,
          [action.id]: {
            ...state.parametersWithException[action.id],
            reason: action.payload,
          },
        },
      };

    case 'SET_APPROVERS':
      return {
        ...state,
        parametersWithException: {
          ...state.parametersWithException,
          [action.id]: {
            ...state.parametersWithException[action.id],
            approvers: action.payload,
          },
        },
      };

    case 'SHOW_PASSWORD_INPUT':
      return {
        ...state,
        showPasswordInput: action.payload,
      };

    case 'SET_PASSWORD':
      return {
        ...state,
        password: action.payload,
      };

    case 'HANDLE_BACK':
      return {
        ...state,
        step: 1,
        password: '',
        showPasswordInput: false,
      };

    default:
      return state;
  }
};

const BulkException: FC<CommonOverlayProps<TBulkCjfExceptionProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { jobId, errors, isCjfException = false },
}) => {
  const { pathname } = useLocation();
  const dispatch = useDispatch();
  const cjfValues = useTypedSelector((state) => state.job.cjfValues);
  const parameters = useTypedSelector((state) => state.job.activeTask.parameters);
  const ssoIdToken = useTypedSelector((state) => state.auth.ssoIdToken);
  const [state, reducerDispatch] = useReducer(reducer, initialState);

  const {
    step,
    applyApproversToAll,
    globalApprovers,
    parametersWithException,
    showPasswordInput,
    password,
    allParametersReadOnly,
  } = state;

  const allowedRolesForException = useMemo(() => {
    return uiPermissions.exceptions.reviewers.join(',');
  }, []);

  const { data, status, fetchData, fetchNext } = useRequest<any>({
    url: apiGetAllJobAssignees(jobId!),
    queryParams: {
      filters: {
        op: FilterOperators.AND,
        fields: [{ field: 'archived', op: FilterOperators.EQ, values: [false] }],
      },
      roles: allowedRolesForException,
    },
    fetchOnInit: false,
  });

  const handleApproverChange = (value: any[]) => {
    reducerDispatch({ type: 'SET_GLOBAL_APPROVERS', payload: value });
  };

  const areAllFieldsValid = Object.values(parametersWithException).every((param) => {
    if (param.exceptionApprovalType === ParameterExceptionTypeEnum.APPROVER_REVIEWER_FLOW) {
      return param.reason.trim() && param.approvers.length > 0;
    }
    return param.reason.trim();
  });

  const allParametersWithReasonFlow = useMemo(() => {
    for (const param of Object.values(parametersWithException)) {
      if (param.exceptionApprovalType !== ParameterExceptionTypeEnum.ACCEPT_WITH_REASON_FLOW) {
        return false;
      }
    }
    return true;
  }, [parametersWithException]);

  const payloadForBulkException = (data: any) => {
    const payload = {
      exceptions: [],
    };

    Object.keys(data).forEach((ruleId) => {
      const item = data[ruleId];

      const { id: parameterExecutionId, value, choices } = item.response || {};

      const { reason, approvers, validationRuleId, exceptionApprovalType } = item || {};

      let exception = payload.exceptions.find(
        (e) => e.parameterExecutionId === parameterExecutionId,
      );

      if (!exception) {
        exception = {
          parameterExecutionId: parameterExecutionId,
          parameterExceptionInitiatorRequest: [],
          parameterExceptionAutoAcceptRequest: [],
        };
        payload.exceptions.push(exception);
      }

      if (exceptionApprovalType === 'ACCEPT_WITH_REASON_FLOW') {
        exception.parameterExceptionAutoAcceptRequest.push({
          ruleId: validationRuleId,
          reason,
          ...(value && { value }),
          ...(choices && { choices }),
        });
      } else if (exceptionApprovalType === 'APPROVER_REVIEWER_FLOW') {
        exception.parameterExceptionInitiatorRequest.push({
          ruleId: validationRuleId,
          initiatorReason: reason,
          reviewers: {
            userId: approvers.map((approver: any) => approver.id),
            userGroupId: [],
          },
          ...(value && { value }),
          ...(choices && { choices }),
        });
      }
    });

    return payload;
  };

  const handleSubmitExceptions = async () => {
    if (ssoIdToken) {
      dispatch(
        ssoSigningRedirect({
          state: SsoStates.INITIATE_BULK_EXCEPTION,
          location: pathname,
          parametersWithException: payloadForBulkException(parametersWithException),
        }),
      );
    } else {
      dispatch(
        jobActions.initiateBulkExceptionsOnParameter({
          parametersWithException: payloadForBulkException(parametersWithException),
          password,
          closeOverlayFn: closeAllOverlays,
        }),
      );
    }
  };

  const filteredCjfValues = useMemo(() => {
    return cjfValues.map((param: any) => {
      const updatedParam = { ...param };

      if (updatedParam.response) {
        updatedParam.response = updatedParam.response.map((res: any) => {
          const updatedRes = { ...res };

          if (
            updatedRes.state === ParameterState.BEING_EXECUTED &&
            updatedRes.exception &&
            updatedRes.exception.some((exc: any) =>
              [ParameterExceptionState.ACCEPTED, ParameterExceptionState.AUTO_ACCEPTED].includes(
                exc.status,
              ),
            )
          ) {
            updatedRes.exception = null;
          }
          return updatedRes;
        });
      }

      return updatedParam;
    });
  }, [cjfValues.length]);

  useEffect(() => {
    if (globalApprovers.length === 0) {
      reducerDispatch({ type: 'SET_APPLY_TO_ALL', payload: false });
    }
  }, [globalApprovers.length]);

  useEffect(() => {
    const parametersData: any[] = [];

    if (isCjfException) {
      errors.forEach((error: any) => {
        const parameter = filteredCjfValues.find(
          (param) => param.id === error.errorInfo.parameterId,
        );
        if (parameter) {
          const _parameter = getExceptionParameter(parameter, error.errorInfo.ruleId);
          if (_parameter) {
            parametersData.push(_parameter);
          }
        }
      });
    } else {
      errors.forEach((error: any) => {
        const parameter = parameters.get(error.errorInfo.parameterId);
        const _parameter = getExceptionParameter(parameter, error.errorInfo.ruleId);
        if (_parameter) {
          parametersData.push(_parameter);
        }
      });
    }

    if (parametersData?.length) {
      reducerDispatch({ type: 'SET_PARAMETERS_WITH_EXCEPTION', payload: parametersData });

      parametersData.forEach((param: any) => {
        if (param?.response?.exception && param?.targetEntityType !== TargetEntityType.TASK) {
          const id = param.validations.ruleId;

          const { reason, initiatorsReason, reviewer } = param?.response?.exception || {};

          reducerDispatch({
            type: 'SET_REASON',
            payload: reason || initiatorsReason || '',
            id,
          });

          reducerDispatch({
            type: 'SET_APPROVERS',
            payload: reviewer.length
              ? reviewer.map((r: any) => {
                  const { user } = r;
                  return {
                    id: user.id,
                    firstName: user.firstName,
                    lastName: user.lastName,
                    employeeId: user.employeeId,
                    value: user.id,
                    label: user?.firstName + ' ' + user?.lastName,
                    externalId: <div>&nbsp;(ID: {user?.employeeId})</div>,
                  };
                })
              : [],
            id,
          });
        }
      });
    }
  }, []);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        title="Validation Breach"
      >
        {step === 1 ? (
          <>
            <AlertBar
              msgText={`The value is out of specification! Please state the reason for the breach and obtain approval to proceed with the job.`}
              msgType={NotificationType.WARNING}
            />
            {!allParametersWithReasonFlow && !allParametersReadOnly && (
              <div className="section">
                <p className="title">Select Approver</p>
                <div className="approvers-field">
                  <SelectApproversField
                    list={data || []}
                    loading={status === 'loading'}
                    fetchData={fetchData}
                    fetchNext={fetchNext}
                    onChange={handleApproverChange}
                    selectedApprovers={globalApprovers}
                    isDisabled={allParametersReadOnly}
                  />
                  <div className="apply-to-all">
                    <Checkbox
                      checked={applyApproversToAll}
                      label={`Apply to all`}
                      onClick={(checked) =>
                        reducerDispatch({ type: 'SET_APPLY_TO_ALL', payload: checked })
                      }
                      disabled={globalApprovers.length === 0}
                    />
                  </div>
                </div>
              </div>
            )}
          </>
        ) : (
          <p className="title review-summary">Review Summary</p>
        )}
        <div className="section">
          {step === 1 && (
            <div className="header">
              <p className="title">Enter Reason</p>
              <p className="info">
                You can enter reason {!allParametersWithReasonFlow ? '& also edit approver' : ''}
              </p>
            </div>
          )}

          {Object.entries(parametersWithException).map(([_, param]) => (
            <ParameterException
              key={param.validationRuleId}
              parameterLabel={param.label}
              step={step}
              showApprovers={
                param?.exceptionApprovalType === ParameterExceptionTypeEnum.APPROVER_REVIEWER_FLOW
              }
              approverList={data || []}
              approverListStatus={status}
              fetchApprovers={fetchData}
              fetchNextApprovers={fetchNext}
              reason={param.reason}
              approvers={param.approvers}
              onReasonChange={(value) =>
                reducerDispatch({
                  type: 'SET_REASON',
                  payload: value,
                  id: param.validationRuleId,
                })
              }
              onApproverChange={(value) =>
                reducerDispatch({
                  type: 'SET_APPROVERS',
                  payload: value,
                  id: param.validationRuleId,
                })
              }
              isReadOnly={
                param?.response?.exception &&
                param?.targetEntityType !== TargetEntityType.TASK &&
                param?.response?.state === ParameterState.PENDING_FOR_APPROVAL
              }
              validations={param.validations}
            />
          ))}
        </div>
        <div className="footer">
          <Button
            variant="textOnly"
            color="blue"
            disabled={false}
            onClick={() => {
              closeAllOverlays();
            }}
          >
            Cancel
          </Button>
          <div>
            {step === 2 && (
              <Button
                variant="textOnly"
                color="blue"
                onClick={() => {
                  reducerDispatch({ type: 'HANDLE_BACK', payload: [] });
                }}
              >
                Back
              </Button>
            )}
            {showPasswordInput ? (
              <>
                <input
                  type="text"
                  id="username"
                  name="username"
                  style={{ height: 0, width: 0, padding: 0, border: 0 }}
                />
                {!ssoIdToken && (
                  <PasswordInputSection
                    handlePasswordChange={(value: string) => {
                      reducerDispatch({ type: 'SET_PASSWORD', payload: value });
                    }}
                  />
                )}
                <Button
                  variant="primary"
                  onClick={handleSubmitExceptions}
                  disabled={ssoIdToken ? false : !password}
                  style={{ marginLeft: '8px' }}
                >
                  Verify
                </Button>
              </>
            ) : (
              <>
                {step === 1 ? (
                  <Button
                    disabled={!areAllFieldsValid}
                    onClick={() => reducerDispatch({ type: 'SET_STEP', payload: 2 })}
                  >
                    Next
                  </Button>
                ) : (
                  <>
                    {!allParametersReadOnly && (
                      <Button
                        disabled={!areAllFieldsValid}
                        onClick={() => {
                          reducerDispatch({ type: 'SHOW_PASSWORD_INPUT', payload: true });
                        }}
                      >
                        Proceed
                      </Button>
                    )}
                  </>
                )}
              </>
            )}
          </div>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

const BulkExceptionModal = React.memo(BulkException, (prevProps, nextProps) => {
  return prevProps?.props?.jobId === nextProps?.props?.jobId;
});

export default BulkExceptionModal;
