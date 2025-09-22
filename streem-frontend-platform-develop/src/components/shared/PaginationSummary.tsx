import React, { FC } from 'react';
import styled from 'styled-components';
import { Pageable } from '#utils/globalTypes';

const Wrapper = styled.div`
  color: #525252;
  font-size: 14px;
  margin-left: auto;
  align-self: center;
  padding-right: 16px;

  span {
    font-weight: bold;
  }
`;

type PaginationSummaryProps = {
  pageable: Pageable;
  entityName?: string;
};

const PaginationSummary: FC<PaginationSummaryProps> = ({ pageable, entityName = '' }) => {
  const { numberOfElements, totalElements } = pageable || {};
  if (!numberOfElements) return null;
  return (
    <Wrapper>
      showing <span>{numberOfElements}</span> of <span>{totalElements}</span> {entityName}
    </Wrapper>
  );
};

export default PaginationSummary;
