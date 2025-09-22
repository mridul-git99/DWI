import ImageUploadIcon from '#assets/svg/ImageUpload';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { apiUploadFile } from '#utils/apiUrls';
import { FileUploadData, ResponseError } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { SvgIconComponent } from '@material-ui/icons';
import React, { createRef, FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

type TImageUploadButtonProps = {
  icon?: SvgIconComponent;
  label?: string;
  onUploadSuccess: (data: FileUploadData) => void;
  onUploadError?: (error: string | ResponseError[]) => void;
  onUploadStart?: () => void;
  disabled?: boolean;
  allowCapture?: boolean;
  acceptedTypes?: string[];
  apiCall?: () => string;
  useCaseId?: string;
  showErrorNotification?: boolean;
};

const Wrapper = styled.div.attrs({
  className: 'upload-image',
})<Pick<TImageUploadButtonProps, 'disabled'>>`
  > div {
    align-items: center;
    cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
    display: flex;
    gap: 4px;

    .icon:only-of-type {
      cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')} !important;
    }
  }
`;

const ImageUploadButton: FC<TImageUploadButtonProps> = ({
  disabled = false,
  icon: Icon = ImageUploadIcon,
  label,
  onUploadError,
  onUploadSuccess,
  onUploadStart,
  allowCapture = false,
  acceptedTypes = ['image/*'],
  apiCall = apiUploadFile,
  useCaseId,
  showErrorNotification = false,
}) => {
  const dispatch = useDispatch();
  const [file, setFile] = useState<Blob | File | null>(null);
  const fileRef = createRef<HTMLInputElement>();

  const validateFile = (file) => {
    if (!file) return false;

    const validFileTypes = acceptedTypes.filter((type) => type.includes('/'));
    const validExtensions = acceptedTypes.filter((type) => type.startsWith('.'));
    const fileExtension = file.name.split('.').pop();

    if (validFileTypes.includes(file.type)) {
      return true;
    } else if (validExtensions.includes(`.${fileExtension.toLowerCase()}`)) {
      return true;
    } else {
      return false;
    }
  };

  useEffect(() => {
    if (file) {
      (async () => {
        let _file = file;
        if (!(file instanceof File)) {
          _file = new File([file], `file.${file.type.split('/')[1]}`, {
            type: file.type,
            lastModified: new Date().getTime(),
          });
        }
        const formData = new FormData();
        formData.append('file', _file);
        onUploadStart && onUploadStart();
        const res = await request('POST', apiCall(), {
          formData,
          ...(useCaseId && { params: { useCaseId } }),
        });

        if (res?.data) {
          onUploadSuccess(res?.data);
          setFile(null);
        } else {
          if (showErrorNotification) {
            dispatch(
              showNotification({
                type: NotificationType.ERROR,
                msg: getErrorMsg(res?.errors),
              }),
            );
          }
          console.error(res?.errors);
          onUploadError && onUploadError(res?.errors);
        }
      })();
    }
  }, [file]);

  return (
    <Wrapper disabled={disabled}>
      <input
        type="file"
        id="file"
        accept={acceptedTypes.join(' ,')}
        ref={fileRef}
        style={{ display: 'none' }}
        onChange={(event) => {
          event.stopPropagation();
          event.preventDefault();
          if (!validateFile(event?.target?.files?.[0])) {
            dispatch(
              showNotification({
                type: NotificationType.ERROR,
                msg: 'Invalid file type',
              }),
            );
            return;
          }
          setFile((event?.target?.files ?? [])[0]);
          event.currentTarget.value = '';
        }}
        {...(allowCapture && {
          onClick: (e) => {
            e.preventDefault();
            e.stopPropagation();
            dispatch(
              openOverlayAction({
                type: OverlayNames.WEBCAM_OVERLAY,
                props: { setFile },
              }),
            );
          },
        })}
      />
      <div
        onClick={() => {
          if (!disabled) {
            fileRef?.current?.click();
          }
        }}
      >
        <Icon className="icon" />
        {label ? <span>{label}</span> : null}
      </div>
    </Wrapper>
  );
};

export default ImageUploadButton;
