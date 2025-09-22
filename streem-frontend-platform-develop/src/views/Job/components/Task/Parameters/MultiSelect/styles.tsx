import styled from 'styled-components';

export const Wrapper = styled.div.attrs({
  className: 'material-parameter',
})`
  .parameter-header {
    font-size: 14px;
    line-height: 1.33;
    letter-spacing: 0.32px;
    margin-bottom: 8px;
    background-color: #fff;
  }

  .multi-select {
    margin-bottom: 4px;
  }

  .list {
    &-container {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    &-item {
      align-items: center;
      display: flex;

      .item-content {
        align-items: center;
        /* background-color: #f4f4f4; */
        border: 1px solid transparent;
        border-bottom-color: #bababa;
        display: flex;
        flex: 1;
        padding: 10px 16px;

        :focus-within {
          border-color: #1d84ff;
        }

        .dummy-checkbox {
          border: 1px solid #000000;
          border-radius: 2px;
          height: 16px;
          width: 16px;
          margin-right: 12px;
          cursor: not-allowed;
        }

        input {
          background-color: transparent;
          border: none;
          outline: none;
          flex: 1;
        }
      }

      > .icon {
        margin-left: 24px;
      }
    }
  }
`;
