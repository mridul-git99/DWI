import { Checklist, Stage, Task } from '#types';
import { Error } from '#utils/globalTypes';
import { RouteComponentProps } from '@reach/router';

export type ReOrderType = {
  from: number;
  id: string;
  to: number;
};

// TODO: merge this as well as Composer type from Job Composer
export enum ComposerEntity {
  CHECKLIST = 'CHECKLIST',
  JOB = 'JOB',
}

export enum EntityType {
  STAGE = 'STAGE',
  TASK = 'TASK',
  PARAMETER = 'PARAMETER',
}

export type ComposerProps = RouteComponentProps<{
  id: string;
}>;

export type ErrorGroups = {
  stagesErrors: Error[];
  tasksErrors: Error[];
  parametersErrors: Error[];
  otherErrors: Error[];
  errorsWithEntity: Error[];
};

export type CopyEntityType = {
  elementId: string;
  type: EntityType;
  checklistId: Checklist['id'];
  stageId?: Stage['id'];
  taskId?: Task['id'];
};
