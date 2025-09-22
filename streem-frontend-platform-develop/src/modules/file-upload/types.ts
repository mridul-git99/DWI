import { Parameter } from '#types';

export enum FileUploadAction {
  UPLOAD_FILE = '@@file-upload/UPLOAD_FILE',
  UPLOAD_FILE_ERROR = '@@file-upload/UPLOAD_FILE_ERROR',
  UPLOAD_FILE_SUCCESS = '@@file-upload/UPLOAD_FILE_SUCCESS',
}

export type UploadFileType = {
  formData: FormData;
  parameter?: Parameter;
  isCorrectingError: boolean;
  setCorrectedParameterValues?: React.Dispatch<React.SetStateAction<any>>;
};
