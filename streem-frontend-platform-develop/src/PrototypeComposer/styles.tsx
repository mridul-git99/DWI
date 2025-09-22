import styled from 'styled-components';

export const ComposerWrapper = styled.div`
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;

  .process-tabs {
    overflow: hidden;

    .process-tabs-list {
      background-color: #fff;
      border-top: 1px solid #f0f0f0;

      button {
        border-right: 1px solid #e0e0e0;
        padding: 6px 16px;
        flex-grow: unset;
        flex-basis: unset;
        min-width: unset;
      }
    }

    .process-tabs-panel {
      height: calc(100% - 49px);
      padding: 0;
    }
  }
`;

export const TasksTabWrapper = styled.div`
  display: grid;
  grid-column-gap: 8px;
  grid-template-areas: 'stage-list task-list';
  grid-template-columns: 320px 1fr;
  overflow: hidden;
  height: 100%;
  padding-top: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: 240px 1fr;

    svg {
      font-size: 16px !important;
    }
  }

  @media (min-width: 1201px) and (max-width: 1366px) {
    grid-template-columns: 280px 1fr;
  }

  .add-item {
    background-color: transparent;
    width: 100%;
    margin-bottom: 16px;

    :hover {
      color: unset;
      border-color: unset;
      > .icon {
        color: inherit;
      }
    }

    > .icon {
      margin-right: 8px;
    }
  }
`;

export const TabPanelWrapper = styled.div`
  height: 100%;
  margin: 16px 16px 0;
  padding: 16px;
  background-color: #fff;
  display: flex;
  flex-direction: column;
`;
