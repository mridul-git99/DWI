import { Button, StyledMenu } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import checkPermission, { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { apiPrintJobDetails } from '#utils/apiUrls';
import { downloadPdf } from '#utils/downloadPdf';
import { jobActions } from '#views/Job/jobStore';
import { useJobStateToFlags } from '#views/Job/utils';
import { MenuItem } from '@material-ui/core';
import { ChevronLeft, ChevronRight, MoreVert } from '@material-ui/icons';
import { navigate } from '@reach/router';
import React, { FC, useState } from 'react';
import { useDispatch } from 'react-redux';

const JobHeaderButtons: FC = () => {
  const dispatch = useDispatch();
  const {
    processName,
    id: jobId,
    code,
    isInboxView,
    taskNavState: { isMobileDrawerOpen },
  } = useTypedSelector((state) => state.job);
  const { isCompletedWithException, isCompleted } = useJobStateToFlags();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const showBulkAssignButton =
    !isInboxView &&
    !isCompleted &&
    !isCompletedWithException &&
    checkPermission(['jobs', 'bulkAssignTasks']);

  const handleClose = () => setAnchorEl(null);

  const handleClick: React.MouseEventHandler<HTMLDivElement> = (event) =>
    setAnchorEl(event.currentTarget);

  const handleDownload = async () => {
    const timestamp = Date.now();
    await downloadPdf({
      url: apiPrintJobDetails(jobId!),
      method: 'GET',
      filename: `Job_${code}_${timestamp}`,
    });
  };

  return (
    <div className="buttons-container">
      {isCompletedWithException && (
        <Button
          className="view-info"
          color="blue"
          variant="secondary"
          onClick={() =>
            dispatch(
              openOverlayAction({
                type: OverlayNames.SHOW_COMPLETED_JOB_WITH_EXCEPTION_INFO,
                props: {
                  jobId,
                  name: processName,
                  code,
                },
              }),
            )
          }
        >
          View Info
        </Button>
      )}

      {showBulkAssignButton && (
        <Button className="bulk-assign" onClick={() => navigate(`/jobs/${jobId}/assignments`)}>
          Bulk Assign Tasks
        </Button>
      )}

      <div
        className="more open-overview"
        onClick={() => {
          dispatch(jobActions.toggleMobileDrawer());
        }}
      >
        {isMobileDrawerOpen ? <ChevronRight /> : <ChevronLeft />}
      </div>

      <div className="more" onClick={handleClick}>
        <MoreVert />
      </div>

      <StyledMenu
        id="job-more-options-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
        style={{ marginTop: 40 }}
      >
        <MenuItem
          onClick={() => {
            // openLinkInNewTab(`/jobs/${jobId}/print`);
            handleDownload();
            handleClose();
          }}
        >
          Download Job
        </MenuItem>
        {(isCompleted || isCompletedWithException) &&
          isFeatureAllowed('jobAnnotation') &&
          checkPermission(['jobs', 'jobAnnotation']) && (
            <MenuItem
              onClick={() => {
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.JOB_ANNOTATIONS,
                    props: {
                      jobId,
                    },
                  }),
                );
                handleClose();
              }}
            >
              Job Annotation
            </MenuItem>
          )}
        <MenuItem
          className="job-activities"
          onClick={() => {
            navigate(`/${isInboxView ? 'inbox' : 'jobs'}/${jobId}/activities`);
            handleClose();
          }}
        >
          View Activities
        </MenuItem>
        <MenuItem
          className="view-verifications"
          onClick={() => {
            dispatch(
              openOverlayAction({
                type: OverlayNames.JOB_VERIFICATION,
                props: {
                  jobId,
                },
              }),
            );
            handleClose();
          }}
        >
          View Verifications
        </MenuItem>
        <MenuItem
          className="view-exceptions"
          onClick={() => {
            dispatch(
              openOverlayAction({
                type: OverlayNames.APPROVALS_LIST_MODAL,
                props: {
                  jobId,
                },
              }),
            );
            handleClose();
          }}
        >
          View Exceptions
        </MenuItem>
        {isFeatureAllowed('plannedVariation') && (
          <MenuItem
            className="view-verifications"
            onClick={() => {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.JOB_PARAMETER_VARIATION,
                  props: {
                    jobId,
                    isReadOnly: isCompleted || isCompletedWithException ? true : false,
                  },
                }),
              );
              handleClose();
            }}
          >
            Parameter Variation
          </MenuItem>
        )}
        {!isCompleted &&
          !isCompletedWithException &&
          checkPermission(['inbox', 'completeWithException']) && (
            <MenuItem
              onClick={() => {
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.COMPLETE_JOB_WITH_EXCEPTION,
                    props: { jobId, code, name },
                  }),
                );
                handleClose();
              }}
            >
              <span style={{ color: '#da1e28' }}>Complete Job with Exception</span>
            </MenuItem>
          )}
      </StyledMenu>
    </div>
  );
};

export default JobHeaderButtons;
