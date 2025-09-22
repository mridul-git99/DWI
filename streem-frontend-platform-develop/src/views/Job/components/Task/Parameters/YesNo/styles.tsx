import styled from 'styled-components';

export const Wrapper = styled.div.attrs({
  className: 'yes-no-parameter',
})`
  display: flex;
  flex-direction: column;

  .buttons-container {
    display: flex;

    .button-item {
      display: flex;
      background-color: #ffffff;
      button {
        background-color: transparent;
        cursor: pointer;
        font-size: 14px;
        letter-spacing: 0.16px;
        line-height: 1.29;
        padding: 8px 20px;
        flex: 1;
      }

      :first-child {
        input {
          background-color: #e1fec0;
          border-bottom-color: #27ae60;

          :active,
          :focus {
            border-color: #27ae60;
          }
        }

        button {
          border: 1px solid #5aa700;
          color: #5aa700;
          outline: none;

          &.filled {
            color: #ffffff;
            background-color: #427a00;
          }

          &.disabled {
            cursor: not-allowed;
          }
        }
      }

      :last-child {
        margin-left: 20px;

        input {
          background-color: #ffebeb;
          border-bottom-color: #eb5757;

          :active,
          :focus {
            border-color: #eb5757;
          }
        }

        button {
          border: 1px solid #ff6b6b;
          color: #ff6b6b;

          &.filled {
            color: #ffffff;
            background-color: #cc5656;
          }

          &.disabled {
            cursor: not-allowed;
          }
        }
      }
    }
  }

  .decline-reason {
    margin-top: 16px;
    .textarea {
      .textarea-wrapper {
        margin-bottom: 10px;
      }
    }
  }
`;
