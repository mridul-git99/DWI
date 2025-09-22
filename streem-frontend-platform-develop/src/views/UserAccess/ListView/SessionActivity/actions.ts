import { actionSpreader } from '#store/helpers';
import { ResponseObj } from '#utils/globalTypes';

import { SessionActivity, SessionActivityAction } from './types';

export const fetchSessionActivities = (payload?: Record<string, string | number>) =>
  actionSpreader(SessionActivityAction.FETCH_SESSION_ACTIVITY, payload);

export const fetchSessionActivitiesOngoing = () =>
  actionSpreader(SessionActivityAction.FETCH_SESSION_ACTIVITY_ONGOING);

export const fetchSessionActivitiesSuccess = ({
  data,
  pageable,
}: Partial<ResponseObj<SessionActivity[]>>) =>
  actionSpreader(SessionActivityAction.FETCH_SESSION_ACTIVITY_SUCCESS, {
    data,
    pageable,
  });

export const fetchSessionActivitiesError = (error: string) =>
  actionSpreader(SessionActivityAction.FETCH_SESSION_ACTIVITY_ERROR, { error });
