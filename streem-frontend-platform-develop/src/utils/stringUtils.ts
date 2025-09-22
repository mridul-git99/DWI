import { StagesById } from '#PrototypeComposer/Stages/reducer.types';
import { TaskOrderInStage, TasksById } from '#PrototypeComposer/Tasks/reducer.types';
import { User } from '#store/users/types';
import { omitBy } from 'lodash';

export const getInitials = (name: string) => {
  let initials: RegExpMatchArray | string = name.match(/\b\w/g) || [];
  initials = ((initials.shift() || '') + (initials.pop() || '')).toUpperCase();
  return initials;
};

// TODO : remove this function from here and use the one defined in the helpers of users service
export const getFullName = (props: Pick<User, 'firstName' | 'lastName'>) => {
  const { firstName = '', lastName = '' } = omitBy(props, (v) => !v);
  return `${firstName} ${lastName}`;
};

export const getOrdinal = (n: number) => {
  const s = ['th', 'st', 'nd', 'rd'],
    v = n % 100;
  return n + (s[(v - 20) % 10] || s[v] || s[0]);
};

export const removeUnderscore = (str: string) => {
  return str.replace(/_/g, ' ');
};

type Response = { tag: string; text: string; childs: Response[] };

export const parseMarkUp = (n: HTMLElement) => {
  const res: Response[] = [];
  const parser = (node: HTMLElement, arr: Response[]) => {
    node.childNodes.forEach((cNode) => {
      const nValue = cNode.textContent as string;
      if (cNode.nodeName === 'BR') {
        arr.push({
          tag: 'P',
          text: '',
          childs: [],
        });
      } else if (
        (cNode.nodeType === 3 && nValue?.codePointAt(0)?.toString(16) !== 'a') ||
        cNode.hasChildNodes()
      ) {
        arr.push({
          tag: cNode.nodeName,
          text: cNode.nodeType === 3 ? nValue : '',
          childs: [],
        });
        parser(cNode, arr[arr.length - 1].childs);
      }
    });
  };

  parser(n, res);
  return res;
};

export const encrypt = (data: string) => btoa(data);

export const isMatchAny = (url: string, patterns: string[]) =>
  !patterns.every((pattern) => url.match(pattern) === null);

export const getFileExtension = (filename: string) => {
  return filename.split('.').pop();
};

export const generateShouldBeText = (label: string | undefined, data: any) => {
  const uom = data?.uom || '';
  if (data?.operator === 'BETWEEN') {
    return `${label} should be between ${data.lowerValue} ${uom} and ${data.upperValue} ${uom}`;
  } else {
    let operatorString = generateShouldBeCriteria(data);
    return `${label} should be ${operatorString} ${data?.value ?? 50} ${uom}`;
  }
};

export const generateShouldBeCriteria = (data: Record<string, string>) => {
  switch (data?.operator) {
    case 'EQUAL_TO':
      return '(=) equal to';
    case 'LESS_THAN':
      return '(<) less than';

    case 'LESS_THAN_EQUAL_TO':
      return '(<=) less than and equal to';

    case 'MORE_THAN':
      return '(>) more than';

    case 'MORE_THAN_EQUAL_TO':
      return '(>=) more than equal to';

    case 'BETWEEN':
      return 'between';
    default:
      return;
  }
};

export const getStageTaskOptions = (
  stagesListById: StagesById,
  tasksListById: TasksById,
  tasksOrderInStage: TaskOrderInStage,
  excludeTaskId?: string,
) => {
  return Object.entries(tasksOrderInStage)
    .map(([stageId, taskIds]) => {
      const stage = stagesListById[stageId];
      if (!stage) return null;

      let tasks = taskIds.map((taskId) => tasksListById[taskId]).filter(Boolean);

      if (excludeTaskId) {
        tasks = tasks.filter((task) => task.id !== excludeTaskId);
      }

      if (tasks.length === 0) return null;

      return {
        label: `Stage ${stage.orderTree} : ${stage.name}`,
        value: stage.id,
        options: tasks.map((task) => ({
          label: `Task ${stage.orderTree}.${task.orderTree} : ${task.name}`,
          value: task.id,
        })),
      };
    })
    .filter(Boolean);
};
