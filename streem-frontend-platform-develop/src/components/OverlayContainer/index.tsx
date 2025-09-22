import { AuthorsDetailsPopover } from '#PrototypeComposer/Overlays/AuthorsDetailsPopover';
import { CopyElementModal } from '#PrototypeComposer/Overlays/CopyElementModal';
import { ChecklistErrorsPopover } from '#PrototypeComposer/Overlays/ChecklistErrorsPopover';
import ConfigureActions from '#PrototypeComposer/Overlays/ConfigureActions';
import ConfigureJobParameters from '#PrototypeComposer/Overlays/ConfigureJobParameters';
import ConfigureTaskConditions from '#PrototypeComposer/Overlays/ConfigureTaskConditions';
import InitiateSignOffModal from '#PrototypeComposer/Overlays/InitiateSignOff';
import PasswordInputModal from '#PrototypeComposer/Overlays/PasswordInput';
import ReleaseSuccessModal from '#PrototypeComposer/Overlays/ReleaseSuccess';
import ReviewSubmitSuccessModal from '#PrototypeComposer/Overlays/ReviewSubmitSuccess';
import ReviewerAssignmentModal from '#PrototypeComposer/Overlays/ReviewerAssignmentModal';
import { ReviewerAssignmentPopover } from '#PrototypeComposer/Overlays/ReviewerAssignmentPopover';
import ReviewerAssignmentSuccessModal from '#PrototypeComposer/Overlays/ReviewerAssignmentSuccess';
import { ReviewersDetailsPopover } from '#PrototypeComposer/Overlays/ReviewersDetailsPopover';
import ScheduleTaskModal from '#PrototypeComposer/Overlays/ScheduleTaskModal';
import SentToAuthorSuccessModal from '#PrototypeComposer/Overlays/SentToAuthorSuccess';
import SignOffInitiatedSuccessModal from '#PrototypeComposer/Overlays/SignOffInitiatedSuccess';
import SignOffProgressModal from '#PrototypeComposer/Overlays/SignOffProgress';
import SignOffSuccessModal from '#PrototypeComposer/Overlays/SignOffSuccess';
import { SubmitReviewModal } from '#PrototypeComposer/Overlays/SubmitReview';
import TaskRecurrenceModal from '#PrototypeComposer/Overlays/TaskRecurrenceModal';
import { TaskMediaModal, TimedTaskConfigModal } from '#PrototypeComposer/modals';
import EditingDisabledModal from '#PrototypeComposer/modals/EditingDisabled';
import { QRGenerator, QRScanner } from '#components';
import { AssignedUserDetailsPopover } from '#components/shared/Avatar';
import WebCamOverlay from '#components/shared/WebCamOverlay';
import { useTypedSelector } from '#store';
import SessionExpireModal from '#views/Auth/Overlays/SessionExpire';
import PutCustomViewModal from '#views/Checklists/JobLogs/Overlays/PutCustomViewModal';
import ArchiveModal from '#views/Checklists/ListView/ArchiveModal';
import ChecklistInfoModal from '#views/Checklists/ListView/ChecklistInfoModal';
import ProcessSharing from '#views/Checklists/Overlays/ProcessSharing';
import RevisionErrorModal from '#views/Checklists/Overlays/RevisionErrorModal';
import AddRemark from '#views/Job/overlays/AddRemark';
import AutomationActionModal from '#views/Job/overlays/AutomationAction';
import AutomationTaskModal from '#views/Job/overlays/AutomationTaskModal';
import JobCompleteAllTasksError from '#views/Job/overlays/CompleteAllTasksError';
import CompleteJobWithException from '#views/Job/overlays/CompleteJobWithException';
import CompletedWithExceptionInfo from '#views/Job/overlays/CompletedWithExceptionInfo';
import EndRecurrenceModal from '#views/Job/overlays/EndRecurrenceModal';
import JobVerification from '#views/Job/overlays/JobVerification';
import ParameterApprovalModal from '#views/Job/overlays/ParameterApproval';
import ParameterVariationContent from '#views/Job/overlays/ParameterVariationContent';
import RecurrenceExecutionModal from '#views/Job/overlays/RecurrenceExecutionModal';
import RepeatTaskError from '#views/Job/overlays/RepeatTaskError';
import Signature from '#views/Job/overlays/SignatureActivity';
import StartTaskError from '#views/Job/overlays/StartTaskError';
import TaskPauseReasonModal from '#views/Job/overlays/TaskPauseReasonModal';
import ViewReason from '#views/Job/overlays/ViewReason';
import ViewReasonModal from '#views/Job/overlays/ViewReasonModal';
import AssingnmentInfo from '#views/Jobs/Assignment/AssignmentInfo';
import SetDateModal from '#views/Jobs/Overlays/SetDateModal';
import SecretKeyModal from '#views/UserAccess/Overlays/SecretKeyModal';
import UserAssignmentUserGroup from '#views/UserAccess/Overlays/UserAssignmentUserGroup';
import ValidateCredentialsModal from '#views/UserAccess/Overlays/ValidateCredentialsModal';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import ReasonModal from '../shared/ReasonModal';
import StartErrorModal from '../shared/StartErrorModal';
import ConfigureColumnsModalContainer from './ConfigureColumnsContainer';
import ConfigureObjectType from './ConfigureObjectType';
import { ConfirmationModal } from './ConfirmationModal';
import { MultiTabModal } from './MultiTabChecker';
import OrientationModal from './OrientationModal';
import { closeAllOverlayAction, closeOverlayAction } from './actions';
import { CommonOverlayProps, OverlayNames } from './types';
import QrCodeParserModal from './QrCodeParserModal';
import RangeFilterModal from '#views/Job/overlays/RangeFilterModal';
import TaskDependency from '#PrototypeComposer/Overlays/TaskDependency';
import ViewTaskDependency from '#PrototypeComposer/Overlays/ViewTaskDependency';
import TaskDependencyError from '#views/Job/overlays/TaskDependencyError';
import ParameterCorrectionModal from '#views/Job/overlays/ParameterCorrection';
import ParameterExceptionModal from '#views/Job/overlays/ParameterException';
import CorrectionContentModal from '#views/Job/overlays/CorrectionContentModal';
import ApprovalsContentModal from '#views/Job/overlays/ApprovalsContentModal';
import TaskExecutorLock from '#PrototypeComposer/Overlays/TaskExecutorLock';
import TaskExecutorLockDetails from '#views/Job/overlays/TaskExecutorLockDetails';
import TaskExecutorLockError from '#views/Job/overlays/TaskExecutorLockError';
import RefetchJob from '#views/Job/overlays/RefetchJob';
import ObjectPreviewModal from '#views/Ontology/Objects/Overlays/ObjectPreviewModal';
import ObjectJobLogPreviewModal from '#views/Ontology/Objects/Overlays/ObjectJobLogPreviewModal';
import JobAnnotations from '#views/Job/overlays/JobAnnotations';
import BulkSelfVerificationModalContainer from '#views/Job/overlays/BulkSelfVerificationModalContainer';
import BulkPeerVerificationModalContainer from '#views/Job/overlays/BulkPeerVerificationModalContainer';
import BulkExceptionModal from '#views/Job/overlays/BulkException';
import ViewExceptionDetails from '#views/Jobs/Overlays/ViewExceptionDetails';
import CreateActionModal from '#PrototypeComposer/Overlays/CreateActionModal';
import SameSessionVerificationModal from '#views/Job/components/Task/Parameters/Verification/SameSessionVerificationModal';
import BulkSameSessionVerificationModal from '#views/Job/components/Task/Parameters/Verification/BulkSameSessionVerificationModal';

const getOverlay = (params: CommonOverlayProps<any>) => {
  const { type } = params;
  switch (type) {
    case OverlayNames.SIGNATURE_MODAL:
      return <Signature {...params} />;

    case OverlayNames.CONFIRMATION_MODAL:
      return <ConfirmationModal {...params} />;

    case OverlayNames.START_TASK_ERROR_MODAL:
      return <StartTaskError {...params} />;

    case OverlayNames.COMPLETE_JOB_WITH_EXCEPTION:
      return <CompleteJobWithException {...params} />;

    case OverlayNames.CHECKLIST_REVIEWER_ASSIGNMENT:
      return <ReviewerAssignmentModal {...params} />;

    case OverlayNames.CHECKLIST_REVIEWER_ASSIGNMENT_SUCCESS:
      return <ReviewerAssignmentSuccessModal {...params} />;

    case OverlayNames.CHECKLIST_REVIEWER_SUBMIT_SUCCESS:
      return <ReviewSubmitSuccessModal {...params} />;

    case OverlayNames.CHECKLIST_SENT_TO_AUTHOR_SUCCESS:
      return <SentToAuthorSuccessModal {...params} />;

    case OverlayNames.SUBMIT_REVIEW_MODAL:
      return <SubmitReviewModal {...params} />;

    case OverlayNames.INITIATE_SIGNOFF:
      return <InitiateSignOffModal {...params} />;

    case OverlayNames.SIGN_OFF_PROGRESS:
      return <SignOffProgressModal {...params} />;

    case OverlayNames.SIGN_OFF_INITIATED_SUCCESS:
      return <SignOffInitiatedSuccessModal {...params} />;

    case OverlayNames.SIGN_OFF_SUCCESS:
      return <SignOffSuccessModal {...params} />;

    case OverlayNames.RELEASE_SUCCESS:
      return <ReleaseSuccessModal {...params} />;

    case OverlayNames.PASSWORD_INPUT:
      return <PasswordInputModal {...params} />;

    case OverlayNames.SESSION_EXPIRE:
      return <SessionExpireModal {...params} />;

    case OverlayNames.CHECKLIST_REVIEWER_ASSIGNMENT_POPOVER:
      return params.popOverAnchorEl ? <ReviewerAssignmentPopover {...params} /> : null;

    case OverlayNames.AVATAR_DETAIL:
      return params.popOverAnchorEl ? <AssignedUserDetailsPopover {...params} /> : null;

    case OverlayNames.AUTHORS_DETAIL:
      return params.popOverAnchorEl ? <AuthorsDetailsPopover {...params} /> : null;

    case OverlayNames.REVIEWERS_DETAIL:
      return params.popOverAnchorEl ? <ReviewersDetailsPopover {...params} /> : null;

    case OverlayNames.CHECKLIST_ERRORS:
      return params.popOverAnchorEl ? <ChecklistErrorsPopover {...params} /> : null;

    case OverlayNames.TIMED_TASK_CONFIG:
      return <TimedTaskConfigModal {...params} />;

    case OverlayNames.TASK_MEDIA:
      return <TaskMediaModal {...params} />;

    case OverlayNames.PARAMETER_APPROVAL:
      return <ParameterApprovalModal {...params} />;

    case OverlayNames.EDITING_DISABLED:
      return <EditingDisabledModal {...params} />;

    case OverlayNames.CHECKLIST_INFO:
      return <ChecklistInfoModal {...params} />;

    case OverlayNames.SHOW_COMPLETED_JOB_WITH_EXCEPTION_INFO:
      return <CompletedWithExceptionInfo {...params} />;

    case OverlayNames.REVISION_ERROR:
      return <RevisionErrorModal {...params} />;

    case OverlayNames.ARCHIVE_MODAL:
      return <ArchiveModal {...params} />;

    case OverlayNames.SECRET_KEY_MODAL:
      return <SecretKeyModal {...params} />;

    case OverlayNames.VALIDATE_CREDENTIALS_MODAL:
      return <ValidateCredentialsModal {...params} />;

    case OverlayNames.ASSIGNMENT_INFO:
      return <AssingnmentInfo {...params} />;

    case OverlayNames.ENTITY_START_ERROR_MODAL:
      return <StartErrorModal {...params} />;

    case OverlayNames.REASON_MODAL:
      return <ReasonModal {...params} />;

    case OverlayNames.JOB_COMPLETE_ALL_TASKS_ERROR:
      return <JobCompleteAllTasksError {...params} />;

    case OverlayNames.WEBCAM_OVERLAY:
      return <WebCamOverlay {...params} />;

    case OverlayNames.AUTOMATION_ACTION:
      return <AutomationActionModal {...params} />;

    case OverlayNames.CONFIGURE_COLUMNS:
      return <ConfigureColumnsModalContainer {...params} />;

    case OverlayNames.CONFIGURE_OBJECT_TYPE:
      return <ConfigureObjectType {...params} />;

    case OverlayNames.CONFIGURE_ACTIONS:
      return <ConfigureActions {...params} />;

    case OverlayNames.PROCESS_SHARING:
      return <ProcessSharing {...params} />;

    case OverlayNames.CONFIGURE_JOB_PARAMETERS:
      return <ConfigureJobParameters {...params} />;

    case OverlayNames.QR_SCANNER:
      return <QRScanner {...params} />;

    case OverlayNames.QR_GENERATOR:
      return <QRGenerator {...params} />;

    case OverlayNames.PUT_CUSTOM_VIEW:
      return <PutCustomViewModal {...params} />;

    case OverlayNames.SET_DATE:
      return <SetDateModal {...params} />;

    case OverlayNames.JOB_VERIFICATION:
      return <JobVerification {...params} />;

    case OverlayNames.VIEW_REASON:
      return <ViewReason {...params} />;

    case OverlayNames.TASK_PAUSE_REASON_MODAL:
      return <TaskPauseReasonModal {...params} />;

    case OverlayNames.AUTOMATION_TASK_MODAL:
      return <AutomationTaskModal {...params} />;

    case OverlayNames.ORIENTATION_MODAL:
      return <OrientationModal {...params} />;

    case OverlayNames.MULTI_TAB_MODAL:
      return <MultiTabModal {...params} />;

    case OverlayNames.JOB_PARAMETER_VARIATION:
      return <ParameterVariationContent {...params} />;

    case OverlayNames.TASK_RECURRENCE_MODAL:
      return <TaskRecurrenceModal {...params} />;

    case OverlayNames.REPEAT_TASK_ERROR_MODAL:
      return <RepeatTaskError {...params} />;

    case OverlayNames.TASK_RECURRENCE_EXECUTION_MODAL:
      return <RecurrenceExecutionModal {...params} />;

    case OverlayNames.END_TASK_RECURRENCE_MODAL:
      return <EndRecurrenceModal {...params} />;

    case OverlayNames.VIEW_REASON_MODAL:
      return <ViewReasonModal {...params} />;

    case OverlayNames.SCHEDULE_TASK_MODAL:
      return <ScheduleTaskModal {...params} />;

    case OverlayNames.ADD_REMARK_MODAL:
      return <AddRemark {...params} />;

    case OverlayNames.CONFIGURE_TASK_CONDITIONS:
      return <ConfigureTaskConditions {...params} />;

    case OverlayNames.QR_CODE_PARSER_MODAL:
      return <QrCodeParserModal {...params} />;
    case OverlayNames.RANGE_FILTER_MODAL:
      return <RangeFilterModal {...params} />;

    case OverlayNames.TASK_DEPENDENCY_MODAL:
      return <TaskDependency {...params} />;

    case OverlayNames.VIEW_TASK_DEPENDENCY_MODAL:
      return <ViewTaskDependency {...params} />;

    case OverlayNames.TASK_DEPENDENCY_ERROR_MODAL:
      return <TaskDependencyError {...params} />;

    case OverlayNames.TASK_EXECUTOR_LOCK:
      return <TaskExecutorLock {...params} />;

    case OverlayNames.TASK_EXECUTOR_LOCK_DETAILS:
      return <TaskExecutorLockDetails {...params} />;

    case OverlayNames.TASK_EXECUTOR_LOCK_ERROR:
      return <TaskExecutorLockError {...params} />;

    case OverlayNames.USER_ASSIGN_USER_GROUP:
      return <UserAssignmentUserGroup {...params} />;

    case OverlayNames.PARAMETER_CORRECTION_MODAL:
      return <ParameterCorrectionModal {...params} />;

    case OverlayNames.PARAMETER_EXCEPTION_MODAL:
      return <ParameterExceptionModal {...params} />;

    case OverlayNames.CORRECTION_LIST_MODAL:
      return <CorrectionContentModal {...params} />;

    case OverlayNames.APPROVALS_LIST_MODAL:
      return <ApprovalsContentModal {...params} />;

    case OverlayNames.REFETCH_JOB_COMPOSER_DATA:
      return <RefetchJob {...params} />;

    case OverlayNames.COPY_ENTITY_MODAL:
      return <CopyElementModal {...params} />;

    case OverlayNames.OBJECT_PREVIEW_MODAL:
      return <ObjectPreviewModal {...params} />;

    case OverlayNames.OBJECT_JOB_LOG_PREVIEW_MODAL:
      return <ObjectJobLogPreviewModal {...params} />;

    case OverlayNames.JOB_ANNOTATIONS:
      return <JobAnnotations {...params} />;

    case OverlayNames.BULK_SELF_VERIFICATION_MODAL:
      return <BulkSelfVerificationModalContainer {...params} />;

    case OverlayNames.BULK_PEER_VERIFICATION_MODAL:
      return <BulkPeerVerificationModalContainer {...params} />;

    case OverlayNames.BULK_EXCEPTION_MODAL:
      return <BulkExceptionModal {...params} />;

    case OverlayNames.VIEW_EXCEPTIONS_DETAILS:
      return <ViewExceptionDetails {...params} />;

    case OverlayNames.CREATE_ACTION_MODAL:
      return <CreateActionModal {...params} />;

    case OverlayNames.SAME_SESSION_VERIFICATION_MODAL:
      return <SameSessionVerificationModal {...params} />;

    case OverlayNames.BULK_SAME_SESSION_VERIFICATION_MODAL:
      return <BulkSameSessionVerificationModal {...params} />;

    default:
      return null;
  }
};

const OverlayContainer: FC = () => {
  const dispatch = useDispatch();

  const currentOverlays = useTypedSelector((state) => state.overlayContainer.currentOverlays);

  const closeOverlay = (params: OverlayNames) => {
    dispatch(closeOverlayAction(params));
  };

  const closeAllOverlays = () => {
    dispatch(closeAllOverlayAction());
  };

  return (
    <div>
      {currentOverlays.map((overlay) =>
        getOverlay({
          ...overlay,
          // key: index.toString(),
          key: overlay.type,
          closeAllOverlays,
          closeOverlay: () => closeOverlay(overlay.type),
        }),
      )}
    </div>
  );
};

export default OverlayContainer;
