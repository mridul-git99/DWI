import { Pageable } from '#utils/globalTypes';

import {
  fetchSessionActivities,
  fetchSessionActivitiesError,
  fetchSessionActivitiesOngoing,
  fetchSessionActivitiesSuccess,
} from './actions';

export interface SessionActivity {
  triggeredOn: string;
  id: string;
  triggeredAt: number;
  event: string;
  action: string;
  severity: string;
  oldData: string | null;
  newData: string | null;
  diffData: string | null;
  details: string;
}

export interface SessionActivityState {
  readonly logs: SessionActivity[];
  readonly pageable: Pageable;
  readonly loading: boolean;
  readonly error?: string;
}

export enum SessionActivityAction {
  FETCH_SESSION_ACTIVITY = '@@sessionActivity/SessionActivity/FETCH_SESSION_ACTIVITY',
  FETCH_SESSION_ACTIVITY_ERROR = '@@sessionActivity/SessionActivity/FETCH_SESSION_ACTIVITY_ERROR',
  FETCH_SESSION_ACTIVITY_ONGOING = '@@sessionActivity/SessionActivity/FETCH_SESSION_ACTIVITY_ONGOING',
  FETCH_SESSION_ACTIVITY_SUCCESS = '@@sessionActivity/SessionActivity/FETCH_SESSION_ACTIVITY_SUCCESS',
}

export enum SessionActivitySeverity {
  CRITICAL = 'CRITICAL',
  ERROR = 'ERROR',
  WARNING = 'WARNING',
  INFORMATION = 'INFORMATION',
  UNKNOWN = 'UNKNOWN',
}

export type SessionActivityActionType = ReturnType<
  | typeof fetchSessionActivities
  | typeof fetchSessionActivitiesError
  | typeof fetchSessionActivitiesOngoing
  | typeof fetchSessionActivitiesSuccess
>;
