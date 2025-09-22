import PushPinImage from '#assets/svg/PushPin';
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell, { TableCellProps } from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import React, { useCallback, useLayoutEffect, useMemo, useRef, useState } from 'react';
import styled from 'styled-components';

export type DataTableColumn = {
  id: string;
  label: string;
  minWidth?: number | string;
  maxWidth?: number | string;
  align?: TableCellProps['align'];
  format?: (value: any) => any;
  pinned?: boolean;
};

const DataTableWrapper = styled.div.attrs({
  className: 'data-table-wrapper',
})`
  display: flex;
  overflow: auto;
`;

const Wrapper = styled.div.attrs({
  className: 'data-table',
})<{ $isCompactView: boolean }>`
  display: flex;
  overflow: hidden;
  width: 100%;

  .data-table-empty {
    height: 64px;
    color: #bbbbbb;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .MuiPaper-root {
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;
    font-family: inherit;
    border-bottom: 1px solid rgb(218, 218, 218);

    .MuiTableContainer-root {
      display: flex;
      flex: 1;
      .MuiTableHead-root {
        .MuiTableRow-head {
          background-color: #dadada;
        }
        .MuiTableCell-stickyHeader {
          background-color: #dadada;
          ${({ $isCompactView }) =>
            $isCompactView &&
            `
              font-size: 12px;
            `}
        }

        .MuiTableCell-head {
          font-size: 14px;
          font-weight: bold;
          line-height: 1.29;
          letter-spacing: 0.16px;
          font-family: inherit;
          color: #333333;
          padding: 12px 16px;

          ${({ $isCompactView }) =>
            $isCompactView &&
            `
              font-size: 12px;
              font-weight: 600;
              color: #000000;
              padding: 8px;
              width: max-content;
              text-wrap: nowrap;
              border: 1px solid rgba(224, 224, 224, 1);
            `}
        }
      }

      .MuiTableBody-root {
        .MuiTableRow-root {
          background-color: #f4f4f4;

          ${({ $isCompactView }) =>
            $isCompactView &&
            `
              background-color: #fff;
            `}

          :hover {
            background-color: rgb(238, 238, 238);
          }

          .MuiTableCell-root {
            span {
              -webkit-box-orient: vertical;
              overflow: hidden;
              display: -webkit-inline-box;
            }
          }
        }
        .MuiTableCell-body {
          font-size: 14px;
          line-height: 1.29;
          color: #333333;
          font-family: inherit;
          padding: 8px 16px;
          vertical-align: top;

          ${({ $isCompactView }) =>
            $isCompactView &&
            `
              color: #262626;
              font-size: 12px;
              font-family: inherit;
              padding: 8px;
              border: 1px solid rgba(224, 224, 224, 1);
            `}

          .primary {
            cursor: pointer;
            color: #1d84ff;
            width: max-content;

            ${({ $isCompactView }) =>
              $isCompactView &&
              `
                width: unset;
              `}

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

          .flex-column {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
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

          .MuiChip-label {
            display: flex !important;
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
        }
      }
    }
  }

  .pinned-column {
    box-shadow: 5px 0 5px -2px rgba(0, 0, 0, 0.3);
  }
`;

const TableView = ({
  columns,
  pinnedColumns,
  rows,
  emptyTitle,
  columnWidth,
  reference,
  isCompactView,
}: {
  columns: DataTableColumn[];
  pinnedColumns: DataTableColumn[];
  rows: any[];
  emptyTitle?: string;
  columnWidth: number[];
  reference: any;
  isCompactView: boolean;
}) => {
  const getStyle = useCallback(
    (
      column: DataTableColumn,
      index: number,
      columnWidth?: number[],
      source?: string,
    ): React.CSSProperties => {
      return column.pinned
        ? {
            position: 'sticky',
            left: columnWidth?.[index - 1] || 0,
            zIndex: source === 'thead' ? 9 : 6,
            background: source !== 'thead' ? 'white' : undefined,
            minWidth: isCompactView ? 'unset' : column.minWidth,
            maxWidth: isCompactView ? 'unset' : column?.maxWidth ?? column.minWidth,
            overflowWrap: 'break-word',
          }
        : !isCompactView
        ? {
            minWidth: column.minWidth,
            maxWidth: column?.maxWidth ?? column.minWidth,
            overflowWrap: 'break-word',
          }
        : {};
    },
    [columnWidth],
  );

  return (
    <Wrapper $isCompactView={isCompactView}>
      <Paper square>
        <TableContainer>
          <Table stickyHeader aria-label="sticky table" ref={reference}>
            <TableHead id="thead">
              <TableRow>
                {pinnedColumns.map((column, index) => (
                  <TableCell
                    key={column.id}
                    align={column.align}
                    style={getStyle(column, index, columnWidth, 'thead')}
                    className="pinned-column"
                  >
                    <PushPinImage size="16" color={'#1D84FF'} />
                    {column.label}
                  </TableCell>
                ))}
                {columns.map((column, index) => (
                  <TableCell
                    key={column.id}
                    align={column.align}
                    style={getStyle(column, index, columnWidth, 'thead')}
                  >
                    {column.label}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.length > 0 &&
                rows.map((row, rowIndex) => {
                  return (
                    <TableRow key={row.id}>
                      {pinnedColumns.map((column, columnIndex) => {
                        return (
                          <TableCell
                            key={row.id && column.id ? row.id + column.id : rowIndex + columnIndex}
                            align={column.align}
                            style={getStyle(column, columnIndex, columnWidth, 'tbody')}
                            className="pinned-column"
                          >
                            {column.format ? (
                              column.format(row)
                            ) : (
                              <span
                                title={row[column.id]}
                                style={{
                                  maxWidth: column?.maxWidth,
                                }}
                              >
                                {row[column.id] ?? '-N/A-'}
                              </span>
                            )}
                          </TableCell>
                        );
                      })}
                      {columns.map((column, columnIndex) => {
                        return (
                          <TableCell
                            key={row.id && column.id ? row.id + column.id : rowIndex + columnIndex}
                            align={column.align}
                          >
                            {column.format ? (
                              column.format(row)
                            ) : (
                              <span
                                title={row[column.id]}
                                style={{
                                  maxWidth: column?.maxWidth,
                                }}
                              >
                                {row[column.id] ?? '-N/A-'}
                              </span>
                            )}
                          </TableCell>
                        );
                      })}
                    </TableRow>
                  );
                })}
            </TableBody>
          </Table>
        </TableContainer>
        {rows.length === 0 && <div className="data-table-empty">{emptyTitle}</div>}
      </Paper>
    </Wrapper>
  );
};

export default function DataTable({
  columns,
  rows,
  emptyTitle,
  isCompactView = false,
}: {
  columns: DataTableColumn[];
  rows: any[];
  emptyTitle?: string;
  isCompactView?: boolean;
}) {
  const [columnWidth, setColumnWidth] = useState<number[]>([]);

  const calculateFirstColumnWidth = () => {
    if (scrollableTableRef.current) {
      const pinnedColumns = Array.from(
        scrollableTableRef.current?.querySelectorAll('#thead .pinned-column'),
      );

      const pinnedWidths: number[] = pinnedColumns.reduce((acc: any, th: any) => {
        const width = th.offsetWidth;
        const cumulativeWidth = (acc.length > 0 ? acc[acc.length - 1] : 0) + width;
        return [...acc, cumulativeWidth];
      }, []);
      setColumnWidth(pinnedWidths);
    }
  };

  const scrollableTableRef = useRef(null);

  useLayoutEffect(() => {
    calculateFirstColumnWidth();
  }, [columns]);

  const { pinned, unpinned } = useMemo(() => {
    return columns.reduce(
      (acc, column) => {
        if (column.pinned) {
          acc.pinned.push(column);
        } else {
          acc.unpinned.push(column);
        }
        return acc;
      },
      { pinned: [], unpinned: [] },
    );
  }, [columns]);

  return (
    <DataTableWrapper>
      <TableView
        columns={unpinned}
        pinnedColumns={pinned}
        rows={rows}
        emptyTitle={emptyTitle}
        columnWidth={columnWidth}
        reference={scrollableTableRef}
        isCompactView={isCompactView}
      />
    </DataTableWrapper>
  );
}
