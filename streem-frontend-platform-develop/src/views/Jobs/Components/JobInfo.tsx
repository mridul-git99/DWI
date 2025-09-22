import { Media } from '#PrototypeComposer/checklist.types';
import {
  AssigneeList,
  Button,
  CustomTag,
  LoadingContainer,
  StatusTag,
  StyledTabs,
  useDrawer,
} from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { isFeatureAllowed } from '#services/uiPermissions';
import { Stage } from '#types';
import { openLinkInNewTab } from '#utils';
import { apiJobInfo, apiSingleProcessScheduler, apiUpdateCjf } from '#utils/apiUrls';
import { InputTypes } from '#utils/globalTypes';
import { isCjfParameterValueValid } from '#utils/parameterUtils';
import { getErrorMsg, request } from '#utils/request';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import CallMadeIcon from '@material-ui/icons/CallMade';
import { navigate } from '@reach/router';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import { RRule } from 'rrule';
import styled from 'styled-components';
import { updateJobSuccess } from '../ListView/actions';
import { AssignedJobStates, UnassignedJobStates } from '../ListView/types';
import CjfDetails from './CjfDetails';
import { useTypedSelector } from '#store';
import { getJobAnnotationDetails } from './Documents/utils';

const JobInfoDrawerWrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  max-height: 0;

  .pending-tasks {
    font-size: 14px;
    color: #161616;
    line-height: 1.14;
    letter-spacing: 0.16px;
    padding: 0px 8px;

    .count {
      font-size: 16px;
    }

    p {
      margin: 0;
      padding-bottom: 4px;
    }

    b {
      color: #111c2d;
    }

    .stage {
      margin: 24px 0px;
    }

    .tasks {
      display: flex;
      justify-content: space-between;
      margin-top: 12px;
      padding: 8px 0px;
      border-bottom: 1px solid #e0e0e0;
      cursor: pointer;
    }

    .task-item {
      display: flex;
      gap: 16px;
      align-items: flex-start;
    }

    .arrow-icon {
      color: #1d84ff;
    }
  }

  .job-summary {
    margin-bottom: 16px;
    border-bottom: 1.5px solid #e0e0e0;

    .engaged-users {
      .assignments {
        margin-left: 0;
      }
    }

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
    .scheduler-heading {
      display: flex;
      justify-content: space-between;
      margin-block: 16px;
      button {
        padding: 0;
        svg {
          margin-right: 4px;
          font-size: 14px;
        }
      }
      h4 {
        margin-block: 0;
      }
    }
    .no-schedule {
      font-size: 14px;
      line-height: 1.14;
      letter-spacing: 0.16px;
      color: #c2c2c2;
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

  .parameters-tabs {
    flex: 0;
  }

  .parameters-tabs-list {
  }

  .parameters-tabs-panel {
    padding: 24px 0 0;
  }
`;

const getJobAnnotationDetails = (jobAnnotationDto: any[]) => {
  const sortedAnnotations = jobAnnotationDto?.sort((a, b) => b.createdAt - a.createdAt);

  const latestRemark = sortedAnnotations?.[0]?.remarks;

  const latestMedia = sortedAnnotations?.[0]?.medias;

  return { latestRemark, latestMedia };
};

const JobInfoDrawer: FC<{
  job: any;
  onCloseDrawer: any;
  isInboxView?: boolean;
}> = ({ onCloseDrawer, job, isInboxView = false }) => {
  const dispatch = useDispatch();

  const {
    selectedFacility: { timeFormat },
  } = useTypedSelector((state) => state.auth);

  const [activeStep, setActiveStep] = useState(job.jobInfoTab || 0);
  const [jobInfo, setJobInfo] = useState<any>();
  const [schedulerInfo, setSchedulerInfo] = useState<any>(null);

  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      isEditing: false,
    },
  });

  const {
    watch,
    register,
    setValue,
    getValues,
    formState: { isDirty, isValid, dirtyFields },
  } = form;

  register('isEditing');

  const isEditing = watch('isEditing');

  const editCjfAllowed = useMemo(() => {
    return (
      jobInfo?.parameterValues?.length > 0 &&
      (jobInfo?.state === AssignedJobStates.ASSIGNED ||
        jobInfo?.state === UnassignedJobStates.UNASSIGNED)
    );
  }, [jobInfo]);

  const handleSubmit = async () => {
    const parameterDataInForm = getValues();
    const parameters = jobInfo?.parameterValues;

    const payload = parameters
      .map((parameter: any) => {
        if (parameterDataInForm?.[parameter.id] && dirtyFields?.[parameter.id]) {
          if (
            isCjfParameterValueValid(parameterDataInForm[parameter.id]) &&
            !parameterDataInForm[parameter.id]?.autoInitialized
          ) {
            return {
              parameterExecutionId: parameter?.response?.[0]?.id,
              parameterExecuteRequest: {
                jobId: jobInfo?.id,
                parameter: parameterDataInForm[parameter.id],
              },
            };
          }
          return null;
        }
        return null;
      })
      .filter(Boolean);

    const { data, errors } = await request('PATCH', apiUpdateCjf(), {
      data: payload,
    });

    if (data) {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'CJF parameter updated successfully!',
        }),
      );
      fetchJobInfo();
      dispatch(updateJobSuccess(data));
      setValue('isEditing', false);
    } else if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
  };

  const fetchJobInfo = async () => {
    try {
      const { data } = await request('GET', apiJobInfo(job.id));
      if (data) setJobInfo(data);
    } catch (e) {
      console.error('Error while fetching job info for JOB ID :: ', job.id, e);
    }
  };

  const fetchSchedulerById = async () => {
    try {
      const { data } = await request('GET', apiSingleProcessScheduler(job.schedulerId));
      if (data) {
        setSchedulerInfo(data);
      }
    } catch (e) {
      console.error('Error Fetching Parameter', e);
    }
  };

  useEffect(() => {
    fetchJobInfo();
    if (job?.schedulerId) {
      fetchSchedulerById();
    }
  }, []);

  const getRecurrenceSummary = () => {
    try {
      if (schedulerInfo?.recurrenceRule) {
        const rule = RRule.fromString(schedulerInfo.recurrenceRule);
        let recurrenceString = rule?.toText() || null;
        if (recurrenceString) {
          const freq = schedulerInfo.recurrenceRule.match('FREQ=([^;]*)')[1];
          if (schedulerInfo.customRecurrence) {
            switch (freq) {
              case 'DAILY':
              case 'WEEKLY':
              case 'MONTHLY':
                recurrenceString = `Repeat ${recurrenceString} at ${formatDateTime({
                  value: job.expectedStartDate,
                  type: InputTypes.TIME,
                })}`;
                break;
              case 'YEARLY':
                recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                  value: job.expectedStartDate,
                })}`;
                break;

              default:
                break;
            }
          } else {
            switch (freq) {
              case 'DAILY':
                recurrenceString = `Repeat ${recurrenceString} at ${formatDateTime({
                  value: job.expectedStartDate,
                  type: InputTypes.TIME,
                })}`;
                break;
              case 'WEEKLY':
                recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                  value: job.expectedStartDate,
                  format: `iiii 'at' ${timeFormat}`,
                })}`;
                break;
              case 'MONTHLY':
                recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                  value: job.expectedStartDate,
                  format: `do 'at' ${timeFormat}`,
                })}`;
                break;
              case 'YEARLY':
                recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                  value: job.expectedStartDate,
                  format: `do MMMM 'at' ${timeFormat}`,
                })}`;
                break;

              default:
                break;
            }
          }

          return [
            {
              label: 'Recurrence',
              value: recurrenceString,
            },
          ];
        }
      }
      return [];
    } catch (e) {
      console.error('Error while creating recurrence string', e);
      return [];
    }
  };

  const additionalJobInformation = (jobInfo: any) => {
    if (isInboxView) {
      return [
        {
          label: 'Job Status',
          value: <StatusTag status={jobInfo?.state ? jobInfo.state : null} />,
        },
        {
          label: 'Job Created by',
          value: `${getFullName(jobInfo?.createdBy)}, ID: ${
            jobInfo?.createdBy?.employeeId
          } on ${formatDateTime({
            value: jobInfo?.createdAt,
          })}`,
        },
        ...(jobInfo?.startedBy && jobInfo?.startedAt
          ? [
              {
                label: 'Job Started by',
                value: `${getFullName(jobInfo?.startedBy)}, ID: ${
                  jobInfo?.startedBy.employeeId
                } on ${formatDateTime({
                  value: jobInfo?.startedAt,
                })}`,
              },
            ]
          : []),
        ...(jobInfo?.endedBy && jobInfo?.endedAt
          ? [
              {
                label: 'Job Completed by',
                value: `${getFullName(jobInfo?.endedBy)}, ID: ${
                  jobInfo?.endedBy.employeeId
                } on ${formatDateTime({
                  value: jobInfo?.endedAt,
                })}`,
              },
            ]
          : []),
        ...(jobInfo?.engagedUsers && jobInfo?.engagedUsers.length > 0
          ? [
              {
                label: 'Engaged Users',
                value: (
                  <div className="engaged-users">
                    <AssigneeList users={jobInfo.engagedUsers} />
                  </div>
                ),
              },
            ]
          : []),
      ];
    } else {
      return [];
    }
  };

  const pendingOnMeTab = () => {
    if (isInboxView) {
      return [
        {
          label: 'Pending on me Tasks',
          value: '2',
          panelContent: <div />,
          renderFn: () => {
            return (
              <div className="pending-tasks">
                <div className="count">
                  <b>Total Pending Tasks {`(${job?.pendingTasksCount})`}</b>
                </div>
                {jobInfo?.pendingOnMeTasks?.map((stage: Stage) => (
                  <div key={stage.name} className="stage">
                    <p>Stage {stage.orderTree}</p>
                    <p>
                      <b>{stage.name}</b>
                    </p>
                    {stage.tasks.map((task) => {
                      const taskExecutionOrderTree = parseInt(task.taskExecutions[0].sortOrder);
                      return (
                        <div
                          className="tasks"
                          key={task.id}
                          onClick={() => {
                            navigate(
                              `/inbox/${job.id}?taskExecutionId=${task.taskExecutions[0].id}`,
                            );
                          }}
                        >
                          <div className="task-item">
                            <p>
                              Task {stage.orderTree}.{task.orderTree}
                              {taskExecutionOrderTree > 1 ? `.${taskExecutionOrderTree - 1}` : ''}
                            </p>
                            <p>{task.name}</p>
                          </div>
                          <div className="arrow-icon">
                            <CallMadeIcon fontSize="small" />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            );
          },
        },
      ];
    } else {
      return [];
    }
  };

  const sections = [
    {
      label: 'Summary',
      value: '0',
      panelContent: <div />,
      renderFn: () => {
        return (
          <LoadingContainer
            loading={!jobInfo}
            component={
              jobInfo && (
                <div>
                  <div className="job-summary">
                    <h4>Process Information</h4>
                    <ReadOnlyGroup
                      className="read-only-group"
                      items={[
                        {
                          label: 'Process Name',
                          value: jobInfo.checklist.name,
                        },
                        {
                          label: 'Process ID',
                          value: jobInfo.checklist.code,
                        },
                        ...(jobInfo.checklist.properties || []).map((property: any) => ({
                          label: property.label,
                          value: property.value,
                        })),
                      ]}
                    />
                  </div>
                  <div className="job-summary">
                    <h4>Job Information</h4>
                    <ReadOnlyGroup
                      className="read-only-group"
                      items={[
                        {
                          label: 'Job ID',
                          value: jobInfo.code,
                        },
                        ...additionalJobInformation(jobInfo),
                      ]}
                    />
                  </div>
                  {isFeatureAllowed('jobAnnotation') &&
                    (jobInfo.state === 'COMPLETED' ||
                      jobInfo.state === 'COMPLETED_WITH_EXCEPTION') &&
                    jobInfo?.jobAnnotationDto?.length > 0 && (
                      <div className="job-summary">
                        <h4>Annotation Information</h4>
                        <ReadOnlyGroup
                          className="read-only-group"
                          items={[
                            {
                              label: 'Remarks',

                              value: getJobAnnotationDetails(jobInfo.jobAnnotationDto).latestRemark,
                            },
                          ]}
                        />
                        <ReadOnlyGroup
                          className="read-only-group read-only-group-media"
                          items={[
                            {
                              label: 'Media',
                              value:
                                getJobAnnotationDetails(jobInfo.jobAnnotationDto)?.latestMedia
                                  .length > 0
                                  ? getJobAnnotationDetails(
                                      jobInfo.jobAnnotationDto,
                                    )?.latestMedia.map((media: Media, index: number) => {
                                      if (media?.archived === false) {
                                        return (
                                          <CustomTag
                                            as={'div'}
                                            key={index}
                                            onClick={() => {
                                              openLinkInNewTab(`/media?link=${media.link}`);
                                            }}
                                          >
                                            <span className="media-list-item">
                                              {media.originalFilename}
                                            </span>
                                          </CustomTag>
                                        );
                                      } else {
                                        return null;
                                      }
                                    })
                                  : '-',
                            },
                          ]}
                        />
                      </div>
                    )}
                </div>
              )
            }
          />
        );
      },
    },
    {
      label: 'CJF Parameters',
      value: '1',
      panelContent: <div />,
      renderFn: () => {
        return <CjfDetails jobInfo={jobInfo} form={form} handleCloseDrawer={handleCloseDrawer} />;
      },
    },
    ...pendingOnMeTab(),
    {
      label: 'Schedule',
      value: isInboxView ? '3' : '2',
      panelContent: <div />,
      renderFn: () => {
        return (
          <LoadingContainer
            loading={!jobInfo}
            component={
              jobInfo && (
                <div>
                  <div className="job-summary">
                    <div className="scheduler-heading">
                      <h4>Schedule</h4>
                    </div>
                    {job.expectedStartDate && job.expectedEndDate ? (
                      <ReadOnlyGroup
                        className="read-only-group"
                        items={[
                          {
                            label: 'Start Date and Time',
                            value: formatDateTime({
                              value: job.expectedStartDate,
                            }),
                          },
                          {
                            label: 'End Date and Time',
                            value: formatDateTime({
                              value: job.expectedEndDate,
                            }),
                          },
                          ...(schedulerInfo ? getRecurrenceSummary() : []),
                        ]}
                      />
                    ) : (
                      <div className="no-schedule">Job is not scheduled</div>
                    )}
                  </div>
                </div>
              )
            }
          />
        );
      },
    },
  ];

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(undefined);
    }, 200);
  };

  const onTabChange = (value: string) => {
    setActiveStep(parseInt(value));
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Job Info',
    hideCloseIcon: true,
    bodyContent: (
      <JobInfoDrawerWrapper>
        <StyledTabs
          containerProps={{
            className: 'parameters-tabs',
          }}
          tabListProps={{
            className: 'parameters-tabs-list',
          }}
          panelsProps={{
            className: 'parameters-tabs-panel',
          }}
          activeTab={activeStep.toString()}
          onChange={onTabChange}
          tabs={sections}
        />
        {sections.map((section) => {
          return activeStep === parseInt(section.value) ? section.renderFn() : null;
        })}
      </JobInfoDrawerWrapper>
    ),
    footerContent: (
      <>
        <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={handleCloseDrawer}>
          Cancel
        </Button>
        {activeStep === 1 && editCjfAllowed && (
          <>
            {isEditing ? (
              <Button type="submit" disabled={!isDirty || !isValid} onClick={handleSubmit}>
                Save
              </Button>
            ) : (
              <Button
                onClick={() => {
                  setValue('isEditing', true);
                }}
                disabled={jobInfo?.showCJFExceptionBanner || jobInfo?.forceCwe}
              >
                Edit
              </Button>
            )}
          </>
        )}
      </>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return <JobInfoDrawerWrapper>{StyledDrawer}</JobInfoDrawerWrapper>;
};

export default JobInfoDrawer;
