import { RoleIdByName } from '#services/uiPermissions';

enum PermissionCategories {
  CHECKLIST_FEATURES = 'Processing Features',
  JOB_FEATURES = 'Job Features',
  ADMINISTRATIVE_FEATURES = 'Administrative Features',
  ONTOLOGY_FEATURES = 'Ontology',
  GLOBAL_PORTAL_FEATURES = 'Global Portal',
}

enum ChecklistFeatures {
  CREATE_AND_REVISE_UNIT_LEVEL_CHECKLISTS = 'Create and Revise Unit-Level Processes',
  VIEW_EXISTING_UNIT_LEVEL_CHECKLISTS = 'View existing Unit-Level Processes',
  VIEW_UNIT_LEVEL_PROTOTYPES = 'View Unit-Level Prototypes',
  ARCHIVE_UNIT_LEVEL_PROTOTYPES = 'Archive Unit-Level Prototypes',
  REVIEW_AND_APPROVE_UNIT_LEVEL_CHECKLISTS = 'Review and Approve Unit-Level Processes',
  RELEASE_UNIT_LEVEL_CHECKLISTS = 'Publish Unit-Level Processes',
  ARCHIVE_UNIT_LEVEL_CHECKLISTS = 'Archive Unit-Level Processes',
  RECALL_CHECKLISTS = 'Recall a Process',
  MANAGE_TRAINED_USERS = 'Manage Trained Users of Unit Level Processes',
}

enum JobFeatures {
  CREATE_JOBS = 'Create Jobs',
  ASSIGN_JOBS = 'Assign Jobs',
  PEER_VERIFICATION = 'Perform peer verification of process parameters',
  CREATE_AND_MODIFY_SCHEDULERS = 'Create and modify Schedulers of a Process',
  EXECUTE_AND_COMPLETE_JOBS = 'Execute and Complete Jobs',
  PRINT_JOBS = 'Print Jobs',
  VIEW_JOBS_AND_JOB_PARAMETER = 'View Jobs and Job Parameter',
  COMPLETE_JOB_WITH_EXCEPTION = 'Complete a Job with Exception',
  MANAGE_TASK_EXCEPTIONS = 'Manage Task Exceptions',
  MANAGE_JOB_ANNOTATIONS = 'Manage Job Annotations',
  DOWNLOAD_PROCESS_TEMPLATES = 'Download Process Templates',
  DOWNLOAD_JOB_LISTS = 'Download Job Lists',
}

enum AdministrativeFeatures {
  MANAGE_USERS_AND_PERMISSIONS = 'Manage Users and Permissions',
  VIEW_EXISTING_USERS = 'View existing Users',
  MANAGE_FACILITY_ACCESS = 'Manage Facility Access',
  VIEW_USER_SESSION_ACTIVITY = 'View User’s Session Activity',
  MANAGE_USER_GROUPS = 'Manage User Groups',
}

enum OntologyFeatures {
  VIEW_OBJECT_TYPES = 'View Object Types',
  CREATE_AND_UPDATE_OBJECTS = 'Create and Update Objects',
  VIEW_OBJECTS = 'View Objects',
  CREATE_OBJECT_TYPES = 'Create Object Types',
  MODIFY_OBJECT_TYPES = 'Modify Object Types',
  BULK_IMPORT_EXPORT_OBJECTS = 'Bulk Import/Export of Objects',
}

enum GlobalPortalFeatures {
  ACCESS_GLOBAL_PORTAL = 'Access Global Portal',
  CREATE_GLOBAL_CHECKLISTS = 'Create Global Processes',
  VIEW_GLOBAL_CHECKLISTS = 'View Global Processes',
  SHARE_GLOBAL_CHECKLISTS_WITH_UNITS = 'Share Global Processes with Units',
  REVIEW_AND_APPROVE_GLOBAL_CHECKLISTS = 'Review and Approve Global Processes',
  RELEASE_GLOBAL_CHECKLISTS = 'Release Global Processes',
  REVISE_AND_ARCHIVE_GLOBAL_CHECKLISTS = 'Revise and Archive Global Processes',
  RECALL_GLOBAL_CHECKLISTS = 'Recall a Global Process',
}

export const rolesDetails = {
  [RoleIdByName.ACCOUNT_OWNER]: {
    name: 'Account Owner',
    permissions: {
      [PermissionCategories.CHECKLIST_FEATURES]: Object.values(ChecklistFeatures).map((v) => v),
      [PermissionCategories.JOB_FEATURES]: Object.values(JobFeatures).map((v) => v),
      [PermissionCategories.ADMINISTRATIVE_FEATURES]: Object.values(AdministrativeFeatures).map(
        (v) => v,
      ),
      [PermissionCategories.ONTOLOGY_FEATURES]: Object.values(OntologyFeatures).map((v) => v),
      [PermissionCategories.GLOBAL_PORTAL_FEATURES]: Object.values(GlobalPortalFeatures).map(
        (v) => v,
      ),
    },
  },
  [RoleIdByName.FACILITY_ADMIN]: {
    name: 'Facility Admin',
    permissions: {
      [PermissionCategories.CHECKLIST_FEATURES]: [
        ChecklistFeatures.CREATE_AND_REVISE_UNIT_LEVEL_CHECKLISTS,
        ChecklistFeatures.VIEW_EXISTING_UNIT_LEVEL_CHECKLISTS,
        ChecklistFeatures.VIEW_UNIT_LEVEL_PROTOTYPES,
        ChecklistFeatures.REVIEW_AND_APPROVE_UNIT_LEVEL_CHECKLISTS,
        ChecklistFeatures.ARCHIVE_UNIT_LEVEL_PROTOTYPES,
        ChecklistFeatures.RECALL_CHECKLISTS,
        ChecklistFeatures.MANAGE_TRAINED_USERS,
      ],
      [PermissionCategories.JOB_FEATURES]: Object.values(JobFeatures).map((v) => v),
      [PermissionCategories.ADMINISTRATIVE_FEATURES]: [
        AdministrativeFeatures.VIEW_EXISTING_USERS,
        AdministrativeFeatures.VIEW_USER_SESSION_ACTIVITY,
        AdministrativeFeatures.MANAGE_USER_GROUPS,
      ],
      [PermissionCategories.ONTOLOGY_FEATURES]: [
        OntologyFeatures.VIEW_OBJECT_TYPES,
        OntologyFeatures.CREATE_AND_UPDATE_OBJECTS,
        OntologyFeatures.VIEW_OBJECTS,
        OntologyFeatures.BULK_IMPORT_EXPORT_OBJECTS,
      ],
    },
  },
  [RoleIdByName.CHECKLIST_PUBLISHER]: {
    name: 'Process Publisher',
    permissions: {
      [PermissionCategories.CHECKLIST_FEATURES]: Object.values(ChecklistFeatures).map((v) => v),
      [PermissionCategories.JOB_FEATURES]: Object.values(JobFeatures).map((v) => v),
      [PermissionCategories.ADMINISTRATIVE_FEATURES]: [
        AdministrativeFeatures.VIEW_EXISTING_USERS,
        AdministrativeFeatures.VIEW_USER_SESSION_ACTIVITY,
        AdministrativeFeatures.MANAGE_USER_GROUPS,
      ],
      [PermissionCategories.ONTOLOGY_FEATURES]: [
        OntologyFeatures.VIEW_OBJECT_TYPES,
        OntologyFeatures.CREATE_AND_UPDATE_OBJECTS,
        OntologyFeatures.VIEW_OBJECTS,
        OntologyFeatures.BULK_IMPORT_EXPORT_OBJECTS,
      ],
    },
  },
  [RoleIdByName.SYSTEM_ADMIN]: {
    name: 'System Admin',
    permissions: {
      [PermissionCategories.ADMINISTRATIVE_FEATURES]: Object.values(AdministrativeFeatures).map(
        (v) => v,
      ),
    },
  },
  [RoleIdByName.SUPERVISOR]: {
    name: 'Supervisor',
    permissions: {
      [PermissionCategories.CHECKLIST_FEATURES]: [
        ChecklistFeatures.VIEW_EXISTING_UNIT_LEVEL_CHECKLISTS,
        ChecklistFeatures.VIEW_UNIT_LEVEL_PROTOTYPES,
        ChecklistFeatures.REVIEW_AND_APPROVE_UNIT_LEVEL_CHECKLISTS,
        ChecklistFeatures.MANAGE_TRAINED_USERS,
      ],
      [PermissionCategories.JOB_FEATURES]: Object.values(JobFeatures).map((v) => v),
      [PermissionCategories.ONTOLOGY_FEATURES]: [
        OntologyFeatures.VIEW_OBJECT_TYPES,
        OntologyFeatures.VIEW_OBJECTS,
      ],
    },
  },
  [RoleIdByName.OPERATOR]: {
    name: 'Operator',
    permissions: {
      [PermissionCategories.CHECKLIST_FEATURES]: [
        ChecklistFeatures.VIEW_EXISTING_UNIT_LEVEL_CHECKLISTS,
      ],
      [PermissionCategories.JOB_FEATURES]: [
        JobFeatures.CREATE_JOBS,
        JobFeatures.EXECUTE_AND_COMPLETE_JOBS,
        JobFeatures.PRINT_JOBS,
        JobFeatures.VIEW_JOBS_AND_JOB_PARAMETER,
        JobFeatures.DOWNLOAD_PROCESS_TEMPLATES,
      ],
      [PermissionCategories.ONTOLOGY_FEATURES]: [
        OntologyFeatures.VIEW_OBJECT_TYPES,
        OntologyFeatures.VIEW_OBJECTS,
      ],
    },
  },
  [RoleIdByName.GLOBAL_ADMIN]: {
    name: 'Global Admin',
    permissions: {
      [PermissionCategories.CHECKLIST_FEATURES]: [
        ChecklistFeatures.VIEW_EXISTING_UNIT_LEVEL_CHECKLISTS,
        ChecklistFeatures.VIEW_UNIT_LEVEL_PROTOTYPES,
      ],
      [PermissionCategories.JOB_FEATURES]: [
        JobFeatures.PRINT_JOBS,
        JobFeatures.VIEW_JOBS_AND_JOB_PARAMETER,
        JobFeatures.DOWNLOAD_PROCESS_TEMPLATES,
        JobFeatures.DOWNLOAD_JOB_LISTS,
      ],
      [PermissionCategories.ADMINISTRATIVE_FEATURES]: [
        AdministrativeFeatures.VIEW_EXISTING_USERS,
        AdministrativeFeatures.VIEW_USER_SESSION_ACTIVITY,
        AdministrativeFeatures.MANAGE_USER_GROUPS,
      ],
      [PermissionCategories.ONTOLOGY_FEATURES]: [
        OntologyFeatures.VIEW_OBJECT_TYPES,
        OntologyFeatures.VIEW_OBJECTS,
        OntologyFeatures.CREATE_OBJECT_TYPES,
        OntologyFeatures.MODIFY_OBJECT_TYPES,
      ],
      [PermissionCategories.GLOBAL_PORTAL_FEATURES]: Object.values(GlobalPortalFeatures).map(
        (v) => v,
      ),
    },
  },
};
