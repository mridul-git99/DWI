import { actionSpreader } from '#store/helpers';
import { Pageable } from '#utils/globalTypes';
import { Job, Verification, fetchJobsType } from '#views/Jobs/ListView/types';

import { ListViewAction } from './types';

export const fetchInbox = (params: fetchJobsType) =>
  actionSpreader(ListViewAction.FETCH_INBOX, { params });

export const fetchInboxOngoing = () => actionSpreader(ListViewAction.FETCH_INBOX_ONGOING);

export const fetchInboxSuccess = ({ data, pageable }: { data: Job[]; pageable: Pageable }) =>
  actionSpreader(ListViewAction.FETCH_INBOX_SUCCESS, { data, pageable });

export const fetchInboxError = (error: any) =>
  actionSpreader(ListViewAction.FETCH_INBOX_ERROR, { error });

export const setSelectedState = (state: string) =>
  actionSpreader(ListViewAction.SET_SELECTED_STATE, { state });

export const resetInbox = () => actionSpreader(ListViewAction.RESET_INBOX);

export const fetchVerifications = (payload: any) =>
  actionSpreader(ListViewAction.FETCH_VERIFICATIONS, payload);

export const fetchVerificationsSuccess = (payload?: {
  data: Verification[];
  pageable: Pageable | null;
}) => actionSpreader(ListViewAction.FETCH_VERIFICATIONS_SUCCESS, payload);

export const fetchApprovals = (payload: any) =>
  actionSpreader(ListViewAction.FETCH_APPROVALS, payload);

export const fetchApprovalsSuccess = (payload?: { data: any[]; pageable: Pageable | null }) =>
  actionSpreader(ListViewAction.FETCH_APPROVALS_SUCCESS, payload);

export const updateApprovalsList = (payload: any) =>
  actionSpreader(ListViewAction.UPDATE_APPROVALS_LIST, payload);
