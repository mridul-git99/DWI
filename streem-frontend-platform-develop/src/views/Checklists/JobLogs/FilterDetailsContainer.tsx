import { FormGroup } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { Constraint, MandatoryParameter } from '#types';
import { validateNumber } from '#utils';
import { apiSingleParameter, baseUrl } from '#utils/apiUrls';
import { InputTypes, ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { getObjectData } from '#views/Ontology/utils';
import { debounce } from 'lodash';
import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { metaFilters } from './Overlays/FiltersDrawer';

const FilterDetailsWrapper = styled.div`
  display: flex;
  gap: 12px;

  .input {
    width: 150px;
  }
`;

const FilterDetail: FC<any> = ({ item, index, onSubmitFilters }) => {
  const dispatch = useDispatch();
  const [state, setState] = useState<{
    valueOptionsLoading: boolean;
    valueOptions: any[];
    selectedParameter?: any;
    selectedValue?: any;
    searchValue?: string;
  }>({
    valueOptionsLoading: false,
    valueOptions: [],
    selectedParameter: undefined,
    searchValue: '',
  });
  const pagination = useRef({
    current: -1,
    isLast: false,
  });

  const { selectedParameter, valueOptions, valueOptionsLoading, selectedValue, searchValue } =
    state;
  const debounceInputRef = useRef(debounce((event, functor) => functor(event), 1000));

  const inputTypeForParameter = useMemo(() => {
    const { type } = selectedParameter || {};
    const isMultiConstraint =
      item?.constraint === Constraint.ANY || item?.constraint === Constraint.NIN;

    if (
      [
        MandatoryParameter.RESOURCE,
        MandatoryParameter.SINGLE_SELECT,
        MandatoryParameter.YES_NO,
      ].includes(type)
    ) {
      if (
        [MandatoryParameter.RESOURCE, MandatoryParameter.SINGLE_SELECT].includes(type) &&
        isMultiConstraint
      ) {
        return InputTypes.MULTI_SELECT;
      }
      return InputTypes.SINGLE_SELECT;
    }
    if ([MandatoryParameter.MULTISELECT, MandatoryParameter.MULTI_RESOURCE].includes(type)) {
      return InputTypes.MULTI_SELECT;
    }
    if ([MandatoryParameter.CALCULATION, MandatoryParameter.SHOULD_BE].includes(type)) {
      return InputTypes.NUMBER;
    }
    return type || InputTypes.SINGLE_LINE;
  }, [selectedParameter, item?.constraint]);

  const fetchReferenceParameter = useCallback(async (parameterId: string) => {
    const { data } = await request('GET', apiSingleParameter(parameterId));
    setState((prev) => ({
      ...prev,
      selectedParameter: data || prev.selectedParameter,
      valueOptionsLoading: false,
    }));
  }, []);

  useEffect(() => {
    const selectedParameterId = item?.key?.split('.')?.[1] || item?.key?.split('.')?.[0];
    if (selectedParameterId) {
      const predefinedParam = metaFilters.find(
        (p: any) => p.id === selectedParameterId || p.value === selectedParameterId,
      );
      if (predefinedParam || selectedParameterId === 'checklistId') {
        setState((prev) => ({ ...prev, selectedParameter: predefinedParam }));
      } else {
        setState((prev) => ({ ...prev, valueOptionsLoading: true }));
        fetchReferenceParameter(selectedParameterId);
      }
    }
  }, [item, fetchReferenceParameter]);

  useEffect(() => {
    if (selectedParameter) {
      if (
        selectedParameter.type === MandatoryParameter.RESOURCE ||
        selectedParameter.type === MandatoryParameter.MULTI_RESOURCE
      ) {
        getOptions();
      } else if (
        selectedParameter.type === MandatoryParameter.SINGLE_SELECT ||
        selectedParameter.type === MandatoryParameter.MULTISELECT
      ) {
        const _valueOptions = selectedParameter?.data?.map((i: any) => ({
          label: i.name,
          value: i.id,
        }));

        let _selectedValue = null;
        if (Array.isArray(item?.value)) {
          _selectedValue = _valueOptions?.filter((o: any) => item.value.includes(o.value));
        } else {
          _selectedValue = _valueOptions?.find((o: any) => o.value === item?.value);
          _selectedValue = _selectedValue ? [_selectedValue] : null;
        }

        setState((prev) => ({
          ...prev,
          valueOptions: _valueOptions,
          selectedValue: _selectedValue || prev?.selectedValue,
        }));
      } else if (selectedParameter.type === MandatoryParameter.YES_NO) {
        const _valueOptions = selectedParameter?.data?.map((i: any) => ({
          label: i.name,
          value: i.id,
          type: i.type,
        }));
        const _selectedValue = _valueOptions?.find(
          (o: any) => o.value === item?.value || o.value === item?.value?.[0],
        );
        setState((prev) => ({
          ...prev,
          valueOptions: _valueOptions,
          selectedValue: _selectedValue ? [_selectedValue] : prev?.selectedValue,
        }));
      }
    }
  }, [selectedParameter]);

  const getOptions = useCallback(
    async (page: number = pagination.current.current + 1, query: string = '') => {
      if (
        selectedParameter.type === MandatoryParameter.RESOURCE ||
        selectedParameter.type === MandatoryParameter.MULTI_RESOURCE
      ) {
        setState((prev) => ({
          ...prev,
          valueOptionsLoading: true,
          valueOptions: page === 0 ? [] : prev?.valueOptions,
        }));
        try {
          const response: ResponseObj<any> = await request(
            'GET',
            `${baseUrl}${selectedParameter.data.urlPath}`,
            {
              params: {
                page,
                ...(query ? { query } : {}),
              },
            },
          );
          if (response.data) {
            if (response.pageable) {
              pagination.current = {
                current: response.pageable?.page,
                isLast: response.pageable?.last,
              };
            }
            const optionsToSet = response.data.map((o: any) => ({
              value: o.id,
              label: o.displayName,
              externalId: o.externalId,
            }));

            let _selectedValues = [];
            const filterValues = item?.value
              ? Array.isArray(item.value)
                ? item.value
                : [item?.value]
              : null;

            if (filterValues && filterValues.length) {
              const promises = filterValues?.map(async (value: string) => {
                let _selectedValue;
                if (value && selectedParameter.data.collection) {
                  const object = await getObjectData({
                    id: value,
                    collection: selectedParameter.data.collection,
                  });
                  _selectedValue = {
                    value: object.id,
                    label: object.displayName,
                    externalId: object.externalId,
                  };
                }
                return _selectedValue;
              });

              _selectedValues = await Promise.all(promises);
            }

            setState((prev) => ({
              ...prev,
              valueOptions: page === 0 ? optionsToSet : [...prev.valueOptions, ...optionsToSet],
              valueOptionsLoading: false,
              selectedValue: _selectedValues.length ? _selectedValues : prev.selectedValue,
              searchValue: prev.searchValue,
            }));
          }
        } catch (e) {
          setState((prev) => ({ ...prev, isLoading: false }));
        }
      }
    },
    [selectedParameter, pagination, item.value],
  );

  // Handle search value changes
  useEffect(() => {
    if (searchValue !== undefined && selectedParameter?.type) {
      if (
        selectedParameter.type === MandatoryParameter.RESOURCE ||
        selectedParameter.type === MandatoryParameter.MULTI_RESOURCE
      ) {
        pagination.current = {
          current: -1,
          isLast: true,
        };
        getOptions(0, searchValue);
      }
    }
  }, [searchValue, selectedParameter, getOptions]);

  const getValue = useMemo(() => {
    if (
      [
        MandatoryParameter.SINGLE_SELECT,
        MandatoryParameter.RESOURCE,
        MandatoryParameter.YES_NO,
        MandatoryParameter.MULTISELECT,
        MandatoryParameter.MULTI_RESOURCE,
      ].includes(selectedParameter?.type)
    ) {
      return selectedValue;
    } else {
      return selectedValue?.value || selectedValue || item.value;
    }
  }, [selectedParameter, selectedValue, item?.value]);

  if (item?.key === 'checklistId') {
    return null;
  }

  return (
    <FormGroup
      inputs={[
        {
          type: inputTypeForParameter,
          props: {
            id: item?.key,
            label: selectedParameter?.label || 'Select Filter',
            options: valueOptions,
            value: getValue,
            isSearchable: true,
            onInputChange: debounce((value, actionMeta) => {
              if (value !== actionMeta.prevInputValue) {
                setState((prev) => ({ ...prev, searchValue: value }));
              }
            }, 500),
            placeholder: selectedParameter?.label || 'Select Filter',
            onChange: (value: any) => {
              let newValue: any = null;
              if (!value || (Array.isArray(value) ? value.length === 0 : !value.value)) {
                dispatch(
                  showNotification({
                    type: NotificationType.ERROR,
                    msg: 'Please select a value',
                  }),
                );
                return;
              }
              if (Array.isArray(value)) {
                newValue = value?.length ? value : null;
              } else if (
                [
                  MandatoryParameter.SINGLE_SELECT,
                  MandatoryParameter.RESOURCE,
                  MandatoryParameter.YES_NO,
                  MandatoryParameter.MULTISELECT,
                  MandatoryParameter.MULTI_RESOURCE,
                ].includes(selectedParameter?.type)
              ) {
                newValue = value?.value ? [value] : null;
              } else if (
                [
                  InputTypes.DATE,
                  InputTypes.DATE_TIME,
                  InputTypes.NUMBER,
                  MandatoryParameter.CALCULATION,
                ].includes(selectedParameter?.type)
              ) {
                newValue = validateNumber(value?.value) ? parseFloat(value?.value) : null;
              } else {
                newValue = value?.value ? value : null;
              }
              debounceInputRef.current(newValue, (newValue: any) => {
                onSubmitFilters(newValue, index);
              });
              setState((prev) => ({ ...prev, selectedValue: newValue }));
            },
            style: {
              flex: 1,
            },
            floatingLabel: true,
            showTooltip: true,
            isLoading: valueOptionsLoading,
            onMenuScrollToBottom: () => {
              if (!valueOptionsLoading && !pagination.current.isLast) {
                getOptions(pagination.current.current + 1, searchValue);
              }
            },
          },
        },
      ]}
    />
  );
};

export const FilterDetails: FC<any> = ({ onSubmit, allFilters }) => {
  const onSubmitFilters = (newValue: any, index: number) => {
    const newFilters = allFilters.map((f: any, i: number) => {
      if (i === index) {
        return {
          ...f,
          value: Array.isArray(newValue)
            ? newValue?.map((v: any) => v.value)
            : typeof newValue === 'object'
            ? newValue?.value
            : newValue,
        };
      }
      return f;
    });
    onSubmit({ filters: newFilters });
  };

  return (
    <FilterDetailsWrapper>
      {allFilters?.map((item: any, index: number) => (
        <FilterDetail
          key={`${item.id}_${item.constraint}_${
            Array.isArray(item.value) ? item.value?.join(',') : item.value
          }`}
          item={item}
          index={index}
          onSubmitFilters={onSubmitFilters}
        />
      ))}
    </FilterDetailsWrapper>
  );
};
