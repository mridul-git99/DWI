import React, { useRef } from 'react';
import { useScrollableSections } from '#components';
import { createGroupConfig } from './helper';
import { AddGroupFooter } from './AddGroupFooter';
import styled from 'styled-components';

const UpsertUserGroupWrapper = styled.div`
  .upsert-user-container {
    display: flex;
    flex-direction: row;

    .label-setup {
      flex: 1;
    }

    .views-setup {
      flex: 5;
      margin-top: 5vh;
      overflow-y: scroll;
      max-height: 80vh;
      .group-header {
        font-size: 20px;
        font-weight: bold;
        margin: 20px 0;
      }
    }

    .user-container {
      width: 98%;

      .filter-bar {
        margin: 20px;
        display: flex;
        gap: 16px;
        justify-content: space-between;

        > div {
          display: flex;
          gap: 16px;
        }
      }

      .no-user-found {
        font-size: 16px;
        text-align: center;
        color: #ccc;
        margin-bottom: 40px;
      }

      .adduser-btn {
        width: max-content;
        margin: 50px auto;
      }
    }

    .users-list {
      max-height: 350px;
      width: 98%;
      margin: 20px auto;
      overflow-y: scroll;
      padding: 0 24px 24px;
    }
  }
`;

export const UpsertUserGroupConfig = ({
  handleUpsertPayload,
  userList,
  id,
  isEdit,
  usersId,
  setUsersId,
  userRemovalReason,
  setUserRemovalReason,
  readOnly,
}: any) => {
  const isDirtyForm = useRef(false);
  const { renderLabels, renderViews } = useScrollableSections({
    title: <div className="group-header">Add New Group</div>,
    items: createGroupConfig({
      handleUpsertPayload,
      isDirtyForm,
      userList,
      id,
      isEdit,
      usersId,
      setUsersId,
      userRemovalReason,
      setUserRemovalReason,
      readOnly,
    }),
  });

  return (
    <UpsertUserGroupWrapper>
      <div className="upsert-user-container">
        <div className="label-setup">{renderLabels()}</div>
        <div className="views-setup">{renderViews()}</div>
      </div>
      <AddGroupFooter
        addUserGroup={handleUpsertPayload}
        isEdit={isEdit}
        isDirtyForm={isDirtyForm}
      />
    </UpsertUserGroupWrapper>
  );
};
