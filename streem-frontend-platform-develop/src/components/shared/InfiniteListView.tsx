import NestedMenuItem from '#components/shared/NestedMenuItem';
import { ChecklistAuditLogsType } from '#PrototypeComposer/ChecklistAuditLogs/types';
import { JobAuditLogType } from '#types';
import { DEFAULT_PAGE_SIZE } from '#utils/constants';
import { fetchDataParams, Pageable } from '#utils/globalTypes';
import { SessionActivity } from '#views/UserAccess/ListView/SessionActivity/types';
import { ExpandMore, Search } from '@material-ui/icons';
import { noop } from 'lodash';
import React, { FC, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { Button } from './Button';
import { StyledMenu } from './StyledMenu';

type Filter = {
  label: string;
  content: JSX.Element | (() => JSX.Element);
  onApply: () => void;
};

export type FilterProp = {
  filters: Filter[];
  onReset: () => void;
  activeCount: number;
};

type DataType =
  | Record<string, string | SessionActivity[]>
  | Record<string, string | JobAuditLogType[]>
  | Record<string, string | ChecklistAuditLogsType[]>;

type ExtraColumn = {
  header: string;
  template: (item: DataType, index: number) => JSX.Element;
};

interface ListViewProps {
  primaryButtonText?: string;
  primaryButtonDisable?: boolean;
  onPrimaryClick?: () => void;
  filterProp?: FilterProp;
  callOnScroll?: boolean;
  data: DataType[];
  fetchData: (params: fetchDataParams) => void;
  pageable: Pageable;
  isSearchable?: boolean;
  beforeColumns?: ExtraColumn[];
  afterColumns?: ExtraColumn[];
}

const Wrapper = styled.div.attrs({})`
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;

  .list-header {
    display: flex;
    padding: 13px 0px 13px 0px;
    border-bottom: 1px solid #999999;
  }

  .list-options {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .left-section {
      display: flex;
      align-items: center;
      gap: 16px;
    }
  }

  .list-body {
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: auto;
  }

  .list-card {
    border-bottom: 1px solid #dadada;
    display: flex;
  }

  .list-header-columns {
    flex: 1;
    font-size: 12px;
    color: #999999;
    flex-wrap: wrap;
    font-weight: bold;
    letter-spacing: 1px;
    padding: 0 12px;
    overflow-wrap: anywhere;
    display: flex;
    align-items: center;

    :first-child {
      width: 30%;
      flex: initial;
    }
  }

  .list-card-columns {
    flex: 1;
    flex-wrap: wrap;
    font-size: 14px;
    color: #666666;
    padding: 16px 12px;
    font-weight: 600;
    overflow-wrap: anywhere;
    display: flex;
    align-items: center;
    word-break: break-all;

    :first-child {
      width: 30%;
      flex: initial;
    }
  }

  .list-title {
    font-size: 20px;
    padding: 4px 0px;
    font-weight: 600;
    color: #1d84ff;
    cursor: pointer;
    align-items: center;
    display: flex;
    text-transform: capitalize;
  }

  .title-group {
    display: flex;
    flex-direction: column;
  }

  .list-code {
    padding-bottom: 4px;
    font-size: 14px;
    line-height: 14px;
    color: #333333;
  }

  .searchboxwrapper {
    position: relative;
    margin-right: 16px;
  }

  .searchbox {
    border: none;
    border-bottom: 1px solid #999999;
    outline: none;
    font-size: 13px;
    font-family: 'Open Sans', sans-serif;
    font-weight: lighter;
    color: #999999;
    width: 180px;
    height: 29px;
    background: #fff;
    padding-left: 10px;
  }

  .searchsubmit {
    width: 14px;
    height: 29px;
    position: absolute;
    top: 0;
    right: 0;
    background: #fff;
    border: none;
    border-bottom: 1px solid #999999;
    color: #999999;
    cursor: pointer;
  }

  .resetOption {
    cursor: pointer;
    color: #1d84ff;
    font-size: 11px;
    font-weight: 600;
    font-style: italic;
  }

  .user-thumb {
    width: 34px;
    height: 34px;
    border-radius: 17px;
    border: solid 1.5px #fff;
    align-items: center;
    background-color: #f7f9fa;
    justify-content: center;
    display: flex;
    color: #1d84ff;
    margin-right: -5px;
    font-size: 13px;
    cursor: pointer;
  }
`;

const FilterWrapper = styled.div.attrs({})`
  width: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;

  .MuiPickersStaticWrapper-root {
    border-bottom: 1px solid #dadada;

    .MuiPickersDesktopDateRangeCalendar-root {
      justify-content: center;

      .MuiPickersDesktopDateRangeCalendar-arrowSwitcher {
        padding: 16px 8px 8px 8px;

        .MuiSvgIcon-root {
          color: #333333;
        }
      }
    }
  }

  .MuiTypography-subtitle1 {
    font-family: 'Open Sans', sans-serif;
    font-size: 16px;
    font-weight: bold;
    color: #333333;
  }

  .MuiPickersDesktopDateRangeCalendar-calendar {
    min-height: 230px;
  }

  .MuiInput-underline:before {
    border-bottom: 1px solid #999999;
  }

  .MuiInput-underline:after {
    border-bottom: 2px solid #1d84ff;
  }

  .MuiInput-underline:hover:not(.Mui-disabled):before {
    border-bottom: 1px solid #999999;
  }

  .MuiFormLabel-root {
    font-size: 10px;
  }

  .MuiIconButton-root {
    color: #999999;
  }

  .MuiFormLabel-root.Mui-focused {
    color: #1d84ff;
  }

  .MuiPickersCalendar-weekDayLabel {
    font-size: 12px;
    line-height: 0.83;
    color: #7f8fa4;
  }

  .MuiPickersDay-root {
    border-radius: 0px !important;
  }

  .MuiPickersDay-today {
    border: none !important;
  }

  .MuiPickersDay-root.Mui-selected {
    background-color: #1d84ff !important;
    color: #000;
  }

  .MuiPickersDateRangeDay-dayInsideRangeInterval {
    color: #000;
  }

  .MuiPickersDateRangeDay-rangeIntervalDayPreview {
    border-radius: 0px !important;
    border-top-left-radius: 0px !important;
    border-bottom-left-radius: 0px !important;
  }

  .MuiPickersDateRangeDay-rangeIntervalDayHighlight {
    background-color: rgba(29, 132, 255, 0.2);
    border-radius: 0px !important;
    border-top-left-radius: 0px !important;
    border-bottom-left-radius: 0px !important;
  }

  .MuiInputBase-input {
    color: #999999;
  }

  .timepicker-container {
    display: flex;
    justify-content: space-between;
    flex: 1;
    padding: 16px;

    .MuiFormControl-root {
      margin-left: 8px;

      :first-child {
        margin-right: 8px;
        margin-left: 0px;
      }

      .MuiInputBase-adornedStart {
        svg {
          padding-left: 4px;
          padding-right: 4px;
          color: #999999;
        }
      }

      .MuiFormHelperText-root {
        display: none;
      }
    }
  }

  .picker-actions {
    width: 100%;
    padding: 16px;
    display: flex;
    justify-content: flex-end;
  }
`;

export const InfiniteListView: FC<ListViewProps> = ({
  primaryButtonText,
  primaryButtonDisable = false,
  onPrimaryClick = () => noop,
  data,
  fetchData,
  pageable,
  beforeColumns,
  afterColumns,
  filterProp,
  callOnScroll = true,
  isSearchable = true,
}) => {
  const loading = useRef(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleOnScroll = (e: React.UIEvent<HTMLElement>) => {
    e.stopPropagation();
    if (callOnScroll) {
      const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
      if (
        scrollTop + clientHeight >= scrollHeight - clientHeight &&
        !pageable.last &&
        !loading.current
      ) {
        loading.current = true;
        fetchData({ page: pageable.page + 1, size: pageable.pageSize || DEFAULT_PAGE_SIZE });
      }
    }
  };

  const onApplyFilter = (e: React.MouseEvent, onApply: () => void) => {
    e.stopPropagation();
    onApply();
    handleClose();
  };

  useEffect(() => {
    loading.current = false;
  }, [pageable.page]);

  return (
    <Wrapper>
      <div className="list-options">
        <div className="left-section">
          {filterProp && (
            <>
              <Button
                variant="secondary"
                aria-controls="top-menu"
                aria-haspopup="true"
                onClick={handleOpen}
                style={{ padding: '8px 16px 8px 24px' }}
              >
                {filterProp?.activeCount !== 0 ? `${filterProp?.activeCount} Filters` : 'Filters '}
                <ExpandMore style={{ fontSize: 20, marginLeft: 8 }} />
              </Button>
              <StyledMenu
                id="filter-menu"
                anchorEl={anchorEl}
                keepMounted
                disableEnforceFocus
                open={Boolean(anchorEl)}
                onClose={handleClose}
                style={{ marginTop: 40 }}
              >
                {filterProp.filters.map((filter) => {
                  return (
                    <NestedMenuItem
                      key={`filter_${filter.label}`}
                      right
                      disabled={true}
                      label={filter.label}
                      mainMenuOpen={anchorEl ? true : false}
                    >
                      <FilterWrapper
                        onClick={(e) => {
                          e.stopPropagation();
                        }}
                        onKeyDown={(e) => e.stopPropagation()}
                      >
                        {typeof filter.content === 'function' ? filter.content() : filter.content}
                        <div className="picker-actions">
                          <Button
                            style={{ padding: '6px 12px' }}
                            onClick={(e) => onApplyFilter(e, filter.onApply)}
                          >
                            Apply Filter
                          </Button>
                        </div>
                      </FilterWrapper>
                    </NestedMenuItem>
                  );
                })}
              </StyledMenu>
            </>
          )}
          {isSearchable && (
            <div className="searchboxwrapper">
              <input className="searchbox" type="text" placeholder="Search" />
              <Search className="searchsubmit" />
            </div>
          )}
          {filterProp?.activeCount && filterProp?.activeCount > 0 ? (
            <span className="resetOption" onClick={filterProp?.onReset}>
              Reset
            </span>
          ) : null}
        </div>
        {primaryButtonText && (
          <Button disabled={primaryButtonDisable} onClick={onPrimaryClick}>
            {primaryButtonText}
          </Button>
        )}
      </div>
      <div className="list-header">
        {beforeColumns?.map((beforeColumn) => (
          <div key={`beforeColumn_${beforeColumn.header}`} className="list-header-columns">
            {beforeColumn.header}
          </div>
        ))}
        {afterColumns?.map((afterColumn) => (
          <div key={`afterColumn_${afterColumn.header}`} className="list-header-columns">
            {afterColumn.header}
          </div>
        ))}
      </div>
      <div className="list-body" onScroll={handleOnScroll}>
        {data.map((el, index) => (
          <div key={`list_el_${el.id}`} className="list-card">
            {beforeColumns?.map((beforeColumn) => beforeColumn.template(el, index))}
            {afterColumns?.map((afterColumn) => afterColumn.template(el, index))}
          </div>
        ))}
      </div>
    </Wrapper>
  );
};
