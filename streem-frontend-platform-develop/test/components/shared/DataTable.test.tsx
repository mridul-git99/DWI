import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { DataTable } from '#components';
import { DataTableColumn } from '#components/shared/DataTable';

describe('DataTable', () => {
  const columns: DataTableColumn[] = [
    {
      id: 'id',
      label: 'ID',
      minWidth: 50,
    },
    {
      id: 'name',
      label: 'Name',
      minWidth: 100,
    },
  ];

  const rows = [
    { id: 1, name: 'John Doe', age: 18 },
    { id: 2, name: 'Jane Smith', age: 18 },
  ];

  test('renders table with correct column headers', () => {
    const { getByText } = render(<DataTable columns={columns} rows={rows} />);

    columns.forEach((column) => {
      const columnHeader = getByText(column.label);
      expect(columnHeader).toBeInTheDocument();
    });
  });

  test('renders table with correct number of rows', () => {
    const { getAllByRole } = render(<DataTable columns={columns} rows={rows} />);
    const tableRows = getAllByRole('row');

    const expectedRowCount = rows.length;
    expect(tableRows.length - 1).toBe(expectedRowCount);
  });

  test('renders table cells with correct data', () => {
    const { queryAllByRole } = render(<DataTable columns={columns} rows={rows} />);
    const tableCells = queryAllByRole('cell');

    const expectedCellCount = columns.length * rows.length;
    expect(tableCells.length).toBe(expectedCellCount);

    rows.forEach((row) => {
      columns.forEach((column) => {
        const cellText = row[column.id].toString();
        const cell = tableCells.find((cell) => cell.textContent === cellText);
        expect(cell).toBeInTheDocument();
      });
    });
  });

  test('handles column formatting correctly', () => {
    const formatMock = jest.fn((value) => `Formatted ${value.age}`);
    const formattedColumns: DataTableColumn[] = [
      ...columns,
      {
        id: 'age',
        label: 'Age',
        minWidth: 80,
        format: formatMock,
      },
    ];

    const { getAllByText } = render(<DataTable columns={formattedColumns} rows={rows} />);
    const ageCells = getAllByText('Formatted 18');
    expect(ageCells.length).toBeGreaterThan(0);
    expect(formatMock).toHaveBeenCalledWith(rows[0]);
  });

  test('handles missing data with N/A placeholder', () => {
    const rowsWithMissingData = [...rows, { id: 3, name: null }];
    const { getByText } = render(<DataTable columns={columns} rows={rowsWithMissingData} />);
    const missingDataCell = getByText('-N/A-');
    expect(missingDataCell).toBeInTheDocument();
  });
});
