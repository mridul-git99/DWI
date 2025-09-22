import styled from 'styled-components';

const Wrapper = styled.div<{ isModal: boolean }>`
  #modal-container {
    ${({ isModal }) => !isModal && ``}
    ${({ isModal }) =>
      !isModal &&
      `position: unset !important;
    transform: unset !important;

    &.openup {
      transform: unset !important;
      .modal-background {
        animation: unset !important;
        .modal {
          opacity: unset !important;
          animation: unset !important;
        }
      }

      &.out {
        animation: unset !important;
        .modal-background {
          animation: unset !important;
          .modal {
            animation: unset !important;
          }
        }
      }
    }`}

    .modal {
      min-width: 487px !important;
      ${({ isModal }) =>
        !isModal &&
        `box-shadow: unset !important;
        display: flex !important;
        flex-direction: column !important;`}

      h2 {
        color: #000 !important;
        font-weight: bold !important;
      }

      > svg {
        top: 32px !important;
        right: 32px !important;
        font-size: 24px !important;
      }

      .modal-header {
        padding: 32px !important;
        border-bottom: none !important;
      }

      .modal-footer {
        padding: 24px 16px !important;
        flex-direction: row-reverse !important;

        .modal-footer-options {
          padding: 0px 16px !important;

          span {
            padding: 3px 8px;
          }
        }
      }

      .modal-body {
        padding: 0px 32px !important;

        .top-content {
          display: flex;
          align-items: center;
          flex: 1;
          justify-content: space-between;
          padding: 0px 0px 16px;

          span {
            padding: 4px 16px;
            color: #1d84ff;
            font-size: 14px;
            cursor: pointer;
          }

          .searchboxwrapper {
            position: relative;
            flex: 1;
            padding: 0px 16px;
            background-color: #f4f4f4;
            border-bottom: 1px solid #bababa;

            svg {
              background: #f4f4f4;
              border: 0;
              height: 40px;
              width: 20px;
              right: unset;
              color: #000;
              position: absolute;
              left: 16px;
              top: 0;
            }

            .searchbox {
              border: none;
              outline: none;
              font-size: 14px;
              color: #999999;
              background: #f4f4f4;
              height: 40px;
              width: calc(100% - 28px);
              margin-left: 28px;

              ::-webkit-input-placeholder {
                color: #bababa;
              }
            }
          }
        }

        .scrollable-content {
          height: calc(100dvh - (40dvh + 163px));
          overflow: auto;

          .item {
            display: flex;
            flex: 1;
            align-items: center;
            border-bottom: 1px solid #eeeeee;
            padding: 9px 0px 9px 15px;

            :last-child {
              border-bottom: none;
            }

            .thumb {
              border: 1px solid #999999;
              width: 40px;
              height: 40px;
              color: #333;
              border-radius: 20px;
              display: flex;
              align-items: center;
              justify-content: center;
              background-color: #dadada;
            }

            .middle {
              display: flex;
              flex: 1;
              flex-direction: column;
              justify-content: center;
              align-items: flex-start;
              padding: 0 15px;

              .userId {
                font-size: 8px;
                font-weight: 600;
                color: #666666;
                margin-bottom: 4px;
              }
              .userName {
                font-size: 20px;
                color: #666666;
                text-transform: capitalize;
              }
            }

            .right {
              margin-top: -15px;
            }
          }
        }
      }
    }
  }
`;

export default Wrapper;
