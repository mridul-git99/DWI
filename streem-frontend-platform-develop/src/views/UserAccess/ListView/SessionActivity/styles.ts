import styled from 'styled-components';
export const Composer = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;

  .list-card-columns:first-child {
    width: unset;
    flex: 1;
  }

  .list-state {
    font-size: 12px;
    padding-top: 4px;
    line-height: 0.83;
    display: flex;
    align-items: center;
    color: #f7b500;

    .list-state-span {
      font-size: 12px;
      display: flex;
      align-items: center;
      text-transform: capitalize;

      .icon {
        font-size: 12px;
        margin: 0px 4px 0px 0px;
        color: inherit;
      }
    }
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
`;
