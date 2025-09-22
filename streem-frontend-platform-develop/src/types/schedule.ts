export interface ScheduleCalendar {
  id?: string;
  isLoading: boolean;
  errors: Record<string, any>;
  list: Record<any, any>;
}
