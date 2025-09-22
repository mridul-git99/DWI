import { useTypedSelector } from '#store';
import React, { FC } from 'react';
import styled from 'styled-components';
import TaskNavCard from './TaskNavCard';
import AccordianIcon from '#assets/svg/accordianIcon.svg';

const StageDetailCardWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;

  .accordian-outer-container {
    cursor: pointer;
    display: flex;
    .accordian-container {
      display: flex;
      .accordian-image {
        margin: 4px;
        cursor: pointer;
        width: 8px;
      }
      .accordian-image-rotate {
        margin: 4px;
        cursor: pointer;
        transform: rotate(-90deg);
        width: 8px;
      }
      .stage-details {
        margin: 4px;
        font-size: 16px;
        font-weight: 700;
      }
    }
    .stage-name {
      font-weight: 700;
      font-size: 16px;
      letter-spacing: 0.16px;
      color: #000000;
      margin: 4px 0;
      margin-left: 12px;
    }
  }
`;

const StageNavCard: FC<{ stage: any; stageNo: number }> = ({ stage, stageNo }) => {
  const tasks = useTypedSelector((state) => state.job.tasks);
  const [accordianStatus, setAccordianStatus] = React.useState<boolean>(true);

  const renderTasks = (taskIds: string[]) => {
    const _tasks: JSX.Element[] = [];
    let taskNo = 1;
    taskIds.forEach((taskId) => {
      const task = tasks.get(taskId);
      !task!.hidden &&
        _tasks.push(<TaskNavCard task={task} taskNo={taskNo} key={task!.id} stageNo={stageNo} />);
      taskNo++;
    });
    return _tasks;
  };

  return (
    <StageDetailCardWrapper key={stage.id}>
      <div
        className="accordian-outer-container"
        onClick={() => setAccordianStatus(!accordianStatus)}
      >
        <div className="accordian-container">
          <img
            className={`${accordianStatus ? 'accordian-image' : 'accordian-image-rotate'}`}
            src={AccordianIcon}
            alt="accordian-icon"
          />
          <div className="stage-details">Stage {stageNo}</div>
        </div>
        <div className="stage-name">{stage.name}</div>
      </div>
      {accordianStatus && renderTasks(stage.tasks)}
    </StageDetailCardWrapper>
  );
};

export default StageNavCard;
