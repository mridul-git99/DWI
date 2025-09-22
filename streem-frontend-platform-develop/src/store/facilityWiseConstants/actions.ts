import { Facility } from '#services/commonTypes';
import { actionSpreader } from '#store/helpers';
import { FacilityWiseConstantsAction } from './types';

export const setInitialFacilityWiseConstants = (facilities: Facility[]) => {
  const initialFacilityWiseConstants = facilities.reduce(
    (data, facilityData) => ({
      ...data,
      [facilityData.id]: {
        timeFormat: facilityData.timeFormat || 'HH:mm:ss',
        dateFormat: facilityData.dateFormat || 'MMM dd, yyyy',
        dateAndTimeStampFormat: facilityData.dateTimeFormat || `MMM dd, yyyy HH:mm:ss`,
        timeZone: facilityData.timeZone || `UTC`,
      },
    }),
    {},
  );
  return actionSpreader(FacilityWiseConstantsAction.SET_INITIAL_FACILITY_WISE_CONSTANTS, {
    initialFacilityWiseConstants,
  });
};

export const setFacilityTimeStampFormat = (
  facilityId: string,
  dateFormat: string,
  timeFormat: string,
) =>
  actionSpreader(FacilityWiseConstantsAction.SET_FACILITY_TIMESTAMP, {
    facilityId,
    dateFormat,
    timeFormat,
  });
