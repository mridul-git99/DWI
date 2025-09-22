import { AssigneeList, Avatar, Button, LoadingContainer, StatusTag, useDrawer } from '#components';
import { apiSingleParameter } from '#utils/apiUrls';
import { request } from '#utils/request';
import { formatDateTime } from '#utils/timeUtils';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import React, { FC, useEffect, useState } from 'react';
import styled from 'styled-components';
import { getParameterValueView } from './CorrectorView';

const CorrectionInfoDrawerWrapper = styled.div`
  padding: 0px 8px;
  width: 100%;

  .loading-container-wrapper {
    justify-content: center;
  }
  .correction-summary {
    margin-bottom: 16px;
    border-bottom: 1.5px solid #e0e0e0;

    :last-child {
      border-bottom: none;
    }
    h4 {
      font-size: 14px;
      font-weight: bold;
      line-height: 1.14;
      letter-spacing: 0.16px;
      color: #161616;
      margin-block: 16px;
    }
    .read-only-group {
      padding: 0;
      gap: 8px;
      .read-only {
        margin-bottom: 16px;
        flex-direction: column;
        .content {
          ::before {
            display: none;
          }
          font-size: 12px;
          line-height: 1.33;
          letter-spacing: 0.32px;
          color: #525252;

          :last-child {
            font-size: 14px;
            line-height: 1.14;
            letter-spacing: 0.16px;
            color: #161616;
            padding-top: 4px;
          }
        }
      }
    }

    .read-only-group-media > .read-only > .content:last-child {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .media-list-item {
      color: #1d84ff;
      font-size: 14px;
      cursor: pointer;
    }
  }

  .file-links {
    display: flex;
    flex-direction: column;
    gap: 4px;
    a {
      margin-right: 8px;
    }
    div {
      color: #1d84ff;
      margin-right: 8px;
      cursor: pointer;
    }
    > div > span {
      color: #1d84ff;
    }
  }

  .assignments {
    margin-left: 0px;
  }
`;

export const CorrectionInfoDrawer: FC<{
  correction: any;
  onCloseDrawer: any;
}> = ({ onCloseDrawer, correction }) => {
  const [parameter, setParameter] = useState<any>({ parameterData: null, loading: true });
  const { parameterData, loading: loadingParameter } = parameter;
  const fetchParameter = async () => {
    setParameter({ parameterData: null, loading: true });
    try {
      const response = await request('GET', apiSingleParameter(correction.parameterId!));
      if (response.data) {
        setParameter({ parameterData: response.data, loading: false });
      }
    } catch (e) {
      console.error('Error Fetching Correction Info Parameter', e);
    }
  };
  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const handleCloseDrawer = () => {
    setDrawerOpen(false);

    setTimeout(() => {
      onCloseDrawer(undefined);
    }, 200);
  };

  useEffect(() => {
    if (correction.parameterId) {
      fetchParameter();
    }
  }, [correction.parameterId]);

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Correction Info',
    hideCloseIcon: true,
    bodyContent: (
      <CorrectionInfoDrawerWrapper>
        <LoadingContainer
          loading={loadingParameter}
          component={
            <div>
              <div className="correction-summary">
                <h4>Process Information</h4>
                <ReadOnlyGroup
                  className="read-only-group"
                  items={[
                    {
                      label: 'Correction ID',
                      value: correction.code,
                    },
                    {
                      label: 'Process Name',
                      value: correction.processName,
                    },
                    {
                      label: 'Task Name',
                      value: correction.taskName,
                    },
                    {
                      label: 'Status',
                      value: <StatusTag status={correction.status} />,
                    },
                    {
                      label: 'Job ID',
                      value: correction.jobCode,
                    },
                    ,
                  ]}
                />
              </div>
              <div className="correction-summary">
                <h4>Parameter Information</h4>
                <ReadOnlyGroup
                  className="read-only-group"
                  items={[
                    {
                      label: 'Parameter Name',
                      value: correction.parameterName,
                    },
                    {
                      label: 'Parameter Old Value',
                      value: getParameterValueView({
                        ...parameterData,
                        response: {
                          value: correction.oldValue,
                          choices: correction.oldChoices,
                          medias: correction.oldMedias,
                        },
                      }),
                    },
                    {
                      label: 'Parameter New Value',
                      value: getParameterValueView({
                        ...parameterData,
                        response: {
                          value: correction.newValue,
                          choices: correction.newChoices,
                          medias: correction.newMedias,
                        },
                      }),
                    },
                    {
                      label: 'Date of Initiation',
                      value: formatDateTime({ value: correction.createdAt }),
                    },
                    {
                      label: 'Initiator',
                      value: (
                        <Avatar
                          user={correction?.createdBy}
                          color="blue"
                          backgroundColor="#F4F4F4"
                          borderColor="#FFFFFF"
                        />
                      ),
                    },
                    {
                      label: 'Initiator Remarks',
                      value: correction.initiatorsReason,
                    },
                    {
                      label: 'Correctors',
                      value: <AssigneeList users={correction.corrector.map((curr) => curr.user)} />,
                    },
                    ...(correction.correctorsReason
                      ? [
                          {
                            label: 'Corrector Remarks',
                            value: correction.correctorsReason,
                          },
                        ]
                      : []),
                    {
                      label: 'Reviewers',
                      value: <AssigneeList users={correction.reviewer.map((curr) => curr.user)} />,
                    },
                    ...(correction?.reviewersReason
                      ? [
                          {
                            label: 'Reviewer Remarks',
                            value: correction.reviewersReason,
                          },
                        ]
                      : []),
                  ]}
                />
              </div>
            </div>
          }
        />
      </CorrectionInfoDrawerWrapper>
    ),
    footerContent: (
      <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={handleCloseDrawer}>
        Cancel
      </Button>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return <CorrectionInfoDrawerWrapper>{StyledDrawer}</CorrectionInfoDrawerWrapper>;
};
