import { Button, StepperContainer, useDrawer } from '#components';
import {
  ShowSectionUsersAndUserGroupTabs,
  useUserAndUserGroupTabs,
} from '#hooks/useUserAndUserGroupTabs';
import { useTypedSelector } from '#store';
import { apiParameterVerificationGroupAssignees, apiVerificationAssignees } from '#utils/apiUrls';
import { jobActions } from '#views/Job/jobStore';
import React, { FC, useCallback, useEffect, useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { BulkParametersSection } from './BulkParametersSection';

const RequestPeerVerificationDrawerWrapper = styled.form`
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
`;

const AssignBulkPeerVerification: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<any>>;
  isReadOnly: boolean;
}> = ({ onCloseDrawer, isReadOnly }) => {
  const dispatch = useDispatch();
  const [activeStep, setActiveStep] = useState(0);
  const jobId = useTypedSelector((state) => state.job.id)!;

  const form = useForm<{
    parameterResponse: any[];
  }>({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      parameterResponse: [],
    },
  });

  const { control, setValue } = form;

  const {
    fields: parameterResponseFields,
    append: parameterResponseAppend,
    remove: parameterResponseRemove,
  } = useFieldArray({
    control,
    name: 'parameterResponse',
  });

  const onSubmit = (params: any) => {
    const parameterResponseValues = parameterResponseFields.map((field) => {
      return {
        parameterExecutionId: field?.response?.id,
        parameterId: field?.id,
        checkedAt: field.checkedAt,
        peerAssignees: params,
      };
    });
    dispatch(
      jobActions.sendBulkPeerVerification({
        values: parameterResponseValues,
      }),
    );
    handleCloseDrawer();
  };

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1);
  };

  const handleBack = useCallback(() => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1);
  }, [setActiveStep]);

  const { bodyContent, footerContent } = useUserAndUserGroupTabs({
    apiUrlUser: () => apiVerificationAssignees(jobId!),
    apiUrlUserGroup: () => apiParameterVerificationGroupAssignees(jobId),
    onSubmit,
    handleCloseDrawer: handleBack,
    showRoleTag: false,
    shouldFilterList: true,
    showSections: ShowSectionUsersAndUserGroupTabs.BOTH,
  });

  const sections = [
    {
      label: 'Check Parameters',
      value: '0',
      panelContent: <div />,
      renderFn: () => (
        <BulkParametersSection
          parameterResponseFields={parameterResponseFields}
          parameterResponseAppend={parameterResponseAppend}
          parameterResponseRemove={parameterResponseRemove}
          setValue={setValue}
        />
      ),
    },
    {
      label: 'Assign Users',
      value: '1',
      panelContent: <div />,
      renderFn: () => <div style={{ overflow: 'hidden' }}>{bodyContent()}</div>,
    },
  ];

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  useEffect(() => {
    return () => {
      handleCloseDrawer();
    };
  }, []);

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  };

  const onSelectingUsers = () => {
    handleNext();
  };

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Request Peer Verification',
    hideCloseIcon: true,
    bodyContent: (
      <RequestPeerVerificationDrawerWrapper>
        <StepperContainer sections={sections} activeStep={activeStep} />
        {sections.map((section) => {
          return activeStep === parseInt(section.value) ? section.renderFn() : null;
        })}
      </RequestPeerVerificationDrawerWrapper>
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
            onClick={onSelectingUsers}
            disabled={parameterResponseFields.length < 2 || isReadOnly}
          >
            Next
          </Button>
        </>
      ) : (
        <>{footerContent()}</>
      ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return StyledDrawer;
};

export default AssignBulkPeerVerification;
