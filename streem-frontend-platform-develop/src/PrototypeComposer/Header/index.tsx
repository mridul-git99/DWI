import { ProcessInitialState } from '#PrototypeComposer';
import {
  recallProcess,
  startChecklistReview,
  submitChecklistForReview,
} from '#PrototypeComposer/reviewer.actions';
import { CollaboratorState, CollaboratorType } from '#PrototypeComposer/reviewer.types';
import ActivityIcon from '#assets/svg/ActivityIcon';
import DownloadIcon from '#assets/svg/DownloadIcon';
import MemoArchive from '#assets/svg/Archive';
import MemoViewInfo from '#assets/svg/ViewInfo';
import errorIcon from '#assets/svg/error-icon.svg';
import { Button, ListActionMenu } from '#components';
import { closeAllOverlayAction, openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import checkPermission, { RoleIdByName } from '#services/uiPermissions';
import { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { ALL_FACILITY_ID } from '#utils/constants';
import { Error, SsoStates } from '#utils/globalTypes';
import { getErrorMsg, request, ssoSigningRedirect } from '#utils/request';
import { archiveChecklist, unarchiveChecklist } from '#views/Checklists/ListView/actions';
import { FormMode } from '#views/Checklists/NewPrototype/types';
import { MenuItem } from '@material-ui/core';
import {
  DoneAll,
  FiberManualRecord,
  Group,
  Info,
  Message,
  MoreVert,
  Settings,
} from '@material-ui/icons';
import KeyboardArrowLeftOutlinedIcon from '@material-ui/icons/KeyboardArrowLeftOutlined';
import { navigate } from '@reach/router';
import React, { FC, useCallback, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { validatePrototype } from '../actions';
import {
  AllChecklistStates,
  Checklist,
  ChecklistStates,
  ChecklistStatesColors,
  ChecklistStatesContent,
} from '../checklist.types';
import { apiValidateRecallProcess, apiPrintProcessPdf } from '#utils/apiUrls';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { downloadPdf } from '#utils/downloadPdf';
import { HeaderWrapper, ArrowButtonContainer, PublishDropdown, ArrowButtonStyled } from './styles';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';

const ListActionMenuButton = styled(ListActionMenu)`
  .MuiPaper-root {
    right: 16px !important;
    left: auto !important;
    top: 130px !important;
  }
`;

const ChecklistHeader: FC<ProcessInitialState> = ({
  isPrimaryAuthor,
  allDoneOk,
  areReviewsPending,
  reviewer,
  author,
  approver,
  headerNotification,
}) => {
  const dispatch = useDispatch();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [showPublishDropdown, setShowPublishDropdown] = useState<boolean>(false);

  const {
    data,
    listOrder,
    profile,
    selectedFacility: { id: facilityId = '' } = {},
    ssoIdToken,
    checklistErrors,
    stageListById,
    taskListById,
    parameterListById,
  } = useTypedSelector((state) => ({
    stageListById: state.prototypeComposer.stages.listById,
    taskListById: state.prototypeComposer.tasks.listById,
    userId: state.auth.userId,
    data: state.prototypeComposer.data as Checklist,
    listOrder: state.prototypeComposer.stages.listOrder,
    profile: state.auth.profile,
    selectedFacility: state.auth.selectedFacility,
    ssoIdToken: state.auth.ssoIdToken,
    checklistErrors: state.prototypeComposer.errors,
    parameterListById: state.prototypeComposer.parameters.listById,
  }));

  const isProcessArchived = useMemo(() => data?.archived, [data?.archived]);

  const handleSubmitForReview = (isViewer = false, showAssignment = true) => {
    if (showAssignment) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.SUBMIT_REVIEW_MODAL,
          props: {
            isViewer,
            isAuthor: !!author,
            isPrimaryAuthor,
          },
        }),
      );
    } else {
      dispatch(submitChecklistForReview(data.id));
    }
  };

  const handleSendToAuthor = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.SUBMIT_REVIEW_MODAL,
        props: {
          sendToAuthor: true,
          allDoneOk,
        },
      }),
    );
  };

  const onStartReview = () => {
    if (data && data.id) dispatch(startChecklistReview(data?.id));
  };

  const handleStartReview = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.CONFIRMATION_MODAL,
        props: {
          onPrimary: onStartReview,
          title: 'Start Reviewing',
          body: 'Are you sure you want to start reviewing this Prototype now?',
        },
      }),
    );
  };

  const handleContinueReview = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.SUBMIT_REVIEW_MODAL,
        props: {
          continueReview: true,
          reviewState: reviewer?.state,
        },
      }),
    );
  };

  const renderButtonsForReviewer = (state: CollaboratorState) => {
    switch (state) {
      case CollaboratorState.NOT_STARTED:
        return (
          <>
            <Button className="submit" onClick={handleStartReview}>
              Start Review
            </Button>
          </>
        );
      case CollaboratorState.BEING_REVIEWED:
        return (
          <>
            <Button className="submit" onClick={() => handleSubmitForReview(false)}>
              Provide Review
            </Button>
          </>
        );
      case CollaboratorState.COMMENTED_OK:
      case CollaboratorState.COMMENTED_CHANGES:
        return (
          <>
            {data?.state !== ChecklistStates.SIGNING_IN_PROGRESS &&
              data?.state !== ChecklistStates.READY_FOR_SIGNING && (
                <>
                  <Button
                    className="submit"
                    style={{ backgroundColor: '#333333' }}
                    onClick={handleContinueReview}
                  >
                    <Message style={{ fontSize: '16px', marginRight: '8px' }} />
                    Continue Review
                  </Button>
                  {!areReviewsPending && (
                    <Button
                      color={allDoneOk ? 'green' : 'blue'}
                      className="submit"
                      onClick={handleSendToAuthor}
                    >
                      <DoneAll style={{ fontSize: '16px', marginRight: '8px' }} />
                      Send to Author
                    </Button>
                  )}
                </>
              )}
          </>
        );
      default:
        return null;
    }
  };

  const onInitiateSignOff = () => {
    dispatch(closeAllOverlayAction());
    dispatch(
      openOverlayAction({
        type: OverlayNames.INITIATE_SIGNOFF,
      }),
    );
  };

  const handleInitiateSignOff = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.CONFIRMATION_MODAL,
        props: {
          onPrimary: onInitiateSignOff,
          title: 'Initiate Sign Off',
          body: 'Are you sure you want to Initiate the Sign Off?',
        },
      }),
    );
  };

  const PrototypeEditButton = () => (
    <Button
      id="edit"
      variant="secondary"
      onClick={() =>
        navigate('prototype', {
          state: {
            mode:
              isPrimaryAuthor &&
              !isProcessArchived &&
              (data?.state === ChecklistStates.BEING_BUILT ||
                data?.state === ChecklistStates.REQUESTED_CHANGES)
                ? FormMode.EDIT
                : FormMode.VIEW,
            formData: {
              description: data?.description,
              name: data.name,
              properties: data.properties,
              authors: data.collaborators.filter((u) => u.type === CollaboratorType.AUTHOR),
              prototypeId: data.id,
              createdBy: data.audit?.createdBy,
              colorCode: data.colorCode,
            },
          },
        })
      }
    >
      <Settings className="icon" fontSize="small" />
    </Button>
  );

  const handleClose = () => {
    setAnchorEl(null);
  };

  const ArchiveMenuItem = () => (
    <MenuItem
      onClick={() => {
        handleClose();
        dispatch(
          openOverlayAction({
            type: OverlayNames.REASON_MODAL,
            props: {
              modalTitle: data?.archived ? 'Unarchive Process' : 'Archive Process',
              modalDesc: `Provide details for ${
                data?.archived ? 'unarchiving' : 'archiving'
              } the process`,
              onSubmitHandler: (reason: string, setFormErrors: (errors?: Error[]) => void) =>
                data?.archived
                  ? dispatch(unarchiveChecklist(data?.id, reason, setFormErrors))
                  : dispatch(archiveChecklist(data?.id, reason, setFormErrors)),
            },
          }),
        );
      }}
    >
      <div className="list-item">
        <MemoArchive />
        <span>{data?.archived ? 'Unarchive Process' : 'Archive Process'}</span>
      </div>
    </MenuItem>
  );

  const checkArchivePermission = () => {
    if (data?.global) {
      if (facilityId === ALL_FACILITY_ID) return true;
    } else if (checkPermission(['checklists', 'archive'])) {
      return true;
    }
    return false;
  };

  const checkDisplayRecallProcess = (checklistState: AllChecklistStates) => {
    const recallProcessValidStates = [
      ChecklistStates.SUBMITTED_FOR_REVIEW,
      ChecklistStates.BEING_REVIEWED,
      ChecklistStates.REQUESTED_CHANGES,
      ChecklistStates.READY_FOR_SIGNING,
      ChecklistStates.SIGN_OFF_INITIATED,
      ChecklistStates.SIGNING_IN_PROGRESS,
      ChecklistStates.READY_FOR_RELEASE,
    ];

    if (recallProcessValidStates.includes(checklistState)) {
      return true;
    } else {
      return false;
    }
  };

  const handleRecallProcess = async () => {
    const { data: apiData, errors } = await request('PATCH', apiValidateRecallProcess(data?.id));
    if (apiData) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.REASON_MODAL,
          props: {
            modalTitle: 'Recall Process',
            modalDesc:
              'Provide details for recalling this process in the middle for the review cycle. All reviewers will be notified that this process has been recalled due to the reason stated below',
            onSubmitHandler: (reason: string, closeModal: () => void) => {
              dispatch(recallProcess(reason, data?.id));
              closeModal();
            },
          },
        }),
      );
    } else if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
  };

  const handleDownloadProcess = async () => {
    const timestamp = Date.now();
    await downloadPdf({
      url: apiPrintProcessPdf(data.id),
      method: 'GET',
      filename: `${data.code}_${timestamp}`,
    });
  };

  const MoreButton = () => (
    <>
      <Button
        id="more"
        variant="secondary"
        onClick={(event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
          setAnchorEl(event.currentTarget);
        }}
      >
        <MoreVert className="icon" fontSize="small" />
      </Button>
      <ListActionMenuButton
        style={{ right: 10 }}
        id="row-more-actions"
        anchorEl={anchorEl}
        keepMounted
        disableEnforceFocus
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem
          onClick={() => {
            handleClose();
            dispatch(
              openOverlayAction({
                type: OverlayNames.CHECKLIST_INFO,
                props: { checklistId: data.id },
              }),
            );
          }}
        >
          <div className="list-item">
            <MemoViewInfo />
            <span>View Info</span>
          </div>
        </MenuItem>
        <MenuItem
          onClick={() => {
            handleClose();
            navigate(`${data.id}/activities`);
          }}
        >
          <div className="list-item">
            <ActivityIcon />
            <span>View Activities</span>
          </div>
        </MenuItem>
        {isFeatureAllowed('downloadProcess') && data?.state != ChecklistStates.BEING_BUILT && (
          <MenuItem
            onClick={() => {
              handleClose();
              handleDownloadProcess();
            }}
          >
            <div className="list-item">
              <DownloadIcon />
              <span>Process Template</span>
            </div>
          </MenuItem>
        )}
        {checkDisplayRecallProcess(data?.state) && checkRecallPermission() ? (
          <MenuItem
            onClick={() => {
              handleClose();
              handleRecallProcess();
            }}
          >
            <div className="list-item">
              <MemoViewInfo />
              <span>Recall Process</span>
            </div>
          </MenuItem>
        ) : null}

        {facilityId !== ALL_FACILITY_ID && data?.state === ChecklistStates.PUBLISHED && (
          <MenuItem onClick={() => navigate(`/checklists/${data?.id}/logs`)}>
            <div className="list-item">
              <MemoViewInfo />
              <span>View Job Logs</span>
            </div>
          </MenuItem>
        )}
        {data?.state === ChecklistStates.PUBLISHED || data?.audit?.createdBy?.archived
          ? checkArchivePermission() && <ArchiveMenuItem />
          : null}
      </ListActionMenuButton>
    </>
  );

  const AuthorSubmitButton = ({
    title,
    disabled = false,
  }: {
    title: string;
    disabled?: boolean;
  }) => (
    <Button
      disabled={disabled}
      className="submit"
      onClick={() => dispatch(validatePrototype(data.id, false))}
    >
      {title}
    </Button>
  );

  const InitiateSignOffButton = ({ title }: { title: string }) => (
    <Button className="submit" onClick={() => handleInitiateSignOff()}>
      {title}
    </Button>
  );

  const ViewReviewersButton = () => (
    <Button id="view-collaborators" variant="secondary" onClick={() => handleSubmitForReview(true)}>
      <Group className="icon" fontSize="small" />
    </Button>
  );

  const ViewSigningStateButton = () => (
    <Button
      variant="secondary"
      onClick={() => dispatch(openOverlayAction({ type: OverlayNames.SIGN_OFF_PROGRESS }))}
    >
      View Signing Status
    </Button>
  );

  const SignOffButton = () => (
    <Button
      className="submit"
      onClick={() => {
        if (ssoIdToken) {
          ssoSigningRedirect({
            checklistId: data.id,
            location: `/checklists/${data.id}`,
            state: SsoStates.SIGN_OFF,
          });
        } else {
          dispatch(openOverlayAction({ type: OverlayNames.PASSWORD_INPUT }));
        }
      }}
    >
      Sign
    </Button>
  );
  const handlePublishClick = useCallback(() => {
    dispatch(validatePrototype(data.id, true));
    setShowPublishDropdown(false);
  }, [dispatch, data, setShowPublishDropdown]);

  const handleArrowClick = () => {
    setShowPublishDropdown(!showPublishDropdown);
  };

  const shouldRenderArrowButton = useMemo(() => {
    return (
      data?.state === ChecklistStates.BEING_BUILT &&
      checkPermission(['checklists', 'quickPublish']) &&
      isFeatureAllowed('quickPublish') &&
      isPrimaryAuthor &&
      !isProcessArchived
    );
  }, [data?.state, isPrimaryAuthor, !isProcessArchived]);

  const PublishPrototypeButton = () => {
    return (
      <PublishDropdown show={showPublishDropdown}>
        <Button onClick={handlePublishClick}>Publish Prototype</Button>
      </PublishDropdown>
    );
  };

  const renderButtonsForAuthor = () => {
    switch (data?.state) {
      case ChecklistStates.BEING_BUILT:
        return (
          <>
            {isPrimaryAuthor && (
              <AuthorSubmitButton disabled={!listOrder.length} title="Submit For Review" />
            )}
          </>
        );

      case ChecklistStates.SUBMITTED_FOR_REVIEW:
      case ChecklistStates.BEING_REVIEWED:
        return (
          <>
            <ViewReviewersButton />
          </>
        );

      case ChecklistStates.REQUESTED_CHANGES:
        return (
          <>
            <ViewReviewersButton />
            {isPrimaryAuthor && (
              <AuthorSubmitButton disabled={!listOrder.length} title="Submit For Review" />
            )}
          </>
        );

      case ChecklistStates.READY_FOR_SIGNING:
        return (
          <>
            <ViewReviewersButton />
            {isPrimaryAuthor && <InitiateSignOffButton title="Initiate Sign Off " />}
          </>
        );

      default:
        return (
          <>
            <ViewReviewersButton />
          </>
        );
    }
  };

  const checkReleasePermission = () => {
    if (data?.state === ChecklistStates.READY_FOR_RELEASE) {
      if (facilityId === ALL_FACILITY_ID) {
        return checkPermission(['checklists', 'releaseGlobal']);
      } else {
        return checkPermission(['checklists', 'release']);
      }
    }
    return false;
  };

  const checkRecallPermission = () => {
    if (facilityId === ALL_FACILITY_ID) {
      return checkPermission(['checklists', 'recallGlobal']);
    } else {
      return checkPermission(['checklists', 'recall']);
    }
  };

  const getErrorTitle = (error: Error) => {
    if (error.entity === 'stage') {
      const task = stageListById[error.id];
      return task ? `Stage ${stageListById[error.id]?.orderTree}` : false;
    } else if (error.entity === 'task') {
      const stage = stageListById[taskListById[error.id]?.stageId];
      return stage ? `Task ${stage?.orderTree}.${taskListById[error.id]?.orderTree}` : false;
    } else {
      const parameter = parameterListById[error.id];
      return parameter ? parameter.label : false;
    }
  };

  const parsedErrors = (checklistErrors || []).reduce<Array<any>>((acc, curr) => {
    const title = getErrorTitle(curr);
    if (title) {
      acc.push({
        ...curr,
        title,
      });
    }
    return acc;
  }, []);

  return (
    <HeaderWrapper>
      <div className="before-header">
        {data?.state !== ChecklistStates.PUBLISHED && headerNotification?.content && (
          <div className={`alert ${headerNotification.class || ''}`}>
            <Info />
            <span>{headerNotification.content}</span>
          </div>
        )}
      </div>
      <div className="main-header">
        <div className="header-content">
          <div style={{ display: 'flex', gap: '8px' }}>
            <KeyboardArrowLeftOutlinedIcon
              style={{ cursor: 'pointer' }}
              onClick={() =>
                navigate(`/checklists?tab=${data?.state === ChecklistStates.PUBLISHED ? '0' : '1'}`)
              }
            />
            <div className="header-content-left">
              <div className="checklist-name">{data?.name}</div>
              <div className="checklist-state">
                <FiberManualRecord
                  className="icon"
                  style={{ color: ChecklistStatesColors[data?.state] }}
                />
                <span>{ChecklistStatesContent[data?.state]}</span>
              </div>
            </div>
          </div>

          <div className="header-content-right">
            {parsedErrors.length > 0 ? (
              <div
                className="error-popover"
                aria-haspopup="true"
                onClick={(event) => {
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.CHECKLIST_ERRORS,
                      popOverAnchorEl: event.currentTarget,
                      props: { errors: parsedErrors },
                    }),
                  );
                }}
              >
                <div className="checklist-errors">
                  <img src={errorIcon} />
                  <span>{parsedErrors.length} issues found</span>
                </div>
              </div>
            ) : null}
            {<PrototypeEditButton />}
            {author && !approver && !isProcessArchived && renderButtonsForAuthor()}

            {shouldRenderArrowButton && (
              <ArrowButtonContainer>
                <ArrowButtonStyled onClick={handleArrowClick}>
                  <KeyboardArrowDownIcon />
                </ArrowButtonStyled>
              </ArrowButtonContainer>
            )}
            {PublishPrototypeButton()}

            {reviewer && !approver && (
              <>
                <ViewReviewersButton />
                {data?.state !== ChecklistStates.REQUESTED_CHANGES &&
                data?.state !== ChecklistStates.BEING_BUILT &&
                !isProcessArchived
                  ? renderButtonsForReviewer(reviewer.state)
                  : null}
              </>
            )}
            {/* Note: We check only against the first value of the array as currently we support a user being assigned a single role only*/}
            {((approver && data?.state !== ChecklistStates.PUBLISHED) ||
              (!!(profile?.roles?.[0].id === RoleIdByName.CHECKLIST_PUBLISHER) &&
                data?.state === ChecklistStates.READY_FOR_RELEASE)) && (
              <>
                <ViewReviewersButton />
                <ViewSigningStateButton />
              </>
            )}
            {approver &&
              !isProcessArchived &&
              data?.state !== ChecklistStates.PUBLISHED &&
              approver?.state !== CollaboratorState.SIGNED && <SignOffButton />}
            {checkReleasePermission() && !isProcessArchived && (
              <Button
                className="submit"
                onClick={() => {
                  if (ssoIdToken) {
                    ssoSigningRedirect({
                      checklistId: data.id,
                      location: `/checklists/${data.id}`,
                      state: SsoStates.RELEASE,
                    });
                  } else {
                    dispatch(
                      openOverlayAction({
                        type: OverlayNames.PASSWORD_INPUT,
                        props: {
                          isReleasing: true,
                        },
                      }),
                    );
                  }
                }}
              >
                Release Prototype
              </Button>
            )}
            <MoreButton />
          </div>
        </div>
      </div>
    </HeaderWrapper>
  );
};

export default ChecklistHeader;
