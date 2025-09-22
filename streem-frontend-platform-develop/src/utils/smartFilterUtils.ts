import { AssignedJobStates, CompletedJobStates } from '#views/Jobs/ListView/types';
import { FilterOperators } from './globalTypes';
import {
  startOfDay,
  endOfDay,
  getUnixTime,
  startOfWeek,
  endOfWeek,
  startOfMonth,
  endOfMonth,
} from 'date-fns';
import { QuickFilter, RangeFilter } from '#views/Inbox/ListView/types';

export const getActiveSmartFilter = (cards: any, activeCard: string) => {
  const activeSmartFilters = cards
    .find((item) => item.label === activeCard)
    ?.filters?.reduce((acc, filter) => {
      acc[filter.field] = true;
      return acc;
    }, {});

  return activeSmartFilters;
};

const buildRangeFilter = ({
  field,
  startDate,
  endDate,
  isEpoch = false,
}: {
  field: string;
  startDate: Date | number;
  endDate: Date | number;
  isEpoch?: boolean;
}) => {
  return [
    {
      field,
      op: FilterOperators.GOE,
      values: isEpoch ? [startDate.toString()] : [getUnixTime(startDate).toString()],
    },
    {
      field,
      op: FilterOperators.LOE,
      values: isEpoch ? [endDate.toString()] : [getUnixTime(endDate).toString()],
    },
  ];
};

export const getRangeFilter = ({
  type,
  field = 'startedAt',
  customStart,
  customEnd,
}: {
  type: RangeFilter;
  field?: string;
  customStart?: number;
  customEnd?: number;
}) => {
  switch (type) {
    case RangeFilter.TODAY:
      const todayStart = startOfDay(new Date());
      const todayEnd = endOfDay(new Date());
      return buildRangeFilter({ field, startDate: todayStart, endDate: todayEnd });
    case RangeFilter.THIS_WEEK:
      const thisWeekStart = startOfWeek(new Date());
      const thisWeekEnd = endOfWeek(new Date());
      return buildRangeFilter({ field, startDate: thisWeekStart, endDate: thisWeekEnd });
    case RangeFilter.THIS_MONTH:
      const thisMonthStart = startOfMonth(new Date());
      const thisMonthEnd = endOfMonth(new Date());
      return buildRangeFilter({ field, startDate: thisMonthStart, endDate: thisMonthEnd });
    case RangeFilter.CUSTOM:
      if (!customStart || !customEnd) {
        return [];
      }
      return buildRangeFilter({ field, startDate: customStart, endDate: customEnd, isEpoch: true });
  }
};

export const getQuickFilter = (type: QuickFilter, showOnlyCompleteState: boolean = false) => {
  switch (type) {
    case QuickFilter.UNSCHEDULED:
      return [
        {
          field: 'expectedStartDate',
          op: FilterOperators.IS_NOT_SET,
          values: [],
        },
      ];

    case QuickFilter.SCHEDULED_FOR_TODAY:
      return getRangeFilter({ type: RangeFilter.TODAY, field: 'expectedStartDate' });

    case QuickFilter.ONGOING:
      return [
        {
          field: 'state',
          op: FilterOperators.ANY,
          values: [AssignedJobStates.IN_PROGRESS, AssignedJobStates.BLOCKED],
        },
      ];

    case QuickFilter.NOT_STARTED:
      return [
        {
          field: 'state',
          op: FilterOperators.EQ,
          values: [AssignedJobStates.ASSIGNED],
        },
      ];

    case QuickFilter.PENDING_START:
      return [
        {
          field: 'expectedStartDate',
          op: FilterOperators.LOE,
          values: [getUnixTime(new Date()).toString()],
        },
        {
          field: 'state',
          op: FilterOperators.EQ,
          values: [AssignedJobStates.ASSIGNED],
        },
      ];

    case QuickFilter.OVERDUE_FOR_COMPLETION:
      return [
        {
          field: 'expectedEndDate',
          op: FilterOperators.LT,
          values: [getUnixTime(new Date()).toString()],
        },
        {
          field: 'state',
          op: FilterOperators.ANY,
          values: [
            AssignedJobStates.IN_PROGRESS,
            AssignedJobStates.BLOCKED,
            AssignedJobStates.ASSIGNED,
          ],
        },
      ];

    case QuickFilter.COMPLETED:
      return [
        {
          field: 'state',
          op: FilterOperators.ANY,
          values: showOnlyCompleteState
            ? [CompletedJobStates.COMPLETED]
            : [CompletedJobStates.COMPLETED, CompletedJobStates.COMPLETED_WITH_EXCEPTION],
        },
      ];

    case QuickFilter.COMPLETED_WITH_EXCEPTION:
      return [
        {
          field: 'state',
          op: FilterOperators.ANY,
          values: [CompletedJobStates.COMPLETED_WITH_EXCEPTION],
        },
      ];

    case QuickFilter.SCHEDULED:
      return [
        {
          field: 'expectedStartDate',
          op: FilterOperators.GT,
          values: [0],
        },
      ];

    case QuickFilter.OVERDUE:
      return [
        {
          field: 'expectedEndDate',
          op: FilterOperators.LT,
          values: [getUnixTime(new Date()).toString()],
        },
      ];

    case QuickFilter.START_DELAYED:
      return [
        {
          field: 'expectedStartDate',
          op: FilterOperators.LT,
          values: [getUnixTime(new Date()).toString()],
        },
      ];

    case QuickFilter.PENDING_APPROVAL:
      return [
        {
          field: 'state',
          op: FilterOperators.EQ,
          values: [AssignedJobStates.BLOCKED],
        },
      ];
  }
};

export const getQuickFiltersForInbox = () => {
  return [
    QuickFilter.UNSCHEDULED,
    QuickFilter.SCHEDULED_FOR_TODAY,
    QuickFilter.ONGOING,
    QuickFilter.PENDING_START,
    QuickFilter.OVERDUE_FOR_COMPLETION,
  ].map((filter) => {
    return {
      label: filter,
      value: filter,
    };
  });
};

export const getQuickFiltersForJobs = () => {
  return [
    QuickFilter.SCHEDULED,
    QuickFilter.UNSCHEDULED,
    QuickFilter.OVERDUE,
    QuickFilter.START_DELAYED,
  ].map((filter) => {
    return {
      label: filter,
      value: filter,
    };
  });
};

export const getQuickFiltersForCompleteJobs = () => {
  return [QuickFilter.COMPLETED, QuickFilter.COMPLETED_WITH_EXCEPTION].map((filter) => {
    return {
      label: filter,
      value: filter,
    };
  });
};

const generateFilter = (field: string, op: FilterOperators, value: any, prefixKey?: string) => {
  return {
    field: prefixKey ? `${prefixKey}.${field}` : field,
    op,
    values: [value],
  };
};

export const generateUserSearchFilters = (op: FilterOperators, value: any, prefixKey?: string) => {
  const fields = ['firstName', 'lastName', 'employeeId', 'email'];
  const filters = {
    op: FilterOperators.OR,
    fields: fields.map((field) => generateFilter(field, op, value, prefixKey)),
  };
  return filters;
};

export const generateGroupSearchFilters = (value: any) => {
  return {
    op: FilterOperators.AND,
    fields: [
      {
        field: 'name',
        op: FilterOperators.LIKE,
        values: [value],
      },
      {
        field: 'active',
        op: FilterOperators.EQ,
        values: [true],
      },
    ],
  };
};
