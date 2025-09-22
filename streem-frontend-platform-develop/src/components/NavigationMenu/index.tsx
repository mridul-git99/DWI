import checkPermission, { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { navigate, useLocation } from '@reach/router';
import { toggleIsDrawerOpen } from '#store/extras/action';
import { ALL_FACILITY_ID } from '#utils/constants';
import { Link } from '@reach/router';
import React, { FC, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { Menu, NavItem, Wrapper } from './styles';
import { MenuItem } from './types';
import InboxIcon from '#assets/svg/InboxIcon';
import JobsIcon from '#assets/svg/JobsIcon';
import ProcessIcon from '#assets/svg/ProcessIcon';
import OntologyIcon from '#assets/svg/OntologyIcon';
import ReportsIcon from '#assets/svg/ReportsIcon';
import TodayIcon from '@material-ui/icons/Today';

export const navigationOptions = {
  inbox: {
    name: 'Inbox',
    icon: InboxIcon,
    menu: [
      { name: 'My Jobs', path: 'jobs' },
      { name: 'Verifications', path: 'verifications' },
      { name: 'Approvals', path: 'approvals' },
      { name: 'Corrections', path: 'corrections' },
    ],
  },
  jobs: { name: 'Jobs', icon: JobsIcon },
  checklists: { name: 'Processes', icon: ProcessIcon },
  schedule: { name: 'Schedule Calendar', icon: TodayIcon },
  ontology: { name: 'Ontology', icon: OntologyIcon },
  reports: { name: 'Reports', icon: ReportsIcon },
};

const NavigationMenu: FC = () => {
  const location = useLocation();
  const dispatch = useDispatch();
  const selectedFacility = useTypedSelector((state) => state.auth.selectedFacility);
  const isDrawerOpen = useTypedSelector((state) => state.extras.isDrawerOpen);

  const menuItems: MenuItem[] = useMemo(
    () =>
      Object.entries(navigationOptions).reduce<MenuItem[]>((acc, [key, value]) => {
        if (key === 'schedule') {
          acc.push({ ...value, path: `/${key}` });
        }
        if (
          checkPermission([
            selectedFacility?.id === ALL_FACILITY_ID ? 'globalSidebar' : 'sidebar',
            key,
          ])
        ) {
          if (key === 'reports') {
            if (isFeatureAllowed('metabaseReports')) {
              acc.push({ ...value, path: `/${key}` });
            } else if (isFeatureAllowed('quicksightReports')) {
              acc.push({ ...value, path: `/quicksight` });
            }
          } else {
            acc.push({ ...value, path: `/${key}` });
          }
        }
        return acc;
      }, []),
    [selectedFacility, navigationOptions],
  );

  return (
    <Wrapper className="navigation-menu">
      <Menu>
        {menuItems.map(({ path, name, icon: Icon, menu }, index) => {
          const isActive = location.pathname.split('/')[1] === path.split('/')[1];
          const menuOptionPath = location.pathname.split('/')[2];
          const isInboxMenuOption = ['jobs', 'verifications', 'approvals'].includes(menuOptionPath);
          return (
            <div key={`${name}-${index}`}>
              <Link
                to={
                  menu
                    ? isInboxMenuOption
                      ? `${path}/${menuOptionPath}`
                      : `${path}/${menu[0].path}`
                    : path
                }
                // key={`${name}-${index}`}
                getProps={() => {
                  return {
                    style: {
                      color: isActive ? '#1d84ff' : '#161616',
                      backgroundColor: isActive ? '#e7f1fd' : 'transparent',
                      textDecoration: 'none',
                      display: 'block',
                    },
                  };
                }}
                onClick={() => {
                  if (menu || isDrawerOpen) {
                    dispatch(toggleIsDrawerOpen());
                  }
                }}
              >
                <NavItem>
                  <Icon size={24} color={isActive ? '#1d84ff' : '#161616'} />
                  {name && <span>{name}</span>}
                </NavItem>
              </Link>
              {menu && isDrawerOpen && (
                <div className="nested-menu">
                  {menu.map((item, index) => {
                    const isActive = menuOptionPath === item.path;
                    if (item.name === 'Approvals' && !checkPermission(['approvals', 'view']))
                      return null;
                    return (
                      <div
                        key={`${item.name}-${index}`}
                        className={isActive ? 'active nested-menu-item' : 'nested-menu-item'}
                        onClick={() => {
                          navigate(`/inbox/${item.path}`);
                          dispatch(toggleIsDrawerOpen());
                        }}
                      >
                        {item.name}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </Menu>
    </Wrapper>
  );
};

export default NavigationMenu;
