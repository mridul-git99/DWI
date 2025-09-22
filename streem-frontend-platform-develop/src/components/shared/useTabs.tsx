import React, { FC, useEffect, useState } from 'react';
import styled from 'styled-components';
import { validateNumber } from '#utils';
import { useQueryParams } from '#hooks/useQueryParams';
import Tooltip from './Tooltip';

export const HeaderWrapper = styled.div.attrs({
  className: 'tab-header',
})<{
  $capitalizeHeader: boolean;
}>`
  background-color: transparent;
  display: flex;
  grid-area: tab-header;
  overflow-x: hidden;

  .tab-header-items {
    display: flex;
    overflow-x: auto;
    .tab-header-item {
      align-items: center;
      background-color: #f4f4f4;
      border-top: 2px solid transparent;
      cursor: pointer;
      display: flex;
      min-width: 160px;
      max-width: 160px;
      padding: 12px 16px;
      text-transform: ${(props) => (props.$capitalizeHeader ? 'capitalize' : 'unset')};
      font-size: 14px;
      font-weight: 600;
      line-height: 1.29;
      letter-spacing: 0.16px;

      span {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      &.active {
        background-color: #ffffff;
        border-top-color: #1d84ff;
      }
    }
  }
`;

export const BodyWrapper = styled.div.attrs({
  className: 'tab-body',
})`
  background-color: #ffffff;
  grid-area: tab-body;
  padding: 16px 16px 0px;
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
`;

export type Tab = {
  id?: string;
  label: string;
  values?: any;
  tabContent: FC | FC<TabContentProps>;
  passThroughProps?: TabContentProps;
  index?: number;
};

export type TabContentProps = {
  label: Tab['label'];
  values?: Tab['values'];
  activeTabValue: string;
};

type useTabType = {
  tabs: Tab[];
  BeforeHeader?: {
    Component: FC<any>;
    props?: any;
  };
  AfterHeader?: {
    Component: FC<any>;
    props?: any;
  };
  useTabIndexFromQuery?: boolean;
  capitalizeHeader?: boolean;
  indicatorForActiveTab?: string;
  showTooltip?: boolean;
};

const useTabs = ({
  tabs,
  BeforeHeader,
  AfterHeader,
  useTabIndexFromQuery = false,
  capitalizeHeader = true,
  indicatorForActiveTab = 'label',
  showTooltip = false,
}: useTabType) => {
  const { getQueryParam, updateQueryParams } = useQueryParams();
  const activeTabIndex = getQueryParam('tab', 0);

  const [activeTab, setActiveTab] = useState(tabs[Number(activeTabIndex)]);

  const TabContent = activeTab.tabContent;

  const onChangeTab = (tab: Tab) => {
    if (useTabIndexFromQuery) {
      const newValue = tabs.findIndex((t) => t.id === tab.id) || 0;
      updateQueryParams({
        newParams: { tab: newValue },
        shouldClear: true,
        navigateOptions: { replace: true },
      });
    } else {
      setActiveTab(tab);
    }
  };

  useEffect(() => {
    if (useTabIndexFromQuery && validateNumber(activeTabIndex)) {
      setActiveTab(tabs[Number(activeTabIndex)]);
    }
  }, [useTabIndexFromQuery, activeTabIndex]);

  const renderTabHeader = () => (
    <HeaderWrapper $capitalizeHeader={capitalizeHeader}>
      {BeforeHeader && (
        <BeforeHeader.Component
          setActiveTab={setActiveTab}
          activeTab={activeTab}
          {...BeforeHeader.props}
        />
      )}
      <div className="tab-header-items">
        {tabs.map((tab) => (
          <div
            className={`tab-header-item ${
              activeTab[`${indicatorForActiveTab}`] === tab[`${indicatorForActiveTab}`]
                ? 'active'
                : ''
            }`}
            key={tab.label}
            onClick={() => onChangeTab(tab)}
          >
            <Tooltip title={showTooltip ? tab.label : ''} placement="bottom-start">
              <span>{tab.label}</span>
            </Tooltip>
          </div>
        ))}
      </div>
      {AfterHeader && (
        <AfterHeader.Component
          setActiveTab={setActiveTab}
          activeTab={activeTab}
          {...AfterHeader.props}
        />
      )}
    </HeaderWrapper>
  );

  const renderTabContent = () => (
    <BodyWrapper>
      <TabContent label={activeTab.label} values={activeTab?.values} key={activeTab.label} />
    </BodyWrapper>
  );

  return { renderTabHeader, renderTabContent };
};

export default useTabs;
