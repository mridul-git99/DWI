import QRIcon from '#assets/svg/QR';
import { Button, Select } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { createFetchList } from '#hooks/useFetchData';
import useParameterResponse from '#hooks/useParameterResponse';
import { useTypedSelector } from '#store';
import { MandatoryParameter, StoreParameter, TaskExecutionType } from '#types';
import { apiGetMasterParameterInfo, apiGetResourceOptions } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { request } from '#utils/request';
import { jobActions } from '#views/Job/jobStore';
import { useJobStateToFlags } from '#views/Job/utils';
import { getQrCodeData, qrCodeValidator } from '#views/Ontology/utils';
import { LinkOutlined } from '@material-ui/icons';
import { debounce, isArray } from 'lodash';
import React, { FC, useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { Wrapper } from './MultiSelect/styles';
import { ParameterProps } from './Parameter';

const ResourceParameterWrapper = styled.div`
  display: flex;
  gap: 12px;
  .react-custom-select {
    flex: 1;
  }
  .qr-selector {
    cursor: pointer;
    border: 1px solid #1d84ff;
    height: 42px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 8px;
  }
`;

const ResourceParameter: FC<ParameterProps> = ({
  parameter,
  isCorrectingError,
  setCorrectedParameterValues,
  source,
  isExceptionEnabled,
}: {
  parameter: StoreParameter;
  isCorrectingError: boolean;
  setCorrectedParameterValues?: any;
  source?: string;
  isExceptionEnabled?: boolean;
}) => {
  const dispatch = useDispatch();
  const jobId = useTypedSelector((state) => state.job.id)!;
  const taskExecutions = useTypedSelector((state) => state.job.taskExecutions)!;

  const urlParams = useMemo(() => {
    return {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
    };
  }, []);

  // Build URL manually with multi-level sorting to avoid axios array serialization issues
  const baseUrlWithSort = useMemo(() => {
    const baseUrl = apiGetResourceOptions(parameter.response.id);
    return `${baseUrl}?sort=createdAt,desc&sort=id,desc`;
  }, [parameter.response.id]);

  const { list, reset, fetchNext, status } = createFetchList(baseUrlWithSort, urlParams, false);

  const { isTaskPaused, isTaskCompleted } = useJobStateToFlags();

  const dataById = useParameterResponse(jobId!, [parameter.autoInitialize?.parameterId]);

  const [choicesFromMaster, setChoicesFromMaster] = useState<any[]>([]);
  const [state, setState] = useState<{
    value: any;
    query: string;
  }>({
    value: null,
    query: '',
  });

  const { value, query } = state;

  const onRemove = (index: number) => {
    const filteredValue = value.filter((_, i) => i !== index);
    setState({
      ...state,
      value: filteredValue,
    });
    onSelectOption(filteredValue);
  };

  const onRemoveAll = () => {
    setState({
      ...state,
      value: [],
    });
    onSelectOption([]);
  };

  // Allow user to select the resource only from the list of resources selected in the MASTER task
  const taskExecution = taskExecutions.get(parameter.response.taskExecutionId);
  const isTaskRepeatOrRecurring =
    taskExecution?.type === TaskExecutionType.REPEAT ||
    taskExecution?.type === TaskExecutionType.RECURRING;

  const options = useMemo(() => {
    if (isTaskRepeatOrRecurring) {
      return choicesFromMaster;
    } else {
      return list;
    }
  }, [list, isTaskRepeatOrRecurring, choicesFromMaster]);

  const setOptionsFromMasterParameter = async () => {
    const { data } = await request('PATCH', apiGetMasterParameterInfo(jobId), {
      data: {
        parameterIds: [parameter.id],
      },
    });

    if (data) {
      const { choices } = data[0];
      setChoicesFromMaster(choices || []);
    }
  };

  useEffect(() => {
    if (isTaskRepeatOrRecurring) {
      setOptionsFromMasterParameter();
    }
  }, []);

  useEffect(() => {
    setState((prev) => ({
      ...prev,
      value: parameter.response.choices?.length
        ? parameter.response.choices.map((choice: any) => ({
            value: choice.objectId,
            label: choice?.objectDisplayName,
            externalId: <div>&nbsp;(ID: {choice?.objectExternalId})</div>,
            option: {
              id: choice.objectId,
              displayName: choice?.objectDisplayName,
              externalId: choice?.objectExternalId,
              collection: choice?.collection,
            },
          }))
        : null,
    }));
  }, [parameter?.response?.audit?.modifiedAt]);

  //  A flag to check if the parameter has variation  from backend
  const onSelectWithQR = async (data: string) => {
    try {
      const qrData = await getQrCodeData({
        shortCode: data,
      });
      const isMultiResource = parameter?.type === MandatoryParameter.MULTI_RESOURCE;
      const existingChoice = parameter?.response?.choices?.find(
        (currChoice) => currChoice.objectId === qrData.objectId,
      );

      const newChoice = isMultiResource
        ? existingChoice
          ? null
          : {
              option: { ...qrData, id: qrData?.objectId },
              value: qrData?.objectId,
              label: qrData?.displayName,
              externalId: qrData?.externalId,
            }
        : {
            option: { ...qrData, id: qrData?.objectId },
            value: qrData?.objectId,
            label: qrData?.displayName,
            externalId: qrData?.externalId,
          };

      const choicesArray = isMultiResource
        ? (parameter?.response?.choices || []).map((currParam) => ({
            option: {
              id: currParam.objectId,
              displayName: currParam?.objectDisplayName,
              externalId: currParam?.objectExternalId,
              collection: currParam?.collection,
            },
            value: currParam.objectId,
            label: currParam?.objectDisplayName,
            externalId: currParam?.objectExternalId,
          }))
        : [];

      const result = newChoice ? [newChoice, ...choicesArray] : choicesArray;

      if (qrData?.objectId) {
        await qrCodeValidator({
          data: qrData,
          callBack: () => onSelectOption(result),
          objectTypeValidation: qrData?.objectTypeId === parameter?.data?.objectTypeId,
          parameterResponseId: parameter.response.id,
          shortCode: data,
        });
      }
    } catch (error) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
        }),
      );
    }
  };

  const onSelectOption = (options: any, isAllSelected: boolean = false) => {
    const newChoices = options?.map((o: any) => ({
      objectId: o.option?.id || o.option?.objectId,
      objectDisplayName: o.option?.displayName || o.option?.objectDisplayName,
      objectExternalId: o.option?.externalId || o.option?.objectExternalId,
      collection: o.option?.collection,
    }));
    const newData = {
      ...parameter,
      data: {
        ...parameter?.data,
        choices: newChoices,
        allSelected: isAllSelected,
      },
    };

    setState((prev) => ({
      ...prev,
      value: options?.length ? options : null,
    }));

    if (isCorrectingError) {
      if (setCorrectedParameterValues) {
        setCorrectedParameterValues((prev) => ({
          ...prev,
          newChoice: newChoices,
        }));
      }
    } else {
      dispatch(
        jobActions.executeParameter({
          parameter: newData,
        }),
      );
    }
  };

  const handleQRSelection = useCallback(() => {
    if (isCorrectingError && source !== 'correction-modal') {
      return;
    }
    dispatch(
      openOverlayAction({
        type: OverlayNames.QR_SCANNER,
        props: { onSuccess: onSelectWithQR },
      }),
    );
  }, [isCorrectingError, source, dispatch, onSelectWithQR]);

  const optionsList = useMemo(() => {
    const selectAllOption = {
      value: 'select-all',
      label: '',
      externalId: <span className="select-all">Select All</span>,
      isSelectAll: true,
    };

    const deselectAllOption = {
      value: 'deselect-all',
      label: '',
      externalId: <span className="deselect-all">Deselect All</span>,
      isDeselectAll: true,
    };

    const _options = options?.map((option) => ({
      value: option.id || option.objectId,
      label: option?.displayName || option?.objectDisplayName,
      externalId: <div>&nbsp;(ID: {option?.externalId || option?.objectExternalId})</div>,
      option,
    }));

    if (parameter.type === MandatoryParameter.MULTI_RESOURCE) {
      const hasFields = parameter.data?.propertyFilters?.fields?.length > 0;
      const hasValue = value?.length > 0;
      const isQueryEmpty = query?.length === 0;

      if (hasFields && isQueryEmpty) {
        if (hasValue) {
          return isCorrectingError
            ? [deselectAllOption, ..._options]
            : [selectAllOption, deselectAllOption, ..._options];
        } else {
          return isCorrectingError ? _options : [selectAllOption, ..._options];
        }
      } else if (hasValue) {
        return [deselectAllOption, ..._options];
      }
    }

    return _options;
  }, [options, value?.length, parameter.type, parameter.data, query]);

  const handleOnChange = (options: any) => {
    if (parameter.type === MandatoryParameter.MULTI_RESOURCE) {
      const castedOptions = options ? (isArray(options) ? options : [options]) : [];
      if (castedOptions.some((option) => option.isDeselectAll)) {
        onRemoveAll();
        return;
      }
      if (castedOptions.some((option) => option.isSelectAll)) {
        if (value?.length === optionsList.length - 1) {
          onSelectOption([]);
        } else {
          const allOptionsExceptSelectAll = optionsList.filter(
            (option) => !(option.isSelectAll || option.isDeselectAll),
          );
          onSelectOption(
            isTaskRepeatOrRecurring ? allOptionsExceptSelectAll : [],
            isTaskRepeatOrRecurring ? false : true,
          );
        }
      } else {
        onSelectOption(castedOptions);
      }
    } else {
      onSelectOption(options ? [options] : []);
    }
  };

  const isDisabled =
    parameter?.autoInitialized ||
    isTaskPaused ||
    isExceptionEnabled ||
    (isTaskCompleted && isCorrectingError && !setCorrectedParameterValues);

  return (
    <Wrapper data-id={parameter.id} data-type={parameter?.type}>
      <ResourceParameterWrapper>
        <Select
          isLoading={status === 'loading'}
          isDisabled={isDisabled}
          options={optionsList}
          hideSelectedOptions={false}
          onMenuOpen={() => {
            if (!isTaskRepeatOrRecurring) {
              reset({ params: urlParams });
            }
            setState((prev) => ({
              ...prev,
              query: '',
            }));
          }}
          isMulti={parameter.type === MandatoryParameter.MULTI_RESOURCE}
          value={value}
          placeholder="You can select one option here"
          onMenuScrollToBottom={() => {
            if (!(status === 'loading') && !isTaskRepeatOrRecurring) {
              fetchNext();
            }
          }}
          closeMenuOnSelect={parameter.type === MandatoryParameter.MULTI_RESOURCE ? false : true}
          onChange={handleOnChange}
          countAsValues={parameter.type === MandatoryParameter.MULTI_RESOURCE}
          onRemove={onRemove}
          isClearable={parameter.type === MandatoryParameter.RESOURCE && !parameter.mandatory}
          onInputChange={debounce((value, actionMeta) => {
            if (value !== actionMeta.prevInputValue) {
              reset({
                params: {
                  ...urlParams,
                  query: value,
                },
              });
              setState((prev) => ({
                ...prev,
                query: value,
              }));
            }
          }, 500)}
          filterOption={() => true}
        />
        {!parameter?.autoInitialized && (
          <div className="qr-selector" onClick={handleQRSelection}>
            <QRIcon />
          </div>
        )}
      </ResourceParameterWrapper>
      {isExceptionEnabled && (
        <div>
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
        </div>
      )}
      {parameter.autoInitialize && dataById?.[parameter.autoInitialize?.parameterId] && (
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <LinkOutlined style={{ marginRight: 8 }} /> Linked to ‘
          {dataById[parameter.autoInitialize.parameterId].label}’
        </div>
      )}
    </Wrapper>
  );
};

export default ResourceParameter;
