import {
  Checklist,
  ChecklistStates,
  ChecklistStatesColors,
  ChecklistStatesContent,
} from '#PrototypeComposer/checklist.types';
import {
  fetchAssignedReviewersForChecklist,
  sendReviewToCr,
  submitChecklistReview,
  submitChecklistReviewWithCR,
} from '#PrototypeComposer/reviewer.actions';
import {
  Collaborator,
  CollaboratorState,
  CollaboratorType,
} from '#PrototypeComposer/reviewer.types';
import MemoAllOk from '#assets/svg/AllOk';
import MemoMoreComments from '#assets/svg/MoreComments';
import MemoNeedsCommenting from '#assets/svg/NeedsCommenting';
import MemoRemoveComments from '#assets/svg/RemoveComments';
import MemoSentToAuthor from '#assets/svg/SentToAuthor';
import { Avatar, BaseModal, Button, Select } from '#components';
import {
  closeOverlayAction,
  openOverlayAction,
  updatePropsAction,
} from '#components/OverlayContainer/actions';
import { CommonOverlayProps, OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { User } from '#store/users/types';
import { getOrdinal } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import {
  Assignment,
  FiberManualRecord,
  Message,
  Person,
  PersonAdd,
  ThumbUp,
} from '@material-ui/icons';
import { ContentState, EditorState, convertToRaw } from 'draft-js';
import draftToHtml from 'draftjs-to-html';
import htmlToDraft from 'html-to-draftjs';
import { groupBy, orderBy } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { Editor } from 'react-draft-wysiwyg';
import 'react-draft-wysiwyg/dist/react-draft-wysiwyg.css';
import { useDispatch } from 'react-redux';
import { AntSwitch, Wrapper } from './SubmitReview.styles';

enum Options {
  OK = 'OK',
  COMMENTS = 'COMMENTS',
}

type Option = { label: string; value: string | null | number };

type InitialState = {
  selected: Options | null;
  editorState: any;
  contentBlock: any;
  selectedReviewer: null | User['id'];
  selectedCycle: null | number;
  reviewCycleOptions: Option[];
  reviewersOptions: Option[];
};

const toolbarOptions = {
  options: ['inline', 'list'],
  inline: { options: ['bold', 'underline'] },
  list: { options: ['unordered'] },
};

const initialState: InitialState = {
  selected: null,
  editorState: EditorState.createEmpty(),
  contentBlock: htmlToDraft(''),
  selectedReviewer: null,
  selectedCycle: null,
  reviewCycleOptions: [],
  reviewersOptions: [],
};

export const SubmitReviewModal: FC<
  CommonOverlayProps<{
    isViewer: boolean;
    sendToAuthor: boolean;
    isAuthor: boolean;
    isPrimaryAuthor: boolean;
    continueReview: boolean;
    allDoneOk: boolean;
    reviewState: CollaboratorState.COMMENTED_CHANGES | CollaboratorState.COMMENTED_OK | null;
  }>
> = ({ closeAllOverlays, closeOverlay, props }) => {
  const isViewer = props?.isViewer || false;
  const isAuthor = props?.isAuthor || false;
  const isPrimaryAuthor = props?.isPrimaryAuthor || false;
  const continueReview = props?.continueReview || false;
  const reviewState = props?.reviewState || null;
  const sendToAuthor = props?.sendToAuthor || false;
  const allDoneOk = props?.allDoneOk || false;

  const dispatch = useDispatch();
  const { data, userId, collaborators } = useTypedSelector((state) => ({
    userId: state.auth.userId,
    collaborators: state.prototypeComposer.collaborators,
    data: state.prototypeComposer.data as Checklist,
  }));

  const [state, setState] = useState(initialState);

  useEffect(() => {
    dispatch(fetchAssignedReviewersForChecklist(data.id));
  }, []);

  useEffect(() => {
    if (collaborators.length > 0) {
      let editorState = EditorState.createEmpty();
      if (state.contentBlock) {
        const commentExist =
          data?.comments?.filter(
            (c) =>
              c.state !== CollaboratorState.COMMENTED_OK &&
              c.commentedBy.id === userId &&
              c.phase === data?.phase,
          ) ?? [];

        if (commentExist.length) {
          const content = htmlToDraft(commentExist[0].comments);
          const contentState = ContentState.createFromBlockArray(content.contentBlocks);

          editorState = EditorState.createWithContent(contentState);
        }
      }

      const reviewerOptions = collaborators.map((r) => ({
        label: `${r.firstName} ${r.lastName} - ${r.employeeId}`,
        value: r.id,
      }));

      const cycleOptions = [];
      for (let i = 1; i <= (data.phase || 1); i++) {
        cycleOptions.push({
          label: getOrdinal(i),
          value: i,
        });
      }

      setState({
        ...state,
        editorState,
        reviewersOptions: [{ label: 'All', value: '' }, ...reviewerOptions],
        reviewCycleOptions: [{ label: 'All', value: '' }, ...cycleOptions],
      });
    }
  }, [collaborators]);

  const handleOnAllOK = () => {
    if (data && data?.id) dispatch(submitChecklistReview(data?.id));
  };

  const handleSendToAuthor = () => {
    if (data && data?.id) dispatch(sendReviewToCr(data?.id));
  };

  const handleOnCompleteWithCR = () => {
    const comments = draftToHtml(convertToRaw(state.editorState.getCurrentContent()));
    if (data && data?.id) dispatch(submitChecklistReviewWithCR(data?.id, comments));
  };

  const groupedComments = groupBy(data?.comments, 'phase');
  const comments: any = [];
  Object.keys(groupedComments)
    .sort()
    .reverse()
    .forEach((item) => {
      let shouldInclude = true;
      let shouldIncludeUser = true;
      if (state.selectedCycle) {
        shouldInclude = state.selectedCycle === Number(item);
      }
      if (state.selectedReviewer) {
        shouldIncludeUser = false;
      }
      if (shouldInclude) {
        groupedComments[item] = orderBy(groupedComments[item], ['commentedAt'], ['desc']);
        comments.push(
          <div className="reviews-group" key={`reviewerGroup_${item}`}>
            <span>{`${getOrdinal(Number(item))} Review`}</span>
            <div className="comments-group">
              {groupedComments[item].map((comment) => {
                if (!shouldIncludeUser && state.selectedReviewer !== comment.commentedBy.id)
                  return null;
                return (
                  <div className="comment-section" key={`comment_${comment.id}`}>
                    <div className="user-detail">
                      <div>
                        <Avatar user={comment.commentedBy} />
                        <h4>{`${comment.commentedBy.firstName} ${comment.commentedBy.lastName},`}</h4>
                        <h5>{`ID : ${comment.commentedBy.employeeId}`}</h5>
                      </div>
                      <div>
                        <span>{formatDateTime({ value: comment.commentedAt })}</span>
                      </div>
                    </div>
                    {comment.state === CollaboratorState.COMMENTED_OK ||
                    comment.state === CollaboratorState.REQUESTED_NO_CHANGES ? (
                      <div
                        className="comment"
                        style={{
                          backgroundColor: '#e1fec0',
                          border: 'solid 1px #5aa700',
                          color: '#427a00',
                          display: 'flex',
                          alignItems: 'center',
                          padding: '16px',
                        }}
                      >
                        All OK, No Comments
                        <ThumbUp
                          style={{
                            color: '#427a00',
                            marginLeft: '6px',
                            fontSize: '20px',
                          }}
                        />
                      </div>
                    ) : (
                      <div
                        className="comment"
                        dangerouslySetInnerHTML={{ __html: comment.comments }}
                      />
                    )}
                  </div>
                );
              })}
            </div>
          </div>,
        );
      }
    });

  const handleAuthorMouseOver = (
    event: React.MouseEvent<HTMLDivElement, MouseEvent>,
    users: Collaborator[],
  ) => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.AUTHORS_DETAIL,
        popOverAnchorEl: event.currentTarget,
        props: {
          users: users.filter(
            (u) => u.type === CollaboratorType.AUTHOR || u.type === CollaboratorType.PRIMARY_AUTHOR,
          ),
        },
      }),
    );
  };

  const toggleSendToAuthor = () => {
    dispatch(
      updatePropsAction(OverlayNames.SUBMIT_REVIEW_MODAL, {
        sendToAuthor: !sendToAuthor,
        isViewer: true,
      }),
    );
  };

  const handleReviewersMouseOver = (
    event: React.MouseEvent<HTMLDivElement, MouseEvent>,
    users: Collaborator[],
  ) => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.REVIEWERS_DETAIL,
        popOverAnchorEl: event.currentTarget,
        props: {
          users,
        },
      }),
    );
  };

  const handleAssignReviewers = (event: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.CHECKLIST_REVIEWER_ASSIGNMENT_POPOVER,
        popOverAnchorEl: event.currentTarget,
        props: {
          checklistId: data?.id,
        },
      }),
    );
  };

  const getTitle = () => {
    if (sendToAuthor && !isViewer) return 'Send to Author';
    if (state.selected === Options.OK) return 'Confirm your choice';
    if (continueReview && reviewState === CollaboratorState.COMMENTED_OK) return 'Add Comments';
    if (continueReview && reviewState === CollaboratorState.COMMENTED_CHANGES)
      return 'How do you want to Continue?';
    if (!sendToAuthor && !isViewer && state.selected === null) return 'How is the Prototype ?';

    return '';
  };

  const getShowHeader = () => {
    if (!sendToAuthor && (isViewer || state.selected === Options.COMMENTS)) return false;

    return true;
  };

  return (
    <Wrapper comments={!sendToAuthor && (isViewer || state.selected === Options.COMMENTS)}>
      <BaseModal
        showFooter={false}
        showHeader={getShowHeader()}
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title={getTitle()}
      >
        {continueReview && reviewState && state.selected === null && (
          <>
            {reviewState === CollaboratorState.COMMENTED_OK ? (
              <div
                style={{
                  padding: '0px 0px 88px',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                }}
              >
                <div className="box-wrapper" style={{ padding: '0px 208px' }}>
                  <div
                    className="box"
                    onClick={() => setState({ ...state, selected: Options.COMMENTS })}
                  >
                    <div className="icon-wrapper">
                      <MemoNeedsCommenting />
                    </div>
                    <h3>Needs Commenting</h3>
                    <span>Some Changes Are Needed</span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="box-wrapper" style={{ padding: '0px 48px 100px' }}>
                <div className="box" onClick={() => setState({ ...state, selected: Options.OK })}>
                  <div className="icon-wrapper">
                    <MemoRemoveComments />
                  </div>
                  <h3>Remove Comments</h3>
                  <span>Cancel your existing Comment</span>
                </div>
                <div
                  className="box"
                  onClick={() => setState({ ...state, selected: Options.COMMENTS })}
                >
                  <div className="icon-wrapper">
                    <MemoMoreComments />
                  </div>
                  <h3>More Comments</h3>
                  <span>Continue from where you left</span>
                </div>
              </div>
            )}
          </>
        )}
        {sendToAuthor && (
          <div
            style={{
              padding: '0px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
            }}
          >
            <div className="box-wrapper" style={{ padding: '0px 208px' }}>
              {allDoneOk ? (
                <div className="box">
                  <div className="icon-wrapper">
                    <MemoAllOk />
                  </div>
                  <h3>All Ok</h3>
                  <span>No Changes required</span>
                </div>
              ) : (
                <div className="box">
                  <div className="icon-wrapper">
                    <MemoSentToAuthor />
                  </div>
                  <h3>Send to Author</h3>
                  <span>Requesting for some Modification</span>
                </div>
              )}
            </div>
            <Button className="submit" onClick={handleSendToAuthor}>
              Confirm
            </Button>
            {!allDoneOk && (
              <Button variant="textOnly" onClick={toggleSendToAuthor}>
                View Requested Changes
              </Button>
            )}
          </div>
        )}

        {!sendToAuthor && !continueReview && !isViewer && state.selected === null && (
          <>
            <div className="box-wrapper" style={{ padding: '0px 48px 100px' }}>
              <div className="box" onClick={() => setState({ ...state, selected: Options.OK })}>
                <div className="icon-wrapper">
                  <MemoAllOk />
                </div>
                <h3>All OK</h3>
                <span>No Comments Needed</span>
              </div>
              <div
                className="box"
                onClick={() => setState({ ...state, selected: Options.COMMENTS })}
              >
                <div className="icon-wrapper">
                  <MemoNeedsCommenting />
                </div>
                <h3>Needs Commenting</h3>
                <span>Some Changes Are Needed</span>
              </div>
            </div>
          </>
        )}

        {!sendToAuthor && continueReview && !isViewer && state.selected === Options.OK && (
          <>
            <div className="box-wrapper" style={{ padding: '0px 208px' }}>
              <div className="box">
                <div className="icon-wrapper">
                  <MemoRemoveComments />
                </div>
                <h3>Remove Comments</h3>
                <span>No Changes are Necessary</span>
              </div>
            </div>
            <Button className="submit" onClick={handleOnAllOK}>
              Confirm
            </Button>
            <Button variant="textOnly" onClick={() => setState({ ...state, selected: null })}>
              Go Back
            </Button>
          </>
        )}

        {!sendToAuthor && !continueReview && !isViewer && state.selected === Options.OK && (
          <>
            <div className="box-wrapper" style={{ padding: '0px 208px' }}>
              <div className="box">
                <div className="icon-wrapper">
                  <MemoAllOk />
                </div>
                <h3>All OK</h3>
                <span>No Comments Needed</span>
              </div>
            </div>
            <Button className="submit" onClick={handleOnAllOK}>
              Confirm
            </Button>
            <Button variant="textOnly" onClick={() => setState({ ...state, selected: null })}>
              Go Back
            </Button>
          </>
        )}

        {!sendToAuthor && (isViewer || state.selected === Options.COMMENTS) ? (
          <>
            <div className="header">
              <div className="header-left">
                <h5>Process Name</h5>
                <h4>{data?.name}</h4>
                <h6>
                  <FiberManualRecord
                    className="icon"
                    style={{ color: ChecklistStatesColors[data?.state] }}
                  />
                  {ChecklistStatesContent[data?.state]}
                </h6>
              </div>
              <div className="header-right">
                {!isAuthor && (
                  <>
                    <AntSwitch
                      checked={!state.selectedReviewer}
                      onChange={() => {
                        if (!state.selectedReviewer) {
                          setState({ ...state, selectedReviewer: userId });
                        } else {
                          setState({ ...state, selectedReviewer: null });
                        }
                      }}
                      name="showAllComments"
                    />
                    <h6>All Comments</h6>
                  </>
                )}
                <div
                  className="icon-wrapper"
                  aria-haspopup="true"
                  onMouseEnter={(e) => handleAuthorMouseOver(e, data?.collaborators)}
                  onMouseLeave={() => dispatch(closeOverlayAction(OverlayNames.AUTHORS_DETAIL))}
                >
                  <Assignment
                    style={{
                      fontSize: '8px',
                      position: 'absolute',
                      top: '8px',
                      left: '8px',
                    }}
                  />
                  <Person style={{ fontSize: '18px' }} />
                </div>
                <div
                  className="icon-wrapper"
                  aria-haspopup="true"
                  onMouseEnter={(e) => handleReviewersMouseOver(e, collaborators)}
                  onMouseLeave={() => dispatch(closeOverlayAction(OverlayNames.REVIEWERS_DETAIL))}
                >
                  <Message
                    style={{
                      fontSize: '8px',
                      position: 'absolute',
                      top: '8px',
                      left: '8px',
                    }}
                  />
                  <Person style={{ fontSize: '18px' }} />
                </div>
                {isPrimaryAuthor &&
                  (data.state === ChecklistStates.SUBMITTED_FOR_REVIEW ||
                    data.state === ChecklistStates.BEING_REVIEWED) && (
                    <div
                      className="icon-wrapper"
                      style={{
                        borderRadius: '4px',
                        borderColor: '#1d84ff',
                      }}
                      aria-haspopup="true"
                      onClick={(e) => handleAssignReviewers(e)}
                    >
                      <PersonAdd style={{ fontSize: '18px' }} />
                    </div>
                  )}
              </div>
            </div>
            <div className="body">
              {!isViewer && (
                <div className="box">
                  <Editor
                    editorState={state.editorState}
                    wrapperClassName="editor-wrapper"
                    editorClassName="editor"
                    toolbarClassName="editor-toolbar"
                    toolbar={toolbarOptions}
                    onEditorStateChange={(editorState: any) => setState({ ...state, editorState })}
                  />
                  <div className="actions-container">
                    <Button
                      color="red"
                      variant="secondary"
                      onClick={() => setState({ ...state, selected: null })}
                    >
                      Cancel
                    </Button>
                    <Button
                      onClick={handleOnCompleteWithCR}
                      disabled={!state.editorState.getCurrentContent().hasText()}
                    >
                      Submit
                    </Button>
                  </div>
                </div>
              )}
              <div className="box">
                <div className="filter-input">
                  <div>
                    <h6>Filter Comments by Reviewers</h6>
                    <Select
                      options={state.reviewersOptions}
                      onChange={(option) => {
                        setState({ ...state, selectedReviewer: option.value });
                      }}
                      placeholder="All"
                    />
                  </div>
                  {state.reviewCycleOptions.length > 2 && (
                    <div>
                      <h6>Filter Comments by Review Cycle</h6>
                      <Select
                        options={state.reviewCycleOptions}
                        onChange={(option) => {
                          setState({ ...state, selectedCycle: option.value });
                        }}
                        placeholder="All"
                      />
                    </div>
                  )}
                </div>
                <div className="comments-section">
                  {comments.length > 0 ? (
                    comments
                  ) : !isViewer ? (
                    <div className="no-comments">No Comments by other Reviewers</div>
                  ) : (
                    <div />
                  )}
                </div>
              </div>
            </div>
          </>
        ) : null}
      </BaseModal>
    </Wrapper>
  );
};
