import { useTypedSelector } from '#store';
import { TaskExecutionStates } from '#types';
import { formatDuration } from '#utils/timeUtils';
import { jobActions } from '#views/Job/jobStore';
import { Timer as TimerIcon } from '@material-ui/icons';
import React, { FC, useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';

const Timer: FC<{
  state: TaskExecutionStates;
  id: string;
}> = ({ state, id }) => {
  const dispatch = useDispatch();
  const timerState = useTypedSelector((state) => state.job.timerState);
  const previousTaskState = useRef<TaskExecutionStates>();

  useEffect(() => {
    if (previousTaskState.current !== state) {
      if (!previousTaskState.current || state === 'IN_PROGRESS') {
        dispatch(jobActions.stopTaskTimer());
        dispatch(jobActions.startTaskTimer({ id }));
      }
      previousTaskState.current = state;
    }
    return () => {
      dispatch(jobActions.stopTaskTimer());
    };
  }, [state]);

  return (
    <div className="task-timer">
      <div className="timer-config">
        <TimerIcon className="icon" />
      </div>
      <div className={`timer ${timerState.limitCrossed ? 'error' : ''}`}>
        <span>{formatDuration(timerState.timeElapsed)}</span>
      </div>
    </div>
  );
};

export default Timer;
