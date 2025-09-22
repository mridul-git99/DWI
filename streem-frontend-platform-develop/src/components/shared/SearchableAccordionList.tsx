import { Button, Checkbox, TextInput } from '#components';
import { Accordion, AccordionActions, AccordionDetails, AccordionSummary } from '@material-ui/core';
import { Clear, ExpandMore, Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useRef, useState } from 'react';
import styled from 'styled-components';

type TSearchableAccordionListProps = {
  accordionSections: Record<string, string>;
  columns: Record<string, Record<string, any>>;
  onColumnToggle: ({
    key,
    checked,
    column,
    sectionkey,
  }: {
    key: string;
    checked: boolean;
    column: Record<string, any>;
    sectionkey: string;
  }) => void;
  searchPlaceholder: string;
  accordionTitle?: string;
  onSectionSelectAll?: (key: string) => void;
  onSectionRemoveAll?: (key: string) => void;
  fetchResources?: (searchQuery: string) => void;
  fetchNextResources?: () => void;
};

const Wrapper = styled.div`
  flex: 1;
  .section {
    flex-direction: column;
    display: flex;
    flex: 1;
    height: 70dvh;

    &-left {
      border-right: 1px solid #000;

      .input {
        flex: unset;
        margin-bottom: 16px;
      }
    }

    &-list {
      padding: 0 8px;
      overflow: auto;

      &-heading {
        font-size: 14px;
        font-weight: bold;
        line-height: 1.57;
        color: #161616;
        margin-bottom: 12px;
      }

      .checkbox-input {
        padding-block: 16px 8px;
        border-top: 1px solid #d9d9d9;

        :first-child {
          border-top: none;
          padding-top: 0;
        }

        .container {
          text-align: left;
          font-size: 14px;
          color: #000;
        }
      }

      .MuiAccordion-root {
        :before {
          background-color: unset;
        }
      }

      .MuiAccordion-root.Mui-expanded {
        margin: unset;
      }

      .columns-accordion {
        box-shadow: none;
        width: 100%;

        .MuiAccordionSummary-root.Mui-expanded {
          min-height: unset;
          .MuiAccordionSummary-content.Mui-expanded {
            margin: 12px 0;
          }
        }

        .MuiAccordionDetails-root {
          padding: 8px 0 8px 8px;
        }

        .MuiAccordionSummary-root {
          padding: 0px 16px 0px 0px;
        }

        .MuiAccordionActions-root {
          padding: 0;

          button {
            margin: 0;
            font-size: 12px;
          }
        }

        .MuiAccordionSummary-content {
          font-size: 14px;
          line-height: 1.14;
          letter-spacing: 0.16px;
          color: #141414;
        }

        .MuiAccordionDetails-root {
          flex-direction: column;
        }

        .MuiAccordion-root.Mui-expanded {
          margin-top: 8px;
        }
        .accordian-summary-container {
          display: flex;
          justify-content: space-between;
          width: 100%;
        }
      }
    }
  }
`;

const SearchableAccordionList: FC<TSearchableAccordionListProps> = ({
  accordionSections,
  columns,
  onColumnToggle,
  searchPlaceholder,
  accordionTitle,
  onSectionSelectAll,
  onSectionRemoveAll,
  fetchResources,
  fetchNextResources,
}) => {
  const [searchQuery, setSearchQuery] = useState<string>('');
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchResources?.(searchQuery);
  }, [searchQuery]);

  const handleScroll = (e: any, columnsKey: string) => {
    if (columnsKey !== 'resourceColumns') {
      return;
    }
    const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
    if (scrollHeight - Math.ceil(scrollTop) <= clientHeight) {
      fetchNextResources?.();
    }
  };

  const getDisableButtonsStatus = useMemo(() => {
    return Object.entries(columns).reduce((result, [sectionKey, columnsData]) => {
      if (sectionKey === 'resourceColumn') {
        return result;
      }
      const allChecked = Object.values(columnsData).every((column) => column.checked === true);
      const allUnchecked = Object.values(columnsData).every((column) => column.checked === false);

      result[sectionKey] = {
        deselectAllDisabled: !allUnchecked,
        selectAllDisabled: !allChecked,
      };

      return result;
    }, {});
  }, [columns]);

  return (
    <Wrapper>
      <div className="section section-left">
        <TextInput
          BeforeElement={Search}
          AfterElement={Clear}
          afterElementClass="clear"
          afterElementWithoutError
          name="search-filter"
          className="search-column-name"
          ref={searchInputRef}
          afterElementClick={() => {
            searchInputRef?.current?.value = '';
            setSearchQuery('');
          }}
          onChange={debounce(({ value }) => setSearchQuery(value), 500)}
          placeholder={searchPlaceholder}
          autoComplete="off"
        />
        <div className="section-list">
          {accordionTitle && <div className="section-list-heading">{accordionTitle}</div>}
          {Object.entries(accordionSections).map(([_key, label]) => (
            <Accordion className="columns-accordion" key={_key} square>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <div className="accordian-summary-container">{label}</div>
              </AccordionSummary>
              {onSectionSelectAll && onSectionRemoveAll && _key !== 'resourceColumns' && (
                <AccordionActions>
                  {getDisableButtonsStatus[_key]?.selectAllDisabled && (
                    <Button
                      color="blue"
                      variant="textOnly"
                      onClick={() => onSectionSelectAll(_key)}
                    >
                      Select All
                    </Button>
                  )}
                  {getDisableButtonsStatus[_key]?.deselectAllDisabled && (
                    <Button
                      color="blue"
                      variant="textOnly"
                      onClick={() => onSectionRemoveAll(_key)}
                    >
                      Deselect All
                    </Button>
                  )}
                </AccordionActions>
              )}
              <AccordionDetails>
                <div className="columns-accordion-details" onScroll={(e) => handleScroll(e, _key)}>
                  {Object.entries(columns[_key]).map(([key, column]) => {
                    if (
                      key !== 'resourceColumn' &&
                      searchQuery &&
                      !column.displayName.toLowerCase().includes(searchQuery?.toLowerCase())
                    )
                      return null;
                    return (
                      <Checkbox
                        key={key}
                        checked={column.checked}
                        label={column.displayName}
                        onClick={(checked) =>
                          onColumnToggle({ key, checked, column, sectionkey: _key })
                        }
                      />
                    );
                  })}
                </div>
              </AccordionDetails>
            </Accordion>
          ))}
        </div>
      </div>
    </Wrapper>
  );
};

export default SearchableAccordionList;
