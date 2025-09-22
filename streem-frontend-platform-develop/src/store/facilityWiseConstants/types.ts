import { setFacilityTimeStampFormat, setInitialFacilityWiseConstants } from './actions';

export type FaciltyWiseConstantsState = {
  [facilityId: string]: {
    timeFormat: string;
    dateFormat: string;
    dateAndTimeStampFormat: string;
    timeZone: string;
  };
};

export enum FacilityWiseConstantsAction {
  SET_INITIAL_FACILITY_WISE_CONSTANTS = '@@facilityWiseConst/SET_INITIAL_FACILITY_WISE_CONSTANTS',
  SET_FACILITY_TIMESTAMP = '@@facilityWiseConst/SET_FACILITY_TIMESTAMP',
}

export type FacilityWiseConstantsActionType = ReturnType<
  typeof setFacilityTimeStampFormat | typeof setInitialFacilityWiseConstants
>;
