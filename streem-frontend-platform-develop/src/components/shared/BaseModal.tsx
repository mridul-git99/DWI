import { Button } from '#components';
import { Close } from '@material-ui/icons';
import React, { FC, ReactNode, useEffect, useRef } from 'react';
import styled, { css } from 'styled-components';

interface BaseModalProps {
  closeAllModals: () => void;
  closeModal: () => void;
  title?: string | ReactNode;
  primaryText?: string;
  secondaryText?: string;
  onPrimary?: () => void;
  onSecondary?: () => void;
  children: ReactNode;
  modalFooterOptions?: ReactNode;
  showHeader?: boolean;
  showFooter?: boolean;
  showPrimary?: boolean;
  showSecondary?: boolean;
  isRound?: boolean;
  animated?: boolean;
  disabledPrimary?: boolean;
  allowCloseOnOutsideClick?: boolean;
  showCloseIcon?: boolean;
}

const Wrapper = styled.div.attrs({ className: 'base-modal' })<{
  animated: boolean;
}>`
  #modal-container {
    position: fixed;
    display: flex;
    flex: 1;
    bottom: 0;
    right: 0;
    top: 0;
    left: 0;
    z-index: 999;

    ${({ animated }) =>
      animated
        ? css`
            transform: scale(0);
            &.openup {
              transform: scale(1);
              .modal-background {
                background: rgba(0, 0, 0, 0);
                animation: fadeIn 0.5s cubic-bezier(0.165, 0.84, 0.44, 1) forwards;
                .modal {
                  opacity: 0;
                  animation: scaleUp 0.5s cubic-bezier(0.165, 0.84, 0.44, 1) forwards;
                }
              }

              &.out {
                animation: quickScaleDown 0s 0.5s linear forwards;
                .modal-background {
                  animation: fadeOut 0.5s cubic-bezier(0.165, 0.84, 0.44, 1) forwards;
                  .modal {
                    animation: scaleDown 0.5s cubic-bezier(0.165, 0.84, 0.44, 1) forwards;
                  }
                }
              }
            }
          `
        : ``}

    .modal-background {
      display: flex;
      flex: 1;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.8);
      .modal {
        overflow: hidden;
        background: white;
        display: flex;
        flex-direction: column;
        font-weight: 300;
        position: relative;
        min-width: 400px;
        max-width: 90dvw;
        max-height: 70dvh;
        box-shadow: 0 1px 10px 0 rgba(0, 0, 0, 0.12), 0 4px 5px 0 rgba(0, 0, 0, 0.14),
          0 2px 4px -1px rgba(0, 0, 0, 0.2);
        h2 {
          font-size: 20px;
          font-weight: 600;
          color: #000000;
          line-height: 20px;
          margin: 0;
        }
        .close-icon {
          cursor: pointer;
          font-size: 20px;
          position: absolute;
          top: 16px;
          right: 16px;
          color: #000000;
        }
        .modal-header {
          display: flex;
          align-items: center;
          padding: 16px;
          border-bottom: 1px solid #eeeeee;
          justify-content: space-between;
        }
        .modal-footer {
          display: flex;
          align-items: center;
          padding: 16px;
          border-top: 1px solid #eeeeee;
        }
        .modal-footer-options {
          display: flex;
          justify-content: flex-start;

          .without-notify-btn {
            color: #1d84ff;
            cursor: pointer;
            font-weight: 600;
            font-size: 14;
          }

          .disabled {
            cursor: default;
            opacity: 0.4;
          }
        }
        .modal-footer-buttons {
          display: flex;
        }
        .modal-body {
          padding: 16px;
          overflow: auto;
          line-height: 20px;
          overflow-wrap: anywhere;
        }
      }
    }
  }

  .escape-overlay {
    position: fixed;
    top: 0;
    left: 0;
    height: 100%;
    width: 100%;
    z-index: 0;
  }

  @keyframes fadeIn {
    0% {
      background: rgba(0, 0, 0, 0);
    }
    100% {
      background: rgba(0, 0, 0, 0.3);
    }
  }

  @keyframes fadeOut {
    0% {
      background: rgba(0, 0, 0, 0.3);
    }
    100% {
      background: rgba(0, 0, 0, 0);
    }
  }

  @keyframes scaleUp {
    0% {
      transform: scale(0.8) translateY(1000px);
      opacity: 0;
    }
    100% {
      transform: scale(1) translateY(0px);
      opacity: 1;
    }
  }

  @keyframes scaleDown {
    0% {
      transform: scale(1) translateY(0px);
      opacity: 1;
    }
    100% {
      transform: scale(0.8) translateY(1000px);
      opacity: 0;
    }
  }

  @keyframes quickScaleDown {
    0% {
      transform: scale(1);
    }
    99.9% {
      transform: scale(1);
    }
    100% {
      transform: scale(0);
    }
  }
`;

export const BaseModal: FC<BaseModalProps> = ({
  onPrimary = () => false,
  onSecondary = () => false,
  children,
  modalFooterOptions,
  title,
  primaryText,
  secondaryText,
  closeModal,
  showPrimary = true,
  showSecondary = true,
  showHeader = true,
  showFooter = true,
  isRound = false,
  animated = true,
  disabledPrimary = false,
  allowCloseOnOutsideClick = true,
  showCloseIcon = true,
}) => {
  const modalContainer = useRef<HTMLDivElement | null>(null);

  const onBaseModalContainerClick = (toExecute: () => void) => {
    animated
      ? setTimeout(() => {
          (() => {
            if (modalContainer && modalContainer.current) {
              modalContainer.current.classList.add('out');
            }
            toExecute();
          })();
        }, 200)
      : (() => {
          if (modalContainer && modalContainer.current) {
            modalContainer.current.classList.add('out');
          }
          toExecute();
        })();
  };

  useEffect(() => {
    if (allowCloseOnOutsideClick) {
      document.addEventListener('keydown', (evt) => (evt.key === 'Escape' ? closeModal() : null));
    }
    window.addEventListener('popstate', closeModal);

    return () => {
      if (allowCloseOnOutsideClick) {
        document.removeEventListener('keydown', (evt) =>
          evt.key === 'Escape' ? closeModal() : null,
        );
      }
      window.removeEventListener('popstate', closeModal);
    };
  }, []);

  return (
    <Wrapper animated={animated}>
      <div id="modal-container" ref={modalContainer} className="openup">
        <div className="modal-background">
          <div className="modal" style={{ borderRadius: isRound ? '16px' : 'unset', zIndex: 10 }}>
            {showCloseIcon && (
              <Close
                className="close-icon"
                data-testid="base-modal-close-icon"
                onClick={() => onBaseModalContainerClick(closeModal)}
              />
            )}
            {showHeader && (
              <div className="modal-header" {...(showCloseIcon && { style: { paddingRight: 48 } })}>
                <h2>{title}</h2>
              </div>
            )}
            <div className="modal-body">{children}</div>
            {showFooter && (
              <div className="modal-footer">
                {modalFooterOptions && (
                  <div className="modal-footer-options">{modalFooterOptions}</div>
                )}
                <div className="modal-footer-buttons">
                  {showSecondary && (
                    <Button
                      color="red"
                      variant="secondary"
                      onClick={() => onBaseModalContainerClick(onSecondary)}
                    >
                      {secondaryText}
                    </Button>
                  )}
                  {showPrimary && (
                    <Button
                      onClick={() => onBaseModalContainerClick(onPrimary)}
                      disabled={disabledPrimary}
                    >
                      {primaryText}
                    </Button>
                  )}
                </div>
              </div>
            )}
          </div>

          {allowCloseOnOutsideClick ? (
            <div
              className="escape-overlay"
              key="escape-overlay"
              role="presentation"
              onClick={() => closeModal()}
            />
          ) : null}
        </div>
      </div>
    </Wrapper>
  );
};
