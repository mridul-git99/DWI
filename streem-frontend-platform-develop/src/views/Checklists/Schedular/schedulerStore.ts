import { generateActions } from '#store/helpers';
import { DEFAULT_PAGINATION } from '#utils/constants';
import { EntityBaseState } from '#views/Ontology/types';
import { Pageable, ResponseError } from '#utils/globalTypes';
import { Collaborator } from '#PrototypeComposer/reviewer.types';
import { Checklist } from '#PrototypeComposer/checklist.types';

//TODO: types
const actions = {
  saveScheduler: { data: {} as SchedulerType, handleClose: {} as () => void },
  saveSchedulerError: { errors: {} as ResponseError[] | undefined },
  saveSchedulerSuccess: { data: {} as SchedulerType },
  fetchSchedulers: { params: {} as SchedulerType },
  fetchSchedulersSuccess: { data: {} as SchedulerType, pageable: {} as Pageable | null },
  archiveScheduler: { schedularId: '', reason: '', setFormErrors: {} as any },
  updateSchedulerList: { id: '' },
  fetchSchedulersVersionHistory: { schedularId: '' },
  modifyScheduler: { schedularId: '', data: {} as SchedulerType, handleClose: {} as () => void },
  modifySchedulerError: { errors: {} as ResponseError[] | undefined },
  modifySchedulerSuccess: { id: '', data: {} as SchedulerType },
  fetchSchedulersVersionHistorySuccess: {
    data: {} as SchedulerType,
  },
  fetchChecklistInfo: { checklistId: '' },
  fetchChecklistInfoSuccess: { data: {} as ChecklistInfo },
};

const { actions: schedulerActions, actionsEnum: SchedulerActionsEnum } = generateActions(
  actions,
  '@@leucine/process/entity/scheduler/',
);

type SchedulerActionsType = ReturnType<typeof schedulerActions[keyof typeof schedulerActions]>;

const initialState: SchedulerState = {
  activeLoading: true,
  listLoading: true,
  submitting: false,
  list: [],
  pageable: DEFAULT_PAGINATION,
  // Active used for both version history and single scheduler data
  active: {},
  checklistInfo: null,
};

export type SchedulerType = any;

export const SchedulerReducer = (
  state = initialState,
  action: SchedulerActionsType,
): SchedulerState => {
  switch (action.type) {
    case SchedulerActionsEnum.saveScheduler:
    case SchedulerActionsEnum.modifyScheduler:
      return { ...state, submitting: true };
    case SchedulerActionsEnum.saveSchedulerError:
    case SchedulerActionsEnum.modifySchedulerError:
      return { ...state, submitting: false };
    case SchedulerActionsEnum.saveSchedulerSuccess:
      return { ...state, submitting: false, list: [action.payload, ...state.list] };
    case SchedulerActionsEnum.fetchSchedulersSuccess:
      return { ...state, list: action.payload.data, pageable: action.payload.pageable };

    case SchedulerActionsEnum.updateSchedulerList:
      return { ...state, list: state.list.filter((item: any) => item.id !== action.payload.id) };

    case SchedulerActionsEnum.fetchSchedulersVersionHistorySuccess:
      return { ...state, active: action.payload.data };

    case SchedulerActionsEnum.modifySchedulerSuccess:
      const { id, data } = action.payload;
      const listUpdated = state.list.filter((item: any) => item.id !== id);
      return { ...state, submitting: false, list: [data, ...listUpdated] };

    case SchedulerActionsEnum.fetchChecklistInfoSuccess:
      return { ...state, checklistInfo: action.payload.data };

    default:
      return { ...state };
  }
};

type Author = Pick<
  Collaborator,
  'modifiedAt' | 'email' | 'employeeId' | 'firstName' | 'lastName' | 'id' | 'state' | 'type'
> & { orderTree: number };

type SignOffUser = Pick<
  Author,
  'id' | 'employeeId' | 'email' | 'firstName' | 'lastName' | 'orderTree' | 'state'
> & { signedAt: number };

type Version = Pick<Checklist, 'id' | 'code' | 'name' | 'versionNumber'> & {
  deprecatedAt: number;
};

type Audit = {
  createdAt: number;
  modifiedAt: number;
  modifiedBy: Pick<Collaborator, 'id' | 'employeeId' | 'firstName' | 'lastName'>;
  createdBy: Pick<Collaborator, 'id' | 'employeeId' | 'firstName' | 'lastName'>;
};

type ChecklistInfo = Pick<
  Checklist,
  'id' | 'name' | 'code' | 'description' | 'state' | 'versionNumber' | 'phase'
> & {
  authors: Author[];
  signOff: SignOffUser[];
  release: {
    releaseAt: number;
    releaseBy: Pick<Collaborator, 'id' | 'firstName' | 'lastName' | 'employeeId'>;
  };
  versions: Version[];
  audit: Audit;
};

export interface SchedulerState extends EntityBaseState<SchedulerType> {
  checklistInfo: ChecklistInfo | null;
  submitting: boolean;
}

export default { schedulerActions, SchedulerActionsEnum };
