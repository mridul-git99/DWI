import { FormGroup } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter, Selections } from '#types';
import { InputTypes } from '#utils/globalTypes';
import { getErrorMsg } from '#utils/request';
import { getObjectData } from '#views/Ontology/utils';
import { LinkOutlined } from '@material-ui/icons';
import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';

const SingleSelectExecutionView: FC<Omit<ParameterProps, 'taskId' | 'isReadOnly'>> = ({
  parameter,
  form,
  onChangeHandler,
  parameterValues,
}) => {
  const dispatch = useDispatch();
  const { list: parametersList } = useTypedSelector(
    (state) => state.prototypeComposer.parameters.parameters,
  );
  const { setValue, watch } = form;

  const [selectedValue, setSelectedValue] = useState(() => {
    return parameter.data
      .filter((item: any) => item.state === 'SELECTED')
      .map((item: any) => ({ label: item.name, value: item.id }));
  });
  const { autoInitialize, autoInitialized, label, type, mandatory, id } = parameter;
  const dependentParameter = autoInitialize?.parameterId ? watch(autoInitialize.parameterId) : null;

  const isMounted = useRef<boolean>(false);

  const linkedParameter = useMemo<Parameter>(() => {
    return parametersList.length > 0
      ? parametersList.find((p) => p?.id === autoInitialize?.parameterId)
      : parameterValues?.find((p) => p?.id === autoInitialize?.parameterId);
  }, [parametersList, parameterValues]);

  const options = useMemo(
    () =>
      parameter.data.map((option: any) => ({
        label: option.name,
        value: option.id,
      })),
    [parameter.data],
  );

  const placeholder = useMemo(
    () =>
      type === 'MULTISELECT' ? 'Select one or more options' : 'You can select one option here',
    [type],
  );

  const typeOfSelect = useCallback((type) => {
    if (type === MandatoryParameter.MULTISELECT) {
      return InputTypes.MULTI_SELECT;
    } else return InputTypes.SINGLE_SELECT;
  }, []);

  const optionChosen = (selectedOptions: any, optionsList: any) => {
    let choices = {};
    const selectedOptionsMap = new Map();
    selectedOptions.forEach((currOption) => {
      selectedOptionsMap.set(currOption.value, currOption.label);
    });

    optionsList.forEach((currOption: any) => {
      choices = selectedOptionsMap.has(currOption.id)
        ? { ...choices, [currOption.id]: 'SELECTED' }
        : { ...choices, [currOption.id]: 'NOT_SELECTED' };
    });
    return choices;
  };

  const selectedData = (selectedOptions: any, optionsList: any) => {
    const selectedOptionsMap = new Map();
    selectedOptions.forEach((currOption) => {
      selectedOptionsMap.set(currOption.value, currOption.label);
    });

    return selectedOptions.length
      ? optionsList.map((currOption: any) => {
          return selectedOptionsMap.has(currOption.id)
            ? {
                ...currOption,
                state: 'SELECTED',
              }
            : { ...currOption, state: 'NOT_SELECTED' };
        })
      : optionsList.map((currOption: any) => ({ ...currOption, state: 'NOT_SELECTED' }));
  };

  const handleAutoInitialize = async () => {
    const objectId = dependentParameter?.data?.choices[0]?.objectId;
    const collection = dependentParameter?.data?.choices[0]?.collection;
    try {
      if (objectId && collection) {
        const object = await getObjectData({ id: objectId, collection });
        const property = object?.properties?.find((p) => p.id === autoInitialize?.property?.id);
        const value = property?.choices?.map((option) => {
          return { label: option.displayName, value: option.id };
        });
        onParameterValueChanged(value);
        if (!value?.length && mandatory) {
          throw `${label} has invalid value`;
        }
      } else {
        onParameterValueChanged([]);
      }
    } catch (e: any) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(e),
        }),
      );
    }
  };

  const onParameterValueChanged = useCallback(
    (_value) => {
      const value = _value ? (Array.isArray(_value) ? _value : [_value]) : [];
      const parameterData = {
        ...parameter,
        data: selectedData(value, parameter.data),
        response: {
          value: null,
          reason: '',
          state: 'EXECUTED',
          choices: optionChosen(value, parameter.data),
          medias: [],
          parameterValueApprovalDto: null,
        },
      };
      setValue(id, parameterData, {
        shouldDirty: true,
        shouldValidate: true,
      });
      onChangeHandler(parameterData);
      setSelectedValue(
        parameterData?.data
          ?.filter((option) => option.state === Selections.SELECTED)
          .map((option) => {
            return { label: option.name, value: option.id };
          }),
      );
    },
    [parameter, selectedData],
  );

  const onRemove = useCallback(
    (index) => {
      const filteredValue = selectedValue.filter((_, i) => i !== index);
      onParameterValueChanged(filteredValue);
    },
    [selectedValue, onParameterValueChanged],
  );

  useEffect(() => {
    isMounted.current = true;

    return () => {
      isMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (dependentParameter?.data?.choices && isMounted.current) handleAutoInitialize();
  }, [dependentParameter]);

  return (
    <>
      <FormGroup
        inputs={[
          {
            type: typeOfSelect(type),
            props: {
              id: id,
              options,
              menuPortalTarget: document.body,
              menuPosition: 'fixed',
              menuShouldBlockScroll: true,
              isClearable: true,
              isDisabled: autoInitialized,
              ['data-id']: id,
              ['data-type']: type,
              placeholder,
              onChange: (value: any) => {
                onParameterValueChanged(value);
              },
              value: selectedValue,
              countAsValues: type === MandatoryParameter.MULTISELECT,
              onRemove,
            },
          },
        ]}
      />
      {autoInitialized && (
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <LinkOutlined style={{ marginRight: 8 }} /> Linked to ‘{linkedParameter?.label}’
        </div>
      )}
    </>
  );
};

export default SingleSelectExecutionView;
