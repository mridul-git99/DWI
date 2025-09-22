import { Property } from './common';
import { Stage } from './stage';

export type ChecklistProperty = Pick<Property, 'id' | 'name' | 'value'>;

export interface Checklist {
  archived?: boolean;
  code: string;
  id: string;
  ancestorId: string;
  name: string;
  version: number | null;
  stages?: Stage[];
  noOfJobs?: number;
  properties?: ChecklistProperty[];
  noOfTasks?: number;
  global: boolean;
}
