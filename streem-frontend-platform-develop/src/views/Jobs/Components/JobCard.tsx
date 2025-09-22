import recurrenceIcon from '#assets/svg/Recurrence.svg';
import { StatusTag } from '#components';
import Tooltip from '#components/shared/Tooltip';
import { useTypedSelector } from '#store';
import { Parameter } from '#types';
import { InputTypes } from '#utils/globalTypes';
import { getParameterContent } from '#utils/parameterUtils';
import { checkJobExecutionDelay, formatDateTime } from '#utils/timeUtils';
import { InboxState } from '#views/Inbox/ListView/types';
import { LabelValueRow } from '#views/Job/components/Header/styles';
import { Divider } from '@material-ui/core';
import { ArrowForward, ChevronLeft } from '@material-ui/icons';
import { navigate } from '@reach/router';
import { getUnixTime } from 'date-fns';
import { capitalize } from 'lodash';
import React, { FC, useMemo } from 'react';
import { Frequency, RRule } from 'rrule';
import styled from 'styled-components';
import { AssignedJobStates, CompletedJobStates, Job, JobStatus } from '../ListView/types';

const JobCardWrapper = styled.div`
  .job-row {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 16px;
    border: 1px solid #e0e0e0;
    position: relative;
    height: 100%;

    .job-status-indicator {
      width: 8px;
      height: 100%;
      margin-right: -16px;
    }

    .completed {
      background-color: #42be65;
    }

    .overdue {
      background-color: #fa4d56;
    }

    .job-row-section {
      display: flex;
      max-width: calc(100% - 52px);

      .job-divider-horizontal {
        margin: 4px 0;
        background-color: #e0e0e0;
      }

      &.left {
        flex: 1;
        flex-direction: column;
        gap: 8px;
        padding: 16px 16px 8px 16px;
        height: 100%;
        .job-row-section-left {
          display: flex;

          .card-item {
            gap: 8px;
          }

          .pending-task {
            margin-left: auto;
          }

          .job-row-tablet-viewer {
            @media (max-width: 900px) {
              display: grid;
              grid-template-columns: auto auto;
              grid-gap: 4px;
            }
            .info-item {
              @media (max-width: 900px) {
                width: 100% !important;
              }
            }
          }
          &.top {
            display: flex;
            justify-content: space-between;

            .badge {
              display: flex;
            }

            .badge-tablet {
              display: none;
            }

            @media (max-width: 900px) {
              flex-direction: column;
              align-items: flex-start;
              gap: 8px;

              .badge {
                display: none;
              }

              .badge-tablet {
                display: flex;
                margin-left: auto;
              }

              .section {
                width: 100%;
              }
            }

            .section {
              display: flex;
              gap: 8px;
              align-items: center;
            }

            .job-name {
              margin: 0;
              font-size: 14px;
              line-height: 16px;
              letter-spacing: 0.16px;
              color: #1d84ff;
              cursor: pointer;
              overflow-wrap: anywhere;
            }
            .job-type {
              margin: 0;
              font-size: 12px;
              line-height: 16px;
              letter-spacing: 0.16px;
              color: #6f6f6f;
            }
            .job-divider {
              margin: 0 8px;
              color: #e0e0e0;
              display: inline-block;
            }

            .schedule-info {
              display: flex;
              align-items: center;
              gap: 8px;
              span {
                font-size: 12px;
                line-height: 12px;
                letter-spacing: 0.32px;
                color: #161616;
                &.primary {
                  color: #1d84ff;
                  cursor: pointer;
                }
              }
              .icon {
                padding: 2px;
                background: #e0e0e0;
                border-radius: 50%;

                svg {
                  font-size: 12px;
                }
              }
              .primary {
                cursor: pointer;
                color: #1d84ff;

                :hover {
                  color: #1d84ff;
                }
              }
            }
          }
        }
      }
      &.right {
        width: 36px;
        height: 100%;
        background-color: #f4f4f4;
        align-items: center;
        justify-content: center;
        cursor: pointer;
      }
    }
  }
`;

const getRecurrenceSummary = (job: Job, timeFormat: string) => {
  try {
    if (job?.scheduler?.recurrenceRule && job.expectedStartDate) {
      const rule = RRule.fromString(job?.scheduler?.recurrenceRule);
      let recurrenceString = rule?.toText() || null;
      if (recurrenceString) {
        const freq = job?.scheduler?.recurrenceRule.match('FREQ=([^;]*)')[1];
        if (job?.scheduler?.customRecurrence) {
          switch (freq) {
            case 'DAILY':
            case 'WEEKLY':
            case 'MONTHLY':
              recurrenceString = `Repeat ${recurrenceString} at ${formatDateTime({
                value: job.expectedStartDate!,
                type: InputTypes.TIME,
              })}`;
              break;
            case 'YEARLY':
              recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                value: job.expectedStartDate!,
                format: `do MMMM 'at' ${timeFormat}`,
              })}`;
              break;

            default:
              break;
          }
        } else {
          switch (freq) {
            case 'DAILY':
              recurrenceString = `Repeat ${recurrenceString} at ${formatDateTime({
                value: job.expectedStartDate!,
                type: InputTypes.TIME,
              })}`;
              break;
            case 'WEEKLY':
              recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                value: job.expectedStartDate!,
                format: `iiii 'at' ${timeFormat}`,
              })}`;
              break;
            case 'MONTHLY':
              recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                value: job.expectedStartDate!,
                format: `do 'at' ${timeFormat}`,
              })}`;
              break;
            case 'YEARLY':
              recurrenceString = `Repeat ${recurrenceString} on ${formatDateTime({
                value: job.expectedStartDate!,
                format: `do MMMM 'at' ${timeFormat}`,
              })}`;
              break;

            default:
              break;
          }
        }

        return recurrenceString;
      }
    }
    return '';
  } catch (e) {
    console.error('Error while creating recurrence string', e);
    return [];
  }
};

const JobCard: FC<{
  job: Job;
  view: string;
  label?: string;
  onSetDate?: (jobId: string) => void;
  setSelectedJob: React.Dispatch<React.SetStateAction<Job | undefined>>;
}> = ({ job, view, label, onSetDate, setSelectedJob }) => {
  const {
    selectedFacility: { timeFormat },
  } = useTypedSelector((state) => state.auth);

  const isInbox = view === 'Inbox';

  const jobColorCode = job?.checklist?.colorCode || '#fff';

  const isPendingStart = job?.expectedStartDate
    ? checkJobExecutionDelay(getUnixTime(new Date()), job.expectedStartDate) && !job.startedAt
    : null;
  const isJobOverdue = job?.expectedEndDate
    ? checkJobExecutionDelay(getUnixTime(new Date()), job.expectedEndDate) && !job.endedAt
    : null;

  const isJobCompleted = job?.state in CompletedJobStates;
  const isJobCompletedWithException = job?.state === CompletedJobStates.COMPLETED_WITH_EXCEPTION;
  const isJobOngoing = [AssignedJobStates.IN_PROGRESS, AssignedJobStates.BLOCKED].includes(
    job?.state as any,
  );

  const badgeStatus = useMemo(() => {
    switch (true) {
      case isJobCompletedWithException:
        return JobStatus.COMPLETED_WITH_EXCEPTION;
      case isJobCompleted:
        return JobStatus.COMPLETED;
      case isJobOngoing:
        return JobStatus.ONGOING;
      case !job?.startedAt:
        return JobStatus.NOT_STARTED;
      default:
        return null;
    }
  }, [isJobCompletedWithException, isJobOngoing, isJobCompleted, job?.startedAt]);

  const cjfParameters = useMemo(() => {
    return (job?.parameterValues || [])
      .filter((parameter: Parameter) => !parameter?.response?.[0]?.hidden) // parameter response is an array of responses and in case of CJF there will always be only one response.
      .sort((a: Parameter, b: Parameter) => a.orderTree - b.orderTree)
      .slice(0, 4)
      .map((parameter: Parameter) => {
        const cjfParameter = { ...parameter, response: parameter?.response?.[0] };
        const value = getParameterContent(cjfParameter);
        return (
          <div className="card-item" key={parameter.label}>
            <label className="info-item-label">{parameter.label}</label>
            <span className="info-item-value" title={value}>
              {value}
            </span>
          </div>
        );
      });
  }, [job?.parameterValues]);

  let rule;
  let rRuleOptions;
  let frequency;

  if (job?.scheduler) {
    rule = RRule?.fromString(job?.scheduler?.recurrenceRule);
    rRuleOptions = rule?.origOptions;
    frequency = Object?.keys(Frequency)[Object?.values(Frequency)?.indexOf(rRuleOptions?.freq!)];
  }

  return (
    <JobCardWrapper>
      <div className="job-row" key={job.id} style={{ background: jobColorCode }}>
        {isInbox && (isJobCompleted || isJobOverdue) && (
          <div
            className={`job-status-indicator ${isJobCompleted ? 'completed ' : 'overdue'}`}
          ></div>
        )}
        <div className="job-row-section left">
          <div className="job-row-section-left top">
            <div className="section">
              <h5
                className="job-name"
                onClick={() =>
                  navigate(
                    `/${view === 'Jobs' ? 'jobs' : 'inbox'}/${job.id}${
                      view === 'Inbox' && job?.firstPendingTaskId
                        ? `?taskExecutionId=${job.firstPendingTaskId}`
                        : ''
                    }`,
                  )
                }
              >
                {job.code} : {job.checklist.name}
              </h5>
              {job.checklist.global && (
                <Divider className="job-divider" orientation="vertical" flexItem />
              )}
              {job.checklist.global && <h5 className="job-type">Global</h5>}
              <div className="badge-tablet">{isInbox && <StatusTag status={badgeStatus} />}</div>
            </div>
            <div className="section">
              {job.expectedStartDate && job.expectedEndDate ? (
                <div className="schedule-info">
                  {frequency && <span>{capitalize(frequency)}</span>}
                  {job?.scheduler && (
                    <Tooltip
                      title={
                        <div
                          style={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '8px',
                            padding: '2px',
                          }}
                        >
                          <div
                            style={{
                              display: 'flex',
                              flexDirection: 'column',
                            }}
                          >
                            <span style={{ fontSize: '12px' }}>Scheduler Name</span>
                            <span>{job.scheduler.name}</span>
                          </div>
                          <div
                            style={{
                              display: 'flex',
                              flexDirection: 'column',
                            }}
                          >
                            <span style={{ fontSize: '12px' }}>Start Date and Time</span>
                            <span>
                              {formatDateTime({
                                value: job.expectedStartDate,
                              })}
                            </span>
                          </div>
                          <div
                            style={{
                              display: 'flex',

                              flexDirection: 'column',
                            }}
                          >
                            <span style={{ fontSize: '12px' }}>End Date and Time</span>
                            <span>
                              {formatDateTime({
                                value: job.expectedEndDate,
                              })}
                            </span>
                          </div>
                          <div
                            style={{
                              display: 'flex',
                              flexDirection: 'column',
                            }}
                          >
                            <span style={{ fontSize: '12px' }}>Recurrence</span>
                            <span>{getRecurrenceSummary(job, timeFormat)}</span>
                          </div>
                        </div>
                      }
                      arrow
                    >
                      <img className="icon" src={recurrenceIcon} alt="recurrence-icon" />
                    </Tooltip>
                  )}
                  <span
                    style={{
                      color:
                        isJobCompleted || isJobCompletedWithException
                          ? '#161616'
                          : isPendingStart
                          ? '#da1e28'
                          : '#161616',
                    }}
                  >
                    {formatDateTime({
                      value: job.expectedStartDate,
                    })}
                  </span>
                  <span className="icon">
                    <ArrowForward />
                  </span>
                  <span
                    style={{
                      color: isJobOverdue ? '#da1e28' : '#161616',
                    }}
                  >
                    {formatDateTime({
                      value: job.expectedEndDate,
                    })}
                  </span>
                </div>
              ) : onSetDate ? (
                <div className="schedule-info">
                  <span className="primary" onClick={() => onSetDate(job.id)}>
                    Set Date
                  </span>
                  <span className="icon">
                    <ArrowForward />
                  </span>
                  <span className="primary" onClick={() => onSetDate(job.id)}>
                    Set Date
                  </span>
                </div>
              ) : null}
              <div className="badge">{isInbox && <StatusTag status={badgeStatus} />}</div>
            </div>
          </div>
          {isInbox && label === InboxState.PENDING_ON_ME ? (
            <Divider className="job-divider-horizontal" orientation="horizontal" />
          ) : job?.parameterValues?.length > 0 ? (
            <Divider className="job-divider-horizontal" orientation="horizontal" />
          ) : null}
          <div className="job-row-section-left bottom">
            <LabelValueRow className="job-row-tablet-viewer">
              {cjfParameters}
              {isInbox && label !== InboxState.ALL_JOBS && (
                <div
                  className="card-item pending-task"
                  onClick={() => {
                    setSelectedJob({
                      ...job,
                      jobInfoTab: '2',
                    });
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  <label className="info-item-label">Pending on me Tasks</label>
                  <span className="info-item-value" style={{ color: '#1d84ff' }}>
                    {job?.pendingTasksCount} / {job?.totalTasksCount}
                  </span>
                </div>
              )}
            </LabelValueRow>
          </div>
        </div>
        <div
          className="job-row-section right"
          onClick={() => {
            setSelectedJob(job);
          }}
        >
          <ChevronLeft />
        </div>
      </div>
    </JobCardWrapper>
  );
};

export default JobCard;
