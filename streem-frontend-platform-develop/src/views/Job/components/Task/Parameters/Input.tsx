import { Button, FormGroup } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import useParameterResponse from '#hooks/useParameterResponse';
import { useTypedSelector } from '#store';
import { MandatoryParameter, ParameterState } from '#types';
import { InputTypes } from '#utils/globalTypes';
import { jobActions } from '#views/Job/jobStore';
import { LinkOutlined } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { ParameterProps } from './Parameter';

const InputParameter: FC<Omit<ParameterProps, 'taskId'>> = ({
  parameter,
  isCorrectingError,
  setCorrectedParameterValues,
  isTaskCompleted,
  isJobBlocked,
  isTaskBlocked,
  isInboxView,
  isExceptionEnabled,
}) => {
  const dispatch = useDispatch();
  const updating = useTypedSelector((state) => state.job.updating);
  const jobId = useTypedSelector((state) => state.job.id)!;

  const dataById = useParameterResponse(jobId!, [parameter.autoInitialize?.parameterId]);

  const inputRef = useRef(null);
  const [value, setValue] = useState(
    parameter.response.value ? parameter.response.value : undefined,
  );

  const debounceInputRef = useRef(debounce((event, functor) => functor(event), 500));

  const isDisabled = useMemo(
    () =>
      parameter?.autoInitialized ||
      (isCorrectingError && !setCorrectedParameterValues) ||
      isExceptionEnabled,
    [parameter, isCorrectingError, setCorrectedParameterValues, isExceptionEnabled],
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

        if (isCorrectingError) {
          if (setCorrectedParameterValues)
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

  const propsByType = useMemo(() => {
    switch (parameter.type) {
      case MandatoryParameter.SINGLE_LINE:
        return { placeholder: 'Write here' };
      case MandatoryParameter.MULTI_LINE:
        return { placeholder: 'Users will write their comments here', rows: 4 };
      default:
        return undefined;
    }
  }, [parameter.type]);

  const showTextArea = useMemo(
    () =>
      parameter.type === MandatoryParameter.SINGLE_LINE
        ? (isTaskCompleted && !isCorrectingError) ||
          !isInboxView ||
          isJobBlocked ||
          isTaskBlocked ||
          (ParameterState.APPROVAL_PENDING === parameter.response.state && !isCorrectingError)
        : false,
    [
      isTaskCompleted,
      isCorrectingError,
      isInboxView,
      isJobBlocked,
      isTaskBlocked,
      parameter.response.state,
      parameter.type,
    ],
  );

  return (
    <div className="input-parameter">
      <div className="new-form-field">
        <FormGroup
          style={{ padding: 0 }}
          inputs={[
            {
              type: showTextArea
                ? InputTypes.MULTI_LINE
                : (parameter.type as unknown as InputTypes),
              props: {
                id: parameter.id,
                value,
                ['data-id']: parameter.id,
                ['data-type']: parameter.type,
                disabled: isDisabled,
                ref: inputRef,
                onChange,
                ...propsByType,
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
    </div>
  );
};

export default InputParameter;
