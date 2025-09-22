import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC, useState } from 'react';
import styled from 'styled-components';
import Webcam from 'react-webcam';

const Wrapper = styled.div`
  .modal {
    min-height: 100dvh !important;
    min-width: 100dvw !important;

    .modal-body {
      padding: 0px !important;
      height: 100vh;
      width: calc(100dvw - 80px);
      margin: 0px 0px 0px 40px;
      justify-content: center;
      align-items: center;
      display: flex;
      flex-direction: column;

      > button {
        position: absolute;
        bottom: 40px;
        height: 10dvh;
        width: 10dvh;
        border-radius: 50%;
        background-color: #fff;
        cursor: pointer;
      }

      > span {
        font-size: 14px;
        color: #000;
        position: absolute;
        top: 50%;
        z-index: 1;
      }
    }
  }
`;

const WebCamOverlay: FC<
  CommonOverlayProps<{
    setFile: (file: Blob) => void;
  }>
> = ({ closeAllOverlays, closeOverlay, props: { setFile } }) => {
  const [state, setState] = useState<{
    loading: boolean;
    error?: boolean;
    videoConstraints: MediaTrackConstraints;
  }>({
    loading: true,
    videoConstraints: {
      facingMode: { exact: 'environment' },
    },
  });
  const { loading, error, videoConstraints } = state;
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        showHeader={false}
      >
        {loading && !error && <span>Loading...</span>}
        {!loading && error && <span>There seems to be an issue. Please Try Again..</span>}
        <Webcam
          height={'100%'}
          screenshotFormat="image/jpeg"
          width={'100%'}
          forceScreenshotSourceSize
          videoConstraints={videoConstraints}
          onUserMedia={() => {
            setState((prev) => ({
              ...prev,
              loading: false,
            }));
          }}
          onUserMediaError={(error) => {
            if (videoConstraints.facingMode === 'user') {
              setState((prev) => ({
                ...prev,
                loading: false,
                error: !!error,
              }));
            } else {
              setState((prev) => ({
                ...prev,
                videoConstraints: {
                  facingMode: 'user',
                },
              }));
            }
          }}
        >
          {({ getScreenshot }) =>
            loading ? (
              <div />
            ) : (
              <button
                onClick={async () => {
                  const imageSrc = getScreenshot();
                  const blob = await fetch(imageSrc!).then((res) => res.blob());
                  setFile(blob);
                  closeOverlay();
                }}
              ></button>
            )
          }
        </Webcam>
      </BaseModal>
    </Wrapper>
  );
};

export default WebCamOverlay;
