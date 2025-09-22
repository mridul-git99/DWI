import { InputTypes } from '#utils/globalTypes';
import { debounce } from 'lodash';
import React, { useEffect, useMemo, useState, FC } from 'react';
import { CommonWrapper } from './styles';
import { FormGroup, LoadingContainer } from '#components';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { MandatoryParameter } from '#types';
import { request } from '#utils/request';
import { apiGetObjectTypes } from '#utils/apiUrls';
import { DndContext, DragEndEvent, closestCenter } from '@dnd-kit/core';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { SortableItem } from './Checklist';
import { useFormContext } from 'react-hook-form';
import { createFetchList } from '#hooks/useFetchData';
import { ObjectType, ObjectTypeProperty } from '#views/Ontology/types';

type TLoadOptions = {
  isReadOnly: boolean;
  fields: any[];
  setFormErrors: () => void;
  validateErrors: () => void;
  handleDragEnd: (event: DragEndEvent) => void;
  sensors: any;
  handleRemove: (index: number) => void;
};

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  sort: 'createdAt,desc',
  usageStatus: 1,
};

const LoadOptions: FC<TLoadOptions> = ({
  isReadOnly,
  fields,
  setFormErrors,
  validateErrors,
  handleDragEnd,
  sensors,
  handleRemove,
}) => {
  const { watch, setValue } = useFormContext();
  const metadata = watch('metadata');
  const type = watch('type');
  const [selectedObjectType, setSelectedObjectType] = useState<ObjectType | null>(null);
  const [selectedObjectProperty, setSelectedObjectProperty] = useState<ObjectTypeProperty | null>(
    null,
  );
  const [propertyOptions, setPropertyOptions] = useState<any[]>([]);
  const [reloading, setReloading] = useState<boolean>(false);

  const { list, reset, fetchNext, status } = createFetchList(apiGetObjectTypes(), urlParams, false);

  const parameterSelectType = useMemo(() => {
    return type === MandatoryParameter.MULTISELECT
      ? InputTypes.MULTI_SELECT
      : InputTypes.SINGLE_SELECT;
  }, [type]);

  const objectTypeOptions = useMemo(() => {
    return list.map((objectType) => ({
      ...objectType,
      label: objectType.displayName,
      value: objectType.id,
    }));
  }, [list]);

  const objectPropertyOptions = useMemo(
    () =>
      (selectedObjectType?.properties || propertyOptions)
        .filter(
          (property) => property.inputType === parameterSelectType && property?.usageStatus === 1,
        )
        .map((property) => ({
          _options: property?.options,
          label: property.displayName,
          value: property.id,
          externalId: property.externalId,
        })),
    [selectedObjectType, propertyOptions, parameterSelectType],
  );

  const reloadOptions = async (objectTypeId: string, propertyId: string) => {
    setReloading(true);
    const response = await request('GET', apiGetObjectTypes(objectTypeId));
    if (response?.data?.properties) {
      const selectedProperty = response.data.properties.find(
        (property) => property.id === propertyId,
      );
      const formattedOptions = selectedProperty?.options.map((option: any) => ({
        id: option.id,
        name: option.displayName,
      }));
      setValue('data', formattedOptions, {
        shouldDirty: true,
      });
      setReloading(false);
    } else {
      setReloading(false);
    }
    validateErrors();
  };

  const getPropertyOptions = async (id: string) => {
    const response = await request('GET', apiGetObjectTypes(id));
    if (response?.data?.properties) {
      setPropertyOptions(response.data.properties);
    }
  };

  useEffect(() => {
    if (metadata) {
      setSelectedObjectType({
        displayName: metadata.objectTypeDisplayName,
        id: metadata.objectTypeId,
        label: metadata.objectTypeDisplayName,
        value: metadata.objectTypeId,
        externalId: metadata.objectTypeExternalId,
        collection: metadata.collection,
      });
      setSelectedObjectProperty({
        label: metadata.propertyDisplayName,
        value: metadata.propertyId,
        externalId: metadata.propertyExternalId,
      });

      if (metadata?.objectTypeId && !isReadOnly) {
        getPropertyOptions(metadata.objectTypeId);
      }
    }
  }, []);

  return (
    <CommonWrapper>
      <FormGroup
        inputs={[
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'objectType',
              label: 'Object Type',
              isLoading: status === 'loading',
              onMenuScrollToBottom: () => {
                fetchNext();
              },
              options: objectTypeOptions,
              value: selectedObjectType,
              onMenuOpen: () => {
                if (status === 'init') {
                  reset({ params: urlParams });
                }
              },
              onInputChange: debounce((value: string, actionMeta: string) => {
                if (value !== actionMeta.prevInputValue) {
                  reset({ params: { ...urlParams, displayName: value ? value : null } });
                }
              }, 500),
              placeholder: 'Select',
              isDisabled: isReadOnly,
              onChange: (value: any) => {
                setSelectedObjectType(value);
                setSelectedObjectProperty(null);
                setFormErrors();
                setValue('metadata', null);
                setValue('validations', [], {
                  shouldDirty: true,
                  shouldValidate: true,
                });
              },
            },
          },
          {
            type: InputTypes.SINGLE_SELECT,
            props: {
              id: 'objectProperty',
              label: 'Object Property',
              options: objectPropertyOptions,
              value: selectedObjectProperty,
              placeholder: 'Select',
              isDisabled: isReadOnly || !selectedObjectType,
              onChange: (value: any) => {
                setSelectedObjectProperty(value);
                if (value?._options) {
                  reloadOptions(selectedObjectType?.id, value?.value);
                  setValue('metadata', {
                    objectTypeId: selectedObjectType?.id,
                    objectTypeDisplayName: selectedObjectType?.displayName,
                    objectTypeExternalId: selectedObjectType?.externalId,
                    collection: selectedObjectType?.collection,
                    propertyId: value?.value,
                    propertyDisplayName: value?.label,
                    propertyExternalId: value?.externalId,
                  });
                  validateErrors();
                }
              },
            },
          },
        ]}
      />
      {selectedObjectProperty && (
        <>
          <p className="options-list-title">
            Options{' '}
            {!isReadOnly && (
              <span
                style={{ color: '#1D84FF', cursor: 'pointer' }}
                onClick={() => {
                  reloadOptions(metadata?.objectTypeId, metadata?.propertyId);
                }}
              >
                Reload Options
              </span>
            )}
          </p>
          {fields.length ? (
            <LoadingContainer
              loading={reloading}
              component={
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
                            isEditable={false}
                            isReadOnly={isReadOnly}
                            remove={handleRemove}
                          />
                        );
                      })}
                    </SortableContext>
                  </DndContext>
                </ul>
              }
            />
          ) : null}
        </>
      )}
    </CommonWrapper>
  );
};

export default LoadOptions;
