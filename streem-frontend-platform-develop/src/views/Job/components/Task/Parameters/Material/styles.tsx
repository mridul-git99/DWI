import styled from 'styled-components';

export const Wrapper = styled.div`
  .list {
    &-container {
      margin: 0;
      padding: 0;
      counter-reset: item;
      list-style-type: none;
    }

    &-item {
      list-style-position: inside;
      margin-bottom: 8px;
      display: flex;
      align-items: center;

      :last-of-type {
        margin-bottom: 0;
      }

      &::before {
        color: #000000;
        content: counter(item) ' ';
        counter-increment: item;
        font-size: 14px;
        margin-right: 12px;
      }

      &-image {
        align-items: center;
        background-color: #f4f4f4;
        cursor: pointer;
        display: flex;
        height: 56px;
        justify-content: center;
        margin-right: 12px;
        padding: 8px;
        width: 56px;
      }

      > input[type='text'] {
        flex: 1;
      }

      &-quantity {
        align-items: center;
        display: flex;
        margin-left: 12px;

        > .icon.disabled {
          cursor: not-allowed;
        }

        .quantity {
          line-height: 1.15;
          padding: 13px 4px;
        }
      }

      .name {
        flex: 1;
      }

      > .icon {
        margin-left: 12px;
      }
    }
  }
`;
