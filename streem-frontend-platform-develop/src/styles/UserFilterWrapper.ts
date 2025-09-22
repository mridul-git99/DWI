import styled from 'styled-components';

export const UserFilterWrapper = styled.div`
  padding: 16px 16px 0px;
  min-height: 380px;

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
      padding: 0px 13px;
      background-color: #fff;
      border-bottom: 1px solid #bababa;

      svg {
        background: #fff;
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
        background: #fff;
        height: 40px;
        width: calc(100% - 28px);
        margin-left: 28px;

        ::-webkit-input-placeholder {
          color: #bababa;
        }
      }
    }
  }

  .top-header {
    display: flex;
    flex-direction: column;
  }

  .filter-tabs-panel {
    padding: unset;
  }

  .scrollable-content {
    height: 320px;
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
        border: 1px solid #fff;
        width: 36px;
        height: 36px;
        color: #1d84ff;
        border-radius: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: #ecedf1;
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
          font-size: 18px;
          color: #666666;
          text-transform: capitalize;
          word-break: break-word;
        }
      }

      .right {
        margin-top: -15px;
      }
    }
  }

  .scrollable-content-trained-users {
    height: 400px;
    overflow: hidden;
  }
`;
