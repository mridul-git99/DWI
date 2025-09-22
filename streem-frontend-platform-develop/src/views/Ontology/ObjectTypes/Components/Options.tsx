import { AddNewItem, TextInput } from '#components';
import {
  closestCenter,
  DndContext,
  DragEndEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import { Close, DragIndicator } from '@material-ui/icons';
import { findIndex } from 'lodash';
import React, { FC, useEffect, useRef } from 'react';
import { useFieldArray, UseFormMethods } from 'react-hook-form';
import styled from 'styled-components';
import { v4 as uuidv4 } from 'uuid';
import { CommonWrapper } from '#PrototypeComposer/Parameters/SetupViews/styles';

const SortableItemWrapper = styled.div`
  display: flex;
  align-items: center;
  touch-action: none;
  background-color: #fff;
  margin-bottom: 8px;

  svg {
    cursor: pointer;
  }

  .content {
    display: flex;
    align-items: center;
    flex: 1;

    svg {
      color: #999999;
      outline: none;
      margin-right: 8px;
    }
  }

  .action {
    padding: 0px 8px;
    display: flex;
    align-items: center;
    svg {
      font-size: 16px;
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

function SortableItem({ item, index, remove, register, isReadOnly }: any) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: item.id,
    disabled: isReadOnly,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <SortableItemWrapper ref={setNodeRef} style={style} className={isDragging ? 'dragging' : ''}>
      <div className="content">
        {!isReadOnly && <DragIndicator {...attributes} {...listeners} />}
        <input
          type="hidden"
          name={`data.${index}.id`}
          ref={register({
            required: true,
          })}
          defaultValue={item.id}
        />
        <TextInput
          name={`data.${index}.displayName`}
          ref={register({
            required: true,
          })}
          defaultValue={item.displayName}
          disabled={isReadOnly}
        />
      </div>
      {!isReadOnly && (
        <div className="action">
          <Close onClick={() => remove(index)} />
        </div>
      )}
    </SortableItemWrapper>
  );
}

const ChecklistParameter: FC<{ form: UseFormMethods<any> }> = ({ form }) => {
  const newItems = useRef<Record<string, boolean>>({});
  const { register, control, setError, clearErrors, errors } = form;
  const { fields, append, remove, move } = useFieldArray({
    control,
    name: 'data',
  });

  useEffect(() => {
    if (fields.length && errors?.data) {
      clearErrors('data');
    } else if (!fields.length && !errors?.data) {
      setError('data', {
        message: 'At least one option is required.',
        type: 'minLength',
      });
    }
  }, [fields]);

  const sensors = useSensors(useSensor(PointerSensor));

  const handleDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;

    if (over && active.id !== over.id) {
      const oldIndex = findIndex(fields, ['id', active.id]);
      const newIndex = findIndex(fields, ['id', over.id]);
      move(oldIndex, newIndex);
    }
  };

  return (
    <CommonWrapper>
      <ul className="list">
        <DndContext
          modifiers={[restrictToVerticalAxis]}
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
        >
          <SortableContext items={fields as any} strategy={verticalListSortingStrategy}>
            {fields.map((item, index) => (
              <SortableItem
                key={item.id}
                item={item}
                index={index}
                register={register}
                remove={remove}
                isReadOnly={!newItems.current[item.id!]}
              />
            ))}
          </SortableContext>
        </DndContext>
      </ul>
      <AddNewItem
        onClick={() => {
          const id = uuidv4();
          newItems.current[id] = true;
          append({ id, displayName: '' });
        }}
      />
    </CommonWrapper>
  );
};

export default ChecklistParameter;
