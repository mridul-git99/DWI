import {
  Collaborator,
  CollaboratorState,
  CollaboratorType,
} from '#PrototypeComposer/reviewer.types';
import { Avatar, BaseModal, Textarea } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { apiGetChecklistInfo } from '#utils/apiUrls';
import { request } from '#utils/request';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { navigate } from '@reach/router';
import { isEmpty, noop } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import styled from 'styled-components';
import { Checklist } from '../../../PrototypeComposer/checklist.types';

const Wrapper = styled.div`
  .modal {
    min-width: 850px !important;
    width: 850px !important;
    max-height: 80dvh;

    @media (max-width: 900px) {
      min-width: 780px !important;
      width: 780px !important;
      max-height: 80dvh;
    }
  }

  .modal-body {
    padding: 0 !important;
    max-height: inherit;
    display: grid;
    grid-template-areas: 'header' 'body';
    grid-template-rows: 92px 1fr;
  }

  .close-icon {
    top: 24px !important;
    right: 32px !important;
  }

  .header {
    grid-area: header;
    align-items: flex-start;
    border-bottom: 1px solid #eeeeee;
    display: flex;
    flex-direction: column;
    padding: 24px;
    justify-content: space-between;

    label {
      font-size: 12px;
      font-weight: normal;
      color: #000000;
    }

    span {
      font-size: 20px;
      font-weight: bold;
      color: #000000;
    }
  }

  .body {
    grid-area: body;
    padding: 24px;
    overflow: auto;

    section {
      align-items: flex-start;
      border-bottom: 1px solid #eeeeee;
      display: flex;
      flex-direction: column;
      margin-top: 24px;

      :first-child {
        margin-top: 0;
      }

      :last-child {
        border: none;
      }

      label {
        color: #333333;
        font-size: 14px;
        font-weight: bold;
        margin-bottom: 16px;
      }

      > div {
        display: flex;
        width: 100%;
      }

      .column {
        border-right: 1px solid #eeeeee;
        display: flex;
        flex: 1;
        flex-direction: column;
        margin-bottom: 24px;
        padding-left: 18px;

        :last-child {
          border-right: 0;
        }

        :first-child {
          padding-left: 0;
        }

        &-label {
          color: #999999;
          font-size: 14px;
          font-weight: normal;
          margin-bottom: 16px;
          text-align: left;
        }
      }
    }

    .owner {
      display: flex;
      margin-bottom: 16px;
      align-items: center;

      &-details {
        align-items: flex-start;
        display: flex;
        flex-direction: column;
        margin-left: 16px;

        .owner-id {
          color: #666666;
          font-size: 10px;
          font-weight: 600;
        }

        .owner-name {
          color: #666666;
          font-size: 18px;
        }
      }
    }

    .authors {
      .secondary-authors {
        display: flex;
        align-items: center;
      }

      .creation-date {
        text-align: left;
      }
    }

    .signing {
      .column:first-child {
        flex: 2;
      }
      .signed-as {
        font-size: 14px;
        color: #000000;
        margin: 13px 0;
        text-align: left;
      }

      .state {
        font-size: 12px;
        padding: 2px 4px;
        background-color: #eeeeee;
        color: #999999;
        margin: 13px 0;
        width: max-content;
      }

      .state.success {
        background-color: #e1fec0;
        color: #5aa700;
      }

      .date {
        font-size: 14px;
        color: #000000;
        margin: 13px 0;
        padding: 2px 0px;
        text-align: left;
      }

      .outer-container {
        display: flex;
        flex-direction: column;
        width: 100%;
        justify-content: space-between;

        .signoff-container {
          display: flex;
          flex-direction: row;
          gap: 8px;
        }

        .column-label:first-child,
        .signoff-column:first-child {
          flex: 2;
          border-right: 1px solid #eeeeee;
        }
        .column-label:not(:first-child),
        .signoff-column:not(:first-child) {
          flex: 1;
          border-right: 1px solid #eeeeee;
        }

        .column-label:last-child,
        .signoff-column:last-child {
          flex: 1;
          border-right: none;
        }
      }
    }

    .release {
      .column {
        :first-child {
          flex: 2;
        }
      }
      .state {
        font-size: 12px;
        color: #5aa700;
        padding: 2px 4px;
        background-color: #e1fec0;
        margin: 13px 0;
        width: max-content;
      }

      .date {
        text-align: left;
      }
    }

    .revision {
      .column {
        flex: 4;

        :first-child {
          flex: 1;
        }

        :last-child {
          flex: 8;
        }

        div {
          margin-bottom: 16px;
          text-align: left;
        }

        .version-code {
          :not(:first-of-type) {
            cursor: pointer;

            :hover {
              color: #1d84ff;
            }
          }
        }
      }
    }

    .description {
      textarea {
        :disabled {
          background-color: transparent;
          padding: 0;
        }

        :active,
        :focus {
          border: none;
        }
      }
    }
    .auto-published {
      padding-bottom: 20px;
    }
  }
  .primary {
    cursor: pointer;
    color: #1d84ff;
  }
`;

type Author = Pick<
  Collaborator,
  'modifiedAt' | 'email' | 'employeeId' | 'firstName' | 'lastName' | 'id' | 'state' | 'type'
> & { orderTree: number };

type SignOffUser = Pick<
  Author,
  'id' | 'employeeId' | 'email' | 'firstName' | 'lastName' | 'orderTree' | 'state'
> & { signedAt: number };

type Version = Pick<Checklist, 'id' | 'code' | 'name' | 'versionNumber'> & {
  deprecatedAt: number;
};

type Audit = {
  createdAt: number;
  modifiedAt: number;
  modifiedBy: Pick<Collaborator, 'id' | 'employeeId' | 'firstName' | 'lastName'>;
  createdBy: Pick<Collaborator, 'id' | 'employeeId' | 'firstName' | 'lastName'>;
};

type ChecklistInfo = Pick<
  Checklist,
  'id' | 'name' | 'code' | 'description' | 'state' | 'versionNumber' | 'phase'
> & {
  authors: Author[];
  signOff: SignOffUser[];
  release: {
    releaseAt: number;
    releaseBy: Pick<Collaborator, 'id' | 'firstName' | 'lastName' | 'employeeId'>;
  };
  versions: Version[];
  audit: Audit;
};

type ChecklistInfoModalProps = {
  checklistId: Checklist['id'];
};

const ChecklistInfoModal: FC<CommonOverlayProps<ChecklistInfoModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { checklistId } = {},
}) => {
  const [state, setState] = useState<ChecklistInfo | null>(null);

  useEffect(() => {
    if (checklistId) {
      (async () => {
        const { data, errors } = await request('GET', apiGetChecklistInfo(checklistId));

        if (data) {
          setState(data);
        } else {
          console.error('error :: ', errors);
        }
      })();
    }
  }, []);

  if (!!state) {
    const primaryAuthor = state.authors.filter(
      (author) => author.type === CollaboratorType.PRIMARY_AUTHOR,
    )[0];
    const secondaryAuthors = state.authors.filter(
      (author) => author.type === CollaboratorType.AUTHOR,
    );

    const allUsersSame = (users: SignOffUser[]): boolean => {
      if (users.length === 0) return true;
      const firstUser = users[0].employeeId;
      return users.every((user) => user.employeeId === firstUser);
    };

    const areApproversAndReviewersSame = allUsersSame(state.signOff);

    return (
      <Wrapper>
        <BaseModal
          closeAllModals={closeAllOverlays}
          closeModal={closeOverlay}
          showHeader={false}
          showFooter={false}
        >
          <div className="header">
            <label>Process Name</label>
            <span>{state?.name}</span>
          </div>
          <div className="body">
            {!isEmpty(state.authors) ? (
              <section className="authors">
                <label>Authoring Information</label>
                <div>
                  <div className="column">
                    <label className="column-label">Process Owner</label>
                    <div className="owner">
                      <Avatar size="large" user={primaryAuthor as Author} />
                      <div className="owner-details">
                        <div className="owner-id">{primaryAuthor?.employeeId}</div>
                        <div className="owner-name">{getFullName(primaryAuthor)}</div>
                      </div>
                    </div>
                  </div>

                  <div className="column">
                    <label className="column-label">Secondary Authors</label>
                    <div className="secondary-authors">
                      {secondaryAuthors?.map((author) => (
                        <Avatar user={author} key={author?.employeeId} />
                      ))}
                    </div>
                  </div>

                  <div className="column">
                    <label className="column-label">Creation Date</label>
                    <div className="creation-date">
                      {formatDateTime({
                        value: state?.audit?.createdAt,
                      })}
                    </div>
                  </div>
                </div>
              </section>
            ) : null}

            {!isEmpty(state.signOff) ? (
              <section className="signing">
                <label>Signing Information</label>
                {allUsersSame(state.signOff) && (
                  <div className="auto-published">
                    This process has been auto-published by {getFullName(state.signOff[0])}(ID:{' '}
                    {state.signOff[0].employeeId}) on{' '}
                    {formatDateTime({ value: state.signOff[0].signedAt })}
                  </div>
                )}

                <div className="outer-container">
                  {!areApproversAndReviewersSame && (
                    <div className="body-container">
                      <div className="signoff-container">
                        {['Collaborators', 'Signed As', 'State', 'Date'].map((label) => {
                          return <div className="column-label">{label}</div>;
                        })}
                      </div>

                      {state.signOff.map((user) => {
                        return (
                          <div className="signoff-container">
                            <div className="signoff-column owner" key={user.employeeId}>
                              <Avatar user={user} />
                              <div className="owner-details">
                                <div className="owner-id">{user.employeeId}</div>
                                <div className="owner-name">{getFullName(user)}</div>
                              </div>
                            </div>
                            <div className="signoff-column signed-as" key={user.employeeId}>
                              {(() => {
                                if (user.orderTree === 1) {
                                  return 'Author';
                                } else if (user.orderTree === 2) {
                                  return 'Reviewer';
                                } else if (user.orderTree === 3) {
                                  return 'Approver';
                                }
                              })()}
                            </div>
                            <div className="signoff-column">
                              {user.state === CollaboratorState.NOT_STARTED ? (
                                <div className="state" key={user.employeeId}>
                                  Pending
                                </div>
                              ) : (
                                <div className="state success" key={user.employeeId}>
                                  Complete
                                </div>
                              )}
                            </div>
                            <div className="signoff-column date" key={user.employeeId}>
                              {user.signedAt && formatDateTime({ value: user.signedAt })}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </section>
            ) : null}

            {!isEmpty(state.release) ? (
              <section className="release">
                <label>Released By</label>
                {allUsersSame(state.signOff) && (
                  <div className="auto-published">
                    This process has been auto-published by {getFullName(state.signOff[0])}(ID:{' '}
                    {state.signOff[0].employeeId}) on{' '}
                    {formatDateTime({ value: state.signOff[0].signedAt })}
                  </div>
                )}
                {!areApproversAndReviewersSame && (
                  <div>
                    <div className="column">
                      <label className="column-label">User</label>

                      <div className="owner">
                        <Avatar user={state.release.releaseBy} />
                        <div className="owner-details">
                          <div className="owner-id">{state.release.releaseBy.employeeId}</div>
                          <div className="owner-name">{getFullName(state.release.releaseBy)}</div>
                        </div>
                      </div>
                    </div>

                    <div className="column">
                      <label className="column-label">State</label>
                      <div className="state">Complete</div>
                    </div>

                    <div className="column">
                      <label className="column-label">Date</label>

                      <div className="date">
                        {formatDateTime({
                          value: state.release.releaseAt,
                        })}
                      </div>
                    </div>
                  </div>
                )}
              </section>
            ) : null}

            {!isEmpty(state.versions) ? (
              <section className="revision">
                <label>Revision History</label>

                <div>
                  <div className="column">
                    <label className="column-label">#</label>

                    {state.versions.map((_, index) => (
                      <div key={index}>{state.versions.length - index}</div>
                    ))}
                  </div>

                  <div className="column">
                    <label className="column-label">Process ID</label>

                    {state.versions.map((version, index) => (
                      <div
                        key={index}
                        className="version-code"
                        onClick={() => {
                          if (index > 0) {
                            closeOverlay();
                            navigate(`/checklists/${version.id}`);
                          }
                        }}
                      >
                        {version.code}
                      </div>
                    ))}
                  </div>

                  <div className="column">
                    <label className="column-label">Deprecated on</label>

                    {state.versions.map((version, index) => (
                      <div key={index}>
                        {version.deprecatedAt
                          ? formatDateTime({ value: version.deprecatedAt })
                          : 'Current'}
                      </div>
                    ))}
                  </div>

                  <div className="column">
                    <label className="column-label">Job Logs</label>

                    {state.versions.map((version, index) => (
                      <div
                        key={index}
                        className="primary"
                        onClick={() => {
                          closeOverlay();
                          navigate(`/checklists/${version.id}/logs`);
                        }}
                      >
                        View
                      </div>
                    ))}
                  </div>
                </div>
              </section>
            ) : null}

            {!!state.description ? (
              <section className="description">
                <label>Process Description</label>

                <Textarea
                  defaultValue={state.description}
                  disabled
                  rows={4}
                  onChange={noop}
                  placeholder=""
                />
              </section>
            ) : null}
          </div>
        </BaseModal>
      </Wrapper>
    );
  } else {
    return null;
  }
};

export default ChecklistInfoModal;
