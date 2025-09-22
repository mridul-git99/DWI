import { BaseModal, Button } from '#components';
import { NotificationType } from '#components/Notification/types';
import AlertBar from '#components/shared/AlertBar';
import PasswordInputSection from '#components/shared/PasswordInputSection';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { User } from '#store/users/types';
import { apiGetAllJobAssignees } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, SsoStates } from '#utils/globalTypes';
import React, { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import styled from 'styled-components';
import { ApproverView } from '../components/Task/Parameters/Exceptions/ApproverView';
import { ExceptionInitiatorView } from '../components/Task/Parameters/Exceptions/InnitiatorView';
import { useDispatch } from 'react-redux';
import { clearRetainedToastIds } from '#store/extras/action';
import { ParameterExceptionTypeEnum } from '#PrototypeComposer/checklist.types';
import { jobActions } from '../jobStore';
import { ssoSigningRedirect } from '#utils/request';
import { useLocation } from '@reach/router';
import { TargetEntityType } from '#types';

const Wrapper = styled.div.attrs({})`
  .modal-body {
    padding: 0px !important;
  }
  .exception-modal-body {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 24px;

    .form-group {
      padding: unset;
    }
  }

  .exception-modal-footer {
    border-top: 1px solid #f4f4f4 !important;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 16px;
    justify-content: flex-start;

    .exception-button-validation-wrapper {
      min-width: 90px;
      padding-inline: 12px;
    }
  }

  .exception-modal-footer-password {
    display: flex;
    flex-direction: row;
    gap: 8px;
    flex: 1;
  }

  span {
    color: #c2c2c2;
  }
`;

const ParameterExceptionModal = ({
  closeAllOverlays,
  closeOverlay,
  props: { parameter, initiatorView = false, jobId, isDisabled = false, rulesId },
}: any) => {
  const { pathname } = useLocation();
  const dispatch = useDispatch();
  const userId = useTypedSelector((state) => state.auth.userId)!;
  const ssoIdToken = useTypedSelector((state) => state.auth.ssoIdToken);

  const {
    list: assigneeList,
    reset: resetAssigneeList,
    status,
    fetchNext,
  } = createFetchList<any>(
    apiGetAllJobAssignees(jobId!),
    {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      filters: {
        op: FilterOperators.AND,
        fields: [{ field: 'archived', op: FilterOperators.EQ, values: [false] }],
      },
    },
    false,
  );

  const reviewerView = useMemo(() => !initiatorView, []);

  const isReadOnly = useMemo(
    () =>
      parameter?.response?.exception?.status === 'REJECTED' ||
      parameter?.response?.exception?.status === 'ACCEPTED'
        ? true
        : initiatorView
        ? false
        : parameter?.response?.exception?.reviewer?.filter((data) => data?.user?.id === userId)
            ?.length && !isDisabled
        ? false
        : true,
    [],
  );

  const acceptWithReasonView = useMemo(
    () => parameter?.exceptionApprovalType === ParameterExceptionTypeEnum.ACCEPT_WITH_REASON_FLOW,
    [],
  );

  const form = useForm<{
    approver: User[] | null;
    reason: string;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      approver: null,
      reason: '',
    },
  });

  const {
    watch,
    formState: { isDirty, isValid },
  } = form;

  const { approver, reason } = watch(['approver', 'reason']);

  const [showPasswordField, setShowPasswordField] = useState(false);
  const [password, setPassword] = useState('');
  const [reviewStatus, setReviewStatus] = useState('');

  const onSubmitModal = async () => {
    dispatch(clearRetainedToastIds());
    const value = parameter?.data?.input || parameter?.response?.value;
    const choices = parameter?.data?.choices;

    if (ssoIdToken) {
      if (initiatorView) {
        if (acceptWithReasonView) {
          dispatch(
            jobActions.autoAcceptExceptionOnParameter({
              parameterResponseId: parameter?.response?.id,
              reason,
              value,
              choices,
              closeOverlayFn: closeOverlay,
            }),
          );
        } else {
          dispatch(
            ssoSigningRedirect({
              state: SsoStates.INITIATE_EXCEPTION,
              parameterResponseId: parameter?.response?.id,
              initiatorReason: reason,
              value,
              choices,
              location: pathname,
              approver: {
                userId: (approver || []).map((curr) => curr.id),
                userGroupId: [],
              },
            }),
          );
        }
      } else {
        dispatch(
          ssoSigningRedirect({
            state: SsoStates.REVIEW_EXCEPTION,
            parameterResponseId: parameter?.response?.id,
            exceptionId: parameter?.response?.exception?.id,
            reviewerReason: reason,
            location: pathname,
            reviewStatus,
            rulesId,
          }),
        );
      }
    } else {
      if (initiatorView) {
        if (acceptWithReasonView) {
          dispatch(
            jobActions.autoAcceptExceptionOnParameter({
              parameterResponseId: parameter?.response?.id,
              reason,
              value,
              choices,
              closeOverlayFn: closeOverlay,
            }),
          );
        } else {
          dispatch(
            jobActions.initiateExceptionOnParameter({
              parameterResponseId: parameter?.response?.id,
              password,
              initiatorReason: reason,
              value,
              choices,
              closeOverlayFn: closeOverlay,
              approver: {
                userId: (approver || []).map((curr) => curr.id),
                userGroupId: [],
              },
            }),
          );
        }
      } else {
        dispatch(
          jobActions.submitExceptionOnParameter({
            parameterResponseId: parameter?.response?.id,
            exceptionId: parameter?.response?.exception?.id,
            password,
            reviewerReason: reason,
            reviewStatus,
            closeOverlayFn: closeOverlay,
            isCjfException: parameter?.targetEntityType === TargetEntityType.PROCESS,
            jobId,
            rulesId,
          }),
        );
      }
    }
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title={`${initiatorView ? 'Validation Breach' : 'Exception Details'}`}
        showFooter={false}
      >
        {initiatorView && (
          <AlertBar
            msgText={`The value is out of specification! Please ${
              acceptWithReasonView ? 'give reason' : 'take approval'
            } to proceed with this parameter execution.`}
            msgType={NotificationType.WARNING}
          />
        )}
        <div className="exception-modal-body">
          {initiatorView && (
            <ExceptionInitiatorView
              approver={approver}
              fetchNext={fetchNext}
              assigneeList={assigneeList}
              resetAssigneeList={resetAssigneeList}
              loggedInUserId={userId}
              form={form}
              assigneeListStatus={status}
              setShowPasswordField={setShowPasswordField}
              acceptWithReasonView={acceptWithReasonView}
            />
          )}
          {reviewerView && (
            <ApproverView
              parameter={parameter}
              form={form}
              setShowPasswordField={setShowPasswordField}
              isReadOnly={isReadOnly}
              jobId={jobId}
            />
          )}
        </div>
        <div className="exception-modal-footer">
          {!isReadOnly && (
            <>
              {showPasswordField ? (
                <div className="exception-modal-footer-password">
                  {!ssoIdToken && <PasswordInputSection handlePasswordChange={setPassword} />}
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
              ) : reviewerView ? (
                <>
                  <Button
                    variant="secondary"
                    color="blue"
                    onClick={() => {
                      setShowPasswordField(true);
                      setReviewStatus('approve');
                    }}
                    disabled={!isDirty || !isValid}
                  >
                    Approve
                  </Button>
                  <Button
                    variant="secondary"
                    color="red"
                    onClick={() => {
                      setShowPasswordField(true);
                      setReviewStatus('reject');
                    }}
                    disabled={!isDirty || !isValid}
                  >
                    Reject
                  </Button>
                </>
              ) : (
                <Button
                  variant="primary"
                  onClick={() => {
                    if (acceptWithReasonView) {
                      onSubmitModal();
                    } else {
                      setShowPasswordField(true);
                    }
                  }}
                  disabled={!isDirty || !isValid}
                >
                  {acceptWithReasonView ? 'Submit' : 'Submit Request'}
                </Button>
              )}
            </>
          )}
          {(initiatorView || (reviewerView && (showPasswordField || isReadOnly))) && (
            <Button
              className="exception-button-validation-wrapper"
              variant="secondary"
              color="blue"
              onClick={() => closeOverlay()}
            >
              {isReadOnly ? 'Close' : 'Cancel'}
            </Button>
          )}
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default ParameterExceptionModal;
