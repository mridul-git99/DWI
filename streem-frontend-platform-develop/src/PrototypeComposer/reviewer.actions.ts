import { actionSpreader } from '#store';
import { User } from '#store/users/types';
import { Checklist } from './checklist.types';
import { ComposerAction } from './reducer.types';
import { Collaborator, CommonReviewPayload } from './reviewer.types';

// REVIEWER ASSIGNMENT
export const fetchAssignedReviewersForChecklist = (checklistId: Checklist['id']) =>
  actionSpreader(ComposerAction.FETCH_REVIEWERS_FOR_CHECKLIST, { checklistId });

export const fetchAssignedReviewersForChecklistSuccess = (data: Collaborator[]) =>
  actionSpreader(ComposerAction.FETCH_REVIEWERS_FOR_CHECKLIST_SUCCESS, {
    data,
  });

export const submitChecklistForReview = (checklistId: Checklist['id']) =>
  actionSpreader(ComposerAction.SUBMIT_CHECKLIST_FOR_REVIEW, { checklistId });

export const assignReviewerToChecklist = (user: Collaborator) =>
  actionSpreader(ComposerAction.ASSIGN_REVIEWER_TO_CHECKLIST, { user });

export const unAssignReviewerFromChecklist = (user: Collaborator) =>
  actionSpreader(ComposerAction.UNASSIGN_REVIEWER_FROM_CHECKLIST, { user });

export const revertReviewersForChecklist = (users: Collaborator[]) =>
  actionSpreader(ComposerAction.REVERT_REVIEWERS_FOR_CHECKLIST, { users });

export const assignReviewersToChecklist = (payload: {
  checklistId: Checklist['id'];
  assignIds: User['id'][];
  unassignIds: User['id'][];
}) => actionSpreader(ComposerAction.ASSIGN_REVIEWERS_TO_CHECKLIST, payload);

export const startChecklistReview = (checklistId: Checklist['id']) =>
  actionSpreader(ComposerAction.START_CHECKLIST_REVIEW, { checklistId });

export const updateChecklistForReview = ({
  collaborators,
  checklist,
  comments,
}: CommonReviewPayload) =>
  actionSpreader(ComposerAction.UPDATE_FOR_REVIEW_PROCESS, {
    collaborators,
    checklist,
    comments,
  });

export const submitChecklistReview = (checklistId: Checklist['id']) =>
  actionSpreader(ComposerAction.SUBMIT_CHECKLIST_REVIEW, { checklistId });

export const submitChecklistReviewWithCR = (checklistId: Checklist['id'], comments: string) =>
  actionSpreader(ComposerAction.SUBMIT_CHECKLIST_REVIEW_WITH_CR, {
    checklistId,
    comments,
  });

export const sendReviewToCr = (checklistId: Checklist['id']) =>
  actionSpreader(ComposerAction.SEND_REVIEW_TO_CR, { checklistId });

export const initiateSignOff = (
  checklistId: Checklist['id'],
  users: { userId: string; orderTree: number }[],
) => actionSpreader(ComposerAction.INITIATE_SIGNOFF, { checklistId, users });

export const fetchApprovers = (checklistId: Checklist['id']) =>
  actionSpreader(ComposerAction.FETCH_APPROVERS, { checklistId });

export const fetchApproversSuccess = (data: Collaborator[]) =>
  actionSpreader(ComposerAction.FETCH_APPROVERS_SUCCESS, {
    data,
  });

export const signOffPrototype = (payload: {
  checklistId: Checklist['id'];
  password?: string | null;
  code?: string;
  state?: string;
}) => actionSpreader(ComposerAction.SIGN_OFF_PROTOTYPE, payload);

export const releasePrototype = (payload: {
  checklistId: Checklist['id'];
  password?: string;
  code?: string;
  state?: string;
}) => actionSpreader(ComposerAction.RELEASE_PROTOTYPE, payload);

export const recallProcess = (reason: string, checklistId: Checklist['id']) =>
  actionSpreader(ComposerAction.RECALL_PROCESS, { reason, checklistId });
