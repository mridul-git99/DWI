import React, { FC, useEffect, useMemo, useRef } from 'react';
import CustomDateTimePicker from '#components/shared/CustomDateTimePicker';
import { Parameter, ParameterProps } from '#PrototypeComposer/Activity/types';
import { getObjectData } from '#views/Ontology/utils';
import { getErrorMsg } from '#utils/request';
import { NotificationType } from '#components/Notification/types';
import { showNotification } from '#components/Notification/actions';
import { useDispatch } from 'react-redux';
import { useTypedSelector } from '#store';
import { LinkOutlined } from '@material-ui/icons';

const DateTimeExecutionView: FC<Omit<ParameterProps, 'taskId' | 'isReadOnly' | 'onChangeHandler'>> =
  ({ parameter, form, parameterValues }: any) => {
    const dispatch = useDispatch();
    const parametersList = useTypedSelector(
      (state) => state.prototypeComposer.parameters.parameters.list,
    );

    const { setValue, watch, clearErrors, setError, trigger } = form;
    const { autoInitialize, label, id, mandatory, autoInitialized } = parameter;

    const parameterInForm = watch(id, {});
    const dependentParameter = autoInitialize?.parameterId
      ? watch(autoInitialize.parameterId)
      : null;

    const isMounted = useRef<boolean>(false);

    const linkedParameter = useMemo<Parameter>(() => {
      return parametersList.length > 0
        ? parametersList.find((p) => p?.id === autoInitialize?.parameterId)
        : parameterValues?.find((p) => p?.id === autoInitialize?.parameterId);
    }, [parametersList, parameterValues]);

    const handleAutoInitialize = async () => {
      const objectId = dependentParameter?.data?.choices[0]?.objectId;
      const collection = dependentParameter?.data?.choices[0]?.collection;
      if (objectId && collection) {
        try {
          const object = await getObjectData({ id: objectId, collection });
          const property = object?.properties?.find((p) => p.id === autoInitialize?.property?.id);
          handleOnChange({
            value: property?.value,
          });
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
        handleOnChange({});
      }
    };

    const handleOnChange = ({ value }: { value?: any }) => {
      const isValueValid = value && value.toString().trim() !== '';
      setValue(
        id,
        {
          ...parameter,
          data: isValueValid ? { ...parameter.data, input: parseInt(value) } : {},
          response: {
            value: parseInt(value),
            reason: '',
            state: 'EXECUTED',
            choices: {},
            medias: [],
            parameterValueApprovalDto: null,
          },
        },
        {
          shouldDirty: true,
        },
      );
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
        <CustomDateTimePicker
          isDisabled={autoInitialized}
          parameter={parameter}
          parameterValue={parameterInForm?.data?.input}
          onChange={handleOnChange}
        />
        {autoInitialized && (
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <LinkOutlined style={{ marginRight: 8 }} /> Linked to ‘{linkedParameter?.label}’
          </div>
        )}
      </>
    );
  };

export default DateTimeExecutionView;
