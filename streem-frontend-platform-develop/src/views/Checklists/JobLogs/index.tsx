import { fetchJobLogColumns, resetComposer } from '#PrototypeComposer/actions';
import { Button, GeneralHeader, Select } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import useTabs from '#components/shared/useTabs';
import checkPermission from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { validateNumber } from '#utils';
import { CustomViewsTargetType, FilterOperators } from '#utils/globalTypes';
import { ViewWrapper } from '#views/Jobs/ListView/styles';
import { DeleteOutlineOutlined, Edit } from '@material-ui/icons';
import { RouteComponentProps, navigate } from '@reach/router';
import React, { FC, useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { OptionProps, components } from 'react-select';
import styled from 'styled-components';
import {
  addCustomView,
  deleteCustomView,
  getCustomViews,
  saveCustomView,
} from '../ListView/actions';
import DynamicContent from './DynamicContent';

const AfterHeaderWrapper = styled.div`
  display: flex;
  padding: 2px 0px 8px 8px;
  gap: 8px;
  min-width: fit-content;
  flex: 1;
  justify-content: space-between;

  .custom-select__menu-list {
    position: fixed;
    box-shadow: 0 1px 10px 0 rgba(0, 0, 0, 0.12), 0 4px 5px 0 rgba(0, 0, 0, 0.14),
      0 2px 4px -1px rgba(0, 0, 0, 0.2);
  }
`;

const OptionWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  text-transform: capitalize;
  width: 110px;

  .more-views {
    flex-grow: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .delete-icon {
    margin: 0;
    color: #161616;
    :hover {
      color: #ff6b6b;
    }
  }
`;

const AfterHeader: FC<any> = ({ setActiveTab, activeTab, checklistId, defaultView }) => {
  const dispatch = useDispatch();
  const {
    prototypeComposer: { data: processData },
    checklistListView: { customViews },
  } = useTypedSelector((state) => state);

  const handleSetActiveTab = (view: any) => {
    const { index } = view;
    setActiveTab(() => {
      navigate(
        `?tab=${validateNumber(index) ? index : Object.keys(customViews.views).length + 1}`,
        { replace: true },
      );
      return {
        id: view.id,
        label: view.label,
        tabContent: DynamicContent,
        values: { id: view.id, checklistId },
      };
    });
  };

  const onPrimary = (data: any) => {
    dispatch(
      addCustomView({
        data: {
          ...data,
          columns: (processData?.jobLogColumns || []).map((column: any, i: number) => ({
            ...column,
            orderTree: i + 1,
          })),
          filters: [
            {
              key: 'checklistId',
              constraint: FilterOperators.EQ,
              value: [checklistId],
            },
          ],
        },
        setActiveTab: handleSetActiveTab,
        checklistId,
      }),
    );
  };

  const handleAddNew = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.PUT_CUSTOM_VIEW,
        props: {
          onPrimary,
        },
      }),
    );
  };

  const onClickDeleteView = (e: React.MouseEvent<SVGSVGElement, MouseEvent>, view: any) => {
    e.stopPropagation();
    dispatch(
      openOverlayAction({
        type: OverlayNames.CONFIRMATION_MODAL,
        props: {
          onPrimary: () => handleDeleteView(view),
          title: 'Delete View',
          body: `Are you sure you want to delete “${view.label}” view?`,
        },
      }),
    );
  };

  const handleDeleteView = (view: any) => {
    const shouldDecrease = activeTab.id === view.id || activeTab.index > view.index;
    dispatch(
      deleteCustomView({
        view,
        tabIndex: shouldDecrease ? activeTab.index - 1 : activeTab.index,
      }),
    );
  };

  const onClickEditLabel = (e: React.MouseEvent<SVGSVGElement, MouseEvent>, view: any) => {
    e.stopPropagation();
    dispatch(
      openOverlayAction({
        type: OverlayNames.PUT_CUSTOM_VIEW,
        props: {
          onPrimary: (data: any) => handleEditLabel(view, data.label),
          isEdit: true,
          view,
        },
      }),
    );
  };

  const handleEditLabel = (view: any, newLabel: string) => {
    dispatch(
      saveCustomView({
        data: { ...view, label: newLabel },
        viewId: view.id,
      }),
    );
  };

  const Option = (props: OptionProps<any>) => {
    return (
      <components.Option {...props}>
        <OptionWrapper>
          <div className="more-views" title={props.label}>
            {props.label}
          </div>
          {!!props.data.id && checkPermission(['jobLogsViews', 'edit']) && (
            <>
              <Edit onClick={(e) => onClickEditLabel(e, props.data)} />
              <DeleteOutlineOutlined
                className="delete-icon"
                onClick={(e) => onClickDeleteView(e, props.data)}
              />
            </>
          )}
        </OptionWrapper>
      </components.Option>
    );
  };

  const options = useMemo(
    () => [
      defaultView,
      ...Object.values(customViews.views).map((view: any, i) => ({ ...view, index: i + 1 })),
    ],
    [customViews.views, defaultView],
  );

  return (
    <AfterHeaderWrapper>
      {checkPermission(['jobLogsViews', 'create']) ? (
        <Button variant="secondary" onClick={handleAddNew}>
          Add New
        </Button>
      ) : null}
      <Select
        options={options}
        value={[{ value: 'more views', label: 'More Views' }]}
        components={{ Option }}
        onChange={handleSetActiveTab}
      />
    </AfterHeaderWrapper>
  );
};

const JobLogs: FC<any> = ({ id, views }) => {
  const { renderTabHeader, renderTabContent } = useTabs({
    tabs: [
      {
        id: 0,
        label: 'Default',
        tabContent: DynamicContent,
        values: { checklistId: id },
        index: 0,
      },
      ...Object.values(views).map((view: any, i) => ({
        id: view.id,
        label: view.label,
        tabContent: DynamicContent,
        values: { id: view.id, checklistId: id },
        index: i + 1,
      })),
    ],
    AfterHeader: {
      Component: AfterHeader,
      props: {
        checklistId: id,
        defaultView: {
          id: 0,
          label: 'Default',
          tabContent: DynamicContent,
          values: { checklistId: id },
          index: 0,
        },
      },
    },
    useTabIndexFromQuery: true,
    indicatorForActiveTab: 'id',
    showTooltip: true,
  });

  return (
    <ViewWrapper>
      <GeneralHeader heading="Job Logs" subHeading="View your Job Logs" />
      <div className="list-table">
        {renderTabHeader()}
        {renderTabContent()}
      </div>
    </ViewWrapper>
  );
};

const JobLogsContainer: FC<RouteComponentProps<{ id: string }>> = ({ id }) => {
  const dispatch = useDispatch();
  const views = useTypedSelector((state) => state.checklistListView.customViews.views);
  const loading = useTypedSelector((state) => state.checklistListView.customViews.loading);
  const jobLogColumnsLoading = useTypedSelector(
    (state) => state.prototypeComposer.jobLogColumnsLoading,
  );

  useEffect(() => {
    if (id) {
      dispatch(fetchJobLogColumns(id));
      dispatch(
        getCustomViews({
          filters: {
            op: FilterOperators.AND,
            fields: [
              { field: 'archived', op: FilterOperators.EQ, values: [false] },
              {
                field: 'targetType',
                op: FilterOperators.EQ,
                values: [CustomViewsTargetType.PROCESS],
              },
              { field: 'processId', op: FilterOperators.EQ, values: [id] },
            ],
          },
        }),
      );
    }
    return () => {
      dispatch(resetComposer());
    };
  }, []);

  if (loading || jobLogColumnsLoading) {
    return null;
  }

  return <JobLogs id={id} views={views} />;
};
export default JobLogsContainer;
