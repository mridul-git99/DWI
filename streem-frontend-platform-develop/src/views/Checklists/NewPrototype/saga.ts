import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { RootState } from '#store';
import {
  apiCreateNewPrototype,
  apiCreateRevisionPrototype,
  apiUpdatePrototype,
} from '#utils/apiUrls';
import { request } from '#utils/request';
import { navigate } from '@reach/router';
import { isArray, pick } from 'lodash';
import { call, put, select, takeLeading } from 'redux-saga/effects';

import { addNewPrototype, addRevisionPrototype, updatePrototype } from './actions';
import { Author, FormMode, FormValuesOnlyWithAuthorIds, NewPrototypeActions } from './types';

type transformFormDataType = {
  data: FormValuesOnlyWithAuthorIds;
  mode: FormMode;
  originalAuthors?: Author['id'][];
};

const transformFormData = ({ data, mode, originalAuthors }: transformFormDataType) => {
  const addAuthorIds: Author['id'][] = [],
    removeAuthorIds: Author['id'][] = [];

  data.authors.forEach((authorId) => {
    if (!originalAuthors?.includes(authorId)) {
      addAuthorIds.push(authorId);
    }
  });

  originalAuthors?.forEach((authorId) => {
    if (!data.authors.includes(authorId)) {
      removeAuthorIds.push(authorId);
    }
  });

  return {
    description: data.description,
    name: data.name,
    useCaseId: data.useCaseId,
    colorCode: data.colorCode,
    properties: data.properties.map((property) => ({
      ...pick(property, ['id', 'name', 'value']),
    })),

    ...(mode === FormMode.ADD
      ? { authors: data.authors.filter((authorId) => authorId !== '0') }
      : {
          addAuthorIds: addAuthorIds.filter((authorId) => authorId !== '0'),
          removeAuthorIds: removeAuthorIds,
        }),
  };
};

function* addPrototypeSaga({ payload }: ReturnType<typeof addNewPrototype>) {
  try {
    const { data, setApiFormErrors } = payload;

    const { selectedFacility } = yield select((state: RootState) => state.auth);

    const { data: response, errors } = yield call(request, 'POST', apiCreateNewPrototype(), {
      data: {
        ...transformFormData({ data, mode: FormMode.ADD }),
        facilityId: selectedFacility?.id,
      },
    });

    if (response) {
      navigate(`/checklists/${response.id}`);
    } else {
      setApiFormErrors(errors);
      console.error('error from the create checklist api  :: ', errors);
    }
  } catch (error) {
    console.error('error came in addPrototypeSaga in NewPrototypeSaga :: ', error);
  }
}

function* addRevisionPrototypeSaga({ payload }: ReturnType<typeof addRevisionPrototype>) {
  try {
    const { checklistId, code, name } = payload;

    const { data: response, errors } = yield call(
      request,
      'POST',
      apiCreateRevisionPrototype(checklistId),
      { data: { id: checklistId } },
    );

    if (response) {
      navigate('/checklists/prototype', {
        state: {
          mode: FormMode.EDIT,
          formData: {
            description: response.description,
            name: response.name,
            properties: response.properties,
            createdBy: response.audit?.createdBy,
            prototypeId: response.id,
            revisedCode: code,
            revisedName: name,
            colorCode: response.colorCode,
          },
        },
      });
    } else {
      console.error('error from the revision checklist api  :: ', errors);
      if (isArray(errors) && errors[0].code === 'E122' && errors[0].id) {
        yield put(
          openOverlayAction({
            type: OverlayNames.REVISION_ERROR,
            props: {
              id: errors[0].id,
            },
          }),
        );
      }
    }
  } catch (error) {
    console.error('error came in addRevisionPrototypeSaga in NewPrototypeSaga :: ', error);
  }
}

function* updatePrototypeSaga({ payload }: ReturnType<typeof updatePrototype>) {
  try {
    const { data, id, originalAuthors, setApiFormErrors } = payload;

    const { selectedFacility } = yield select((state: RootState) => state.auth);

    const { data: response, errors } = yield call(request, 'PATCH', apiUpdatePrototype(id), {
      data: {
        ...transformFormData({ data, originalAuthors, mode: FormMode.EDIT }),
        facilityId: selectedFacility?.id,
      },
    });

    if (response) {
      navigate(`/checklists/${id}`);
    } else {
      setApiFormErrors(errors);
      console.error('error from the create checklist api  :: ', errors);
    }
  } catch (error) {
    console.error('error came in addPrototypeSaga in NewPrototypeSaga :: ', error);
  }
}

export function* NewPrototypeSaga() {
  yield takeLeading(NewPrototypeActions.ADD_NEW_PROTOTYPE, addPrototypeSaga);
  yield takeLeading(NewPrototypeActions.UPDATE_PROTOTYPE, updatePrototypeSaga);
  yield takeLeading(NewPrototypeActions.ADD_REVISION_PROTOTYPE, addRevisionPrototypeSaga);
}
