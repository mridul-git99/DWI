import { fetchComposerData, resetComposer } from '#PrototypeComposer/actions';
import {
  Button,
  FormGroup,
  LoadingContainer,
  StepperContainer,
  StyledTabs,
  useDrawer,
} from '#components';
import { useTypedSelector } from '#store';
import { MandatoryParameter } from '#types';
import { apiSingleProcessScheduler } from '#utils/apiUrls';
import { InputTypes } from '#utils/globalTypes';
import { request } from '#utils/request';
import { formatDateTime } from '#utils/timeUtils';
import { JobForm } from '#views/Jobs/Components/CreateJob/JobForm';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import { getUnixTime } from 'date-fns';
import { zonedTimeToUtc } from 'date-fns-tz';
import { omit } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import { Frequency, RRule } from 'rrule';
import styled from 'styled-components';
import schedulersActionObjects from '../schedulerStore';
import { Scheduler } from './Scheduler';

type Props = {
  showBasic?: number;
};

const SchedulerDrawerWrapper = styled.form.attrs({})<Props>`
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
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
          }
        }
      }
    }
  }

  .job-summary {
    margin-block: 16px;
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
  }

  .MuiTabPanel-root {
    padding: 0;
  }

  .basic-details-section {
    display: ${({ showBasic }) => (showBasic === 0 ? 'unset' : 'none')};
  }
  .job-details-section {
    display: ${({ showBasic }) => (showBasic === 1 ? 'unset' : 'none')};
  }
  .scheduler-details-section {
    display: ${({ showBasic }) => (showBasic === 2 ? 'unset' : 'none')};
  }
`;

const CreateSchedularDrawer: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<boolean>>;
  checklist: Record<string, string>;
  schedular: any;
  readOnly: boolean;
  setReadOnly: React.Dispatch<React.SetStateAction<boolean>>;
  handleClose: () => void;
}> = ({ onCloseDrawer, checklist, schedular, readOnly, setReadOnly, handleClose }) => {
  const dispatch = useDispatch();
  const [activeStep, setActiveStep] = useState(0);
  const {
    prototypeComposer: { data: checklistData },
    schedular: { submitting },
    auth: { selectedFacility },
  } = useTypedSelector((state) => state);
  const [currentSchedular, setCurrentSchedular] = useState<any>(null);
  const formData = useRef<any>(null);

  const form = useForm<{
    name: string;
    description: string;
    dueDateDuration: Record<string, number>;
    dueDateInterval: number;
    expectedStartDate: number | string | undefined;
    recurrence: string;
    rRuleOptions: any;
    repeatCount: number;
    repeatEvery: string | undefined;
    weekDays: any;
  }>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      name: '',
      description: '',
      dueDateDuration: { year: 0, month: 0, week: 0, day: 0, hour: 0, minute: 0 },
      dueDateInterval: 0,
      expectedStartDate: undefined,
      recurrence: 'DAILY',
      rRuleOptions: {},
      repeatCount: 0,
      repeatEvery: undefined,
      weekDays: {},
    },
  });

  const {
    register,
    handleSubmit,
    formState: { isDirty, isValid },
    watch,
    getValues,
    reset,
  } = form;

  useEffect(() => {
    if (schedular?.checklistId) {
      dispatch(fetchComposerData({ id: schedular.checklistId }));
    }

    return () => {
      dispatch(resetComposer());
    };
  }, [schedular?.checklistId]);

  const getCjfParameterContent = (parameter: any, parameterId: string) => {
    let parameterContent;
    const selectedParameter = checklistData?.parameters?.find(
      (item: any) => item.id === parameterId,
    );
    const responseDetailsForChoiceBasedParameters = ({ data }: any) => {
      let detailList = data?.reduce((acc, currData: any) => {
        if (currData.state === 'SELECTED') {
          acc.push(currData.name);
        }
        return acc;
      }, []);
      return detailList?.join(', ');
    };

    switch (selectedParameter?.type) {
      case MandatoryParameter.SHOULD_BE:
      case MandatoryParameter.MULTI_LINE:
      case MandatoryParameter.SINGLE_LINE:
      case MandatoryParameter.NUMBER:
        parameterContent = parameter.data.input;
        break;
      case MandatoryParameter.DATE:
      case MandatoryParameter.DATE_TIME:
        parameterContent = formatDateTime({
          value: parameter.data.input,
          type: parameter.type === MandatoryParameter.DATE ? InputTypes.DATE : InputTypes.DATE_TIME,
        });
        break;
      case MandatoryParameter.YES_NO:
        parameterContent = responseDetailsForChoiceBasedParameters(parameter);
        break;
      case MandatoryParameter.SINGLE_SELECT:
        parameterContent = responseDetailsForChoiceBasedParameters(parameter);
        break;
      case MandatoryParameter.RESOURCE:
        parameterContent = parameter.data.choices.reduce((acc: any, currChoice: any) => {
          acc = `${currChoice.objectDisplayName} (ID: ${currChoice.objectExternalId})`;
          return acc;
        }, '');
        break;
      case MandatoryParameter.MULTI_RESOURCE:
        parameterContent = parameter?.data?.choices
          ?.reduce((acc: string, currChoice: any) => {
            const str = `${currChoice.objectDisplayName} (ID: ${currChoice.objectExternalId})`;
            acc?.push(str);
            return acc;
          }, [])
          ?.join(', ');
        break;
      case MandatoryParameter.MULTISELECT:
        parameterContent = responseDetailsForChoiceBasedParameters(parameter);
        break;
      default:
        return;
    }

    return parameterContent;
  };

  const basicInformation = () => {
    return !readOnly ? (
      <div
        style={{
          height: '100%',
        }}
        key="basic-info-section"
        className="basic-details-section"
      >
        <FormGroup
          style={{ marginBottom: 24 }}
          inputs={[
            {
              type: InputTypes.MULTI_LINE,
              props: {
                id: 'description',
                label: 'Description',
                name: 'description',
                optional: true,
                placeholder: 'Write Here',
                rows: 3,
                ref: register,
              },
            },
          ]}
        />
      </div>
    ) : (
      <div
        style={{
          height: '100%',
        }}
        key="basic-info-section"
        className="basic-details-section"
      >
        <div className="job-summary">
          <ReadOnlyGroup
            className="read-only-group"
            items={[
              {
                label: 'Version #',
                value: `${currentSchedular?.versionNumber}  ${
                  currentSchedular?.deprecatedAt === null ? '(Current)' : ''
                }`,
              },
              ...(currentSchedular?.description
                ? [
                    {
                      label: 'Description',
                      value: currentSchedular?.description,
                    },
                  ]
                : []),
            ]}
          />
        </div>
      </div>
    );
  };

  const JobDetailsView = () => {
    return (
      <LoadingContainer
        loading={!currentSchedular}
        component={
          <div>
            <div className="job-summary">
              <ReadOnlyGroup
                className="read-only-group"
                items={[
                  {
                    label: 'Process',
                    value: schedular?.checklistName,
                  },
                ]}
              />
            </div>
            {currentSchedular?.data?.parameterValues && (
              <div className="job-summary">
                <ReadOnlyGroup
                  className="read-only-group"
                  items={[
                    ...(Object?.entries(currentSchedular?.data?.parameterValues) || []).reduce(
                      (acc: any, [parameterId, { parameter }]) => {
                        acc.push({
                          label: parameter?.label,
                          value: getCjfParameterContent(parameter, parameterId),
                        });
                        return acc;
                      },
                      [],
                    ),
                  ]}
                />
              </div>
            )}
          </div>
        }
      />
    );
  };

  const sections = [
    {
      label: 'Basic Details',
      value: '0',
      panelContent: <div />,
      renderFn: basicInformation,
    },

    {
      label: 'Job Details',
      value: '1',
      panelContent: <div />,
      renderFn: () => (
        <div
          className="job-details-section"
          style={{
            height: '100%',
          }}
        >
          {schedular?.value ? JobDetailsView() : <JobForm form={form} checklist={checklist} />}
        </div>
      ),
    },
    {
      label: readOnly ? 'Recurrence' : 'Scheduler',
      value: '2',
      panelContent: <div />,
      renderFn: () => (
        <div
          className="scheduler-details-section"
          style={{
            height: '100%',
          }}
        >
          <Scheduler form={form} readOnly={readOnly} />
        </div>
      ),
    },
  ];

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1);
    const _data = getValues();
    formData.current = { ...formData.current, ..._data };
  };

  const handleBack = () => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1);
  };

  const onTabChange = (value: string) => {
    setActiveStep(parseInt(value));
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
      setReadOnly(false);
      handleClose();
    }, 200);
  };

  useEffect(() => {
    if (schedular.value) {
      fetchSingleScheduler();
    }
  }, []);

  const fetchSingleScheduler = async () => {
    try {
      const response = await request('GET', apiSingleProcessScheduler(schedular.value));
      if (response.data) {
        const rule = RRule.fromString(response.data.recurrenceRule);
        const rRuleOptions = rule.origOptions;
        const frequency =
          Object?.keys(Frequency)[Object?.values(Frequency)?.indexOf(rRuleOptions?.freq)];
        reset({
          name: response.data.name,
          description: response.data?.description,
          dueDateDuration: response.data.dueDateDuration,
          dueDateInterval: response.data.dueDateInterval,
          expectedStartDate: formatDateTime({
            value: response.data.expectedStartDate,
            format: `yyyy-MM-dd'T'HH:mm`,
          }),
          recurrence: response.data.customRecurrence ? 'custom' : frequency,
          rRuleOptions: omit(rRuleOptions, ['dtstart', 'interval', 'freq']) || {},
          repeatCount: rRuleOptions.interval,
          repeatEvery: response.data.customRecurrence ? frequency : null,
          weekDays: rRuleOptions?.byweekday?.reduce((acc, week) => {
            acc[week.weekday] = true;
            return acc;
          }, {}),
        });
        setCurrentSchedular(response.data);
      }
    } catch (e) {
      console.error('Error Fetching Parameter', e);
    }
  };

  const { name } = watch(['name']);

  const onSubmit = () => {
    const _data = { ...getValues(), ...formData.current };
    _data.customRecurrence = false;
    if (_data.recurrence === 'none') {
      _data.recurrence = null;
      _data.isRepeated = false;
    } else {
      let freq = _data.recurrence;
      _data.isRepeated = true;
      if (_data.recurrence === 'custom') {
        freq = _data.repeatEvery;
        _data.customRecurrence = true;
      }
      const rule = new RRule({
        freq: RRule[freq as keyof typeof RRule],
        interval: _data.repeatCount || 1,
        dtstart: new Date(_data.expectedStartDate),
        ...(_data.rRuleOptions || {}),
      });
      _data.recurrence = rule.toString();
    }
    _data.expectedStartDate = getUnixTime(
      zonedTimeToUtc(_data.expectedStartDate, selectedFacility?.timeZone),
    );

    if (!schedular?.value) {
      const parameterValues = Object.keys(_data).reduce<Record<string, any>>(
        (acc, parameterId: string) => {
          if (
            _data[parameterId] &&
            ![
              'checklistId',
              'name',
              'description',
              'expectedStartDate',
              'recurrence',
              'isRepeated',
              'isCustomRecurrence',
              'dueDateInterval',
              'dueDateDuration',
              'rRuleOptions',
              'repeatCount',
              'repeatEvery',
              'weekDays',
              'customRecurrence',
            ].includes(parameterId)
          ) {
            if (
              [
                MandatoryParameter.SINGLE_SELECT,
                MandatoryParameter.MULTISELECT,
                MandatoryParameter.YES_NO,
                MandatoryParameter.CHECKLIST,
              ].includes(_data[parameterId]?.type)
            ) {
              if (
                _data[parameterId]?.data?.length > 0 &&
                _data[parameterId]?.data?.some((item) => item.state === 'SELECTED')
              ) {
                acc[parameterId] = {
                  parameter: _data[parameterId],
                  reason: _data[parameterId]?.response?.reason || '',
                };
              }
            } else if (
              [
                MandatoryParameter.NUMBER,
                MandatoryParameter.DATE,
                MandatoryParameter.DATE_TIME,
                MandatoryParameter.SINGLE_LINE,
                MandatoryParameter.MULTI_LINE,
              ].includes(_data[parameterId]?.type)
            ) {
              const inputValue = _data[parameterId]?.data?.input;
              if (inputValue && inputValue !== null) {
                acc[parameterId] = {
                  parameter: [
                    MandatoryParameter.SINGLE_LINE,
                    MandatoryParameter.MULTI_LINE,
                  ].includes(_data[parameterId]?.type)
                    ? {
                        ..._data[parameterId],
                        data: {
                          ..._data[parameterId]?.data,
                          input: inputValue.trim(),
                        },
                      }
                    : _data[parameterId],
                  reason: _data[parameterId]?.response?.reason || '',
                };
              }
            } else {
              acc[parameterId] = {
                parameter: _data[parameterId],
                reason: _data[parameterId]?.response?.reason || '',
              };
            }
          }
          return acc;
        },
        {},
      );
      const newData = {
        name: name,
        description: _data?.description,
        checklistId: _data?.checklistId,
        parameterValues,
        expectedStartDate: _data?.expectedStartDate,
        recurrence: _data?.recurrence,
        repeated: _data?.isRepeated,
        customRecurrence: _data?.customRecurrence,
        dueDateInterval: _data?.dueDateInterval,
        dueDateDuration: _data?.dueDateDuration,
      };
      dispatch(
        schedulersActionObjects.schedulerActions.saveScheduler({
          data: newData,
          handleClose: handleCloseDrawer,
        }),
      );
    } else {
      const newData = {
        name: name,
        description: _data?.description,
        checklistId: currentSchedular.checklistId,
        expectedStartDate: _data?.expectedStartDate,
        recurrence: _data?.recurrence,
        repeated: _data?.isRepeated,
        customRecurrence: _data?.customRecurrence,
        dueDateInterval: _data?.dueDateInterval,
        dueDateDuration: _data?.dueDateDuration,
      };
      dispatch(
        schedulersActionObjects.schedulerActions.modifyScheduler({
          schedularId: currentSchedular.id,
          data: newData,
          handleClose: handleCloseDrawer,
        }),
      );
    }
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: readOnly ? 'View Scheduler' : schedular?.value ? 'Revise Scheduler' : 'Create Scheduler',
    hideCloseIcon: true,
    bodyContent: (
      <SchedulerDrawerWrapper onSubmit={handleSubmit(onSubmit)} showBasic={activeStep}>
        {readOnly ? (
          <div className="job-summary" style={{ margin: '8px 0 0 0', borderBottom: 'unset' }}>
            <ReadOnlyGroup
              className="read-only-group"
              items={[
                {
                  label: 'Scheduler Name',
                  value: currentSchedular?.name,
                },
              ]}
            />
          </div>
        ) : (
          <FormGroup
            style={{ marginBlock: 24 }}
            inputs={[
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  placeholder: 'Write here',
                  label: 'Label',
                  id: 'name',
                  name: 'name',
                  ref: register({
                    required: true,
                  }),
                },
              },
            ]}
          />
        )}
        {readOnly ? (
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
        ) : (
          <StepperContainer activeStep={activeStep} sections={sections} />
        )}

        {sections.map((section) => {
          return activeStep === parseInt(section.value) ? section.renderFn() : null;
        })}
      </SchedulerDrawerWrapper>
    ),
    footerContent: (
      <>
        {readOnly ? (
          <>
            <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={handleCloseDrawer}>
              Close
            </Button>
          </>
        ) : (
          <>
            {activeStep !== 0 && (
              <Button variant="secondary" onClick={handleBack}>
                Previous
              </Button>
            )}
            <Button variant="secondary" style={{ marginLeft: 'auto' }} onClick={handleCloseDrawer}>
              Cancel
            </Button>
            {activeStep === sections.length - 1 ? (
              <Button
                type="submit"
                onClick={onSubmit}
                loading={submitting}
                disabled={!isDirty || !isValid || submitting}
              >
                {schedular?.value ? 'Update' : 'Create'}
              </Button>
            ) : (
              <Button onClick={handleNext} disabled={activeStep === 0 ? !name : !isValid}>
                Next
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

  return <SchedulerDrawerWrapper>{StyledDrawer}</SchedulerDrawerWrapper>;
};

export default CreateSchedularDrawer;
