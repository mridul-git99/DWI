import { Pageable } from '#utils/globalTypes';
import { Job, Verification } from '#views/Jobs/ListView/types';
import { RouteComponentProps } from '@reach/router';

import {
  fetchApprovals,
  fetchApprovalsSuccess,
  fetchInbox,
  fetchInboxError,
  fetchInboxOngoing,
  fetchInboxSuccess,
  fetchVerifications,
  fetchVerificationsSuccess,
  resetInbox,
  setSelectedState,
  updateApprovalsList,
} from './actions';

type TabContentProps = Record<string, any>;
export type ListViewProps = RouteComponentProps;
export type TabViewProps = RouteComponentProps<TabContentProps>;

export interface ListViewState {
  readonly jobs: Job[];
  readonly loading: boolean;
  readonly error: any;
  readonly selectedState: string;
  readonly pageable: Pageable;
  readonly verifications: {
    loading: boolean;
    list: Verification[];
    pageable: Pageable;
  };
  readonly approvals: {
    loading: boolean;
    list: any[];
    pageable: Pageable;
  };
}

export enum InboxState {
  PENDING_ON_ME = 'Pending on me',
  ALL_JOBS = 'All Jobs',
}

export enum RangeFilter {
  TODAY = 'Today',
  THIS_WEEK = 'This Week',
  THIS_MONTH = 'This Month',
  CUSTOM = 'Custom',
}

export enum QuickFilter {
  UNSCHEDULED = 'Unscheduled',
  SCHEDULED_FOR_TODAY = 'Scheduled for Today',
  ONGOING = 'Ongoing',
  PENDING_START = 'Pending Start',
  OVERDUE_FOR_COMPLETION = 'Overdue for Completion',
  COMPLETED = 'Completed',
  COMPLETED_WITH_EXCEPTION = 'Completed with Exception',
  SCHEDULED = 'Scheduled',
  OVERDUE = 'Over Due',
  START_DELAYED = 'Start Delayed',
  PENDING_APPROVAL = 'Pending Approval',
  NOT_STARTED = 'Not Started',
}

export enum ListViewAction {
  FETCH_INBOX = '@@inbox/ListView/FETCH_INBOX',
  FETCH_INBOX_ERROR = '@@inbox/ListView/FETCH_INBOX_ERROR',
  FETCH_INBOX_ONGOING = '@@inbox/ListView/FETCH_INBOX_ONGOING',
  FETCH_INBOX_SUCCESS = '@@inbox/ListView/FETCH_INBOX_SUCCESS',
  SET_SELECTED_STATE = '@@inbox/ListView/SET_SELECTED_STATE',
  RESET_INBOX = '@@inbox/ListView/RESET_INBOX',
  FETCH_VERIFICATIONS = '@@inbox/ListView/FETCH_VERIFICATIONS',
  FETCH_VERIFICATIONS_SUCCESS = '@@inbox/ListView/FETCH_VERIFICATIONS_SUCCESS',
  FETCH_APPROVALS = '@@inbox/ListView/FETCH_APPROVALS',
  FETCH_APPROVALS_SUCCESS = '@@inbox/ListView/FETCH_APPROVALS_SUCCESS',
  UPDATE_APPROVALS_LIST = '@@inbox/ListView/UPDATE_APPROVALS_LIST',
}

export type ListViewActionType = ReturnType<
  | typeof fetchInbox
  | typeof fetchInboxError
  | typeof fetchInboxOngoing
  | typeof fetchInboxSuccess
  | typeof setSelectedState
  | typeof resetInbox
  | typeof fetchVerifications
  | typeof fetchVerificationsSuccess
  | typeof fetchApprovals
  | typeof fetchApprovalsSuccess
  | typeof updateApprovalsList
>;
