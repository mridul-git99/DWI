import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import {
  closeAllOverlayAction,
  closeOverlayAction,
  openOverlayAction,
  updatePropsAction,
} from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { RootState } from '#store';
import {
  apiAssignReviewersToChecklist,
  apiGetApproversForChecklist,
  apiGetReviewersForChecklist,
  apiPrototypeRelease,
  apiPrototypeSignOff,
  apiRecallProcess,
  apiSendReviewToCr,
  apiSignOffOrder,
  apiStartChecklistReview,
  apiSubmitChecklistForReview,
  apiSubmitChecklistReview,
  apiSubmitChecklistReviewWithCR,
  apiValidatePassword,
} from '#utils/apiUrls';
import { LoginErrorCodes } from '#utils/constants';
import { ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { encrypt } from '#utils/stringUtils';
import { call, put, select, takeLatest } from 'redux-saga/effects';
import { Checklist, ChecklistStates, Comment } from './checklist.types';
import { ComposerAction } from './reducer.types';
import {
  assignReviewersToChecklist,
  fetchApprovers,
  fetchApproversSuccess,
  fetchAssignedReviewersForChecklist,
  fetchAssignedReviewersForChecklistSuccess,
  initiateSignOff,
  recallProcess,
  releasePrototype,
  sendReviewToCr,
  signOffPrototype,
  startChecklistReview,
  submitChecklistForReview,
  submitChecklistReview,
  submitChecklistReviewWithCR,
  updateChecklistForReview,
} from './reviewer.actions';
import {
  Collaborator,
  CollaboratorState,
  CollaboratorType,
  CommonReviewPayload,
  CommonReviewResponse,
} from './reviewer.types';

const getState = (state: RootState) => state.prototypeComposer.data?.state;
const getCurrentPhase = (state: RootState) => state.prototypeComposer.data?.phase;
const getCurrentReviewers = (state: RootState) =>
  (state.prototypeComposer.data as Checklist)?.collaborators || [];
const getCurrentComments = (state: RootState) =>
  (state.prototypeComposer.data as Checklist)?.comments || [];

function* fetchReviewersForChecklistSaga({
  payload,
}: ReturnType<typeof fetchAssignedReviewersForChecklist>) {
  try {
    const { checklistId } = payload;

    const { data, errors } = yield call(request, 'GET', apiGetReviewersForChecklist(checklistId));

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(fetchAssignedReviewersForChecklistSuccess(data));
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'fetchReviewersForChecklistSaga', error);
  }
}

function* fetchApproversSaga({ payload }: ReturnType<typeof fetchApprovers>) {
  try {
    const { checklistId } = payload;

    const { data, errors } = yield call(request, 'GET', apiGetApproversForChecklist(checklistId));

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      fetchApproversSuccess(
        data.filter(
          (collaborator: Collaborator) => collaborator.type === CollaboratorType.SIGN_OFF_USER,
        ),
      ),
    );
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'fetchApproversSaga', error);
  }
}

function* recallProcessSaga({ payload }: ReturnType<typeof recallProcess>) {
  const { reason, checklistId } = payload;

  try {
    const { data, errors }: ResponseObj<CommonReviewResponse> = yield call(
      request,
      'POST',
      apiRecallProcess(checklistId),
      {
        data: { reason },
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield* onSuccess(data, true);

    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Process Recalled Successfully!',
      }),
    );

    return data;
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'recallProcessSaga', error, true);
  }
}

function* submitChecklistForReviewCall(checklistId: Checklist['id']) {
  try {
    const { data, errors }: ResponseObj<CommonReviewResponse['checklist']> = yield call(
      request,
      'PATCH',
      apiSubmitChecklistForReview(checklistId),
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield* onSuccess({ checklist: data });

    return data;
  } catch (error) {
    throw error;
  }
}

function* submitChecklistForReviewSaga({ payload }: ReturnType<typeof submitChecklistForReview>) {
  const { checklistId } = payload;

  try {
    yield* submitChecklistForReviewCall(checklistId);
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'submitChecklistForReviewSaga', error, true);
  }
}

function* assignReviewersToChecklistSaga({
  payload,
}: ReturnType<typeof assignReviewersToChecklist>) {
  const { checklistId, assignIds: assignedUserIds, unassignIds: unassignedUserIds } = payload;

  try {
    const state = getState(yield select());
    const phase = getCurrentPhase(yield select());
    let data;
    const assignmentsModified = assignedUserIds.length || unassignedUserIds.length;

    const { data: assignmentData, errors }: ResponseObj<CommonReviewResponse['checklist']> =
      yield call(request, 'PATCH', apiAssignReviewersToChecklist(checklistId), {
        data: {
          assignedUserIds,
          unassignedUserIds,
        },
      });

    if (errors) {
      throw getErrorMsg(errors);
    }
    if (assignmentData) {
      if (
        state !== ChecklistStates.SUBMITTED_FOR_REVIEW &&
        state !== ChecklistStates.BEING_REVIEWED
      ) {
        const response = yield* submitChecklistForReviewCall(checklistId);
        if (response) {
          data = response;
        }
      }
    }

    yield* onSuccess({ checklist: data });

    if (state === ChecklistStates.BEING_BUILT && phase === 1) {
      yield put(
        openOverlayAction({
          type: OverlayNames.CHECKLIST_REVIEWER_ASSIGNMENT_SUCCESS,
        }),
      );
    } else {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: assignmentsModified ? 'Assignments Modified' : 'Submitted',
        }),
      );
    }
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'assignReviewersToChecklistSaga', error, true);
  }
}

function* startChecklistReviewSaga({ payload }: ReturnType<typeof startChecklistReview>) {
  const { checklistId } = payload;

  try {
    const { errors, data }: ResponseObj<CommonReviewResponse> = yield call(
      request,
      'PATCH',
      apiStartChecklistReview(checklistId),
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield* onSuccess(data);
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'startChecklistReviewSaga', error, true);
  }
}

function* onSuccess(data: CommonReviewResponse, isRecallProcess?: boolean) {
  try {
    const payloadToSend: CommonReviewPayload = {
      collaborators: [],
      checklist: {},
      comments: [],
    };

    let isLast = true;
    let allDoneOk = true;

    if (data?.collaborators?.length) {
      if (isRecallProcess) {
        payloadToSend.collaborators = data.collaborators;
      } else {
        const currentReviewers = getCurrentReviewers(yield select());
        const addedIndexes: number[] = [];
        const collaborators: Collaborator[] = [];

        data.collaborators.forEach((collab) => {
          let isUpdated = false;
          const { id, phase, phaseType } = collab;
          collaborators.push(
            ...currentReviewers.reduce((accumulator, reviewer, index) => {
              if (reviewer.phase === phase && reviewer.phaseType === phaseType) {
                if (reviewer.id === id) {
                  if (!isUpdated && reviewer.state !== CollaboratorState.SIGNED) {
                    accumulator.push(collab || reviewer);
                    isUpdated = true;
                  } else if (!addedIndexes.includes(index)) {
                    accumulator.push(reviewer);
                    addedIndexes.push(index);
                  }

                  if (collab.state !== CollaboratorState.COMMENTED_OK) {
                    allDoneOk = false;
                  }
                } else {
                  if (!addedIndexes.includes(index)) {
                    accumulator.push(reviewer);
                    addedIndexes.push(index);
                  }

                  if (reviewer.state !== CollaboratorState.COMMENTED_OK) {
                    allDoneOk = false;
                    if (reviewer.state !== CollaboratorState.COMMENTED_CHANGES) {
                      isLast = false;
                    }
                  }
                }
              } else {
                if (!addedIndexes.includes(index)) {
                  accumulator.push(reviewer);
                  addedIndexes.push(index);
                }
              }
              return accumulator;
            }, [] as Collaborator[]),
          );

          if (!isUpdated) collaborators.push(collab);
        });

        payloadToSend.collaborators = collaborators;
      }
    }

    if (data.comment) {
      const currentComments = getCurrentComments(yield select());

      let isUpdated = false;
      const { phase, commentedBy } = data.comment;

      const comments = currentComments.reduce((acc, c) => {
        if (c.phase === phase && c.commentedBy.id === commentedBy.id) {
          acc.push(data.comment as Comment);
          isUpdated = true;
        } else {
          acc.push(c);
        }
        return acc;
      }, [] as Comment[]);

      if (!isUpdated) comments.push(data.comment);

      payloadToSend.comments = comments;
    }

    if (data.checklist) {
      payloadToSend.checklist = data.checklist;
    }

    yield put(updateChecklistForReview(payloadToSend));

    return { payloadToSend, allDoneOk, isLast };
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'onSuccessSaga', error);
  }
}

function* afterSubmitChecklistReview(isLast: boolean, allDoneOk: boolean) {
  try {
    if (isLast) {
      yield put(
        updatePropsAction(OverlayNames.SUBMIT_REVIEW_MODAL, {
          sendToAuthor: true,
          isViewer: false,
          allDoneOk,
        }),
      );
    } else {
      yield put(closeOverlayAction(OverlayNames.SUBMIT_REVIEW_MODAL));
      yield put(
        openOverlayAction({
          type: OverlayNames.CHECKLIST_REVIEWER_SUBMIT_SUCCESS,
        }),
      );
    }
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'afterSubmitChecklistReview', error);
  }
}

function* submitChecklistReviewSaga({ payload }: ReturnType<typeof submitChecklistReview>) {
  const { checklistId } = payload;

  try {
    const { data, errors }: ResponseObj<CommonReviewResponse> = yield call(
      request,
      'PATCH',
      apiSubmitChecklistReview(checklistId),
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    const { isLast, allDoneOk } = yield* onSuccess(data);
    yield* afterSubmitChecklistReview(isLast, allDoneOk);
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'submitChecklistReviewSaga', error, true);
  }
}

function* submitChecklistReviewWithCRSaga({
  payload,
}: ReturnType<typeof submitChecklistReviewWithCR>) {
  const { checklistId, comments } = payload;

  try {
    const { data, errors }: ResponseObj<CommonReviewResponse> = yield call(
      request,
      'PATCH',
      apiSubmitChecklistReviewWithCR(checklistId),
      {
        data: {
          comments,
        },
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    const { isLast, allDoneOk } = yield* onSuccess(data);
    yield* afterSubmitChecklistReview(isLast, allDoneOk);
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'submitChecklistReviewWithCRSaga', error, true);
  }
}

function* sendReviewToCrSaga({ payload }: ReturnType<typeof sendReviewToCr>) {
  const { checklistId } = payload;

  try {
    const { data, errors }: ResponseObj<CommonReviewResponse> = yield call(
      request,
      'PATCH',
      apiSendReviewToCr(checklistId),
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield* onSuccess(data);
    yield put(closeOverlayAction(OverlayNames.SUBMIT_REVIEW_MODAL));
    yield put(
      openOverlayAction({
        type: OverlayNames.CHECKLIST_SENT_TO_AUTHOR_SUCCESS,
        props: {
          heading:
            data?.collaborators?.[0].state === CollaboratorState.REQUESTED_CHANGES
              ? 'Comments Sent to Author'
              : 'Great Job !',
        },
      }),
    );
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'sendReviewToCrSaga', error, true);
  }
}

function* initiateSignOffSaga({ payload }: ReturnType<typeof initiateSignOff>) {
  const { checklistId, users } = payload;

  try {
    const { errors, data }: ResponseObj<CommonReviewResponse> = yield call(
      request,
      'POST',
      apiSignOffOrder(checklistId),
      { data: { users } },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield* onSuccess(data);
    yield put(closeAllOverlayAction());
    yield put(
      openOverlayAction({
        type: OverlayNames.SIGN_OFF_INITIATED_SUCCESS,
      }),
    );
  } catch (error) {
    yield put(closeAllOverlayAction());
    yield* handleCatch('Prototype Composer', 'initiateSignOffSaga', error);
  }
}

function* signOffPrototypeSaga({ payload }: ReturnType<typeof signOffPrototype>) {
  try {
    const { checklistId, password, code, state } = payload;
    const { data: validateData, errors: validateErrors } = yield call(
      request,
      'PATCH',
      apiValidatePassword(),
      { data: { password: password ? encrypt(password) : null, code, state } },
    );

    if (validateData) {
      const { errors, data }: ResponseObj<CommonReviewResponse> = yield call(
        request,
        'PATCH',
        apiPrototypeSignOff(checklistId),
      );

      if (errors) {
        throw getErrorMsg(errors);
      }

      yield* onSuccess(data);
      yield put(closeAllOverlayAction());
      yield put(
        openOverlayAction({
          type: OverlayNames.SIGN_OFF_SUCCESS,
        }),
      );
    } else {
      throw getErrorMsg(validateErrors);
    }
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'signOffPrototypeSaga', error, true);
  }
}

function* releasePrototypeSaga({ payload }: ReturnType<typeof releasePrototype>) {
  try {
    const { checklistId, password, code, state } = payload;
    const { data: validateData, errors: validateErrors } = yield call(
      request,
      'PATCH',
      apiValidatePassword(),
      { data: { password: password ? encrypt(password) : null, code, state } },
    );

    if (validateData) {
      const { errors, data }: ResponseObj<CommonReviewResponse['checklist']> = yield call(
        request,
        'PATCH',
        apiPrototypeRelease(checklistId),
      );

      if (errors) {
        throw getErrorMsg(errors);
      }

      yield* onSuccess({ checklist: data });
      yield put(closeAllOverlayAction());
      yield put(
        openOverlayAction({
          type: OverlayNames.RELEASE_SUCCESS,
        }),
      );
    } else if (validateErrors[0].code === LoginErrorCodes.SSO_INVALID_CREDENTIALS) {
      throw getErrorMsg(validateErrors);
    } else {
      throw validateErrors?.[0]?.message || 'Unable to Release the Prototype';
    }
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'releasePrototypeSaga', error, true);
  }
}

export function* ReviewerSaga() {
  yield takeLatest(ComposerAction.FETCH_REVIEWERS_FOR_CHECKLIST, fetchReviewersForChecklistSaga);
  yield takeLatest(ComposerAction.FETCH_APPROVERS, fetchApproversSaga);
  yield takeLatest(ComposerAction.ASSIGN_REVIEWERS_TO_CHECKLIST, assignReviewersToChecklistSaga);
  yield takeLatest(ComposerAction.START_CHECKLIST_REVIEW, startChecklistReviewSaga);
  yield takeLatest(ComposerAction.SUBMIT_CHECKLIST_FOR_REVIEW, submitChecklistForReviewSaga);
  yield takeLatest(ComposerAction.SUBMIT_CHECKLIST_REVIEW, submitChecklistReviewSaga);
  yield takeLatest(ComposerAction.SUBMIT_CHECKLIST_REVIEW_WITH_CR, submitChecklistReviewWithCRSaga);
  yield takeLatest(ComposerAction.INITIATE_SIGNOFF, initiateSignOffSaga);
  yield takeLatest(ComposerAction.SEND_REVIEW_TO_CR, sendReviewToCrSaga);
  yield takeLatest(ComposerAction.SIGN_OFF_PROTOTYPE, signOffPrototypeSaga);
  yield takeLatest(ComposerAction.RELEASE_PROTOTYPE, releasePrototypeSaga);
  yield takeLatest(ComposerAction.RECALL_PROCESS, recallProcessSaga);
}
