import React from 'react';
import { render, fireEvent, getByTestId } from '@testing-library/react';
import { Add } from '@material-ui/icons';
import { AddNewItem } from '#components';

describe('AddNew', () => {
  test('renders with default label', () => {
    const { getByText } = render(<AddNewItem />);
    const labelElement = getByText('Add New');
    expect(labelElement).toBeInTheDocument();
  });

  test('renders with custom label', () => {
    const { getByText } = render(<AddNewItem label="Custom Label" />);
    const labelElement = getByText('Custom Label');
    expect(labelElement).toBeInTheDocument();
  });

  test('renders with default icon', () => {
    const { getByTestId } = render(<AddNewItem />);
    const iconElement = getByTestId('add-new-icon');
    expect(iconElement).toBeInTheDocument();
    expect(iconElement).toHaveClass('icon');
  });

  test('calls onClick callback when clicked', () => {
    const onClickMock = jest.fn();
    const { getByTestId } = render(<AddNewItem onClick={onClickMock} />);
    const addNewElement = getByTestId('add-new');
    fireEvent.click(addNewElement);
    expect(onClickMock).toHaveBeenCalledTimes(1);
  });

  test('does not render label if label prop is an empty string', () => {
    const { queryByText } = render(<AddNewItem label="" />);
    const labelElement = queryByText('Add New');
    expect(labelElement).toBeNull();
  });
});
