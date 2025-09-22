import { Button } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import CustomDateTimePicker from '#components/shared/CustomDateTimePicker';
import useParameterResponse from '#hooks/useParameterResponse';
import { useTypedSelector } from '#store';
import { jobActions } from '#views/Job/jobStore';
import { LinkOutlined } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useCallback, useMemo, useRef } from 'react';
import { useDispatch } from 'react-redux';

const DateTimeInput: FC<any> = ({
  parameter,
  isCorrectingError,
  setCorrectedParameterValues,
  isExceptionEnabled,
  isCorrectionInitiated,
  isCorrectionCorrected,
}) => {
  const dispatch = useDispatch();
  const jobId = useTypedSelector((state) => state.job.id);

  const dataById = useParameterResponse(jobId!, [parameter.autoInitialize?.parameterId]);

  const debounceInputRef = useRef(debounce((event, functor) => functor(event), 500));

  const isDisabled = useMemo(
    () =>
      parameter?.autoInitialized ||
      (isCorrectingError && !setCorrectedParameterValues) ||
      isExceptionEnabled,
    [parameter, isCorrectingError, setCorrectedParameterValues, isExceptionEnabled],
  );

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
    },
    [isCorrectingError],
  );

  return (
    <>
      <CustomDateTimePicker
        parameter={parameter}
        onChange={onChange}
        parameterValue={parameter.response.value}
        isDisabled={isDisabled}
        isCorrectingError={isCorrectingError}
        setCorrectedParameterValues={setCorrectedParameterValues}
        isExceptionEnabled={isExceptionEnabled}
        isCorrectionInitiated={isCorrectionInitiated}
        isCorrectionCorrected={isCorrectionCorrected}
      />
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
    </>
  );
};

export default DateTimeInput;
