import styled from 'styled-components';

export const ValidationWrapper = styled.div`
  .validation {
    display: flex;
    flex-direction: column;
    gap: 10px;
    border-bottom: 1px solid #e0e0e0;
    margin-bottom: 24px;
    padding-bottom: 8px;
    :last-of-type {
      border-bottom: none;
      margin-bottom: 0;
    }

    .validation-header {
      background-color: #f4f4f4;
      padding: 12px;
      > div {
        font-size: 14px;
        font-weight: 700;
      }
    }

    .upper-row {
      display: flex;
      align-items: center;
      gap: 16px;
      .remove-icon {
        cursor: pointer;
        margin-top: 6px;
        font-size: 16px;
      }
    }
  }
  .form-group {
    flex: 1;
    flex-direction: row;
    gap: 16px;

    > div {
      flex: 1;
      margin-bottom: 16px;
    }

    > div:last-child {
      margin-bottom: 16px;
    }
  }

  .custom-select__menu {
    z-index: 2;
  }

  .validation-disabled {
    display: flex;
    flex-direction: row;
    gap: 10px;
    > img {
      margin-bottom: 24px;
    }
  }
`;
