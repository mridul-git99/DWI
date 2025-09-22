import QRIcon from '#assets/svg/QR';
import { FormGroup } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter, ParameterType, SelectAllOptionAction } from '#types';
import { apiGetObjects, baseUrl } from '#utils/apiUrls';
import { FilterOperators, InputTypes, ResponseObj, SelectorOptionsEnum } from '#utils/globalTypes';
import { getFilterValuesForResource } from '#utils/parameterUtils';
import { request } from '#utils/request';
import { addHoursOffsetToTime, adjustDateByDaysAtEndOfDay } from '#utils/timeUtils';
import { TObject } from '#views/Ontology/types';
import { getQrCodeData, qrCodeValidator } from '#views/Ontology/utils';
import { LinkOutlined } from '@material-ui/icons';
import { getUnixTime } from 'date-fns';
import { debounce, isArray } from 'lodash';
import React, {
  FC,
  useCallback,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const ResourceParameterWrapper = styled.div`
  display: flex;
  gap: 12px;
  .form-group,
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

type TResourceExecutionViewState = {
  isLoading: Boolean;
  options: any[];
  isOpen: Boolean;
  searchValue: string;
  deselectedOptions: any[];
};

const ResourceExecutionView: FC<Omit<ParameterProps, 'taskId'> & { onChangeHandler?: any }> = ({
  parameter,
  form,
  onChangeHandler,
  parameterValues,
}) => {
  const dispatch = useDispatch();
  const [allSelectionEnabled, setAllSelectionEnabled] = useState(false);
  const parametersList = useTypedSelector(
    (state) => state.prototypeComposer.parameters.parameters.list,
  );
  const facilityTimeZone = useTypedSelector((state) => state.auth?.selectedFacility?.timeZone);

  const selectRef = useRef(null);
  const [menuPlacement, setMenuPlacement] = useState<string>('auto');

  const [state, setState] = useState<TResourceExecutionViewState>({
    isLoading: false,
    options: [],
    isOpen: false,
    searchValue: '',
    deselectedOptions: [],
  });
  const { setValue, watch } = form;
  const { autoInitialize, type, label, id, mandatory, autoInitialized } = parameter;
  const { options, isLoading, isOpen, searchValue, deselectedOptions } = state;
  const pagination = useRef({
    current: -1,
    isLast: false,
    totalElements: 0,
  });

  const parameterInForm = watch(id, {});
  const dependentParameter = autoInitialize?.parameterId ? watch(autoInitialize.parameterId) : null;

  const isMounted = useRef<boolean>(false);

  const linkedParameter = useMemo<Parameter>(() => {
    return parametersList.length > 0
      ? parametersList.find((p) => p?.id === autoInitialize?.parameterId)
      : parameterValues?.find((p) => p?.id === autoInitialize?.parameterId);
  }, [parametersList, parameterValues]);

  const referencedParameterIds = useMemo(() => {
    return parameter?.data?.propertyFilters?.fields?.reduce((acc, field) => {
      if (field?.referencedParameterId) {
        acc.push(field.referencedParameterId);
      }
      return acc;
    }, [] as string[]);
  }, [parameter]);

  const linkedParameterObjectId = useRef(dependentParameter?.data?.choices?.[0]?.objectId);
  const parameterForFilters = watch(referencedParameterIds, {});

  useEffect(() => {
    if (isOpen) getOptions(getUrl(0, searchValue));
  }, [isOpen, searchValue]);

  useEffect(() => {
    isMounted.current = true;

    return () => {
      isMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (dependentParameter?.data?.choices && isMounted.current) {
      handleAutoInitialize();
    }
  }, [dependentParameter]);

  const checkReferencedParameterValues = (fields: any[], parameterForFilters) => {
    return fields.every((field) => {
      if (field?.referencedParameterId) {
        const referencedParameter = parameterForFilters?.[field.referencedParameterId];
        const referencedParameterValue = getFilterValuesForResource(referencedParameter);
        return (
          referencedParameterValue &&
          (!isArray(referencedParameterValue) || referencedParameterValue.length > 0)
        );
      }
      return true;
    });
  };

  const getOptions = async (url?: string) => {
    if (!url) return;

    // If there are property filters with a referencedParameterId, check their values if null skip api call
    if (parameter?.data?.propertyFilters) {
      const { fields } = parameter.data.propertyFilters;
      if (!checkReferencedParameterValues(fields, parameterForFilters)) {
        return;
      }
    }
    setState((prev) => ({ ...prev, isLoading: true }));
    try {
      const response: ResponseObj<any> = await request('GET', url);
      if (response.pageable) {
        pagination.current = {
          current: response.pageable?.page,
          isLast: response.pageable?.last,
          totalElements: response.pageable?.totalElements,
        };
      }
      setState((prev) => ({
        ...prev,
        options:
          pagination.current.current === 0 ? response.data : [...prev.options, ...response.data],
        isLoading: false,
      }));
    } catch (e) {
      setState((prev) => ({ ...prev, isLoading: false }));
    }
  };

  const getUrl = (page: number, searchValue?: string) => {
    let urlString = '';
    if (parameter?.data?.propertyFilters) {
      urlString = `${baseUrl}${parameter.data.urlPath}&page=${page}&filters=${encodeURIComponent(
        JSON.stringify(getFields(parameter.data.propertyFilters)),
      )}`;
    } else {
      urlString = `${baseUrl}${parameter.data.urlPath}&page=${page}`;
    }
    return urlString + (searchValue ? `&query=${searchValue}` : '');
  };

  const getFields = (filters: { op: string; fields: any[] }) => {
    const { fields, op } = filters;
    const _fields: {
      field: any;
      op: any;
      values: any;
    }[] = [];
    fields?.forEach((currField) => {
      const referencedParameterId = currField?.referencedParameterId;
      if (referencedParameterId) {
        const referencedParameter = parameterForFilters?.[referencedParameterId];
        if (referencedParameter) {
          const calculatedValue = getResourceFilterValues(currField, referencedParameter);
          _fields.push({
            field: currField.field,
            op: currField.op,
            values: calculatedValue
              ? isArray(calculatedValue) && calculatedValue.length > 0
                ? calculatedValue
                : [calculatedValue]
              : [null],
          });
        }
      } else {
        const calculatedValue = getResourceFilterValues(currField);
        _fields.push({
          field: currField?.field,
          op: currField?.op,
          values: calculatedValue
            ? isArray(calculatedValue) && calculatedValue.length > 0
              ? calculatedValue
              : [calculatedValue]
            : [null],
        });
      }
    });

    return { op, fields: _fields };
  };

  const getResourceFilterValues = (filterField: any, referencedParameter?: Parameter) => {
    if (filterField.selector === SelectorOptionsEnum.CONSTANT) {
      if (filterField.propertyType === 'DATE' && filterField?.values[0]) {
        const value = adjustDateByDaysAtEndOfDay(
          getUnixTime(new Date()),
          filterField.values[0],
          facilityTimeZone!,
        );
        return value;
      } else if (filterField.propertyType === 'DATE_TIME' && filterField?.values[0]) {
        const value = addHoursOffsetToTime(getUnixTime(new Date()), filterField.values[0]);
        return value;
      } else {
        return filterField.values;
      }
    } else if (filterField.selector === SelectorOptionsEnum.PARAMETER && !!referencedParameter) {
      //For propertytype date and date time we hour tolerance from number parameter then need to convert it.
      if (filterField.propertyType === 'DATE') {
        const referencedParameterValue = getFilterValuesForResource(referencedParameter);
        const value = adjustDateByDaysAtEndOfDay(
          getUnixTime(new Date()),
          referencedParameterValue,
          facilityTimeZone!,
        );
        return value;
      } else if (filterField.propertyType === 'DATE_TIME') {
        const referencedParameterValue = getFilterValuesForResource(referencedParameter);
        const value = addHoursOffsetToTime(getUnixTime(new Date()), referencedParameterValue);
        return value;
      } else {
        return getFilterValuesForResource(referencedParameter);
      }
    }
  };

  const handleAutoInitialize = async () => {
    const objectId = dependentParameter?.data?.choices[0]?.objectId;
    const collection = dependentParameter?.data?.choices[0]?.collection;
    if (linkedParameterObjectId.current !== objectId || !parameterInForm?.data?.choices) {
      try {
        if (objectId && collection) {
          const res: ResponseObj<TObject> = await request('GET', apiGetObjects(objectId), {
            params: {
              collection,
            },
          });
          if (res.data) {
            const relation = res.data.relations.find(
              (r) => r.id === parameter.autoInitialize?.relation.id,
            );
            if (relation) {
              const target = relation.targets[0];
              const parameterData = {
                ...parameter,
                data: {
                  ...parameter.data,
                  choices: [
                    {
                      objectId: target.id,
                      objectDisplayName: target.displayName,
                      objectExternalId: target.externalId,
                      collection: target.collection,
                    },
                  ],
                },
                response: {
                  value: null,
                  reason: '',
                  state: 'EXECUTED',
                  choices: {},
                  medias: [],
                  parameterValueApprovalDto: null,
                },
              };
              setValue(parameter.id, parameterData, {
                shouldValidate: true,
              });
              linkedParameterObjectId.current = objectId;
              onChangeHandler(parameterData);
            } else {
              setValue(parameter.id, parameter, {
                shouldDirty: true,
                shouldValidate: true,
              });
              linkedParameterObjectId.current = undefined;
              onChangeHandler(parameter);
              if (mandatory) {
                throw `${label} has invalid value`;
              }
            }
          }
        } else {
          setValue(parameter.id, parameter, {
            shouldDirty: true,
            shouldValidate: true,
          });
          linkedParameterObjectId.current = undefined;
          onChangeHandler(parameter);
          if (mandatory) {
            throw `${label} has invalid value`;
          }
        }
      } catch (error) {
        dispatch(
          showNotification({
            type: NotificationType.ERROR,
            msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
          }),
        );
        const parameterData = {
          ...parameter,
          data: {
            ...parameter.data,
            choices: undefined,
          },
        };
        setValue(parameter.id, parameterData, {
          shouldDirty: true,
          shouldValidate: true,
        });
        onChangeHandler(parameterData);
      }
    }
  };

  const selectedValues = (allSelected: boolean = false) => {
    return allSelected
      ? options
          .filter((element) => !deselectedOptions.some((item) => item.objectId === element.id))
          .map((c) => ({
            value: c.id,
            label: c.displayName,
            externalId: <div>&nbsp;(ID: {c.externalId})</div>,
            option: {
              displayName: c.displayName,
              externalId: c.externalId,
              collection: c.collection,
            },
          }))
      : parameterInForm?.data?.choices?.length
      ? parameterInForm.data.choices.map((c) => ({
          value: c.objectId,
          label: c.objectDisplayName,
          externalId: <div>&nbsp;(ID: {c.objectExternalId})</div>,
          option: {
            displayName: c.objectDisplayName,
            externalId: c.objectExternalId,
            collection: c.collection,
          },
        }))
      : null;
  };

  const saveParameterValue = (value = [], allSelected = false, deselectChoices = []) => {
    const _options = value ? (isArray(value) ? value : [value]) : [];
    const isValueValid = allSelected ? true : _options.length > 0;
    const parameterData = {
      ...parameter,
      data: {
        ...parameter.data,
        deselectChoices,
        allSelected,
        choices: _options.map((currOption: any) => ({
          objectId: currOption.value,
          objectDisplayName: currOption.option.displayName,
          objectExternalId: currOption.option.externalId,
          collection: currOption.option.collection,
        })),
      },
    };
    setValue(parameter.id, isValueValid || !parameter.mandatory ? parameterData : null, {
      shouldDirty: true,
      shouldValidate: true,
    });
    onChangeHandler(parameterData);
  };

  const onParameterValueChange = (value, selectedValue) => {
    const _options = value ? (isArray(value) ? value : [value]) : [];
    if (allSelectionEnabled) {
      if (selectedValue?.action === SelectAllOptionAction.DESELECT_ACTION) {
        const findRemoveValue = {
          objectId: selectedValue?.option?.value,
          objectDisplayName: selectedValue?.option?.option?.displayName,
          objectExternalId: selectedValue?.option?.option?.externalId,
          collection: selectedValue?.option?.option?.collection,
        };
        const deselectOptions: any[] = [...deselectedOptions, findRemoveValue];
        setState((prev) => ({
          ...prev,
          deselectedOptions: deselectOptions,
        }));
        saveParameterValue([], true, deselectOptions);
      } else if (selectedValue?.action === SelectAllOptionAction.SELECT_ACTION) {
        const findRemoveValue = {
          objectId: selectedValue?.option?.value,
          objectDisplayName: selectedValue?.option?.option?.displayName,
          objectExternalId: selectedValue?.option?.option?.externalId,
          collection: selectedValue?.option?.option?.collection,
        };
        const deselectOptions: any[] = deselectedOptions.filter(
          (item) => item.objectId !== findRemoveValue.objectId,
        );
        setState((prev) => ({
          ...prev,
          deselectedOptions: deselectOptions,
        }));
        saveParameterValue([], true, deselectOptions);
      }
    } else {
      saveParameterValue(_options);
    }
  };

  const onRemove = useCallback(
    (index) => {
      const filteredValues = selectedValues(allSelectionEnabled)?.filter((_, i) => i !== index);
      onParameterValueChange(filteredValues, {
        action: SelectAllOptionAction.DESELECT_ACTION,
        option: selectedValues(allSelectionEnabled)[index],
      });
    },
    [selectedValues, onParameterValueChange, allSelectionEnabled],
  );

  const onSelectWithQR = async (data: string) => {
    try {
      const qrData = await getQrCodeData({
        shortCode: data,
        objectTypeId: parameter?.data?.objectTypeId,
      });
      const isObjectIdPresent = (choices, objectId) =>
        choices.some((choice) => choice.objectId === objectId);

      const isObjectIdInChoices = isObjectIdPresent(
        parameterInForm?.data?.choices || [],
        qrData.objectId,
      );
      const deselectOptions = allSelectionEnabled
        ? deselectedOptions.filter((option) => option.objectId !== qrData.objectId)
        : [];
      if (qrData?.objectId) {
        const parameterData = {
          ...parameter,
          data: {
            ...parameter.data,
            allSelected: allSelectionEnabled,
            deselectChoices: deselectOptions,
            choices: allSelectionEnabled
              ? []
              : isObjectIdInChoices
              ? parameterInForm?.data?.choices || []
              : [
                  ...[qrData].map((currOption) => ({
                    objectId: currOption.objectId,
                    objectDisplayName: currOption.displayName,
                    objectExternalId: currOption.externalId,
                    collection: currOption.collection,
                    value: currOption.objectId,
                  })),
                  ...(type === MandatoryParameter.MULTI_RESOURCE
                    ? parameterInForm?.data?.choices || []
                    : []),
                ],
          },
          response: {
            value: null,
            reason: '',
            state: 'EXECUTED',
            choices: {},
            medias: [],
            parameterValueApprovalDto: null,
          },
        };
        await qrCodeValidator({
          data: qrData,
          callBack: () => {
            setValue(parameter.id, parameterData, {
              shouldDirty: true,
              shouldValidate: true,
            });
            setState((prev) => ({ ...prev, deselectedOptions: deselectOptions }));
            onChangeHandler(parameterData);
          },
          objectTypeValidation: qrData?.objectTypeId === parameter?.data?.objectTypeId,
          filters: parameter?.data?.propertyFilters
            ? {
                op: getFields(parameter.data.propertyFilters).op,
                fields: [
                  ...(getFields(parameter.data.propertyFilters)?.fields || []),
                  { field: 'id', op: FilterOperators.EQ, values: [qrData?.objectId] },
                ],
              }
            : {},
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

  const inputByViewType = (type: ParameterType) => {
    switch (type) {
      case MandatoryParameter.RESOURCE:
        return InputTypes.SINGLE_SELECT;
      case MandatoryParameter.MULTI_RESOURCE:
        return InputTypes.MULTI_SELECT;
      default:
        return InputTypes.SINGLE_SELECT;
    }
  };

  const fetchOptions = (options: any[]) => {
    const selectAllOption = {
      value: 'select-all',
      label: '',
      externalId: <span className="select-all">Select All</span>,
      isSelectAll: true,
    };

    const deselectAllOption = {
      value: 'deselect-all',
      label: '',
      externalId: <div className="deselect-all">Deselect All</div>,
      isDeselectAll: true,
    };

    const _options = options?.map((option) => ({
      value: option.id,
      label: option.displayName,
      externalId: <div>&nbsp;(ID: {option.externalId})</div>,
      option,
    }));

    if (parameter.type === MandatoryParameter.MULTI_RESOURCE) {
      const hasFields = parameter?.data?.propertyFilters?.fields?.length > 0;
      const hasValue = selectedValues(allSelectionEnabled)?.length > 0;
      const isQueryEmpty = searchValue?.length === 0;

      let eligibleOptions = [..._options];
      if (hasValue) {
        eligibleOptions.unshift(deselectAllOption);
      }
      if (isQueryEmpty && hasFields) {
        eligibleOptions.unshift(selectAllOption);
      }
      return eligibleOptions;
    }

    return type === MandatoryParameter.MULTI_RESOURCE && selectedValues(allSelectionEnabled)?.length
      ? [deselectAllOption, ..._options]
      : _options;
  };

  const selectionChange = (allSelected: boolean) => {
    setAllSelectionEnabled(() => allSelected);
    setState((prev) => ({ ...prev, deselectedOptions: [] }));
    saveParameterValue([], allSelected, []);
  };

  useLayoutEffect(() => {
    const calculateMenuPosition = () => {
      if (selectRef.current) {
        const selectRect = selectRef.current.getBoundingClientRect();
        const availableSpaceBelow = window.innerHeight - selectRect.bottom;
        setMenuPlacement(availableSpaceBelow < 250 ? 'top' : 'bottom');
      }
    };

    calculateMenuPosition();

    window.addEventListener('resize', calculateMenuPosition);

    return () => {
      window.removeEventListener('resize', calculateMenuPosition);
    };
  }, []);

  return (
    <>
      <ResourceParameterWrapper>
        <FormGroup
          ref={selectRef}
          inputs={[
            {
              type: inputByViewType(type),
              props: {
                id: parameter.id,
                options: fetchOptions(options),
                menuPortalTarget: document.body,
                menuPosition: 'fixed',
                menuShouldBlockScroll: true,
                menuPlacement,
                ['data-id']: parameter.id,
                ['data-type']: type,
                hideSelectedOptions: false,
                onMenuScrollToBottom: () => {
                  if (!isLoading && !pagination.current.isLast) {
                    getOptions(getUrl(pagination.current.current + 1, searchValue));
                  }
                },
                onMenuOpen: () => {
                  setState((prev) => ({ ...prev, isOpen: true }));
                },
                onMenuClose: () => {
                  setState((prev) => ({ ...prev, isOpen: false }));
                },
                onChange: (value: any, selectedOption: any) => {
                  if (parameter.type === MandatoryParameter.RESOURCE) {
                    saveParameterValue(value);
                  } else {
                    const allSelected =
                      SelectAllOptionAction.SELECT_ACTION === selectedOption?.action &&
                      selectedOption?.option?.isSelectAll;
                    const isDeselectAll =
                      SelectAllOptionAction.SELECT_ACTION === selectedOption?.action &&
                      selectedOption?.option?.isDeselectAll;
                    if (allSelected) {
                      selectionChange(true);
                    } else if (isDeselectAll) {
                      selectionChange(false);
                    } else {
                      onParameterValueChange(value, selectedOption);
                    }
                  }
                },
                onInputChange: debounce((value, actionMeta) => {
                  if (value !== actionMeta.prevInputValue) {
                    setState((prev) => ({ ...prev, searchValue: value }));
                  }
                }, 500),
                value: selectedValues(allSelectionEnabled),
                isClearable: parameter.type === MandatoryParameter.RESOURCE && !parameter.mandatory,
                isDisabled: autoInitialized,
                countAsValues: type === MandatoryParameter.MULTI_RESOURCE,
                onRemove,
                extraCount: allSelectionEnabled
                  ? pagination.current?.totalElements -
                    deselectedOptions.length -
                    selectedValues(allSelectionEnabled)?.length
                  : 0,
                filterOption: () => true,
              },
            },
          ]}
        />
        {!autoInitialized && (
          <div
            className="qr-selector"
            onClick={() => {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.QR_SCANNER,
                  props: { onSuccess: onSelectWithQR },
                }),
              );
            }}
          >
            <QRIcon />
          </div>
        )}
      </ResourceParameterWrapper>
      {autoInitialized && (
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <LinkOutlined style={{ marginRight: 8 }} /> Linked to ‘{linkedParameter?.label}’
        </div>
      )}
    </>
  );
};

export default ResourceExecutionView;
