import { produce } from 'immer';
import { ScheduleCalendar } from '../../types/schedule';
import { generateActions } from '../../store/helpers';
import { startOfDay } from 'date-fns';

const actions = {
  fetchScheduleList: {
    startTime: '',
    endTime: '',
    filters: {},
  },
  fetchScheduleListSuccess: {
    scheduleList: [] as any[],
  },
  fetchScheduleListFailure: {
    error: '',
  },
};

export const initialState: ScheduleCalendar = {
  isLoading: true,
  id: '',
  errors: {},
  list: {},
};

export const { actions: scheduleActions, actionsEnum: ScheduleActionsEnum } = generateActions(
  actions,
  '@@leucine/schedule/entity/',
);

export type ScheduleActionsType = ReturnType<typeof scheduleActions[keyof typeof scheduleActions]>;

export const scheduleCalendarReducer = (state = initialState, action: ScheduleActionsType) =>
  produce(state, (draft) => {
    switch (action.type) {
      case ScheduleActionsEnum.fetchScheduleList:
        draft.isLoading = true;
        break;
      case ScheduleActionsEnum.fetchScheduleListSuccess:
        draft.isLoading = false;
        draft.list = action.payload.scheduleList.reduce((acc, item) => {
          const key = startOfDay(item.start).getTime();
          if (!acc[key]) {
            acc[key] = [];
          }
          acc[key].push(item);
          return acc;
        }, {});
        break;
      default:
        break;
    }
  });
