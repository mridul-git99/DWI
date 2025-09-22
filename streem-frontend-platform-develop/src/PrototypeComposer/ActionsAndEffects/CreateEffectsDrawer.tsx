import React, { Dispatch, FC, SetStateAction, useEffect, useMemo } from 'react';
import styled from 'styled-components';
import { Button, FormGroup, ToggleSwitch, useDrawer } from '#components';
import Mentions from '#components/shared/Mentions';
import { Controller, useForm } from 'react-hook-form';
import { InputTypes } from '#utils/globalTypes';
import { nonEmptyStringRegex } from '#utils/constants';
import { useTypedSelector } from '#store';
import { getErrorMsg, request } from '#utils/request';
import { apiCreateEffect, apiEditOrGetEffect, apiGetEffectsByActionId } from '#utils/apiUrls';
import { useQueryParams } from '#hooks/useQueryParams';
import { EffectEntity, EffectType, MethodType } from '#types/actionsAndEffects';
import { useDispatch } from 'react-redux';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

const Wrapper = styled.div`
  display: flex;
  width: 100%;
  flex-direction: column;
  flex: 1;
  padding: 16px 0px;
  gap: 16px;

  .form-group {
    padding: 0;

    .input {
      margin-bottom: 0 !important;
    }
  }
`;

type TCreateEffectsDrawerProps = {
  onCloseDrawer: Dispatch<SetStateAction<boolean>>;
  effectData?: Record<string, any>;
  actions: Record<string, any>;
  isReadOnly: boolean;
  setEffects?: Dispatch<SetStateAction<any[]>>;
};

const effectTypeOptions = [
  { value: EffectType.SQL_QUERY, label: 'SQL Query' },
  { value: EffectType.REST_API, label: 'REST API' },
  { value: EffectType.MONGO_QUERY, label: 'MONGO Query' },
];

const methodsOptions = [
  { value: MethodType.GET, label: 'GET' },
  { value: MethodType.POST, label: 'POST' },
  { value: MethodType.PATCH, label: 'PATCH' },
  { value: MethodType.PUT, label: 'PUT' },
];

const CreateEffectsDrawer: FC<TCreateEffectsDrawerProps> = ({
  onCloseDrawer,
  effectData,
  actions,
  isReadOnly,
  setEffects,
}) => {
  const dispatch = useDispatch();
  const { getQueryParam } = useQueryParams();

  const actionId = getQueryParam('actionId');

  const parameters = useTypedSelector((state) => state.prototypeComposer.parameters.listById);
  const cjfParameters = useTypedSelector((state) => state.prototypeComposer.data?.parameters) || [];
  const tasks = useTypedSelector((state) => state.prototypeComposer.tasks.listById);

  const effects = actions[actionId!].effects || [];

  const mentionItems = useMemo(() => {
    const mentionItems = {
      '@p': [...Object.values(parameters), ...cjfParameters].map((parameter) => ({
        id: parameter.id,
        value: parameter.label,
        entity: EffectEntity.parameter,
      })),
      '@t': Object.values(tasks).map((task) => ({
        id: task.id,
        value: task.name,
        entity: EffectEntity.task,
      })),
      '@e': effects.map((effect) => ({
        id: effect.id,
        value: effect.name,
        entity: EffectEntity.effect,
      })),
      '@s': [
        {
          id: 'jobId',
          value: 'Job Id',
          entity: EffectEntity.constant,
        },
        {
          id: 'facilityId',
          value: 'facility Id',
          entity: EffectEntity.constant,
        },
        {
          id: 'usecaseId',
          value: 'usecase Id',
          entity: EffectEntity.constant,
        },
      ],
    };

    return mentionItems;
  }, [parameters, cjfParameters.length]);

  const form = useForm<{
    name: string;
    description: string;
    effectType: EffectType | null;
    apiMethod?: MethodType | null;
    apiEndpoint?: string | null;
    apiHeader: string | null;
    apiPayload: string | null;
    isJavascriptEnabled: boolean;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      name: effectData?.name || '',
      description: effectData?.description || '',
      effectType: effectData?.effectType || null,
      apiMethod: effectData?.apiMethod || null,
      apiHeader: effectData?.apiHeaders ? effectData?.apiHeaders : null,
      isJavascriptEnabled: effectData?.javascriptEnabled || false,
      apiEndpoint: JSON.stringify(effectData?.apiEndpoint) || null,
      apiPayload: effectData?.apiPayload
        ? JSON.stringify(effectData?.apiPayload)
        : effectData?.query
        ? JSON.stringify(effectData?.query)
        : null,
    },
  });

  const {
    handleSubmit,
    register,
    watch,
    control,
    formState: { isDirty, isValid },
  } = form;

  const { effectType, isJavascriptEnabled } = watch(['effectType', 'isJavascriptEnabled']);

  const createEffect = async (effectsData: any) => {
    const { data, errors } = await request(
      effectData ? 'PATCH' : 'POST',
      effectData ? apiEditOrGetEffect(effectData.id) : apiCreateEffect(actionId!),
      {
        data: {
          name: effectsData.name,
          description: effectsData.description,
          orderTree: effectData?.orderTree || effects.length,
          effectType: effectsData.effectType,
          ...(effectsData?.apiEndpoint && {
            apiEndpoint: JSON.parse(effectsData.apiEndpoint),
          }),
          apiMethod: effectsData.apiMethod,

          ...(effectsData.effectType === EffectType.REST_API &&
            effectsData?.apiHeader && { apiHeaders: JSON.parse(effectsData.apiHeader) }),
          javascriptEnabled: effectsData?.isJavascriptEnabled || false,
          actionId,
          ...(effectsData.effectType === EffectType.REST_API && effectsData?.apiPayload
            ? {
                apiPayload: JSON.parse(effectsData.apiPayload),
              }
            : {
                query: JSON.parse(effectsData.apiPayload),
              }),
        },
      },
    );

    if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }

    if (data) {
      const effectsResponse = await request('GET', apiGetEffectsByActionId(actionId!));
      if (effectsResponse.data && setEffects) {
        setEffects(effectsResponse.data);
      }

      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Effect ${effectData ? 'updated' : 'created'} successfully`,
        }),
      );
      handleCloseDrawer();
    }
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  };

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Create Effect',
    hideCloseIcon: true,
    bodyContent: (
      <Wrapper>
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write here',
                label: 'Effect Name',
                id: 'effectName',
                name: 'name',
                disabled: isReadOnly,
                ref: register({
                  required: true,
                  pattern: nonEmptyStringRegex,
                }),
              },
            },
          ]}
        />
        <FormGroup
          inputs={[
            {
              type: InputTypes.MULTI_LINE,
              props: {
                placeholder: 'Write here',
                label: 'Description',
                id: 'description',
                name: 'description',
                optional: true,
                disabled: isReadOnly,
                ref: register({
                  pattern: nonEmptyStringRegex,
                }),
                rows: 3,
                maxRows: 6,
              },
            },
          ]}
        />
        <Controller
          control={control}
          name="effectType"
          key="effectType"
          shouldUnregister={false}
          rules={{
            required: true,
          }}
          render={({ value, onChange }) => (
            <FormGroup
              inputs={[
                {
                  type: InputTypes.SINGLE_SELECT,
                  props: {
                    id: 'effectType',
                    label: 'Type',
                    options: effectTypeOptions,
                    isDisabled: isReadOnly,
                    value: value
                      ? effectTypeOptions.find((option) => option.value === value)
                      : null,
                    placeholder: 'Select',
                    onChange: (option: { value: string }) => {
                      onChange(option.value);
                    },
                  },
                },
              ]}
            />
          )}
        />
        <Controller
          control={control}
          name="isJavascriptEnabled"
          key="isJavascriptEnabled"
          shouldUnregister={false}
          defaultValue={isJavascriptEnabled}
          render={({ value, onChange }) => (
            <ToggleSwitch
              height={24}
              width={48}
              offLabel="Enable Javascript"
              onColor="#24a148"
              onChange={(isChecked) => {
                onChange(isChecked);
              }}
              onLabel="Disable Javascript"
              checked={value}
              disabled={isReadOnly}
            />
          )}
        />
        {effectType === EffectType.REST_API && (
          <>
            <Controller
              control={control}
              name="apiMethod"
              key="apiMethod"
              shouldUnregister={false}
              rules={{
                required: true,
              }}
              render={({ value, onChange }) => (
                <FormGroup
                  inputs={[
                    {
                      type: InputTypes.SINGLE_SELECT,
                      props: {
                        id: 'apiMethod',
                        label: 'Method',
                        options: methodsOptions,
                        isDisabled: isReadOnly,
                        value: value
                          ? methodsOptions.find((option) => option.value === value)
                          : null,
                        placeholder: 'Select',
                        onChange: (option: { value: string }) => {
                          onChange(option.value);
                        },
                      },
                    },
                  ]}
                />
              )}
            />
            <Controller
              control={control}
              name="apiEndpoint"
              key="apiEndpoint"
              shouldUnregister={false}
              rules={{
                required: true,
              }}
              render={({ value, onChange }) => (
                <Mentions
                  key="apiEndpoint"
                  label="URL"
                  mentionItems={mentionItems}
                  placeholder="Type @s for static data, @e for effects, @t for tasks and @p for parameters list"
                  onChange={(value) => {
                    onChange(value);
                  }}
                  {...(value ? { initialState: value } : {})}
                  isReadOnly={isReadOnly}
                />
              )}
            />
            <Controller
              control={control}
              name="apiHeader"
              key="apiHeader"
              shouldUnregister={false}
              rules={{
                required: false,
              }}
              render={({ value, onChange }) => {
                return (
                  <Mentions
                    key="apiPayload"
                    label="Api Header"
                    mentionItems={[]}
                    isMultiLine={true}
                    onChange={(value) => {
                      onChange(value);
                    }}
                    {...(value ? { initialState: value } : {})}
                    isReadOnly={isReadOnly}
                  />
                );
              }}
            />
          </>
        )}
        <Controller
          control={control}
          name="apiPayload"
          key="apiPayload"
          shouldUnregister={false}
          rules={{
            required: effectType === EffectType.MONGO_QUERY || effectType === EffectType.SQL_QUERY,
          }}
          render={({ value, onChange }) => (
            <Mentions
              key="apiPayload"
              label={
                effectType === EffectType.REST_API
                  ? 'API Payload'
                  : effectType === EffectType.SQL_QUERY
                  ? 'SQL Query'
                  : 'Mongo Query'
              }
              placeholder="Type @s for static data, @e for effects, @t for tasks and @p for parameters list"
              mentionItems={mentionItems}
              isMultiLine={true}
              onChange={(value) => {
                onChange(value);
              }}
              {...(value ? { initialState: value } : {})}
              isReadOnly={isReadOnly}
            />
          )}
        />
      </Wrapper>
    ),
    footerContent: (
      <>
        <Button variant="secondary" onClick={handleCloseDrawer}>
          Cancel
        </Button>
        {!isReadOnly && (
          <Button
            type="submit"
            disabled={!isValid || !isDirty}
            onClick={handleSubmit((data) => {
              createEffect(data);
            })}
          >
            Save
          </Button>
        )}
      </>
    ),
  });

  return StyledDrawer;
};

export default CreateEffectsDrawer;
