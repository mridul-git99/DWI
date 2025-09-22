import { Button } from '#components';
import { useQueryParams } from '#hooks/useQueryParams';
import { useTypedSelector } from '#store/helpers';
import { AddCircleOutline } from '@material-ui/icons';
import React, { createRef, FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { addNewTask, setActiveTask } from './actions';
import DynamicTaskDrawer from './DynamicTaskDrawer';
import { TaskListWrapper } from './styles';
import TaskCard from './TaskCard';

const Tasks: FC<{ isReadOnly: boolean }> = ({ isReadOnly }) => {
  const { getQueryParam } = useQueryParams();

  const taskId = getQueryParam('taskId');
  const stageId = getQueryParam('stageId');

  let refMap: React.RefObject<HTMLDivElement>[] = [];

  const dispatch = useDispatch();
  const {
    stages: { activeStageId },
    tasks: { tasksOrderInStage, listById, activeTaskId },
    data,
  } = useTypedSelector((state) => state.prototypeComposer);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  useEffect(() => {
    if (taskId && activeStageId) {
      const taskListOrder = tasksOrderInStage[stageId] ?? [];

      dispatch(setActiveTask(taskId));

      const index = taskListOrder.findIndex((id) => id === taskId);
      refMap[index]?.current?.scrollIntoView({
        behavior: 'smooth',
      });
    }
  }, [taskId, activeStageId]);

  if (activeStageId) {
    const taskListOrder = tasksOrderInStage[activeStageId];
    refMap = taskListOrder.map(() => createRef<HTMLDivElement>());

    return (
      <TaskListWrapper>
        {taskListOrder?.map((taskId, index) => {
          const task = listById[taskId];
          return (
            <div className="task-list-item" key={`${task.id}-${index}`} ref={refMap[index]}>
              <TaskCard
                task={task}
                index={index}
                isActive={taskId === activeTaskId}
                isFirstTask={index === 0}
                isLastTask={index === taskListOrder.length - 1}
                isReadOnly={isReadOnly}
              />
            </div>
          );
        })}
        {!isReadOnly && (
          <div style={{ display: 'flex' }}>
            <Button
              variant="secondary"
              className="add-item"
              onClick={() => {
                if (data && activeStageId) {
                  dispatch(
                    addNewTask({
                      checklistId: data.id,
                      stageId: activeStageId,
                    }),
                  );
                }
              }}
            >
              <AddCircleOutline className="icon" fontSize="small" />
              Add New Task
            </Button>
            <Button
              variant="secondary"
              className="add-item"
              onClick={() => {
                setIsDrawerOpen(true);
              }}
              disabled
            >
              <AddCircleOutline className="icon" fontSize="small" />
              Add Dynamic Task
            </Button>
          </div>
        )}
        {isDrawerOpen && <DynamicTaskDrawer onCloseDrawer={setIsDrawerOpen} />}
      </TaskListWrapper>
    );
  }

  return null;
};

export default Tasks;
