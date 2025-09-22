import styled from 'styled-components';

export const CommonWrapper = styled.div`
  padding-block: 16px;
  .list {
    list-style-type: none;
    margin: 16px 0 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    row-gap: 8px;

    &-item {
      display: flex;
      align-items: center;
      column-gap: 16px;

      > svg {
        margin-top: 24px;
      }

      .input-label {
        color: #525252;
        display: flex;
        font-size: 12px;
        letter-spacing: 0.32px;
        line-height: 1.33;
        margin-bottom: 8px;
      }
    }
  }
  .form-row {
    flex: 1;
    flex-direction: row;
    gap: 16px;

    > div {
      flex: 1;
      margin-bottom: 16px !important;
    }
  }
  .validation-text {
    font-size: 14px;
    font-weight: 400;
    margin-bottom: 8px;
  }

  .options-list-title {
    font-size: 12px;
    color: #525252;
    letter-spacing: 0.32px;
    margin-top: 24px;
  }

  .options-action {
    display: flex;
    gap: 16px;
  }

  .remove-options {
    display: flex;
    align-items: center;
    color: #da1e28;
    font-size: 14px;
    cursor: pointer;
    gap: 8px;
    margin-top: 16px;
  }
`;
