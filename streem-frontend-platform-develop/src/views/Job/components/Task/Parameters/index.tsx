import { useTypedSelector } from '#store';
import { useJobStateToFlags } from '#views/Job/utils';
import React, { FC } from 'react';
import styled, { css } from 'styled-components';
import Parameter from './Parameter';

export type ParameterListProps = {
  isTaskStarted?: boolean;
  isTaskCompleted?: boolean;
  isTaskPaused?: boolean;
  isCorrectingError: boolean;
};

const Wrapper = styled.div.attrs({
  className: 'parameter-list',
})<{
  isInboxView: boolean;
  isReadOnly?: boolean;
  isTaskCompleted?: boolean;
  isCorrectingError: boolean;
  isJobBlocked?: boolean;
  isTaskBlocked?: boolean;
}>`
  display: flex;
  flex-direction: column;

  ${({ isReadOnly }) =>
    isReadOnly
      ? css`
          pointer-events: none;
        `
      : null}

  .parameter {
    padding: 14px 16px;
    background-color: #f4f4f4;

    :last-child {
      border-bottom: none;
    }

    .checklist-parameter,
    .material-parameter,
    .should-be-parameter > .parameter-content,
    .yes-no-parameter,
    .input-parameter,
    .calculation-parameter,
    .upload-image,
    .date-time-parameter,
    .signature-interaction {
      ${({ isTaskCompleted, isCorrectingError, isInboxView, isJobBlocked, isTaskBlocked }) =>
        (isTaskCompleted && !isCorrectingError) || !isInboxView || isJobBlocked || isTaskBlocked
          ? css`
              pointer-events: none;
            `
          : null}
    }

    .optional-badge {
      font-size: 14px;
      line-height: 1.43;
      letter-spacing: 0.16px;
      margin-bottom: 16px;
      color: #999999;
    }

    .error-badge {
      align-items: center;
      color: #ff6b6b;
      display: flex;
      font-size: 12px;
      margin-bottom: 16px;
      padding: 4px;

      > .icon {
        color: #ff6b6b;
        margin-right: 8px;
      }
    }

    .parameter-audit {
      color: #999999;
      font-size: 12px;
      line-height: 0.83;
      margin-top: 8px;
      display: flex;
      align-items: center;
      gap: 8px;

      span {
        color: #1d84ff;
        cursor: pointer;
      }
    }

    .parameter-verified {
      display: flex;
      flex-direction: row;
      gap: 4px;
      align-items: center;
    }

    .calculation-parameter {
      display: flex;
      flex-direction: column;
      background-color: #fff;
      padding: 10px 12px;
      border: 1px solid #e0e0e0;

      .head {
        opacity: 0.6;
        margin-bottom: 10px;
      }

      .expression {
        margin-bottom: 15px;
        margin-left: 12px;
      }

      .variable {
        display: flex;
        gap: 8px;
        margin-left: 12px;
        .name {
          font-weight: bold;
        }
      }

      .result {
        padding: 8px;
        background-color: rgba(29, 132, 255, 0.1);
      }
    }

    .parameter-label {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      color: #161616;
      font-weight: 600;
      font-size: 14px;
    }

    .parameter-variation {
      color: #1d84ff;
      font-size: 14px;
      display: flex;
      align-items: center;
      cursor: pointer;
    }

    .parameter-verification {
      padding-top: 14px;
      display: flex;
      align-items: center;
      gap: 10px;

      button {
        margin-right: 0px;
      }
    }
  }
`;

const ParameterList: FC<ParameterListProps> = ({
  isTaskStarted,
  isTaskCompleted,
  isTaskPaused,
  isCorrectingError,
}) => {
  const {
    isInboxView,
    activeTask: task,
    errors: { parametersErrors },
  } = useTypedSelector((state) => state.job);

  const { isBlocked, isTaskBlocked } = useJobStateToFlags();

  const renderParameters = () => {
    const parameters: JSX.Element[] = [];
    task.parameters.forEach((parameter: any) => {
      !parameter.response.hidden &&
        parameters.push(
          <Parameter
            key={parameter.response.id}
            parameter={parameter}
            isTaskCompleted={isTaskCompleted}
            isLoggedInUserAssigned={task.isTaskAssigned}
            isCorrectingError={isCorrectingError}
            errors={parametersErrors?.get(parameter.response.id)}
            isJobBlocked={isBlocked}
            isTaskBlocked={isTaskBlocked}
          />,
        );
    });
    return parameters;
  };

  return (
    <Wrapper
      isReadOnly={!isTaskStarted || isTaskPaused}
      isJobBlocked={isBlocked}
      isTaskCompleted={isTaskCompleted}
      isCorrectingError={isCorrectingError}
      isInboxView={isInboxView}
      isTaskBlocked={isTaskBlocked}
    >
      {renderParameters()}
    </Wrapper>
  );
};

export default ParameterList;
