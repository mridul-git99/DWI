import styled, { css } from 'styled-components';
import { StageCardProps } from './types';

const StageListWrapper = styled.div.attrs({
  className: 'stage-list-container',
})`
  grid-area: stage-list;
  overflow: auto;
  padding: 0px 8px 0px 16px;
`;

const StageCardWrapper = styled.div.attrs({
  className: 'stage',
})<Pick<StageCardProps, 'isActive'> & { hasError: boolean }>`
  background-color: #ffffff;
  border: solid 1px #eeeeee;
  box-shadow: 1px 2px 8px 0 rgba(29, 132, 255, 0.2);
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  position: relative;

  .stage {
    &-header {
      align-items: center;
      background-color: #fff;
      border-bottom: 1px solid #f4f4f4;
      border-top-left-radius: 4px;
      border-top-right-radius: 4px;
      display: flex;

      .order-control {
        background-color: #f4f4f4;
        border-top-left-radius: 4px;
        display: flex;
        flex-direction: column;
        padding: 4px;
      }

      .stage-actions {
        margin-left: auto;
      }

      .stage-icon {
        margin-right: 16px;

        @media (max-width: 1200px) {
          margin-right: 8px;
        }
      }

      svg {
        color: #161616;
      }
    }

    &-name {
      color: #000000;
      font-size: 20px;
      font-weight: bold;
      margin-left: 16px;
      padding-block: 12px;

      @media (max-width: 1200px) {
        font-size: 16px;
      }
    }

    &-body {
      padding: 16px;

      @media (max-width: 1200px) {
        padding: 8px;
      }
    }

    &-task-count {
      color: #666666;
      display: none;
      font-size: 14px;
      margin-top: 8px;
    }

    &-task-properties {
      align-items: center;
      display: flex;
      justify-content: flex-end;
      margin-bottom: 8px;

      > .icon {
        color: #f7b500;
        margin-left: 12px;

        :only-child,
        :first-child {
          margin-left: 0;
        }
      }

      .stage-badge {
        align-items: center;
        background-color: #ffebeb;
        border-radius: 4px;
        display: flex;
        margin-left: 12px;
        padding: 4px;
        width: max-content;

        > .icon {
          margin-right: 4px;
          color: #ff6b6b;
        }

        span {
          font-size: 12px;
          line-height: 0.83;
          color: #ff6b6b;
        }
      }
    }

    &-error-wrapper {
      align-items: center;
      color: #eb5757;
      display: flex;
      font-size: 12px;
      justify-content: flex-start;
      margin-top: 8px;
      margin-bottom: 8px;

      .stage-error-icon {
        font-size: 16px;
        color: #eb5757;
        margin-right: 5px;
      }
    }

    &-footer {
      align-items: center;
      background-color: #f4f4f4;
      border-bottom-left-radius: 4px;
      border-bottom-right-radius: 4px;
      color: #999999;
      cursor: pointer;
      display: flex;
      font-size: 14px;
      padding: 8px 16px;

      > .icon {
        margin-right: 8px;
      }
    }
  }

  ${({ isActive }) =>
    isActive
      ? css`
          border-color: #1d84ff;
        `
      : null}

  ${({ hasError }) =>
    hasError
      ? css`
          border-color: #eb5757;
        `
      : null}
`;

export { StageListWrapper, StageCardWrapper };
