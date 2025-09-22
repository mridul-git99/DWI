import SearchableAccordionList from '#components/shared/SearchableAccordionList';
import SortableList from '#components/shared/SortableList';
import { default as React } from 'react';

export const ConfigureColumnsModal = ({
  allColumns,
  accordionSections,
  handleColumnToggle,
  onSectionSelectAll,
  onSectionRemoveAll,
  items,
  pinnedItems,
  handleDragEnd,
  handlePushPinHandler,
  fetchResources,
  fetchNextResources,
}: any) => {
  return (
    <div className="body">
      <SearchableAccordionList
        accordionSections={accordionSections}
        columns={allColumns}
        searchPlaceholder="Search column name"
        accordionTitle="All Columns"
        onColumnToggle={handleColumnToggle}
        onSectionSelectAll={onSectionSelectAll}
        onSectionRemoveAll={onSectionRemoveAll}
        fetchResources={fetchResources}
        fetchNextResources={fetchNextResources}
      />
      <SortableList
        title="Selected Columns"
        pinnedTitle="Pinned Columns"
        info="You can rearrange this selected columns (Cannot edit Default columns.)"
        pinnedInfo="You can pin maximum 2 columns at a time"
        items={items}
        pinnedItems={pinnedItems}
        isPinDisabled={pinnedItems.length >= 2}
        onDragEnd={handleDragEnd}
        handlePushPinHandler={handlePushPinHandler}
        isPinViewEnabled={true}
        keysForId={['id', 'triggerType']}
      />
    </div>
  );
};
