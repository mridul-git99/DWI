import { OverlayContainerReducer } from '#components/OverlayContainer/reducer';
import { ComposerReducer as prototypeComposer } from '#PrototypeComposer/reducer';
import { AuthReducer } from '#views/Auth/reducer';
import { ChecklistListViewReducer } from '#views/Checklists/ListView/reducer';
import { InboxListViewReducer } from '#views/Inbox/ListView/reducer';
import { SessionActivityReducer } from '#views/UserAccess/ListView/SessionActivity/reducer';
import { combineReducers } from 'redux';
import { PropertiesReducer } from './properties/reducer';
import { UsersServiceReducer } from '../services/users/reducer';
import { NewJobListViewReducer } from '#views/Jobs/ListView/reducer';
import { AuditLogFiltersReducer } from './audit-log-filters/reducer';
import { FacilitiesReducer } from './facilities/reducer';
import { FileUploadReducer } from './file-upload/reducer';
import { UsersReducer } from './users/reducer';
import { ExtrasReducer } from './extras/reducer';
import { FacilityWiseConstantsReducer } from './facilityWiseConstants/reducer';
import { OntologyReducer } from '../views/Ontology/reducer';
import { ReportsListViewReducer } from '../views/Reports/ListView/reducer';
import { SchedulerReducer } from '#views/Checklists/Schedular/schedulerStore';
import { jobReducer } from '#views/Job/jobStore';
import { scheduleCalendarReducer } from '#views/ScheduleCalendar/scheduleStore';

export const rootReducer = combineReducers({
  auth: AuthReducer,
  checklistListView: ChecklistListViewReducer,
  facilities: FacilitiesReducer,
  fileUpload: FileUploadReducer,
  inboxListView: InboxListViewReducer,
  overlayContainer: OverlayContainerReducer,
  prototypeComposer,
  sessionActivity: SessionActivityReducer,
  users: UsersReducer,
  extras: ExtrasReducer,
  properties: PropertiesReducer,
  usersService: UsersServiceReducer,
  auditLogFilters: AuditLogFiltersReducer,
  jobListView: NewJobListViewReducer,
  facilityWiseConstants: FacilityWiseConstantsReducer,
  ontology: OntologyReducer,
  reports: ReportsListViewReducer,
  schedular: SchedulerReducer,
  job: jobReducer,
  schedule: scheduleCalendarReducer,
});
