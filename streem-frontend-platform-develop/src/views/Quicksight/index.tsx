import { DropdownFilter } from '#components';
import { useTypedSelector } from '#store';
import {
  apiGetAuthorConsoleUrl,
  apiGetReaderDashboardIds,
  apiGetReaderDashboardUrl,
  apiGetReportRole,
} from '#utils/apiUrls';
import axiosInstance, { setAuthHeader } from '#utils/axiosClient';
import { RouteComponentProps } from '@reach/router';
import { createEmbeddingContext } from 'amazon-quicksight-embedding-sdk';
import React, { FC, useEffect, useState } from 'react';

const QuicksightView: FC<RouteComponentProps> = () => {
  const { accessToken } = useTypedSelector((state) => state.auth);
  const [dashboards, setDashboards] = useState<any[]>([]);
  const dashboardRef = React.useRef<HTMLDivElement>(null);
  const didInit = React.useRef(false);
  async function switchDashboard(dashboardId: any) {
    axiosInstance.get(apiGetReaderDashboardUrl(dashboardId)).then((response) => {
      loadDashboard(response.data.message);
    });
  }
  async function loadDashboard(dashboardUrl: any) {
    if (dashboardRef.current) {
      dashboardRef.current.innerHTML = '';
    }

    const embeddingContext = await createEmbeddingContext({
      onChange: (changeEvent: any, metadata: any) => {
        console.log('Context received a change', changeEvent, metadata);
      },
    });
    const frameOptions = {
      url: dashboardUrl,
      // replace this value with the url generated via embedding API
      container: dashboardRef.current,
      height: window.innerHeight - 96 + 'px',
      width: window.innerWidth - 80 + 'px',
      onChange: (changeEvent: { eventName: any }) => {
        switch (changeEvent.eventName) {
          case 'FRAME_MOUNTED': {
            console.log('dashboard experience frame is mounted.');
            break;
          }
          case 'FRAME_LOADED': {
            console.log('dashboard experience frame is loaded.');
            break;
          }
        }
      },
    };
    const contentOptions = {
      toolbarOptions: {
        export: true,
      },
      onMessage: async (messageEvent: { eventName: any }) => {
        switch (messageEvent.eventName) {
          case 'ERROR_OCCURRED': {
            console.error('dashboard embedded experience fails loading.');
            break;
          }
        }
      },
    };
    await embeddingContext.embedDashboard(frameOptions, contentOptions);
  }

  async function loadReport(consoleUrl: any) {
    const embeddingContext = await createEmbeddingContext({
      onChange: (changeEvent: any, metadata: any) => {
        console.log('Context received a change', changeEvent, metadata);
      },
    });
    const frameOptions = {
      url: consoleUrl,
      container: dashboardRef.current,
      height: window.innerHeight - 96 + 'px',
      width: window.innerWidth - 80 + 'px',
      onChange: (changeEvent: { eventName: any }) => {
        switch (changeEvent.eventName) {
          case 'FRAME_MOUNTED': {
            console.log('report experience frame is mounted.');
            break;
          }
          case 'FRAME_LOADED': {
            console.log('report experience frame is loaded.');
            break;
          }
        }
      },
    };
    const contentOptions = {
      toolbarOptions: {
        export: true,
      },
      onMessage: async (messageEvent: { eventName: any }) => {
        switch (messageEvent.eventName) {
          case 'ERROR_OCCURRED': {
            console.error('quicksight embeddingContext error', messageEvent);
            break;
          }
        }
      },
    };
    await embeddingContext.embedConsole(frameOptions, contentOptions);
  }

  function init() {
    setAuthHeader(accessToken);
    axiosInstance.get(apiGetReportRole()).then((response) => {
      if (response.data.message == 'author') {
        axiosInstance.get(apiGetAuthorConsoleUrl()).then((response) => {
          loadReport(response.data.message);
        });
      } else {
        axiosInstance.get(apiGetReaderDashboardIds()).then((response) => {
          // Populate dashboard dropdown
          let dashboardIds = response.data.message.replace('[', '').replace(']', '').split(',');
          let newDashboards: { label: string; value: string }[] = [];
          dashboardIds.forEach((e: string) =>
            newDashboards.push({ label: e.split(':')[1].trim(), value: e.split(':')[0].trim() }),
          );
          setDashboards(newDashboards);
        });
      }
    });
  }
  useEffect(() => {
    if (!didInit.current) {
      didInit.current = true;
      if (accessToken) {
        setTimeout(init, 200);
      }
    }
  }, [accessToken]);

  return (
    <div style={{ paddingTop: 16, paddingRight: 16 }}>
      <DropdownFilter
        label="Select Dashboard"
        options={dashboards}
        updateFilter={(option) => {
          switchDashboard(option.value);
        }}
      />
      <div style={{ flex: 1, marginInline: 16, marginTop: 16 }} ref={dashboardRef} />
    </div>
  );
};
export default QuicksightView;
