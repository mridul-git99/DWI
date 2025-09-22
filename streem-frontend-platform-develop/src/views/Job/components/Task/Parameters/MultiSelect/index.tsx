import { Select } from '#components';
import { get, isArray } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { MandatoryParameter, Selections } from '#types';
import { jobActions } from '#views/Job/jobStore';
import { ParameterProps } from '../Parameter';
import { Wrapper } from './styles';
import { useJobStateToFlags } from '#views/Job/utils';
import { LinkOutlined } from '@material-ui/icons';
import { useTypedSelector } from '#store';
import useParameterResponse from '#hooks/useParameterResponse';

const MultiSelectParameter: FC<ParameterProps & { isMulti: boolean }> = ({
  parameter,
  isCorrectingError,
  isMulti,
  setCorrectedParameterValues,
}) => {
  const dispatch = useDispatch();
  const jobId = useTypedSelector((state) => state.job.id)!;
  const { isTaskPaused, isTaskCompleted } = useJobStateToFlags();
  const dataById = useParameterResponse(jobId!, [parameter.autoInitialize?.parameterId]);
  const [state, setState] = useState<{
    value: any;
  }>({
    value: null,
  });

  const { value } = state;

  useEffect(() => {
    const selectedValue = options.filter(
      (el) => get(parameter?.response?.choices, el.value) === Selections.SELECTED,
    );
    setState((prev) => ({
      ...prev,
      value: selectedValue,
    }));
  }, [parameter?.response?.audit?.modifiedAt]);

  const updateDataAndChoice = useMemo(() => {
    return (parameter, options) => {
      const newChoice = parameter.data.reduce((acc, el) => {
        const state =
          options.findIndex((e) => e.value === el.id) > -1
            ? Selections.SELECTED
            : Selections.NOT_SELECTED;
        acc[el.id] = state;
        return acc;
      }, {});

      const newData = {
        ...parameter,
        data: parameter.data.map((el) => ({
          ...el,
          state: newChoice[el.id] || Selections.NOT_SELECTED,
        })),
      };

      return { newData, newChoice };
    };
  }, []);

  const onSelectOption = (_options) => {
    const options = _options ? (Array.isArray(_options) ? _options : [_options]) : [];
    const { newData, newChoice } = updateDataAndChoice(parameter, options);

    setState((prev) => ({
      ...prev,
      value: options?.length ? options : null,
    }));

    if (isCorrectingError && setCorrectedParameterValues) {
      setCorrectedParameterValues((prev) => ({
        ...prev,
        newChoice: newChoice,
      }));
    } else {
      dispatch(jobActions.executeParameter({ parameter: newData }));
    }
  };

  const onRemove = (index) => {
    const filteredValue = value.filter((_, i) => i !== index);
    setState({
      ...state,
      value: filteredValue,
    });
    onSelectOption(filteredValue);
  };

  const options = parameter.data.map((el) => ({ label: el.name, value: el.id }));

  const isDisabled =
    parameter?.autoInitialized ||
    isTaskPaused ||
    (isTaskCompleted && isCorrectingError && !setCorrectedParameterValues);

  return (
    <>
      <Wrapper>
        <Select
          isMulti={isMulti}
          className="multi-select"
          data-id={parameter.id}
          data-type={parameter.type}
          options={options}
          value={value}
          placeholder={isMulti ? 'Select one or more options' : 'You can select one option here'}
          isClearable={true}
          onChange={(options) => {
            const castedOptions = options ? (isArray(options) ? options : [options]) : [];
            onSelectOption(castedOptions);
          }}
          isDisabled={isDisabled}
          onRemove={onRemove}
          countAsValues={parameter.type === MandatoryParameter.MULTISELECT}
        />
      </Wrapper>
      {parameter.autoInitialize && dataById?.[parameter.autoInitialize?.parameterId] && (
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <LinkOutlined style={{ marginRight: 8 }} /> Linked to ‘
          {dataById[parameter.autoInitialize.parameterId].label}’
        </div>
      )}
    </>
  );
};

export default MultiSelectParameter;
