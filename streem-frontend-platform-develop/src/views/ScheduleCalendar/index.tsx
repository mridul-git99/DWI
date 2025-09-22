import { useTypedSelector } from '#store';
import { FilterOperators } from '#utils/globalTypes';
import { scheduleActions } from '#views/ScheduleCalendar/scheduleStore';
import { RouteComponentProps } from '@reach/router';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { CalendarContent } from './CalendarContent';
import { DisabledStates } from '#PrototypeComposer/checklist.types';

const ScheduleCalendarWrapper = styled.div`
  padding: 8px;
`;

const ScheduleCalendar: FC<RouteComponentProps> = () => {
  const dispatch = useDispatch();
  const useCaseId = useTypedSelector((state) => state.auth?.selectedUseCase?.id || '');

  const fetchData = (start, end) => {
    dispatch(
      scheduleActions.fetchScheduleList({
        startTime: start,
        endTime: end,
        filters: {
          op: 'AND',
          fields: [
            {
              op: FilterOperators.EQ,
              field: 'archived',
              values: [false],
            },
            {
              op: FilterOperators.EQ,
              field: 'useCaseId',
              values: [useCaseId],
            },
            {
              op: FilterOperators.NE,
              field: 'state',
              values: [DisabledStates.DEPRECATED],
            },
          ],
        },
      }),
    );
  };

  return (
    <ScheduleCalendarWrapper>
      <CalendarContent fetchData={fetchData} />
    </ScheduleCalendarWrapper>
  );
};

export default ScheduleCalendar;
