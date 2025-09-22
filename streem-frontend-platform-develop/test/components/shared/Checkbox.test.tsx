import { Checkbox } from '#components';
import { fireEvent, render } from '@testing-library/react';
import React from 'react';

describe('Checkbox', () => {
  test('renders the checkbox with label', () => {
    const { getByLabelText } = render(<Checkbox label="Test Checkbox" />);
    const checkbox = getByLabelText('Test Checkbox');
    expect(checkbox).toBeInTheDocument();
    expect(checkbox.checked).toBeFalsy();
  });

  test('renders the checkbox as checked when checked prop is true', () => {
    const { getByLabelText } = render(<Checkbox label="Test Checkbox" checked />);
    const checkbox = getByLabelText('Test Checkbox');
    expect(checkbox.checked).toBeTruthy();
  });

  test('calls onClick handler when checkbox is clicked', () => {
    const handleClick = jest.fn();
    const { getByLabelText } = render(<Checkbox label="Test Checkbox" onClick={handleClick} />);
    const checkbox = getByLabelText('Test Checkbox');
    fireEvent.click(checkbox);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  test('renders the checkbox with value', () => {
    const { getByLabelText } = render(<Checkbox label="Test Checkbox" value="test-value" />);
    const checkbox = getByLabelText('Test Checkbox');
    expect(checkbox.value).toBe('test-value');
  });

  test('renders the checkbox with name', () => {
    const { getByLabelText } = render(<Checkbox label="Test Checkbox" name="test-name" />);
    const checkbox = getByLabelText('Test Checkbox');
    expect(checkbox.name).toBe('test-name');
  });
});
