import { BaseModal, Checkbox } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { Checklist, ChecklistStates } from '#PrototypeComposer/checklist.types';
import {
  Collaborator,
  CollaboratorState,
  CollaboratorType,
} from '#PrototypeComposer/reviewer.types';
import { defaultParams, OtherUserState, useUsers } from '#services/users';
import { useTypedSelector } from '#store';
import { ALL_FACILITY_ID } from '#utils/constants';
import { FilterOperators } from '#utils/globalTypes';
import { getInitials } from '#utils/stringUtils';
import { usePrevious } from '#utils/usePrevious';
import { Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import {
  assignReviewersToChecklist,
  assignReviewerToChecklist,
  fetchAssignedReviewersForChecklist,
  revertReviewersForChecklist,
  unAssignReviewerFromChecklist,
} from '../reviewer.actions';
import Wrapper from './ReviewerAssignment.styles';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';

type initialState = {
  assignedUsers: Collaborator['id'][];
  unassignedUsers: Collaborator['id'][];
  searchQuery: string;
  preAssignedUsers: Collaborator[];
};

const initialState: initialState = {
  assignedUsers: [],
  unassignedUsers: [],
  searchQuery: '',
  preAssignedUsers: [],
};

const ReviewerAssignmentModal: FC<
  CommonOverlayProps<{
    checklistId: Checklist['id'];
    isModal: boolean;
  }>
> = ({ closeAllOverlays = () => false, closeOverlay, props: { checklistId, isModal = true } }) => {
  const {
    data: { collaborators, state: checklistState },
    assignees,
    selectedFacility: { id: facilityId = '' } = {},
  } = useTypedSelector((state) => ({
    assignees: state.prototypeComposer.collaborators,
    data: state.prototypeComposer.data as Checklist,
    selectedFacility: state.auth.selectedFacility,
  }));

  const dispatch = useDispatch();
  const [state, setState] = useState(initialState);
  const { assignedUsers, unassignedUsers, searchQuery, preAssignedUsers } = state;
  const prevSearch = usePrevious(searchQuery);
  const prevAssignees = usePrevious(assignees);

  const {
    users: list,
    loadMore,
    loadAgain,
  } = useUsers({
    userState:
      facilityId === ALL_FACILITY_ID ? OtherUserState.REVIEWERS_GLOBAL : OtherUserState.REVIEWERS,
    params: { ...defaultParams(false) },
  });

  useEffect(() => {
    dispatch(fetchAssignedReviewersForChecklist(checklistId));
    return () => {
      dispatch(revertReviewersForChecklist([]));
    };
  }, []);

  useEffect(() => {
    if (
      (prevAssignees === undefined || prevAssignees.length === 0) &&
      assignees.length > 0 &&
      assignedUsers.length === 0 &&
      unassignedUsers.length === 0
    ) {
      setState({ ...state, preAssignedUsers: assignees });
    }
  }, [assignees]);

  useEffect(() => {
    if (prevSearch !== searchQuery) {
      loadAgain({
        newParams: {
          ...defaultParams(false),
          ...(searchQuery && {
            filters: generateUserSearchFilters(FilterOperators.LIKE, searchQuery),
          }),
        },
      });
    }
  }, [searchQuery]);

  const handleOnScroll = (e: React.UIEvent<HTMLElement>) => {
    e.stopPropagation();
    const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
    if (scrollTop + clientHeight >= scrollHeight - clientHeight * 0.7) loadMore();
  };

  const onCheckChanged = (user: Collaborator, checked: boolean, isPreAssigned: boolean) => {
    if (checked) {
      if (isPreAssigned) {
        setState({
          ...state,
          unassignedUsers: [...unassignedUsers, user.id],
          assignedUsers: assignedUsers.filter((i) => i !== user.id),
        });
        dispatch(unAssignReviewerFromChecklist(user));
      } else {
        setState({
          ...state,
          assignedUsers: assignedUsers.filter((i) => i !== user.id),
        });
        dispatch(unAssignReviewerFromChecklist(user));
      }
    } else {
      if (isPreAssigned) {
        setState({
          ...state,
          unassignedUsers: unassignedUsers.filter((i) => i !== user.id),
        });
      } else {
        setState({
          ...state,
          assignedUsers: [...assignedUsers, user.id],
          unassignedUsers: unassignedUsers.filter((i) => i !== user.id),
        });
      }
      dispatch(assignReviewerToChecklist(user));
    }
  };

  const userRow = (user: Collaborator, checked: boolean, isPreAssigned: boolean) => {
    return (
      <div className="item" key={`user_${user.id}`}>
        <div className="right">
          {checklistId && (
            <Checkbox
              checked={checked}
              onClick={() => onCheckChanged(user, checked, isPreAssigned)}
              disabled={collaborators.some(
                (collaborator) =>
                  collaborator.id === user.id &&
                  collaborator.state !== CollaboratorState.NOT_STARTED,
              )}
            />
          )}
        </div>
        <div className="thumb">{getInitials(`${user.firstName} ${user.lastName}`)}</div>
        <div className="middle">
          <span className="userId">{user.employeeId}</span>
          <span className="userName">{`${user.firstName} ${user.lastName}`}</span>
        </div>
      </div>
    );
  };

  const bodyView: JSX.Element[] = [];

  if (list) {
    if (searchQuery === '') {
      preAssignedUsers.forEach((user) => {
        if (user.id !== '0') {
          const checked = assignees.some((item) => item.id === user.id);
          const isAuthor = collaborators.some(
            (item) =>
              item.id === user.id &&
              (item.type === CollaboratorType.AUTHOR ||
                item.type === CollaboratorType.PRIMARY_AUTHOR),
          );
          if (!isAuthor) bodyView.push(userRow(user, checked, true));
        }
      });
    }

    (list as unknown as Array<Collaborator>).forEach((user) => {
      const isPreAssigned = preAssignedUsers.some((item) => item.id === user.id);
      const checked = assignees.some((item) => item.id === user.id);
      const isAuthor = collaborators.some(
        (item) =>
          item.id === user.id &&
          (item.type === CollaboratorType.AUTHOR || item.type === CollaboratorType.PRIMARY_AUTHOR),
      );

      if (user.id !== '0' && !isAuthor) {
        if (searchQuery !== '') {
          bodyView.push(userRow(user, checked, isPreAssigned));
        } else if (!isPreAssigned) {
          bodyView.push(userRow(user, checked, isPreAssigned));
        }
      }
    });
  }

  const onPrimary = () => {
    if (checklistId)
      dispatch(
        assignReviewersToChecklist({
          checklistId: checklistId,
          assignIds: assignedUsers,
          unassignIds: unassignedUsers,
        }),
      );
    closeOverlay();
  };

  const onSecondary = () => {
    closeOverlay();
  };

  return (
    <Wrapper isModal={isModal}>
      <BaseModal
        animated={isModal}
        closeAllModals={closeAllOverlays}
        closeModal={onSecondary}
        title="Add Reviewers"
        primaryText="Confirm"
        secondaryText="Cancel"
        onSecondary={onSecondary}
        onPrimary={onPrimary}
        disabledPrimary={
          isModal
            ? checklistState === ChecklistStates.BEING_BUILT &&
              !state.assignedUsers.length &&
              !state.unassignedUsers.length
            : !state.assignedUsers.length && !state.unassignedUsers.length
        }
      >
        <div className="top-content">
          <div className="searchboxwrapper">
            <Search className="searchsubmit" />
            <input
              className="searchbox"
              type="text"
              onChange={debounce((e) => setState({ ...state, searchQuery: e.target.value }), 500)}
              placeholder="Search Users"
            />
          </div>
        </div>
        <div className="scrollable-content" onScroll={handleOnScroll}>
          {bodyView}
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default ReviewerAssignmentModal;
