import React, { createContext, useState } from 'react';

type TJobLogViewContextType = {
  activeStep: number;
  setActiveStep: (step: number) => void;
  label: string;
  setLabel: (label: string) => void;
  columns: any[];
  setColumns: (columns: any[]) => void;
  filters: any[];
  setFilters: (filters: any[]) => void;
  resetContext: () => void;
};

const JobLogViewContext = createContext<TJobLogViewContextType | null>(null);

export const JobLogViewProvider: React.FC = ({ children }) => {
  const [activeStep, setActiveStep] = useState<number>(0);
  const [label, setLabel] = useState<string>('');
  const [columns, setColumns] = useState<any[]>([]);
  const [filters, setFilters] = useState<any[]>([]);

  const resetContext = () => {
    setActiveStep(0);
    setLabel('');
    setColumns([]);
    setFilters([]);
  };

  return (
    <JobLogViewContext.Provider
      value={{
        activeStep,
        setActiveStep,
        label,
        setLabel,
        columns,
        setColumns,
        filters,
        setFilters,
        resetContext,
      }}
    >
      {children}
    </JobLogViewContext.Provider>
  );
};

export const useCreateJobLogView = (): TJobLogViewContextType => {
  const context = React.useContext(JobLogViewContext);

  if (!context) {
    throw new Error('useCreateJobLogView must be used within a JobLogViewProvider');
  }

  return context;
};
