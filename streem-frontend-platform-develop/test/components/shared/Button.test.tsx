import { Button } from '#components';
import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import 'jest-styled-components';

describe('Button component', () => {
  it('renders button with default props', () => {
    const { getByText } = render(<Button>Default Button</Button>);
    const buttonElement = getByText('Default Button');
    expect(buttonElement).toBeInTheDocument();
    expect(buttonElement.tagName).toBe('BUTTON');
    expect(buttonElement).toHaveStyle('background-color: #1d84ff');
    expect(buttonElement).toHaveStyle('color: #ffffff');
    expect(buttonElement).toMatchSnapshot();
    // Add more assertions for default props if needed
  });

  it('renders button with specified color and variant', () => {
    const { getByText } = render(
      <Button color="green" variant="secondary">
        Secondary Green Button
      </Button>,
    );
    const buttonElement = getByText('Secondary Green Button');
    expect(buttonElement).toBeInTheDocument();
    expect(buttonElement).toHaveStyle('background-color: #ffffff');
    expect(buttonElement).toHaveStyle('border-color: #5aa700');
    expect(buttonElement).toHaveStyle('color: #5aa700');
    expect(buttonElement).toMatchSnapshot();
    // Add more assertions for the specified color and variant
  });

  it('handles click event', () => {
    const handleClick = jest.fn();
    const { getByText } = render(<Button onClick={handleClick}>Click Me</Button>);
    const buttonElement = getByText('Click Me');
    fireEvent.click(buttonElement);
    expect(handleClick).toHaveBeenCalledTimes(1);
    expect(buttonElement).toMatchSnapshot();
    // Add more assertions for the click event if needed
  });

  it('renders text-only button with transparent background', () => {
    const { getByText } = render(
      <Button variant="textOnly" color="red">
        Text-only Button
      </Button>,
    );
    const buttonElement = getByText('Text-only Button');
    expect(buttonElement).toBeInTheDocument();
    expect(buttonElement).toHaveStyle('background-color: transparent');
    expect(buttonElement).toHaveStyle('color: #ff6b6b');
    expect(buttonElement).toMatchSnapshot();
    // Add more assertions for the text-only variant if needed
  });

  it('renders disabled secondary button', () => {
    const { getByText } = render(
      <Button disabled variant="secondary">
        Disabled Button
      </Button>,
    );
    const buttonElement = getByText('Disabled Button');
    expect(buttonElement).toBeInTheDocument();
    expect(buttonElement).toBeDisabled();
    expect(buttonElement).toHaveStyle('background-color: #fff');
    expect(buttonElement).toHaveStyle('color: #bbbbbb');
    expect(buttonElement).toMatchSnapshot();
    // Add more assertions for the disabled secondary button if needed
  });
});
