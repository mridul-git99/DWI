import { Error } from '#utils/globalTypes';
import {
  copyEntities,
  copyEntitiesError,
  copyEntitiesSuccess,
  executeBranchingRulesParameter,
  fetchComposerDataError,
  fetchComposerDataOngoing,
  fetchComposerDataSuccess,
  fetchJobLogColumns,
  fetchJobLogColumnsSuccess,
  processParametersMapSuccess,
  quickPublish,
  quickPublishSuccess,
  resetChecklistValidationErrors,
  resetComposer,
  setChecklistValidationErrors,
  updateHiddenParameterIds,
} from './actions';
import { deleteParameterSuccess, updateStoreParameter } from './Activity/actions';
import { ParameterListState } from './Activity/reducer.types';
import { Checklist } from './checklist.types';
import { ChecklistAuditLogsState } from './ChecklistAuditLogs/types';
import {
  assignReviewerToChecklist,
  fetchApproversSuccess,
  fetchAssignedReviewersForChecklistSuccess,
  recallProcess,
  revertReviewersForChecklist,
  unAssignReviewerFromChecklist,
  updateChecklistForReview,
} from './reviewer.actions';
import { Collaborator } from './reviewer.types';
import { StageListState } from './Stages/reducer.types';
import { TaskListState } from './Tasks/reducer.types';
import { ComposerEntity } from './types';

export type ComposerState = {
  readonly parameters: ParameterListState;
  readonly approvers: Collaborator[];
  readonly collaborators: Collaborator[];
  readonly data?: Checklist;
  readonly entity?: ComposerEntity;
  readonly error?: any;
  readonly loading: boolean;
  readonly stages: StageListState;
  readonly tasks: TaskListState;
  readonly auditLogs: ChecklistAuditLogsState;
  readonly errors: Error[];
  readonly isCopying: boolean;
  readonly jobLogColumnsLoading: boolean;
};

export enum ComposerAction {
  ASSIGN_REVIEWERS_TO_CHECKLIST = '@@prototypeComposer/prototype/ASSIGN_REVIEWERS_TO_CHECKLIST',
  ASSIGN_REVIEWER_TO_CHECKLIST = '@@prototypeComposer/prototype/ASSIGN_REVIEWER_TO_CHECKLIST',
  FETCH_APPROVERS = '@@prototypeComposer/prototype/FETCH_APPROVERS',
  FETCH_APPROVERS_SUCCESS = '@@prototypeComposer/prototype/FETCH_APPROVERS_SUCCESS',
  FETCH_COMPOSER_DATA = '@@prototypeComposer/prototype/FETCH_COMPOSER_DATA',
  FETCH_COMPOSER_DATA_ERROR = '@@prototypeComposer/prototype/FETCH_COMPOSER_DATA_ERROR',
  FETCH_COMPOSER_DATA_ONGOING = '@@prototypeComposer/prototype/FETCH_COMPOSER_DATA_ONGOING',
  FETCH_COMPOSER_DATA_SUCCESS = '@@prototypeComposer/prototype/FETCH_COMPOSER_DATA_SUCCESS',
  FETCH_REVIEWERS_FOR_CHECKLIST = '@@prototypeComposer/prototype/FETCH_REVIEWERS_FOR_CHECKLIST',
  FETCH_REVIEWERS_FOR_CHECKLIST_SUCCESS = '@@prototypeComposer/prototype/FETCH_REVIEWERS_FOR_CHECKLIST_SUCCESS',
  EXECUTE_LATEST_BRANCHING_RULES = '@@prototypeComposer/prototype/EXECUTE_LATEST_BRANCHING_RULES',
  UPDATE_HIDDEN_PARAMETER_IDS = '@@prototypeComposer/prototype/UPDATE_HIDDEN_PARAMETER_IDS',
  INITIATE_SIGNOFF = '@@prototypeComposer/prototype/INITIATE_SIGNOFF',
  RELEASE_PROTOTYPE = '@@prototypeComposer/prototype/RELEASE_PROTOTYPE',
  RESET_COMPOSER = '@@prototypeComposer/prototype/RESET_COMPOSER',
  REVERT_REVIEWERS_FOR_CHECKLIST = '@@prototypeComposer/prototype/REVERT_REVIEWERS_FOR_CHECKLIST',
  SEND_REVIEW_TO_CR = '@@prototypeComposer/prototype/SEND_REVIEW_TO_CR',
  SIGN_OFF_PROTOTYPE = '@@prototypeComposer/prototype/SIGN_OFF_PROTOTYPE',
  START_CHECKLIST_REVIEW = '@@prototypeComposer/prototype/START_CHECKLIST_REVIEW',
  SUBMIT_CHECKLIST_FOR_REVIEW = '@@prototypeComposer/prototype/SUBMIT_CHECKLIST_FOR_REVIEW',
  SUBMIT_CHECKLIST_REVIEW = '@@prototypeComposer/prototype/SUBMIT_CHECKLIST_REVIEW',
  SUBMIT_CHECKLIST_REVIEW_WITH_CR = '@@prototypeComposer/prototype/SUBMIT_CHECKLIST_REVIEW_WITH_CR',
  UNASSIGN_REVIEWER_FROM_CHECKLIST = '@@prototypeComposer/prototype/UNASSIGN_REVIEWER_FROM_CHECKLIST',
  UPDATE_FOR_REVIEW_PROCESS = '@@prototypeComposer/prototype/UPDATE_FOR_REVIEW_PROCESS',
  VALIDATE_PROTOTYPE = '@@prototypeComposer/prototype/validate',
  PROCESS_PARAMETER_MAP_SUCCESS = '@@prototypeComposer/prototype/PROCESS_PARAMETER_MAP_SUCCESS',
  RECALL_PROCESS = '@@prototypeComposer/prototype/RECALL_PROCESS',
  SET_CHECKLIST_VALIDATION_ERRORS = '@@prototypeComposer/prototype/SET_CHECKLIST_VALIDATION_ERRORS',
  RESET_CHECKLIST_VALIDATION_ERRORS = '@@prototypeComposer/prototype/RESET_CHECKLIST_VALIDATION_ERRORS',
  COPY_ENTITY = '@@prototypeComposer/prototype/COPY_ENTITY',
  COPY_ENTITY_SUCCESS = '@@prototypeComposer/prototype/COPY_ENTITY_SUCCESS',
  COPY_ENTITY_ERROR = '@@prototypeComposer/prototype/COPY_ENTITY_ERROR',
  QUICK_PUBLISH = '@@prototypeComposer/prototype/QUICK_PUBLISH',
  QUICK_PUBLISH_SUCCESS = '@@prototypeComposer/prototype/QUICK_PUBLISH_SUCCESS',
  FETCH_JOB_LOG_COLUMNS = '@@prototypeComposer/prototype/FETCH_JOB_LOG_COLUMNS',
  FETCH_JOB_LOG_COLUMNS_SUCCESS = '@@prototypeComposer/prototype/FETCH_JOB_LOG_COLUMNS_SUCCESS',
}

export type ComposerActionType = ReturnType<
  | typeof assignReviewerToChecklist
  | typeof fetchApproversSuccess
  | typeof fetchAssignedReviewersForChecklistSuccess
  | typeof fetchComposerDataError
  | typeof fetchComposerDataOngoing
  | typeof fetchComposerDataSuccess
  | typeof resetComposer
  | typeof revertReviewersForChecklist
  | typeof unAssignReviewerFromChecklist
  | typeof updateChecklistForReview
  | typeof processParametersMapSuccess
  | typeof updateStoreParameter
  | typeof executeBranchingRulesParameter
  | typeof updateHiddenParameterIds
  | typeof deleteParameterSuccess
  | typeof recallProcess
  | typeof setChecklistValidationErrors
  | typeof resetChecklistValidationErrors
  | typeof copyEntities
  | typeof copyEntitiesSuccess
  | typeof copyEntitiesError
  | typeof quickPublish
  | typeof quickPublishSuccess
  | typeof fetchJobLogColumnsSuccess
  | typeof fetchJobLogColumns
>;
