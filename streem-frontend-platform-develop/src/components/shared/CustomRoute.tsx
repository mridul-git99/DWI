import checkPermission from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { setGlobalError } from '#store/extras/action';
import { logout } from '#views/Auth/actions';
import { Redirect, RouteComponentProps } from '@reach/router';
import React, { FC, useEffect } from 'react';
import { useDispatch } from 'react-redux';

type Props = RouteComponentProps & {
  as: FC | FC<RouteComponentProps<{ id: string }>>;
  isProtected?: boolean;
};

export const CustomRoute: FC<Props> = ({ as: Component, isProtected = true, ...props }) => {
  const {
    isLoggedIn,
    selectedFacility,
    hasSetChallengeQuestion,
    token,
    selectedUseCase,
    ssoIdToken,
  } = useTypedSelector((state) => state.auth);

  const dispatch = useDispatch();

  const { location } = props;

  const { hasGlobalError } = useTypedSelector((state) => state.extras);

  useEffect(() => {
    if (hasGlobalError) {
      dispatch(setGlobalError(false));
    }
  }, [location?.pathname]);

  if (isLoggedIn) {
    if (!hasSetChallengeQuestion) {
      if (location?.pathname !== '/auth/register/recovery') {
        if (token) return <Redirect from="" to="/auth/register/recovery" noThrow />;
        dispatch(logout({ ssoIdToken: ssoIdToken }));
      }
    } else if (location?.search?.includes('?link=')) {
      if (props.path === 'media') {
        return <Component {...props} />;
      } else {
        return <Redirect from="" to="/media" noThrow />;
      }
    } else {
      const isFacilitySelectionPage = props.path === 'facility/selection';
      if (selectedFacility || isFacilitySelectionPage) {
        const userCanAccessHomePage = checkPermission(['home']);
        if (
          isFacilitySelectionPage ||
          location?.pathname.includes('/users') ||
          location?.pathname.includes('/sso') ||
          (props.path === 'home' && userCanAccessHomePage) ||
          (selectedUseCase && isProtected && location?.pathname !== '/')
        ) {
          //sso path is included as it is getting redirected to home page.
          return <Component {...props} />;
        } else if (userCanAccessHomePage) {
          return <Redirect from="" to="/home" noThrow />;
        } else {
          return <Redirect from="" to="/users" noThrow />;
        }
      }

      return <Redirect from="" to="/facility/selection" noThrow />;
    }
  }

  return !isProtected ? <Component {...props} /> : <Redirect from="" to="/auth/login" noThrow />;
};
