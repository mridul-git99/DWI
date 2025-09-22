import { Facilities } from '#store/facilities/types';
import { User } from '#store/users/types';
import { RouteComponentProps } from '@reach/router';
import { Toggleables } from './helpers';

export enum PAGE_TYPE {
  EDIT = 'edit',
  ADD = 'add',
  PROFILE = 'profile',
  ADD_USER_GROUP = 'add_user_group',
}

export type ViewUserProps = RouteComponentProps<{
  id?: User['id'];
}> & { pageType: PAGE_TYPE };

export type EditUserProps = RouteComponentProps & {
  user: User;
  facilities: Facilities;
  isAccountOwner: boolean;
  isEditable: boolean;
  pageType: PAGE_TYPE;
};

export type EditUserRequestInputs = {
  firstName: string;
  lastName: string;
  employeeId: string;
  email: string | null;
  department: string;
  roles: string;
  facilities?: { id: string }[];
  username?: string;
  password?: string;
  confirmPassword?: string;
  userType?: string;
  reason?: string;
};

export type TogglesState = {
  [Toggleables.EDIT_PASSWORD]: boolean;
  [Toggleables.EDIT_QUESTIONS]: boolean;
};

export enum UserType {
  LOCAL = 'LOCAL',
  AZURE_AD = 'AZURE_AD',
  OKTA = 'OKTA',
}
