import React from 'react';
import { DataTable, Pagination } from '#components';

export const UserGroupTable = ({
  columns,
  loading = false,
  currentPageData,
  pageable,
  fetchData,
}: any) => {
  return (
    <div style={{ ...(loading ? { display: 'none' } : { display: 'contents' }) }}>
      <DataTable columns={columns} rows={currentPageData} emptyTitle="No User Group Found" />
      <Pagination pageable={pageable} fetchData={fetchData} />
    </div>
  );
};
