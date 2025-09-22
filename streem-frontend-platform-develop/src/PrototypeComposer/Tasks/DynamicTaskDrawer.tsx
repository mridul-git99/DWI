import { Button, FormGroup, useDrawer } from '#components';
import { useTypedSelector } from '#store';
import { MandatoryParameter } from '#types';
import { apiGetParameters } from '#utils/apiUrls';
import { FilterOperators, InputTypes } from '#utils/globalTypes';
import { request } from '#utils/request';
import React, { FC, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { addNewTask } from './actions';

const DynamicTaskDrawerWrapper = styled.form`
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  padding-top: 16px;

  .form-group {
    padding: 0;
    margin-bottom: 16px;

    :last-of-type {
      margin-bottom: 0;
    }
  }
`;

const DynamicTaskDrawer: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<any>>;
}> = ({ onCloseDrawer }) => {
  const dispatch = useDispatch();
  const {
    prototypeComposer: {
      stages: { activeStageId },
      tasks: { activeTaskId },
      data: checklist,
    },
  } = useTypedSelector((state) => state);
  const [isLoadingParameters, setIsLoadingParameters] = useState(false);
  const [parameterList, setParameterList] = useState<any[]>([]);
  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  const {
    handleSubmit,
    formState: { isDirty, isValid },
    getValues,
    register,
    setValue,
    watch,
  } = form;

  const { iteratorType } = watch(['iteratorType']);

  useEffect(() => {
    register('iteratorType', { required: true });
    register('parameterId', { required: true });
  }, []);

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  useEffect(() => {
    if (iteratorType) {
      fetchParameters();
    }
  }, [iteratorType]);

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  };

  const handleDynamicTask = () => {
    const data = getValues();
    if (data && checklist && activeStageId && activeTaskId) {
      dispatch(
        addNewTask({
          checklistId: checklist.id,
          stageId: activeStageId,
          data,
          type: 'DYNAMIC',
        }),
      );
      handleCloseDrawer();
    }
  };

  const fetchParameters = async () => {
    if (checklist?.id) {
      setIsLoadingParameters(true);
      const parameters = await request('GET', apiGetParameters(checklist.id), {
        params: {
          sort: 'createdAt,desc',
          filters: {
            op: FilterOperators.AND,
            fields: [
              { field: 'archived', op: FilterOperators.EQ, values: [false] },
              {
                field: 'type',
                op: FilterOperators.ANY,
                values:
                  iteratorType === 'LIST'
                    ? [MandatoryParameter.MULTISELECT, MandatoryParameter.MULTI_RESOURCE]
                    : [
                        MandatoryParameter.NUMBER,
                        MandatoryParameter.CALCULATION,
                        MandatoryParameter.SHOULD_BE,
                      ],
              },
            ],
          },
        },
      });
      setParameterList(parameters.data);
      setIsLoadingParameters(false);
    }
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Create Dynamic Subtasks',
    hideCloseIcon: true,
    bodyContent: (
      <DynamicTaskDrawerWrapper onSubmit={handleSubmit(handleDynamicTask)}>
        <p>Number of sub-tasks to be generated will be based on</p>
        <FormGroup
          key="basic-info-section"
          inputs={[
            {
              type: InputTypes.RADIO,
              props: {
                groupProps: {
                  id: 'iteratorType',
                  name: 'iteratorType',
                  onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                    setValue('iteratorType', e.target.value, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  },
                },
                items: [
                  {
                    key: 'VALUE',
                    label: 'The value of a parameter',
                    value: 'VALUE',
                    desc: `A 'Number', 'Should be' or a ‘Calculation’ parameter can be used here`,
                    disabled: true,
                  },
                  {
                    key: 'LIST',
                    label: 'The items of a parameter',
                    value: 'LIST',
                    desc: `A 'Multi Select' or a 'Multi Select Resource' Parameter can be used here`,
                  },
                ],
              },
            },
            ...(iteratorType
              ? [
                  {
                    type: InputTypes.SINGLE_SELECT,
                    props: {
                      id: 'parameterId',
                      label: 'Select Parameter',
                      isLoading: isLoadingParameters,
                      options: parameterList.map((number: any) => ({
                        label: number.label,
                        value: number.id,
                      })),
                      placeholder: 'Select Parameter',
                      onChange: (_option: any) => {
                        setValue('parameterId', _option.value, {
                          shouldDirty: true,
                          shouldValidate: true,
                        });
                      },
                    },
                  },
                ]
              : []),
          ]}
        />
      </DynamicTaskDrawerWrapper>
    ),
    footerContent: (
      <>
        <Button
          variant="secondary"
          style={{ marginLeft: 'auto' }}
          onClick={() => handleCloseDrawer()}
        >
          Cancel
        </Button>
        <Button type="submit" disabled={!isDirty || !isValid} onClick={handleDynamicTask}>
          Save
        </Button>
      </>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return StyledDrawer;
};

export default DynamicTaskDrawer;
