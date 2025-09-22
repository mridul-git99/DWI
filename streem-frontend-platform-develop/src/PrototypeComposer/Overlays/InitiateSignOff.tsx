import { Avatar, BaseModal, Checkbox } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { Checklist } from '#PrototypeComposer/checklist.types';
import { initiateSignOff } from '#PrototypeComposer/reviewer.actions';
import { CollaboratorType } from '#PrototypeComposer/reviewer.types';
import { useTypedSelector } from '#store';
import { Info } from '@material-ui/icons';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    min-width: 400px !important;

    .close-icon {
      top: 22px !important;
      right: 16px !important;
      font-size: 24px !important;
    }

    h2 {
      color: #000 !important;
      font-weight: bold !important;
    }

    .modal-header {
      padding: 24px 32px !important;
      border-bottom: 1px solid #eeeeee !important;
    }

    .modal-footer {
      padding: 24px 50px !important;
      flex-direction: row-reverse !important;
      border-top: 1px solid #eeeeee !important;
    }

    .modal-body {
      padding: 32px !important;
      justify-content: center;
      align-items: center;
      display: flex;
      flex-direction: column;

      > table {
        tbody {
          tr {
            :nth-child(-n + 2) {
              td {
                border: none;
              }
            }

            :last-child {
              td {
                border-bottom: none;
              }
            }

            td {
              padding: 12px 0px;
              min-width: 120px;
              border-left: 1px solid #eeeeee;
              border-bottom: 1px solid #eeeeee;

              :first-child {
                min-width: 336px;
                border-left: none;
              }

              .heading {
                font-weight: bold;
                font-size: 14px;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;

                &.light {
                  font-weight: normal;
                }

                .ellipse {
                  width: 24px;
                  height: 24px;
                  border-radius: 50%;
                  border: solid 1px #fff;
                  background-color: #999999;
                  font-size: 14px;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  color: #fff;
                  margin-bottom: 8px;
                }
              }

              .item {
                display: flex;
                align-items: center;

                .middle {
                  display: flex;
                  flex: 1;
                  flex-direction: column;
                  justify-content: center;
                  align-items: flex-start;
                  padding: 0 15px;
                  gap: 6px;

                  .userId {
                    font-size: 8px;
                    font-weight: 600;
                    color: #666666;
                    height: 11px;
                  }
                  .userName {
                    font-size: 20px;
                    color: #666666;
                    text-transform: capitalize;
                  }
                }
              }

              .checkmark-wrapper {
                display: flex;
                align-items: center;
                justify-content: center;

                .container {
                  padding-left: 9px;
                }
              }
            }
          }
        }
      }

      .before-header {
        display: flex;
        align-items: center;
        justify-content: center;
        margin: -25px 0px 4px 0px;

        .alert {
          padding: 4px 80px;
          border-radius: 4px;
          border: solid 1px #ff6b6b;
          background-color: rgba(255, 107, 107, 0.16);
          min-width: 600px;
          display: flex;
          align-items: center;
          justify-content: center;

          span {
            color: #333333;
            font-size: 12px;
            line-height: 16px;
          }

          svg {
            color: #cc5656;
            font-size: 16px;
            line-height: 16px;
            margin-right: 8px;
          }
        }
      }
    }
  }
`;

type ParsedUser = {
  email: string;
  firstName: string;
  lastName: string;
  employeeId: string;
  id: string;
  type: CollaboratorType;
  phase: number;
  checkedAuthor: boolean;
  checkedReviewer: boolean;
  checkedApprover: boolean;
};

/*
  The signatories will be sorted in this order
*/
const collaboratorTypeOrder = {
  [CollaboratorType.PRIMARY_AUTHOR]: 1,
  [CollaboratorType.AUTHOR]: 2,
  [CollaboratorType.REVIEWER]: 3,
} as const;

const getUsersListByReviewCycle = (users: any[] = []) => {
  const maxPhase = Math.max(...users.map((item) => item.phase));

  const { newUsers, selectedUsers } = users.reduce(
    (acc, collaborator) => {
      const isAuthorOrPrimaryAuthor =
        collaborator.type === CollaboratorType.AUTHOR ||
        collaborator.type === CollaboratorType.PRIMARY_AUTHOR;

      if (isAuthorOrPrimaryAuthor || collaborator.phase === maxPhase) {
        acc.newUsers.push({
          id: collaborator.id,
          email: collaborator.email,
          firstName: collaborator.firstName,
          lastName: collaborator.lastName,
          employeeId: collaborator.employeeId,
          type: collaborator.type,
          phase: collaborator.phase,
          checkedAuthor: isAuthorOrPrimaryAuthor,
          checkedReviewer: false,
          checkedApprover: false,
        });
        if (isAuthorOrPrimaryAuthor)
          acc.selectedUsers.push({ userId: collaborator.id, orderTree: 1 });
      }
      return acc;
    },
    { newUsers: [], selectedUsers: [] } as {
      newUsers: ParsedUser[];
      selectedUsers: { userId: string; orderTree: number }[];
    },
  );
  return { newUsers, selectedUsers };
};

const InitiateSignOffModal: FC<CommonOverlayProps<any>> = ({ closeAllOverlays, closeOverlay }) => {
  const dispatch = useDispatch();
  const [users, setUsers] = useState<ParsedUser[]>([]);
  const [error, setError] = useState(
    'You need to select at least one Reviewer and one Approver to Initiate the Sign Off process',
  );
  const [selection, setSelection] = useState<{ userId: string; orderTree: number }[]>([]);

  const { data } = useTypedSelector((state) => ({
    data: state.prototypeComposer?.data as unknown as Checklist,
  }));

  const sortSignatories = (users: ParsedUser[]) => {
    return users.sort((a, b) => {
      if (collaboratorTypeOrder[a.type] !== collaboratorTypeOrder[b.type]) {
        return collaboratorTypeOrder[a.type] - collaboratorTypeOrder[b.type];
      }
      const aFullName = a.firstName + (a.lastName ?? '');
      const bFullName = b.firstName + (b.lastName ?? '');
      return aFullName > bFullName ? 1 : aFullName < bFullName ? -1 : 0;
    });
  };

  useEffect(() => {
    const { newUsers, selectedUsers } = getUsersListByReviewCycle(data?.collaborators);
    setUsers(sortSignatories(newUsers));
    setSelection(selectedUsers);
  }, []);

  const onCheckChange = (id: string, orderTree: number) => {
    let checking = true;

    const newUsers = users.map((u) => {
      if (u.id !== id) {
        return u;
      } else {
        switch (orderTree) {
          case 2:
            if (u.checkedReviewer) {
              checking = false;
            }
            return { ...u, checkedReviewer: !u.checkedReviewer };
          case 3:
            if (u.checkedApprover) {
              checking = false;
            }
            return { ...u, checkedApprover: !u.checkedApprover };
          default:
            return u;
        }
      }
    });

    //TODO: sorting done on every check change is this needed ?
    setUsers(sortSignatories(newUsers));

    let newSelection: { userId: string; orderTree: number }[] = [];
    if (checking) {
      newSelection = [...selection, { userId: id, orderTree }];
    } else {
      selection.forEach((s) => {
        if (s.userId !== id) {
          newSelection.push(s);
        } else {
          if (s.orderTree !== orderTree) {
            newSelection.push(s);
          }
        }
      });
    }
    setSelection(newSelection);

    let consistReviewer = false;
    let consistApprover = false;

    /*
      all collaborator must be a part of signing process
      fetch unique user ids from selection to verify the same
    */
    const uniqueSelections = new Set<string>();

    newSelection.forEach((c) => {
      if (c.orderTree === 2) consistReviewer = true;
      if (c.orderTree === 3) consistApprover = true;
      uniqueSelections.add(c.userId);
    });

    if (uniqueSelections.size !== newUsers.length) {
      setError(
        'You need to select at least one role for each collaborator to Initiate the Sign Off process',
      );
    } else if (consistReviewer === false && consistApprover === false) {
      setError(
        'You need to select at least one Reviewer and one Approver to Initiate the Sign Off process',
      );
    } else if (consistReviewer === false) {
      setError('You need to select at least one Reviewer to Initiate the Sign Off process');
    } else if (consistApprover === false) {
      setError('You need to select at least one Approver to Initiate the Sign Off process');
    } else {
      setError('');
    }
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title="Initiate Sequential Sign Off"
        primaryText="Initiate"
        secondaryText="Cancel"
        onSecondary={closeOverlay}
        onPrimary={() => dispatch(initiateSignOff(data.id, selection))}
        disabledPrimary={!!error}
      >
        {error && (
          <div className="before-header">
            <div className="alert">
              <Info />
              <span>{error}</span>
            </div>
          </div>
        )}
        <table cellSpacing={0} cellPadding={0}>
          <tbody>
            <tr>
              <td />
              <td colSpan={3}>
                <div className="heading light">Signing As</div>
              </td>
            </tr>
            <tr>
              <td />
              <td>
                <div className="heading">
                  <div className="ellipse">1</div>
                  Author
                </div>
              </td>
              <td>
                <div className="heading">
                  <div className="ellipse">2</div>Reviewer
                </div>
              </td>
              <td>
                <div className="heading">
                  <div className="ellipse">3</div>Approver
                </div>
              </td>
            </tr>
            {users.map((u) => (
              <tr key={u.employeeId}>
                <td>
                  <div className="item">
                    <Avatar size="large" user={u} />
                    <div className="middle">
                      <span className="userId">{u.employeeId}</span>
                      <span className="userName">
                        {u.firstName} {u.lastName}
                      </span>
                    </div>
                  </div>
                </td>
                <td>
                  <div className="checkmark-wrapper">
                    {(u.type === CollaboratorType.AUTHOR ||
                      u.type === CollaboratorType.PRIMARY_AUTHOR) && <Checkbox checked disabled />}
                  </div>
                </td>
                <td>
                  <div className="checkmark-wrapper">
                    {u.type === CollaboratorType.REVIEWER && (
                      <Checkbox
                        checked={u.checkedReviewer}
                        onClick={() => onCheckChange(u.id, 2)}
                        disabled={u.checkedApprover}
                      />
                    )}
                  </div>
                </td>
                <td>
                  <div className="checkmark-wrapper">
                    {u.type === CollaboratorType.REVIEWER && (
                      <Checkbox
                        checked={u.checkedApprover}
                        onClick={() => onCheckChange(u.id, 3)}
                        disabled={u.checkedReviewer}
                      />
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </BaseModal>
    </Wrapper>
  );
};

export default InitiateSignOffModal;
