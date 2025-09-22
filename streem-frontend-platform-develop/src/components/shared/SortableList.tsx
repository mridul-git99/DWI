import PushPinImage from '#assets/svg/PushPin';
import { JobLogColumnType } from '#PrototypeComposer/checklist.types';
import {
  closestCenter,
  DndContext,
  DragEndEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { DragIndicator } from '@material-ui/icons';
import React, { FC } from 'react';
import styled from 'styled-components';
import { Button } from './Button';

const Wrapper = styled.div`
  flex: 1;
  .info {
    font-size: 12px;
    margin-block: 16px 8px;
    align-self: flex-start;
    text-align: left;
  }
  h4 {
    font-size: 14px;
    font-weight: 600;
    margin: 0;
  }
`;

const SortableItemWrapper = styled.div`
  display: flex;
  align-items: center;
  touch-action: none;
  background-color: #fff;

  svg {
    cursor: pointer;
  }

  .content {
    border-bottom: 1px solid #cccccc;
    display: flex;
    align-items: center;
    padding: 12px;
    flex: 1;
    justify-content: space-between;

    > div {
      display: flex;
      align-items: center;
    }

    svg {
      color: #999999;
      outline: none;
      margin-right: 8px;
    }
  }

  &.dragging {
    z-index: 1;
    transition: none;

    * {
      cursor: grabbing;
    }

    scale: 1.02;
    box-shadow: -1px 0 15px 0 rgba(34, 33, 81, 0.01), 0px 15px 15px 0 rgba(34, 33, 81, 0.25);

    &:focus-visible {
      box-shadow: 0 0px 10px 2px #4c9ffe;
    }
  }
`;

const getId = (item: JobLogColumnType, keysForId: string[]) =>
  keysForId?.map((key) => item[key]).join('_');

function SortableItem({
  item,
  labelKey,
  keysForId,
  handlePushPinHandler,
  isSelected,
  isPinDisabled,
  isPinViewEnabled,
}: {
  item: any;
  labelKey: string;
  keysForId: string[];
  isPinDisabled?: boolean;
  handlePushPinHandler: any;
  isSelected?: boolean;
  isPinViewEnabled: boolean;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: getId(item, keysForId),
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <SortableItemWrapper ref={setNodeRef} style={style} className={isDragging ? 'dragging' : ''}>
      <div className="content">
        <div>
          <DragIndicator {...attributes} {...listeners} />
          <span>{item[labelKey]}</span>
        </div>
        {isPinViewEnabled && (
          <Button
            style={{ paddingRight: 0 }}
            disabled={isPinDisabled}
            variant="textOnly"
            onClick={handlePushPinHandler}
          >
            <PushPinImage
              color={`${isSelected ? '#1D84FF' : isPinDisabled ? '#aeb5b4' : '#6F6F6F'}`}
            />
          </Button>
        )}
      </div>
    </SortableItemWrapper>
  );
}

type BaseProps = {
  title: string;
  info: string;
  items: JobLogColumnType[];
  onDragEnd: (event: DragEndEvent, isPinned: boolean) => void;
  isPinViewEnabled?: boolean;
  labelKey?: string;
  keysForId?: string[];
};

interface PinViewEnabledProps extends BaseProps {
  isPinViewEnabled: true;
  pinnedTitle: string;
  pinnedInfo: string;
  pinnedItems: JobLogColumnType[];
  isPinDisabled: boolean;
  handlePushPinHandler: (item: JobLogColumnType) => void;
}

interface PinViewDisabledProps extends BaseProps {
  isPinViewEnabled: false;
  pinnedTitle: never;
  pinnedInfo: never;
  pinnedItems: never;
  isPinDisabled: never;
  handlePushPinHandler: never;
}

type SortableItemProps = PinViewEnabledProps | PinViewDisabledProps;

const SortableList: FC<SortableItemProps> = ({
  title,
  pinnedTitle,
  info,
  pinnedInfo,
  items,
  pinnedItems,
  isPinDisabled,
  onDragEnd,
  handlePushPinHandler,
  isPinViewEnabled = false,
  labelKey = 'displayName',
  keysForId = ['id'],
}) => {
  const sensors = useSensors(useSensor(PointerSensor));
  return (
    <Wrapper className="section section-right">
      {isPinViewEnabled && (
        <div>
          <h4>{pinnedTitle}</h4>
          <span className="info">{pinnedInfo}</span>
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={(e) => onDragEnd(e, true)}
            modifiers={[restrictToVerticalAxis]}
          >
            <SortableContext
              items={pinnedItems?.map((i) => ({
                ...i,
                id: getId(i, keysForId),
              }))}
              strategy={verticalListSortingStrategy}
            >
              {pinnedItems.map((item) => {
                return (
                  <SortableItem
                    key={getId(item, keysForId)}
                    item={item}
                    isSelected={true}
                    handlePushPinHandler={() => handlePushPinHandler(item)}
                    isPinViewEnabled={isPinViewEnabled}
                    labelKey={labelKey}
                    keysForId={keysForId}
                  />
                );
              })}
            </SortableContext>
          </DndContext>
        </div>
      )}
      <div style={{ marginTop: '10px' }}>
        <h4>{title}</h4>
        <span className="info">{info}</span>
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={(e) => onDragEnd(e, false)}
          modifiers={[restrictToVerticalAxis]}
        >
          <SortableContext
            items={items.map((i) => ({ ...i, id: getId(i, keysForId) }))}
            strategy={verticalListSortingStrategy}
          >
            {items.map((item) => {
              return (
                <SortableItem
                  key={getId(item, keysForId)}
                  item={item}
                  isPinDisabled={isPinDisabled}
                  handlePushPinHandler={() => handlePushPinHandler(item)}
                  isPinViewEnabled={isPinViewEnabled}
                  labelKey={labelKey}
                  keysForId={keysForId}
                />
              );
            })}
          </SortableContext>
        </DndContext>
      </div>
    </Wrapper>
  );
};

export default SortableList;
