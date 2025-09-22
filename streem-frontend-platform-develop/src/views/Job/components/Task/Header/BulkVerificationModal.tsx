import { BaseModal, Button } from '#components';
import PasswordInputSection from '#components/shared/PasswordInputSection';
import { useTypedSelector } from '#store';
import { ParameterVerificationTypeEnum, TaskActionType } from '#types';
import { SsoStates } from '#utils/globalTypes';
import { ssoSigningRedirect } from '#utils/request';
import { ParameterVerificationDetails } from '#views/Job/components/Task/Parameters/Verification/ParameterVerificationDetails';
import { jobActions } from '#views/Job/jobStore';
import { useLocation } from '@reach/router';
import React, { useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    width: 50dvw;

    .modal-body {
      overflow: unset;
    }

    .self-verification-container {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin: 16px 8px;
      max-height: 280px;
      min-height: 200px;
      overflow: auto;

      .selectall-container {
        display: flex;
        justify-content: flex-end;
      }

      .no-data-found {
        text-align: center;
        padding: 16px;
        font-size: 16px;
        color: #ccc;
      }
    }

    .exception-modal-footer {
      border-top: 1px solid #f4f4f4 !important;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px 16px 0 16px;
      background-color: #ffffff;

      button {
        margin: 0px;
      }
    }

    .exception-modal-footer-password {
      display: flex;
      gap: 8px;
      flex: 1;
    }

    .close-icon {
      color: #e0e0e0 !important;
      font-size: 16px !important;
    }

    .modal-header {
      border-bottom: 1px solid #f4f4f4 !important;
      h2 {
        color: #161616 !important;
        font-weight: bold !important;
        font-size: 14px !important;
      }
    }
  }
`;

type Props = {
  isReadOnly?: boolean;
  verificationPendingParameters: any[];
  closeOverlay: () => void;
  closeAllOverlays: () => void;
  source: typeof TaskActionType[keyof typeof TaskActionType];
};

export const BulkVerificationModal = (props: Props) => {
  const [showPasswordField, setShowPasswordField] = useState(false);
  const [password, setPassword] = useState('');
  const { verificationPendingParameters, closeOverlay, closeAllOverlays, source } = props;
  const dispatch = useDispatch();
  const { pathname } = useLocation();

  const form = useForm<{
    parameterResponse: any[];
  }>({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      parameterResponse: [],
    },
  });

  const { control, setValue } = form;

  const {
    fields: parameterResponseFields,
    append: parameterResponseAppend,
    remove: parameterResponseRemove,
  } = useFieldArray({
    control,
    name: 'parameterResponse',
  });

  const { ssoIdToken } = useTypedSelector((state) => state.auth);

  const handleParameterSelection = (params: any) => {
    const index = parameterResponseFields?.findIndex((field) => field.id === params.id);
    if (index > -1) {
      parameterResponseRemove(index);
    } else {
      parameterResponseAppend({
        ...params,
        checkedAt: Math.floor(Date.now() / 1000),
      });
    }
    showPasswordField && setShowPasswordField(false);
  };

  const handleSelectAll = () => {
    const selectedParams = verificationPendingParameters.map((param) => ({
      ...param,
      checkedAt: Math.floor(Date.now() / 1000),
    }));
    setValue('parameterResponse', selectedParams);
  };

  const onPrimary = () => {
    const selectedParameterResponseValues = parameterResponseFields.map((fields) => {
      return {
        parameterExecutionId: fields?.response?.id,
        parameterId: fields?.id,
        checkedAt: fields?.checkedAt,
      };
    });

    if (ssoIdToken) {
      if (source === TaskActionType.BULK_SELF_VERIFICATION) {
        dispatch(
          ssoSigningRedirect({
            state: SsoStates.BULK_SELF_VERIFICATION,
            values: selectedParameterResponseValues,
            location: pathname,
          }),
        );
      } else {
        dispatch(
          ssoSigningRedirect({
            state: SsoStates.BULK_PEER_VERIFICATION,
            values: selectedParameterResponseValues,
            location: pathname,
          }),
        );
      }
    } else {
      if (source === TaskActionType.BULK_SELF_VERIFICATION) {
        dispatch(
          jobActions.completeBulkSelfVerification({
            values: selectedParameterResponseValues,
            password,
          }),
        );
      } else {
        dispatch(
          jobActions.completeBulkPeerVerification({
            values: selectedParameterResponseValues,
            password,
          }),
        );
      }
    }
    closeOverlay();
  };

  return (
    <Wrapper>
      <BaseModal
        onSecondary={closeOverlay}
        closeModal={closeOverlay}
        closeAllModals={closeAllOverlays}
        title={`Bulk ${
          source === TaskActionType.BULK_SELF_VERIFICATION ? 'Self' : 'Peer'
        } Verification`}
        primaryText="Submit"
        secondaryText="Cancel"
        showFooter={false}
      >
        <div className="self-verification-container">
          {source === TaskActionType.BULK_SELF_VERIFICATION && (
            <div className="selectall-container">
              <Button
                color="blue"
                variant="textOnly"
                disabled={parameterResponseFields.length === verificationPendingParameters.length}
                onClick={() => handleSelectAll()}
              >
                Select All
              </Button>
            </div>
          )}
          {verificationPendingParameters.length > 0 ? (
            verificationPendingParameters.map((parameter) => {
              return (
                <ParameterVerificationDetails
                  selected={parameterResponseFields.some((field) => field.id === parameter.id)}
                  handleParameterSelection={handleParameterSelection}
                  parameter={parameter}
                  verificationType={ParameterVerificationTypeEnum.SELF}
                />
              );
            })
          ) : (
            <div className="no-data-found">No parameters to verify</div>
          )}
        </div>
        <div className="exception-modal-footer">
          <>
            {showPasswordField ? (
              <div className="exception-modal-footer-password">
                {!ssoIdToken && (
                  <PasswordInputSection
                    handlePasswordChange={(value: string) => setPassword(value)}
                  />
                )}
                <Button
                  variant="primary"
                  onClick={onPrimary}
                  disabled={ssoIdToken ? false : !password}
                >
                  Verify
                </Button>
                <Button
                  variant="secondary"
                  color="blue"
                  onClick={() => setShowPasswordField(false)}
                >
                  Cancel
                </Button>
              </div>
            ) : (
              <>
                <Button
                  variant="primary"
                  onClick={() => {
                    setShowPasswordField(true);
                  }}
                  disabled={parameterResponseFields.length < 2}
                >
                  Submit
                </Button>
                <Button variant="secondary" color="blue" onClick={() => closeOverlay()}>
                  Cancel
                </Button>
              </>
            )}
          </>
        </div>
      </BaseModal>
    </Wrapper>
  );
};
