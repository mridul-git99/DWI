import { AddNewItem, TextInput } from '#components';
import {
  DndContext,
  DragEndEvent,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import {
  SortableContext,
  arrayMove,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Close, DragIndicator } from '@material-ui/icons';
import { compact, findIndex } from 'lodash';
import React, { FC } from 'react';
import { useFormContext } from 'react-hook-form';
import styled from 'styled-components';
import { v4 as uuidv4 } from 'uuid';
import { CommonWrapper } from './styles';
import LoadOptions from './LoadOptions';
import removeIcon from '#assets/svg/remove-icon.svg';

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

export const SortableItem = ({
  item,
  index,
  remove = undefined,
  isReadOnly,
  isEditable,
  onChangeOptionLabel = undefined,
}: any) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: item.id,
    disabled: isReadOnly,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <SortableItemWrapper
      ref={setNodeRef}
      style={style}
      key={item.id}
      className={isDragging ? 'dragging' : ''}
    >
      <div className="content">
        {!isReadOnly && <DragIndicator {...attributes} {...listeners} />}
        <input type="hidden" name={`data${index}.id`} defaultValue={item.id} />
        <TextInput
          name={`data${index}.name`}
          defaultValue={item.name}
          disabled={!isEditable}
          onChange={({ value }: any) => {
            onChangeOptionLabel?.(value, index);
          }}
        />
      </div>
      {!isReadOnly && (
        <div className="action">
          <Close
            onClick={() => {
              remove?.(index);
            }}
          />
        </div>
      )}
    </SortableItemWrapper>
  );
};

const ChecklistParameter: FC<{
  isReadOnly: boolean;
  stepperCount: number;
  loadOptions: boolean;
}> = ({ isReadOnly, stepperCount, loadOptions }) => {
  const { setError, clearErrors, setValue, watch, trigger } = useFormContext();
  const fields = watch('data', []);
  const sensors = useSensors(useSensor(PointerSensor));

  const setFormErrors = (isValid?: boolean) => {
    if (!isValid) {
      setError('data', {
        stepperCount: stepperCount,
        message: 'All Options Should be Filled.',
        type: 'minLength',
      });
    } else {
      clearErrors('data');
      trigger();
    }
  };

  const handleDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;

    if (over && active.id !== over.id) {
      const oldIndex = findIndex(fields, ['id', active.id]);
      const newIndex = findIndex(fields, ['id', over.id]);
      setValue('data', arrayMove(fields, oldIndex, newIndex), {
        shouldDirty: true,
      });
    }
  };

  const handleRemove = (index: number) => {
    fields[index] = undefined;
    setValue('data', compact(fields), {
      shouldDirty: true,
    });
    validateErrors();
  };

  const validateErrors = () => {
    let isValid = true;
    fields.every((validation: any) => {
      if (!validation && fields.length === 1) {
        isValid = false;
        return isValid;
      }
      if (!validation) return true;
      const keyToValidate = ['name'];
      keyToValidate.every((key) => {
        const checkSingleProperty = !!validation?.[key]?.length && validation?.[key]?.trim() !== '';
        if (!checkSingleProperty) {
          isValid = false;
        }
        return isValid;
      });
      return isValid;
    });
    setFormErrors(isValid);
  };

  const onChangeOptionLabel = (value: string, index: number) => {
    const trimmedValue = value.trim();
    fields[index] = {
      ...fields[index],
      name: trimmedValue,
    };
    setValue('data', fields, {
      shouldDirty: true,
    });
    validateErrors();
  };

  const onAddItem = () => {
    fields[fields.length] = { id: uuidv4(), name: '' };
    setValue('data', fields, {
      shouldDirty: true,
    });
    setFormErrors();
  };

  return (
    <CommonWrapper>
      {loadOptions ? (
        <LoadOptions
          isReadOnly={isReadOnly}
          fields={fields}
          setFormErrors={setFormErrors}
          validateErrors={validateErrors}
          handleDragEnd={handleDragEnd}
          sensors={sensors}
          handleRemove={handleRemove}
        />
      ) : (
        <>
          <ul className="list">
            <DndContext
              modifiers={[restrictToVerticalAxis]}
              sensors={sensors}
              collisionDetection={closestCenter}
              onDragEnd={handleDragEnd}
            >
              <SortableContext items={fields as any} strategy={verticalListSortingStrategy}>
                {fields.map((item, index) => {
                  return (
                    <SortableItem
                      key={item.id}
                      item={item}
                      index={index}
                      remove={handleRemove}
                      isEditable={!isReadOnly}
                      onChangeOptionLabel={onChangeOptionLabel}
                      isReadOnly={isReadOnly}
                    />
                  );
                })}
              </SortableContext>
            </DndContext>
          </ul>
          {!isReadOnly && (
            <div className="options-action">
              <AddNewItem
                onClick={() => {
                  onAddItem();
                }}
              />
              {fields?.length ? (
                <div
                  className="remove-options"
                  onClick={() => {
                    setValue('data', [], {
                      shouldDirty: true,
                    });
                    setFormErrors();
                  }}
                >
                  <img src={removeIcon} alt="remove" />
                  <span>Remove all Options</span>
                </div>
              ) : null}
            </div>
          )}
        </>
      )}
    </CommonWrapper>
  );
};

export default ChecklistParameter;
