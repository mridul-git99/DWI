import { Button, FormGroup, ToggleSwitch, useDrawer } from '#components';
import { useTypedSelector } from '#store';
import { apiGetObjectTypes } from '#utils/apiUrls';
import { InputTypes, ResponseObj, fetchDataParams } from '#utils/globalTypes';
import { request } from '#utils/request';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, nonEmptyStringRegex } from '#utils/constants';
import {
  createObjectTypeRelation,
  editObjectTypeRelation,
  fetchObjectTypes,
} from '#views/Ontology/actions';
import { ObjectType } from '#views/Ontology/types';
import React, { FC, useEffect, useState, useRef } from 'react';
import { debounce } from 'lodash';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { findHighestSortOrder } from '#views/Ontology/utils';

const AddPropertyDrawerWrapper = styled.form`
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  .form-group {
    padding: 0;
    margin-bottom: 16px;
    :last-of-type {
      margin-bottom: 0;
    }
  }
`;

const AddRelationDrawer: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<boolean | string>>;
  label: string | boolean;
  relation: any;
  setSelectedRelation: React.Dispatch<React.SetStateAction<any>>;
  setShouldToggle: React.Dispatch<React.SetStateAction<boolean>>;
}> = ({ onCloseDrawer, relation, label, setSelectedRelation, setShouldToggle }) => {
  const dispatch = useDispatch();
  const {
    ontology: {
      objectTypes: { list, listLoading, pageable, active },
    },
  } = useTypedSelector((state) => state);
  const searchedValue = useRef<string>('');

  const [selectedObjectTypeInfo, setSelectedObjectTypeInfo] = useState<ObjectType | null>(null);

  const form = useForm<{
    mandatory: boolean;
    displayName: string;
    description: string;
    cardinality: any;
    objectType: string;
    reason: string;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: { mandatory: true, reason: '' },
  });

  const {
    register,
    handleSubmit,
    formState: { isDirty, isValid },
    setValue,
    watch,
    getValues,
    reset,
  } = form;

  const showReasonField = label === 'Edit' ? isDirty : true;

  register('mandatory');
  register('cardinality', {
    required: true,
  });
  register('objectType', {
    required: true,
  });

  const { mandatory, cardinality, objectType } = watch(['mandatory', 'cardinality', 'objectType']);

  const fetchData = (params: fetchDataParams = {}, appendData: boolean = false) => {
    const { page = DEFAULT_PAGE_NUMBER, query = searchedValue.current } = params;
    dispatch(
      fetchObjectTypes(
        {
          page,
          size: DEFAULT_PAGE_SIZE,
          usageStatus: 1,
          ...(query && { displayName: query }),
        },
        appendData,
      ),
    );
  };

  const fetchObjectTypeOfSelectedRelation = async (id: string) => {
    const response: ResponseObj<ObjectType> = await request('GET', apiGetObjectTypes(id));
    if (response?.data) {
      setSelectedObjectTypeInfo(response?.data);
    }
  };

  const handleMenuScrollToBottom = () => {
    if (!listLoading && !pageable.last) {
      fetchData({ page: pageable.page + 1 }, true);
    }
  };

  useEffect(() => {
    if (!relation) {
      fetchData();
    }
  }, []);

  const basicInformation = () => {
    return (
      <div
        style={{
          height: '100%',
        }}
      >
        <FormGroup
          style={{ marginBottom: 24 }}
          inputs={[
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'objectType',
                label: 'Object Type',
                options: list
                  ?.filter((currType) => currType?.id !== active?.id)
                  ?.map((currType) => ({
                    ...currType,
                    label: currType.displayName,
                    value: currType.externalId,
                  })),
                isLoading: label === 'Edit' && !selectedObjectTypeInfo,
                isDisabled: label === 'Edit',
                onInputChange: debounce((searchedString: string, actionMeta) => {
                  if (searchedString !== actionMeta.prevInputValue) {
                    searchedValue.current = searchedString;
                    fetchData({ query: searchedString });
                  }
                }, 500),
                onMenuScrollToBottom: handleMenuScrollToBottom,
                placeholder: 'Select Object Type',
                value: objectType
                  ? [{ label: objectType?.displayName, value: objectType?.id }]
                  : null,
                onChange: (option: { value: string }) => {
                  setValue('objectType', option, {
                    shouldDirty: true,
                    shouldValidate: true,
                  });
                },
              },
            },
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                id: 'id',
                label: 'ID',
                disabled: true,
                placeholder: 'Auto Generated',
              },
            },
            {
              type: InputTypes.MULTI_LINE,
              props: {
                id: 'description',
                label: 'Description',
                name: 'description',
                optional: true,
                placeholder: 'Write Here',
                rows: 3,
                ref: register,
              },
            },
            {
              type: InputTypes.SINGLE_SELECT,
              props: {
                id: 'cardinality',
                label: 'Cardinality',
                placeholder: 'Select ',
                options: [
                  { label: 'One-To-One', value: 'ONE_TO_ONE' },
                  { label: 'One-To-Many', value: 'ONE_TO_MANY' },
                ],
                defaultValue: cardinality
                  ? [
                      {
                        label: cardinality === 'ONE_TO_ONE' ? 'One-To-One' : 'One-To-Many',
                        value: cardinality,
                      },
                    ]
                  : null,
                isDisabled: label === 'Edit',
                onChange: (option: { value: string }) => {
                  setValue('cardinality', option.value, {
                    shouldDirty: true,
                    shouldValidate: true,
                  });
                },
              },
            },
          ]}
        />
        <ToggleSwitch
          height={24}
          width={48}
          offLabel="Optional"
          onColor="#24a148"
          checked={mandatory}
          onChange={(isChecked) => {
            setValue('mandatory', isChecked, {
              shouldDirty: true,
              shouldValidate: true,
            });
          }}
          onLabel="Required"
        />
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
                },
              },
            ]}
          />
        )}
      </div>
    );
  };

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
      setSelectedRelation(null);
    }, 200);
  };

  const onSubmit = () => {
    const _data = getValues();

    if (label === 'Edit') {
      const newData = {
        id: relation?.id,
        displayName: _data.displayName,
        description: _data?.description || '',
        flags: _data.mandatory ? 16 : 0,
        objectTypeId: _data?.objectType?.id,
        sortOrder: relation?.sortOrder,
        target: {
          type: 'OBJECTS',
          cardinality: _data.cardinality,
        },
        usageStatus: 1,
        reason: _data.reason,
      };
      dispatch(
        editObjectTypeRelation({
          objectTypeId: active?.id,
          data: newData,
          relationId: relation?.id,
        }),
      );
    } else {
      let sortOrder = 1;
      let maxSortOrder = findHighestSortOrder(
        [...(active?.properties || []), ...(active?.relations || [])] || [],
      );
      sortOrder = maxSortOrder + 1;
      const newData = {
        id: null,
        displayName: _data.displayName,
        description: _data?.description || '',
        sortOrder,
        flags: _data.mandatory ? 16 : 0,
        objectTypeId: _data?.objectType?.id,
        target: {
          type: 'OBJECTS',
          cardinality: _data.cardinality,
        },
        usageStatus: 1,
        reason: _data.reason,
      };
      dispatch(createObjectTypeRelation({ objectTypeId: active?.id, data: newData }));
    }
    setTimeout(() => setShouldToggle((prev) => !prev), 300);
    handleCloseDrawer();
  };

  useEffect(() => {
    if (relation?.id) {
      reset({
        displayName: relation?.displayName,
        description: relation?.description,
        cardinality: relation?.target?.cardinality,
        objectType: selectedObjectTypeInfo,
        mandatory: relation.flags === 16 ? true : false,
      });

      if (!selectedObjectTypeInfo) {
        fetchObjectTypeOfSelectedRelation(relation?.objectTypeId);
      }
    }
  }, [selectedObjectTypeInfo]);

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: label === 'Edit' ? 'Edit Relation' : 'Create a New Relation',
    hideCloseIcon: true,
    bodyContent: (
      <AddPropertyDrawerWrapper onSubmit={handleSubmit(onSubmit)}>
        <FormGroup
          style={{ marginBlock: 24 }}
          inputs={[
            {
              type: InputTypes.SINGLE_LINE,
              props: {
                placeholder: 'Write here',
                label: 'Label',
                id: 'displayName',
                name: 'displayName',
                ref: register({
                  required: true,
                }),
              },
            },
          ]}
        />
        {basicInformation()}
      </AddPropertyDrawerWrapper>
    ),
    footerContent: (
      <>
        <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={handleCloseDrawer}>
          Cancel
        </Button>

        <Button type="submit" disabled={!isDirty || !isValid} onClick={onSubmit}>
          {label === 'Edit' ? 'Update' : 'Create'}
        </Button>
      </>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return <AddPropertyDrawerWrapper>{StyledDrawer}</AddPropertyDrawerWrapper>;
};

export default AddRelationDrawer;
