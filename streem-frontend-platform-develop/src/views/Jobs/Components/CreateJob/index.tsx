import { Button, StepperContainer, useDrawer } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { useTypedSelector } from '#store';
import { MandatoryParameter } from '#types';
import { createJob, createJobSuccess, updateJob } from '#views/Jobs/ListView/actions';
import { getUnixTime } from 'date-fns';
import { zonedTimeToUtc } from 'date-fns-tz';
import React, { FC, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { JobForm } from './JobForm';
import { Scheduler } from './Scheduler';

const CreateJobDrawerWrapper = styled.form`
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;

  .custom-label {
    align-items: center;
    color: #525252;
    display: flex;
    font-size: 12px;
    justify-content: flex-start;
    letter-spacing: 0.32px;
    line-height: 1.33;
    margin: 0px;
    margin-bottom: 8px;
  }

  .form-group {
    padding: 0;
    margin-bottom: 16px;

    :last-of-type {
      margin-bottom: 0;
    }
  }

  .due-after-section {
    display: flex;
    margin-bottom: 16px;
    .form-group {
      flex-direction: row;
      gap: 0.8%;
      width: 100%;
      > div {
        margin-bottom: 0;
        width: 16%;
        input {
          width: calc(100% - 32px);
        }
      }
    }
  }

  .custom-recurrence-section {
    display: flex;
    margin-bottom: 16px;
    .form-group {
      flex-direction: row;
      gap: 0.8%;
      width: 100%;
      > div {
        margin-bottom: 0;
        flex: 1;
      }
    }
    .week-day-container {
      display: flex;
      .week-day {
        background-color: #f4f4f4;
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        margin-right: 16px;
        font-size: 14px;
        cursor: pointer;
      }
    }
  }

  .scheduler-summary {
    border-top: 1.5px solid #e0e0e0;
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
      }
    }
  }
`;

const CreateJobDrawer: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<any>>;
  checklist?: Record<string, string>;
}> = ({ onCloseDrawer, checklist }) => {
  const dispatch = useDispatch();
  const timeZone = useTypedSelector((state) => state.auth.selectedFacility?.timeZone);
  const {
    jobListView: { submitting, createdData },
    auth: { selectedUseCase },
    prototypeComposer: {
      parameters: {
        parameters: { list: parametersList },
      },
    },
  } = useTypedSelector((state) => state);
  const [activeStep, setActiveStep] = useState(0);
  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  const {
    handleSubmit,
    formState: { isDirty, isValid },
    getValues,
  } = form;

  const [isMounted, setIsMounted] = useState<boolean>(false);

  const sections = [
    {
      label: 'Job Details',
      value: '0',
      panelContent: <div />,
      renderFn: () => <JobForm form={form} checklist={checklist} />,
    },
    {
      label: 'Schedule',
      value: '1',
      panelContent: <div />,
      renderFn: () => <Scheduler form={form} />,
    },
  ];

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  useEffect(() => {
    if (activeStep === 0 && !submitting && createdData && isMounted) {
      handleNext();
    }
  }, [submitting, createdData, isMounted]);

  useEffect(() => {
    dispatch(createJobSuccess(undefined, true));
    setIsMounted(true);
    return () => {
      handleCloseDrawer();
    };
  }, []);

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1);
  };

  const handleCloseDrawer = (shouldReRender = false) => {
    setDrawerOpen(false);
    dispatch(createJobSuccess(undefined, shouldReRender));
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  };

  const handleCreateJob = () => {
    const data = getValues();
    const parameterValues = parametersList.reduce((acc, parameter: any) => {
      if (data[parameter.id]) {
        if (
          [
            MandatoryParameter.NUMBER,
            MandatoryParameter.DATE,
            MandatoryParameter.DATE_TIME,
            MandatoryParameter.SINGLE_LINE,
            MandatoryParameter.MULTI_LINE,
          ].includes(data[parameter.id]?.type)
        ) {
          const inputValue = data[parameter.id]?.data?.input;

          if (inputValue && inputValue !== null) {
            acc[parameter.id] = {
              parameter: [MandatoryParameter.SINGLE_LINE, MandatoryParameter.MULTI_LINE].includes(
                data[parameter.id]?.type,
              )
                ? {
                    ...data[parameter.id],
                    data: {
                      ...data[parameter.id]?.data,
                      input: inputValue.trim(),
                    },
                  }
                : data[parameter.id],
              reason: data[parameter?.id]?.response?.reason || '',
            };
          }
        } else if (
          [
            MandatoryParameter.SINGLE_SELECT,
            MandatoryParameter.MULTISELECT,
            MandatoryParameter.YES_NO,
            MandatoryParameter.CHECKLIST,
          ].includes(data[parameter.id]?.type)
        ) {
          if (
            data[parameter.id]?.data?.length > 0 &&
            data[parameter.id]?.data?.some((item) => item.state === 'SELECTED')
          ) {
            acc[parameter.id] = {
              parameter: data[parameter.id],
              reason: data[parameter?.id]?.response?.reason || '',
            };
          }
        } else if (
          Object.keys(data[parameter.id]?.data).length === 0 ||
          (data[parameter.id]?.data?.choices &&
            data[parameter.id]?.data?.choices !== null &&
            (data[parameter.id]?.data?.choices?.length > 0 ||
              data[parameter.id]?.data?.allSelected))
        ) {
          acc[parameter.id] = {
            parameter: data[parameter.id],
            reason: data[parameter?.id]?.response?.reason || '',
          };
        }
      }
      return acc;
    }, {});

    dispatch(
      createJob({
        parameterValues,
        validateUserRole: checklist ? true : false,
        checklistId: data.checklistId,
        selectedUseCaseId: selectedUseCase!.id,
      }),
    );
  };

  const handleScheduleJob = () => {
    const _data = getValues();
    dispatch(
      updateJob({
        job: {
          id: createdData?.id,
          expectedStartDate: getUnixTime(zonedTimeToUtc(_data.expectedStartDate, timeZone!)),
          expectedEndDate: getUnixTime(zonedTimeToUtc(_data.expectedEndDate, timeZone!)),
        },
      }),
    );
    handleCloseDrawer();
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Create Job',
    hideCloseIcon: true,
    bodyContent: (
      <CreateJobDrawerWrapper onSubmit={handleSubmit(handleCreateJob)}>
        <StepperContainer sections={sections} activeStep={activeStep} />
        {sections.map((section) => {
          return activeStep === parseInt(section.value) ? section.renderFn() : null;
        })}
      </CreateJobDrawerWrapper>
    ),
    footerContent:
      activeStep === 0 ? (
        <>
          <Button
            variant="secondary"
            key="cancel"
            style={{ marginLeft: 'auto' }}
            onClick={() => handleCloseDrawer()}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            key="create-job"
            onClick={handleCreateJob}
            disabled={!isDirty || !isValid || submitting}
            loading={submitting}
          >
            Create Job & Continue
          </Button>
        </>
      ) : (
        <>
          <Button
            variant="secondary"
            key="skip-for-now"
            style={{ marginLeft: 'auto' }}
            onClick={() => {
              dispatch(
                showNotification({
                  type: NotificationType.SUCCESS,
                  msg: 'Job is created but not scheduled',
                }),
              );
              handleCloseDrawer();
            }}
          >
            Skip For Now
          </Button>
          <Button
            key="schedule-job"
            type="submit"
            disabled={!isDirty || !isValid}
            onClick={handleScheduleJob}
          >
            Save
          </Button>
        </>
      ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return StyledDrawer;
};

export default CreateJobDrawer;
