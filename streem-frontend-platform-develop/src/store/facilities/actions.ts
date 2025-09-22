import { actionSpreader } from '../helpers';
import { Facilities, FacilitiesAction } from './types';
import { Facility } from '#services/commonTypes';

export const fetchFacilities = (params?: Record<string, string | number>) =>
  actionSpreader(FacilitiesAction.FETCH_FACILITIES, { params });

export const fetchFacilitiesOngoing = () =>
  actionSpreader(FacilitiesAction.FETCH_FACILITIES_ONGOING);

export const fetchFacilitiesSuccess = (facilities: Facilities) =>
  actionSpreader(FacilitiesAction.FETCH_FACILITIES_SUCCESS, {
    facilities,
  });

export const fetchFacilitiesError = (error: string) =>
  actionSpreader(FacilitiesAction.FETCH_FACILITIES_ERROR, { error });

export const switchFacility = ({
  facilityId,
  loggedInUserId,
}: {
  facilityId: Facility['id'];
  loggedInUserId: string;
}) =>
  actionSpreader(FacilitiesAction.SWITCH_FACILITY, {
    facilityId,
    loggedInUserId,
  });

export const switchFacilitySuccess = (accessToken: string, facilityId: Facility['id']) =>
  actionSpreader(FacilitiesAction.SWITCH_FACILITY_SUCCESS, {
    accessToken,
    facilityId,
  });

export const switchFacilityError = (error: any) =>
  actionSpreader(FacilitiesAction.SWITCH_FACILITY_ERROR, { error });
