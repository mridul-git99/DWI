import { BaseModal, Button } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC, useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import SignatureCanvas from 'react-signature-canvas';
import styled from 'styled-components';

interface SignatureModalProps {
  user: { id: string; name: string; parameterMandatory: {} };
  onAcceptSignature: (imageData: string) => void;
}

const Wrapper = styled.div.attrs({})`
  .sign-modal-header {
    display: flex;
    align-items: center;
    padding: 7px 32px 15px;
    margin: 0px -15px;
    justify-content: space-between;
    border-bottom: 1px dashed #e0e0e0;

    > .header {
      display: flex;
      flex-direction: column;
      align-items: flex-start;

      > span {
        font-size: 20px;
        font-weight: bold;
        line-height: 0.8;
        color: #161616;
      }

      > .top-right {
        margin-top: 8px;
        display: flex;
        flex-direction: column;
        align-items: flex-start;

        > span {
          font-size: 14px;
          color: #4f4f4f;
          text-align: right;
        }
      }
    }
  }

  .sign-modal-body {
    padding: 16px 0;
    height: 350px;

    canvas {
      border-radius: 4px;
    }
  }

  .sign-modal-footer-buttons {
    display: flex;
    justify-content: space-between;
  }
`;

const SignatureModal: FC<CommonOverlayProps<SignatureModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { user, onAcceptSignature },
}) => {
  const canvasRef = useRef<any | null>(null);
  const dispatch = useDispatch();
  const [isDirty, setIsDirty] = React.useState(false);

  useEffect(() => {
    if (canvasRef && canvasRef.current) {
      const canvas = canvasRef.current.getCanvas();
      canvas.style.width = '100%';
      canvas.style.height = '100%';
      canvas.style.backgroundColor = '#fafafa';
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
    }
  }, []);

  const onSuccess = () => {
    const canvas = canvasRef.current.getCanvas();
    if (user?.parameterMandatory && !isDirty) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'Please sign before submitting.',
        }),
      );
    } else {
      onAcceptSignature(canvas.toDataURL());
      closeOverlay();
    }
  };

  const onBegin = () => {
    setIsDirty(true);
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showHeader={false}
        showFooter={false}
      >
        <div className="sign-modal-header">
          <div className="header">
            <span>Signature</span>
            <div className="top-right">
              <span>{user.name}</span>
              <span>ID: {user.id}</span>
            </div>
          </div>
        </div>
        <div className="sign-modal-body">
          <SignatureCanvas onBegin={onBegin} ref={canvasRef} backgroundColor="#fafafa" />
        </div>
        <div className="sign-modal-footer-buttons">
          <Button variant="secondary" color="red" onClick={closeOverlay}>
            Cancel
          </Button>
          <Button onClick={onSuccess}>Accept Signature</Button>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default SignatureModal;
