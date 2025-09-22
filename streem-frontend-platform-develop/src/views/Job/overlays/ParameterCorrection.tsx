import { BaseModal, Button } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import PasswordInputSection from '#components/shared/PasswordInputSection';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { User } from '#store/users/types';
import { Parameter, ParameterCorrectionStatus } from '#types';
import { apiGetAllJobAssignees } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators } from '#utils/globalTypes';
import React, { FC, useState } from 'react';
import { useForm } from 'react-hook-form';
import styled from 'styled-components';
import {
  CorrectorView,
  InitiatorView,
  ReviewerView,
} from '../components/Task/Parameters/Corrections';
import checkPermission from '#services/uiPermissions';

const Wrapper = styled.div.attrs({})`
  .correction-modal-body {
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 24px;

    .form-group {
      padding: unset;
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

type Props = {
  onSubmit: (data: any) => void;
  onCancel: () => void;
  onSubmitModalText?: string;
  isReadOnly?: boolean;
  isInitiated?: boolean;
  isCorrected?: boolean;
  correction?: any;
  parameter?: Parameter;
  isLoggedInUserReviewer?: boolean;
  isLoggedInUserCorrector?: boolean;
};

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  filters: {
    op: FilterOperators.AND,
    fields: [{ field: 'archived', op: FilterOperators.EQ, values: [false] }],
  },
};

const getDefaultUser = (lastUpdatedByUser: User, loggedInUser: User, isCorrector: boolean) => {
  if (
    isCorrector &&
    lastUpdatedByUser &&
    lastUpdatedByUser?.id !== '1' &&
    loggedInUser?.id !== lastUpdatedByUser?.id
  ) {
    return [
      {
        ...lastUpdatedByUser,
        externalId: lastUpdatedByUser.employeeId,
        value: lastUpdatedByUser.id,
        label: `${lastUpdatedByUser.firstName} ${lastUpdatedByUser.lastName}`,
      },
    ];
  }

  if (!isCorrector && loggedInUser) {
    if (checkPermission(['corrections', 'reviewers'])) {
      return [
        {
          ...loggedInUser,
          externalId: loggedInUser.employeeId,
          value: loggedInUser.id,
          label: `${loggedInUser.firstName} ${loggedInUser.lastName}`,
        },
      ];
    }
  }

  return [];
};

const ParameterCorrectionModal: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: {
    onSubmit,
    onSubmitModalText = 'Submit Request',
    isReadOnly = false,
    isInitiated = false,
    isCorrected = false,
    correction = null,
    parameter = null,
    isLoggedInUserReviewer,
    isLoggedInUserCorrector,
  },
}) => {
  const {
    auth: { userId: loggedInUserId, profile, ssoIdToken },
    job: { id: jobId },
  } = useTypedSelector((state) => state);
  const {
    list: assigneeList,
    reset: resetAssigneeList,
    status,
    fetchNext,
  } = createFetchList<User>(apiGetAllJobAssignees(jobId!), urlParams, false);

  const form = useForm<{
    correctors: User[];
    reviewers: User[];
    initiatorReason: string;
    correctorReason: string;
    reviewerReason: string;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      correctors: getDefaultUser(parameter?.response?.audit?.modifiedBy, profile, true),
      reviewers: getDefaultUser(parameter?.response?.audit?.modifiedBy, profile, false),
      initiatorReason: '',
      correctorReason: '',
      reviewerReason: '',
    },
  });

  const {
    watch,
    formState: { isDirty, isValid },
  } = form;

  const { correctors, reviewers, initiatorReason, correctorReason, reviewerReason } = watch([
    'correctors',
    'reviewers',
    'initiatorReason',
    'correctorReason',
    'reviewerReason',
  ]);
  const [showPasswordField, setShowPasswordField] = useState(false);
  const [password, setPassword] = useState('');
  const [correctedParameterValues, setCorrectedParameterValues] = useState({
    newValue: null,
    newChoice: null,
    medias: [],
  });
  const [performCorrectionStatus, setPerformCorrectionStatus] = useState('');

  const onSubmitModal = () => {
    const { newValue, newChoice, medias } = correctedParameterValues;
    onSubmit({
      correctors,
      reviewers,
      initiatorReason,
      password,
      correctorReason,
      newValue,
      newChoice,
      medias,
      reviewerReason,
      performCorrectionStatus,
    });
    closeOverlay();
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title="Parameter Correction"
        showFooter={false}
      >
        <div className="correction-modal-body">
          {isCorrected ? (
            <ReviewerView
              form={form}
              correction={correction}
              parameter={parameter}
              isLoggedInUserReviewer={isLoggedInUserReviewer}
            />
          ) : isInitiated ? (
            <CorrectorView
              form={form}
              correction={correction}
              parameter={parameter!}
              setCorrectedParameterValues={setCorrectedParameterValues}
              isLoggedInUserCorrector={!!isLoggedInUserCorrector}
            />
          ) : (
            <InitiatorView
              correctors={correctors}
              reviewers={reviewers}
              fetchNext={fetchNext}
              assigneeList={assigneeList}
              resetAssigneeList={resetAssigneeList}
              loggedInUserId={loggedInUserId!}
              form={form}
              assigneeListStatus={status}
            />
          )}
        </div>
        <div className="correction-modal-footer">
          {!isReadOnly && (
            <>
              {showPasswordField ? (
                <div className="correction-modal-footer-password">
                  {!ssoIdToken && (
                    <PasswordInputSection handlePasswordChange={(value) => setPassword(value)} />
                  )}
                  <Button
                    variant="primary"
                    onClick={() => {
                      onSubmitModal();
                    }}
                    disabled={ssoIdToken ? false : !password}
                  >
                    Verify
                  </Button>
                </div>
              ) : (
                <Button
                  variant="primary"
                  onClick={() => {
                    setShowPasswordField(true);
                    setPerformCorrectionStatus(ParameterCorrectionStatus.ACCEPTED);
                  }}
                  disabled={!isDirty || !isValid}
                >
                  {isCorrected ? 'Approve' : onSubmitModalText}
                </Button>
              )}
            </>
          )}
          {!isReadOnly && isCorrected && isLoggedInUserReviewer && !showPasswordField && (
            <Button
              variant="secondary"
              color="red"
              onClick={() => {
                setShowPasswordField(true);
                setPerformCorrectionStatus(ParameterCorrectionStatus.REJECTED);
              }}
              disabled={!isDirty || !isValid}
            >
              Reject
            </Button>
          )}
          <Button variant="secondary" color="blue" onClick={() => closeOverlay()}>
            {isReadOnly ? 'Close' : 'Cancel'}
          </Button>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default ParameterCorrectionModal;
