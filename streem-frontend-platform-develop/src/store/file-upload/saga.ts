import { apiUploadFile } from '#utils/apiUrls';
import { request } from '#utils/request';
import { call, put, takeLatest } from 'redux-saga/effects';

import { uploadFile, uploadFileError, uploadFileSuccess } from './actions';
import { FileUploadAction } from './types';

function* uploadFileSaga({ payload }: ReturnType<typeof uploadFile>) {
  try {
    const { formData } = payload;

    const { data, errors } = yield call(request, 'POST', apiUploadFile(), {
      formData: formData,
    });

    if (data) {
      yield put(uploadFileSuccess(data));
    } else {
      yield put(uploadFileError(errors));
    }
  } catch (error) {
    console.error('error came in file upload saga :: ', error);
  }
}

export function* FileUploadSagaNew() {
  yield takeLatest(FileUploadAction.UPLOAD_FILE, uploadFileSaga);
}
