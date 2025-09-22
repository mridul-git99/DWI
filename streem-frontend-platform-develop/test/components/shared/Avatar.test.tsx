import { Avatar, AvatarExtras } from '#components';
import React from 'react';
import { render, fireEvent } from 'test-utils';
import { User } from '#store/users/types';

const mockDispatch = jest.fn();

describe('Avatar Component', () => {
  const user = {
    id: 1,
    firstName: 'John',
    lastName: 'Doe',
    employeeId: '12345',
  } as unknown as User;

  afterEach(() => {
    mockDispatch.mockClear();
  });

  test('renders initials correctly', () => {
    const { getByText } = render(<Avatar user={user} />);
    const initials = getByText('JD');
    expect(initials).toBeInTheDocument();
  });

  test('dispatches openOverlayAction on mouse enter', () => {
    const { getByTestId } = render(<Avatar user={user} />, { mockDispatch });
    const avatar = getByTestId('avatar-wrapper');
    fireEvent.mouseEnter(avatar);
    expect(mockDispatch).toHaveBeenCalledTimes(1);
    // Check the dispatched action if needed
    // expect(mockDispatch).toHaveBeenCalledWith(/* expected action */);
  });

  test('dispatches closeOverlayAction on mouse leave', () => {
    const { getByTestId } = render(<Avatar user={user} />, { mockDispatch });
    const avatar = getByTestId('avatar-wrapper');
    fireEvent.mouseLeave(avatar);

    expect(mockDispatch).toHaveBeenCalledTimes(1);
    // Check the dispatched action if needed
    // expect(mockDispatch).toHaveBeenCalledWith(/* expected action */);
  });
});

describe('AvatarExtras Component', () => {
  const users = [
    {
      id: 1,
      firstName: 'John',
      lastName: 'Doe',
      employeeId: '12345',
    },
    {
      id: 2,
      firstName: 'Jane',
      lastName: 'Smith',
      employeeId: '54321',
    },
  ] as unknown as User[];

  test('renders user count correctly', () => {
    const { getByText } = render(<AvatarExtras users={users} />);
    const count = getByText('+2');
    expect(count).toBeInTheDocument();
  });

  test('dispatches openOverlayAction on mouse enter', () => {
    const { getByTestId } = render(<AvatarExtras users={users} />, { mockDispatch });
    const avatar = getByTestId('avatar-extras-wrapper');
    fireEvent.mouseEnter(avatar);

    expect(mockDispatch).toHaveBeenCalledTimes(1);
    // Check the dispatched action if needed
    // expect(mockDispatch).toHaveBeenCalledWith(/* expected action */);
  });
});
