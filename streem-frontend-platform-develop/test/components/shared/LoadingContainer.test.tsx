import { LoadingContainer } from '#components';
import { render } from '@testing-library/react';
import React from 'react';

describe('LoadingContainer', () => {
  test('renders component when loading is false', () => {
    const { getByTestId } = render(
      <LoadingContainer loading={false} component={<div data-testid="content">Hello World</div>} />,
    );
    const contentElement = getByTestId('content');
    expect(contentElement).toBeInTheDocument();
    expect(contentElement.textContent).toBe('Hello World');
  });

  test('renders CircularProgress when loading is true', () => {
    const { getByTestId } = render(<LoadingContainer loading={true} />);
    const progressElement = getByTestId('loading-container-progress');
    expect(progressElement).toBeInTheDocument();
  });

  test('sets the color of CircularProgress correctly', () => {
    const { getByTestId } = render(<LoadingContainer loading={true} />);
    const progressElement = getByTestId('loading-container-progress');
    expect(progressElement).toHaveStyle('color: rgb(29, 132, 255)');
  });

  test('renders null when loading is false and no component is provided', () => {
    const { container } = render(<LoadingContainer loading={false} />);
    expect(container.firstChild).toBeNull();
  });
});
