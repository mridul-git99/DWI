import PadLockIcon from '#assets/svg/padlock.svg';
import { Button, FormGroup, TabContentProps } from '#components';
import checkPermission from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { StoreParameter } from '#types';
import { baseUrl } from '#utils/apiUrls';
import { nonEmptyStringRegex } from '#utils/constants';
import { InputTypes } from '#utils/globalTypes';
import { request } from '#utils/request';
import { navigate } from '@reach/router';
import { debounce, get, merge, isArray } from 'lodash';
import React, { FC, memo, useEffect, useRef, useState } from 'react';
import { Controller, RegisterOptions, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { FlagPositions, getBooleanFromDecimal } from '../ObjectTypes';
import { createObject, editObject } from '../actions';
import {
  Cardinality,
  CommonFields,
  Constraint,
  ObjectTypeProperty,
  ObjectTypeRelation,
  Validation,
} from '../types';
import { PropertyFlags } from '../utils';

const ObjectFormWrapper = styled.div`
  background-color: #fff;
  height: 100%;
  display: flex;
  flex: 1;
  padding: 24px 16px;

  form {
    display: flex;
    flex: 1;
    flex-direction: column;

    .form-group {
      justify-content: space-between;
    }

    .error-container {
      color: #cc5656;
    }

    .actions {
      padding: 16px 0;
      display: flex;
      flex-direction: row-reverse;
      gap: 16px;
      button {
        margin-right: 0;
      }
    }
    .automation-label-wrapper {
      display: flex;
      flex-direction: row;
      gap: 8px;
    }
  }
`;

const getValidations = (validations?: Validation[]) => {
  if (!validations?.length) return null;
  const validators: RegisterOptions = {
    validate: {},
  };
  validations.forEach((validation) => {
    switch (validation.constraint) {
      case Constraint.GT:
        validators.validate = {
          ...validators.validate,
          [validation.constraint]: (value: string) =>
            (value && parseInt(value) > parseInt(validation.value)) || validation.errorMessage,
        };
        break;
      case Constraint.LT:
        validators.validate = {
          ...validators.validate,
          [validation.constraint]: (value: string) =>
            (value && parseInt(value) < parseInt(validation.value)) || validation.errorMessage,
        };
        break;
      case Constraint.GTE:
        validators.validate = {
          ...validators.validate,
          [validation.constraint]: (value: string) =>
            (value && parseInt(value) >= parseInt(validation.value)) || validation.errorMessage,
        };
        break;
      case Constraint.LTE:
        validators.validate = {
          ...validators.validate,
          [validation.constraint]: (value: string) =>
            (value && parseInt(value) <= parseInt(validation.value)) || validation.errorMessage,
        };
        break;
      case Constraint.MIN:
        validators.validate = {
          ...validators.validate,
          [validation.constraint]: (value: string | []) =>
            (value && value.length >= parseInt(validation.value)) || validation.errorMessage,
        };
        break;
      case Constraint.MAX:
        validators.validate = {
          ...validators.validate,
          [validation.constraint]: (value: string | []) =>
            (value && value.length <= parseInt(validation.value)) || validation.errorMessage,
        };
        break;
    }
  });
  return validators;
};

const dropdownOptionsHandler = (options: any, isProperty: boolean) => {
  if (!options) {
    return null;
  }
  if (Array.isArray(options)) {
    if (options.length === 0) {
      return null;
    }
    return options.map((option) => (isProperty ? option.value : option));
  }
  return isProperty ? [options.value] : [options];
};

const getDisplayName = ({
  displayName,
  isAutomationModalView,
  isSelectProperty,
  isParameterLocked,
}: {
  displayName: string;
  isAutomationModalView: boolean;
  isSelectProperty: boolean;
  isParameterLocked: boolean;
}) => {
  if (isAutomationModalView && !isSelectProperty && isParameterLocked) {
    return (
      <div className="automation-label-wrapper">
        <div>{displayName}</div>
        <img src={PadLockIcon} alt="property-field-locked" style={{ marginRight: 8 }} />
      </div>
    );
  } else {
    return displayName;
  }
};

const RelationField = memo<{
  relation: ObjectTypeRelation;
  isReadOnly: boolean;
  control: any;
  isAutomationModalView: boolean;
  automationConfiguredParameter?: StoreParameter | null;
  relationConfiguration?: any;
}>(
  ({
    relation,
    isReadOnly,
    control,
    isAutomationModalView,
    automationConfiguredParameter,
    relationConfiguration,
  }) => {
    const {
      objects: { active: selectedObject },
    } = useTypedSelector((state) => state.ontology);

    const [selectOptions, setSelectOptions] = useState<{
      isFetching: boolean;
      options: CommonFields[];
      isOpen: Boolean;
      searchValue: string;
    }>({
      isFetching: false,
      options: [],
      isOpen: false,
      searchValue: '',
    });

    const { isOpen, searchValue } = selectOptions;

    const pagination = useRef({
      current: -1,
      isLast: true,
    });

    const relationHasDefaultValue = relationConfiguration?.defaultValue;

    const shouldDisableField = isAutomationModalView
      ? !!automationConfiguredParameter || isReadOnly || !!relationHasDefaultValue
      : isReadOnly;
    const { setValue } = control;
    const registrationId = `relations.${relation?.id}`;

    let defaultValue:
      | {
          collection: string;
          id: string;
          displayName: string;
          label: string;
          value: string;
          externalId: string;
        }[]
      | undefined;

    const formatExternalId = (item: any): any => ({
      ...item,
      externalId: `(ID: ${item.externalId})`,
    });

    if (isAutomationModalView && automationConfiguredParameter) {
      const _defaultValue = automationConfiguredParameter?.choices?.map((value) => ({
        collection: value.collection,
        id: value.objectId,
        displayName: value.objectDisplayName,
        label: value.objectDisplayName,
        value: value.objectId,
        externalId: `(ID: ${value.objectExternalId})`,
      }));
      if (_defaultValue && _defaultValue.length > 0) {
        defaultValue = _defaultValue;
      } else if (relationHasDefaultValue) {
        defaultValue = isArray(relationHasDefaultValue)
          ? relationHasDefaultValue.map(formatExternalId)
          : [formatExternalId(relationHasDefaultValue)];
      }
    } else if (relationHasDefaultValue) {
      defaultValue = isArray(relationHasDefaultValue)
        ? relationHasDefaultValue.map(formatExternalId)
        : [formatExternalId(relationHasDefaultValue)];
    } else {
      defaultValue = selectedObject?.relations
        .find((_relation) => _relation.id === relation.id)
        ?.targets?.map((value) => ({
          ...value,
          label: value.displayName,
          value: value.id,
          externalId: `(ID: ${value.externalId})`,
        }));
    }

    const getOptions = async (path: string, page: number, searchValue: string) => {
      try {
        setSelectOptions((prev) => ({
          ...prev,
          isFetching: true,
        }));
        if (path) {
          let urlString = `${baseUrl}${path}&page=${page}`;
          urlString = urlString + (searchValue ? `&query=${searchValue}` : '');
          const response: {
            data: CommonFields[];
            errors: { message: string }[];
            pageable: any;
          } = await request('GET', urlString);
          if (response.pageable) {
            pagination.current = {
              current: response.pageable?.page,
              isLast: response.pageable?.last,
            };
          }
          if (response.data) {
            setSelectOptions((prev) => ({
              ...prev,
              isFetching: false,
              options:
                pagination.current.current === 0
                  ? response.data
                  : [...prev.options, ...response.data],
            }));
          }
        }
      } catch (e) {
        console.error(`Error in Fetching Options for ${relation.id}`, e);
      } finally {
        setSelectOptions((prev) => ({
          ...prev,
          isFetching: false,
        }));
      }
    };

    useEffect(() => {
      if (!isReadOnly && isOpen) {
        getOptions(relation.target.urlPath, 0, searchValue);
      }
    }, [isOpen, searchValue]);

    useEffect(() => {
      if (isAutomationModalView && (automationConfiguredParameter || relationHasDefaultValue)) {
        setValue(registrationId, defaultValue, { shouldDirty: true, shouldValidate: true });
      }
    }, [automationConfiguredParameter, relationHasDefaultValue]);

    const handleMenuScrollToBottom = () => {
      if (!selectOptions.isFetching && !pagination.current.isLast) {
        getOptions(relation.target.urlPath, pagination.current.current + 1, searchValue);
      }
    };

    return (
      <Controller
        name={registrationId}
        control={control}
        key={registrationId}
        rules={{
          required: !(relation?.flags === 0),
        }}
        defaultValue={defaultValue || null}
        render={({ onChange, value, name }) => {
          return (
            <FormGroup
              style={{ padding: 'unset', marginBottom: 24 }}
              inputs={[
                {
                  type: InputTypes.MULTI_SELECT,
                  props: {
                    value,
                    name,
                    id: name,
                    isMulti: relation.target.cardinality === Cardinality.ONE_TO_MANY,
                    placeholder: `Select ${relation.displayName}`,
                    label: getDisplayName({
                      displayName: relation.displayName,
                      isAutomationModalView,
                      isSelectProperty: false,
                      isParameterLocked:
                        !!automationConfiguredParameter || !!relationHasDefaultValue,
                    }),
                    isDisabled: shouldDisableField,
                    optional: relation?.flags === 0,
                    ...(isReadOnly && { styles: undefined }),
                    options: selectOptions?.options?.map((option) => ({
                      ...option,
                      value: option?.id,
                      label: option.displayName,
                      externalId: `(ID: ${option.externalId})`,
                    })),
                    onMenuOpen: () => {
                      setSelectOptions((prev) => ({ ...prev, isOpen: true }));
                    },
                    onMenuClose: () => {
                      setSelectOptions((prev) => ({ ...prev, isOpen: false }));
                    },
                    onInputChange: debounce((value, actionMeta) => {
                      if (value !== actionMeta.prevInputValue) {
                        setSelectOptions((prev) => ({ ...prev, searchValue: value }));
                      }
                    }, 500),
                    onMenuScrollToBottom: handleMenuScrollToBottom,
                    onChange: (options: any) => {
                      const selectedOption = dropdownOptionsHandler(options, false);
                      onChange(selectedOption);
                    },
                  },
                },
              ]}
            />
          );
        }}
      />
    );
  },
);

const PropertyField = memo<{
  property: ObjectTypeProperty;
  isReadOnly: boolean;
  control: any;
  errors: Record<string, string>;
  isAutomationModalView: boolean;
  automationConfiguredParameter?: StoreParameter | null;
  propertyConfiguration?: any;
  isEditing: boolean;
}>(
  ({
    property,
    isReadOnly,
    control,
    errors,
    isAutomationModalView,
    automationConfiguredParameter,
    propertyConfiguration,
    isEditing,
  }) => {
    const {
      objects: { active: selectedObject },
    } = useTypedSelector((state) => state.ontology);
    const propertyDefaultValue = propertyConfiguration?.defaultValue;
    const { flags, validations, inputType, displayName, options } = property;
    const registrationId = `properties.${property?.id}`;
    const isRequired = getBooleanFromDecimal(flags, FlagPositions.MANDATORY);
    const isAutoGenerated = getBooleanFromDecimal(flags, FlagPositions.AUTOGENERATE);
    const isExternalId = flags === PropertyFlags.EXTERNAL_ID;
    const validators = getValidations(validations);
    const errorsValues = get(errors, registrationId);
    const shouldDisableField = isAutomationModalView
      ? !!automationConfiguredParameter || isReadOnly || !!propertyDefaultValue
      : isReadOnly;
    const { setValue } = control;
    let defaultValue:
      | string
      | number
      | {
          label: string;
          value: string;
        }[]
      | undefined;

    if (isAutomationModalView && automationConfiguredParameter) {
      const _defaultValue = [InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(inputType)
        ? Object.entries(automationConfiguredParameter?.choices || {}).reduce(
            (acc, [key, value]) => {
              value === 'SELECTED' && acc.push(key);
              return acc;
            },
            [],
          )
        : automationConfiguredParameter?.value;

      if (_defaultValue) {
        defaultValue = _defaultValue;
      } else if (propertyDefaultValue) {
        defaultValue =
          inputType === InputTypes.SINGLE_SELECT ? [propertyDefaultValue] : propertyDefaultValue;
      }
    } else if (propertyDefaultValue) {
      defaultValue =
        InputTypes.SINGLE_SELECT === inputType ? [propertyDefaultValue] : propertyDefaultValue;
    } else {
      defaultValue = [InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(inputType)
        ? selectedObject?.properties
            .find((_property) => _property.id === property.id)
            ?.choices?.map((value) => ({
              label: value.displayName,
              value: value.id,
            }))
        : selectedObject?.properties.find((_property) => _property.id === property.id)?.value || '';
    }

    useEffect(() => {
      if (
        (isAutomationModalView && automationConfiguredParameter) ||
        (isAutomationModalView && !!propertyDefaultValue)
      ) {
        setValue(registrationId, defaultValue, { shouldDirty: true, shouldValidate: true });
      }
    }, [automationConfiguredParameter]);

    return (
      <Controller
        name={registrationId}
        control={control}
        key={registrationId}
        rules={{
          required: isAutoGenerated ? false : isRequired,
          ...(validators && validators),
          ...(![InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(inputType) && {
            pattern: /.*\S+.*/,
          }),
        }}
        defaultValue={defaultValue}
        render={({ onChange, value, name }) => {
          return (
            <FormGroup
              style={{ padding: 'unset', marginBottom: 24 }}
              inputs={[
                ...([InputTypes.SINGLE_SELECT, InputTypes.MULTI_SELECT].includes(inputType)
                  ? [
                      {
                        type: inputType,
                        props: {
                          defaultValue:
                            isAutomationModalView && automationConfiguredParameter?.data
                              ? value?.map((id) => {
                                  const data = automationConfiguredParameter.data.find(
                                    (item) => item.id === id,
                                  );
                                  return {
                                    label: data.name,
                                    value: data.id,
                                    id: data.id,
                                  };
                                })
                              : propertyDefaultValue
                              ? value?.map((id) => {
                                  const data = property?.options?.find((item) => item.id === id);
                                  return {
                                    label: data.displayName,
                                    value: data.id,
                                    id: data.id,
                                  };
                                })
                              : value,
                          placeholder: `Select ${displayName}`,
                          label: getDisplayName({
                            displayName,
                            isAutomationModalView,
                            isSelectProperty: false,
                            isParameterLocked:
                              !!automationConfiguredParameter || !!propertyDefaultValue,
                          }),
                          id: name,
                          name,
                          options: options?.map((option) => ({
                            label: option.displayName,
                            value: option.id,
                          })),
                          optional: !isRequired,
                          isDisabled: shouldDisableField,
                          ...(isReadOnly && { styles: undefined }),
                          onChange: (_options: { value: string } | { value: string }[]) => {
                            const selectedOption = dropdownOptionsHandler(_options, true);
                            onChange(selectedOption);
                          },
                        },
                      },
                    ]
                  : [
                      {
                        type: inputType,
                        props: {
                          defaultValue: value,
                          placeholder: isAutoGenerated ? 'Auto Generated' : `Enter ${displayName}`,
                          label: getDisplayName({
                            displayName,
                            isAutomationModalView,
                            isSelectProperty: false,
                            isParameterLocked:
                              !!automationConfiguredParameter || !!propertyDefaultValue,
                          }),
                          id: name,
                          name,
                          optional: !isRequired,
                          disabled:
                            shouldDisableField || isAutoGenerated || (isEditing && isExternalId),
                          ...(inputType === InputTypes.MULTI_LINE && {
                            rows: 3,
                          }),
                          onChange: ({ value }: { name: string; value: string }) => {
                            const trimmedValue = value.toString().trim();
                            onChange(trimmedValue);
                          },
                        },
                      },
                    ]),
                ...(validators && errorsValues
                  ? [
                      {
                        type: InputTypes.ERROR_CONTAINER,
                        props: {
                          id: 'object-form-errors',
                          messages: errorsValues?.types,
                        },
                      },
                    ]
                  : []),
              ]}
            />
          );
        }}
      />
    );
  },
);

const ObjectView: FC<TabContentProps> = ({
  label,
  values: {
    objectTypeId,
    id,
    readOnly = false,
    onCancel = () => navigate(-1),
    onDone = () => navigate(-1),
    createObjectAutomationDetail,
    allRefParams,
  },
}) => {
  const dispatch = useDispatch();
  const {
    objects: { active: selectedObject },
    objectTypes: { active: selectedObjectType },
  } = useTypedSelector((state) => state.ontology);

  const {
    handleSubmit,
    formState: { isDirty, isValid, errors, dirtyFields },
    register,
    control,
  } = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
  });
  const dateTimeInputs = useRef<Record<string, string>>({});
  const isArchived = selectedObject?.usageStatus === 7;
  const isEditing = id && id !== 'new';
  const isAutomationModalView = !!label;
  const isReadOnly =
    readOnly ||
    !checkPermission([
      'ontology',
      isAutomationModalView ? 'createObjectByAutomation' : 'editObject',
    ]) ||
    !id ||
    isArchived;

  const showReasonField = !isReadOnly && !label && ((isEditing && isDirty) || id === 'new');

  const parseData = (data: Record<string, Record<string, string>>) => {
    return merge(
      {
        ...data,
        ...(data?.relations &&
          Object.entries<string>(data?.relations)?.reduce<
            Record<string, Record<string, string | null>>
          >(
            (acc, [key, value]) => {
              const relation = selectedObjectType?.relations.find(
                (relation) => relation.id === key,
              );
              if (relation) {
                acc['relations'][relation?.id] = value ? value : null;
              }
              return acc;
            },
            {
              relations: {},
            },
          )),
      },
      dateTimeInputs.current,
    );
  };

  const onSubmit = (data: Record<string, Record<string, string>>) => {
    if (objectTypeId) {
      if (isEditing) {
        const editedData = Object.keys(data)?.reduce<Record<string, Record<string, string>>>(
          (acc, key) => {
            acc[key] = {};
            return acc;
          },
          {},
        );
        Object.entries(dirtyFields).forEach(([key, value]) => {
          if (key === 'reason') {
            editedData[key] = data.reason;
          } else {
            editedData[key] = {};
            Object.keys(value).forEach((_key) => {
              if (key === 'relations') {
                editedData[key] = { ...data.relations };
              } else {
                editedData['relations'] = { ...data.relations };
              }
              editedData[key][_key] = get(data, [key, _key]);
            });
          }
        });
        const parsedData = parseData(editedData);
        dispatch(editObject(parsedData, objectTypeId, id, onDone));
      } else {
        const parsedData = parseData(data);
        if (isAutomationModalView) {
          onDone({ objectTypeId, ...parsedData });
        } else {
          dispatch(createObject(parsedData, objectTypeId, onDone));
        }
      }
    }
  };

  if (!selectedObjectType) return null;

  return (
    <ObjectFormWrapper>
      <form onSubmit={handleSubmit(onSubmit)}>
        {selectedObjectType?.properties
          ?.filter((property: ObjectTypeProperty) => property.flags !== 1)
          ?.map((property: ObjectTypeProperty) => {
            let parameter = null;
            let propertyConfiguration = null;
            if (isAutomationModalView) {
              propertyConfiguration = (
                createObjectAutomationDetail?.actionDetails?.configuration || []
              ).find((curr) => curr?.propertyId === property.id);
              parameter = propertyConfiguration
                ? allRefParams?.[propertyConfiguration.parameterId]
                : null;
            }
            return (
              <PropertyField
                key={property.id}
                property={property}
                isReadOnly={isReadOnly}
                control={control}
                errors={errors}
                isAutomationModalView={isAutomationModalView}
                automationConfiguredParameter={parameter}
                propertyConfiguration={propertyConfiguration}
                isEditing={isEditing}
              />
            );
          })}
        {(selectedObjectType?.relations ?? [])?.map((relation) => {
          let parameter = null;
          let relationConfiguration = null;
          if (isAutomationModalView) {
            relationConfiguration = (
              createObjectAutomationDetail?.actionDetails?.configuration || []
            ).find((curr) => curr?.relationId === relation.id);
            parameter = relationConfiguration
              ? allRefParams?.[relationConfiguration.parameterId]
              : null;
          }
          return (
            <RelationField
              key={relation.id}
              relation={relation}
              isReadOnly={isReadOnly}
              control={control}
              isAutomationModalView={isAutomationModalView}
              automationConfiguredParameter={parameter}
              relationConfiguration={relationConfiguration}
            />
          );
        })}
        {showReasonField && (
          <FormGroup
            style={{ padding: 'unset', marginBottom: 24 }}
            inputs={[
              {
                type: InputTypes.MULTI_LINE,
                props: {
                  id: 'reason',
                  name: 'reason',
                  label: 'Provide Reason',
                  placeholder: 'Users will write their comments here',
                  rows: 4,
                  ref: register({
                    required: true,
                    pattern: nonEmptyStringRegex,
                  }),
                  disabled: isReadOnly,
                },
              },
            ]}
          />
        )}
        <div className="actions">
          {!isReadOnly && (
            <Button type="submit" disabled={!isDirty || !isValid}>
              {isEditing ? 'Update' : 'Create'}
            </Button>
          )}
          <Button
            variant="secondary"
            onClick={() => {
              onCancel();
            }}
          >
            Cancel
          </Button>
        </div>
      </form>
    </ObjectFormWrapper>
  );
};

export default ObjectView;
