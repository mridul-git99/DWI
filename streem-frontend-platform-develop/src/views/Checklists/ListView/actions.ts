import { actionSpreader } from '#store/helpers';
import { Error, ResponseObj } from '#utils/globalTypes';
import { AxiosRequestConfig } from 'axios';
import { Checklist } from '../types';
import { ListViewAction } from './types';

export const fetchChecklists = (params: Record<string, string | number>, enableLoading: boolean) =>
  actionSpreader(ListViewAction.FETCH_CHECKLISTS, {
    params,
    enableLoading,
  });

export const fetchChecklistsForListView = (params: Record<string, string | number>) =>
  actionSpreader(ListViewAction.FETCH_CHECKLISTS_FOR_LISTVIEW, {
    params,
    enableLoading: true,
  });

export const clearData = () => actionSpreader(ListViewAction.CLEAR_DATA);

export const fetchChecklistsOngoing = () => actionSpreader(ListViewAction.FETCH_CHECKLISTS_ONGOING);

export const fetchChecklistsSuccess = ({ data, pageable }: Partial<ResponseObj<Checklist[]>>) =>
  actionSpreader(ListViewAction.FETCH_CHECKLISTS_SUCCESS, {
    data,
    pageable,
  });

export const fetchChecklistsError = (error: any) =>
  actionSpreader(ListViewAction.FETCH_CHECKLISTS_ERROR, { error });

export const archiveChecklist = (
  id: Checklist['id'],
  reason: string,
  setFormErrors: (errors?: Error[]) => void,
) =>
  actionSpreader(ListViewAction.ARCHIVE, {
    id,
    reason,
    setFormErrors,
  });

export const unarchiveChecklist = (
  id: Checklist['id'],
  reason: string,
  setFormErrors: (errors?: Error[]) => void,
) =>
  actionSpreader(ListViewAction.UNARCHIVE, {
    id,
    reason,
    setFormErrors,
  });

export const handlePublishedArchive = (id: Checklist['id']) =>
  actionSpreader(ListViewAction.HANDLE_PUBLISHED_ARCHIVE, { id });

export const updateList = (id: Checklist['id']) =>
  actionSpreader(ListViewAction.UPDATE_LIST, { id });

export const fetchProcessLogs = (params: any) =>
  actionSpreader(ListViewAction.FETCH_PROCESS_LOGS, params);

export const fetchProcessLogsError = (error?: any) =>
  actionSpreader(ListViewAction.FETCH_PROCESS_LOGS_ERROR, { error });

export const fetchProcessLogsSuccess = ({
  data,
  pageable,
  processName,
}: Partial<ResponseObj<any[]>> & { processName: string }) =>
  actionSpreader(ListViewAction.FETCH_PROCESS_LOGS_SUCCESS, {
    data,
    pageable,
    processName,
  });

export const addCustomView = ({
  data,
  checklistId,
  setActiveTab,
}: {
  data: any;
  checklistId?: string;
  setActiveTab: any;
}) => actionSpreader(ListViewAction.ADD_CUSTOM_VIEW, { data, checklistId, setActiveTab });

export const addCustomViewError = (error?: any) =>
  actionSpreader(ListViewAction.ADD_CUSTOM_VIEW_ERROR, { error });

export const addCustomViewSuccess = (data: any) =>
  actionSpreader(ListViewAction.ADD_CUSTOM_VIEW_SUCCESS, {
    data,
  });

export const getCustomViews = (params: AxiosRequestConfig['params'] = {}) =>
  actionSpreader(ListViewAction.GET_CUSTOM_VIEWS, { params });

export const getCustomViewsError = (error?: any) =>
  actionSpreader(ListViewAction.GET_CUSTOM_VIEWS_ERROR, { error });

export const getCustomViewsSuccess = (data: any) =>
  actionSpreader(ListViewAction.GET_CUSTOM_VIEWS_SUCCESS, {
    data,
  });

export const saveCustomView = ({ data, viewId }: { data: any; viewId: string }) =>
  actionSpreader(ListViewAction.SAVE_CUSTOM_VIEW, { data, viewId });

export const saveCustomViewError = (error?: any) =>
  actionSpreader(ListViewAction.SAVE_CUSTOM_VIEW_ERROR, { error });

export const saveCustomViewSuccess = (data: any) =>
  actionSpreader(ListViewAction.SAVE_CUSTOM_VIEW_SUCCESS, {
    data,
  });

export const deleteCustomView = ({ view, tabIndex }: { view: any; tabIndex: number }) =>
  actionSpreader(ListViewAction.DELETE_CUSTOM_VIEW, { view, tabIndex });

export const deleteCustomViewError = (error?: any) =>
  actionSpreader(ListViewAction.DELETE_CUSTOM_VIEW_ERROR, { error });

export const deleteCustomViewSuccess = (data: any) =>
  actionSpreader(ListViewAction.DELETE_CUSTOM_VIEW_SUCCESS, {
    data,
  });

export const exportChecklist = (params: Record<string, string>) =>
  actionSpreader(ListViewAction.EXPORT_CHECKLIST, params);
