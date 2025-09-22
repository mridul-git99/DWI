import ImageUpload from '#assets/svg/ImageUpload';
import { BaseModal } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { BrowserMultiFormatReader, NotFoundException } from '@zxing/library';
import React, { FC, createRef, useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const Wrapper = styled.div`
  #modal-container {
    z-index: 1301 !important;
    .modal {
      min-height: 100dvh !important;
      min-width: 100dvw !important;

      .close-icon {
        font-size: 32px !important;
        color: #fff !important;
        z-index: 10 !important;
      }

      &-body {
        padding: 0 !important;
        .video-container {
          min-height: 100dvh;
          min-width: 100dvw;
          line-height: 0;
          position: relative;
          overflow: hidden;

          .scan-region-highlight {
            outline: rgba(0, 0, 0, 0.5) solid 50vmax;
            .scan-region-highlight-svg {
              stroke: #64a2f3 !important;
              stroke-width: 8 !important;
              width: 126% !important;
              height: 126% !important;
              left: -13% !important;
              top: -13% !important;
            }
            .code-outline-highlight {
              stroke: rgba(255, 255, 255, 0.8) !important;
              stroke-width: 15 !important;
              stroke-dasharray: none !important;
            }
          }

          video {
            height: 100dvh;
            width: 100%;
            object-fit: cover;
            /* 
             * Removed transform: rotateY(180deg) to fix mirroring issue with back camera
             */
          }

          .upload {
            bottom: 100px;
            left: calc(50dvw - 10ch);
            z-index: 2;
            position: absolute;
            color: #161616;
            font-size: 16px;
            font-weight: 600;
            padding: 8px 12px;
            border-radius: 50px;
            background-color: #fff;
            display: flex;
            gap: 8px;
            align-items: center;
            cursor: pointer;
          }
        }
      }
    }
  }
`;

const UploadFromGallery = ({ onSuccess, closeOverlay }: any) => {
  const fileRef = createRef<HTMLInputElement>();
  const dispatch = useDispatch();

  const inputChangeHandler = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const imageUrl = URL.createObjectURL(file);
      const codeReader = new BrowserMultiFormatReader();

      codeReader
        .decodeFromImage(undefined, imageUrl)
        .then((result) => {
          onSuccess(result.getText());
          closeOverlay();
        })
        .catch((err) => {
          console.error(err);
          dispatch(
            showNotification({
              type: NotificationType.ERROR,
              msg: 'Not able to identify the code',
            }),
          );
        });
    }
  };

  return (
    <div className="upload" onClick={() => fileRef?.current?.click()}>
      <ImageUpload className="icon" />
      <span>Upload from Gallery</span>
      <input
        type="file"
        id="file"
        accept={'image/*'}
        ref={fileRef}
        style={{ display: 'none' }}
        onChange={inputChangeHandler}
      />
    </div>
  );
};

type Props = {
  header?: string;
  onSuccess: (data: any) => void;
  onClose?: () => void;
  onError?: any;
  hideUploadFromGallery?: boolean;
};

export const QRScanner: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { onSuccess, onError, hideUploadFromGallery, onClose },
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);

  const codeReader = useRef<BrowserMultiFormatReader | null>(null);

  useEffect(() => {
    if (!codeReader.current) {
      codeReader.current = new BrowserMultiFormatReader();
      const constraints = {
        video: {
          facingMode: 'environment', // Ensures back camera is selected
        },
      };
      codeReader.current!.decodeFromConstraints(constraints, 'qr-video', (result, err) => {
        if (result) {
          onSuccess(result.getText());
          closeOverlay();
        }
        if (err && !(err instanceof NotFoundException)) {
          console.error(err);
          onError(err);
        }
      });
    }

    return () => {
      codeReader.current?.reset();
    };
  }, []);

  const handleClose = () => {
    onClose?.();
    closeOverlay();
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={handleClose}
        showHeader={false}
        showFooter={false}
      >
        <div className="video-container">
          <video id="qr-video" ref={videoRef}></video>
          {!hideUploadFromGallery && (
            <UploadFromGallery onSuccess={onSuccess} closeOverlay={closeOverlay} />
          )}
        </div>
      </BaseModal>
    </Wrapper>
  );
};
