import { useTypedSelector } from '#store';
import React, { FC } from 'react';
import styled from 'styled-components';
import StageNavCard from './components/StageNavCard';

const TaskNavigationWrapper = styled.div.attrs({
  id: 'task-navigation',
})<{ $isMobileDrawerOpen: boolean }>`
  width: ${({ $isMobileDrawerOpen }) => ($isMobileDrawerOpen ? '100%' : '0%')};
  padding: ${({ $isMobileDrawerOpen }) => ($isMobileDrawerOpen ? '1.5rem' : '0')};
  @media (min-width: 900px) {
    width: 35%;
    padding: 1.5rem;
  }
  transition: all 0.3s;
  display: flex;
  background-color: #fff;
  overflow: auto;
  font-size: 0.875rem;
  flex-direction: column;
  gap: 1.5rem;
`;

const TaskNavigation: FC = () => {
  const {
    stages,
    taskNavState: { isMobileDrawerOpen },
  } = useTypedSelector((state) => state.job);

  const renderStages = () => {
    const _stages: JSX.Element[] = [];
    let stageNo = 1;
    stages.forEach((stage) => {
      !stage.hidden &&
        _stages.push(<StageNavCard stage={stage} stageNo={stageNo} key={stage.id} />);
      stageNo++;
    });
    return _stages;
  };

  return (
    <TaskNavigationWrapper $isMobileDrawerOpen={isMobileDrawerOpen} data-testid="task-nav">
      {renderStages()}
    </TaskNavigationWrapper>
  );
};

export default TaskNavigation;
