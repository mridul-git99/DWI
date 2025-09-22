import { Router } from '@reach/router';
import React, { FC } from 'react';
import { PAGE_TYPE } from './ManageUser/types';
import EditUser from './ManageUser';
import { UpsertUserGroupContainer } from './UpsertUserGroup/index';
import { EditUserGroupContainer } from './UpsertUserGroup/EditUserGroup';
import UserGroupAuditLogs from './ListView/UserGroupAuditLogs/AuditLogs';
import ListView from './ListView';
import { UserAccessViewProps } from './types';

const UserAccessView: FC<UserAccessViewProps> = () => (
  <Router>
    <ListView path="/" />
    <EditUser path="add" pageType={PAGE_TYPE.ADD} />
    <EditUser path="profile/:id" pageType={PAGE_TYPE.PROFILE} />
    <EditUser path="edit/:id" pageType={PAGE_TYPE.EDIT} />
    <UpsertUserGroupContainer path="group/add" pageType={PAGE_TYPE.ADD_USER_GROUP} />
    <EditUserGroupContainer path="group/edit/:id" pageType={PAGE_TYPE.ADD_USER_GROUP} />
    <UserGroupAuditLogs path="group/audit-logs/:id" />
  </Router>
);

export default UserAccessView;
