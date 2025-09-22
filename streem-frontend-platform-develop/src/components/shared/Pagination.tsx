import { DEFAULT_PAGE_SIZE } from '#utils/constants';
import { fetchDataParams, Pageable } from '#utils/globalTypes';
import { ArrowLeft, ArrowRight } from '@material-ui/icons';
import React, { FC } from 'react';
import styled from 'styled-components';
import { useQueryParams } from '../../hooks/useQueryParams';

const PaginationWrapper = styled.div.attrs(({ className = 'pagination', id = 'pagination' }) => ({
  className,
  id,
}))`
  margin-top: auto;
  padding: 4px 0 0;
  display: flex;
  justify-content: center;
  align-items: center;

  > .icon {
    color: #000000;
  }

  > span {
    cursor: pointer;
    padding: 8px 0;
    margin: 0 10px;
    font-size: 14px;
    border-bottom: 4px solid transparent;

    &.active {
      border-bottom-color: #1d84ff;
    }
  }

  @media (max-width: 900px) {
    margin-bottom: 16px;
  }
`;

export const Pagination: FC<{
  pageable: Pageable;
  pageSize?: number;
  fetchData?: boolean | (({ filters, page, size }: fetchDataParams) => void);
}> = ({ pageable, pageSize, fetchData }) => {
  const showPaginationArrows = pageable.totalPages > 10;
  const { updateQueryParams } = useQueryParams();

  return (
    <PaginationWrapper>
      <ArrowLeft
        className={`icon ${showPaginationArrows ? '' : 'hide'}`}
        onClick={() => {
          if (pageable.page > 0) {
            if (typeof fetchData === 'boolean') {
              updateQueryParams({
                newParams: { page: pageable.page - 1 },
                navigateOptions: { replace: true },
              });
            } else {
              fetchData?.({
                page: pageable.page - 1,
                size: pageSize || pageable.pageSize || DEFAULT_PAGE_SIZE,
              });
            }
          }
        }}
      />
      {Array.from({ length: pageable.totalPages }, (_, i) => i)
        .slice(Math.floor(pageable.page / 10) * 10, Math.floor(pageable.page / 10) * 10 + 10)
        .map((el) => (
          <span
            key={el}
            className={pageable.page === el ? 'active' : ''}
            onClick={() => {
              if (typeof fetchData === 'boolean') {
                updateQueryParams({
                  newParams: { page: el },
                  navigateOptions: { replace: true },
                });
              } else {
                fetchData?.({
                  page: el,
                  size: pageSize || pageable.pageSize || DEFAULT_PAGE_SIZE,
                });
              }
            }}
          >
            {el + 1}
          </span>
        ))}
      <ArrowRight
        className={`icon ${showPaginationArrows ? '' : 'hide'}`}
        onClick={() => {
          if (pageable.page < pageable.totalPages - 1) {
            if (typeof fetchData === 'boolean') {
              updateQueryParams({
                newParams: { page: pageable.page + 1 },
                navigateOptions: { replace: true },
              });
            } else {
              fetchData?.({
                page: pageable.page + 1,
                size: pageSize || pageable.pageSize || DEFAULT_PAGE_SIZE,
              });
            }
          }
        }}
      />
    </PaginationWrapper>
  );
};
