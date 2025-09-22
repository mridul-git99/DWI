import { Button, Select, Textarea, TextInput } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { useTypedSelector } from '#store';
import { MandatoryParameter, Parameter } from '#types';
import { apiGetParameters, apiSingleParameter } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes } from '#utils/globalTypes';
import { request } from '#utils/request';
import { Add, Clear, DragHandle } from '@material-ui/icons';
import { EquationNode, EquationParserError, parse } from 'equation-parser';
import {
  createResolverFunction,
  defaultFunctions,
  EquationResolveError,
  format,
  resolve,
  VariableLookup,
} from 'equation-resolver';
import { debounce } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { useFormContext } from 'react-hook-form';
import { CommonWrapper } from './styles';

const comparisons = [
  'equals',
  'less-than',
  'greater-than',
  'less-than-equals',
  'greater-than-equals',
  'approximates',
];

const getEquationError = (
  parsedEquation: (EquationNode | EquationParserError | EquationResolveError)[],
) => {
  if (parsedEquation && parsedEquation.length && parsedEquation[0].type.includes('error')) {
    switch ((parsedEquation[0] as any).errorType) {
      case 'operatorLast':
        return 'Invalid operator add to the end of the equation';
      case 'variableUnknown':
        return `Unknown variable ${(parsedEquation[0] as any).name}`;
      case 'invalidChar':
        return `Invalid character ${(parsedEquation[0] as any).character}`;
      default:
        return `Invalid Input`;
    }
  }
};

const math = (equationsArr: string[], defaultVariables: VariableLookup) => {
  const variables: VariableLookup = { ...defaultVariables };
  const functions = { ...defaultFunctions };
  const equations = equationsArr.map((input) => {
    const [inputEquation, inputUnit] = input.split(':');

    const node = parse(inputEquation);

    if (node.type === 'equals' && node.a.type === 'variable') {
      const value = resolve(node.b, { variables, functions });
      if (value.type !== 'resolve-error') {
        variables[node.a.name] = value;
      }
    } else if (
      node.type === 'equals' &&
      node.a.type === 'function' &&
      node.a.args.every((arg) => arg.type === 'variable')
    ) {
      const { name, args } = node.a;
      functions[name] = createResolverFunction(
        args.map((arg: any) => arg.name),
        node.b,
        { variables, functions },
      );
    }

    const formatted = comparisons.includes(node.type)
      ? node
      : format(node, inputUnit ? parse(inputUnit) : null, {
          variables,
          functions,
        });

    return formatted;
  });

  return equations;
};

const MapVariable: FC<{
  variableName: string;
  value: string;
  isReadOnly: boolean;
  onRemoveVariable: (variableName: string) => void;
  onParameterSelect: (option: any, variableName: string) => void;
  onVariableNameChange: (value: string, variableName: string) => void;
}> = ({
  variableName,
  value,
  isReadOnly,
  onRemoveVariable,
  onParameterSelect,
  onVariableNameChange,
}: any) => {
  const { id: checklistId } = useTypedSelector((state) => state.prototypeComposer.data!);
  const {
    parameters: { addParameter },
  } = useTypedSelector((state) => state.prototypeComposer);

  const [selectedParameter, setSelectedParameter] = useState<Parameter | null>(null);

  const urlParams = {
    page: DEFAULT_PAGE_NUMBER,
    size: DEFAULT_PAGE_SIZE,
    sort: 'createdAt,desc',
    filters: {
      op: FilterOperators.AND,
      fields: [
        { field: 'archived', op: FilterOperators.EQ, values: [false] },
        {
          field: 'type',
          op: FilterOperators.ANY,
          values: [
            MandatoryParameter.NUMBER,
            MandatoryParameter.CALCULATION,
            MandatoryParameter.SHOULD_BE,
          ],
        },
        ...(addParameter?.parameterId
          ? [
              {
                field: 'id',
                op: FilterOperators.NE,
                values: [addParameter.parameterId],
              },
            ]
          : []),
      ],
    },
  };

  const { list, reset, status, fetchNext } = createFetchList<Parameter>(
    apiGetParameters(checklistId),
    urlParams,
    false,
  );

  const fetchReferenceParameter = async (referencedParameterId: string) => {
    const { data } = await request('GET', apiSingleParameter(referencedParameterId));
    if (data) {
      setSelectedParameter(data);
    }
  };

  useEffect(() => {
    if (value?.parameterId) {
      fetchReferenceParameter(value.parameterId);
    }
  }, [value?.parameterId]);

  return (
    <li className="list-item" key={variableName}>
      <TextInput
        placeholder="X"
        label="Parameter Name"
        defaultValue={variableName === 'undefined' ? undefined : variableName}
        disabled={isReadOnly}
        onBlur={(e: any) => {
          onVariableNameChange(e.target.value, variableName);
        }}
      />
      <DragHandle style={{ marginInline: 16 }} />
      <div
        style={{
          flex: 1,
        }}
      >
        <label className="input-label">Select Parameter</label>
        <div
          style={{
            flex: 1,
            ...(variableName === 'undefined' && {
              background: '#f4f4f4',
            }),
          }}
        >
          <Select
            isDisabled={isReadOnly || variableName === 'undefined'}
            isLoading={status === 'loadingNext'}
            onMenuOpen={() => {
              if (checklistId) {
                reset({ params: { ...urlParams } });
              }
            }}
            onInputChange={debounce((searchedValue: string, actionMeta) => {
              if (searchedValue !== actionMeta.prevInputValue)
                reset({
                  params: {
                    ...urlParams,
                    filters: {
                      ...urlParams.filters,
                      fields: [
                        ...urlParams.filters.fields,
                        ...(searchedValue
                          ? [{ field: 'label', op: FilterOperators.LIKE, values: [searchedValue] }]
                          : []),
                      ],
                    },
                  },
                });
            }, 500)}
            options={list.map((parameter) => ({
              value: parameter.id,
              ...parameter,
            }))}
            value={
              value?.parameterId
                ? {
                    value: value.parameterId,
                    label: selectedParameter?.label,
                  }
                : undefined
            }
            onChange={(option: any) => {
              onParameterSelect(option, variableName);
            }}
            onMenuScrollToBottom={() => fetchNext()}
          />
        </div>
      </div>
      {!isReadOnly && (
        <Clear
          style={{ marginLeft: 16, cursor: 'pointer' }}
          onClick={() => {
            onRemoveVariable(variableName);
          }}
        />
      )}
    </li>
  );
};

const CalculationParameter: FC<{ isReadOnly: boolean }> = ({ isReadOnly }) => {
  const { register, watch, setValue, errors, setError, clearErrors } = useFormContext();

  register('data.variables');

  const variables = watch('data.variables', {});
  const expression = watch('data.expression', '');
  const precision = watch('data.precision', '9');

  const equations = (expression as string)
    .split(/\n/g)
    .map((s) => s.trim())
    .filter((s) => s);

  const defaultVariables = Object.keys(variables).reduce(
    (acc: VariableLookup, variableName: string) => {
      acc[variableName] = { type: 'number', value: 1 };
      return acc;
    },
    {},
  );
  const parsedEquations = math(equations, defaultVariables);
  const equationError = getEquationError(parsedEquations);

  useEffect(() => {
    if (equationError) {
      setError('data.expression', {
        message: equationError,
      });
    } else {
      clearErrors('data.expression');
    }
  }, [equationError, expression]);

  const onVariableNameChange = (value: string, variableName: string) => {
    const formValue = variables[variableName];
    delete variables[variableName];
    setValue(
      'data.variables',
      {
        ...variables,
        [value]: formValue,
      },
      {
        shouldDirty: true,
        shouldValidate: true,
      },
    );
  };

  const onParameterSelect = (option: any, variableName: string) => {
    setValue(
      'data.variables',
      {
        ...variables,
        [variableName]: {
          parameterId: option.id,
          taskId: option.taskId,
        },
      },
      {
        shouldDirty: true,
        shouldValidate: true,
      },
    );
  };

  const onRemoveVariable = (variableName: string) => {
    delete variables[variableName];
    setValue(
      'data.variables',
      {
        ...variables,
      },
      {
        shouldDirty: true,
        shouldValidate: true,
      },
    );
  };

  return (
    <CommonWrapper>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <TextInput
          name={`data.precision`}
          label="Enter Desired Precision"
          type={InputTypes.NUMBER}
          placeholder="Round off number value"
          ref={register({
            required: true,
          })}
          value={precision}
          tooltipLabel='The precision indicates the number of decimal places to which the number will be rounded. For instance, a precision of "2" will round to two decimal places.'
        />
        <TextInput
          name={`data.uom`}
          label="Unit of Measurement"
          disabled={isReadOnly}
          optional={true}
          ref={register}
        />
      </div>
      {Object.entries(variables).length > 0 && (
        <ul className="list" {...(isReadOnly && { style: { marginBottom: '16px' } })}>
          {Object.entries(variables).map(([variableName, value]: [string, any], index) => {
            return (
              <MapVariable
                key={index}
                variableName={variableName}
                value={value}
                isReadOnly={isReadOnly}
                onRemoveVariable={onRemoveVariable}
                onParameterSelect={onParameterSelect}
                onVariableNameChange={onVariableNameChange}
              />
            );
          })}
        </ul>
      )}
      {!isReadOnly && (
        <Button
          type="button"
          variant="textOnly"
          style={{ padding: '8px 0', marginBlock: 16 }}
          onClick={() => {
            setValue(
              'data.variables',
              {
                ...variables,
                ['undefined']: undefined,
              },
              {
                shouldDirty: true,
                shouldValidate: true,
              },
            );
          }}
          disabled={'undefined' in variables}
        >
          <Add /> Add Parameter
        </Button>
      )}
      <Textarea
        defaultValue={expression}
        name="data.expression"
        ref={register({
          required: true,
          pattern: {
            value: /^[^=]+$/,
            message: "Invalid operator '=' in the equation",
          },
        })}
        label="Write Calculation"
        rows={4}
        disabled={isReadOnly}
        error={equationError || errors?.data?.expression?.message}
      />
    </CommonWrapper>
  );
};

export default CalculationParameter;
