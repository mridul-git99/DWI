import { Checklist } from '#PrototypeComposer/checklist.types';
import { actionSpreader } from '#store/helpers';
import { fetchJobsType, fetchJobsSuccessType, ListViewAction, Job } from './types';

export const fetchJobs = (params: fetchJobsType) =>
  actionSpreader(ListViewAction.FETCH_JOBS, { params });

export const fetchJobsOngoing = () => actionSpreader(ListViewAction.FETCH_JOBS_ONGOING);

export const fetchJobsSuccess = ({ data, pageable }: fetchJobsSuccessType) =>
  actionSpreader(ListViewAction.FETCH_JOBS_SUCCESS, { data, pageable });

export const fetchJobsError = (error: any) =>
  actionSpreader(ListViewAction.FETCH_JOBS_ERROR, { error });

export const createJob = (params: {
  parameterValues: Record<string, any>;
  checklistId: Checklist['id'];
  selectedUseCaseId: string;
  validateUserRole: boolean;
}) => actionSpreader(ListViewAction.CREATE_JOB, params);

export const createJobSuccess = (data: any, shouldReRender = true) =>
  actionSpreader(ListViewAction.CREATE_JOB_SUCCESS, { data, shouldReRender });

export const createJobError = () => actionSpreader(ListViewAction.CREATE_OR_UPDATE_JOB_ERROR);

export const updateJob = (payload: { job: Partial<Job> }) =>
  actionSpreader(ListViewAction.UPDATE_JOB, payload);

export const updateJobSuccess = (data: Job, shouldReRender = true) =>
  actionSpreader(ListViewAction.UPDATE_JOB_SUCCESS, { data, shouldReRender });

export const fetchJobsExcel = (params: any) =>
  actionSpreader(ListViewAction.FETCH_JOBS_EXCEL, {
    params,
  });

export const fetchJobsExcelError = (error: any) =>
  actionSpreader(ListViewAction.FETCH_JOBS_EXCEL_ERROR, { error });
