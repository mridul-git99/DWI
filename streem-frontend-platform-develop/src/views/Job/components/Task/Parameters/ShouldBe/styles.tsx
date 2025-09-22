import styled from 'styled-components';

export const Wrapper = styled.div.attrs({
  className: 'should-be-parameter',
})`
  display: flex;
  flex-direction: column;

  > .new-form-field {
    margin-bottom: 16px;

    :last-child {
      margin-bottom: 0;
    }
  }

  .is-between-values {
    display: flex;
    align-items: center;

    > .new-form-field {
      flex: 1;
    }
  }

  .parameter-text {
    margin-bottom: 8px;
    color: #161616;
    font-weight: 600;
    font-size: 14px;
    line-height: 12px;
    display: inline-block;
  }

  .off-limit-reason {
    display: flex;
    flex-direction: column;
    margin-top: 16px;

    .warning {
      color: #ff6b6b;
      font-size: 12px;
      margin-bottom: 32px;
    }

    .buttons-container {
      display: flex;
      margin-top: 16px;
    }
  }

  .pending-approval {
    align-items: center;
    color: #000000;
    display: flex;
    font-size: 12px;
    margin-bottom: 16px;

    > .icon {
      color: #f7b500;
      margin-right: 5px;
    }
  }

  .approved {
    align-items: center;
    color: #5aa700;
    display: flex;
    font-size: 12px;
    margin-bottom: 16px;

    > .icon {
      color: #5aa700;
      margin-right: 5px;
    }
  }

  .rejected {
    align-items: center;
    color: #ff6b6b;
    display: flex;
    font-size: 12px;
    margin-bottom: 16px;

    > .icon {
      color: #ff6b6b;
      margin-right: 5px;
    }
  }
`;
