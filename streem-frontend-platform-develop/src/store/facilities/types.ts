import { Facility } from '#services/commonTypes';

import {
  fetchFacilities,
  fetchFacilitiesError,
  fetchFacilitiesOngoing,
  fetchFacilitiesSuccess,
  switchFacilityError,
  switchFacilitySuccess,
} from './actions';

export type Facilities = Facility[];

export interface FacilitiesState {
  readonly list: Facilities | undefined;
  readonly loading: boolean;
  readonly error?: string;
}

export enum FacilitiesAction {
  FETCH_FACILITIES = '@@facilities/FETCH_FACILITIES',
  FETCH_FACILITIES_ERROR = '@@facilities/FETCH_FACILITIES_ERROR',
  FETCH_FACILITIES_ONGOING = '@@facilities/FETCH_FACILITIES_ONGOING',
  FETCH_FACILITIES_SUCCESS = '@@facilities/FETCH_FACILITIES_SUCCESS',
  SWITCH_FACILITY = '@@facilities/SWITCH_FACILITY',
  SWITCH_FACILITY_SUCCESS = '@@facilities/SWITCH_FACILITY_SUCCESS',
  SWITCH_FACILITY_ERROR = '@@facilities/SWITCH_FACILITY_ERROR',
}

export type FacilitiesActionType = ReturnType<
  | typeof fetchFacilities
  | typeof fetchFacilitiesError
  | typeof fetchFacilitiesOngoing
  | typeof fetchFacilitiesSuccess
  | typeof switchFacilityError
  | typeof switchFacilitySuccess
>;
