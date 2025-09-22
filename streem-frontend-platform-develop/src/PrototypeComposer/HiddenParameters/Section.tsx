import { Checkbox } from '#components';
import { Stage, Task } from '#types';
import { ArrowDropDown, ArrowRight } from '@material-ui/icons';
import React, { FC, useState } from 'react';
import styled from 'styled-components';
import { HiddenParametersStateType } from '.';

type Props = {
  stage: Stage;
  isFirst: boolean;
  isReadOnly: boolean;
  parameterChanges: HiddenParametersStateType;
  updateParameterChanges: (ids: string[], value: boolean) => void;
};

export const AssignmentSectionWrapper = styled.div.attrs({
  className: 'section',
})`
  display: flex;
  flex-direction: column;
  margin-bottom: 1px;

  .checkbox-input {
    label.container {
      color: #333333;
      font-weight: bold;
    }
  }

  .pill {
    border: 1px solid transparent;
    border-radius: 4px;
    font-size: 12px;
    margin-left: 16px;
    padding: 2px 4px;

    &.partial {
      background-color: rgba(247, 181, 0, 0.2);
      border-color: #f7b500;
      color: #dba613;
    }

    &.assigned,
    &.completed {
      background-color: #e1fec0;
      border-color: #5aa700;
      color: #5aa700;
    }

    &.unassigned {
      background-color: #ffebeb;
      border-color: #cc5656;
      color: #cc5656;
    }
  }

  .section {
    &-header {
      align-items: center;
      background-color: #eeeeee;
      display: flex;
      justify-content: space-between;
      padding: 10px 16px;

      .checkbox-input {
        margin-right: auto;
      }

      .icon-wrapper {
        align-items: center;
        background-color: #dadada;
        border-radius: 50%;
        display: flex;
        margin-right: 8px;
        padding: 2px;
      }

      .icon {
        color: #1d84ff;
        font-size: 20px;
        margin-left: 16px;
      }

      .toggle-section {
        color: #333333;
        font-size: 20px;
        margin-left: 0;
      }
    }

    &-task {
      display: flex;
      flex-direction: column;
      margin-left: 20px;

      &-item {
        background-color: #ffffff;
        border-bottom: 1px solid #eeeeee;
        display: flex;
        padding: 10px 16px;
        flex-direction: column;

        &-header {
          display: flex;
          margin-bottom: 10px;
        }

        &-param {
          margin-left: 10px;

          &-item {
            padding: 10px 16px;
          }
        }

        > span {
          margin-left: 32px;
          margin-right: auto;
        }

        &.disabled {
          opacity: 0.5;
          pointer-events: none;
        }

        :last-child {
          border-bottom: none;
        }

        .checkbox-input {
          margin-left: 32px;
          margin-right: auto;

          label.container {
            font-weight: normal;
          }
        }

        .icon {
          color: #1d84ff;
          font-size: 20px;
          margin-left: 16px;
        }
      }
    }

    &-parameter {
      margin-left: 20px;
    }
  }

  .section-process-items {
    padding: 10px 42px;
    .checkbox-input > label {
      font-size: 16px;
      font-weight: normal;
    }
  }
`;

const Section: FC<Props> = ({
  stage,
  isFirst,
  parameterChanges,
  isReadOnly,
  updateParameterChanges,
}) => {
  const [isOpen, toggleIsOpen] = useState(isFirst);
  let isWholeStageSelected = stage.tasks.length > 0;
  const parameterIdsInStage: string[] = [];

  const renderTask = (task: Task) => {
    let isWholeTaskSelected = task.parameters.length > 0;
    const parameterIdsInTask: string[] = [];
    const parameterRows = task.parameters.map((param) => {
      parameterIdsInTask.push(param.id);
      parameterIdsInStage.push(param.id);
      if (!parameterChanges[param.id]) {
        isWholeTaskSelected = false;
      }
      return (
        <div className="section-task-item-param-item" key={param.id}>
          <Checkbox
            checked={parameterChanges[param.id]}
            label={`Parameter ${task.orderTree}.${param.orderTree} : ${param.label}`}
            disabled={isReadOnly}
            onClick={(checked) => updateParameterChanges([param.id], checked)}
          />
        </div>
      );
    });

    if (!isWholeTaskSelected) {
      isWholeStageSelected = false;
    }

    return (
      <div className="section-task-item" key={task.id}>
        <div className="section-task-item-header">
          <Checkbox
            checked={isWholeTaskSelected}
            label={`Task ${stage.orderTree}.${task.orderTree} : ${task.name}`}
            disabled={isReadOnly}
            onClick={(checked) => updateParameterChanges(parameterIdsInTask, checked)}
          />
        </div>

        <div className="section-task-item-param">{parameterRows}</div>
      </div>
    );
  };

  const taskRows = stage.tasks.map((task) => renderTask(task));

  return (
    <AssignmentSectionWrapper>
      <div className="section-header">
        <div className="icon-wrapper" onClick={() => toggleIsOpen((val) => !val)}>
          {isOpen ? (
            <ArrowDropDown className="icon toggle-section" />
          ) : (
            <ArrowRight className="icon toggle-section" />
          )}
        </div>

        <Checkbox
          checked={isWholeStageSelected}
          label={
            <div>
              <span style={{ fontWeight: 'bold' }}>Stage {stage.orderTree}</span> {stage.name}
            </div>
          }
          disabled={isReadOnly}
          onClick={(checked) => updateParameterChanges(parameterIdsInStage, checked)}
        />
      </div>
      {isOpen && <div className="section-task">{taskRows}</div>}
    </AssignmentSectionWrapper>
  );
};

export default Section;
