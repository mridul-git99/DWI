import { ProcessLevelRoles } from '#services/uiPermissions';

enum PermissionCategories {
  VIEW_JOBS = 'View Jobs',
  CREATE_JOBS = 'Create Jobs',
  MANAGE_JOBS = 'Manage Jobs',
  EXECUTE_JOBS = 'Execute Jobs',
  REVIEW_JOBS = 'Review Jobs',
}

enum ViewJobFeatures {
  VIEW_JOBS_AND_JOB_PARAMETER = 'View Jobs and Job Parameter',
  PRINT_JOBS = 'Print Jobs',
  VIEW_SCHEDULERS = 'View Schedulers',
}

enum CreateJobFeatures {
  CREATE_JOBS = 'Create Jobs',
}

enum ManageJobFeatures {
  CREATE_AND_MODIFY_SCHEDULERS = 'Create and modify Schedulers',
  ASSIGN_JOBS = 'Assign Jobs',
  MANAGE_PLANNED_VARIATIONS = 'Manage Planned Variations',
  COMPLETE_JOB_WITH_EXCEPTION = 'Complete Job with Exception',
}

enum ExecuteJobFeatures {
  EXECUTE_JOBS = 'Execute Jobs',
}

enum ReviewJobFeatures {
  PERFORM_PEER_VARIFICATION_OF_PROCESS_PARAMETERS = 'Perform Peer Varification of Process Parameters',
  MANAGE_TASK_EXCEPTIONS = 'Manage Task Exceptions',
}

export const processRolesDetails = {
  [ProcessLevelRoles.JOB_EXECUTOR]: {
    name: 'Job Executor',
    permissions: {
      [PermissionCategories.VIEW_JOBS]: Object.values(ViewJobFeatures).map((v) => v),
      [PermissionCategories.EXECUTE_JOBS]: Object.values(ExecuteJobFeatures).map((v) => v),
    },
  },
  [ProcessLevelRoles.JOB_REVIEWER]: {
    name: 'Job Reviewer',
    permissions: {
      [PermissionCategories.VIEW_JOBS]: Object.values(ViewJobFeatures).map((v) => v),
      [PermissionCategories.REVIEW_JOBS]: Object.values(ReviewJobFeatures).map((v) => v),
    },
  },
  [ProcessLevelRoles.JOB_MANAGER]: {
    name: 'Job Manager',
    permissions: {
      [PermissionCategories.VIEW_JOBS]: Object.values(ViewJobFeatures).map((v) => v),
      [PermissionCategories.MANAGE_JOBS]: Object.values(ManageJobFeatures).map((v) => v),
    },
  },
  [ProcessLevelRoles.JOB_ISSUER]: {
    name: 'Job Issuer',
    permissions: {
      [PermissionCategories.VIEW_JOBS]: Object.values(ViewJobFeatures).map((v) => v),
      [PermissionCategories.CREATE_JOBS]: Object.values(CreateJobFeatures).map((v) => v),
    },
  },
};
