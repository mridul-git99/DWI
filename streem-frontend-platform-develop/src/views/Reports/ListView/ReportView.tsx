import { LoadingContainer } from '#components';
import { useTypedSelector } from '#store';
import React, { FC } from 'react';
import styled from 'styled-components';

const ReportContentWrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  height: 100%;

  .report-iframe-content {
    height: 100%;
    width: 100%;
  }

  .report-iframe-dashboard {
    height: 100%;
    width: 100%;
  }
`;

const ReportView: FC = () => {
  const { loading, report } = useTypedSelector((state) => state.reports);

  return (
    <ReportContentWrapper>
      <LoadingContainer
        loading={loading}
        component={
          <div className="report-iframe-content">
            <iframe
              className="report-iframe-dashboard"
              src={report?.uri}
              title="Dashboard"
              frameBorder="0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowFullScreen={true}
            ></iframe>
          </div>
        }
      />
    </ReportContentWrapper>
  );
};

export default ReportView;
