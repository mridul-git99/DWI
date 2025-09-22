import { StepperContainer, useDrawer } from '#components';
import React, { Dispatch, FC, SetStateAction, useEffect, useMemo } from 'react';
import styled from 'styled-components';
import {
  BasicInfoSection,
  ColumnsSection,
  ConfigureColumnsSection,
  FiltersSection,
} from './CreateLogViewSections';
import { useCreateJobLogView } from '#hooks/useCreateJobLogView';

type TCreateLogViewDrawerProps = {
  onCloseDrawer: Dispatch<SetStateAction<boolean>>;
  selectedView: Record<string, any>;
  setReRender: Dispatch<SetStateAction<boolean>>;
  setSelectedView: Dispatch<SetStateAction<Record<string, any> | null>>;
};

const Wrapper = styled.div`
  display: flex;
  width: 100%;
  flex-direction: column;
  flex: 1;

  .MuiStepper-root {
    padding-top: 0 !important;
  }
`;

const CreateLogViewDrawer: FC<TCreateLogViewDrawerProps> = ({
  onCloseDrawer,
  selectedView,
  setReRender,
  setSelectedView,
}) => {
  const { activeStep, resetContext, setColumns, setFilters, setLabel } = useCreateJobLogView();

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    resetContext();
    setTimeout(() => {
      onCloseDrawer(false);
      setSelectedView(null);
    }, 200);
  };

  const sections = useMemo(
    () => [
      {
        label: 'Basic Info',
        value: '0',
        renderFn: () => <BasicInfoSection onCloseDrawer={handleCloseDrawer} />,
      },
      {
        label: 'Select Columns',
        value: '1',
        renderFn: () => <ColumnsSection onCloseDrawer={handleCloseDrawer} />,
      },
      {
        label: 'Configure Columns',
        value: '2',
        renderFn: () => <ConfigureColumnsSection onCloseDrawer={handleCloseDrawer} />,
      },
      {
        label: 'Apply Filters',
        value: '3',
        renderFn: () => (
          <FiltersSection
            onCloseDrawer={handleCloseDrawer}
            setReRender={setReRender}
            selectedView={selectedView}
          />
        ),
      },
    ],
    [handleCloseDrawer],
  );

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  useEffect(() => {
    if (selectedView) {
      setLabel(selectedView.label);
      setColumns(selectedView.columns);
      setFilters(selectedView.filters);
    }
  }, [selectedView]);

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: selectedView ? 'Edit Log View' : 'Create a New Log View',
    hideCloseIcon: true,
    bodyContent: (
      <Wrapper>
        <StepperContainer sections={sections} activeStep={activeStep} />
        {sections.map((section) => {
          return activeStep === parseInt(section.value) ? section.renderFn() : null;
        })}
      </Wrapper>
    ),
  });

  return StyledDrawer;
};

export default CreateLogViewDrawer;
