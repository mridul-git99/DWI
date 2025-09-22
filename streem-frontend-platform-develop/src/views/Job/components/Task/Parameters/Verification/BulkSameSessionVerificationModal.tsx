import React, { FC, useState, useEffect, useMemo } from 'react';
import { BaseModal, Button, TextInput, Select } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { InputTypes } from '#utils/globalTypes';
import { useDispatch } from 'react-redux';
import { useTypedSelector } from '#store/helpers';
import { encrypt } from '#utils/stringUtils';
import { apiGetUser, apiAccountLookUp } from '#utils/apiUrls';
import { request } from '#utils/request';
import { navigate } from '@reach/router';
import { compressState, decompressUrl } from '#utils/decompressUrl';
import { verifierLoginUtils } from './verifierLoginUtils';
import { Visibility } from '@material-ui/icons';
import { useForm } from 'react-hook-form';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import styled from 'styled-components';
import { Checkbox } from '#components/shared/Checkbox';
import PasswordInputSection from '#components/shared/PasswordInputSection';
import {
  extractCommonVerifiers,
  validateBulkSameSessionVerification,
  formatParametersForDisplay,
  ParameterWithVerification,
  BULK_SAME_SESSION_CONSTANTS,
} from '#utils/bulkSameSessionVerificationUtils';

interface BulkSameSessionVerificationModalProps {
  parameters: any[];
  onSuccess?: () => void;
}

type BulkSameSessionVerificationModalComponentProps =
  CommonOverlayProps<BulkSameSessionVerificationModalProps>;

type Inputs = {
  password: string;
};

const Wrapper = styled.div.attrs({})`
  .correction-modal-body {
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 24px;

    .form-group {
      padding: unset;
    }

    .verifier-section {
      margin-bottom: 0px;

      .verifier-label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        font-weight: 500;
        color: #525252;
      }

      .verifier-loading,
      .verifier-error,
      .verifier-empty,
      .verifier-display {
        padding: 8px 12px;
        border-radius: 0;
        font-size: 14px;
      }

      .verifier-loading,
      .verifier-display {
        background-color: #f0f0f0;
        color: #666;
      }

      .verifier-error {
        background-color: #fee;
        color: #d00;
        border: 1px solid #fcc;
      }

      .verifier-empty {
        background-color: #fff3cd;
        color: #856404;
        border: 1px solid #ffeaa7;
      }
    }

    .parameter-selection {
      margin-bottom: 0px;

      .parameter-label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        font-weight: 500;
        color: #525252;
      }

      .parameter-list {
        max-height: 200px;
        overflow-y: auto;
        border-radius: 0;
        background-color: #fff;
        padding: 8px;
      }

      .parameter-item {
        display: flex;
        align-items: center;
        padding: 6px 8px;
        cursor: pointer;
        border-radius: 0;
        margin-bottom: 4px;

        &:hover {
          background-color: #f8f8f8;
        }
      }

      .select-all {
        font-weight: 500;
        margin-bottom: 8px;
        padding-bottom: 8px;
        border-bottom: 1px solid #ddd;
      }
    }

    .parameter-verification {
      .verification-label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        font-weight: 500;
        color: #525252;
      }

      .verification-loading {
        padding: 8px 12px;
        background-color: #f0f0f0;
        border-radius: 0;
        font-size: 14px;
        color: #666;
        text-align: center;
      }

      .verification-input {
        margin-bottom: 12px;
      }

      .verification-buttons {
        display: flex;
        gap: 12px;
      }
    }

    .confirmation-section {
      margin-top: 16px;
      margin-bottom: 16px;

      .verifier-info {
        display: flex;
        flex-direction: column;
        margin-bottom: 20px;

        .info-row {
          display: flex;
          margin-bottom: 8px;

          .info-label {
            font-weight: 500;
            min-width: 120px;
            color: #525252;
          }

          .info-value {
            color: #161616;
          }
        }
      }

      .auth-section {
        margin-bottom: 24px;
      }

      .parameters-section {
        margin-top: 24px;
        border-top: 1px solid #e0e0e0;
        padding-top: 16px;
      }
    }
  }

  .correction-modal-footer {
    border-top: 1px solid #f4f4f4 !important;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0px;
    justify-content: flex-end;
  }

  .correction-modal-footer-password {
    display: flex;
    flex-direction: row;
    gap: 8px;
  }

  span {
    color: #c2c2c2;
  }
`;

const BulkSameSessionVerificationModal: FC<BulkSameSessionVerificationModalComponentProps> = ({
  props,
  closeOverlay,
  closeAllOverlays,
}) => {
  const { parameters = [], onSuccess } = props || {};
  const dispatch = useDispatch();

  // Convert parameters to the expected format
  const formattedParameters: ParameterWithVerification[] = useMemo(() => {
    return parameters.map((param: any) => {
      // Extract verifiers from parameterVerifications
      const peerVerifications =
        param.response?.parameterVerifications?.filter((v: any) => v.verificationType === 'PEER') ||
        [];

      const requestedTo = peerVerifications.map((v: any) => v.requestedTo);

      return {
        id: param.id || '',
        label: param.label || param.name || `Parameter ${param.id}`,
        taskName: 'Current Task',
        response: param.response,
        verifications: {
          requestedTo: requestedTo,
        },
      };
    });
  }, [parameters]);

  // State management - simplified to 2 steps
  const [currentStep, setCurrentStep] = useState<string>('verifier-selection');
  const [selectedVerifier, setSelectedVerifier] = useState<any>(null);
  const [verifierUserType, setVerifierUserType] = useState<'LOCAL' | 'SSO' | null>(null);
  const [fetchingUserType, setFetchingUserType] = useState(false);
  const [fetchingVerifiers, setFetchingVerifiers] = useState(false);
  const [passwordInputType, setPasswordInputType] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [password, setPassword] = useState('');

  // Parameter selection state
  const [selectedParameterIds, setSelectedParameterIds] = useState<Set<string>>(new Set());

  // Form management
  const {
    register,
    handleSubmit,
    formState: { isValid },
    reset,
  } = useForm<Inputs>({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  // Selected parameters
  const selectedParameters = useMemo(() => {
    return formattedParameters.filter(
      (param) => selectedParameterIds.has(param.id) && param.response?.id,
    );
  }, [formattedParameters, selectedParameterIds]);

  // Extract common verifiers and validate
  const { commonVerifiers, hasCommonVerifiers, totalParameters } = useMemo(() => {
    return extractCommonVerifiers(
      selectedParameters.length > 0 ? selectedParameters : formattedParameters,
    );
  }, [selectedParameters, formattedParameters]);

  const validation = useMemo(() => {
    return validateBulkSameSessionVerification(
      selectedParameters.length > 0 ? selectedParameters : formattedParameters,
    );
  }, [selectedParameters, formattedParameters]);

  const displayParameters = useMemo(() => {
    return formatParametersForDisplay(formattedParameters);
  }, [formattedParameters]);

  // Initialize selected parameters
  useEffect(() => {
    // Initialize with empty selection (all parameters unchecked)
    setSelectedParameterIds(new Set());
  }, [formattedParameters]);

  // Toggle individual parameter
  const handleParameterToggle = (paramId: string, checked: boolean) => {
    const newSelection = new Set(selectedParameterIds);
    if (!checked) {
      newSelection.delete(paramId);
    } else {
      newSelection.add(paramId);
    }
    setSelectedParameterIds(newSelection);
  };

  // Auto-select verifier if only one available
  useEffect(() => {
    if (commonVerifiers.length === 1 && !selectedVerifier) {
      setSelectedVerifier(commonVerifiers[0]);
    } else if (selectedVerifier && !commonVerifiers.some((v) => v.id === selectedVerifier.id)) {
      setSelectedVerifier(commonVerifiers.length === 1 ? commonVerifiers[0] : null);
    }
  }, [commonVerifiers, selectedVerifier]);

  // Fetch verifier user type when verifier is selected
  useEffect(() => {
    const fetchVerifierUserType = async () => {
      if (selectedVerifier) {
        setFetchingUserType(true);
        setVerifierUserType(null);

        try {
          const userDetailsResponse = await request('GET', apiGetUser(selectedVerifier.id));

          if (!userDetailsResponse.errors && userDetailsResponse.data) {
            const userData = userDetailsResponse.data;

            // Map API values to our enum values
            const rawUserType = userData.userType || 'LOCAL';
            let detectedUserType: 'LOCAL' | 'SSO';

            if (rawUserType === 'AZURE' || rawUserType === 'OKTA') {
              detectedUserType = 'SSO';
            } else {
              detectedUserType = 'LOCAL';
            }

            setVerifierUserType(detectedUserType);
          }
        } catch (error) {
          setVerifierUserType('LOCAL'); // Default to LOCAL
        } finally {
          setFetchingUserType(false);
        }
      } else {
        setVerifierUserType(null);
        setFetchingUserType(false);
      }
    };

    fetchVerifierUserType();
  }, [selectedVerifier]);

  // Show error if validation fails
  useEffect(() => {
    if (!validation.isValid) {
      setError(validation.errorMessage || 'Validation failed');
    } else {
      setError(null);
    }
  }, [validation]);

  const isGroupAssignment = commonVerifiers.length > 1;

  // Handle step navigation - simplified
  const handleNext = () => {
    if (currentStep === 'verifier-selection') {
      if (verifierUserType === 'SSO') {
        // For SSO users, trigger lookup directly
        handleSsoAction();
      } else {
        // For LOCAL users, go to password confirmation
        setCurrentStep('password-confirmation');
      }
    }
  };

  const handleBack = () => {
    if (currentStep === 'password-confirmation') {
      setCurrentStep('verifier-selection');
    }
  };

  // Handle SSO action - call lookup and redirect
  const handleSsoAction = async () => {
    if (!selectedVerifier) return;

    if (selectedParameters.length === 0) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'No parameters selected for verification',
          autoClose: 5000,
        }),
      );
      return;
    }

    setLoading(true);

    const {
      auth: { accessToken: initiatorToken },
    } = (window as any).store.getState();

    if (!initiatorToken) {
      setLoading(false);
      return;
    }

    try {
      // Get user details
      const userDetailsResponse = await request('GET', apiGetUser(selectedVerifier.id));
      if (userDetailsResponse.errors) {
        throw new Error('Failed to fetch verifier user details');
      }

      const username = userDetailsResponse.data.username;

      const lookupResponse = await request('GET', apiAccountLookUp(), {
        params: {
          username: username,
        },
      });

      if (lookupResponse.errors) {
        throw new Error('Account lookup failed');
      }

      if (lookupResponse.data.type !== 'LOCAL') {
        // SSO verifier - redirect to SSO with bulk same session context
        const redirectUrl = new URL(lookupResponse.data.redirectUrl);
        const existingState = redirectUrl.searchParams.get('state')!;
        const formattedState = existingState.replace(/^["']|["']$/g, '').replace(/ /g, '+');

        try {
          const decompressedStateString = await decompressUrl(formattedState);
          if (!decompressedStateString || typeof decompressedStateString !== 'string') {
            throw new Error('Failed to decompress SSO state');
          }
          const parsedState = JSON.parse(decompressedStateString);

          // Add bulk same session verification context
          parsedState.sameSession = 'true';
          parsedState.bulkVerification = 'true';
          parsedState.parameters = selectedParameters.map((param) => ({
            parameterExecutionId: param.response?.id,
            parameterId: param.id,
          }));
          parsedState.initiatorJwtToken = initiatorToken;
          parsedState.pendingAction = 'accept';
          parsedState.verifierUserName = username;
          parsedState.returnTo = window.location.href; // Capture current page URL

          const compressedUpdatedState = await compressState(parsedState);
          redirectUrl.searchParams.set('state', compressedUpdatedState);
          navigate(redirectUrl.href);
        } catch (error) {
          throw new Error('Failed to process SSO redirect');
        }
      } else {
        throw new Error('Expected SSO user but got LOCAL user');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'Bulk verification failed',
          detail: errorMessage,
          autoClose: 8000,
        }),
      );
    } finally {
      setLoading(false);
    }
  };

  // Form submission handler - using shared verifierLoginUtils
  const onSubmit = async () => {
    if (!selectedVerifier) return;

    if (selectedParameters.length === 0) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'No parameters selected for verification',
          autoClose: 5000,
        }),
      );
      return;
    }

    if (!password) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'Password is required',
          autoClose: 5000,
        }),
      );
      return;
    }

    setLoading(true);
    setCurrentStep('processing');

    const {
      auth: { accessToken: initiatorToken },
    } = (window as any).store.getState();

    if (!initiatorToken) {
      const errorMessage = 'No initiator token found';

      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'Bulk verification failed',
          detail: errorMessage,
          autoClose: 8000,
        }),
      );

      closeOverlay();
      setLoading(false);
      return;
    }

    try {
      // Get verifier user details
      const userDetailsResponse = await request('GET', apiGetUser(selectedVerifier.id));
      if (userDetailsResponse.errors) {
        throw new Error('Failed to fetch verifier user details');
      }

      const username = userDetailsResponse.data?.username;
      if (!username) {
        throw new Error('Verifier username not found');
      }

      // Prepare bulk parameters for the shared utility - only use selected parameters
      const bulkParameters = selectedParameters
        .filter((param) => param.response?.id) // Filter out parameters without response ID
        .map((param) => ({
          parameterExecutionId: param.response!.id,
          parameterId: param.id,
        }));

      if (bulkParameters.length === 0) {
        throw new Error('No parameters selected for verification');
      }

      // Use shared verifierLoginUtils for bulk verification
      await verifierLoginUtils({
        verifierUserName: username,
        password: password,
        bulkParameters,
        isBulk: true,
        initiatorToken,
        code: null,
        state: null,
        pendingAction: 'accept',
        rejectionReason: '',
      });

      // Success - dispatch notification and close
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Bulk verification completed successfully!',
          detail: `${bulkParameters.length} parameters have been approved`,
          autoClose: 5000,
        }),
      );

      if (onSuccess) {
        onSuccess();
      }
      closeOverlay();
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';

      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'Bulk verification failed',
          detail: errorMessage,
          autoClose: 8000,
        }),
      );

      closeOverlay();
    } finally {
      setLoading(false);
    }
  };

  // Handle password change
  const handlePasswordChange = (value: string) => {
    setPassword(value);
  };

  // Get modal title based on current step
  const getModalTitle = () => {
    switch (currentStep) {
      case 'verifier-selection':
        return 'Bulk Same Session Verification - Select Verifier';
      case 'password-confirmation':
        return 'Bulk Same Session Verification - Confirm Approval';
      case 'processing':
        return 'Processing Bulk Verification...';
      default:
        return 'Bulk Same Session Verification';
    }
  };

  // Render different steps
  const renderStepContent = () => {
    switch (currentStep) {
      case 'verifier-selection':
        return (
          <div className="correction-modal-body">
            {/* Parameter Selection */}
            <div className="parameter-selection">
              <label className="parameter-label">Parameters:</label>
              <div className="parameter-list">
                {displayParameters.map((param) => (
                  <div className="parameter-item" key={param.id}>
                    <Checkbox
                      checked={selectedParameterIds.has(param.id)}
                      onClick={(checked) => handleParameterToggle(param.id, checked)}
                      label={`${param.name} ${param.taskName ? `(${param.taskName})` : ''}`}
                    />
                  </div>
                ))}
              </div>
            </div>

            {/* Verifier Selection */}
            <div className="verifier-section">
              <label className="verifier-label">Verifier:</label>
              {fetchingVerifiers ? (
                <div className="verifier-loading">Loading verifiers...</div>
              ) : error ? (
                <div className="verifier-error">{error}</div>
              ) : commonVerifiers.length === 0 ? (
                <div className="verifier-empty">
                  No eligible verifiers found for these parameters.
                </div>
              ) : (
                <Select
                  value={
                    selectedVerifier
                      ? {
                          value: selectedVerifier.id,
                          label: `${selectedVerifier.firstName} ${selectedVerifier.lastName}`,
                          externalId: selectedVerifier.employeeId,
                        }
                      : null
                  }
                  onChange={(selectedOption) => {
                    if (selectedOption && isGroupAssignment) {
                      const verifier = commonVerifiers.find((v) => v.id === selectedOption.value);
                      setSelectedVerifier(verifier);
                    } else {
                      setSelectedVerifier(null);
                    }
                  }}
                  placeholder="Select a verifier"
                  options={commonVerifiers.map((verifier) => ({
                    value: verifier.id,
                    label: `${verifier.firstName} ${verifier.lastName}`,
                    externalId: verifier.employeeId,
                  }))}
                  isDisabled={!isGroupAssignment}
                  isClearable={isGroupAssignment}
                  menuPortalTarget={document.body}
                />
              )}
            </div>
          </div>
        );

      case 'password-confirmation':
        return (
          <div className="correction-modal-body">
            <div className="confirmation-section">
              <div className="verifier-info">
                <div className="info-row">
                  <div className="info-label">Verifying as:</div>
                  <div className="info-value">
                    {selectedVerifier?.firstName} {selectedVerifier?.lastName}
                  </div>
                </div>
                <div className="info-row">
                  <div className="info-label">Action:</div>
                  <div className="info-value">Approve {selectedParameters.length} parameters</div>
                </div>
              </div>

              <div className="auth-section">
                <label className="verification-label">Authentication Required:</label>
                <PasswordInputSection handlePasswordChange={handlePasswordChange} />
              </div>

              <div className="parameters-section">
                <label className="parameter-label">Parameters to be approved:</label>
                <div className="parameter-list">
                  {selectedParameters.map((param) => (
                    <div className="parameter-item" key={param.id}>
                      <Checkbox
                        checked={true}
                        disabled={true}
                        label={`${param.label} ${param.taskName ? `(${param.taskName})` : ''}`}
                      />
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        );

      case 'processing':
        return (
          <div className="correction-modal-body">
            <div className="parameter-verification">
              <div className="verification-loading">
                Processing bulk verification...
                <div style={{ fontSize: '14px', color: '#666', marginTop: '8px' }}>
                  Please wait while we verify {selectedParameters.length} parameters
                </div>
              </div>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  // Get footer buttons based on current step
  const getFooterButtons = () => {
    switch (currentStep) {
      case 'verifier-selection':
        return (
          <>
            <Button variant="secondary" onClick={closeOverlay}>
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleNext}
              disabled={!selectedVerifier || !validation.isValid || selectedParameters.length === 0}
            >
              Next
            </Button>
          </>
        );

      case 'password-confirmation':
        return (
          <>
            <Button variant="secondary" onClick={handleBack}>
              Back
            </Button>
            <Button variant="primary" onClick={onSubmit} disabled={loading || !password}>
              {loading ? 'Processing...' : 'Approve All'}
            </Button>
          </>
        );

      case 'processing':
        return null;

      default:
        return <Button onClick={closeOverlay}>Close</Button>;
    }
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title={getModalTitle()}
        showFooter={false}
      >
        {renderStepContent()}
        <div className="correction-modal-footer">{getFooterButtons()}</div>
      </BaseModal>
    </Wrapper>
  );
};

export default BulkSameSessionVerificationModal;
