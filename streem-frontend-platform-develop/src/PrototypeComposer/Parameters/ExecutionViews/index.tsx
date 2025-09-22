import { ParameterProps } from '#PrototypeComposer/Activity/types';
import ResourceExecutionView from '#PrototypeComposer/Parameters/ExecutionViews/Resource';
import SingleLineExecutionView from '#PrototypeComposer/Parameters/ExecutionViews/SingleLine';
import SingleSelectExecutionView from '#PrototypeComposer/Parameters/ExecutionViews/SingleSelect';
import YesNoTaskView from '#PrototypeComposer/Parameters/ExecutionViews/YesNo';
import { MandatoryParameter } from '#types';
import React, { FC, useEffect } from 'react';
import styled from 'styled-components';
import DateTimeExecutionView from './DateTime';

export const ParameterViewWrapper = styled.div`
  width: 100%;

  :last-child {
    border-bottom: none;
  }

  :hover {
    .container {
      touch-action: none;
    }
  }

  .container {
    display: flex;
    position: relative;
    border: 1px solid #fff;

    flex-direction: column;

    .content {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 1;

      .filters-validations {
        margin-top: 8px;
        display: flex;
        align-items: center;
        color: #1d84ff;
        font-size: 14px;
        line-height: 16px;
        cursor: pointer;

        svg {
          font-size: 16px;
          margin-right: 8px;
        }
      }

      .parameter-label {
        font-size: 14px;
        line-height: 1.33;
        letter-spacing: 0.32px;
        color: #525252;
        &-optional {
          color: #999999;
          font-size: 12px;
          margin-left: 4px;
        }
      }

      .form-group {
        padding: 0;
        margin-bottom: 0px;
        > div {
          margin-bottom: 0px;
        }
      }
    }
  }
`;

const ParameterView: FC<ParameterProps> = ({
  parameter,
  form,
  onChangeHandler,
  parameterValues,
  initialHiddenParameterIds,
}) => {
  const { register, unregister, setValue, watch } = form;

  const isEditing = watch('isEditing');

  useEffect(() => {
    register(parameter.id, {
      required: parameter.mandatory,
    });
    setValue(parameter.id, parameter, {
      shouldValidate: initialHiddenParameterIds?.[parameter.id] ? undefined : isEditing,
    });
    return () => {
      unregister(parameter.id);
    };
  }, []);

  const renderTaskViewByType = () => {
    switch (parameter.type) {
      case MandatoryParameter.MULTISELECT:
      case MandatoryParameter.SINGLE_SELECT:
        return (
          <SingleSelectExecutionView
            parameter={parameter}
            form={form}
            onChangeHandler={onChangeHandler}
            parameterValues={parameterValues}
          />
        );

      case MandatoryParameter.DATE:
      case MandatoryParameter.DATE_TIME:
        return (
          <DateTimeExecutionView
            parameter={parameter}
            form={form}
            parameterValues={parameterValues}
          />
        );

      case MandatoryParameter.NUMBER:
      case MandatoryParameter.MULTI_LINE:
      case MandatoryParameter.SINGLE_LINE:
        return (
          <SingleLineExecutionView
            parameter={parameter}
            form={form}
            parameterValues={parameterValues}
            onChangeHandler={onChangeHandler}
          />
        );

      case MandatoryParameter.YES_NO:
        return <YesNoTaskView parameter={parameter} form={form} />;

      case MandatoryParameter.RESOURCE:
      case MandatoryParameter.MULTI_RESOURCE:
        return (
          <ResourceExecutionView
            parameter={parameter}
            form={form}
            onChangeHandler={onChangeHandler}
            parameterValues={parameterValues}
          />
        );

      default:
        return null;
    }
  };

  return (
    <ParameterViewWrapper>
      <div className="container">
        <div className="content">
          <div className="parameter-label">
            {parameter.label}
            {!parameter.mandatory && <span className="parameter-label-optional">optional</span>}
          </div>
          {renderTaskViewByType()}
        </div>
      </div>
    </ParameterViewWrapper>
  );
};

export default ParameterView;
