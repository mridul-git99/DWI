import { TaskExecutionStates } from '#types';
import { ParameterExecutionState } from '../types/parameter';

export const taskStateColor = (taskStatus: TaskExecutionStates | ParameterExecutionState) => {
  switch (taskStatus) {
    case 'NOT_STARTED':
      return '#E0E0E0';
    case 'IN_PROGRESS':
    case 'PAUSED':
    case 'BEING_EXECUTED':
      return '#1D84FF';
    case 'COMPLETED':
    case 'EXECUTED':
      return '#42BE65';
    case 'COMPLETED_WITH_EXCEPTION':
    case 'SKIPPED':
      return '#F1C21B';
    case 'PENDING_FOR_APPROVAL':
      return '#F1C21B';
    default:
      break;
  }
};

export const taskStateText = (taskStatus: TaskExecutionStates | ParameterExecutionState) => {
  switch (taskStatus) {
    case 'NOT_STARTED':
      return 'Task Not Started';
    case 'IN_PROGRESS':
      return 'Task In Progress';
    case 'PAUSED':
      return 'Task Paused';
    case 'COMPLETED':
      return 'Task Completed';
    case 'COMPLETED_WITH_EXCEPTION':
      return 'Task Completed with an exception';
    case 'SKIPPED':
      return 'Task Skipped';
    case 'EXECUTED':
      return 'Task Executed';
    case 'BEING_EXECUTED':
      return 'Task Being Executed';
    case 'PENDING_FOR_APPROVAL':
      return 'Task Pending for Approval';
    default:
      return '';
  }
};

export const STATUS_COLOR_MAP = {
  ONGOING: {
    statusText: 'Ongoing',
    color: '#0043ce',
    backgroundColor: '#d0e2ff',
  },
  SCHEDULED_FOR_TODAY: {
    statusText: 'Scheduled for Today',
    color: '#6929c4',
    backgroundColor: '#e8daff',
  },
  COMPLETED_WITH_EXCEPTION: {
    statusText: 'Completed with Exception',
    color: '#ff541e',
    backgroundColor: '#ffedd7',
  },
  PENDING_START: {
    statusText: 'Pending Start',
    color: '#9f1853',
    backgroundColor: '#ffd6e8',
  },
  IN_PROGRESS: {
    statusText: 'Ongoing',
    color: '#0043ce',
    backgroundColor: '#d0e2ff',
  },
  ASSIGNED: {
    statusText: 'Not Started',
    color: '#ff541e',
    backgroundColor: '#ffedd7',
  },
  UNASSIGNED: {
    statusText: 'Not Started',
    color: '#ff541e',
    backgroundColor: '#ffedd7',
  },
  BLOCKED: {
    statusText: 'Ongoing',
    color: '#0043ce',
    backgroundColor: '#d0e2ff',
  },
  COMPLETED: {
    statusText: 'Completed',
    color: '#0e6027',
    backgroundColor: '#a7f0ba',
  },
  UNSCHEDULED: {
    statusText: 'Unscheduled',
    color: '#161616',
    backgroundColor: '#e0e0e0',
  },
  OVERDUE_FOR_COMPLETION: {
    statusText: 'Overdue for Completion',
    color: '#a2191f',
    backgroundColor: '#ffd7d9',
  },
  SCHEDULED: {
    statusText: 'Scheduled',
    color: '#000000',
    backgroundColor: '#d2e6ff',
  },
  OVERDUE: {
    statusText: 'Overdue',
    color: '#000000',
    backgroundColor: '#fce6d1',
  },
  START_DELAYED: {
    statusText: 'Start Delayed',
    color: '#000000',
    backgroundColor: '#fcf3d1',
  },
  PENDING_APPROVAL: {
    statusText: 'Pending Approval',
    color: '#000000',
    backgroundColor: '#fcf3d1',
  },
  NOT_STARTED: {
    statusText: 'Not Started',
    color: '#ff541e',
    backgroundColor: '#ffedd7',
  },
  // Correction Statuses
  INITIATED: {
    statusText: 'Awaiting Correction',
    color: '#E99A00',
    backgroundColor: '#FCF3D1',
  },
  CORRECTED: {
    statusText: 'Awaiting Review',
    color: '#0043CE',
    backgroundColor: '#D0E2FF',
  },
  ACCEPTED: {
    statusText: 'Accepted',
    color: '#0E6027',
    backgroundColor: '#A7F0BA',
  },
  REJECTED: {
    statusText: 'Rejected',
    color: '#A2191F',
    backgroundColor: '#FFD7D9',
  },
};
