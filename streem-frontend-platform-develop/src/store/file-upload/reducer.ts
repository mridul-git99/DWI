import { Reducer } from 'redux';

import { FileUploadAction, FileUploadActionType, FileUploadState } from './types';

const initialState: FileUploadState = {
  data: undefined,
  error: undefined,
};

const reducer: Reducer<FileUploadState, FileUploadActionType> = (state = initialState, action) => {
  switch (action.type) {
    case FileUploadAction.UPLOAD_FILE_ERROR:
      return { ...state, error: action.payload.error };

    case FileUploadAction.UPLOAD_FILE_SUCCESS:
      return { ...state, data: action.payload.fileData };

    case FileUploadAction.RESET_FILE_UPLOAD:
      return { ...initialState };

    default:
      return { ...state };
  }
};

export { reducer as FileUploadReducer };
