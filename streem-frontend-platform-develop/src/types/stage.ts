import { Task } from './task';

export interface Stage {
  id: string;
  name: string;
  code: string;
  orderTree: number;
  tasks: Task[];
}

export interface StoreStage extends Omit<Stage, 'tasks'> {
  visibleTasksCount: number;
  tasks: string[];
}
