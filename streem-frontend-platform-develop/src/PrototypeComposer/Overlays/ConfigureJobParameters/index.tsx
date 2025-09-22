import { processParametersMapSuccess } from '#PrototypeComposer/actions';
import { ParameterVerificationTypeEnum } from '#PrototypeComposer/checklist.types';
import { BaseModal, Checkbox, TextInput } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import { useTypedSelector } from '#store';
import { MandatoryParameter, TargetEntityType } from '#types';
import { apiBatchMapParameters, apiGetParameters } from '#utils/apiUrls';
import { DEFAULT_PAGINATION } from '#utils/constants';
import { FilterField, FilterOperators, Pageable, ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
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
import { Clear, DeleteOutlined, DragIndicator, Search } from '@material-ui/icons';
import { debounce, findIndex, orderBy } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    width: 60dvw;
    padding: 0;

    &-body {
      display: flex !important;
      padding: 0 !important;

      .body {
        display: flex;

        .section {
          flex-direction: column;
          display: flex;
          flex: 1;
          padding: 16px;
          overflow: auto;

          &-left {
            border-right: 1px solid #f4f4f4;

            .input {
              flex: unset;
              margin-bottom: 16px;
            }

            span {
              font-size: 14px;

              &.info {
                margin-bottom: 16px;
              }
            }

            .checkbox-input {
              padding-block: 24px 12px;
              border-top: 1px solid #d9d9d9;

              .container {
                text-align: left;
                font-size: 14px;
                color: #000;
              }
            }
          }

          &-right {
            h4 {
              font-size: 14px;
              font-weight: 600;
              margin: 0;
            }
            .info {
              font-size: 12px;
              margin-block: 16px 8px;
              align-self: flex-start;
              text-align: left;
            }

            .parameter-wrapper {
              display: flex;
              align-items: center;
              background-color: #fff;

              .content {
                border-bottom: 1px solid #cccccc;
                display: flex;
                align-items: center;
                padding: 12px;
                flex: 1;

                span {
                  color: rgba(0, 0, 0, 0.45);
                }
              }
            }
          }
        }
      }
    }

    &-footer {
      justify-content: flex-end;
    }
  }
`;

type Props = {
  checklistId: string;
};

const defaultParameters = ['Process ID', 'Process Name', 'Job ID'];

const ConfigureJobParameters: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { checklistId },
}) => {
  const dispatch = useDispatch();
  const { data: checklist } = useTypedSelector((state) => state.prototypeComposer);
  const { parameters = [] } = checklist!;
  const [items, setItems] = useState<any[]>(orderBy(parameters, ['orderTree'], ['asc']));
  const [allItems, setAllItems] = useState<Record<string, any>>({});
  const [isLoading, setLoading] = useState(false);
  const [pagination, setPagination] = useState<Pageable>(DEFAULT_PAGINATION);

  const [filterFields, setFilterFields] = useState<FilterField[]>([
    { field: 'targetEntityType', op: FilterOperators.EQ, values: [TargetEntityType.UNMAPPED] },
    { field: 'label', op: FilterOperators.LIKE, values: [''] },
    { field: 'archived', op: FilterOperators.EQ, values: [false] },
    {
      field: 'type',
      op: FilterOperators.ANY,
      values: Object.values(MandatoryParameter).filter(
        (type) =>
          ![
            MandatoryParameter.CHECKLIST,
            MandatoryParameter.MEDIA,
            MandatoryParameter.FILE_UPLOAD,
            MandatoryParameter.SIGNATURE,
            MandatoryParameter.CALCULATION,
            MandatoryParameter.SHOULD_BE,
            MandatoryParameter.YES_NO,
          ].includes(type),
      ),
    },
  ]);

  const searchInput = useRef<HTMLInputElement | null>(null);

  const parseDataToState = (data: any[], _pagination: Pageable) => {
    let selectedParameters = items.reduce((acc, parameter) => {
      if (searchInput.current && searchInput.current.value) {
        if (parameter.label.toLowerCase().search(searchInput.current.value.toLowerCase()) === -1)
          return acc;
      }
      acc[parameter.id] = {
        ...parameter,
        checked: true,
      };
      return acc;
    }, {});

    selectedParameters = parameters.reduce((acc, parameter) => {
      if (searchInput.current && searchInput.current.value) {
        if (parameter.label.toLowerCase().search(searchInput.current.value.toLowerCase()) === -1)
          return acc;
      }
      if (!selectedParameters?.[parameter.id])
        acc[parameter.id] = {
          ...parameter,
          checked: false,
        };
      return acc;
    }, selectedParameters);

    const allParameters = data.reduce((acc, parameter) => {
      if (!selectedParameters?.[parameter.id])
        acc[parameter.id] = {
          ...parameter,
          checked: false,
        };
      return acc;
    }, selectedParameters);

    if (_pagination.page === 0) {
      setAllItems(allParameters);
    } else {
      setAllItems((prev) => ({ ...prev, ...allParameters }));
    }
    setPagination(_pagination);
  };

  const fetchData = async (pageNumber?: number) => {
    if (checklistId) {
      setLoading(true);
      try {
        const { data, pageable }: ResponseObj<any[]> = await request(
          'GET',
          apiGetParameters(checklistId),
          {
            params: {
              page: typeof pageNumber === 'number' ? pageNumber : pagination.page + 1,
              size: pagination.pageSize,
              filters: {
                op: FilterOperators.AND,
                fields: filterFields,
              },
            },
          },
        );
        if (data && pageable) {
          parseDataToState(data, pageable);
        }
      } catch (e) {
        console.error('Error While Fetching Parameters', e);
      }
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData(0);
  }, [filterFields]);

  const sensors = useSensors(useSensor(PointerSensor));

  const onPrimary = async () => {
    const payload = {
      mappedParameters: items.reduce((acc, item, index) => {
        acc[item.id] = index + 1;
        return acc;
      }, {}),
    };
    const { data, errors }: ResponseObj<any[]> = await request(
      'PATCH',
      apiBatchMapParameters(checklistId),
      {
        data: payload,
      },
    );
    if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
    if (data) {
      dispatch(processParametersMapSuccess(data, payload));
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Action Performed Successfully',
        }),
      );
    }
    handleClose();
  };

  const handleClose = () => {
    closeOverlay();
  };

  const handleDragEnd = (e: DragEndEvent) => {
    const { active, over } = e;

    if (over && active.id !== over.id) {
      setItems((items) => {
        const oldIndex = findIndex(items, ['id', active.id]);
        const newIndex = findIndex(items, ['id', over.id]);
        return arrayMove(items, oldIndex, newIndex);
      });
    }
  };

  const handleOnScroll = (e: React.UIEvent<HTMLElement>) => {
    e.stopPropagation();
    const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
    if (
      !isLoading &&
      scrollTop + clientHeight >= scrollHeight - clientHeight * 0.7 &&
      !pagination.last
    ) {
      fetchData();
    }
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={handleClose}
        title="Configure ‘Create Job’ form"
        secondaryText="Cancel"
        primaryText="Save"
        onSecondary={handleClose}
        onPrimary={onPrimary}
      >
        <div className="body">
          <div className="section section-left" onScroll={handleOnScroll}>
            <TextInput
              BeforeElement={Search}
              AfterElement={Clear}
              ref={searchInput}
              afterElementClick={() => {
                if (searchInput.current) {
                  searchInput.current.value = '';
                }
                setFilterFields((prev) =>
                  prev.map((field) => {
                    if (field.field === 'label') {
                      return { ...field, values: [''] };
                    }
                    return field;
                  }),
                );
              }}
              afterElementClass="clear"
              afterElementWithoutError
              name="search-filter"
              onChange={debounce(
                ({ value }) =>
                  setFilterFields((prev) =>
                    prev.map((field) => {
                      if (field.field === 'label') {
                        return { ...field, values: [value] };
                      }
                      return field;
                    }),
                  ),
                500,
              )}
              placeholder="Search Parameter Name"
              autoComplete="off"
            />
            <span className="info">Select the parameters to add to the ‘Create job’ form</span>
            <span className="info">
              <strong>Note:</strong> Parameters that you want to initialize before a job starts
              should be included in the ‘Create job’ form
            </span>
            {Object.entries(allItems).map(([key, item]) => {
              return (
                <Tooltip
                  title={
                    'Parameter cannot be added to Create Job Form because verifications are enabled. Disable the verification to add them to Create Job Form'
                  }
                  arrow
                  placement="right"
                  key={key}
                  disableHoverListener={
                    item?.verificationType !== ParameterVerificationTypeEnum.NONE ? false : true
                  }
                >
                  <span>
                    <Checkbox
                      key={key}
                      checked={item.checked}
                      label={item.label}
                      onClick={(checked) => {
                        if (checked) {
                          setItems((prev) => [...prev, item]);
                        } else {
                          setItems((prev) => prev.filter((i) => i.id !== key));
                        }
                        setAllItems((prev) => ({
                          ...prev,
                          [key]: { ...prev[key], checked },
                        }));
                      }}
                      disabled={item?.verificationType !== ParameterVerificationTypeEnum.NONE}
                    />
                  </span>
                </Tooltip>
              );
            })}
          </div>
          <div className="section section-right">
            <h4>Selected Parameters</h4>
            <span className="info">
              You can rearrange this selected columns (Cannot edit Default columns.)
            </span>
            {defaultParameters.map((defaultParameter) => (
              <div className="parameter-wrapper" key={defaultParameter}>
                <div className="content">
                  <span>{defaultParameter}</span>
                </div>
              </div>
            ))}
            <span className="info">
              You can drag and drop the parameters below to define the sequence of the parameters,
              as they should show up in the ‘Create job’ form
            </span>
            <DndContext
              sensors={sensors}
              collisionDetection={closestCenter}
              onDragEnd={handleDragEnd}
              modifiers={[restrictToVerticalAxis]}
            >
              <SortableContext items={items} strategy={verticalListSortingStrategy}>
                {items.map((item) => (
                  <SortableItem
                    key={item.id}
                    item={item}
                    setItems={setItems}
                    setAllItems={setAllItems}
                  />
                ))}
              </SortableContext>
            </DndContext>
          </div>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default ConfigureJobParameters;

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

    svg {
      color: #999999;
      outline: none;
      margin-right: 8px;
    }
  }

  .action {
    padding: 0px 4px;
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

export function SortableItem({
  item,
  setItems,
  setAllItems,
}: {
  item: any;
  setItems: React.Dispatch<React.SetStateAction<any[]>>;
  setAllItems: React.Dispatch<React.SetStateAction<Record<string, any>>>;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: item.id,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <SortableItemWrapper ref={setNodeRef} style={style} className={isDragging ? 'dragging' : ''}>
      <div className="content">
        <DragIndicator {...attributes} {...listeners} />
        <span>{item.label}</span>
      </div>
      <div className="action">
        <DeleteOutlined
          onClick={() => {
            setItems((prev) => prev.filter((i) => i.id !== item.id));
            setAllItems((prev) => ({
              ...prev,
              [item.id]: { ...prev[item.id], checked: false },
            }));
          }}
        />
      </div>
    </SortableItemWrapper>
  );
}
