import { Button, Checkbox, FormGroup } from '#components';
import DropdownCheckboxOption from '#components/shared/DropdownCheckboxOption';
import { updateParameterApi } from '#PrototypeComposer/Activity/actions';
import { useTypedSelector } from '#store';
import {
  BranchingRule,
  MandatoryParameter,
  Parameter,
  ParameterType,
  TargetEntityType,
} from '#types';
import { validateNumber } from '#utils';
import { baseUrl } from '#utils/apiUrls';
import { FilterOperators, InputTypes, ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { Constraint } from '#views/Ontology/types';
import { getObjectData } from '#views/Ontology/utils';
import { AddCircleOutline, Close } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Controller, useFieldArray, useForm, useWatch } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import { components, InputActionMeta, OptionProps } from 'react-select';
import styled from 'styled-components';
import { v4 as uuidv4 } from 'uuid';

const RuleConfigurationWrapper = styled.form`
  display: flex;
  flex: 1;
  position: relative;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  width: 100%;

  .header {
    display: flex;
    padding-right: 16px;
    color: rgba(0, 0, 0, 0.87);
    font-size: 24px;
    align-items: center;
    .actions {
      display: flex;
      margin-left: auto;
    }
  }

  .rule-cards {
    overflow-y: auto;
    padding-right: 16px;
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 16px;
  }
`;

const RuleCardWrapper = styled.div`
  border: 1px solid #e0e0e0;
  padding-top: 16px;

  .upper-row {
    display: flex;
    align-items: center;
    padding-right: 16px;
    .remove-icon {
      cursor: pointer;
      margin-top: 20px;
      font-size: 16px;
    }
  }

  .form-group {
    flex: 1;
    flex-direction: row;
    gap: 16px;
    padding: 8px 16px;

    > div {
      margin-bottom: 16px;
    }
  }

  .nested-optgroup {
    padding: 12px 0px 0px 24px;
    :last-of-type {
      border-bottom: 1px solid #e0e0e0;
      padding-bottom: 8px;
    }
    .nested-optgroup-label {
      color: #333;
      margin-bottom: 4px;
    }
    .nested-optgroup-option {
      padding-left: 16px;
      > div {
        border-bottom: none;
        background-color: #fff;
        padding-block: 8px;
      }
    }
  }
`;

export const conditionByParameterType = (type: ParameterType) => {
  switch (type) {
    case MandatoryParameter.DATE_TIME:
      return {
        [Constraint.GTE]: 'is greater than equal to',
        [Constraint.LTE]: 'is less than equal to',
      };
    case MandatoryParameter.NUMBER:
    case MandatoryParameter.CALCULATION:
      return {
        [Constraint.EQ]: 'is equal to',
        [Constraint.NE]: 'is not equal to',
        [Constraint.LT]: 'is less than',
        [Constraint.GT]: 'is greater than',
        [Constraint.LTE]: 'is less than equal to',
        [Constraint.GTE]: 'is greater than equal to',
        [Constraint.BETWEEN]: 'is between',
      };
    default:
      return {
        [Constraint.EQ]: 'is equal to',
        [Constraint.NE]: 'is not equal to',
      };
  }
};

const renderNestedOption = (props: OptionProps<any>, label: string, nestedOptions: any[]) => {
  const { innerProps, selectOption, selectProps, setValue } = props;
  const selectedValues = nestedOptions.reduce((acc: any, v: any) => {
    if (selectProps.value?.some((_v: any) => _v.value === v.value)) {
      acc[v.value] = true;
    }
    return acc;
  }, {});
  const isOptionsChecked = Object.keys(selectedValues).length === nestedOptions.length;
  return (
    <div className="nested-optgroup">
      <div
        className="nested-optgroup-label"
        onClick={() => {
          if (isOptionsChecked) {
            setValue(
              selectProps.value.filter(
                (v: any) => !nestedOptions?.some((_v) => _v.value === v.value),
              ),
              'deselect-option',
            );
          } else {
            setValue([...selectProps.value, ...nestedOptions], 'select-option');
          }
        }}
      >
        <Checkbox preventDefault={true} label={label} checked={isOptionsChecked} />
      </div>
      {nestedOptions?.map((nestedOption) => {
        if (nestedOption.options?.length) {
          return renderNestedOption(props, nestedOption.label, nestedOption.options);
        }
        const isChecked = !!selectedValues?.[nestedOption.value];
        return (
          <div className="nested-optgroup-option" key={nestedOption.value}>
            <components.Option
              {...props}
              innerProps={{
                ...innerProps,
                onClick: () => selectOption(nestedOption),
              }}
            >
              <Checkbox preventDefault={true} label={nestedOption.label} checked={isChecked} />
            </components.Option>
          </div>
        );
      })}
    </div>
  );
};

const Option = (props: OptionProps<any>) => {
  const { data } = props;
  const nestedOptions = data.options;

  if (nestedOptions) {
    const label = data.label;
    return renderNestedOption(props, label, nestedOptions);
  }

  return <DropdownCheckboxOption {...props} />;
};

const RuleCard: FC<any> = ({
  item,
  index,
  remove,
  isReadOnly,
  parameter,
  hiddenParametersList,
  visibleParametersList,
  control,
}) => {
  const thenValue = useWatch<{ value: string }>({ name: `rules.${index}.thenValue`, control });
  const ruleConstraint = useWatch<string>({
    name: `rules.${index}.constraint`,
    control,
  });

  const [menuIsOpen, setMenuIsOpen] = useState<any>();
  const [state, setState] = useState<{
    isLoading: Boolean;
    options: any[];
    selectedOption: Record<string, string> | null;
  }>({
    isLoading: false,
    options: [],
    selectedOption: null,
  });
  const { options, isLoading, selectedOption } = state;
  const searchedValue = useRef<string>('');
  const [searchedParam, setSearchedParam] = useState('');
  const pagination = useRef({
    current: -1,
    isLast: true,
  });

  useEffect(() => {
    if (
      [
        MandatoryParameter.RESOURCE,
        MandatoryParameter.MULTI_RESOURCE,
        MandatoryParameter.SINGLE_SELECT,
      ].includes(parameter.type)
    ) {
      getOptions();
    }

    if (
      item?.input &&
      item.input?.length > 0 &&
      [MandatoryParameter.RESOURCE, MandatoryParameter.MULTI_RESOURCE]?.includes(parameter.type)
    ) {
      (async () => {
        const object = await getObjectData({
          id: item.input[0],
          collection: parameter.data.collection,
        });
        if (object) {
          setState((prev) => ({
            ...prev,
            selectedOption: {
              value: object.id,
              label: object.displayName,
              externalId: object.externalId,
            },
          }));
        }
      })();
    }
  }, []);

  const getOptions = async (reset = false) => {
    if (!parameter.data.length) {
      try {
        setState((prev) => ({ ...prev, isLoading: true }));
        const response: ResponseObj<any> = await request(
          'GET',
          `${baseUrl}${parameter.data.urlPath}`,
          {
            params: {
              page: reset ? 0 : pagination.current.current + 1,
              ...(searchedValue.current && {
                filters: {
                  op: FilterOperators.AND,
                  fields: [
                    {
                      field: 'displayName',
                      op: FilterOperators.LIKE,
                      values: [searchedValue.current],
                    },
                  ],
                },
              }),
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
          setState((prev) => ({
            ...prev,
            options: [
              ...(reset ? [] : prev.options),
              ...(response.data || []).map((option: any) => ({
                value: option.id,
                label: option.displayName,
                externalId: option.externalId,
              })),
            ],
            isLoading: false,
          }));
        }
      } catch (e) {
        setState((prev) => ({ ...prev, isLoading: false }));
      }
    } else if (!options.length) {
      setState((prev) => ({
        ...prev,
        options: parameter.data.map((i: any) => ({ label: i.name, value: i.id })),
      }));
    }
  };

  const onInputChange = (inputValue: string, { action, prevInputValue }: InputActionMeta) => {
    if (action === 'input-change') {
      setSearchedParam(inputValue);
      return inputValue;
    }
    if (action === 'menu-close') {
      if (prevInputValue) setMenuIsOpen(true);
      else setMenuIsOpen(undefined);
    }
    return prevInputValue;
  };

  const handleFilterOption = () => {
    return true;
  };

  const getOptionsAndSelectedValue = (list: any[], value: any) => {
    const selectedParameters: any[] = [];
    let options = [];
    if (thenValue?.value) {
      options = searchedParam ? [] : list;
      list.forEach((stage: any) => {
        const stageOptions: any[] = [];
        stage.options.forEach((task: any) => {
          const taskOptions: any[] = [];
          task.options.forEach((p: any) => {
            if (value?.includes(p.value)) {
              selectedParameters.push(p);
            }
            if (searchedParam && p.label.toLowerCase().includes(searchedParam.toLowerCase())) {
              taskOptions.push(p);
            }
          });
          if (searchedParam && taskOptions.length) {
            stageOptions.push({
              ...task,
              options: taskOptions,
            });
          }
        });
        if (searchedParam && stageOptions.length) {
          options.push({
            ...stage,
            options: stageOptions,
          });
        }
      });
    }
    return { selectedParameters, options };
  };

  const conditionOptions = useMemo(() => {
    return Object.entries(conditionByParameterType(parameter.type)).map(([value, label]) => ({
      value,
      label,
    }));
  }, [parameter.type]);

  return (
    <RuleCardWrapper>
      <input
        type="hidden"
        name={`rules.${index}.id`}
        ref={control.register()}
        defaultValue={item.id}
      />
      <div className="upper-row">
        <FormGroup
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'parameterId',
                label: 'IF',
                options: [],
                value: [
                  {
                    label: parameter.label,
                    value: parameter.id,
                  },
                ],
                isDisabled: true,
                menuPlacement: 'bottom',
                placeholder: 'Select Parameter',
                style: {
                  flex: 1,
                },
              },
            },
          ]}
        />
        {!isReadOnly && (
          <Close
            className="remove-icon"
            onClick={() => {
              remove(index);
            }}
          />
        )}
      </div>
      <div style={{ display: 'flex', flexDirection: 'row' }}>
        <Controller
          control={control}
          name={`rules.${index}.constraint`}
          rules={{
            required: true,
          }}
          defaultValue={
            [MandatoryParameter.SINGLE_SELECT, MandatoryParameter.RESOURCE].includes(parameter.type)
              ? Constraint.EQ
              : item.constraint || null
          }
          render={({ onChange, value }) => (
            <FormGroup
              inputs={[
                {
                  type: InputTypes.SINGLE_SELECT,
                  props: {
                    id: 'ruleCondition',
                    label: 'Condition',
                    options: conditionOptions,
                    value: value
                      ? [MandatoryParameter.NUMBER, MandatoryParameter.CALCULATION].includes(
                          parameter.type,
                        )
                        ? [
                            {
                              label: conditionByParameterType(parameter.type)[value],
                              value,
                            },
                          ]
                        : [
                            {
                              label: 'is equal to',
                              value: Constraint.EQ,
                            },
                          ]
                      : null,
                    isDisabled: [
                      MandatoryParameter.SINGLE_SELECT,
                      MandatoryParameter.RESOURCE,
                    ].includes(parameter.type),
                    placeholder: 'Select Condition',
                    onChange: (option: { label: string; value: string }) => {
                      onChange(option.value);
                    },
                    style: {
                      width: 200,
                    },
                  },
                },
              ]}
              style={{ flex: 'unset' }}
            />
          )}
        />
        {[MandatoryParameter.NUMBER, MandatoryParameter.CALCULATION].includes(parameter.type) ? (
          ruleConstraint && ruleConstraint === Constraint.BETWEEN ? (
            <>
              <Controller
                control={control}
                name={`rules.${index}.input.[0]`}
                rules={{
                  required: true,
                }}
                defaultValue={item.input?.[0] ?? null}
                render={({ onChange, value }) => {
                  return (
                    <FormGroup
                      inputs={[
                        {
                          type: InputTypes.NUMBER,
                          props: {
                            id: 'input',
                            label: 'Lower Value',
                            value: value,
                            isDisabled: isReadOnly,
                            placeholder: 'Enter Value',
                            onChange: (option: any) => {
                              const value = validateNumber(option?.value)
                                ? parseFloat(option.value)
                                : null;
                              onChange(value);
                            },
                            style: {
                              flex: 1,
                            },
                          },
                        },
                      ]}
                    />
                  );
                }}
              />
              <Controller
                control={control}
                name={`rules.${index}.input.[1]`}
                rules={{
                  required: true,
                }}
                defaultValue={item.input?.[1] ?? null}
                render={({ onChange, value }) => {
                  return (
                    <FormGroup
                      inputs={[
                        {
                          type: InputTypes.NUMBER,
                          props: {
                            id: 'input',
                            label: 'Upper Value',
                            value: value,
                            isDisabled: isReadOnly,
                            placeholder: 'Enter Value',
                            onChange: (option: any) => {
                              const value = validateNumber(option?.value)
                                ? parseFloat(option.value)
                                : null;
                              onChange(value);
                            },
                            style: {
                              flex: 1,
                            },
                          },
                        },
                      ]}
                    />
                  );
                }}
              />
            </>
          ) : (
            <Controller
              control={control}
              name={`rules.${index}.input`}
              rules={{
                required: true,
              }}
              defaultValue={item.input || null}
              render={({ onChange, value }) => {
                return (
                  <FormGroup
                    inputs={[
                      {
                        type: InputTypes.NUMBER,
                        props: {
                          id: 'input',
                          label: 'Value',
                          value: value,
                          isDisabled: isReadOnly,
                          placeholder: 'Enter Value',
                          onChange: (option: any) => {
                            const value = validateNumber(option?.value) ? [option.value] : null;
                            onChange(value);
                          },
                          style: {
                            flex: 1,
                          },
                        },
                      },
                    ]}
                  />
                );
              }}
            />
          )
        ) : (
          <Controller
            control={control}
            name={`rules.${index}.input`}
            rules={{
              required: true,
            }}
            defaultValue={item.input || null}
            render={({ onChange, value }) => {
              return (
                <FormGroup
                  inputs={[
                    {
                      type: InputTypes.SINGLE_SELECT,
                      props: {
                        id: 'input',
                        label: 'Value',
                        options: options,
                        value: options?.find((o: any) => o?.value === value?.[0]) ?? selectedOption,
                        isDisabled: isReadOnly,
                        placeholder: 'Select Value',
                        menuPlacement: 'bottom',
                        onChange: (option: any) => {
                          onChange([option.value]);
                        },
                        ...(!parameter.data.length && {
                          onMenuScrollToBottom: () => {
                            if (!isLoading && !pagination.current.isLast) {
                              getOptions();
                            }
                          },
                          isLoading,
                          onInputChange: debounce((searchedString: string, actionMeta) => {
                            if (searchedString !== actionMeta.prevInputValue) {
                              searchedValue.current = searchedString;
                              getOptions(true);
                            }
                          }, 500),
                        }),
                        style: {
                          flex: 1,
                        },
                      },
                    },
                  ]}
                />
              );
            }}
          />
        )}
      </div>
      <div style={{ display: 'flex', flexDirection: 'row' }}>
        <Controller
          control={control}
          name={`rules.${index}.thenValue`}
          rules={{
            required: true,
          }}
          defaultValue={item.thenValue || null}
          render={({ onChange, value }) => (
            <FormGroup
              inputs={[
                {
                  type: InputTypes.SINGLE_SELECT,
                  props: {
                    id: 'thenValue',
                    label: 'Then',
                    options: [
                      {
                        label: 'Show',
                        value: 'show',
                      },
                      {
                        label: 'Hide',
                        value: 'hide',
                      },
                    ],
                    value: [value],
                    isDisabled: isReadOnly,
                    onChange: (option: { label: string; value: string }) => {
                      onChange(option);
                    },
                    placeholder: 'Select',
                    style: {
                      width: 200,
                    },
                  },
                },
              ]}
              style={{ flex: 'unset' }}
            />
          )}
        />
        {thenValue?.value === 'show' ? (
          <Controller
            control={control}
            name={`rules.${index}.show.parameters`}
            rules={{
              required: true,
            }}
            shouldUnregister={true}
            key={`rules.${index}.show.parameters`}
            defaultValue={item.show?.parameters || null}
            render={({ onChange, value, name }) => {
              const { selectedParameters, options } = getOptionsAndSelectedValue(
                hiddenParametersList,
                value,
              );
              return (
                <FormGroup
                  style={{ width: 'calc(100% - 264px)' }}
                  inputs={[
                    {
                      type: InputTypes.MULTI_SELECT,
                      props: {
                        id: name,
                        name,
                        label: 'Show',
                        options,
                        value: selectedParameters,
                        isDisabled: isReadOnly,
                        filterOption: handleFilterOption,
                        placeholder: 'Select Parameters',
                        closeMenuOnSelect: false,
                        menuIsOpen: menuIsOpen,
                        onInputChange: onInputChange,
                        components: { Option },
                        menuPlacement: 'bottom',
                        onChange: (value: any[]) => {
                          onChange(
                            value.length > 0 && !value[0]?.options
                              ? value.map((v) => v.value)
                              : null,
                          );
                        },
                        style: {
                          flex: 1,
                          width: '100%',
                        },
                      },
                    },
                  ]}
                />
              );
            }}
          />
        ) : (
          <Controller
            control={control}
            name={`rules.${index}.hide.parameters`}
            rules={{
              required: true,
            }}
            shouldUnregister={true}
            key={`rules.${index}.hide.parameters`}
            defaultValue={item.hide?.parameters || null}
            render={({ onChange, value, name }) => {
              const { selectedParameters, options } = getOptionsAndSelectedValue(
                visibleParametersList,
                value,
              );
              return (
                <FormGroup
                  style={{ width: 'calc(100% - 264px)' }}
                  inputs={[
                    {
                      type: InputTypes.MULTI_SELECT,
                      props: {
                        id: name,
                        name,
                        label: 'Hide',
                        value: selectedParameters,
                        options,
                        isDisabled: isReadOnly,
                        filterOption: handleFilterOption,
                        placeholder: 'Select Parameters',
                        closeMenuOnSelect: false,
                        menuIsOpen: menuIsOpen,
                        onInputChange: onInputChange,
                        components: { Option },
                        menuPlacement: 'bottom',
                        onChange: (value: any[]) => {
                          onChange(
                            value.length > 0 && !value[0]?.options
                              ? value.map((v) => v.value)
                              : null,
                          );
                        },
                        style: {
                          flex: 1,
                          width: '100%',
                        },
                      },
                    },
                  ]}
                />
              );
            }}
          />
        )}
      </div>
    </RuleCardWrapper>
  );
};

const RuleConfiguration: FC<{ parameter: Parameter; isReadOnly: boolean }> = ({
  parameter,
  isReadOnly,
}) => {
  const dispatch = useDispatch();
  const {
    parameters: { listById, parameterOrderInTaskInStage },
    stages: { listOrder, listById: stagesById },
    tasks: { listById: tasksById, tasksOrderInStage },
    data,
  } = useTypedSelector((state) => state.prototypeComposer);
  const jobParameters = data?.parameters ?? [];
  const [hiddenParametersList, setHiddenParametersList] = useState<any[]>([]);
  const [visibleParametersList, setVisibleParametersList] = useState<any[]>([]);
  const form = useForm<{
    rules: BranchingRule[];
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
  });

  const {
    control,
    reset,
    getValues,
    formState: { isDirty, isValid },
  } = form;

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'rules',
    keyName: 'key',
  });

  useEffect(() => {
    const cjfHiddenListOptions: any[] = [];
    const cjfShowListOptions: any[] = [];

    if (parameter.targetEntityType === TargetEntityType.PROCESS) {
      const createJobOptionHide: any = {
        label: 'Create Job Form',
        options: [],
      };

      const createJobOptionShow: any = {
        label: 'Create Job Form',
        options: [],
      };

      jobParameters
        .filter((currParam) => currParam?.hidden && currParam?.id !== parameter?.id)
        .forEach((currParameter: any) => {
          const jobParameterOption: any = {
            label: currParameter.label,
            value: currParameter.id,
          };
          createJobOptionHide.options.push(jobParameterOption);
        });
      if (createJobOptionHide.options.length) {
        cjfHiddenListOptions.push({
          label: '',
          value: '',
          options: [createJobOptionHide],
        });
      }

      jobParameters
        .filter((currParam) => !currParam?.hidden && currParam?.id !== parameter?.id)
        .forEach((currParameter: any) => {
          const jobParameterOption: any = {
            label: currParameter.label,
            value: currParameter.id,
          };
          createJobOptionShow.options.push(jobParameterOption);
        });

      if (createJobOptionShow.options.length) {
        cjfShowListOptions.push({
          label: '',
          value: '',
          options: [createJobOptionShow],
        });
      }

      filterParameterOptions(cjfHiddenListOptions, cjfShowListOptions);
    } else {
      filterParameterOptions();
    }
    handleReset();
  }, [parameter.id]);

  const handleReset = () => {
    reset({
      rules: (parameter?.rules || []).map((rule) => ({
        ...rule,
        thenValue: rule.hide ? { label: 'Hide', value: 'hide' } : { label: 'Show', value: 'show' },
      })),
    });
  };

  const filterParameterOptions = useCallback(
    (hiddenParams: any[] = [], shownParams: any[] = []) => {
      const listOptions: any[] = [];
      const hiddenListOptions: any[] = [];
      const visibleListOptions: any[] = [];
      listOrder.forEach((stageId, stageIndex) => {
        const stage = stagesById[stageId];
        const stageNumber = stageIndex + 1;
        const stageOption: any = {
          label: `Stage ${stageNumber} : ${stage?.name}`,
          value: stage.id,
          options: [],
        };
        const hiddenStageOption: any = {
          label: `Stage ${stageNumber} : ${stage?.name}`,
          value: stage.id,
          options: [],
        };
        const visibleStageOption: any = {
          label: `Stage ${stageNumber} : ${stage?.name}`,
          value: stage.id,
          options: [],
        };
        tasksOrderInStage[stageId].forEach((taskId, taskIndex) => {
          const task = tasksById[taskId];
          const taskOption: any = {
            label: `Task ${stageNumber}.${taskIndex + 1} : ${task?.name}`,
            value: task.id,
            options: [],
          };
          const hiddenTaskOption: any = {
            label: `Task ${stageNumber}.${taskIndex + 1} : ${task?.name}`,
            value: task.id,
            options: [],
          };
          const visibleTaskOption: any = {
            label: `Task ${stageNumber}.${taskIndex + 1} : ${task?.name}`,
            value: task.id,
            options: [],
          };
          parameterOrderInTaskInStage[stageId][taskId].forEach((parameterId) => {
            if (parameterId !== parameter.id) {
              const p = listById[parameterId];
              taskOption.options.push({
                label: p.label,
                value: p.id,
              });
              if (p.hidden) {
                hiddenTaskOption.options.push({
                  label: p.label,
                  value: p.id,
                });
              } else {
                visibleTaskOption.options.push({
                  label: p.label,
                  value: p.id,
                });
              }
            }
          });
          if (taskOption.options.length) {
            stageOption.options.push(taskOption);
          }
          if (hiddenTaskOption.options.length) {
            hiddenStageOption.options.push(hiddenTaskOption);
          }
          if (visibleTaskOption.options.length) {
            visibleStageOption.options.push(visibleTaskOption);
          }
        });
        listOptions.push(stageOption);
        hiddenListOptions.push(hiddenStageOption);
        visibleListOptions.push(visibleStageOption);
      });

      setHiddenParametersList([...hiddenParams, ...hiddenListOptions]);
      setVisibleParametersList([, ...shownParams, ...visibleListOptions]);
    },
    [parameter.id],
  );

  const onSubmit = (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    e.stopPropagation();
    const data: { rules: BranchingRule[] } = getValues();
    const updatedRules = (data.rules || []).map((rule) => {
      return {
        ...rule,
      };
    });
    dispatch(
      updateParameterApi({
        ...parameter,
        rules: updatedRules.map((rule) => {
          delete rule.thenValue;
          return rule;
        }),
      }),
    );
    reset({ rules: data.rules });
  };

  const onAddNewRule = () => {
    append({
      id: uuidv4(),
    });
  };

  return (
    <RuleConfigurationWrapper>
      <div className="header">
        Rules for {parameter.label}
        {!isReadOnly && (
          <div className="actions">
            <Button disabled={!isDirty} variant="secondary" color="red" onClick={handleReset}>
              Cancel
            </Button>
            <Button onClick={onSubmit} disabled={!isDirty || !isValid}>
              Save
            </Button>
          </div>
        )}
      </div>
      <div className="rule-cards">
        {fields.map((item, index) => (
          <RuleCard
            key={item.key}
            item={item}
            index={index}
            remove={remove}
            isReadOnly={isReadOnly}
            parameter={parameter}
            hiddenParametersList={hiddenParametersList}
            visibleParametersList={visibleParametersList}
            control={control}
          />
        ))}
        {!isReadOnly && (
          <Button
            type="button"
            variant="secondary"
            style={{ marginBottom: 16, padding: '6px 8px' }}
            onClick={onAddNewRule}
          >
            <AddCircleOutline style={{ marginRight: 8 }} /> Add
          </Button>
        )}
      </div>
    </RuleConfigurationWrapper>
  );
};

export default RuleConfiguration;
