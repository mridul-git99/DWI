import { Button, FormGroup } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import useParameterResponse from '#hooks/useParameterResponse';
import { useTypedSelector } from '#store';
import { InputTypes } from '#utils/globalTypes';
import { jobActions } from '#views/Job/jobStore';
import { LinkOutlined } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { ParameterProps } from './Parameter';

type TNumberParameterWrapper = {
  isExceptionEnabled: boolean;
};

const NumberParameterWrapper = styled.div.attrs({
  className: 'input-parameter',
})<TNumberParameterWrapper>`
  .exception-container {
    display: flex;
    flex-direction: row;
  }
`;

const NumberParameter: FC<Omit<ParameterProps, 'taskId'>> = ({
  parameter,
  isCorrectingError,
  setCorrectedParameterValues,
  isExceptionEnabled,
}: any) => {
  const dispatch = useDispatch();
  const updating = useTypedSelector((state) => state.job.updating);
  const jobId = useTypedSelector((state) => state.job.id)!;

  const dataById = useParameterResponse(jobId!, [parameter.autoInitialize?.parameterId]);

  const inputRef = useRef(null);
  const [value, setValue] = useState(
    parameter.response.value ? parameter.response.value : undefined,
  );

  const debounceInputRef = useRef(debounce((event, functor) => functor(event), 2000));

  const isDisabled = useMemo(
    () =>
      parameter?.autoInitialized ||
      (isCorrectingError && !setCorrectedParameterValues) ||
      isExceptionEnabled,
    [isCorrectingError, isExceptionEnabled],
  );

  useEffect(() => {
    if (
      !updating &&
      parameter.response.value !== value &&
      inputRef.current &&
      document.activeElement !== inputRef.current
    ) {
      setValue(parameter.response.value ? parameter.response.value : '');
    }
  }, [parameter.response.value]);

  const onChange = useCallback(
    ({ value }: { value: string }) => {
      if (!isCorrectingError) {
        dispatch(
          jobActions.setUpdating({
            updating: true,
          }),
        );
        dispatch(
          jobActions.setExecutingParameterIds({
            id: parameter.id,
            value: true,
          }),
        );
      }
      debounceInputRef.current(value, (value: string) => {
        const _parameter = {
          ...parameter,
          data: {
            ...parameter.data,
            input: value,
          },
        };

        if (isCorrectingError && setCorrectedParameterValues) {
          setCorrectedParameterValues((prev) => ({ ...prev, newValue: value }));
        } else {
          dispatch(
            jobActions.executeParameter({
              parameter: _parameter,
            }),
          );
        }
      });
      setValue(value);
    },
    [isCorrectingError],
  );

  return (
    <NumberParameterWrapper className="input-parameter" isExceptionEnabled={isExceptionEnabled}>
      <div className="new-form-field exception-container">
        <FormGroup
          style={{ padding: 0, width: '100%' }}
          inputs={[
            {
              type: InputTypes.NUMBER,
              props: {
                id: parameter.id,
                value: value,
                ['data-id']: parameter.id,
                ['data-type']: parameter.type,
                disabled: isDisabled,
                ref: inputRef,
                onChange,
                placeholder: 'Ex. 2',
              },
            },
          ]}
        />
      </div>
      {isExceptionEnabled && (
        <Button
          style={{ marginTop: 8 }}
          variant="secondary"
          color="blue"
          onClick={() => {
            dispatch(
              openOverlayAction({
                type: OverlayNames.APPROVALS_LIST_MODAL,
                props: {
                  jobId,
                },
              }),
            );
          }}
        >
          View exception details
        </Button>
      )}
      {parameter.autoInitialize && dataById?.[parameter.autoInitialize?.parameterId] && (
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <LinkOutlined style={{ marginRight: 8 }} /> Linked to ‘
          {dataById[parameter.autoInitialize.parameterId].label}’
        </div>
      )}
    </NumberParameterWrapper>
  );
};

export default NumberParameter;
