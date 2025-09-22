import React from 'react';
import { render, fireEvent, waitFor, getByRole } from '@testing-library/react';
import { BaseModal, Button } from '#components';
import { Close } from '@material-ui/icons';

describe('BaseModal', () => {
  const defaultProps = {
    closeAllModals: jest.fn(),
    closeModal: jest.fn(),
    onPrimary: jest.fn(),
    onSecondary: jest.fn(),
    children: <div>Modal Content</div>,
    modalFooterOptions: <div>Footer Options</div>,
    title: 'Modal Title',
    primaryText: 'Primary Button',
    secondaryText: 'Secondary Button',
    showHeader: true,
    showFooter: true,
    showPrimary: true,
    showSecondary: true,
    isRound: false,
    animated: true,
    disabledPrimary: false,
    allowCloseOnOutsideClick: true,
    showCloseIcon: true,
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  test('renders the modal content', () => {
    const { getByText } = render(<BaseModal {...defaultProps} />);
    const modalContent = getByText('Modal Content');
    expect(modalContent).toBeInTheDocument();
  });

  test('renders the modal title', () => {
    const { getByText } = render(<BaseModal {...defaultProps} />);
    const modalTitle = getByText('Modal Title');
    expect(modalTitle).toBeInTheDocument();
  });

  test('renders the primary and secondary buttons', () => {
    const { getByText } = render(<BaseModal {...defaultProps} />);
    const primaryButton = getByText('Primary Button');
    const secondaryButton = getByText('Secondary Button');
    expect(primaryButton).toBeInTheDocument();
    expect(secondaryButton).toBeInTheDocument();
  });

  test('calls the onPrimary callback when the primary button is clicked', async () => {
    const onPrimaryMock = jest.fn();
    const { getByRole } = render(
      <BaseModal {...defaultProps} onPrimary={onPrimaryMock} primaryText="Primary Button" />,
    );
    const primaryButton = getByRole('button', { name: 'Primary Button' });
    fireEvent.click(primaryButton);
    await waitFor(() => {
      expect(onPrimaryMock).toHaveBeenCalledTimes(1);
    });
  });

  test('calls the onSecondary callback when the secondary button is clicked', async () => {
    const onSecondaryMock = jest.fn();
    const { getByRole } = render(<BaseModal {...defaultProps} onSecondary={onSecondaryMock} />);
    const secondaryButton = getByRole('button', { name: 'Secondary Button' });
    fireEvent.click(secondaryButton);
    await waitFor(() => {
      expect(onSecondaryMock).toHaveBeenCalledTimes(1);
    });
  });

  test('calls the closeModal callback when the close icon is clicked', async () => {
    const { getByTestId } = render(<BaseModal {...defaultProps} />);
    const closeIcon = getByTestId('base-modal-close-icon');
    fireEvent.click(closeIcon);
    await waitFor(() => {
      expect(defaultProps.closeModal).toHaveBeenCalledTimes(1);
    });
  });

  test('does not render the modal header when showHeader is false', () => {
    const { queryByText } = render(<BaseModal {...defaultProps} showHeader={false} />);
    const modalTitle = queryByText('Modal Title');
    expect(modalTitle).toBeNull();
  });

  test('does not render the modal footer when showFooter is false', () => {
    const { queryByText } = render(<BaseModal {...defaultProps} showFooter={false} />);
    const primaryButton = queryByText('Primary Button');
    const secondaryButton = queryByText('Secondary Button');
    expect(primaryButton).toBeNull();
    expect(secondaryButton).toBeNull();
  });

  test('does not render the close icon when showCloseIcon is false', () => {
    const { queryByTestId } = render(<BaseModal {...defaultProps} showCloseIcon={false} />);
    const closeIcon = queryByTestId('base-modal-close-icon');
    expect(closeIcon).toBeNull();
  });

  test('closes the modal when clicking outside if allowCloseOnOutsideClick is true', () => {
    const { getByRole } = render(<BaseModal {...defaultProps} />);
    const escapeOverlay = getByRole('presentation');
    fireEvent.click(escapeOverlay);
    expect(defaultProps.closeModal).toHaveBeenCalledTimes(1);
  });
});
