import chevronDown from '#assets/svg/chevron-down.svg';
import closeIcon from '#assets/svg/close-icon-svg.svg';
import menuIcon from '#assets/svg/hamburger-icon.svg';
import leucineLogo from '#assets/svg/leucine-logo-white.svg';
import qrCodeIcon from '#assets/svg/qr-code-icon.svg';
import rightArrow from '#assets/svg/right-arrow-white.svg';
import settingsIcon from '#assets/svg/settings-icon.svg';
import { NestedSelect, Select } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import checkPermission from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { toggleIsDrawerOpen } from '#store/extras/action';
import { switchFacility } from '#store/facilities/actions';
import { logout, setSelectedUseCase } from '#views/Auth/actions';
import { getQrCodeData, qrCodeValidator } from '#views/Ontology/utils';
import { navigate, useLocation } from '@reach/router';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import { ImageWrapper } from '../../styles/ImageWrapper';
import { HeaderMenu, Wrapper } from './styles';
import { navigationOptions } from '#components/NavigationMenu';
import { ALL_FACILITY_ID } from '#utils/constants';

type FacilityOption = {
  label: string;
  value: string;
};

const Header: FC = () => {
  const location = useLocation();
  const dispatch = useDispatch();

  const {
    auth: { profile, facilities, selectedFacility, userId, selectedUseCase, useCases, ssoIdToken },
    extras: { isDrawerOpen },
  } = useTypedSelector((state) => state);

  const facilitiesOptions: FacilityOption[] = facilities.map((facility) => ({
    label: facility.name,
    value: facility.id,
  }));

  const onSelectWithQR = async (data: string) => {
    try {
      const qrData = await getQrCodeData({
        shortCode: data,
      });
      if (qrData) {
        await qrCodeValidator({
          data: qrData,
          callBack: () =>
            navigate(`/ontology/object-types/${qrData.objectTypeId}/objects/${qrData.objectId}`),
          objectTypeValidation: true,
        });
      }
    } catch (error) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
        }),
      );
    }
  };

  const DropdownIcon = () => <img src={chevronDown} />;

  return (
    <Wrapper className="header-bar">
      <div className="left-section">
        {selectedUseCase && !location.pathname.includes('/home') && (
          <div onClick={() => dispatch(toggleIsDrawerOpen())} style={{ cursor: 'pointer' }}>
            <img src={isDrawerOpen ? closeIcon : menuIcon} />
          </div>
        )}
        <ImageWrapper className="header-logo">
          <img src={leucineLogo} onClick={() => navigate('/')} />
        </ImageWrapper>
      </div>
      <div className="right-section">
        {selectedFacility && checkPermission(['header', 'usersAndAccess']) && (
          <div className="header-item">
            <NestedSelect
              id="system-settings-selector"
              items={{
                'system-settings': {
                  label: 'User and Access Settings',
                },
              }}
              onChildChange={() => {
                navigate('/users');
              }}
              label={() => (
                <HeaderMenu>
                  <img src={settingsIcon} />
                </HeaderMenu>
              )}
            />
          </div>
        )}
        {selectedUseCase && !location.pathname.includes('/home') && (
          <div
            className="header-item"
            onClick={() => {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.QR_SCANNER,
                  props: { onSuccess: onSelectWithQR },
                }),
              );
            }}
          >
            <Tooltip title={'Scan QR'} arrow>
              <img src={qrCodeIcon} />
            </Tooltip>
          </div>
        )}
        {selectedUseCase && !location.pathname.includes('/home') && (
          <Select
            options={useCases.filter((useCase) => useCase.enabled)}
            value={selectedUseCase}
            components={{ DropdownIndicator: DropdownIcon }}
            onChange={(option: any) => {
              dispatch(setSelectedUseCase(option));
              let navigateTo = 'inbox';
              Object.keys(navigationOptions).every((key) => {
                if (
                  checkPermission([
                    (selectedFacility?.id ?? '') === ALL_FACILITY_ID ? 'globalSidebar' : 'sidebar',
                    key,
                  ])
                ) {
                  navigateTo = key;
                  return false;
                }
                return true;
              });
              navigate(`/${navigateTo}`);
            }}
          />
        )}
        {selectedFacility ? (
          <Select
            options={facilitiesOptions}
            value={{
              label: selectedFacility.name,
              value: selectedFacility.id,
            }}
            components={{ DropdownIndicator: DropdownIcon }}
            onChange={(option: any) =>
              dispatch(
                switchFacility({
                  facilityId: option.value as string,
                  loggedInUserId: userId!,
                }),
              )
            }
          />
        ) : null}
        <div className="header-item">
          <NestedSelect
            id="user=profile"
            items={{
              'user-info': {
                label: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    <span>
                      {profile?.firstName} {profile?.lastName}
                    </span>
                    <span>ID {profile?.employeeId}</span>
                  </div>
                ),
              },
              ...(selectedFacility && {
                'my-account': {
                  label: 'My Account',
                },
              }),
              logout: {
                label: (
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <span>Logout</span>
                    <img src={rightArrow} />
                  </div>
                ),
              },
            }}
            onChildChange={(option: any) => {
              if (option.value === 'my-account') {
                navigate(`/users/profile/${profile?.id}`);
              } else if (option.value === 'logout') {
                dispatch(
                  logout({
                    ssoIdToken,
                  }),
                );
              }
            }}
            label={() => (
              <div style={{ display: 'flex', color: '#fff', alignItems: 'center', gap: '8px' }}>
                <span>
                  {profile?.firstName} {profile?.lastName}
                </span>
                {DropdownIcon()}
              </div>
            )}
          />
        </div>
      </div>
    </Wrapper>
  );
};

export default Header;
