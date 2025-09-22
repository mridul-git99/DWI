import styled from 'styled-components';

export const KeyGenerator = styled.div`
  padding: 24px 16px;

  h3 {
    font-size: 16px;
    line-height: 1.25;
    color: #333333;
    margin: 0px 0px 32px;
  }

  p {
    font-size: 14px;
    line-height: 1.14;
    letter-spacing: 0.16px;
    color: #666666;
    margin: 8px 0px 0px;
  }
`;

export const Credentials = styled.div`
  padding: 24px 16px;

  .row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 32px;
    min-height: 32px;

    :last-child {
      margin-bottom: 0px;
    }

    .form-group {
      padding: 0px 16px;
    }

    .custom-span {
      font-size: 14x;
      line-height: 1.14;
      color: rgba(51, 51, 51, 1);
      min-width: 150px;
      display: flex;
      align-items: flex-start;

      :nth-child(2) {
        flex: 1;
        padding: 0px 20px;
      }

      :last-child {
        justify-content: flex-end;
      }

      .with-icon {
        svg {
          font-size: 16px;
          margin-left: 8px;
        }
      }
    }
  }
`;

export const Composer = styled.form`
  overflow: hidden;
  display: flex;
  flex: 1;

  .action-sidebar {
    display: flex;
    flex: 1;
    max-width: 20dvw;
    background-color: #fff;
    padding: 64px 24px 24px;
    flex-direction: column;
    align-items: center;
    box-shadow: 0 0 1px 0 rgba(0, 0, 0, 0.04), 0 0 2px 0 rgba(0, 0, 0, 0.06),
      -4px 0 8px 0 rgba(0, 0, 0, 0.04);
    margin-left: -4px;
    z-index: 1;

    > svg {
      margin: 32px;
    }

    .registration-info {
      font-size: 14px;
      line-height: 1.14;
      letter-spacing: 0.16px;
      color: #333333;
      text-align: center;

      &.alert {
        display: flex;
        flex-direction: row;
        padding: 4px 16px;
        border-radius: 4px;
        border: solid 1px #ff6b6b;
        background-color: rgba(255, 107, 107, 0.2);
        color: #cc5656;
      }
    }

    .primary-button {
      margin-right: unset;
      width: 100%;
      padding: 16px;
      margin-top: 16px;

      :first-child {
        margin-top: 0px;
      }
    }

    .cancel-button {
      width: 100%;
      padding: 16px;
      margin-top: 16px;
      color: #000;
    }
  }

  .custom-select__multi-value--is-disabled {
    ::before {
      content: '';
      border-radius: 50%;
      border: 2px solid;
      width: 0.5px;
      height: 0.5px;
      top: unset;
      position: absolute;
      left: 2px;
    }
  }
`;

export const CustomInputGroup = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;

  .actions-bar {
    display: flex;
    padding: 32px;
  }
`;
