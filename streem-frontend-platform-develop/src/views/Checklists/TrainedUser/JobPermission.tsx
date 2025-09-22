import { Checkbox } from '#components';
import React, { useState } from 'react';
import styled from 'styled-components';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';
import { AccordionDetails } from '@material-ui/core';
import { processRolesDetails } from '../Roles/roleDetails';

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  padding: 16px;
  margin: 16px 0;
  border: 1px solid #f1f1f1;

  .permission-group {
    width: 100%;

    .permission-header {
      color: #333;
      font-size: 20px;
      font-weight: 600;
      margin-bottom: 8px;
      border-bottom: 1px solid #e0e0e0;
      width: 100%;
      padding-bottom: 8px;
      display: flex;
    }

    .list-item {
      align-items: center;
      display: flex;
      padding: 8px 0 0 0;
      height: 80px;

      .permission-group-checkbox {
        flex: 1;

        .permission-group-text {
          margin-left: 28px;
        }
      }

      .checkbox-input {
        margin-top: -19px;

        label.container {
          color: #333333;
          font-weight: bold;
        }
      }
    }
  }
  .arrow-action-container {
    display: flex;

    .actions {
      color: #1d84ff;
      font-size: 16px;
      white-space: nowrap;
      padding: 0px 20px 0px 0px;
      display: flex;
      align-items: center;
    }
  }

  .permission-container {
    display: grid;
    grid-template-columns: auto auto;
    column-gap: 8px;
    grid-row-gap: 16px;

    .permission-box {
      display: flex;
      flex-direction: column;

      .permission-header-tag {
        color: #161616;
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 8px;
      }
    }
  }
`;

const permissionList = [
  { id: 1, value: 'Job Executor' },
  { id: 2, value: 'Job Reviewer' },
  { id: 3, value: 'Job Manager' },
  { id: 4, value: 'Job Issuer' },
];

export const JobPermissionContainer = ({
  permissionField,
  permissionAppend,
  permissionRemove,
}: any) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const getArrowIcon = (isOpen: boolean) => (
    <div className="arrow-action-container">
      <div className="actions">See Permissions</div>
      {isOpen ? (
        <ArrowDropUpIcon style={{ color: '#1d84ff' }} />
      ) : (
        <ArrowDropDownIcon style={{ color: '#1d84ff' }} />
      )}
    </div>
  );

  const handleChoose = (permission: any) => {
    const selectedIndex = permissionField.findIndex((el) => el.value === permission.value);
    if (selectedIndex > -1) {
      permissionRemove(selectedIndex);
    } else {
      permissionAppend(permission);
    }
  };

  return (
    <Wrapper>
      <div className="permission-group">
        <span className="permission-header">Permissions: </span>
        <div className="list-item">
          {permissionList.map((permission, index) => (
            <div key={permission.id} className="permission-group-checkbox">
              <Checkbox
                onClick={() => handleChoose(permission)}
                disabled={false}
                checked={permissionField.filter((el) => el.value === permission.value)?.length}
              />
              <span className="permission-group-text">{permission.value}</span>
            </div>
          ))}
        </div>
        <div onClick={() => setIsExpanded(!isExpanded)}>{getArrowIcon(isExpanded)}</div>
        {!!permissionField?.length && isExpanded && (
          <AccordionDetails>
            <div className="permission-container">
              {permissionField.map((permission: any) => (
                <div className="permission-box">
                  <div className="permission-header-tag">{permission.value}</div>
                  {Object.entries(processRolesDetails[permission.id]?.permissions).map(
                    ([permissionCategory, permissionArr]) => (
                      <div>
                        <div
                          style={{
                            fontWeight: 'bold',
                            fontSize: '14px',
                            color: '#161616',
                          }}
                        >
                          {permissionCategory}
                        </div>
                        <ul className="permission-list">
                          {permissionArr?.map((permission, i) => (
                            <li
                              className="permission-list-item"
                              style={{ color: '#525252', fontSize: '14px' }}
                            >
                              {permission}
                            </li>
                          ))}
                        </ul>
                      </div>
                    ),
                  )}
                </div>
              ))}
            </div>
          </AccordionDetails>
        )}
      </div>
    </Wrapper>
  );
};
