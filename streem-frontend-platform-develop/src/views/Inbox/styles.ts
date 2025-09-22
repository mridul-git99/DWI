import styled from 'styled-components';

const InboxJobsWrapper = styled.div`
  display: flex;
  flex: 1;
  height: 100%;

  .MuiTabs-root {
    min-height: 40px;
  }

  .MuiTabs-indicator {
    height: 2px;
  }

  .jobs-tabs-list {
    .MuiTab-root {
      max-width: 160px;
      padding: 0px 12px;
      min-height: 40px;
    }
  }

  .jobs-tabs {
    width: 100%;
  }

  .jobs-tabs-panel {
    padding: 16px 0px 0px;
    height: calc(100% - 49px);

    > div {
      height: 100%;
    }
  }
`;

const InboxWrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  height: 100%;
  background-color: #ffffff;
  padding: 8px 16px 0px 16px;
`;

export { InboxJobsWrapper, InboxWrapper };
