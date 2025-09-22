import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import {
  apiArchiveObject,
  apiArchiveObjectTypeProperty,
  apiArchiveObjectTypeRelation,
  apiCreateObjectType,
  apiCreateObjectTypeProperty,
  apiCreateObjectTypeRelation,
  apiEditObjectType,
  apiEditObjectTypeProperty,
  apiEditObjectTypeRelation,
  apiGetObjectAuditChangeLog,
  apiGetObjects,
  apiGetObjectTypes,
  apiQrShortCode,
  apiUnArchiveObject,
  apiReorderColumns,
} from '#utils/apiUrls';
import { Error, ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { call, put, select, takeLatest, takeLeading } from 'redux-saga/effects';
import * as actions from './actions';
import { ObjectType, OntologyAction } from './types';
import { RootState } from '#store';

function* fetchObjectTypesSaga({ payload }: ReturnType<typeof actions.fetchObjectTypes>) {
  try {
    const { params, appendData } = payload;
    const { data, pageable, errors }: ResponseObj<ObjectType[]> = yield call(
      request,
      'GET',
      apiGetObjectTypes(),
      {
        params,
      },
    );

    if (data && pageable) {
      yield put(actions.fetchObjectTypesSuccess({ data, pageable, appendData }));
    }

    if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    console.error('error from fetchObjectTypesSaga function in Ontology Saga :: ', error);
    yield put(actions.fetchObjectTypesError(error));
  }
}

function* fetchObjectTypeSaga({ payload }: ReturnType<typeof actions.fetchObjectType>) {
  try {
    const { id } = payload;
    const { data } = yield call(request, 'GET', apiGetObjectTypes(id));

    if (data) {
      yield put(actions.fetchObjectTypeSuccess(data));
    }
  } catch (error) {
    console.error('error from fetchObjectTypeSaga function in Ontology Saga :: ', error);
    yield put(actions.fetchObjectTypeError(error));
  }
}

function* fetchObjectSaga({ payload }: ReturnType<typeof actions.fetchObject>) {
  try {
    const { id, params } = payload;
    const { data } = yield call(request, 'GET', apiGetObjects(id), { params });

    if (data) {
      yield put(actions.fetchObjectSuccess(data));
    }
  } catch (error) {
    console.error('error from fetchObjectSaga function in Ontology Saga :: ', error);
    yield put(actions.fetchObjectError(error));
  }
}

function* fetchObjectsSaga({ payload }: ReturnType<typeof actions.fetchObjects>) {
  try {
    const { params } = payload;
    const { data, pageable } = yield call(request, 'GET', apiGetObjects(), {
      params,
    });

    if (data) {
      yield put(actions.fetchObjectsSuccess({ data, pageable }));
    }
  } catch (error) {
    console.error('error from fetchObjectsSaga function in Ontology Saga :: ', error);
    yield put(actions.fetchObjectsError(error));
  }
}

function* objectActionSaga({
  payload,
}: ReturnType<typeof actions.createObject | typeof actions.editObject>) {
  try {
    const isEditing = 'objectId' in payload;
    const { object, objectTypeId, onDone } = payload;
    const { data, errors }: ResponseObj<any> = yield call(
      request,
      isEditing ? 'PATCH' : 'POST',
      apiGetObjects(isEditing ? payload.objectId : undefined),
      { data: { ...object, objectTypeId } },
    );
    if (errors) {
      throw getErrorMsg(errors);
    }

    if (data) {
      yield put(actions.setActiveObject(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Object ${isEditing ? 'Updated' : 'created'} successfully`,
        }),
      );
    }

    onDone();
  } catch (e) {
    yield* handleCatch(
      'ObjectsView',
      `objectActionSaga ${'objectId' in payload ? 'Updating' : 'Creating'}`,
      e,
      true,
    );
  }
}

function* archiveObjectSaga({ payload }: ReturnType<typeof actions.archiveObject>) {
  try {
    const { id, reason, setFormErrors, collectionName } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiArchiveObject(id), {
      data: { reason, collectionName },
    });

    if (data) {
      yield put(actions.updateObjectsList(id));
      yield put(
        openOverlayAction({
          type: OverlayNames.ARCHIVE_MODAL,
          props: { mode: 'archive' },
        }),
      );
      setFormErrors(errors);
    } else {
      if ((errors as Error[]).some((error) => error.code === 'E120')) {
        setFormErrors(undefined);
        yield put(
          openOverlayAction({
            type: OverlayNames.ARCHIVE_MODAL,
            props: { mode: 'cannotArchive' },
          }),
        );
      } else {
        setFormErrors(errors);
        console.error('error from apiArchiveObject :: ', errors);
      }
    }
  } catch (error) {
    console.error('error in archiveObjectSaga :: ', error);
  }
}

function* unarchiveObjectSaga({ payload }: ReturnType<typeof actions.unarchiveObject>) {
  try {
    const { id, reason, setFormErrors, collectionName } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiUnArchiveObject(id), {
      data: { reason, collectionName },
    });

    if (data) {
      yield put(actions.updateObjectsList(id));
      yield put(
        openOverlayAction({
          type: OverlayNames.ARCHIVE_MODAL,
          props: { mode: 'unarchive' },
        }),
      );
    } else {
      console.error('error from apiUnarchiveObject :: ', errors);
    }

    setFormErrors(errors);
  } catch (error) {
    console.error('error in unarchiveObjectSaga :: ', error);
  }
}

function* createObjectTypeSaga({ payload }: ReturnType<typeof actions.createObjectType>) {
  try {
    const {
      params: { data: objectTypeData, navigate },
    } = payload;
    const { data, errors } = yield call(request, 'POST', apiCreateObjectType(), {
      data: objectTypeData,
    });
    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `New Object Type Created Successfully`,
        }),
      );
      navigate('/ontology');
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'createObjectTypeSaga', error, true);
  }
}

function* archiveObjectTypePropertySaga({
  payload,
}: ReturnType<typeof actions.archiveObjectTypeProperty>) {
  try {
    const {
      params: { objectTypeId, propertyId, reason, setFormErrors, fetchProperties },
    } = payload;

    const { data, errors } = yield call(
      request,
      'PATCH',
      apiArchiveObjectTypeProperty(objectTypeId, propertyId),
      {
        data: { reason },
      },
    );

    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Object Property Archived`,
        }),
      );
      yield put(
        actions.archiveObjectTypePropertyRelationSuccess({
          id: propertyId,
          type: 'property',
        }),
      );
      setFormErrors(errors);
      fetchProperties();
    } else if (errors) {
      setFormErrors(errors);
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'archiveObjectTypePropertySaga', error, true);
  }
}

function* createObjectTypePropertySaga({
  payload,
}: ReturnType<typeof actions.createObjectTypeProperty>) {
  try {
    const {
      params: { objectTypeId, data },
    } = payload;
    const { data: _data, errors } = yield call(
      request,
      'PATCH',
      apiCreateObjectTypeProperty(objectTypeId),
      {
        data,
      },
    );
    if (_data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `New Object Type Property Created Successfully`,
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'createObjectTypePropertySaga', error, true);
  }
}

function* createObjectTypeRelationSaga({
  payload,
}: ReturnType<typeof actions.createObjectTypeRelation>) {
  try {
    const {
      params: { objectTypeId, data },
    } = payload;
    const { data: _data, errors } = yield call(
      request,
      'PATCH',
      apiCreateObjectTypeRelation(objectTypeId),
      {
        data: data,
      },
    );
    if (_data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `New Object Type Relation Created Successfully`,
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'createObjectTypeRelationSaga', error, true);
  }
}

function* archiveObjectTypeRelationSaga({
  payload,
}: ReturnType<typeof actions.archiveObjectTypeRelation>) {
  try {
    const {
      params: { objectTypeId, relationId, reason, setFormErrors, fetchRelations },
    } = payload;

    const { data, errors } = yield call(
      request,
      'PATCH',
      apiArchiveObjectTypeRelation(objectTypeId, relationId),
      {
        data: { reason },
      },
    );
    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Object Relation Archived`,
        }),
      );
      yield put(
        actions.archiveObjectTypePropertyRelationSuccess({
          id: relationId,
          type: 'relation',
        }),
      );
      setFormErrors(errors);
      fetchRelations();
    } else if (errors) {
      setFormErrors(errors);
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'archiveObjectTypeRelationSaga ', error, true);
  }
}

function* editObjectTypeRelationSaga({ payload }: ReturnType<any>) {
  try {
    const {
      params: { objectTypeId, data: editedData, relationId },
    } = payload;

    const { data, errors } = yield call(
      request,
      'PATCH',
      apiEditObjectTypeRelation(objectTypeId, relationId),
      {
        data: editedData,
      },
    );

    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Object Relation Updated`,
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'editObjectTypeRelationSaga', error, true);
  }
}

function* editObjectTypePropertySaga({ payload }: ReturnType<any>) {
  try {
    const {
      params: { objectTypeId, data: editedData, propertyId },
    } = payload;
    const { data, errors } = yield call(
      request,
      'PATCH',
      apiEditObjectTypeProperty(objectTypeId, propertyId),
      {
        data: editedData,
      },
    );

    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Object Property Updated`,
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'editObjectTypePropertySaga', error, true);
  }
}

function* editObjectTypeSaga({ payload }: ReturnType<any>) {
  try {
    const {
      params: { objectTypeId, data: editedData, navigate },
    } = payload;
    const { data, errors } = yield call(request, 'PATCH', apiEditObjectType(objectTypeId), {
      data: editedData,
    });
    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Object Type Updated`,
        }),
      );
      navigate('/ontology');
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'editObjectTypeSaga', error, true);
  }
}

function* postQrShortCodeSaga({ payload }: ReturnType<typeof actions.fetchQrShortCodeData>) {
  try {
    const { params } = payload;
    const { object, handlePrintQRCode } = params;
    const { data, errors } = yield call(request, 'POST', apiQrShortCode(), {
      data: {
        objectId: object?.id,
        objectTypeId: object?.objectType?.id,
      },
    });

    if (data?.shortCode) {
      yield put(actions.updateObjectsList(object?.id, data));
      yield put(
        openOverlayAction({
          type: OverlayNames.QR_GENERATOR,
          props: {
            data: data?.shortCode,
            selectedObject: object,
            id: 'QRCode',
            onPrimary: handlePrintQRCode,
            primaryText: 'Print QR',
            title: `QR Code for ${object?.displayName}`,
          },
        }),
      );
    } else {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('Ontology', 'postQrShortCodeSaga', error, true);
  }
}

function* postQrEditSaga({ payload }: ReturnType<typeof actions.editQrData>) {
  try {
    const { params } = payload;
    const { data, errors } = yield call(request, 'PATCH', apiQrShortCode(), {
      data: params,
    });

    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `QR code has been updated`,
        }),
      );
    } else {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('Ontology', 'patchQrShortCodeSaga', error, true);
  }
}

function* fetchObjectChangeLogsSaga({ payload }: ReturnType<typeof actions.fetchObjectChangeLogs>) {
  try {
    const { params } = payload;
    const { data, pageable } = yield call(request, 'GET', apiGetObjectAuditChangeLog(), {
      params,
    });
    if (data) {
      yield put(actions.fetchObjectChangeLogsSuccess({ data, pageable }));
    }
  } catch (error) {
    console.error('error from fetchObjectChangeLogsSaga function in Ontology Saga :: ', error);
  }
}

function* reorderColumnsSaga({ payload }: ReturnType<typeof actions.reorderColumns>) {
  try {
    const { objectTypeId, propertySortOrderMap, relationSortOrderMap, reason } = payload.params;
    const { data, errors } = yield call(request, 'PATCH', apiReorderColumns(objectTypeId), {
      data: {
        propertySortOrderMap,
        relationSortOrderMap,
        reason,
      },
    });
    if (data) {
      const { id: activeId }: RootState['job'] = yield select(
        (state: RootState) => state.ontology.objectTypes.active,
      );

      if (activeId === data.id) {
        yield put(actions.fetchObjectTypeSuccess(data));
      }
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Column Configured in desired arrangement`,
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('ontology', 'editObjectTypePropertySaga', error, true);
  }
}

export function* OntologySaga() {
  yield takeLatest(OntologyAction.FETCH_OBJECT_TYPES, fetchObjectTypesSaga);
  yield takeLatest(OntologyAction.FETCH_OBJECT_TYPE, fetchObjectTypeSaga);
  yield takeLatest(OntologyAction.FETCH_OBJECT, fetchObjectSaga);
  yield takeLatest(OntologyAction.FETCH_OBJECTS, fetchObjectsSaga);
  yield takeLeading(OntologyAction.CREATE_OBJECT, objectActionSaga);
  yield takeLeading(OntologyAction.EDIT_OBJECT, objectActionSaga);
  yield takeLatest(OntologyAction.ARCHIVE_OBJECT, archiveObjectSaga);
  yield takeLatest(OntologyAction.UNARCHIVE_OBJECT, unarchiveObjectSaga);
  yield takeLatest(OntologyAction.CREATE_OBJECT_TYPE, createObjectTypeSaga);
  yield takeLatest(OntologyAction.ARCHIVE_OBJECT_TYPE_PROPERTY, archiveObjectTypePropertySaga);
  yield takeLatest(OntologyAction.CREATE_OBJECT_TYPE_PROPERTY, createObjectTypePropertySaga);
  yield takeLatest(OntologyAction.CREATE_OBJECT_TYPE_RELATION, createObjectTypeRelationSaga);
  yield takeLatest(OntologyAction.ARCHIVE_OBJECT_TYPE_RELATION, archiveObjectTypeRelationSaga);
  yield takeLatest(OntologyAction.EDIT_OBJECT_TYPE_RELATION, editObjectTypeRelationSaga);
  yield takeLatest(OntologyAction.EDIT_OBJECT_TYPE_PROPERTY, editObjectTypePropertySaga);
  yield takeLatest(OntologyAction.EDIT_OBJECT_TYPE, editObjectTypeSaga);
  yield takeLatest(OntologyAction.SHORT_CODE_QR_DATA, postQrShortCodeSaga);
  yield takeLatest(OntologyAction.EDIT_QR_DATA, postQrEditSaga);
  yield takeLatest(OntologyAction.FETCH_OBJECT_CHANGE_LOGS, fetchObjectChangeLogsSaga);
  yield takeLatest(OntologyAction.REORDER_COLUMNS, reorderColumnsSaga);
}
