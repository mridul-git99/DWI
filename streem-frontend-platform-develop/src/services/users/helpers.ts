import { omitBy } from 'lodash';
import { User } from './types';

type getUserNameArgs = {
  user: Pick<User, 'employeeId' | 'firstName' | 'lastName'>;
  withEmployeeId?: boolean;
};

export const getUserName = ({ user, withEmployeeId = false }: getUserNameArgs) => {
  const { firstName = '', lastName = '', employeeId } = omitBy(user, (v) => !v);
  return `${firstName} ${lastName}${withEmployeeId ? ` (ID: ${employeeId})` : ''}`;
};
