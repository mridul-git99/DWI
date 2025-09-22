import PushPinImage from '#assets/svg/PushPin';
import { LogType, TriggerTypeEnum } from '#PrototypeComposer/checklist.types';
import { openLinkInNewTab } from '#utils';
import { InputTypes } from '#utils/globalTypes';
import { formatDateTime } from '#utils/timeUtils';
import { navigate } from '@reach/router';
import { defaultRangeExtractor, useVirtualizer } from '@tanstack/react-virtual';
import { capitalize } from 'lodash';
import React, { useCallback, useMemo } from 'react';
import styled, { css } from 'styled-components';
import { CustomTag } from './CustomTag';

const VirtualizationTableWrapper = styled.div`
  overflow: auto;
  width: 100%;

  .data-table-empty {
    height: 64px;
    color: #bbbbbb;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .primary {
    cursor: pointer;
    color: #1d84ff;
    width: max-content;

    :hover {
      color: #1d84ff;
    }
  }

  .secondary {
    cursor: pointer;
    color: #da1e28;

    :hover {
      color: #da1e28;
    }
  }

  .description {
    font-size: 12px;
    letter-spacing: 0.16px;
    color: #6f6f6f;
    display: flex;
    align-items: center;
    gap: 4px;
    svg {
      font-size: 12px;
    }
  }

  .overdue {
    font-size: 12px;
    line-height: 1.6;
    letter-spacing: 0.16px;

    &.orange {
      color: #f1821b;
    }

    &.red {
      color: #da1e28;
    }
  }
`;

const CellWrapper = styled.div<{
  $isHead: boolean;
  $width: number;
  $isCompact: boolean;
  $minHeight: number;
  $isPinned: boolean;
  $left?: number;
}>`
  min-height: ${({ $minHeight }) => $minHeight}px;
  width: ${({ $width }) => $width}px;
  padding: 7px 12px;
  overflow-wrap: break-word;
  color: rgb(51, 51, 51);
  font-size: 14px;
  background-color: ${({ $isHead, $isCompact }) =>
    $isHead ? 'rgb(218, 218, 218)' : $isCompact ? '#fff' : 'rgb(244, 244, 244)'};
  display: ${({ $isHead }) => ($isHead ? 'flex' : 'block')};

  ${({ $isCompact }) =>
    $isCompact &&
    css`
      font-size: 12px;
      color: #262626;
      padding: 8px;
      border: 1px solid rgba(224, 224, 224, 1);
    `}

  ${({ $isPinned }) =>
    $isPinned &&
    css`
      position: sticky;
      left: ${({ $left }) => $left || 0}px;
      z-index: 1;
      box-shadow: 5px 0 5px -2px rgba(0, 0, 0, 0.3);
    `}
`;

const estimateSize = 150;

const renderCell = (column: any, row: any) => {
  if (row[column.id + column.triggerType]) {
    if (column.triggerType === TriggerTypeEnum.RESOURCE) {
      const rowValue = row[column.id + column.triggerType];
      const cellValue = Object.values(rowValue.resourceParameters).reduce<any[]>((acc, p: any) => {
        acc.push(
          `${p.displayName}: ${p.choices
            .map((c: any) => `${c.objectDisplayName} (ID: ${c.objectExternalId})`)
            .join(', ')}`,
        );
        return acc;
      }, []);
      return cellValue.join(',');
    }
    if (column.triggerType === TriggerTypeEnum.JOB_ID) {
      return (
        <span
          title={row[column.id + column.triggerType].value}
          className="primary"
          onClick={() => {
            navigate(`/inbox/${row[column.id + column.triggerType].jobId}`);
          }}
        >
          {row[column.id + column.triggerType].value}
        </span>
      );
    } else if (column.triggerType === TriggerTypeEnum.PARAMETER_SELF_VERIFIED_BY) {
      const selfVerifiedAt = row[column.id + TriggerTypeEnum.PARAMETER_SELF_VERIFIED_AT]?.value;
      return (
        <>
          {row[column.id + column.triggerType].value ? (
            <span title={row[column.id + column.triggerType].value}>
              Performed at {formatDateTime({ value: selfVerifiedAt })}, by{' '}
              {row[column.id + column.triggerType].value}
            </span>
          ) : (
            '-'
          )}
        </>
      );
    } else if (column.triggerType === TriggerTypeEnum.PARAMETER_PEER_VERIFIED_BY) {
      const peerVerifiedAt = row[column.id + TriggerTypeEnum.PARAMETER_PEER_VERIFIED_AT]?.value;
      const peerVerificationStatus = row[column.id + TriggerTypeEnum.PARAMETER_PEER_STATUS]?.value;
      return (
        <>
          {row[column.id + column.triggerType].value ? (
            <span title={row[column.id + column.triggerType].value}>
              {capitalize(peerVerificationStatus.toLowerCase())} at{' '}
              {formatDateTime({ value: peerVerifiedAt })}, by{' '}
              {row[column.id + column.triggerType].value}
            </span>
          ) : (
            '-'
          )}
        </>
      );
    }

    if (column.type === LogType.DATE) {
      return formatDateTime({
        value: row[column.id + column.triggerType].value,
        type: InputTypes.DATE,
      });
    } else if (column.type === LogType.DATE_TIME) {
      return formatDateTime({
        value: row[column.id + column.triggerType].value,
        type: InputTypes.DATE_TIME,
      });
    } else if (column.type === LogType.TIME) {
      return formatDateTime({
        value: row[column.id + column.triggerType].value,
        type: InputTypes.TIME,
      });
    } else if (
      column.type === LogType.FILE &&
      row[column.id + column.triggerType]?.medias?.length
    ) {
      return (
        <div className="file-links">
          {row[column.id + column.triggerType].medias.map(
            (media: any, index: number, array: any[]) => {
              return (
                <CustomTag
                  as={'div'}
                  key={media.id}
                  onClick={() => openLinkInNewTab(`/media?link=${media.link}`)}
                >
                  <span>
                    {' '}
                    {media.name}
                    {index < array.length - 1 && <span style={{ color: '#333333' }}>,</span>}
                  </span>
                </CustomTag>
              );
            },
          )}
        </div>
      );
    }
    return (
      <span title={row[column.id + column.triggerType].value}>
        {row[column.id + column.triggerType].value || '-'}
      </span>
    );
  }
  return '-';
};

export function VirtualizationTable({
  columns,
  data,
  emptyTitle,
  isCompact = false,
}: {
  data: Array<any>;
  columns: Array<any>;
  emptyTitle: string;
  isCompact?: boolean;
}) {
  const parentRef = React.useRef<HTMLDivElement | null>(null);

  const stickyIndexs = useMemo(() => {
    const indexs: Array<number> = [];
    columns.every((column, index) => {
      if (column.pinned) {
        indexs.push(index);
        return true;
      }
      return false;
    });
    return indexs;
  }, [columns]);

  const virtualizer = useVirtualizer({
    count: data.length,
    estimateSize: () => 50,
    overscan: 4,
    getScrollElement: () => parentRef.current,
    measureElement:
      typeof window !== 'undefined' && navigator.userAgent.indexOf('Firefox') === -1
        ? (element) => element?.getBoundingClientRect().height
        : undefined,
  });

  const columnVirtualizer = useVirtualizer({
    horizontal: true,
    count: columns.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => estimateSize,
    overscan: (columns.length + stickyIndexs.length) % 2 ? 5 : 4,
    rangeExtractor: useCallback(
      (range) => {
        const next = new Set([...stickyIndexs, ...defaultRangeExtractor(range)]);
        return [...next];
      },
      [stickyIndexs],
    ),
  });

  const horizontalScrollOffset = columnVirtualizer.scrollOffset || 0;
  const columnItems = columnVirtualizer.getVirtualItems();
  const [before, after] =
    columnItems.length > 0
      ? [
          stickyIndexs.length > 0
            ? horizontalScrollOffset > stickyIndexs.length * estimateSize
              ? columnItems[stickyIndexs.length].start - stickyIndexs.length * estimateSize
              : columnItems[0].start
            : columnItems[0].start,
          columnVirtualizer.getTotalSize() - columnItems[columnItems.length - 1].end,
        ]
      : [0, 0];

  return (
    <VirtualizationTableWrapper ref={parentRef}>
      <div
        style={{
          height: virtualizer.getTotalSize(),
          position: 'relative',
        }}
      >
        {virtualizer.getVirtualItems().map((row) => {
          return (
            <div
              key={row.index}
              data-index={row.index}
              ref={virtualizer.measureElement}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                transform: `translateY(${row.start}px)`,
                display: 'flex',
              }}
            >
              <div style={{ width: `${before}px` }} />
              {columnItems.map((column) => {
                const originalColumn = columns[column.index];
                return (
                  <CellWrapper
                    key={column.index}
                    $isHead={row.index === 0}
                    $width={column.size}
                    $isCompact={isCompact}
                    $minHeight={row.size}
                    $isPinned={originalColumn.pinned}
                    $left={column.start}
                  >
                    {row.index === 0 ? (
                      <div
                        style={{
                          alignSelf: 'center',
                          color: 'rgb(51, 51, 51)',
                          fontWeight: 'bold',
                          width: '100%',
                        }}
                      >
                        {originalColumn.displayName}
                        {originalColumn.pinned && (
                          <PushPinImage
                            size="16"
                            color={'#1D84FF'}
                            style={{ position: 'absolute' }}
                          />
                        )}
                      </div>
                    ) : (
                      renderCell(originalColumn, data[row.index])
                    )}
                  </CellWrapper>
                );
              })}
              <div style={{ width: `${after}px` }} />
            </div>
          );
        })}
      </div>
      {data.length === 1 && <div className="data-table-empty">{emptyTitle}</div>}
    </VirtualizationTableWrapper>
  );
}
