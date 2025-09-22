import styled from 'styled-components';
export const Composer = styled.div<{ isTrainedUserView?: boolean }>`
  margin: ${({ isTrainedUserView }) => (isTrainedUserView ? 'unset' : '8px')};
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex: 1;

  .audit-logs-wrapper {
    grid-area: activity;
  }

  .parameter-wrapper,
  .audit-logs-wrapper {
    background: #fff;
    box-shadow: 0 5px 8px 0 rgba(0, 0, 0, 0.1);
    padding: ${({ isTrainedUserView }) => (isTrainedUserView ? 'unset' : '8px')};

    overflow: hidden;
    display: flex;
    flex: 1;

    .list-card-columns:first-child {
      width: unset;
      flex: 1;
    }

    .icon {
      font-size: 16px;
      color: #ff6b6b;
    }

    .log-header {
      display: flex;
      align-items: center;

      .header-item {
        font-size: 14px;
        color: #666666;
        margin-right: 16px;
        align-items: center;
        display: flex;
      }
    }

    .log-row {
      display: flex;
      flex-direction: column;
      padding: 8px 11px;
      margin-top: 16px;
      border-left: 1px dashed #bababa;

      .log-item {
        display: flex;
        align-items: center;
        padding: 8px 0px;
      }

      .circle {
        margin-left: -16px;
        background-color: #bababa;
        border-radius: 4px;
        height: 8px;
        width: 8px;
      }

      .content {
        margin-left: 16px;
        display: flex;
        align-items: center;

        .content-items {
          font-size: 12px;
          color: #666666;
          margin-right: 8px;
          word-break: keep-all;
        }
      }
    }

    .list-header-columns {
      :first-child {
        padding-left: 30px;
      }
    }

    .user-actions {
      font-size: 14px;
      color: #666666;
      font-weight: 600;
      letter-spacing: 1px;
      cursor: pointer;
    }
  }
`;
