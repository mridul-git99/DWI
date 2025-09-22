import { StyledRadio } from '#components';
import { RoleIdByName } from '#services/uiPermissions';
import { rolesDetails } from '#views/UserAccess/ManageUser/rolesDetails';
import { FormControlLabel, RadioGroup } from '@material-ui/core';
import Accordion from '@material-ui/core/Accordion';
import AccordionDetails from '@material-ui/core/AccordionDetails';
import AccordionSummary from '@material-ui/core/AccordionSummary';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';
import React, { FC, useState } from 'react';
import styled from 'styled-components';

export interface RoleProps {
  id: string;
  selected?: RoleIdByName;
  label: string;
  placeHolder: string;
  disabled?: boolean;
  error?: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

const Wrapper = styled.div.attrs({})`
  flex: 1;

  .role-wrapper {
    position: relative;
    display: flex;
    flex-direction: column;
    flex: 1;
    background-color: #fff;
    opacity: 1;

    > label {
      font-size: 8px;
      color: #999999;
      font-weight: 600;
      padding: 4px;
    }

    .MuiAccordion-root {
      box-shadow: unset;

      .MuiAccordionSummary-root {
        padding: 16px 0px;
        min-height: unset;
      }

      .MuiIconButton-root {
        padding: 2px 12px;
      }

      .MuiAccordionSummary-content {
        margin: 0px;
      }

      .MuiAccordionDetails-root {
        padding: 0px;
        flex-direction: column;

        .permission-group {
          display: flex;
          flex: 1;
          flex-direction: column-reverse;
        }

        .permission-group-text {
          color: #333;
          font-size: 14px;
          font-weight: bold;
          padding: 10px 0px;
        }

        .permission {
          display: flex;
          flex-wrap: wrap;
        }

        .permission-text {
          font-size: 12px;
          font-weight: 300;
          color: #666;
          width: 50%;
          padding: 4px;
        }

        .permission-details {
          .permission-title {
            font-size: 12px;
            line-height: 0;
            font-weight: 300;
            color: #666;
            flex: 0.3;
            align-items: center;
            display: flex;
          }

          display: flex;
          flex: 1;
          padding: 4px 0px;
        }

        .permission-text::before {
          content: '	• 	';
          margin: 0px 15px 0px 6px;
          color: #666666;
        }
      }
    }

    .bordered {
      border-bottom: 1px dashed #dadada;
    }

    .role-radio-group-wrapper {
      .MuiFormGroup-root {
        display: grid;
        row-gap: 8px;
        grid-template-columns: auto auto auto;
      }
    }

    .input {
      flex: 1;
      font-size: 16px;
      padding: 4px 8px 4px 0px;
      color: #666666;
      border: none;
      outline: none;
      background-color: transparent;
      ::-webkit-input-placeholder {
        color: #999999;
      }
      :-moz-placeholder {
        color: #999999;
      }
      ::-moz-placeholder {
        color: #999999;
      }
      :-ms-input-placeholder {
        color: #999999;
      }
    }

    .input.disabled {
      color: #333;
    }

    .actions {
      color: #1d84ff;
      font-size: 16px;
      white-space: nowrap;
      padding: 0px 20px 0px 0px;
      display: flex;
      align-items: center;
    }
  }

  .role-wrapper.active {
    border: none;
    border-bottom: 2px solid #1d84ff;
    border-bottom-left-radius: 0px;
    border-bottom-right-radius: 0px;
    opacity: 1;

    > label {
      color: #1d84ff;
    }
  }

  .role-wrapper.error {
    border: none;
    border-bottom: 2px solid #ff6b6b;
    border-bottom-left-radius: 0px;
    border-bottom-right-radius: 0px;
    opacity: 1;

    > label {
      color: #ff6b6b;
    }
  }

  .role-wrapper.disabled {
    border: none !important;
    opacity: 1 !important;
    padding-top: 0px;
  }

  .icon {
    font-size: 16px;
    color: #bababa;
  }

  .icon.success {
    color: #5aa700;
    font-size: 16px;
  }

  .permission-list {
    margin: 0px;
    padding: 0px;
    padding-left: 16px;

    .permission-list-item {
      color: #525252;
      font-size: 14px;
      margin-top: 8px;
    }

    .permission-list-item:first-child {
      margin-top: 12px;
    }
  }
`;

export const Role: FC<RoleProps> = ({ label, selected, disabled = false, id, error, onChange }) => {
  const [isActive, setIsActive] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  const onFocus = (): void => {
    setIsActive(true);
  };

  const onBlur = (): void => {
    setIsActive(false);
  };

  const onToggle = (_: unknown, expanded: boolean): void => {
    setIsExpanded(expanded);
  };

  const getArrowIcon = (isOpen: boolean) => (
    <>
      <div className="actions">See Permissions</div>
      {isOpen ? (
        <ArrowDropUpIcon style={{ color: '#1d84ff' }} />
      ) : (
        <ArrowDropDownIcon style={{ color: '#1d84ff' }} />
      )}
    </>
  );

  return (
    <Wrapper>
      <div
        className={`role-wrapper ${isActive ? 'active' : ''}
          ${error ? 'error' : ''}
          ${disabled ? 'disabled' : ''}`}
      >
        {label && <label>{label}</label>}
        <div>
          <Accordion onChange={onToggle}>
            <AccordionSummary
              aria-controls="panel1a-content"
              id="panel1a-header"
              expandIcon={false}
            >
              {disabled && (
                <>
                  <input
                    className={`input disabled`}
                    style={{ textTransform: 'capitalize' }}
                    value={rolesDetails[selected!]?.name}
                    onFocus={onFocus}
                    onBlur={onBlur}
                    disabled={disabled}
                  />
                  {getArrowIcon(isExpanded)}
                </>
              )}
              {!disabled && (
                <div>
                  <div className="role-radio-group-wrapper" onClick={(e) => e.stopPropagation()}>
                    <RadioGroup id={id} name={id} onChange={onChange} defaultValue={selected}>
                      {Object.entries(rolesDetails).map(([roleId, role]) => {
                        if (roleId !== RoleIdByName.ACCOUNT_OWNER) {
                          return (
                            <FormControlLabel
                              control={<StyledRadio />}
                              key={roleId}
                              label={role.name}
                              value={roleId}
                            />
                          );
                        }
                      })}
                    </RadioGroup>
                  </div>
                  {selected && (
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        marginTop: '16px',
                      }}
                    >
                      {getArrowIcon(isExpanded)}
                    </div>
                  )}
                </div>
              )}
            </AccordionSummary>{' '}
            {selected && (
              <AccordionDetails>
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'auto auto auto',
                    columnGap: '8px',
                    gridRowGap: '16px',
                  }}
                >
                  {Object.entries(rolesDetails[selected].permissions).map(
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
                          {permissionArr.map((permission) => (
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
              </AccordionDetails>
            )}
          </Accordion>
        </div>
      </div>
    </Wrapper>
  );
};
