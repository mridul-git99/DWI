import { User } from '#store/users/types';
import { Checklist, Comment } from './checklist.types';

export enum CollaboratorState {
  ILLEGAL = 'ILLEGAL',
  NOT_STARTED = 'NOT_STARTED',
  BEING_REVIEWED = 'BEING_REVIEWED',
  COMMENTED_OK = 'COMMENTED_OK',
  COMMENTED_CHANGES = 'COMMENTED_CHANGES',
  REQUESTED_CHANGES = 'REQUESTED_CHANGES',
  REQUESTED_NO_CHANGES = 'REQUESTED_NO_CHANGES',
  SIGNED = 'SIGNED',
}

export enum CollaboratorStateContent {
  ILLEGAL = 'ILLEGAL',
  NOT_STARTED = 'Not Started',
  BEING_REVIEWED = 'In Progress',
  COMMENTED_OK = 'All OK',
  COMMENTED_CHANGES = 'Comments',
  REQUESTED_CHANGES = 'Done, Comments',
  REQUESTED_NO_CHANGES = 'Done, All OK',
  SIGNED = 'Signed',
}

export enum CollaboratorStateColors {
  ILLEGAL = '#FF6B6B',
  NOT_STARTED = '#333333',
  BEING_REVIEWED = '#1D84FF',
  COMMENTED_OK = '#5AA700',
  COMMENTED_CHANGES = '#C29004',
  REQUESTED_CHANGES = '#C29004',
  REQUESTED_NO_CHANGES = '#5AA700',
  SIGNED = '#5AA700',
}

export enum CollaboratorType {
  PRIMARY_AUTHOR = 'PRIMARY_AUTHOR',
  AUTHOR = 'AUTHOR',
  REVIEWER = 'REVIEWER',
  SIGN_OFF_USER = 'SIGN_OFF_USER',
}

export enum PhaseType {
  BUILD = 'BUILD',
  REVIEW = 'REVIEW',
  SIGN_OFF = 'SIGN_OFF',
}

export type Collaborator = Pick<User, 'id' | 'employeeId' | 'firstName' | 'lastName' | 'email'> &
  Pick<Comment, 'id' | 'comments' | 'commentedAt' | 'modifiedAt'> & {
    state: CollaboratorState;
    phase: number;
    phaseType: PhaseType;
    type: CollaboratorType;
  };

export type CommonReviewPayload = {
  collaborators: Collaborator[] | [];
  checklist: Pick<Checklist, 'id' | 'state' | 'phase'> | Record<string, unknown>;
  comments: Comment[] | [];
};

export type CommonReviewResponse = {
  collaborators?: Collaborator[];
  checklist?: Pick<Checklist, 'id' | 'state' | 'phase'>;
  comment?: Comment;
};
