import CorrectionInitiator from '#assets/svg/correction-initiator.svg';
import InfoIcon from '#assets/svg/info.svg';
import PadLockIcon from '#assets/svg/padlock.svg';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import { useTypedSelector } from '#store';
import { MandatoryParameter, NonMandatoryParameter, ParameterState, StoreParameter } from '#types';
import { SsoStates } from '#utils/globalTypes';
import {
  getCorrectionStatusFlag,
  getExceptionParameter,
  getExceptionStatusAndDetails,
} from '#utils/parameterUtils';
import { ssoSigningRedirect } from '#utils/request';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { jobActions } from '#views/Job/jobStore';
import { ParameterExceptionState } from '#views/Jobs/ListView/types';
import { CircularProgress } from '@material-ui/core';
import { Cancel, CheckCircle, Error } from '@material-ui/icons';
import { useLocation } from '@reach/router';
import { capitalize, groupBy, omit } from 'lodash';
import React, { FC, useCallback, useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import styled, { css } from 'styled-components';
import ParameterLabel from '../ParameterLabel';
import CalculationParameter from './Calculation';
import ChecklistParameter from './Checklist';
import { CorrectionActionButton } from './Corrections';
import FileUploadParameter from './FileUpload';
import ImageCaptureParameter from './ImageCapture';
import InputParameter from './Input';
import InstructionParameter from './Instruction';
import MaterialParameter from './Material';
import MultiSelectParameter from './MultiSelect';
import NumberParameter from './Number';
import ResourceParameter from './Resource';
import ShouldBeParameter from './ShouldBe';
import SignatureParameter from './Signature';
import ParameterVerificationView from './Verification/ParameterVerificationView';
import YesNoParameter from './YesNo';
import DateTimeInput from './DateTimeInput';

export type ParameterProps = {
  parameter: StoreParameter;
  isCorrectingError: boolean;
  isTaskCompleted?: boolean;
  isLoggedInUserAssigned?: boolean;
  errors?: string[];
  setCorrectedParameterValues?: React.Dispatch<React.SetStateAction<any>>;
  source?: string;
  isExceptionEnabled?: boolean;
  isJobBlocked?: boolean;
  isTaskBlocked?: boolean;
  isInboxView?: boolean;
};

// Helper function to generate border style based on correction status
const generateBorderStyle = ({
  isCorrectionInitiated,
  IsCorrectionCorrected,
  isExceptionEnabled,
}: {
  isCorrectionInitiated: boolean;
  IsCorrectionCorrected: boolean;
  isExceptionEnabled: boolean;
}) => {
  if (isCorrectionInitiated || IsCorrectionCorrected || isExceptionEnabled) {
    const borderColor = isCorrectionInitiated
      ? '#F1C21B'
      : IsCorrectionCorrected || isExceptionEnabled
      ? '#005DCC'
      : 'unset';
    const backgroundColor = isCorrectionInitiated
      ? '#FFF8E1'
      : IsCorrectionCorrected || isExceptionEnabled
      ? '#E7F1FD'
      : 'unset';
    return css`
      border-left: 8px solid ${borderColor} !important;
      background-color: ${backgroundColor};
    `;
  }
  return null;
};

const ParameterWrapper = styled.div.attrs({
  className: 'parameter',
})<{
  isCorrectionInitiated: boolean;
  IsCorrectionCorrected: boolean;
  isExceptionEnabled: boolean;
}>`
  .input-parameter,
  .should-be-parameter {
    .input-wrapper {
      ${generateBorderStyle}
    }
  }

  .file-upload-parameter,
  .parameter-media {
    ${generateBorderStyle}
    > div > .card, .container {
      background-color: ${({ isCorrectionInitiated, IsCorrectionCorrected }) =>
        `${isCorrectionInitiated || IsCorrectionCorrected ? 'transparent' : '#ffffff'}`};
    }
  }

  .custom-select__control,
  .signature-interaction,
  .textarea-wrapper {
    ${generateBorderStyle}
  }

  .approved {
    align-items: center;
    color: #5aa700;
    display: flex;
    font-size: 12px;
    margin-top: 8px;

    > .icon {
      color: #5aa700;
      margin-right: 5px;
    }
  }

  .rejected {
    align-items: center;
    color: #ff6b6b;
    display: flex;
    font-size: 12px;
    margin-top: 8px;

    > .icon {
      color: #ff6b6b;
      margin-right: 5px;
    }
  }

  .auto-approved {
    align-items: center;
    display: flex;
    font-size: 12px;
    margin-top: 8px;
    color: #999999;

    > .icon {
      color: #f59e0b;
      margin-right: 5px;
    }
  }
`;

const Parameter: FC<ParameterProps> = ({
  parameter,
  isCorrectingError,
  isLoggedInUserAssigned,
  isTaskCompleted,
  errors,
  setCorrectedParameterValues,
  source,
  isJobBlocked,
  isTaskBlocked,
}) => {
  const {
    state,
    audit,
    parameterVerifications = [],
    variations,
    id,
    correction,
    hasActiveException,
  } = parameter.response!;
  const dispatch = useDispatch();
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const parameterExecutionId = params.get('parameterExecutionId');
  const scrollRef = React.useRef<HTMLDivElement>(null);
  const {
    job: { id: jobId, isInboxView, state: jobState, executingParameterIds },
    auth: { ssoIdToken },
  } = useTypedSelector((state) => state);
  const { pathname } = useLocation();

  const { verificationType } = parameter;

  const verificationsByType = useMemo(() => {
    const verificationGroupedData = groupBy(parameterVerifications, 'verificationType');
    const peerArray = verificationGroupedData['PEER']
      ? verificationGroupedData['PEER'].map((obj) => omit(obj, 'verificationType'))
      : [];
    const selfVerification = verificationGroupedData['SELF']
      ? verificationGroupedData['SELF'][0]
      : null;
    return {
      SELF: selfVerification,
      PEER: peerArray,
    };
  }, [parameterVerifications]);

  useEffect(() => {
    if (state === ParameterState.EXECUTED) {
      dispatch(
        jobActions.onSuccessErrorsHandler({
          parameter,
        }),
      );
    }
  }, [state]);

  const {
    isCorrectionInitiated,
    isCorrectionCorrected,
    showCorrectionInitiatorIcon,
    showCorrectionActionButton,
  } = useMemo(() => {
    return getCorrectionStatusFlag(
      parameter.response?.correction?.status || '',
      isCorrectingError,
      parameter.type,
      parameter.response.correction,
    );
  }, [isCorrectingError, parameter.type, parameter.response.correction]);

  const isExceptionEnabled = parameter?.response?.exception?.length
    ? parameter?.response?.exception?.some(
        (exception: any) => exception.status === ParameterExceptionState.INITIATED,
      )
    : false;

  useEffect(() => {
    if (parameterExecutionId === id && scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [parameterExecutionId]);

  const lockParameter = useMemo(
    () => ParameterState.APPROVAL_PENDING === state && !isCorrectingError,
    [state, isCorrectingError],
  );

  const formatLastUpdatedBy = useCallback(
    (audit: any) => {
      const { modifiedBy, modifiedAt } = audit;
      const fullName = getFullName(modifiedBy);
      const employeeId = modifiedBy?.employeeId || 'N/A';
      const formattedDate = formatDateTime({ value: modifiedAt });

      return parameter.type === MandatoryParameter.CALCULATION &&
        state === ParameterState.BEING_EXECUTED
        ? `Partial Input Parameter(s) fetched by ${fullName}, ID: ${employeeId} on ${formattedDate}`
        : `Last updated by ${fullName}, ID: ${employeeId} on ${formattedDate}`;
    },
    [audit],
  );

  return (
    <ParameterWrapper
      key={parameter.id}
      className="parameter"
      isCorrectionInitiated={isCorrectionInitiated}
      IsCorrectionCorrected={isCorrectionCorrected}
      isExceptionEnabled={isExceptionEnabled}
      ref={scrollRef}
    >
      {parameter.type in MandatoryParameter && !parameter.mandatory && (
        <div className="optional-badge">Optional</div>
      )}

      {typeof errors === 'string' ? (
        <div className="error-badge">
          <Error className="icon" />
          <span>{capitalize(errors)}</span>
        </div>
      ) : (
        (errors || []).map((error, index) => (
          <div className="error-badge" key={`errror_${index}`}>
            <Error className="icon" />
            <span>{capitalize(error)}</span>
          </div>
        ))
      )}

      {parameter?.label &&
        ![`${MandatoryParameter.SHOULD_BE}`, `${MandatoryParameter.CALCULATION}`].includes(
          parameter.type,
        ) && (
          <div className="parameter-label">
            <div>
              {lockParameter && (
                <img src={PadLockIcon} alt="parameter-locked" style={{ marginRight: 8 }} />
              )}
              <ParameterLabel parameter={parameter} />
            </div>
            {variations && variations?.length > 0 && (
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
                        parameter,
                        onSubmit: ({
                          correctors,
                          reviewers,
                          initiatorReason,
                          password,
                        }: {
                          correctors: { id: string }[];
                          reviewers: { id: string }[];
                          initiatorReason: string;
                          password: string;
                        }) => {
                          if (ssoIdToken) {
                            ssoSigningRedirect({
                              state: SsoStates.INITIATE_CORRECTION,
                              parameterResponseId: id,
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
                                parameterResponseId: id,
                                password,
                                initiatorReason,
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
        )}

      {[`${MandatoryParameter.SHOULD_BE}`].includes(parameter.type) ? (
        <ShouldBeParameter
          isLoggedInUserAssigned={isLoggedInUserAssigned}
          verificationType={verificationType}
          parameter={parameter}
          verificationsByType={verificationsByType}
          isCorrectingError={isCorrectingError}
          setCorrectedParameterValues={setCorrectedParameterValues}
        />
      ) : (
        <>
          <div
            {...(lockParameter && {
              style: {
                pointerEvents: 'none',
              },
            })}
          >
            {(() => {
              switch (parameter.type) {
                case MandatoryParameter.CHECKLIST:
                  return (
                    <ChecklistParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                    />
                  );

                case NonMandatoryParameter.INSTRUCTION:
                  return (
                    <InstructionParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                    />
                  );

                case NonMandatoryParameter.MATERIAL:
                  return (
                    <MaterialParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                    />
                  );

                case MandatoryParameter.MEDIA:
                  return (
                    <ImageCaptureParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      isTaskCompleted={isTaskCompleted}
                      isLoggedInUserAssigned={isLoggedInUserAssigned}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                    />
                  );
                case MandatoryParameter.FILE_UPLOAD:
                  return (
                    <FileUploadParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      isTaskCompleted={isTaskCompleted}
                      isLoggedInUserAssigned={isLoggedInUserAssigned}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                    />
                  );
                case MandatoryParameter.MULTISELECT:
                case MandatoryParameter.SINGLE_SELECT:
                  return (
                    <MultiSelectParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      isMulti={parameter.type === MandatoryParameter.MULTISELECT}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                    />
                  );

                case MandatoryParameter.SIGNATURE:
                  return (
                    <SignatureParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      isTaskCompleted={isTaskCompleted || !isLoggedInUserAssigned}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                    />
                  );

                case MandatoryParameter.CALCULATION:
                  return (
                    <CalculationParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      isTaskCompleted={isTaskCompleted || !isLoggedInUserAssigned}
                    />
                  );

                case MandatoryParameter.MULTI_RESOURCE:
                case MandatoryParameter.RESOURCE:
                  return (
                    <ResourceParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                      isExceptionEnabled={isExceptionEnabled}
                      source={source}
                    />
                  );

                case MandatoryParameter.NUMBER:
                  return (
                    <NumberParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                      isExceptionEnabled={isExceptionEnabled}
                    />
                  );

                case MandatoryParameter.SINGLE_LINE:
                case MandatoryParameter.MULTI_LINE:
                  return (
                    <InputParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                      isInboxView={isInboxView}
                      isJobBlocked={isJobBlocked}
                      isTaskBlocked={isTaskBlocked}
                      isTaskCompleted={isTaskCompleted}
                      isExceptionEnabled={isExceptionEnabled}
                    />
                  );

                case MandatoryParameter.DATE_TIME:
                case MandatoryParameter.DATE:
                  return (
                    <DateTimeInput
                      parameter={parameter}
                      isExceptionEnabled={isExceptionEnabled}
                      isCorrectingError={isCorrectingError}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                      isCorrectionInitiated={isCorrectionInitiated}
                      isCorrectionCorrected={isCorrectionCorrected}
                    />
                  );

                case MandatoryParameter.YES_NO:
                  return (
                    <YesNoParameter
                      parameter={parameter}
                      isCorrectingError={isCorrectingError}
                      setCorrectedParameterValues={setCorrectedParameterValues}
                    />
                  );

                default:
                  return null;
              }
            })()}
          </div>

          {!isCorrectingError && !hasActiveException && (
            <ParameterVerificationView
              parameterState={state}
              verificationsByType={verificationsByType}
              verificationType={verificationType}
              isLoggedInUserAssigned={isLoggedInUserAssigned}
              parameterResponseId={id}
              modifiedBy={audit?.modifiedBy?.id}
              correctionEnabled={isCorrectingError}
              isExceptionEnabled={isExceptionEnabled}
              parameterId={parameter.id}
            />
          )}
        </>
      )}

      <div className="parameter-audit">
        {executingParameterIds?.[parameter.id] && (
          <CircularProgress size={14} style={{ color: 'rgb(29, 132, 255)' }} />
        )}
        {state !== 'NOT_STARTED'
          ? audit
            ? audit.modifiedBy && <>{formatLastUpdatedBy(audit)}</>
            : 'Updating...'
          : null}
      </div>

      {parameter.validations.map((validation: any) => {
        const _parameter = getExceptionParameter(parameter, validation.ruleId);

        if (!_parameter.response.exception) {
          return null;
        }

        const {
          isCorrectionModalEnabled,
          showException,
          showAutoApprovedException,
          exceptionActionPerformer,
          isExceptionRejected,
          exceptionErrorMsg,
          reviewersReason,
        } = getExceptionStatusAndDetails(_parameter, source);

        return (
          <>
            {!isCorrectionModalEnabled && (showException || showAutoApprovedException) && (
              <div className="parameter-exception-audit">
                {showException ? (
                  isExceptionRejected ? (
                    <span className="rejected">
                      <Cancel className="icon" fontSize="small" />
                      Exception {exceptionErrorMsg} Rejected by{' '}
                      {getFullName(exceptionActionPerformer?.[0]?.user)}, ID:{' '}
                      {exceptionActionPerformer?.[0].user.employeeId} on{' '}
                      {formatDateTime({ value: exceptionActionPerformer?.[0]?.modifiedAt })} stating
                      reason `{reviewersReason}`
                    </span>
                  ) : (
                    <span className="approved">
                      <CheckCircle className="icon" fontSize="small" />
                      Exception {exceptionErrorMsg} Approved by{' '}
                      {getFullName(exceptionActionPerformer?.[0]?.user)}, ID:{' '}
                      {exceptionActionPerformer?.[0].user.employeeId} on{' '}
                      {formatDateTime({ value: exceptionActionPerformer?.[0]?.modifiedAt })} stating
                      reason `{reviewersReason}`
                    </span>
                  )
                ) : showAutoApprovedException ? (
                  <span className="auto-approved">
                    <CheckCircle className="icon" fontSize="small" />
                    Executed with exception {exceptionErrorMsg} stating reason `
                    {_parameter?.response?.exception?.reason}`
                  </span>
                ) : null}
              </div>
            )}
          </>
        );
      })}

      {showCorrectionActionButton && (
        <CorrectionActionButton
          correction={correction}
          parameter={parameter}
          isLoggedInUserAssigned={isLoggedInUserAssigned}
        />
      )}
    </ParameterWrapper>
  );
};

export default Parameter;
