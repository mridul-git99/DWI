import PadLockIcon from '#assets/svg/padlock.svg';
import { Button, TextInput, Textarea } from '#components';
import { closeOverlayAction, openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { roles } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { ParameterCorrectionStatus, ParameterState, SupervisorResponse } from '#types';
import { InputTypes, SsoStates } from '#utils/globalTypes';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { jobActions } from '#views/Job/jobStore';
import { CheckCircle, Error, Warning } from '@material-ui/icons';
import React, { FC, useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { ParameterProps } from '../Parameter';
import ParameterVerificationView from '../Verification/ParameterVerificationView';
import { Wrapper } from './styles';
import { generateShouldBeText } from '#utils/stringUtils';
import { debounce } from 'lodash';
import InfoIcon from '#assets/svg/info.svg';
import { validateNumber } from '#utils';
import Tooltip from '#components/shared/Tooltip';
import CorrectionInitiator from '#assets/svg/correction-initiator.svg';
import { ssoSigningRedirect } from '#utils/request';
import { useLocation } from '@reach/router';

const checkIsOffLimit = ({
  observedValue,
  desiredValue1,
  desiredValue2,
  operator,
}: {
  observedValue: number | null;
  desiredValue1: number;
  desiredValue2?: number;
  operator: string;
}) => {
  if (!observedValue && !validateNumber(observedValue)) {
    return false;
  } else {
    switch (operator) {
      case 'EQUAL_TO':
        if (!(observedValue === desiredValue1)) {
          return true;
        }
        break;
      case 'LESS_THAN':
        if (!(observedValue < desiredValue1)) {
          return true;
        }
        break;
      case 'LESS_THAN_EQUAL_TO':
        if (!(observedValue <= desiredValue1)) {
          return true;
        }
        break;
      case 'MORE_THAN':
        if (!(observedValue > desiredValue1)) {
          return true;
        }
        break;
      case 'MORE_THAN_EQUAL_TO':
        if (!(observedValue >= desiredValue1)) {
          return true;
        }
        break;
      case 'BETWEEN':
        if (!(observedValue >= desiredValue1 && observedValue <= desiredValue2)) {
          return true;
        }
      default:
        return false;
    }
  }
};

const ShouldBeParameter: FC<
  ParameterProps & {
    verificationsByType: any;
    verificationType: string;
  }
> = ({
  parameter,
  isCorrectingError,
  verificationType,
  verificationsByType,
  isLoggedInUserAssigned,
  setCorrectedParameterValues,
}) => {
  const {
    auth: { profile, ssoIdToken },
    job: { updating, id: jobId, isInboxView, state: jobState },
  } = useTypedSelector((state) => state);
  const { pathname } = useLocation();
  const { variations } = parameter.response;
  const numberInputRef = useRef<HTMLInputElement>(null);
  const debounceInputRef = useRef(debounce((event, functor) => functor(event), 2000));
  const parameterDataRef = useRef(parameter.data);
  const isDisabled = isCorrectingError && !setCorrectedParameterValues;

  const dispatch = useDispatch();

  const [state, setState] = useState({
    approvalTime: parameter?.response?.parameterValueApprovalDto?.createdAt,
    approver: parameter?.response?.parameterValueApprovalDto?.approver,
    isApprovalPending: parameter?.response?.state === ParameterState.PENDING_FOR_APPROVAL,
    isVerificationPending: parameter?.response?.state === ParameterState.APPROVAL_PENDING,
    isApproved: parameter?.response?.parameterValueApprovalDto
      ? parameter?.response?.parameterValueApprovalDto?.state === ParameterState.APPROVED
      : undefined,

    isOffLimit: checkIsOffLimit({
      observedValue: parseFloat(parameter?.response?.value),
      operator: parameterDataRef.current?.operator,
      ...(parameterDataRef.current?.operator === 'BETWEEN'
        ? {
            desiredValue1: parseFloat(parameterDataRef.current?.lowerValue),
            desiredValue2: parseFloat(parameterDataRef.current?.upperValue),
          }
        : { desiredValue1: parseFloat(parameterDataRef.current?.value) }),
    }),
    isUserAuthorisedForApproval: profile?.roles?.some((role) =>
      [
        roles.SUPERVISOR,
        roles.FACILITY_ADMIN,
        roles.CHECKLIST_PUBLISHER,
        roles.ACCOUNT_OWNER,
      ].includes(role.name),
    ),
    isValueChanged: false,
    value: parameter?.response?.value,
  });

  useEffect(() => {
    setState((prevState) => ({
      ...prevState,
      approvalTime: parameter?.response?.parameterValueApprovalDto?.createdAt,
      approver: parameter?.response?.parameterValueApprovalDto?.approver,
      isApprovalPending: parameter?.response?.state === ParameterState.PENDING_FOR_APPROVAL,
      isVerificationPending: parameter?.response?.state === ParameterState.APPROVAL_PENDING,
      isApproved: parameter?.response?.parameterValueApprovalDto
        ? parameter?.response?.parameterValueApprovalDto?.state === ParameterState.APPROVED
        : undefined,
      isOffLimit: checkIsOffLimit({
        observedValue:
          prevState.value !== parameter?.response?.value
            ? prevState.value
            : parseFloat(parameter?.response?.value),
        operator: parameterDataRef.current?.operator,
        ...(parameterDataRef.current?.operator === 'BETWEEN'
          ? {
              desiredValue1: parseFloat(parameterDataRef.current?.lowerValue),
              desiredValue2: parseFloat(parameterDataRef.current?.upperValue),
            }
          : { desiredValue1: parseFloat(parameterDataRef.current?.value) }),
      }),
      isValueChanged: prevState.value !== parameter?.response?.value,
      value:
        prevState.value !== parameter?.response?.value
          ? prevState.value
          : parameter?.response?.value,
    }));
  }, [parameter]);

  useEffect(() => {
    if (parameter.response?.variations?.[0]?.newVariation) {
      parameterDataRef.current = parameter.response.variations[0].newVariation;
    }
  }, [parameter.response?.variations]);

  const renderApprovalButtons = () => (
    <div className="buttons-container">
      <Button
        variant="secondary"
        color="blue"
        onClick={() => {
          dispatch(
            jobActions.approveRejectParameter({
              parameterId: parameter.id,
              parameterResponseId: parameter.response.id,
              type: SupervisorResponse.APPROVE,
            }),
          );
        }}
      >
        Approve
      </Button>
      <Button
        variant="secondary"
        color="red"
        onClick={() => {
          dispatch(
            jobActions.approveRejectParameter({
              parameterId: parameter.id,
              parameterResponseId: parameter.response.id,
              type: SupervisorResponse.REJECT,
            }),
          );
        }}
      >
        Reject
      </Button>
    </div>
  );

  const deviationValueHandler = (value: string) => {
    if (isCorrectingError) {
      dispatchActions(value);
    } else {
      dispatch(
        openOverlayAction({
          type: OverlayNames.REASON_MODAL,
          props: {
            modalTitle: 'State your Reason',
            modalDesc: `Warning! ${generateShouldBeText(
              parameter?.label,
              parameterDataRef.current,
            )}`,
            onSubmitHandler: (reason: string) => {
              dispatchActions(value, reason);
              dispatch(closeOverlayAction(OverlayNames.REASON_MODAL));
            },
            onCancelHandler: () => {
              setState((prevState) => ({
                ...prevState,
                value: parameter.response.value!,
              }));
              numberInputRef.current!.value = parameter.response.value!;
            },
          },
        }),
      );
    }
  };

  useEffect(() => {
    if (!updating && parameter.response.value !== state.value) {
      setState((prevState) => ({
        ...prevState,
        value: parameter.response.value!,
      }));
      numberInputRef.current!.value = parameter.response.value!;
    }
  }, [parameter.response.value, updating]);

  //TODO: In Data field Which Payload should go variation Data or configuration data.

  const dispatchActions = (value: string, reason: string = '') => {
    if (isCorrectingError) {
      if (setCorrectedParameterValues) {
        setCorrectedParameterValues((prev) => ({ ...prev, newValue: value }));
      }
    } else {
      dispatch(
        jobActions.executeParameter({
          parameter: {
            ...parameter,
            data: { ...parameter.data, input: value },
          },
          reason: reason,
        }),
      );
    }
  };

  const onChangeHandler = ({ value }: { value: string }) => {
    debounceInputRef.current(value, (value: string) => {
      setState((prevState) => ({
        ...prevState,
        value,
        isValueChanged: prevState.value !== value,
      }));
      if (value) {
        switch (parameterDataRef.current?.operator) {
          case 'EQUAL_TO':
            if (!(parseFloat(value) === parseFloat(parameterDataRef.current?.value))) {
              setState((prevState) => ({ ...prevState, isOffLimit: true }));
              deviationValueHandler(value);
            } else {
              dispatchActions(value);
            }
            break;
          case 'LESS_THAN':
            if (!(parseFloat(value) < parseFloat(parameterDataRef.current?.value))) {
              setState((prevState) => ({ ...prevState, isOffLimit: true }));
              deviationValueHandler(value);
            } else {
              dispatchActions(value);
            }
            break;
          case 'LESS_THAN_EQUAL_TO':
            if (!(parseFloat(value) <= parseFloat(parameterDataRef.current?.value))) {
              setState((prevState) => ({ ...prevState, isOffLimit: true }));
              deviationValueHandler(value);
            } else {
              dispatchActions(value);
            }
            break;
          case 'MORE_THAN':
            if (!(parseFloat(value) > parseFloat(parameterDataRef.current?.value))) {
              setState((prevState) => ({ ...prevState, isOffLimit: true }));
              deviationValueHandler(value);
            } else {
              dispatchActions(value);
            }
            break;
          case 'MORE_THAN_EQUAL_TO':
            if (!(parseFloat(value) >= parseFloat(parameterDataRef.current?.value))) {
              setState((prevState) => ({ ...prevState, isOffLimit: true }));
              deviationValueHandler(value);
            } else {
              dispatchActions(value);
            }
            break;
          case 'BETWEEN':
            if (
              !(
                parseFloat(value) >= parseFloat(parameterDataRef.current?.lowerValue) &&
                parseFloat(value) <= parseFloat(parameterDataRef.current?.upperValue)
              )
            ) {
              setState((prevState) => ({ ...prevState, isOffLimit: true }));
              deviationValueHandler(value);
            } else {
              dispatchActions(value);
            }
            break;
          default:
            setState((prevState) => ({ ...prevState, isOffLimit: false }));
        }
      } else {
        dispatchActions(value);
      }
    });
  };

  const showCorrectionInitiatorIcon =
    isCorrectingError &&
    (parameter.response.correction === null ||
      [ParameterCorrectionStatus.ACCEPTED, ParameterCorrectionStatus.REJECTED].includes(
        parameter.response.correction.status,
      ));

  return (
    <Wrapper data-id={parameter.id} data-type={parameter.type}>
      <div
        className="parameter-content"
        style={
          !isCorrectingError && (state.isApprovalPending || state.isVerificationPending)
            ? { pointerEvents: 'none' }
            : {}
        }
      >
        {state.isApprovalPending ? (
          <span className="pending-approval">
            <Warning className="icon" />
            {state.isUserAuthorisedForApproval
              ? 'This Parameter Needs Approval'
              : 'Pending for Approval'}
          </span>
        ) : null}

        {!state.isApprovalPending ? (
          state.isApproved === true ? (
            <span className="approved">
              <CheckCircle className="icon" />
              Observation Approved by {getFullName(state.approver)} on{' '}
              {formatDateTime({ value: state.approvalTime! })}
            </span>
          ) : state.isApproved === false ? (
            <span className="rejected">
              <Error className="icon" />
              Observation rejected by {getFullName(state.approver)} on{' '}
              {formatDateTime({ value: state.approvalTime! })}
            </span>
          ) : null
        ) : null}

        <div className="parameter-text" style={{ width: '100%' }} data-for={parameter.id}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            {state.isVerificationPending && !isCorrectingError && (
              <img src={PadLockIcon} alt="parameter-locked" style={{ marginRight: 8 }} />
            )}
            {generateShouldBeText(parameter?.label, parameterDataRef.current)}
            {variations?.length > 0 && (
              <div
                className="parameter-variation"
                style={{
                  pointerEvents: `${
                    (
                      !isInboxView
                        ? 'all'
                        : jobState === 'COMPLETED' || jobState === 'COMPLETED_WITH_EXCEPTION'
                    )
                      ? 'all'
                      : 'unset'
                  }`,
                }}
                onClick={() => {
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.JOB_PARAMETER_VARIATION,
                      props: {
                        jobId: jobId,
                        isReadOnly: true,
                        parameterId: parameter.response.id,
                      },
                    }),
                  );
                }}
              >
                <Tooltip
                  title={
                    <div>
                      {variations.map((currentVariation) => (
                        <>
                          <div>Variation Name: {currentVariation.name}</div>
                          <div>Variation Number: {currentVariation.variationNumber}</div>
                          {currentVariation?.description && (
                            <div>Variation Description: {currentVariation.description}</div>
                          )}
                        </>
                      ))}
                    </div>
                  }
                  arrow
                  placement="bottom"
                >
                  <img src={InfoIcon} alt="parameter-info" style={{ marginRight: 8 }} />
                </Tooltip>
                Variation Planned
              </div>
            )}
            {showCorrectionInitiatorIcon && (
              <img
                src={CorrectionInitiator}
                alt="correction-initiator"
                className="correction-initiator-icon"
                onClick={() => {
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.PARAMETER_CORRECTION_MODAL,
                      props: {
                        isCorrectingError,
                        parameter,
                        onSubmit: ({
                          correctors,
                          reviewers,
                          initiatorReason,
                          password,
                        }: {
                          correctors: string[];
                          reviewers: string[];
                          initiatorReason: string;
                          password: string;
                        }) => {
                          if (ssoIdToken) {
                            ssoSigningRedirect({
                              state: SsoStates.INITIATE_CORRECTION,
                              parameterResponseId: parameter.response.id,
                              initiatorReason,
                              correctors: {
                                userId: correctors.map((curr) => curr.id),
                                userGroupId: [],
                              },
                              reviewers: {
                                userId: reviewers.map((curr) => curr.id),
                                userGroupId: [],
                              },
                              location: pathname,
                            });
                          } else {
                            dispatch(
                              jobActions.initiateErrorCorrectionOnParameter({
                                parameterResponseId: parameter.response.id,
                                initiatorReason,
                                password,
                                correctors: {
                                  userId: correctors.map((curr) => curr.id),
                                  userGroupId: [],
                                },
                                reviewers: {
                                  userId: reviewers.map((curr) => curr.id),
                                  userGroupId: [],
                                },
                              }),
                            );
                          }
                        },
                      },
                    }),
                  );
                }}
              />
            )}
          </div>
        </div>

        <TextInput
          type={InputTypes.NUMBER}
          defaultValue={state.value!}
          onChange={onChangeHandler}
          placeholder="Enter Observed Value"
          ref={numberInputRef}
          disabled={isDisabled}
        />
      </div>

      {state.isOffLimit ? (
        <div className="off-limit-reason">
          {parameter?.response?.reason && (
            <Textarea
              value={parameter.response.reason}
              disabled={true}
              label="Reason"
              placeholder="Reason for change"
              rows={4}
            />
          )}
          {(() => {
            if (state.isUserAuthorisedForApproval && state.isApprovalPending) {
              return renderApprovalButtons();
            }
          })()}
        </div>
      ) : null}
      {!isCorrectingError && (
        <ParameterVerificationView
          parameterState={
            state.isOffLimit && state.isValueChanged
              ? ParameterState.NOT_STARTED
              : parameter.response!.state!
          }
          verificationsByType={verificationsByType}
          verificationType={verificationType}
          isLoggedInUserAssigned={!!isLoggedInUserAssigned}
          parameterResponseId={parameter.response.id}
          modifiedBy={parameter.response?.audit?.modifiedBy?.id}
        />
      )}
    </Wrapper>
  );
};

export default ShouldBeParameter;
