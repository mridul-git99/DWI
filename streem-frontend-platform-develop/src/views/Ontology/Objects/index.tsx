import { Avatar, Button, CardWithTitle, LoadingContainer, StyledTabs } from '#components';
import { useTypedSelector } from '#store';
import { InputTypes } from '#utils/globalTypes';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { LabelValueRow } from '#views/Job/components/Header/styles';
import KeyboardArrowLeftOutlinedIcon from '@material-ui/icons/KeyboardArrowLeftOutlined';
import { RouteComponentProps, navigate } from '@reach/router';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { fetchObject, fetchObjectType, resetOntology } from '../actions';
import { Choice, ObjectTypeProperty } from '../types';
import AuditLogTabContent from './TabContent';
import AddEditObjectDrawer from './components/AddEditObjectDrawer';
import ProcessTabContent from './components/ProcessTabContent';
import RelationTabContent from './components/RelationTabContent';
import ObjectJobLogsContainer from './components/ObjectJobLogsContainer';
import JobsListView from './JobsListView';

const ObjectViewWrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;

  .object-header {
    padding: 12px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background-color: #fff;
    border-bottom: 1px solid #e0e0e0;

    .object-header-section {
      display: flex;
      &.heading {
        display: flex;
        gap: 8px;
        align-items: center;
        cursor: pointer;
      }
      &.left {
        flex-direction: column;
        gap: 4px;
        .meta-info {
          display: flex;
          gap: 8px;
          align-items: center;
        }
        h4 {
          font-weight: 700;
          font-size: 16px;
          line-height: 20px;
          color: #161616;
          margin: 0;
        }
        span {
          font-size: 12px;
          line-height: 12px;
          text-align: center;
          letter-spacing: 0.32px;
          color: #525252;
        }
        .badge {
          padding: 4px 8px;
          background: #d0e2ff;
          color: #0043ce;
          font-size: 12px;
          line-height: 16px;
        }
      }
      &.right {
      }
    }
  }

  .object-tabs {
    display: flex;
    overflow: hidden;

    .object-tabs-list {
      padding: 16px;
      background-color: #fff;
      min-width: 200px;
      border-right: 1px solid #e0e0e0;
    }

    .MuiTabs-flexContainer {
      border-bottom: none;
      button {
        border-bottom: 1px solid #e0e0e0;
        :last-of-type {
          border-bottom: none;
        }

        &.Mui-selected {
          background: #e7f1fd;
          .MuiTab-wrapper {
            font-weight: 400;
          }
        }
      }
    }

    .MuiTabs-indicator {
      left: 0;
      right: unset;
    }
  }

  .object-tabs-panel {
    padding: 16px 16px 0 16px;
    overflow: auto;
  }

  .relation-tab-empty {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    color: #6f6f6f;
  }

  .overview-tab {
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  .MuiTableRow-root {
    background-color: #fff !important;
  }
`;

const OverViewTabContent = () => {
  const {
    ontology: {
      objects: { active: selectedObject },
      objectTypes: { active: selectedObjectType },
    },
  } = useTypedSelector((state) => state);

  const objectPropertiesMapByExternalId = selectedObject?.properties?.reduce<
    Record<string, string | Choice[] | undefined>
  >((acc, property) => {
    acc[property.externalId] = property.choices?.length ? property.choices : property.value;
    return acc;
  }, {});

  const propertyToValue = (property: ObjectTypeProperty) => {
    let propertyValue = objectPropertiesMapByExternalId?.[property.externalId];
    if (propertyValue) {
      if (Array.isArray(propertyValue)) {
        propertyValue = propertyValue.map((option) => option.displayName).join(', ');
      } else {
        if ([InputTypes.DATE, InputTypes.TIME, InputTypes.DATE_TIME].includes(property.inputType)) {
          propertyValue = formatDateTime({ value: propertyValue, type: property.inputType });
        }
      }
    } else {
      propertyValue = '-';
    }
    return propertyValue;
  };

  return (
    <div className="overview-tab">
      <CardWithTitle>
        <LabelValueRow style={{ padding: '8px 16px', justifyContent: 'space-between' }}>
          {[...(selectedObjectType?.properties || [])]
            .sort((a, b) => a.sortOrder - b.sortOrder)
            .reduce((acc: any, property) => {
              if (!['displayName', 'externalId'].includes(property.externalId) && acc.length < 4) {
                if (['createdBy', 'updatedBy'].includes(property.externalId)) {
                  let user;
                  property.externalId === 'createdBy'
                    ? (user = selectedObject?.createdBy)
                    : (user = selectedObject?.modifiedBy);
                  acc.push(
                    <div className="card-item" key={property.id}>
                      <label className="info-item-label">{property.displayName}</label>
                      <div
                        className="info-item-value"
                        style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                      >
                        <Avatar user={user} allowMouseEvents={false} size="small" />
                        <div className="info-item-value">
                          <span className="info-item-value">{user.employeeId}</span>
                          <span className="info-item-value">{getFullName(user)}</span>
                        </div>
                      </div>
                    </div>,
                  );
                } else {
                  acc.push(
                    <div className="card-item" key={property.id}>
                      <label className="info-item-label">{property.displayName}</label>
                      <span className="info-item-value">{propertyToValue(property)}</span>
                    </div>,
                  );
                }
              }
              return acc;
            }, [])}
        </LabelValueRow>
      </CardWithTitle>
      <JobsListView />
    </div>
  );
};

const ObjectsContent = ({
  id,
  objectTypeId,
}: RouteComponentProps<{ id: string; objectTypeId: string }>) => {
  const dispatch = useDispatch();
  const [showAddEditObjectDrawer, setShowAddEditObjectDrawer] = useState(false);
  const {
    objects: { active: selectedObject, activeLoading: loadingObject },
    objectTypes: { active: selectedObjectType },
  } = useTypedSelector((state) => state.ontology);

  useEffect(() => {
    return () => {
      dispatch(resetOntology(['objects', 'activeLoading']));
    };
  }, []);

  useEffect(() => {
    if (objectTypeId) {
      dispatch(fetchObjectType(objectTypeId));
    }
  }, [id]);

  useEffect(() => {
    if (
      selectedObjectType &&
      selectedObjectType.externalId &&
      id &&
      selectedObjectType.id === objectTypeId
    ) {
      dispatch(fetchObject(id, { collection: selectedObjectType.externalId }));
    }
  }, [selectedObjectType]);

  return (
    <LoadingContainer
      loading={loadingObject}
      component={
        <ObjectViewWrapper>
          <div className="object-header">
            <div className="object-header-section left">
              <div style={{ display: 'flex' }}>
                <div className="object-header-section heading">
                  <KeyboardArrowLeftOutlinedIcon onClick={() => navigate(-1)} />
                  <h4>{selectedObject?.displayName}</h4>
                </div>
              </div>
              <div className="meta-info">
                <div className="badge">
                  {selectedObject?.usageStatus === 1 ? 'Active' : 'Inactive'}
                </div>
                <span>ID: {selectedObject?.externalId}</span>
                {selectedObject?.createdAt && (
                  <span>
                    Created Date:{' '}
                    {formatDateTime({ value: selectedObject!.createdAt, type: InputTypes.DATE })}
                  </span>
                )}
              </div>
            </div>
            <div className="object-header-section right">
              <Button variant="secondary" onClick={() => setShowAddEditObjectDrawer(true)}>
                View Properties
              </Button>
            </div>
          </div>
          <StyledTabs
            containerProps={{
              className: 'object-tabs',
            }}
            tabListProps={{
              className: 'object-tabs-list',
              orientation: 'vertical',
            }}
            panelsProps={{
              className: 'object-tabs-panel',
            }}
            tabs={[
              {
                value: '0',
                label: 'Overview',
                panelContent: <OverViewTabContent />,
              },
              {
                value: '1',
                label: 'Relations',
                panelContent: <RelationTabContent />,
              },
              {
                value: '2',
                label: 'Process',
                panelContent: <ProcessTabContent />,
              },
              {
                value: '3',
                label: 'Audit Logs',
                panelContent: <AuditLogTabContent />,
              },
              {
                value: '4',
                label: 'Job Logs',
                panelContent: <ObjectJobLogsContainer />,
              },
            ]}
            queryString="objectOverviewTab"
          />
          {selectedObject && selectedObjectType && showAddEditObjectDrawer && (
            <AddEditObjectDrawer
              onCloseDrawer={setShowAddEditObjectDrawer}
              values={{
                objectTypeId: selectedObjectType.id,
                id: selectedObject.id,
              }}
            />
          )}
        </ObjectViewWrapper>
      }
    />
  );
};

export default ObjectsContent;
