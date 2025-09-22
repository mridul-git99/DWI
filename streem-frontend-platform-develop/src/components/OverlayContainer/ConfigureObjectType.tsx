import { BaseModal } from '#components';
import { DragEndEvent } from '@dnd-kit/core';
import { arrayMove } from '@dnd-kit/sortable';
import React, { FC, useState } from 'react';
import { CommonOverlayProps } from './types';
import { useDispatch } from 'react-redux';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { reorderColumns } from 'src/views/Ontology/actions';
import SortableList from '#components/shared/SortableList';

type Props = {
  columns: any[];
  selectedColumns: any[];
  objectTypeId: string;
};

const ConfigureColumnsObjectModal: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { selectedColumns, objectTypeId },
}) => {
  const [items, setItems] = useState(() => selectedColumns);

  const dispatch = useDispatch();

  const handleDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;
    if (over && active.id !== over.id) {
      setItems((items) => {
        const oldIndex = items.findIndex((item) => `${item.id}` === active.id);
        const newIndex = items.findIndex((item) => `${item.id}` === over.id);
        return arrayMove(items, oldIndex, newIndex);
      });
    }
  };

  const onPrimary = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.REASON_MODAL,
        props: {
          modalTitle: 'Configure Columns',
          modalDesc: `Enter reason for change`,
          shouldAskForReason: true,
          onSubmitHandler: (
            reason: string,
            _: (errors?: Error[]) => void,
            closeModal: () => void,
          ) => {
            const propertySortOrderMap: Record<string, number> = {};
            const relationSortOrderMap: Record<string, number> = {};
            items.forEach((item, index) => {
              if (item.type === 'PROPERTY') {
                propertySortOrderMap[item.id] = index + 1;
              } else if (item.type === 'RELATION') {
                relationSortOrderMap[item.id] = index + 1;
              }
            });
            const requestData = {
              objectTypeId,
              propertySortOrderMap,
              relationSortOrderMap,
              reason,
            };
            dispatch(reorderColumns(requestData));
            closeModal();
          },
        },
      }),
    );
  };

  return (
    <BaseModal
      closeAllModals={closeAllOverlays}
      closeModal={closeOverlay}
      title="Configure Columns"
      secondaryText="Cancel"
      primaryText="Save"
      onSecondary={closeOverlay}
      onPrimary={onPrimary}
      disabledPrimary={items.length === 0}
    >
      <div className="body">
        <div className="section section-right">
          <SortableList
            title="Selected Columns"
            info="You can rearrange these selected columns (Cannot edit Default columns.)"
            items={items}
            labelKey="label"
            onDragEnd={handleDragEnd}
            keysForId={['id']}
          />
        </div>
      </div>
    </BaseModal>
  );
};
export default ConfigureColumnsObjectModal;
