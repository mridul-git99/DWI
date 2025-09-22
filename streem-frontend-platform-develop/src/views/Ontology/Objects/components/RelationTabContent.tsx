import { CardWithTitle, DataTable, LoadingContainer } from '#components';
import { useTypedSelector } from '#store';
import { apiGetObjectTypes } from '#utils/apiUrls';
import { request } from '#utils/request';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { ObjectType } from '#views/Ontology/types';
import React, { useEffect, useState } from 'react';

const getColumnsByRelation = (relation: ObjectType) => {
  let displayName: string = '',
    externalId: string = '';
  relation?.properties?.every((property) => {
    if (property.externalId === 'displayName') {
      displayName = property?.displayName;
      return !externalId;
    }
    if (property.externalId === 'externalId') {
      externalId = property?.displayName;
      return !displayName;
    }
    return true;
  });
  return [
    {
      id: 'name',
      label: 'Relation Name',
      minWidth: 240,
    },
    {
      id: 'displayName',
      label: displayName,
      minWidth: 152,
    },
    {
      id: 'externalId',
      label: externalId,
      minWidth: 152,
    },
  ];
};

const RelationTabContent = () => {
  const {
    ontology: {
      objects: { active: selectedObject },
    },
  } = useTypedSelector((state) => state);

  const [relationsData, setRelationsData] = useState<Record<string, ObjectType>>(
    selectedObject?.relations?.length ? {} : {},
  );

  const fetchData = async (type: string) => {
    try {
      const { data } = await request('GET', apiGetObjectTypes(type));

      if (data) {
        setRelationsData((prev) => ({
          ...(prev || {}),
          [type]: data,
        }));
      }
    } catch (error) {
      console.error('error from fetch object type in object view :: ', error);
    }
  };

  useEffect(() => {
    if (selectedObject?.relations?.length) {
      selectedObject.relations.forEach((relation) => {
        fetchData(relation.objectTypeId);
      });
    }
  }, []);

  return (
    <LoadingContainer
      loading={!relationsData}
      component={
        Object.keys(relationsData).length > 0 ? (
          <div className="relation-tab">
            {selectedObject?.relations?.map((relation) => {
              const relationObjectType = relationsData?.[relation?.objectTypeId];
              return (
                <CardWithTitle>
                  <h4 className="card-label">{relationObjectType?.displayName}</h4>
                  <TabContentWrapper>
                    <div style={{ padding: 16 }}>
                      <DataTable
                        columns={getColumnsByRelation(relationObjectType)}
                        rows={relation?.targets?.map((target) => {
                          return {
                            ...target,
                            name: relation?.displayName,
                          };
                        })}
                        emptyTitle="No Relations Found"
                      />
                    </div>
                  </TabContentWrapper>
                </CardWithTitle>
              );
            })}
          </div>
        ) : (
          <div className="relation-tab-empty">No Relations Objects</div>
        )
      }
    />
  );
};

export default RelationTabContent;
