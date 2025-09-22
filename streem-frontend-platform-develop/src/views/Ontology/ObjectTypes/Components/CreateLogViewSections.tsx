import { Button, FormGroup } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import SearchableAccordionList from '#components/shared/SearchableAccordionList';
import SortableList from '#components/shared/SortableList';
import { useCreateJobLogView } from '#hooks/useCreateJobLogView';
import { createFetchList } from '#hooks/useFetchData';
import { LogType, TriggerTypeEnum } from '#PrototypeComposer/checklist.types';
import { useTypedSelector } from '#store';
import {
  apiCreateObjectTypeJobLogsCustomView,
  apiEditObjectTypeJobLogsCustomView,
  apiGetObjectTypes,
} from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { formatFilters } from '#utils/filtersToQueryParams';
import { InputTypes } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { FilterCard, FiltersWrapper } from '#views/Ontology/Objects/Overlays/JobLogsFilterDrawer';
import { objectJobLogColumns } from '#views/Ontology/utils';
import { DragEndEvent } from '@dnd-kit/core';
import { arrayMove } from '@dnd-kit/sortable';
import { AddCircleOutline } from '@material-ui/icons';
import { findIndex, isEqual, orderBy } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { FormProvider, useFieldArray, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { v4 as uuidv4 } from 'uuid';

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  justify-content: space-between;

  .columns-accordion-details {
    max-height: 500px;
    overflow: auto;
  }
`;

const Footer: FC<{
  onCancel: () => void;
  onPrevious?: () => void;
  onNext: () => void;
  disableNext?: boolean;
  backLabel?: string;
  nextLabel?: string;
}> = ({ onCancel, onPrevious, onNext, disableNext, backLabel = 'Back', nextLabel = 'Next' }) => {
  const FooterWrapper = styled.div`
    padding: 16px 0px;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid #e0e0e0;
    position: sticky;
    bottom: 0;
    background: white;

    button {
      height: 32px;
    }
  `;

  return (
    <FooterWrapper>
      <Button variant="textOnly" style={{ marginRight: 'auto' }} onClick={onCancel} color="blue">
        Cancel
      </Button>
      {onPrevious && (
        <Button variant="textOnly" onClick={onPrevious} color="blue">
          {backLabel}
        </Button>
      )}
      <Button onClick={onNext} disabled={disableNext}>
        {nextLabel}
      </Button>
    </FooterWrapper>
  );
};

export const BasicInfoSection: FC<{
  onCloseDrawer: () => void;
}> = ({ onCloseDrawer }) => {
  const { setActiveStep, label, setLabel } = useCreateJobLogView();

  return (
    <Wrapper>
      <FormGroup
        inputs={[
          {
            type: InputTypes.SINGLE_LINE,
            props: {
              placeholder: 'Write here',
              label: 'Label',
              id: 'label',
              value: label,
              onChange: ({ value }: { value: string }) => {
                setLabel(value);
              },
            },
          },
        ]}
        style={{ padding: '0px' }}
      />
      <Footer
        onCancel={onCloseDrawer}
        onNext={() => {
          setActiveStep(1);
        }}
        disableNext={!label}
      />
    </Wrapper>
  );
};

export const ColumnsSection: FC<{
  onCloseDrawer: () => void;
}> = ({ onCloseDrawer }) => {
  const { setActiveStep, columns, setColumns } = useCreateJobLogView();
  const {
    list: objectTypesList,
    reset: objectTypesListReset,
    fetchNext: objectTypesListFetchNext,
  } = createFetchList(
    apiGetObjectTypes(),
    {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      usageStatus: 1,
    },
    true,
  );

  const [allItems, setAllItems] = useState<Record<string, any>>({});

  const orderedColumns = useMemo(() => {
    return orderBy(columns, ['orderTree'], ['asc']);
  }, [columns]);

  const fetchResources = (searchQuery: string) => {
    objectTypesListReset({
      params: {
        displayName: searchQuery,
        page: DEFAULT_PAGE_NUMBER,
        size: DEFAULT_PAGE_SIZE,
      },
    });
  };

  const formatColumns = () => {
    const selectedColumns = orderedColumns.reduce<Record<string, any>>((acc, column) => {
      acc[`${column.id}_${column.triggerType}`] = {
        ...column,
        checked: true,
      };
      return acc;
    }, {});

    const allColumns = [
      ...objectJobLogColumns,
      ...objectTypesList.map((ot, i) => ({
        id: ot.id,
        type: LogType.TEXT,
        displayName: ot.displayName,
        triggerType: TriggerTypeEnum.RESOURCE,
        orderTree: columns.length + i + 1,
      })),
    ].reduce((acc, column) => {
      if (!selectedColumns?.[`${column.id}_${column.triggerType}`])
        acc[`${column.id}_${column.triggerType}`] = {
          ...column,
          checked: false,
        };
      return acc;
    }, selectedColumns);

    setAllItems(allColumns);
  };

  const allColumns = Object.entries(allItems).reduce<Record<string, Record<string, any>>>(
    (acc, [key, c]) => {
      if (
        c.triggerType !== TriggerTypeEnum.PARAMETER_SELF_VERIFIED_AT &&
        c.triggerType !== TriggerTypeEnum.PARAMETER_PEER_VERIFIED_AT &&
        c.triggerType !== TriggerTypeEnum.PARAMETER_PEER_STATUS
      ) {
        if (c.triggerType !== TriggerTypeEnum.RESOURCE && c.id === '-1') {
          acc.commonColumns[key] = c;
        } else {
          acc.resourceColumns[key] = c;
        }
      }
      return acc;
    },
    { commonColumns: {}, resourceColumns: {} },
  );

  useEffect(() => {
    if (objectTypesList.length) {
      formatColumns();
    }
  }, [objectTypesList]);

  return (
    <Wrapper>
      <SearchableAccordionList
        accordionSections={{
          commonColumns: 'Process Agnostic Properties /Common Properties',
          resourceColumns: 'Resources',
        }}
        columns={allColumns}
        onColumnToggle={({ key, checked, column }) => {
          if (checked) {
            setColumns((prev) => [...prev, column]);
          } else {
            setColumns((prev) => prev.filter((i) => `${i.id}_${i.triggerType}` !== key));
          }
          setAllItems((prev) => ({
            ...prev,
            [key]: { ...prev[key], checked },
          }));
        }}
        searchPlaceholder="Search"
        fetchResources={fetchResources}
        fetchNextResources={objectTypesListFetchNext}
      />
      <Footer
        onCancel={onCloseDrawer}
        onPrevious={() => {
          setActiveStep(0);
        }}
        onNext={() => {
          setActiveStep(2);
        }}
        disableNext={!columns.length}
      />
    </Wrapper>
  );
};

export const ConfigureColumnsSection: FC<{
  onCloseDrawer: () => void;
}> = ({ onCloseDrawer }) => {
  const { setActiveStep, columns, setColumns } = useCreateJobLogView();

  const handleDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;
    if (over && active.id !== over.id) {
      setColumns((items) => {
        const oldIndex = findIndex(items, (item) => {
          if (active.id === `${item.id}_${item.triggerType}`) {
            return true;
          }
          return false;
        });
        const newIndex = findIndex(items, (item) => {
          if (over.id === `${item.id}_${item.triggerType}`) {
            return true;
          }
          return false;
        });
        return arrayMove(items, oldIndex, newIndex);
      });
    }
  };

  return (
    <Wrapper>
      <SortableList
        title="Selected Columns"
        info="You can rearrange this selected columns (Cannot edit Default columns.)"
        items={columns}
        onDragEnd={handleDragEnd}
        keysForId={['id', 'triggerType']}
      />
      <Footer
        onCancel={onCloseDrawer}
        onPrevious={() => {
          setActiveStep(1);
        }}
        onNext={() => {
          setActiveStep(3);
        }}
      />
    </Wrapper>
  );
};

export const FiltersSection: FC<{
  onCloseDrawer: () => void;
  setReRender: (prev: boolean) => void;
  selectedView: any;
}> = ({ onCloseDrawer, setReRender, selectedView }) => {
  const dispatch = useDispatch();
  const { setActiveStep, filters, label, columns } = useCreateJobLogView();

  const logViewId = selectedView?.id;

  const {
    objectTypes: { active },
  } = useTypedSelector((state) => state.ontology);

  const { selectedUseCase } = useTypedSelector((state) => state.auth);

  const formMethods = useForm<{
    filters: any[];
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      filters: (filters || []).map((filter: any) => ({
        key: filter.key,
        constraint: filter.constraint,
        value: filter.value?.[0],
      })),
    },
  });

  const {
    formState: { isDirty, isValid },
    control,
    watch,
  } = formMethods;

  const formValues = watch('filters');

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'filters',
  });

  const controlledFields = useMemo(() => {
    return fields.map((field, index) => ({
      ...field,
      ...formValues[index],
    }));
  }, [fields]);

  const onAddNewFilter = () => {
    const id = uuidv4();
    append({
      id,
    });
  };

  const onRemoveFilter = (index: number) => {
    remove(index);
  };

  const upsertView = async () => {
    const method = logViewId ? 'PATCH' : 'POST';
    const url = logViewId
      ? apiEditObjectTypeJobLogsCustomView(logViewId)
      : apiCreateObjectTypeJobLogsCustomView(active?.id);

    const { data, errors } = await request(method, url, {
      data: {
        label,
        columns,
        filters: formatFilters(formValues),
        useCaseId: selectedUseCase?.id,
      },
    });

    if (data) {
      const actionType = logViewId ? 'updated' : 'created';
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Log View ${actionType} successfully`,
        }),
      );
      onCloseDrawer();
      setReRender((prev) => !prev);
    } else if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
  };

  const handleDisableNext = () => {
    const formHasChanges =
      selectedView?.label !== label || !isEqual(selectedView?.columns, columns);
    if (formValues.length === 0) {
      return false;
    } else {
      return !isValid || (!isDirty && !formHasChanges);
    }
  };

  return (
    <Wrapper>
      <FormProvider {...formMethods}>
        <FiltersWrapper>
          <div className="filter-cards">
            {controlledFields.map((item, index) => {
              return (
                <FilterCard
                  key={item.id}
                  item={item}
                  index={index}
                  remove={onRemoveFilter}
                  control={control}
                />
              );
            })}
            <Button
              type="button"
              variant="secondary"
              style={{ marginBottom: 16, padding: '6px 8px' }}
              onClick={onAddNewFilter}
            >
              <AddCircleOutline style={{ marginRight: 8 }} /> Add Filter
            </Button>
          </div>
        </FiltersWrapper>
      </FormProvider>
      <Footer
        onCancel={onCloseDrawer}
        onPrevious={() => {
          setActiveStep(2);
        }}
        onNext={() => {
          upsertView();
        }}
        nextLabel="Save changes"
        disableNext={handleDisableNext()}
      />
    </Wrapper>
  );
};
