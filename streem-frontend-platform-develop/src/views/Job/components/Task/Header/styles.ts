import styled from 'styled-components';

const Wrapper = styled.div.attrs({
  className: 'task-header',
})`
  grid-area: task-header;
  .task-config {
    display: flex;
    flex-direction: column;
    padding: 16px;
    border-bottom: 1px solid #e0e0e0;
  }

  .task-header {
    display: flex;
    flex-direction: column;

    .task-banner {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 4px 8px;
      height: 24px;
      font-size: 12px;
      line-height: 16px;
    }

    .task-overdue-banner {
      color: #a2191f;
      background-color: #ffd7d9;
    }

    .scheduled-task-banner {
      color: #0043ce;
      background-color: #d0e2ff;
    }

    .task-info {
      padding-block: 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      line-height: 12px;
      letter-spacing: 0.32px;
      color: #525252;
      border-bottom: 1px solid #e0e0e0;
      .left-section {
        display: flex;
        align-items: center;
        > div {
          padding-inline: 16px;
          border-right: 1px solid #e0e0e0;
          :last-child {
            border-right: none;
          }
        }

        .icon-bg {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 8px;
          border-radius: 64px;
          background-color: #f4f4f4;
          color: #1d84ff;
        }
      }
      .right-section {
        padding-right: 16px;
        .more {
          padding: 6px;
          border: none;
          cursor: pointer;
          background-color: #f4f4f4;
          color: #161616;
          display: flex;
          align-items: center;
        }
      }
    }

    .task-error {
      padding: 12px 16px;
      background-color: white;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .task-error-wrapper {
      align-items: center;
      color: #eb5757;
      display: flex;
      font-size: 12px;
      justify-content: flex-start;

      .task-error-icon {
        font-size: 16px;
        color: #eb5757;
        margin-right: 5px;
      }
    }

    .start-audit {
      background-color: #f4f4f4;
      padding: 8px 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: #999999;
    }

    .task-config {
      .wrapper {
        align-items: center;
        display: flex;

        .task-name {
          color: #161616;
          flex: 1;
          font-size: 16px;
          font-weight: 600;
          line-height: 1.25;
        }
      }

      .task-timer {
        align-items: flex-start;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .timer-config {
          display: flex;

          > div {
            display: flex;
            flex-direction: column;

            span {
              color: #000000;
              font-size: 14px;
              line-height: 1.14;
              letter-spacing: 0.16px;

              :nth-child(2n) {
                margin-top: 8px;
                color: #999999;
                letter-spacing: 0.32px;
                font-size: 12px;
              }
            }
          }

          > .icon {
            margin-right: 8px;
            font-size: 24px;
            color: #525252;
          }
        }

        .timer {
          display: flex;
          flex-direction: column;
          align-items: center;
          font-size: 24px;
          line-height: 24px;
          font-weight: 700;
          color: #525252;

          span {
            :first-child {
              padding: 4px;
              font-weight: 700;
            }

            :nth-child(2n) {
              color: #ff6b6b;
              margin-top: 8px;
            }
          }

          &.error {
            color: #ff6b6b;
            font-weight: 700;
          }
        }
      }
    }

    .reason-tags {
      display: flex;
      flex-wrap: wrap;
    }

    .reason-wrapper {
      padding: 16px 0px 16px 16px;

      .badge {
        background-color: #e0e0e0;
        padding: 4px 12px;
        display: flex;
        align-items: center;
        width: max-content;
        cursor: pointer;
        font-size: 12px;
        line-height: 16px;

        &.skip {
          background-color: #ffedd7;
          color: #ff541e;
        }
      }

      .reason {
        border: 1px solid #dadada;
        padding: 12px 16px;
        color: hsl(0, 0%, 20%);
      }
    }
  }
`;

export { Wrapper };
