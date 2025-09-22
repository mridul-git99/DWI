import { AllChecklistStates } from '#PrototypeComposer/checklist.types';

type ChecklistProperty = {
  id: string;
  name: string;
  value: string;
};

// TODO REMOVE THESE TYPES AND USE FROM PROTOTYPE COMPOSER TYPES FILES.
export interface Checklist {
  archived: boolean;
  code: string;
  id: string;
  name: string;
  noOfJobs: number;
  properties: ChecklistProperty[];
  state: AllChecklistStates;
  version: number | null;
}
