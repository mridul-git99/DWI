import { withStyles, Switch } from '@material-ui/core';
import styled from 'styled-components';

export const Wrapper = styled.div.attrs({})<{ comments: boolean }>`
  .modal {
    min-width: unset !important;
    max-width: unset !important;

    @media (max-width: 900px) {
      max-width: 780px !important;
    }

    ${({ comments }) => {
      return comments
        ? `
        .close-icon {
      top: 38px !important;
      right: 16px !important;
      font-size: 24px !important;
    }
    min-height: 700px !important;
    width: 1000px !important;
    `
        : `
        .close-icon {
      top: 22px !important;
      right: 16px !important;
      font-size: 24px !important;
    }
    width: unset !important;
    `;
    }}

    h2 {
      color: #000 !important;
      font-weight: bold !important;
      height: 20px;
    }

    .modal-header {
      padding: 24px 32px !important;
      border-bottom: 1px solid #eeeeee !important;
    }

    .modal-footer {
      padding: 24px 16px !important;
      flex-direction: row-reverse !important;
      border-top: none !important;

      .modal-footer-options {
        padding: 0px 16px !important;

        span {
          padding: 3px 8px;
        }
      }

      .modal-footer-buttons {
        padding: 0px 16px !important;
        justify-content: flex-start !important;

        button {
          padding: 12px 24px !important;

          :first-child {
            color: #ff6b6b;
            border-color: #ff6b6b;
          }
        }
      }
    }

    .modal-body {
      ${({ comments }) =>
        comments
          ? `
      padding: 0px !important;
      `
          : `
      padding: 48px !important;
      align-items: center;
      `}
      flex: 1 !important;
      font-size: 14px;
      color: #000000;
      display: flex;
      flex-direction: column;

      .box-wrapper {
        display: flex;
        justify-content: center;
        align-items: center;

        .box {
          width: 240px;
          height: 240px;
          display: flex;
          justify-content: center;
          align-items: center;
          border-radius: 4px;
          border: solid 1px #eeeeee;
          flex-direction: column;
          cursor: pointer;
          margin-left: 80px;

          :first-child {
            margin-left: 0px;
          }

          .icon-wrapper {
            border-radius: 40px;
            display: flex;
            justify-content: center;
            align-items: center;

            svg {
              width: 150px;
              height: 150px;
            }
          }

          h3 {
            font-size: 20px;
            line-height: 27px;
            font-weight: bold;
            color: #000000;
            margin: 0px;
          }

          span {
            font-size: 12px;
            color: #666666;
            margin-top: 4px;
          }
        }
      }

      .caption {
        font-size: 14px;
        color: #999999;
        padding-top: 120px;
      }

      .submit {
        width: 240px;
        margin-right: 0px;
        justify-content: center;
        margin: 24px 0px 15px 0px;
      }

      .header {
        display: flex;
        padding: 12px 16px;
        border-bottom: 1px solid #dadada;

        .header-left {
          display: flex;
          flex: 1;
          flex-direction: column;
          align-items: flex-start;
          justify-content: center;

          h5 {
            font-size: 14px;
            margin: 0px;
          }

          h4 {
            font-size: 20px;
            font-weight: bold;
            margin: 4px 0px 8px 0px;
            text-transform: capitalize;
          }

          h6 {
            align-items: center;
            display: flex;
            font-size: 12px;
            margin: 0px 0px 0px -3px;
            text-transform: capitalize;

            .icon {
              color: #1d84ff;
            }
          }
        }

        .header-right {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: flex-end;
          padding-right: 35px;

          h6 {
            font-size: 12px;
            line-height: 12px;
            color: #999999;
            margin: 2px 0px 0px 8px;
          }

          .icon-wrapper {
            width: 40px;
            height: 40px;
            margin-left: 16px;
            border-radius: 20px;
            display: flex;
            justify-content: center;
            align-items: center;
            border: solid 1px #dadada;
            position: relative;
            cursor: pointer;
          }
        }
      }

      .body {
        display: flex;
        flex: 1;

        .box {
          display: flex;
          flex: 1;
          flex-direction: column;
          padding: 24px;

          :first-child {
            border-right: 1px solid #eeeeee;
          }

          .filter-input {
            position: relative;
            width: 100%;
            display: flex;

            > div {
              display: flex;
              flex: 1;
              flex-direction: column;
              margin-left: 6px;
              max-width: 240px;

              :first-child {
                margin-left: 0px;
                margin-right: 6px;
              }

              .select {
                .button {
                  border-bottom: 1px solid #999999;
                }
                svg {
                  font-size: 14px;
                }
                .option-list {
                  border: none !important;

                  .option-list-item {
                    text-align: left;
                    cursor: pointer;
                  }
                }
              }
            }

            h6 {
              font-size: 12px;
              line-height: 1.33;
              letter-spacing: 0.32px;
              color: #000000;
              margin: 0px 0px 8px 0px;
              text-align: left;
            }

            .wrapper {
              margin-top: 0px !important;

              &::after {
                color: #000000;
                content: '⌄';
                position: absolute;
                right: 10px;
                top: 6px;
                font-size: 18px;
              }

              input {
                border-bottom: 1px solid #999999 !important;
              }
            }

            .input {
              font-size: 14px !important;
              padding: 10px 16px !important;
              color: #000 !important;
              background-color: #f4f4f4;
            }

            .collaborators-wrapper {
              position: absolute;
              top: 60px;
              left: 0px;
              width: 220px;
              background: #fff;
              z-index: 1;
              border-radius: 4px;
              box-shadow: 0 1px 5px 0 rgba(0, 0, 0, 0.12), 0 2px 2px 0 rgba(0, 0, 0, 0.14),
                0 3px 1px -2px rgba(0, 0, 0, 0.2);

              .reviewer-row {
                padding: 8px 12px;
                display: flex;
                flex-direction: row;
                justify-content: space-between;
                align-items: center;
                background-color: #fff;
                cursor: pointer;
              }

              .reviewer-row:hover {
                background-color: rgba(29, 132, 255, 0.2);
              }
            }
          }

          .comments-section {
            display: flex;
            flex: 1;
            flex-direction: column;
            max-height: 500px;
            margin-right: -24px;
            padding-right: 24px;
            overflow: auto;

            .no-comments {
              display: flex;
              flex: 1;
              align-items: center;
              color: #999999;
              justify-content: center;
            }

            .reviews-group {
              display: flex;
              flex: 1;
              flex-direction: column;
              padding: 24px 0px;
              border-bottom: 1px solid #eeeeee;

              > span {
                font-size: 14px;
                font-weight: bold;
                color: #000000;
              }

              .comments-group {
                display: flex;
                flex: 1;
                flex-direction: column;
                align-items: flex-start;

                .comment-section {
                  width: 100%;
                  .user-detail {
                    padding: 24px 0px 8px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;

                    > div {
                      display: flex;
                      align-items: center;
                      > div {
                        :first-child {
                          color: #1d84ff;
                          border: none;
                          background-color: #ecedf1;
                        }
                      }

                      h5 {
                        margin: 0;
                        font-size: 14px;
                        margin-left: 4px;
                      }

                      h4 {
                        margin: 0;
                        margin-left: 8px;
                        font-weight: bold;
                        font-size: 14px;
                      }

                      span {
                        font-size: 12px;
                        color: #999999;
                      }
                    }
                  }

                  .comment {
                    background-color: #f4f4f4;
                    padding: 16px 16px 32px 16px;
                    text-align: left;
                    margin-left: 40px;
                    border-radius: 4px;
                  }
                }
              }
            }
          }

          .editor-wrapper {
            overflow-y: auto;
            min-height: 500px;
            max-height: calc(100dvh - 300px);
            border-radius: 4px;
            border: solid 1px #bababa;
            margin-bottom: 16px;
            display: flex;
            flex-direction: column-reverse;

            .DraftEditor-root {
              margin-top: -15px;
              padding: 8px;
            }
          }

          .editor {
            word-break: break-all;
            overflow-wrap: break-word;

            * {
              font-weight: unset;
            }
          }

          .editor-toolbar {
            margin-bottom: 0px;
            opacity: 0.6;
            border: none;
            border-top: 1px solid #bababa;

            .rdw-option-wrapper {
              border: none !important;
            }
          }

          .actions-container {
            display: flex;
            flex: 1;
          }
        }
      }
    }
  }
`;

export const AntSwitch = withStyles(() => ({
  root: {
    width: 28,
    height: 16,
    padding: 0,
    display: 'flex',
  },
  switchBase: {
    padding: 2,
    color: '#bababa',
    '&$checked': {
      transform: 'translateX(12px)',
      color: '#FFF',
      '& + $track': {
        opacity: 1,
        backgroundColor: '#5aa700',
        borderColor: '#5aa700',
      },
    },
  },
  thumb: {
    width: 12,
    height: 12,
    boxShadow: 'none',
    '&:before': {
      content: '""',
      color: '#5aa700',
      left: '5px',
      top: '3.9px',
      width: '2.5px',
      height: '5px',
      border: 'solid #5aa700',
      borderWidth: '0 2px 2px 0',
      transform: 'rotate(45deg)',
      position: 'absolute',
    },
  },
  track: {
    border: `1px solid #bababa`,
    borderRadius: 16 / 2,
    opacity: 1,
    backgroundColor: '#FFF',
  },
  checked: {},
}))(Switch);
