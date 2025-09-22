import { DataTable, LoadingContainer, Pagination } from '#components';
import { useTypedSelector } from '#store';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { fetchDataParams } from '#utils/globalTypes';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { navigate } from '@reach/router';
import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { fetchReport, fetchReportSuccess, fetchReports } from './action';

const TabContent = () => {
  const dispatch = useDispatch();

  const { selectedUseCase } = useTypedSelector((state) => state.auth);

  const {
    reports: { list, pageable },
    loading,
    report,
  } = useTypedSelector((state) => state.reports);

  const fetchData = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE } = params;
    dispatch(
      fetchReports({
        page,
        size,
        sort: 'createdAt,desc',
      }),
    );
  };

  const reportClickHandler = (item) => {
    if (item.type === 'NON_EMBEDDED') {
      dispatch(fetchReport({ id: item.id, useCaseId: selectedUseCase.id }));
      navigate(`/reports/${item.id}`);
    }
  };

  useEffect(() => {
    //  if only one report is there directly open dashboard view
    if (list?.length === 1) {
      reportClickHandler(list?.[0]);
    }
  }, [report.id]);

  useEffect(() => {
    if (selectedUseCase?.id) {
      fetchData();
    }
  }, [selectedUseCase?.id]);

  useEffect(() => {
    return () => {
      dispatch(fetchReportSuccess({ data: {}, pageable: { ...pageable, page: 0 } }));
    };
  }, []);

  return (
    <TabContentWrapper>
      <LoadingContainer
        loading={loading}
        component={
          <DataTable
            columns={[
              {
                id: 'name',
                label: 'Report Title',
                minWidth: 240,
                format: function renderComp(item) {
                  return (
                    <span
                      className="primary"
                      onClick={() => reportClickHandler(item)}
                      title={item.name}
                    >
                      {item.name}
                    </span>
                  );
                },
              },
            ]}
            rows={list}
            emptyTitle="No Reports Found"
          />
        }
      />
      <Pagination pageable={pageable} fetchData={fetchData} />
    </TabContentWrapper>
  );
};

export default TabContent;
