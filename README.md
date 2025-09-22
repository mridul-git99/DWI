# Comprehensive Schema Documentation - DWI Platform

## Table of Contents
1. [Overview](#overview)
2. [System Architecture Diagrams](#system-architecture-diagrams)
3. [Database Schema](#database-schema)
4. [Backend API Schema](#backend-api-schema)
5. [Frontend Data Models](#frontend-data-models)
6. [Configuration Schema](#configuration-schema)
7. [System Architecture](#system-architecture)

## Overview

The DWI (Digital Work Instruction) Platform is a comprehensive workflow management system built with:
- **Backend**: Spring Boot Java application with PostgreSQL database and MongoDB for ontology data
- **Frontend**: React TypeScript application with Redux for state management
- **Additional**: Quartz for job scheduling, JAaS for authentication, Email services, and observability stack

## Database Schema

### Core Entities and Relationships

#### 1. User Management
```sql
-- Primary user entity
users (
  id BIGINT PRIMARY KEY,
  employee_id VARCHAR(255) NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255),
  email VARCHAR(255) NOT NULL,
  username VARCHAR(255) NOT NULL,
  archived BOOLEAN DEFAULT FALSE,
  organisations_id BIGINT REFERENCES organisations(id),
  created_at BIGINT,
  modified_at BIGINT
)

-- Organizations
organisations (
  id BIGINT PRIMARY KEY,
  addresses_id BIGINT REFERENCES addresses(id),
  contact_number VARCHAR(255),
  deleted_status BOOLEAN,
  created_at BIGINT,
  modified_at BIGINT
)

-- Addresses
addresses (
  id BIGINT PRIMARY KEY,
  city VARCHAR(255),
  country VARCHAR(255),
  line1 VARCHAR(255),
  line2 VARCHAR(255),
  state VARCHAR(255),
  pincode INTEGER,
  created_at BIGINT,
  modified_at BIGINT
)

-- Facilities
facilities (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  code VARCHAR(255) NOT NULL,
  organisations_id BIGINT REFERENCES organisations(id),
  addresses_id BIGINT REFERENCES addresses(id),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- User-Facility Mapping
user_facilities_mapping (
  users_id BIGINT REFERENCES users(id),
  facilities_id BIGINT REFERENCES facilities(id),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id),
  PRIMARY KEY (users_id, facilities_id)
)
```

#### 2. Process Management (Checklists)
```sql
-- Checklists (Process Templates)
checklists (
  id BIGINT PRIMARY KEY,
  name VARCHAR(512),
  code VARCHAR(20) NOT NULL,
  state VARCHAR(50) NOT NULL, -- Enum: DRAFT, UNDER_REVIEW, PUBLISHED, etc.
  job_log_columns JSONB DEFAULT '[]',
  archived BOOLEAN DEFAULT FALSE,
  versions_id BIGINT REFERENCES versions(id),
  organisations_id BIGINT REFERENCES organisations(id),
  use_cases_id BIGINT REFERENCES use_cases(id),
  review_cycle INTEGER DEFAULT 1,
  description TEXT,
  released_at BIGINT,
  released_by BIGINT REFERENCES users(id),
  is_global BOOLEAN DEFAULT FALSE,
  color_code VARCHAR(50),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Checklist-Facility Mapping
checklist_facility_mapping (
  checklists_id BIGINT REFERENCES checklists(id),
  facilities_id BIGINT REFERENCES facilities(id),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id),
  PRIMARY KEY (checklists_id, facilities_id)
)

-- Versions for version control
versions (
  id BIGINT PRIMARY KEY,
  parent BIGINT,
  self BIGINT,
  ancestor BIGINT,
  version INTEGER,
  type VARCHAR(255),
  versioned_at BIGINT,
  deprecated_at BIGINT,
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Stages (Process Steps)
stages (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  archived BOOLEAN DEFAULT FALSE,
  order_tree INTEGER NOT NULL,
  checklists_id BIGINT REFERENCES checklists(id),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Tasks (Individual Work Items)
tasks (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  archived BOOLEAN DEFAULT FALSE,
  order_tree INTEGER NOT NULL,
  has_stop BOOLEAN DEFAULT FALSE,
  max_period BIGINT,
  min_period BIGINT,
  timer_operator VARCHAR(255),
  is_timed BOOLEAN DEFAULT FALSE,
  is_mandatory BOOLEAN DEFAULT FALSE,
  stages_id BIGINT REFERENCES stages(id),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Parameters (Data Collection Points)
parameters (
  id BIGINT PRIMARY KEY,
  label VARCHAR(255),
  description TEXT,
  type VARCHAR(50) NOT NULL, -- TEXT, NUMBER, DATE, CHECKLIST, etc.
  mode VARCHAR(50), -- OPTIONAL, REQUIRED
  is_mandatory BOOLEAN DEFAULT FALSE,
  order_tree INTEGER,
  data JSONB DEFAULT '{}',
  verification_type VARCHAR(50),
  tasks_id BIGINT REFERENCES tasks(id),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)
```

#### 3. Job Execution
```sql
-- Jobs (Process Instances)
jobs (
  id BIGINT PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  state VARCHAR(50) NOT NULL, -- UNASSIGNED, ASSIGNED, IN_PROGRESS, COMPLETED, etc.
  checklists_id BIGINT REFERENCES checklists(id),
  facilities_id BIGINT REFERENCES facilities(id),
  organisations_id BIGINT REFERENCES organisations(id),
  use_cases_id BIGINT REFERENCES use_cases(id),
  started_at BIGINT,
  started_by BIGINT REFERENCES users(id),
  ended_at BIGINT,
  ended_by BIGINT REFERENCES users(id),
  is_scheduled BOOLEAN DEFAULT FALSE,
  schedulers_id BIGINT REFERENCES schedulers(id),
  expected_start_date BIGINT,
  expected_end_date BIGINT,
  checklist_ancestor_id BIGINT,
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Task Executions
task_executions (
  id BIGINT PRIMARY KEY,
  reason TEXT,
  correction_reason TEXT,
  state VARCHAR(255) NOT NULL, -- NOT_STARTED, IN_PROGRESS, COMPLETED, etc.
  started_at BIGINT,
  ended_at BIGINT,
  started_by BIGINT REFERENCES users(id),
  tasks_id BIGINT REFERENCES tasks(id),
  jobs_id BIGINT REFERENCES jobs(id),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Task Execution User Assignments
task_execution_user_mapping (
  users_id BIGINT REFERENCES users(id),
  task_executions_id BIGINT REFERENCES task_executions(id),
  action_performed BOOLEAN,
  state VARCHAR(255),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id),
  PRIMARY KEY (users_id, task_executions_id)
)

-- Parameter Values (Data Collected)
parameter_values (
  id BIGINT PRIMARY KEY,
  value TEXT,
  choices JSONB DEFAULT '{}',
  reason TEXT,
  state VARCHAR(255), -- VALID, INVALID, PENDING_APPROVAL, etc.
  parameters_id BIGINT REFERENCES parameters(id),
  jobs_id BIGINT REFERENCES jobs(id),
  task_executions_id BIGINT REFERENCES task_executions(id),
  parameter_value_approval_id BIGINT,
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Parameter Value Approvals
parameter_value_approvals (
  id BIGINT PRIMARY KEY,
  state VARCHAR(255), -- PENDING, APPROVED, REJECTED
  users_id BIGINT REFERENCES users(id),
  created_at BIGINT
)
```

#### 4. Media Management
```sql
-- Media Files
medias (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  filename VARCHAR(255) NOT NULL,
  description TEXT,
  type VARCHAR(255) NOT NULL, -- IMAGE, VIDEO, PDF, etc.
  link VARCHAR(255) NOT NULL,
  archived BOOLEAN DEFAULT FALSE,
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Parameter-Media Mappings
parameter_media_mapping (
  medias_id BIGINT REFERENCES medias(id),
  parameters_id BIGINT REFERENCES parameters(id),
  archived BOOLEAN DEFAULT FALSE,
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id),
  PRIMARY KEY (medias_id, parameters_id)
)

-- Parameter Value-Media Mappings
parameter_value_media_mapping (
  medias_id BIGINT REFERENCES medias(id),
  parameter_values_id BIGINT REFERENCES parameter_values(id),
  archived BOOLEAN DEFAULT FALSE,
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id),
  PRIMARY KEY (medias_id, parameter_values_id)
)

-- Task-Media Mappings
task_media_mapping (
  medias_id BIGINT REFERENCES medias(id),
  tasks_id BIGINT REFERENCES tasks(id),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id),
  PRIMARY KEY (medias_id, tasks_id)
)
```

#### 5. Auditing and Logging
```sql
-- Job Audits
job_audits (
  id BIGINT PRIMARY KEY,
  jobs_id BIGINT REFERENCES jobs(id),
  stages_id BIGINT REFERENCES stages(id),
  tasks_id BIGINT REFERENCES tasks(id),
  action VARCHAR(255),
  details TEXT,
  diff_data JSONB,
  event VARCHAR(255),
  facilities_id BIGINT REFERENCES facilities(id),
  new_data JSONB,
  old_data JSONB,
  organisations_id BIGINT REFERENCES organisations(id),
  severity VARCHAR(255),
  triggered_at BIGINT,
  triggered_by BIGINT REFERENCES users(id)
)

-- Checklist Audits
checklist_audits (
  id BIGINT PRIMARY KEY,
  checklists_id BIGINT REFERENCES checklists(id),
  action VARCHAR(255),
  details TEXT,
  triggered_at BIGINT,
  triggered_by BIGINT REFERENCES users(id),
  facilities_id BIGINT REFERENCES facilities(id),
  organisations_id BIGINT REFERENCES organisations(id)
)

-- Email Audits
email_audits (
  id BIGINT PRIMARY KEY,
  from_address TEXT NOT NULL,
  to_addresses TEXT[],
  body TEXT,
  subject TEXT,
  cc TEXT[],
  bcc TEXT[],
  retry_attempts SMALLINT,
  max_attempts SMALLINT,
  created_on BIGINT NOT NULL
)
```

#### 6. Collaboration and Reviews
```sql
-- Checklist Collaborators
checklist_collaborator_mapping (
  id BIGINT PRIMARY KEY,
  checklists_id BIGINT REFERENCES checklists(id),
  users_id BIGINT REFERENCES users(id),
  order_tree INTEGER NOT NULL,
  review_cycle INTEGER NOT NULL,
  state VARCHAR(255), -- PENDING, APPROVED, REJECTED, etc.
  type VARCHAR(255) NOT NULL, -- AUTHOR, PRIMARY_AUTHOR, REVIEWER, SIGN_OFF_USER
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Collaborator Comments
checklist_collaborator_comments (
  id BIGINT PRIMARY KEY,
  checklists_id BIGINT REFERENCES checklists(id),
  checklist_collaborator_mappings_id BIGINT REFERENCES checklist_collaborator_mapping(id),
  comments TEXT NOT NULL,
  review_state VARCHAR(255),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)
```

#### 7. Automation and Scheduling
```sql
-- Automations
automations (
  id BIGINT PRIMARY KEY,
  action VARCHAR(255) NOT NULL,
  config JSONB DEFAULT '{}',
  display_name VARCHAR(255),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Task Automation Mappings
task_automation_mapping (
  tasks_id BIGINT REFERENCES tasks(id),
  automations_id BIGINT REFERENCES automations(id),
  action_type VARCHAR(255),
  is_active BOOLEAN DEFAULT TRUE,
  order_tree INTEGER,
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id),
  PRIMARY KEY (tasks_id, automations_id)
)

-- Schedulers
schedulers (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(50), -- CRON, INTERVAL
  cron_expression VARCHAR(255),
  interval_duration BIGINT,
  is_active BOOLEAN DEFAULT TRUE,
  organisations_id BIGINT REFERENCES organisations(id),
  use_cases_id BIGINT REFERENCES use_cases(id),
  checklists_id BIGINT REFERENCES checklists(id),
  expected_duration BIGINT,
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)
```

#### 8. Properties and Configuration
```sql
-- Properties (Custom Fields)
properties (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  place_holder VARCHAR(255),
  order_tree INTEGER NOT NULL,
  is_mandatory BOOLEAN NOT NULL,
  type VARCHAR(255) NOT NULL, -- TEXT, NUMBER, DATE, CHOICE, etc.
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id)
)

-- Checklist Property Values
checklist_property_values (
  properties_id BIGINT REFERENCES properties(id),
  checklists_id BIGINT REFERENCES checklists(id),
  value VARCHAR(255),
  created_at BIGINT,
  modified_at BIGINT,
  created_by BIGINT REFERENCES users(id),
  modified_by BIGINT REFERENCES users(id),
  PRIMARY KEY (properties_id, checklists_id)
)

-- Codes (Auto-generated identifiers)
codes (
  organisations_id BIGINT REFERENCES organisations(id),
  type VARCHAR(255),
  clause SMALLINT,
  counter INTEGER,
  PRIMARY KEY (organisations_id, type, clause)
)
```

### Key Database Constraints and Indexes

1. **Foreign Key Constraints**: All relationships between tables are enforced through foreign keys
2. **Unique Constraints**: 
   - User email and username are unique within organization
   - Checklist codes are unique within organization
   - Job codes are unique within organization
3. **Check Constraints**: 
   - State enums are validated at database level
   - Mandatory boolean fields have proper defaults
4. **Indexes**: 
   - All foreign key columns are indexed
   - Commonly queried fields like state, created_at are indexed
   - Composite indexes on frequently joined columns

## Backend API Schema

### Core API Endpoints

#### 1. Job Management APIs

```java
// Job Controller Endpoints
GET    /v1/jobs                           // List jobs with pagination and filters
GET    /v1/jobs/{jobId}                   // Get specific job details
POST   /v1/jobs                           // Create new job
PATCH  /v1/jobs/{jobId}                   // Update job
PATCH  /v1/jobs/{jobId}/start             // Start job execution
PATCH  /v1/jobs/{jobId}/complete          // Complete job
PATCH  /v1/jobs/{jobId}/complete-with-exception // Complete with exception
GET    /v1/jobs/{jobId}/state             // Get job state
GET    /v1/jobs/{jobId}/reports           // Get job reports
GET    /v1/jobs/{jobId}/assignments       // Get job assignees
PATCH  /v1/jobs/{jobId}/assignments       // Bulk assign users to job
GET    /v1/jobs/assignee/me               // Get jobs assigned to current user
GET    /v1/jobs/approvals                 // Get pending approvals
```

**Job DTO Structure:**
```typescript
interface JobDto {
  id: string;
  code: string;
  state: JobStates; // UNASSIGNED, IN_PROGRESS, COMPLETED, etc.
  totalTasks: number;
  completedTasks: number;
  checklist: ChecklistJobDto;
  schedulerId?: number;
  expectedStartDate?: number;
  expectedEndDate?: number;
  relations: RelationValueDto[];
  parameterValues: ParameterDto[];
  startedAt?: number;
  endedAt?: number;
  softErrors?: Error[];
}
```

#### 2. Checklist Management APIs

```java
// Checklist Controller Endpoints
GET    /v1/checklists                     // List checklists
GET    /v1/checklists/{checklistId}       // Get checklist details
POST   /v1/checklists                     // Create checklist
PATCH  /v1/checklists/{checklistId}       // Update checklist
PATCH  /v1/checklists/{checklistId}/archive // Archive checklist
PATCH  /v1/checklists/{checklistId}/publish // Publish checklist
POST   /v1/checklists/{checklistId}/revision // Create revision
GET    /v1/checklists/{checklistId}/parameters // Get parameters
PATCH  /v1/checklists/{checklistId}/review/submit // Submit for review
PATCH  /v1/checklists/{checklistId}/review/sign-off // Sign off checklist
```

**Checklist DTO Structure:**
```typescript
interface ChecklistDto {
  id: string;
  name: string;
  code: string;
  state: ChecklistStates; // DRAFT, UNDER_REVIEW, PUBLISHED, etc.
  description?: string;
  stages: StageDto[];
  properties: PropertyValueDto[];
  relations: RelationDto[];
  version: VersionDto;
  collaborators: CollaboratorDto[];
  isGlobal: boolean;
  colorCode?: string;
}
```

#### 3. Parameter Management APIs

```java
// Parameter Controller Endpoints
GET    /v1/parameters                     // List parameters
GET    /v1/parameters/{parameterId}       // Get parameter details
POST   /v1/parameters                     // Create parameter
PATCH  /v1/parameters/{parameterId}       // Update parameter
PATCH  /v1/parameters/{parameterId}/execute // Execute parameter
GET    /v1/parameters/{parameterId}/rules // Get parameter rules
POST   /v1/parameters/{parameterId}/verify // Verify parameter value
```

**Parameter DTO Structure:**
```typescript
interface ParameterDto {
  id: string;
  label: string;
  description?: string;
  type: ParameterType; // TEXT, NUMBER, DATE, CHECKLIST, etc.
  mode: ParameterMode; // OPTIONAL, REQUIRED
  isMandatory: boolean;
  orderTree: number;
  data: Record<string, any>; // Configuration data
  verificationType?: VerificationType;
  rules: ParameterRuleDto[];
  validations: ValidationDto[];
  autoInitialize?: AutoInitializeDto;
}
```

#### 4. User Management APIs

```java
// User Controller (via JAaS service)
GET    /v1/users                          // List users
GET    /v1/users/{userId}                 // Get user details
POST   /v1/users                          // Create user
PATCH  /v1/users/{userId}                 // Update user
PATCH  /v1/users/{userId}/archive         // Archive user
GET    /v1/users/by/roles                 // Get users by roles
```

**User DTO Structure:**
```typescript
interface UserDto {
  id: string;
  employeeId: string;
  firstName: string;
  lastName?: string;
  email: string;
  username: string;
  archived: boolean;
  state: UserStates;
  roles: RoleDto[];
  facilities: FacilityDto[];
  organisation: OrganisationDto;
}
```

#### 5. Media Management APIs

```java
// Media Controller Endpoints
GET    /v1/medias                         // List media files
GET    /v1/medias/{mediaId}               // Get media details
POST   /v1/medias                         // Upload media
DELETE /v1/medias/{mediaId}               // Delete media
```

### API Response Format

All API responses follow a consistent format:

```typescript
interface ApiResponse<T> {
  data: T;
  errors?: Error[];
  message?: string;
  pagination?: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

interface Error {
  code: string;
  message: string;
  details?: Record<string, any>;
}
```

### Authentication & Authorization

- **Authentication**: JWT tokens provided by JAaS service
- **Authorization**: Role-based access control (RBAC)
- **Facility Switching**: Users can switch between assigned facilities
- **Session Management**: Token refresh and session timeout handling

## Frontend Data Models

### TypeScript Type Definitions

#### 1. Core Types

```typescript
// Common Types
export type Audit = {
  createdAt: number;
  modifiedAt: number;
  modifiedBy: PartialUser;
  createdBy: PartialUser;
};

export type PartialUser = Pick<User, 'employeeId' | 'firstName' | 'id' | 'lastName' | 'archived'>;

export enum Constraint {
  LT = 'LT',
  GT = 'GT',
  LTE = 'LTE',
  GTE = 'GTE',
  NE = 'NE',
  MIN = 'MIN',
  MAX = 'MAX',
  PATTERN = 'PATTERN',
  EQ = 'EQ',
  ANY = 'ANY',
  LIKE = 'LIKE'
}
```

#### 2. Job Types

```typescript
// Job States
export const JOB_STATES = {
  UNASSIGNED: { title: 'Not Started', color: '#f7b500' },
  ASSIGNED: { title: 'Not Started', color: '#f7b500' },
  BLOCKED: { title: 'Approval Pending', color: '#f7b500' },
  IN_PROGRESS: { title: 'Started', color: '#1d84ff' },
  COMPLETED: { title: 'Completed', color: '#5aa700' },
  COMPLETED_WITH_EXCEPTION: { title: 'Completed with Exception', color: '#f7b500' }
} as const;

export type JobStates = keyof typeof JOB_STATES;

// Job Interface
export interface Job {
  checklist: Checklist;
  code: string;
  id: string;
  state: JobStates;
  name?: string;
  assignees: (PartialUser & { jobId: string })[];
  expectedStartDate?: number;
  expectedEndDate?: number;
  startedAt?: number;
  endedAt?: number;
  scheduler?: Record<string, any>;
  parameterValues: Parameter[];
}

// Job Store Structure
export interface JobStore {
  id?: string;
  stages: Map<string, StoreStage>;
  tasks: Map<string, StoreTask>;
  taskExecutions: Map<string, StoreTaskExecution>;
  loading: boolean;
  state?: JobStates;
  code?: string;
  processId?: string;
  processName?: string;
  isInboxView: boolean;
  pendingTasks: Set<string>;
  showVerificationBanner: boolean;
  showCorrectionBanner: boolean;
  timerState: {
    earlyCompletion: boolean;
    limitCrossed: boolean;
    timeElapsed: number;
  };
  auditLogs: {
    logs: JobAuditLogType[];
    pageable: Pageable;
    loading: boolean;
  };
  activeTask: {
    loading: boolean;
    parameters: Map<string, StoreParameter>;
    isTaskAssigned: boolean;
    stageOrderTree: number;
    pendingSelfVerificationParameters: any[];
    pendingPeerVerificationParameters: any[];
  };
  errors: {
    parametersErrors: Map<string, string[]>;
    taskErrors: any[];
  };
}
```

#### 3. Parameter Types

```typescript
// Parameter Types
export enum ParameterType {
  NUMBER = 'NUMBER',
  TEXT = 'TEXT',
  TEXTAREA = 'TEXTAREA',
  SINGLE_LINE = 'SINGLE_LINE',
  MULTI_LINE = 'MULTI_LINE',
  RICH_TEXT = 'RICH_TEXT',
  YES_NO = 'YES_NO',
  CHECKLIST = 'CHECKLIST',
  SINGLE_SELECT = 'SINGLE_SELECT',
  MULTI_SELECT = 'MULTI_SELECT',
  DATE = 'DATE',
  DATE_TIME = 'DATE_TIME',
  TIME = 'TIME',
  FILE_UPLOAD = 'FILE_UPLOAD',
  SIGNATURE = 'SIGNATURE',
  INSTRUCTION = 'INSTRUCTION',
  MATERIAL = 'MATERIAL',
  SHOULD_BE = 'SHOULD_BE',
  CALCULATION = 'CALCULATION',
  RESOURCE = 'RESOURCE'
}

export interface Parameter {
  id: string;
  label: string;
  description?: string;
  type: ParameterType;
  mode: 'OPTIONAL' | 'REQUIRED';
  orderTree: number;
  data: ParameterData;
  rules: ParameterRule[];
  validations: Validation[];
  response?: ParameterResponse;
  verification?: ParameterVerification;
  hidden?: boolean;
  autoInitialize?: AutoInitialize;
}

export interface ParameterData {
  placeholder?: string;
  multiSelect?: boolean;
  options?: Option[];
  calculations?: Calculation[];
  urlPath?: string;
  propertyValidations?: PropertyValidation[];
  constraints?: Constraint[];
  autoInitialize?: AutoInitialize;
  verificationType?: VerificationType;
  targetEntityType?: string;
  relatedParametersInfo?: RelatedParametersInfo;
}
```

#### 4. Task and Stage Types

```typescript
// Task Types
export interface Task {
  id: string;
  name: string;
  orderTree: number;
  isOptional: boolean;
  hasStop: boolean;
  isTimed: boolean;
  timerOperator?: TimerOperator;
  minPeriod?: number;
  maxPeriod?: number;
  parameters: Parameter[];
  automations: Automation[];
  medias: Media[];
  stopReasons?: StopReason[];
}

export interface StoreTask extends Omit<Task, 'parameters'> {
  stageId: string;
  isAssigned: boolean;
  assignees: TaskAssignee[];
  state: TaskExecutionStates;
  taskExecution?: TaskExecution;
  parameterCount: number;
  completedParameterCount: number;
  corrections: Correction[];
}

// Stage Types
export interface Stage {
  id: string;
  name: string;
  orderTree: number;
  tasks: Task[];
}

export interface StoreStage extends Omit<Stage, 'tasks'> {
  taskIds: string[];
  taskExecutionState: StageExecutionStates;
  isActive: boolean;
}
```

#### 5. Process (Checklist) Types

```typescript
// Process States
export enum ProcessState {
  BEING_BUILT = 'BEING_BUILT',
  READY_FOR_REVIEW = 'READY_FOR_REVIEW',
  UNDER_REVIEW = 'UNDER_REVIEW',
  REQUESTED_CHANGES = 'REQUESTED_CHANGES',
  READY_FOR_SIGN_OFF = 'READY_FOR_SIGN_OFF',
  SIGN_OFF_INITIATED = 'SIGN_OFF_INITIATED',
  WAITING_FOR_SIGN_OFF = 'WAITING_FOR_SIGN_OFF',
  PUBLISHED = 'PUBLISHED',
  DEPRECATED = 'DEPRECATED'
}

export interface Checklist {
  id: string;
  name: string;
  code: string;
  state: ProcessState;
  description?: string;
  stages: Stage[];
  properties: Property[];
  relations: Relation[];
  version?: Version;
  collaborators: Collaborator[];
  facilities: Facility[];
  isGlobal: boolean;
  colorCode?: string;
  jobLogColumns?: JobLogColumn[];
  reviewCycle: number;
}
```

### Redux Store Structure

```typescript
// Root State
export interface RootState {
  users: UsersState;
  facilities: FacilitiesState;
  properties: PropertiesState;
  extras: ExtrasState;
  auditLogFilters: AuditLogFiltersState;
  fileUpload: FileUploadState;
  facilityWiseConstants: FacilityWiseConstantsState;
  prototypeComposer: PrototypeComposerState;
  job: JobStore;
}

// Users State
export interface UsersState {
  active: UsersGroup;
  archived: UsersGroup;
  all: UsersGroup;
  loading: boolean;
  error?: string;
  selectedState: string;
  selectedUser?: User;
  currentPageData: User[];
  selectedUserLoading: boolean;
}

// Facilities State
export interface FacilitiesState {
  list: Facility[];
  selected?: Facility;
  loading: boolean;
  error?: string;
}
```

### Frontend Component Architecture

```typescript
// Component Props Patterns
interface BaseComponentProps {
  className?: string;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}

interface DataComponentProps<T> extends BaseComponentProps {
  data: T;
  loading?: boolean;
  error?: string | null;
  onRefresh?: () => void;
}

interface FormComponentProps<T> extends BaseComponentProps {
  initialValues?: T;
  onSubmit: (values: T) => void | Promise<void>;
  validationSchema?: any;
  disabled?: boolean;
}

// Higher-Order Components
interface WithPaginationProps {
  page: number;
  size: number;
  total: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

interface WithFiltersProps<T> {
  filters: T;
  onFiltersChange: (filters: T) => void;
  onClearFilters: () => void;
}
```

## Configuration Schema

### Backend Configuration (application.properties)

```properties
# Spring Core Configuration
spring.main.banner-mode=off
server.error.whitelabel.enabled=false
spring.servlet.multipart.enabled=true
server.compression.enabled=true
management.endpoint.health.probes.enabled=true

# Database Configuration
spring.jpa.database=postgresql
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.datasource.url=jdbc:postgresql://${datasource.host}:${datasource.port}/${datasource.database}
spring.datasource.username=${datasource.username}
spring.datasource.password=${datasource.password}
spring.jpa.hibernate.ddl-auto=validate
spring.datasource.hikari.maximum-pool-size=${datasource.pool-size}

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://${mongodb.username}:${mongodb.password}@${mongodb.host}:${mongodb.port}/${mongodb.database}
mongodb.host=localhost
mongodb.port=27017
mongodb.database=ontology
mongodb.username=root
mongodb.password=root

# Liquibase Configuration
spring.liquibase.change-log=classpath:/db/changelog/changelog-platform-dev-master.xml
spring.liquibase.enabled=false

# Server Configuration
spring.application.name=dwi
server.port=8080
server.servlet.context-path=/

# File Storage Configuration
spring.servlet.multipart.max-file-size=250MB
spring.servlet.multipart.max-request-size=2GB
medias.location=/tmp
medias.cdn=http://assets.platform.leucinetech.com
medias.file-types=pdf,jpeg,jpg,png,doc,docx,xls,xlsx,ppt,pptx,csv

# JAaS Service Configuration
jaas.service=c6d8285b72a84efb8fbd608c7cada484
jaas.root=http://localhost:9091
jaas.path.authLogin=/v1/auth/login
jaas.path.users=/v1/users
jaas.path.facility=/v1/facilities
jaas.path.roles=/v1/roles

# Email Configuration
email.host=smtp.gmail.com
email.port=587
email.protocol=smtp
email.tlsEnabled=true
email.authEnabled=true
email.enabled=true

# Quartz Scheduler Configuration
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate

# AWS Configuration
aws.access.key=
aws.secret.key=
aws.account.id=
aws.region=ap-south-1
aws.sqs.queue.name=email-delivery-queue

# Observability Configuration
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
otel.service.name=backend-dwi
otel.exporter.otlp.endpoint=http://localhost:4317
otel.traces.sampler.probability=1.0
```

### Frontend Configuration (package.json)

```json
{
  "name": "streem-frontend",
  "version": "4.9.1",
  "customer_version": "4.9",
  "license": "UNLICENSED",
  "engines": {
    "node": "18.13.0",
    "yarn": "1.22.19",
    "npm": "8.19.3"
  },
  "scripts": {
    "start": "node scripts/start.js",
    "build": "bash scripts/pre-build.sh && node scripts/build.js && bash scripts/post-build.sh",
    "lint:check": "eslint -c .eslintrc 'src/**/*{.ts,.tsx}'",
    "lint:fix": "eslint --fix .eslintrc 'src/**/*{.ts,.tsx}'",
    "test": "jest"
  },
  "dependencies": {
    "react": "^17.0.1",
    "react-dom": "^17.0.1",
    "@material-ui/core": "^4.12.3",
    "@material-ui/icons": "^4.11.2",
    "react-redux": "^7.2.4",
    "redux": "^4.1.1",
    "redux-saga": "^1.1.3",
    "styled-components": "^5.3.1",
    "axios": "^0.21.2",
    "lodash": "^4.17.21",
    "date-fns": "^2.30.0",
    "i18next": "^20.4.0",
    "react-i18next": "^11.11.4"
  },
  "devDependencies": {
    "typescript": "^4.9.3",
    "@types/react": "^17.0.19",
    "@types/react-dom": "^17.0.9",
    "webpack": "^5.76.0",
    "babel-loader": "^8.2.5",
    "css-loader": "^6.7.1",
    "eslint": "^7.32.0",
    "prettier": "^2.3.2",
    "jest": "^29.5.0"
  }
}
```

### Environment Variables

```bash
# Database
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=dwi
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# MongoDB
MONGODB_HOST=localhost
MONGODB_PORT=27017
MONGODB_DATABASE=ontology
MONGODB_USERNAME=root
MONGODB_PASSWORD=root

# JAaS Service
JAAS_ROOT=http://localhost:9091
JAAS_SERVICE_ID=c6d8285b72a84efb8fbd608c7cada484

# Email
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=username
EMAIL_PASSWORD=password
EMAIL_FROM_ADDRESS=noreply@leucinetech.com

# File Storage
MEDIAS_LOCATION=/tmp
MEDIAS_CDN=http://assets.platform.leucinetech.com

# AWS
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_ACCOUNT_ID=
AWS_REGION=ap-south-1

# Observability
OTEL_ENDPOINT=http://localhost:4317
PROMETHEUS_ENDPOINT=http://localhost:9090
```

## System Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React App     │    │  Spring Boot    │    │   PostgreSQL    │
│   (Frontend)    │◄──►│   (Backend)     │◄──►│   (Primary DB)  │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Redux       │    │     JAaS        │    │    MongoDB      │
│  (State Mgmt)   │    │ (Auth Service)  │    │  (Ontology DB)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │     Quartz      │
                       │  (Scheduler)    │
                       └─────────────────┘
```

### Data Flow Architecture

```
Frontend (React/Redux) ◄────────────────► Backend (Spring Boot)
         │                                         │
         │                                         │
         ▼                                         ▼
┌─────────────────┐                      ┌─────────────────┐
│  Component UI   │                      │   Controllers   │
│                 │                      │                 │
│ • Job Views     │                      │ • JobController │
│ • Process Views │                      │ • ChecklistCtrl │
│ • Parameter UI  │                      │ • ParameterCtrl │
└─────────────────┘                      └─────────────────┘
         │                                         │
         ▼                                         ▼
┌─────────────────┐                      ┌─────────────────┐
│ Redux Actions/  │                      │    Services     │
│     Sagas       │                      │                 │
│                 │                      │ • JobService    │
│ • Job Actions   │                      │ • ChecklistSvc  │
│ • API Calls     │                      │ • ParameterSvc  │
└─────────────────┘                      └─────────────────┘
         │                                         │
         ▼                                         ▼
┌─────────────────┐                      ┌─────────────────┐
│  Redux Store    │                      │  Repositories   │
│                 │                      │                 │
│ • Jobs State    │                      │ • JPA Repos     │
│ • Users State   │                      │ • Custom Repos  │
│ • UI State      │                      │ • Mongo Repos   │
└─────────────────┘                      └─────────────────┘
                                                   │
                                                   ▼
                                         ┌─────────────────┐
                                         │   Databases     │
                                         │                 │
                                         │ • PostgreSQL    │
                                         │ • MongoDB       │
                                         └─────────────────┘
```

### Security Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │     JAaS        │
│                 │    │                 │    │ Authentication  │
│ • JWT Storage   │◄──►│ • Auth Filter   │◄──►│   Service       │
│ • Token Refresh │    │ • Role Checks   │    │                 │
│ • Route Guards  │    │ • API Security  │    │ • User Mgmt     │
└─────────────────┘    └─────────────────┘    │ • Role Mgmt     │
                                              │ • Session Mgmt  │
                                              └─────────────────┘
```

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Load Balancer                            │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        │                           │
        ▼                           ▼
┌─────────────────┐        ┌─────────────────┐
│  Frontend App   │        │  Backend App    │
│  (React/Nginx)  │        │ (Spring Boot)   │
│                 │        │                 │
│ • Static Files  │        │ • REST APIs     │
│ • React Router  │        │ • Business Logic│
│ • Caching       │        │ • Data Access   │
└─────────────────┘        └─────────────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
                    ▼                ▼                ▼
          ┌─────────────────┐ ┌─────────────┐ ┌─────────────┐
          │   PostgreSQL    │ │   MongoDB   │ │    JAaS     │
          │  (Primary DB)   │ │ (Ontology)  │ │  Service    │
          └─────────────────┘ └─────────────┘ └─────────────┘
```

---

## Summary

This comprehensive schema documentation covers:

1. **Database Schema**: Complete PostgreSQL schema with 50+ tables covering user management, process definitions, job execution, auditing, and more
2. **Backend API Schema**: RESTful API endpoints, DTOs, and service contracts
3. **Frontend Data Models**: TypeScript interfaces, Redux store structure, and component patterns
4. **Configuration Schema**: Environment variables, application properties, and deployment configs
5. **System Architecture**: High-level system design, data flow, security, and deployment architecture

The DWI platform is a robust workflow management system with strong data modeling, comprehensive APIs, and modern frontend architecture supporting complex business processes in manufacturing and operational environments.


GIVE ME COMPLETE SCHEMAA. 
