import { JobLogColumnType, LogType, TriggerTypeEnum } from '#PrototypeComposer/checklist.types';
import { BaseModal, Button, LoadingContainer } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { apiGetObjectTypes } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { DragEndEvent } from '@dnd-kit/core';
import { arrayMove } from '@dnd-kit/sortable';
import { findIndex } from 'lodash';
import React, { FC, useCallback, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import { ConfigureColumnsModal } from './ConfigureColumnsModal';
import { Column, CommonOverlayProps } from './types';

export const Wrapper = styled.div`
  .modal {
    padding: 0;
    max-height: 80dvh !important;

    &-body {
      padding: 0 !important;

      .body {
        display: flex;

        .section {
          flex-direction: column;
          display: flex;
          flex: 1;
          padding: 16px 0 0 16px;
          height: 60dvh;

          .search-column-name {
            margin-right: 8px;
          }

          .columns-accordion-details {
            max-height: 160px;
            overflow: auto;
          }

          &-right {
            overflow: auto;
          }
        }
      }
    }

    &-footer {
      justify-content: space-between;
    }
  }
`;

type Props = {
  columns: JobLogColumnType[];
  selectedColumns: JobLogColumnType[];
  onColumnSelection: (selectedColumns: JobLogColumnType[]) => void;
};

const accordionSections = {
  commonColumns: 'Process Agnostic Properties',
  resourceColumns: 'Resources',
  processColumns: 'Process properties',
};

const ConfigureColumnSection = ({
  closeAllOverlays,
  closeOverlay,
  columns,
  onColumnSelection,
  unPinnedColumns = [],
  pinnedColumns = [],
}: any) => {
  const [items, setItems] = useState<JobLogColumnType[]>(unPinnedColumns);
  const [pinnedItems, setPinnedItems] = useState<any>(pinnedColumns);
  const [allItems, setAllItems] = useState<Record<string, any>>({});
  const {
    status: objectTypesListStatus,
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

  const onPrimary = useCallback(() => {
    closeOverlay();
    const pinnedItemsDetails = pinnedItems.map((column: Column, i: number) => ({
      ...column,
      orderTree: i + 1,
      pinned: true,
    }));
    const data = items.map((column, i) => ({
      ...column,
      pinned: false,
      orderTree: i + 1 + pinnedItemsDetails.length,
    }));
    onColumnSelection([...pinnedItemsDetails, ...data]);
  }, [closeOverlay, pinnedItems, items, onColumnSelection]);

  const onRemoveAll = useCallback(() => {
    setAllItems((prevAllItems) => {
      const updatedItems = Object.keys(prevAllItems).reduce((acc, key) => {
        acc[key] = { ...prevAllItems[key], checked: false };
        return acc;
      }, {});

      return updatedItems;
    });
    setPinnedItems([]);
    setItems([]);
  }, [allItems]);

  const handlePushPinHandler = useCallback(
    (item: JobLogColumnType): void => {
      const isPinned: boolean = pinnedItems.some(
        (i: Column) => `${i.id}_${i.triggerType}` === `${item.id}_${item.triggerType}`,
      );

      const unpinItem = (): void => {
        setItems((prev: JobLogColumnType[]) => [...prev, { ...item, pinned: false }]);
        setPinnedItems((prev: Column[]) =>
          prev.filter(
            (i: Column) => `${i.id}_${i.triggerType}` !== `${item.id}_${item.triggerType}`,
          ),
        );
      };

      const pinItem = (): void => {
        if (pinnedItems.length === 2) return;

        setItems((prev: JobLogColumnType[]) =>
          prev.filter(
            (i: JobLogColumnType) =>
              `${i.id}_${i.triggerType}` !== `${item.id}_${item.triggerType}`,
          ),
        );
        setPinnedItems((prev: Column[]) => [...prev, item]);
      };

      isPinned ? unpinItem() : pinItem();
    },
    [pinnedItems, setItems, setPinnedItems],
  );

  const moveItemInArray = useCallback(
    (items: JobLogColumnType[], activeId: string, overId: string) => {
      const oldIndex = findIndex(items, (item) => `${item.id}_${item.triggerType}` === activeId);
      const newIndex = findIndex(items, (item) => `${item.id}_${item.triggerType}` === overId);
      if (oldIndex !== -1 && newIndex !== -1) {
        return arrayMove(items, oldIndex, newIndex);
      }
      return items;
    },
    [],
  );

  const handleDragEnd = useCallback((e: DragEndEvent, isPinned: boolean) => {
    const { active, over } = e;
    if (over && active.id !== over.id) {
      isPinned
        ? setPinnedItems((items: JobLogColumnType[]) => moveItemInArray(items, active.id, over.id))
        : setItems((items: JobLogColumnType[]) => moveItemInArray(items, active.id, over.id));
    }
  }, []);

  const parseDataToState = useCallback(() => {
    const _selectedColumns = [...items, ...pinnedItems].reduce<Record<string, any>>(
      (acc, column) => {
        acc[`${column.id}_${column.triggerType}`] = {
          ...column,
          checked: true,
        };
        return acc;
      },
      {},
    );

    const allColumns = [
      ...columns,
      ...objectTypesList.map((ot, i) => ({
        id: ot.id,
        type: LogType.TEXT,
        displayName: ot.displayName,
        triggerType: TriggerTypeEnum.RESOURCE,
        orderTree: columns.length + i + 1,
      })),
    ].reduce((acc, column) => {
      if (!_selectedColumns?.[`${column.id}_${column.triggerType}`]) {
        acc[`${column.id}_${column.triggerType}`] = {
          ...column,
          checked: false,
        };
      }
      return acc;
    }, _selectedColumns);
    setAllItems(allColumns);
  }, [items, pinnedItems, columns, objectTypesList]);

  useEffect(() => {
    if (objectTypesList?.length && objectTypesListStatus === 'success') {
      parseDataToState();
    }
  }, [objectTypesListStatus]);

  const allColumns = useMemo(() => {
    const filteredItems = Object.entries(allItems).reduce<Record<string, Record<string, Column>>>(
      (acc, [key, item]) => {
        if (
          item.triggerType !== TriggerTypeEnum.PARAMETER_SELF_VERIFIED_AT &&
          item.triggerType !== TriggerTypeEnum.PARAMETER_PEER_VERIFIED_AT &&
          item.triggerType !== TriggerTypeEnum.PARAMETER_PEER_STATUS
        ) {
          if (item.triggerType !== TriggerTypeEnum.RESOURCE) {
            if (item.id === '-1') {
              acc.commonColumns[key] = item;
            } else {
              acc.processColumns[key] = item;
            }
          } else {
            acc.resourceColumns[key] = item;
          }
        }
        return acc;
      },
      { commonColumns: {}, processColumns: {}, resourceColumns: {} },
    );

    const sortColumnsByDesiredOrder = (columns, order) => {
      return Object.entries(columns)
        .sort(([_, a], [__, b]) => {
          const indexA = order.indexOf(a.displayName);
          const indexB = order.indexOf(b.displayName);

          if (indexA === -1 && indexB === -1) return 0;
          if (indexA === -1) return 1;
          if (indexB === -1) return -1;

          return indexA - indexB;
        })
        .reduce<Record<string, Column>>((acc, [key, value]) => {
          acc[key] = value;
          return acc;
        }, {});
    };

    const desiredOrder = [
      'Job Id',
      'Job State',
      'Job Start',
      'Job Started By',
      'Job Created At',
      'Job Created By',
      'Process Id',
      'Process Name',
      'Annotation Remark',
      'Annotation Media',
    ];

    return Object.entries(accordionSections).reduce<Record<string, Record<string, Column>>>(
      (result, [key]) => {
        if (key === 'commonColumns') {
          result[key] = sortColumnsByDesiredOrder(filteredItems[key], desiredOrder);
        } else {
          result[key] = filteredItems[key];
        }
        return result;
      },
      {},
    );
  }, [allItems]);

  const onSectionSelectAll = useCallback(
    (key: string) => {
      const newSelectedData: any[] = [];
      setItems((prev) => {
        const ids = [...pinnedItems, ...Object.values(prev)]?.map(
          (column) => `${column.id}_${column.triggerType}`,
        );
        const newSet = new Set(ids);
        Object.values(allColumns[key]).forEach((column) => {
          if (!newSet.has(`${column.id}_${column.triggerType}`)) {
            newSelectedData.push(column);
          }
        });
        return [...items, ...newSelectedData];
      });
      setAllItems((prev) => {
        const newObj: Record<string, any> = {};
        newSelectedData.forEach((column) => {
          newObj[`${column.id}_${column.triggerType}`] = { ...column, checked: true };
        });
        return {
          ...prev,
          ...newObj,
        };
      });
    },
    [pinnedItems, allColumns, items],
  );

  const onSectionRemoveAll = useCallback(
    (key: string) => {
      setAllItems((prev) => {
        const newObj: Record<string, any> = {};
        Object.values(allColumns[key]).forEach((column) => {
          newObj[`${column.id}_${column.triggerType}`] = { ...column, checked: false };
        });
        return {
          ...prev,
          ...newObj,
        };
      });
      setItems((prev) => {
        return prev.filter((column) => !allColumns[key][`${column.id}_${column.triggerType}`]);
      });
      setPinnedItems((prev: Column[]) => {
        return prev.filter(
          (column: Column) => !allColumns[key][`${column.id}_${column.triggerType}`],
        );
      });
    },
    [allColumns],
  );

  const handleColumnToggle = useCallback(
    ({ checked, column }) => {
      const key = `${column.id}_${column.triggerType}`;

      if (checked) {
        setItems((prev) => [...prev, column]);
      } else {
        setItems((prev) => prev.filter((i) => `${i.id}_${i.triggerType}` !== key));
        setPinnedItems((prev) => prev.filter((i) => `${i.id}_${i.triggerType}` !== key));
      }
      setAllItems((prev) => ({
        ...prev,
        [key]: { ...prev[key], checked },
      }));
    },
    [setItems, setPinnedItems, setAllItems],
  );

  const fetchResources = (searchQuery) => {
    objectTypesListReset({
      params: {
        displayName: searchQuery,
        page: DEFAULT_PAGE_NUMBER,
        size: DEFAULT_PAGE_SIZE,
      },
    });
  };

  const fetchNextResources = () => {
    objectTypesListFetchNext();
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title="Configure Columns"
        secondaryText="Cancel"
        primaryText="OK"
        onSecondary={closeOverlay}
        onPrimary={onPrimary}
        disabledPrimary={items.length === 0}
        modalFooterOptions={
          <Button variant="textOnly" onClick={onRemoveAll}>
            Remove All Columns
          </Button>
        }
      >
        <ConfigureColumnsModal
          allColumns={allColumns}
          accordionSections={accordionSections}
          handleColumnToggle={handleColumnToggle}
          onSectionSelectAll={onSectionSelectAll}
          onSectionRemoveAll={onSectionRemoveAll}
          items={items}
          pinnedItems={pinnedItems}
          handleDragEnd={handleDragEnd}
          handlePushPinHandler={handlePushPinHandler}
          fetchResources={fetchResources}
          fetchNextResources={fetchNextResources}
        />
      </BaseModal>
    </Wrapper>
  );
};

export const ConfigureColumnsModalContainer: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { columns, selectedColumns, onColumnSelection },
}) => {
  const [items, setItems] = useState<JobLogColumnType[]>([]);
  const [pinnedItems, setPinnedItems] = useState<any>([]);
  useEffect(() => {
    const sortedItems = selectedColumns.sort((a, b) => a.orderTree - b.orderTree);
    const { pinnedItems, nonPinnedItems } = sortedItems.reduce(
      (acc, column) => {
        if (column.pinned) {
          acc.pinnedItems.push(column);
        } else {
          acc.nonPinnedItems.push(column);
        }
        return acc;
      },
      { pinnedItems: [], nonPinnedItems: [] },
    );

    setItems(nonPinnedItems);
    setPinnedItems(pinnedItems);
  }, [selectedColumns]);

  return (
    <LoadingContainer
      loading={![...items, ...pinnedItems].length}
      component={
        <ConfigureColumnSection
          closeAllOverlays={closeAllOverlays}
          closeOverlay={closeOverlay}
          columns={columns}
          onColumnSelection={onColumnSelection}
          unPinnedColumns={items}
          pinnedColumns={pinnedItems}
        />
      }
    />
  );
};

export default ConfigureColumnsModalContainer;
