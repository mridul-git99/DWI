import InfoIcon from '#assets/svg/info.svg';
import { Button, FormGroup } from '#components';
import Tooltip from '#components/shared/Tooltip';
import { useTypedSelector } from '#store';
import { Parameter } from '#types';
import { apiGetObjectTypes } from '#utils/apiUrls';
import { InputTypes, ResponseObj, SelectorOptionsEnum } from '#utils/globalTypes';
import { request } from '#utils/request';
import { ObjectType } from '#views/Ontology/types';
import { AddCircleOutline } from '@material-ui/icons';
import { debounce, keyBy } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import { useFormContext } from 'react-hook-form';
import styled from 'styled-components';
import { v4 as uuidv4 } from 'uuid';
import ResourceFilterRow from './ResourceFilterRow';

export const FilterWrapper = styled.div`
  .filters-constraint {
    max-width: 140px;
    > div {
      margin-bottom: 0 !important;
    }
  }
  .filters {
    padding-bottom: 24px;

    .add-button {
      padding: 6px 8px;
    }

    .filter {
      margin-bottom: 8px;
      display: flex;
      flex-direction: column;
      gap: 12px;

      :last-of-type {
        margin-bottom: 0;
      }

      .filter-header {
        background-color: #f4f4f4;
        padding: 12px;
        > div {
          font-size: 14px;
          font-weight: 700;
        }
      }

      .filter-text {
        font-size: 14px;
      }

      .upper-row {
        display: flex;
        align-items: center;
        gap: 16px;
        :before {
          content: '';
          display: inline-block;
          height: 1px;
          position: absolute;
          vertical-align: middle;
          width: 18px;
          left: 0;
          border-top: 1px dotted #6f6f6f;
        }

        .remove-icon {
          cursor: pointer;
          margin-top: 6px;
          font-size: 16px;
        }
      }
    }
  }
  .form-group {
    flex: 1;
    flex-direction: row;
    gap: 16px;

    > div {
      flex: 1;
      margin-bottom: 16px;
    }

    > div:last-child {
      margin-bottom: 16px;
    }
  }

  .custom-select__menu {
    z-index: 2;
  }

  .filters-disabled {
    display: flex;
    flex-direction: row;
    gap: 10px;
  }
`;

type ResourceFilterState = {
  isActiveLoading: boolean;
  selectedObjectType?: ObjectType;
};

const ResourceFilter: FC<{
  isReadOnly: boolean;
  parameter?: Parameter;
  checklistId?: string;
  isVariationView?: boolean;
  stepperCount?: number;
}> = ({ isReadOnly, parameter, checklistId, isVariationView = false, stepperCount }) => {
  const { watch, setValue, clearErrors, setError } = useFormContext();
  const data = watch('data', {
    propertyFilters: { op: '', fields: [] },
  });
  const autoInitialized = watch('autoInitialized', false);

  const { propertyFilters = {} } = data;
  const { op = '', fields = [] } = propertyFilters || {};

  const updateFilters = (updatedFilters: any) => {
    const isFilter = updatedFilters?.fields?.length;
    const isOp = updatedFilters?.op;
    let isValid = true;
    if (updatedFilters?.fields?.length) {
      if (!updatedFilters?.op) {
        isValid = false;
      } else {
        updatedFilters.fields.every((filter: any) => {
          if (!filter) return true;
          let keyToValidate = ['field', 'op'];

          if ([InputTypes.DATE, InputTypes.DATE_TIME].includes(filter?.propertyType)) {
            keyToValidate.push('dateUnit');
          }

          if (filter?.selector === SelectorOptionsEnum.CONSTANT) {
            keyToValidate.push('values');
          } else if (filter?.selector === SelectorOptionsEnum.PARAMETER) {
            keyToValidate = [...keyToValidate, 'referencedParameterId'];
          } else {
            keyToValidate = [...keyToValidate, 'values'];
          }
          keyToValidate.every((key) => {
            const checkSingleProperty = !!filter?.[key]?.length;
            if (!checkSingleProperty) {
              isValid = false;
            }
            return isValid;
          });
          return isValid;
        });
      }
    } else if (updatedFilters?.op) {
      isValid = false;
    }
    setValue(
      'data',
      {
        ...data,
        propertyFilters:
          isOp || isFilter
            ? {
                ...(isOp && { op: updatedFilters.op }),
                ...(isFilter && { fields: updatedFilters.fields }),
              }
            : null,
      },
      {
        shouldDirty: true,
      },
    );
    if (!isValid) {
      setError('data', {
        stepperCount: stepperCount,
        message: 'All Filters Options Should be Filled.',
      });
    } else {
      clearErrors('data');
    }
  };

  useEffect(() => {
    updateFilters(propertyFilters);
    return () => {
      clearErrors('data');
    };
  }, []);

  return (
    <FilterWrapper>
      <FormGroup
        className="filters-constraint"
        inputs={[
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'filterConstraint',
              options: [
                {
                  label: 'AND',
                  value: 'AND',
                },
                // {
                //   label: 'OR',
                //   value: 'OR',
                // },
              ],
              isDisabled: isReadOnly || autoInitialized,
              isClearable: true,
              defaultValue: op
                ? [
                    {
                      label: op,
                      value: op,
                    },
                  ]
                : null,
              placeholder: 'Select One',
              onChange: (value: any) => {
                const _propertyFilters = {
                  ...data.propertyFilters,
                  op: value?.value ?? null,
                };
                updateFilters(_propertyFilters);
              },
            },
          },
        ]}
      />
      <div className="filters">
        <ResourceFormCard
          parameter={parameter}
          isReadOnly={isReadOnly}
          updateFilters={updateFilters}
          checklistId={checklistId}
          isVariationView={isVariationView}
        />
        <div className="filters-disabled">
          {!isReadOnly && !isVariationView && (
            <Button
              type="button"
              variant="secondary"
              className="add-button"
              disabled={autoInitialized}
              onClick={() => {
                fields[fields.length] = {
                  key: uuidv4(),
                  id: uuidv4(),
                };

                const _propertyFilters = {
                  ...data.propertyFilters,
                  fields,
                };

                updateFilters(_propertyFilters);
              }}
            >
              <AddCircleOutline style={{ marginRight: 8 }} /> Add
            </Button>
          )}
          {!isReadOnly && autoInitialized && (
            <Tooltip
              title={'Filter disabled for Linked parameters. Remove Linking to enable Filter.'}
              arrow
              textAlignment="left"
            >
              <img src={InfoIcon} alt="parameter-info" style={{ marginRight: 8 }} />
            </Tooltip>
          )}
        </div>
      </div>
    </FilterWrapper>
  );
};

const ResourceFormCard: FC<{
  isReadOnly: boolean;
  isVariationView: boolean;
  parameter?: Parameter;
  updateFilters: (updatedFilters: any) => void;
  checklistId?: string;
}> = ({ parameter, isReadOnly, updateFilters, checklistId, isVariationView = false }) => {
  const [state, setState] = useState<ResourceFilterState>({
    selectedObjectType: undefined,
    isActiveLoading: false,
  });

  const debounceInputRef = useRef(debounce((event, functor) => functor(event), 500));

  const { selectedObjectType, isActiveLoading } = state;

  const { watch } = useFormContext();

  const data = watch('data', {
    propertyFilters: { op: '', fields: [] },
  });

  const propertiesMap = useRef<Record<string, any>>({});

  const { propertyFilters = {} } = data;
  const { fields = [] } = propertyFilters || {};

  if (!checklistId) {
    const { data: processData } = useTypedSelector((state) => state.prototypeComposer);
    checklistId = processData?.id;
  }

  const handleConstantValue = (updatedFilters: any) => {
    debounceInputRef.current(updatedFilters, (updatedFilters: any) => {
      updateFilters(updatedFilters);
    });
  };

  const fetchObjectType = async (id: string) => {
    setState((prev) => ({ ...prev, isActiveLoading: true }));
    const res: ResponseObj<ObjectType> = await request('GET', apiGetObjectTypes(id));
    if (res?.data) {
      propertiesMap.current = keyBy(
        [...(res?.data?.properties || []), ...(res?.data?.relations || [])] || [],
        'id',
      );
    }
    setState((prev) => ({ ...prev, isActiveLoading: false, selectedObjectType: res?.data }));
  };

  const fieldKeysCorrection = (index: number) => {
    const updatedFields = {} as Record<string, any>;
    const keysToInclude = ['field', 'op', 'id', 'fieldType', 'propertyType'];
    Object.keys(fields[index]).forEach((key) => {
      if (keysToInclude.includes(key)) {
        updatedFields[key] = fields[index][key];
      }
    });
    fields[index] = updatedFields;
  };

  useEffect(() => {
    if (data?.objectTypeId) {
      fetchObjectType(data.objectTypeId);
    }
  }, [data?.objectTypeId]);

  return (fields || []).map((item: any, index: number) => {
    if (!item) return null;

    return (
      <ResourceFilterRow
        key={item.id}
        index={index}
        item={item}
        isActiveLoading={isActiveLoading}
        isReadOnly={isReadOnly}
        fields={fields}
        updateFilters={updateFilters}
        data={data}
        fieldKeysCorrection={fieldKeysCorrection}
        isVariationView={isVariationView}
        handleConstantValue={handleConstantValue}
        selectedObjectType={selectedObjectType}
        propertiesMap={propertiesMap}
        checklistId={checklistId}
        parameter={parameter}
      />
    );
  });
};
export default ResourceFilter;
