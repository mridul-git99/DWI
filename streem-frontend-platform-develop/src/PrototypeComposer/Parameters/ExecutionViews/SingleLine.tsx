import { FormGroup } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter, ParameterType } from '#types';
import { InputTypes } from '#utils/globalTypes';
import { getErrorMsg } from '#utils/request';
import { getObjectData } from '#views/Ontology/utils';
import { LinkOutlined } from '@material-ui/icons';
import React, { FC, useCallback, useEffect, useMemo, useRef } from 'react';
import { useDispatch } from 'react-redux';

const SingleLineExecutionView: FC<
  Omit<ParameterProps, 'taskId' | 'isReadOnly' | 'onChangeHandler'>
> = ({ parameter, form, parameterValues, onChangeHandler }) => {
  const dispatch = useDispatch();
  const parametersList = useTypedSelector(
    (state) => state.prototypeComposer.parameters.parameters.list,
  );

  const { setValue, watch, clearErrors, setError, trigger } = form;
  const { autoInitialize, type, label, id, mandatory, autoInitialized } = parameter;

  const parameterInForm = watch(id, {});
  const dependentParameter = autoInitialize?.parameterId ? watch(autoInitialize.parameterId) : null;

  const isMounted = useRef<boolean>(false);

  const linkedParameter = useMemo<Parameter>(() => {
    return parametersList.length > 0
      ? parametersList.find((p) => p?.id === autoInitialize?.parameterId)
      : parameterValues?.find((p) => p?.id === autoInitialize?.parameterId);
  }, [parametersList, parameterValues]);

  const parseValue = useCallback((value: string, type: ParameterType) => {
    switch (type) {
      case MandatoryParameter.SINGLE_LINE:
      case MandatoryParameter.MULTI_LINE:
      case MandatoryParameter.NUMBER:
        return value;
      default:
        return;
    }
  }, []);

  const handleAutoInitialize = async () => {
    const objectId = dependentParameter?.data?.choices[0]?.objectId;
    const collection = dependentParameter?.data?.choices[0]?.collection;
    if (objectId && collection) {
      try {
        const object = await getObjectData({ id: objectId, collection });
        const property = object?.properties?.find((p) => p.id === autoInitialize?.property?.id);
        handleOnChange(property?.value);
        if (!property?.value && mandatory) {
          throw `${label} has invalid value`;
        }
      } catch (e: any) {
        dispatch(
          showNotification({
            type: NotificationType.ERROR,
            msg: typeof e !== 'string' ? getErrorMsg(e) : e,
          }),
        );
      }
    } else {
      handleOnChange();
    }
  };

  const handleOnChange = (value?: any) => {
    const isValueValid = value && value.toString().trim() !== '';
    const parameterData = {
      ...parameter,
      data: isValueValid ? { ...parameter.data, input: parseValue(value, type) } : {},
      response: {
        value: parseValue(value, type),
        reason: '',
        state: 'EXECUTED',
        choices: {},
        medias: [],
        parameterValueApprovalDto: null,
      },
    };
    setValue(id, parameterData, {
      shouldDirty: true,
    });
    onChangeHandler(parameterData);
    if (isValueValid) {
      clearErrors(id);
      trigger(id);
    } else if (mandatory) {
      setError(id, {
        type: 'manual',
        message: 'required',
      });
    }
  };

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

  return (
    <>
      <FormGroup
        style={{ padding: 0 }}
        inputs={[
          {
            type: type as unknown as InputTypes,
            props: {
              id,
              ['data-id']: id,
              ['data-type']: type,
              disabled: autoInitialized,
              value: parameterInForm?.data?.input || '',
              onChange: (value: any) => {
                handleOnChange(value.value);
              },
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

export default SingleLineExecutionView;
