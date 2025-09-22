import { actionSpreader } from '#store';

import { FileUploadAction, FileUploadSucessType, UploadFileType } from './types';

export const uploadFile = ({ formData }: UploadFileType) =>
  actionSpreader(FileUploadAction.UPLOAD_FILE, { formData });

export const uploadFileSuccess = (fileData: FileUploadSucessType) =>
  actionSpreader(FileUploadAction.UPLOAD_FILE_SUCCESS, { fileData });

export const uploadFileError = (error: string) =>
  actionSpreader(FileUploadAction.UPLOAD_FILE_ERROR, { error });

export const resetFileUpload = () => actionSpreader(FileUploadAction.RESET_FILE_UPLOAD);
