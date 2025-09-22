import { useDrawer } from '#components';
import {
  ShowSectionUsersAndUserGroupTabs,
  useUserAndUserGroupTabs,
} from '#hooks/useUserAndUserGroupTabs';
import { useTypedSelector } from '#store';
import { apiParameterVerificationGroupAssignees, apiVerificationAssignees } from '#utils/apiUrls';
import React, { useCallback, useEffect } from 'react';

export const AssignPeerVerificationContainer = (props) => {
  const { onCloseDrawer } = props;
  const { id: jobId } = useTypedSelector((state) => state.job);

  useEffect(() => {
    setDrawerOpen(true);
  }, [onCloseDrawer]);

  const handleCloseDrawer = useCallback(() => {
    setDrawerOpen && setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  }, [onCloseDrawer, setDrawerOpen]);

  const { bodyContent, footerContent } = useUserAndUserGroupTabs({
    apiUrlUser: () => apiVerificationAssignees(jobId!),
    apiUrlUserGroup: () => apiParameterVerificationGroupAssignees(jobId),
    handleCloseDrawer,
    showRoleTag: false,
    showSections: ShowSectionUsersAndUserGroupTabs.BOTH,
    shouldFilterList: true,
    ...props,
  });

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Assign Users',
    hideCloseIcon: true,
    bodyContent: <>{bodyContent()}</>,
    footerContent: <>{footerContent()}</>,
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return StyledDrawer;
};
