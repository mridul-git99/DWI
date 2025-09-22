import React from 'react';
import styled from 'styled-components';

const RoleWrapper = styled.div`
  display: flex;
  margin-left: auto;
  flex-wrap: wrap;
  max-width: 50%;

  .role-tag {
    padding: 4px 8px;
    margin: 4px;
    background-color: #f1f1f1;
    border-radius: 4px;
    font-size: 12px;
    color: #333;
  }

  .blue {
    background-color: #d0e2ff;
    color: #0043ce;
  }

  .green {
    background-color: #a7f0ba;
    color: #0e6027;
  }
`;

const getClassName = (roleName: any) => {
  switch (roleName) {
    case 'GLOBAL_ADMIN':
      return 'blue';
    default:
      return 'green';
  }
};

export const RoleTag = ({ roles }: any) => {
  return (
    <RoleWrapper>
      {roles?.map((role: any) => (
        <div className={`role-tag ${getClassName(role.name)}`} key={role.name}>
          {role.name?.split('_').join(' ')}
        </div>
      ))}
    </RoleWrapper>
  );
};
