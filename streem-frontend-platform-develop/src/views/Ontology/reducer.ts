import { DEFAULT_PAGINATION } from '#utils/constants';
import { get, set } from 'lodash';
import { EntityBaseState, OntologyAction, OntologyActionType, OntologyState } from './types';

const initialEntityBase = {
  list: [],
  listLoading: true,
  activeLoading: true,
  pageable: DEFAULT_PAGINATION,
};

const initialState: OntologyState = {
  objectTypes: initialEntityBase,
  objects: initialEntityBase,
  objectChangeLogs: initialEntityBase,
};

const reducer = (state = initialState, action: OntologyActionType): OntologyState => {
  const updateKey = (key: keyof OntologyState, value: Partial<EntityBaseState>) => ({
    ...state,
    [key]: { ...state[key], ...value },
  });

  switch (action.type) {
    case OntologyAction.FETCH_OBJECTS:
      return updateKey('objects', { listLoading: true });

    case OntologyAction.FETCH_OBJECT:
      return updateKey('objects', { activeLoading: true });

    case OntologyAction.FETCH_OBJECT_TYPES:
      return updateKey('objectTypes', { listLoading: true });

    case OntologyAction.FETCH_OBJECT_TYPE:
      return updateKey('objectTypes', { activeLoading: true });

    case OntologyAction.FETCH_OBJECT_TYPES_SUCCESS:
      return updateKey('objectTypes', {
        listLoading: false,
        list: action.payload.appendData
          ? [...state.objectTypes.list, ...action.payload.data]
          : action.payload.data,
        pageable: action.payload.pageable,
      });

    case OntologyAction.FETCH_OBJECT_TYPE_SUCCESS:
      return updateKey('objectTypes', {
        activeLoading: false,
        active: action.payload?.data,
      });

    case OntologyAction.FETCH_OBJECT_SUCCESS:
      return updateKey('objects', {
        activeLoading: false,
        active: action.payload?.data,
      });

    case OntologyAction.FETCH_OBJECTS_SUCCESS:
      return updateKey('objects', {
        listLoading: false,
        list: action.payload.data,
        pageable: action.payload.pageable,
      });

    case OntologyAction.FETCH_OBJECT_ERROR:
    case OntologyAction.FETCH_OBJECTS_ERROR:
      return updateKey('objects', {
        listLoading: false,
        activeLoading: false,
        error: action.payload?.error,
      });

    case OntologyAction.FETCH_OBJECT_TYPES_ERROR:
    case OntologyAction.FETCH_OBJECT_TYPE_ERROR:
      return updateKey('objectTypes', {
        listLoading: false,
        activeLoading: false,
        error: action.payload?.error,
      });

    case OntologyAction.SET_ACTIVE_OBJECT:
      return updateKey('objects', {
        activeLoading: false,
        active: action.payload?.object,
      });

    case OntologyAction.RESET_ONTOLOGY:
      return set(state, action.payload.keys, get(initialState, action.payload.keys));

    case OntologyAction.UPDATE_OBJECTS_LIST:
      return updateKey('objects', {
        listLoading: false,
        list: state.objects.list.reduce<Object[]>((acc, item) => {
          if (item.id === action.payload.id) {
            if (action.payload.value) {
              acc.push({
                ...item,
                ...action.payload.value,
              });
            }
          } else {
            acc.push(item);
          }
          return acc;
        }, []),
      });

    case OntologyAction.FETCH_OBJECT_CHANGE_LOGS:
      return updateKey('objectChangeLogs', { listLoading: true });

    case OntologyAction.FETCH_OBJECT_CHANGE_LOGS_SUCCESS:
      return updateKey('objectChangeLogs', {
        listLoading: false,
        list: action.payload.data,
        pageable: action.payload.pageable,
      });

    case OntologyAction.ARCHIVE_OBJECT_TYPE_PROPERTY_RELATION_SUCCESS:
      const { id, type } = action.payload;

      if (type === 'property') {
        return updateKey('objectTypes', {
          active: {
            ...state.objectTypes.active,
            properties: state.objectTypes?.active?.properties.filter(
              (property) => property.id !== id,
            ),
          },
        });
      } else if (type === 'relation') {
        return updateKey('objectTypes', {
          active: {
            ...state.objectTypes.active,
            relations: state.objectTypes?.active?.relations.filter(
              (relation) => relation.id !== id,
            ),
          },
        });
      }

    default:
      return { ...state };
  }
};

export { reducer as OntologyReducer };
