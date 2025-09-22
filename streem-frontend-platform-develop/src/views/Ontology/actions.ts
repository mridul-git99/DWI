import { actionSpreader } from '#store/helpers';
import {
  fetchDataType,
  fetchListSuccessType,
  TObject,
  ObjectType,
  OntologyAction,
  ObjectTypeProperty,
  ObjectTypeRelation,
} from './types';

export const fetchObjectTypes = (params: fetchDataType, appendData = false) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_TYPES, { params, appendData });

export const fetchObjectTypesSuccess = ({
  data,
  pageable,
  appendData,
}: fetchListSuccessType<ObjectType>) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_TYPES_SUCCESS, { data, pageable, appendData });

export const fetchObjectTypesError = (error: any) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_TYPES_ERROR, { error });

export const fetchObjectType = (id: string) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_TYPE, { id });

export const fetchObjectTypeSuccess = (data?: ObjectType) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_TYPE_SUCCESS, { data });

export const fetchObjectTypeError = (error: any) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_TYPE_ERROR, { error });

export const fetchObjects = (params: fetchDataType) =>
  actionSpreader(OntologyAction.FETCH_OBJECTS, { params });

export const fetchObjectsSuccess = ({ data, pageable }: fetchListSuccessType<TObject>) =>
  actionSpreader(OntologyAction.FETCH_OBJECTS_SUCCESS, { data, pageable });

export const fetchObjectsError = (error: any) =>
  actionSpreader(OntologyAction.FETCH_OBJECTS_ERROR, { error });

export const fetchObject = (id: string, params: fetchDataType) =>
  actionSpreader(OntologyAction.FETCH_OBJECT, { id, params });

export const fetchObjectSuccess = (data?: TObject) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_SUCCESS, { data });

export const fetchObjectError = (error: any) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_ERROR, { error });

export const setActiveObject = (object?: TObject) =>
  actionSpreader(OntologyAction.SET_ACTIVE_OBJECT, { object });

export const createObject = (object: any, objectTypeId: string, onDone: () => void) =>
  actionSpreader(OntologyAction.CREATE_OBJECT, { object, objectTypeId, onDone });

export const editObject = (
  object: any,
  objectTypeId: string,
  objectId: string,
  onDone: () => void,
) =>
  actionSpreader(OntologyAction.EDIT_OBJECT, {
    object,
    objectTypeId,
    objectId,
    onDone,
  });

export const resetOntology = (keys: string[]) =>
  actionSpreader(OntologyAction.RESET_ONTOLOGY, { keys });

export const archiveObject = (
  id: TObject['id'],
  reason: string,
  setFormErrors: (errors?: Error[]) => void,
  collectionName: string,
) =>
  actionSpreader(OntologyAction.ARCHIVE_OBJECT, {
    id,
    reason,
    setFormErrors,
    collectionName,
  });

export const unarchiveObject = (
  id: TObject['id'],
  reason: string,
  setFormErrors: (errors?: Error[]) => void,
  collectionName: string,
) =>
  actionSpreader(OntologyAction.UNARCHIVE_OBJECT, {
    id,
    reason,
    setFormErrors,
    collectionName,
  });

export const updateObjectsList = (id: TObject['id'], value?: TObject) =>
  actionSpreader(OntologyAction.UPDATE_OBJECTS_LIST, { id, value });

export const createObjectType = (params: any) =>
  actionSpreader(OntologyAction.CREATE_OBJECT_TYPE, { params });

export const archiveObjectTypeProperty = (params: any) =>
  actionSpreader(OntologyAction.ARCHIVE_OBJECT_TYPE_PROPERTY, { params });

export const createObjectTypeProperty = (params: any) =>
  actionSpreader(OntologyAction.CREATE_OBJECT_TYPE_PROPERTY, { params });

export const createObjectTypeRelation = (params: any) =>
  actionSpreader(OntologyAction.CREATE_OBJECT_TYPE_RELATION, { params });

export const archiveObjectTypeRelation = (params: any) =>
  actionSpreader(OntologyAction.ARCHIVE_OBJECT_TYPE_RELATION, { params });

export const editObjectTypeRelation = (params: any) =>
  actionSpreader(OntologyAction.EDIT_OBJECT_TYPE_RELATION, { params });

export const editObjectTypeProperty = (params: any) =>
  actionSpreader(OntologyAction.EDIT_OBJECT_TYPE_PROPERTY, { params });

export const editObjectType = (params: any) =>
  actionSpreader(OntologyAction.EDIT_OBJECT_TYPE, { params });

export const reorderColumns = (params: any) =>
  actionSpreader(OntologyAction.REORDER_COLUMNS, { params });

export const fetchQrShortCodeData = (params: any) =>
  actionSpreader(OntologyAction.SHORT_CODE_QR_DATA, { params });

export const editQrData = (params: any) => actionSpreader(OntologyAction.EDIT_QR_DATA, { params });

export const fetchObjectChangeLogs = (params: any) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_CHANGE_LOGS, { params });

export const fetchObjectChangeLogsSuccess = ({ data, pageable }: any) =>
  actionSpreader(OntologyAction.FETCH_OBJECT_CHANGE_LOGS_SUCCESS, { data, pageable });

export const archiveObjectTypePropertyRelationSuccess = ({
  id,
  type,
}: {
  id: ObjectTypeProperty['id'] | ObjectTypeRelation['id'];
  type: 'property' | 'relation';
}) => actionSpreader(OntologyAction.ARCHIVE_OBJECT_TYPE_PROPERTY_RELATION_SUCCESS, { id, type });
