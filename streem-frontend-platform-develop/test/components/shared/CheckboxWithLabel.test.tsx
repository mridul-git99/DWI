import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { CheckboxWithLabel } from '#components';

describe('Checkbox', () => {
  test('renders the checkbox with the label', () => {
    const { getByLabelText } = render(<CheckboxWithLabel label="Check me" />);
    const checkbox = getByLabelText('Check me') as HTMLInputElement;

    expect(checkbox).toBeInTheDocument();
    expect(checkbox.checked).toBe(false);
    expect(checkbox.readOnly).toBe(true);
  });

  test('renders the checkbox as checked when isChecked prop is true', () => {
    const { getByLabelText } = render(<CheckboxWithLabel label="Check me" isChecked />);
    const checkbox = getByLabelText('Check me') as HTMLInputElement;

    expect(checkbox.checked).toBe(true);
  });

  test('renders multiple checkboxes with unique labels', () => {
    const { getByLabelText } = render(
      <>
        <CheckboxWithLabel label="Checkbox 1" />
        <CheckboxWithLabel label="Checkbox 2" />
        <CheckboxWithLabel label="Checkbox 3" />
      </>,
    );

    expect(getByLabelText('Checkbox 1')).toBeInTheDocument();
    expect(getByLabelText('Checkbox 2')).toBeInTheDocument();
    expect(getByLabelText('Checkbox 3')).toBeInTheDocument();
  });

  test('does not allow user interaction when readOnly is true', () => {
    const handleClick = jest.fn();
    const { getByLabelText } = render(
      <CheckboxWithLabel label="Check me" onClick={handleClick} readOnly />,
    );
    const checkbox = getByLabelText('Check me') as HTMLInputElement;

    fireEvent.click(checkbox);
    fireEvent.change(checkbox, { target: { checked: false } });

    expect(checkbox.checked).toBe(false);
    expect(handleClick).not.toHaveBeenCalled();
  });
});
