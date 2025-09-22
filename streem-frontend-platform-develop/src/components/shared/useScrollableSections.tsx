import useScrollSpy from '#utils/useScrollSpy';
import { capitalize } from 'lodash';
import React, { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import KeyboardArrowLeftOutlinedIcon from '@material-ui/icons/KeyboardArrowLeftOutlined';
import { navigate } from '@reach/router';

const LabelsWrapper = styled.div.attrs({
  className: 'scrollable-labels',
  id: 'scrollable-labels',
})`
  display: flex;
  flex-direction: column;
  overflow: auto;
  padding: 16px 6px;
  @media (min-width: 900px) {
    padding: 16px 8px;
  }
  @media (min-width: 1200px) {
    padding: 16px;
  }

  h1 {
    margin: 0;
    font-size: 1.5dvw;
    line-height: 1.25;
    color: #333333;
    @media (max-width: 900px) {
      font-size: 2.5dvw;
    }
  }

  .title {
    display: flex;
    gap: 4px;
    align-items: center;
    margin-bottom: 16px;
  }

  .label {
    padding: 8px 10px;
    font-size: 14px;
    font-weight: normal;
    color: #999999;
    line-height: 1.14;
    letter-spacing: 0.16px;
    border-left: 2px solid #dadada;
    text-decoration: none;
    cursor: pointer;

    :hover {
      color: #333333;
      border-left: 2px solid #333333;
    }
  }

  .label.active {
    border-left: 2px solid #1d84ff;
    font-weight: bold;
    color: #333333;
  }
`;

export const CardWithTitle = styled.div.attrs(
  ({ className = 'card-with-title', id = 'card-with-title' }) => ({
    className,
    id,
  }),
)`
  .card-label {
    padding: 16px;
    font-size: 14px;
    font-weight: 700;
    line-height: 16px;
    color: #161616;
    margin: unset;
    border-bottom: solid 1px #e0e0e0;
  }
  background: #fff;
  margin-bottom: 6px;
  display: flex;
  flex-direction: column;
  @media (min-width: 900px) {
    margin-bottom: 8px;
  }
  @media (min-width: 1200px) {
    margin-bottom: 16px;
  }

  :last-child {
    margin-bottom: unset;
  }
`;

const ViewsWrapper = styled.div.attrs({
  className: 'scrollable-views',
  id: 'scrollable-views',
})`
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: auto;
  margin: 16px 0px;
  padding: 0px 6px;
  @media (min-width: 900px) {
    padding: 0px 8px;
  }
  @media (min-width: 1200px) {
    padding: 0px 16px;
  }
`;

type Item = {
  label: string;
  view: JSX.Element;
};

export type useScrollableSectionsProps = {
  title: string | JSX.Element;
  items: Item[];
};

type useScrollableSectionsState = {
  paddingBottom: number;
};

export function useScrollableSections({ title, items }: useScrollableSectionsProps) {
  const [state, setState] = useState<useScrollableSectionsState>({
    paddingBottom: 0,
  });
  const { paddingBottom } = state;
  const itemsRef = useRef<Array<HTMLDivElement>>([]);
  const scrollTarget = useRef<HTMLDivElement>(null);
  const contentTarget = useRef<HTMLDivElement>(null);

  const selectedIndex = useScrollSpy({
    itemsRef,
    scrollTarget,
    contentTarget,
  });

  const updateSize = () => {
    if (scrollTarget.current && contentTarget.current && itemsRef.current.length) {
      const scrollHeight = scrollTarget.current.clientHeight;
      const lastItemHeight = itemsRef.current[itemsRef.current.length - 1].clientHeight;
      const padding = scrollHeight - lastItemHeight;
      setState({
        paddingBottom: padding,
      });
    }
  };

  const scrollToSection = (index: number) => {
    itemsRef.current[index]?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  };

  useEffect(() => {
    updateSize();
  }, []);

  useEffect(() => {
    itemsRef.current = itemsRef.current.slice(0, items.length);
  }, [items.length]);

  const renderLabels = (): JSX.Element => (
    <LabelsWrapper>
      <div className="title">
        <KeyboardArrowLeftOutlinedIcon style={{ cursor: 'pointer' }} onClick={() => navigate(-1)} />
        <h1>{title}</h1>
      </div>
      {items.map((item, index) => (
        <a
          key={`section_label_${index}`}
          className={`label ${index === selectedIndex && 'active'}`}
          onClick={() => scrollToSection(index)}
        >
          {capitalize(item.label)}
        </a>
      ))}
    </LabelsWrapper>
  );

  const renderViews = (): JSX.Element => (
    <ViewsWrapper ref={scrollTarget}>
      <div ref={contentTarget} style={{ paddingBottom }}>
        {items.map(({ view: View, label }, index) => (
          <CardWithTitle
            id={`section_view_${index}`}
            key={`section_view_${index}`}
            ref={(el) => (itemsRef.current[index] = el as HTMLDivElement)}
          >
            {label && <h4 className="card-label">{label}</h4>}
            {View}
          </CardWithTitle>
        ))}
      </div>
    </ViewsWrapper>
  );

  return { renderLabels, renderViews };
}
