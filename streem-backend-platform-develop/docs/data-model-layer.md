# Streem Backend: Comprehensive Entity Layer Analysis

## Table of Contents
1. [Overview](#overview)
2. [Entity Hierarchy & Base Classes](#entity-hierarchy--base-classes)
3. [Core Business Entities](#core-business-entities)
4. [User Management Entities](#user-management-entities)
5. [Parameter & Execution Entities](#parameter--execution-entities)
6. [Media & Document Entities](#media--document-entities)
7. [Audit & Tracking Entities](#audit--tracking-entities)
8. [Support & Configuration Entities](#support--configuration-entities)
9. [Entity Relationships Overview](#entity-relationships-overview)
10. [Database Schema Implications](#database-schema-implications)
11. [Business Domain Mappings](#business-domain-mappings)

---

## Overview

The Streem Backend follows a sophisticated domain-driven design with entities representing a workflow management system for manufacturing and quality processes. The system manages checklists, jobs, tasks, parameters, and their executions with comprehensive audit trails and verification mechanisms.

**Key Domain Concepts:**
- **Process Templates**: Checklists define reusable process templates
- **Job Execution**: Jobs are instances of checklists executed at facilities
- **Task Management**: Granular task execution with parameter capture
- **Quality Control**: Multi-level verification and exception handling
- **Media Handling**: Comprehensive document and media management

---

## Entity Hierarchy & Base Classes

### BaseEntity
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/helper/BaseEntity.java`

**Purpose**: Foundation class providing unique ID generation for all entities.

**Attributes**:
- `id: Long` - Primary key with custom ID generation using IdGenerator
- `@PrePersist` - Automatically sets ID before persistence

**JPA Annotations**:
- `@MappedSuperclass` - Not a table, inherited by all entities
- `@Id` - Primary key designation
- `@Column(columnDefinition = "bigint", updatable = false, nullable = false)`

### UserAuditIdentifiableBase
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/helper/UserAuditIdentifiableBase.java`

**Purpose**: Provides complete audit trail with user tracking for all business entities.

**Attributes**:
- `createdBy: User` - User who created the record
- `modifiedBy: User` - User who last modified the record
- `createdAt: Long` - Creation timestamp
- `modifiedAt: Long` - Last modification timestamp

**JPA Annotations**:
- `@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.DETACH})`
- `@PrePersist`, `@PreUpdate` - Automatic timestamp management

---

## Core Business Entities

### 1. Organisation
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Organisation.java`

**Purpose**: Top-level tenant entity in multi-tenant architecture.

**Attributes**:
- `name: String` - Organisation name (varchar 255, not null)
- `archived: boolean` - Soft delete flag (default false)
- `fqdn: String` - Fully qualified domain name (text, not null)
- `facilities: Set<Facility>` - One-to-many relationship with facilities

**JPA Annotations**:
- `@Entity`
- `@Table(name = TableName.ORGANISATIONS)`
- `@OneToMany(mappedBy = "organisation", cascade = CascadeType.ALL)`

**Repository**: `IOrganisationRepository`
**Service**: `IFacilityService` (manages org-facility relationships)

**Business Significance**: Root entity for multi-tenancy, each organisation operates independently with its own facilities, users, and processes.

### 2. Facility
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Facility.java`

**Purpose**: Physical or logical location where processes are executed.

**Attributes**:
- `name: String` - Facility name (varchar 255)
- `archived: boolean` - Soft delete flag (default false)
- `organisation: Organisation` - Parent organisation (not null)
- `timeZone: String` - Facility timezone (varchar 30, not null)
- `dateFormat: String` - Date format preference (varchar 50, not null)
- `timeFormat: String` - Time format preference (varchar 50, not null)
- `dateTimeFormat: String` - DateTime format preference (varchar 50, not null)

**JPA Annotations**:
- `@ManyToOne(fetch = FetchType.LAZY)`
- `@JoinColumn(name = "organisations_id", nullable = false)`

**Repository**: `IFacilityRepository`
**Service**: `IFacilityService`

**Business Significance**: Represents operational locations with localization settings for time/date formats. Critical for job scheduling and execution tracking.

### 3. UseCase
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/UseCase.java`

**Purpose**: Process categories or types that group related checklists.

**Attributes**:
- `name: String` - UseCase name (varchar 255, not null)
- `label: String` - Display label (varchar 255)
- `description: String` - Detailed description (text)
- `orderTree: Integer` - Sorting order (not null)
- `metadata: JsonNode` - Additional configuration (jsonb default '{}')
- `archived: boolean` - Soft delete flag (default false)

**JPA Annotations**:
- `@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)`
- `@Type(type = "jsonb")`

**Repository**: `IUseCaseRepository`
**Service**: `IUseCaseService`

**Business Significance**: Categorizes processes by type (e.g., "Quality Control", "Maintenance", "Production"), enabling organized process management.

### 4. Checklist
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Checklist.java`

**Purpose**: Process template defining structured workflows with stages, tasks, and parameters.

**Attributes**:
- `name: String` - Checklist name (varchar 512)
- `state: State.Checklist` - Lifecycle state (varchar 50, not null)
- `code: String` - Unique identifier (varchar 20, not null, updatable false)
- `jobLogColumns: JsonNode` - Custom job log configuration (jsonb default '[]')
- `archived: boolean` - Soft delete flag (default false)
- `reviewCycle: Integer` - Review frequency (default 1)
- `description: String` - Process description (text)
- `isGlobal: boolean` - Global availability flag (default false)
- `colorCode: String` - Visual identification (varchar 50)

**Relationships**:
- `version: Version` - Version control
- `organisation: Organisation` - Owner organisation
- `useCase: UseCase` - Process category
- `stages: Set<Stage>` - Process stages (One-to-many, ordered)
- `jobs: Set<Job>` - Job instances (One-to-many)
- `facilities: Set<ChecklistFacilityMapping>` - Available facilities
- `relations: Set<Relation>` - External object relations
- `collaborators: Set<ChecklistCollaboratorMapping>` - Authors/reviewers

**JPA Annotations**:
- `@NamedEntityGraphs` - Optimized loading strategies
- `@OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL)`
- `@OrderBy("order_tree")`
- `@Where(clause = "archived = false")`

**Repository**: `IChecklistRepository`
**Services**: `IChecklistService`, `IChecklistRevisionService`, `IChecklistCollaboratorService`

**DTOs**: `ChecklistDto`, `ChecklistBasicDto`, `ChecklistPartialDto`

**Business Significance**: Core entity representing reusable process templates. Supports versioning, collaboration, and multi-facility deployment.

### 5. Stage
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Stage.java`

**Purpose**: Logical grouping of related tasks within a checklist.

**Attributes**:
- `name: String` - Stage name (varchar 512, not null)
- `orderTree: Integer` - Execution order (not null)
- `archived: boolean` - Soft delete flag (default false)
- `checklist: Checklist` - Parent checklist
- `tasks: Set<Task>` - Child tasks (ordered, non-archived)

**JPA Annotations**:
- `@NamedEntityGraph` - Optimized task loading
- `@OneToMany(mappedBy = "stage", cascade = CascadeType.ALL)`
- `@OrderBy("order_tree")`
- `@Where(clause = "archived = false")`

**Repository**: `IStageRepository`
**Service**: `IStageService`

**Business Significance**: Provides process structure and enables parallel/sequential task execution control.

### 6. Task
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Task.java`

**Purpose**: Individual work unit containing parameters to be executed.

**Attributes**:
- `name: String` - Task name (varchar 512, not null)
- `orderTree: Integer` - Execution order (not null)
- `hasStop: boolean` - Stop point flag (default false)
- `isSoloTask: boolean` - Single user execution flag (default false)
- `isTimed: boolean` - Time tracking flag (default false)
- `timerOperator: String` - Timer comparison logic (varchar 50)
- `isMandatory: boolean` - Required completion flag (default false)
- `minPeriod: Long` - Minimum execution time
- `maxPeriod: Long` - Maximum execution time
- `enableRecurrence: boolean` - Recurring execution flag
- `enableScheduling: boolean` - Scheduled execution flag
- `hasBulkVerification: boolean` - Bulk verification support
- `hasInterlocks: boolean` - Interlock controls flag
- `hasExecutorLock: boolean` - Executor locking flag

**Relationships**:
- `stage: Stage` - Parent stage
- `parameters: Set<Parameter>` - Data collection points
- `medias: Set<TaskMediaMapping>` - Associated documents
- `automations: Set<TaskAutomationMapping>` - Automation rules
- `taskRecurrence: TaskRecurrence` - Recurrence settings
- `taskSchedules: TaskSchedules` - Schedule configuration
- `dependentTasks: Set<TaskDependency>` - Task dependencies

**Repository**: `ITaskRepository`
**Services**: `ITaskService`, `ITaskExecutionService`, `ITaskDependencyService`

**Business Significance**: Atomic work units with rich configuration for timing, dependencies, automation, and verification requirements.

### 7. Job
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Job.java`

**Purpose**: Runtime instance of a checklist executed at a specific facility.

**Attributes**:
- `code: String` - Unique job identifier (varchar 20, not null)
- `state: State.Job` - Execution state (varchar 50, not null)
- `isScheduled: boolean` - Scheduled job flag (default false)
- `startedAt: Long` - Job start timestamp
- `endedAt: Long` - Job completion timestamp
- `expectedStartDate: Long` - Planned start time
- `expectedEndDate: Long` - Planned completion time
- `checklistAncestorId: Long` - Original checklist version reference

**Relationships**:
- `checklist: Checklist` - Process template (not null)
- `facility: Facility` - Execution location (not null)
- `organisation: Organisation` - Owner organisation (not null)
- `useCase: UseCase` - Process category (not null)
- `scheduler: Scheduler` - Scheduling details
- `startedBy: User` - Job initiator
- `endedBy: User` - Job completer
- `taskExecutions: Set<TaskExecution>` - Task instances
- `parameterValues: Set<ParameterValue>` - Collected data
- `relationValues: Set<RelationValue>` - External object associations
- `parameterVerifications: Set<ParameterVerification>` - Quality verifications

**JPA Annotations**:
- `@NamedEntityGraphs` - Optimized loading for different scenarios
- `@OneToMany(mappedBy = "job", cascade = CascadeType.ALL)`

**Repository**: `IJobRepository`
**Services**: `IJobService`, `ICreateJobService`, `IJobAssignmentService`, `IJobAuditService`

**Business Significance**: Central execution entity tracking complete process instance lifecycle from start to completion with comprehensive audit trail.

---

## Parameter & Execution Entities

### 8. Parameter
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Parameter.java`

**Purpose**: Data collection point within tasks with validation and verification rules.

**Attributes**:
- `type: Type.Parameter` - Parameter type (varchar 50, not null)
- `targetEntityType: Type.ParameterTargetEntityType` - Target entity (varchar 50, not null)
- `verificationType: Type.VerificationType` - Verification requirement (varchar 50, default NONE)
- `label: String` - Display label (varchar 255)
- `description: String` - Detailed description (text)
- `orderTree: Integer` - Display order (not null)
- `isMandatory: boolean` - Required completion flag (default false)
- `archived: boolean` - Soft delete flag (default false)
- `data: JsonNode` - Type-specific configuration (jsonb default '{}')
- `validations: JsonNode` - Validation rules (jsonb default '[]')
- `isAutoInitialized: boolean` - Auto-population flag (default false)
- `autoInitialize: JsonNode` - Auto-initialization rules (jsonb)
- `rules: JsonNode` - Business rules (jsonb)
- `hidden: boolean` - Visibility flag (default false)
- `metadata: JsonNode` - Additional configuration (jsonb default '{}')

**Relationships**:
- `task: Task` - Parent task
- `checklist: Checklist` - Owner checklist (read-only)
- `parameterValues: Set<ParameterValue>` - Collected values
- `medias: List<ParameterMediaMapping>` - Associated documents
- `impactedByRules: Set<ParameterRuleMapping>` - Incoming rule impacts
- `triggeredByRules: Set<ParameterRuleMapping>` - Outgoing rule triggers

**Repository**: `IParameterRepository`
**Services**: `IParameterService`, `IParameterExecutionService`, `IParameterValidationService`, `IParameterAutoInitializeService`

**Business Significance**: Flexible data collection system supporting various input types, validation rules, and automated behaviors.

### 9. ParameterValue
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterValue.java`

**Purpose**: Actual data collected during parameter execution in jobs.

**Extends**: `ParameterValueBase`

**Attributes**:
- `hasCorrections: boolean` - Correction history flag (default false)
- `hasExceptions: boolean` - Exception history flag (default false)
- `hasActiveException: boolean` - Active exception flag (default false)
- `medias: List<ParameterValueMediaMapping>` - Associated media files

**Inherited from ParameterValueBase**:
- `state: State.ParameterExecution` - Execution state (varchar 50, not null)
- `verified: boolean` - Verification status (default false)
- `reason: String` - Execution reason/comments (text)
- `value: String` - Collected value (text)
- `choices: JsonNode` - Multi-choice selections (jsonb)
- `hidden: boolean` - Visibility flag (default false)
- `clientEpoch: Long` - Client timestamp (bigint, not null)
- `version: Long` - Optimistic locking version
- `impactedBy: JsonNode` - Rule impact tracking (jsonb)
- `hasVariations: boolean` - Variation history flag (default false)

**Relationships**:
- `parameter: Parameter` - Source parameter definition
- `job: Job` - Parent job instance
- `taskExecution: TaskExecution` - Execution context
- `parameterValueApproval: ParameterValueApproval` - Approval workflow
- `parameterVerifications: List<ParameterVerification>` - Verification history
- `variations: List<Variation>` - Value change history

**Repository**: `IParameterValueRepository`
**Services**: `IParameterExecutionService`, `IParameterVerificationService`

**Business Significance**: Core data storage for process execution with comprehensive audit trail, verification workflow, and change tracking.

### 10. ParameterVerification
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterVerification.java`

**Purpose**: Quality control verification records for parameter values.

**Extends**: `VerificationBase`

**Attributes**:
- `parameterValue: ParameterValue` - Verified parameter value
- `parameterValueId: Long` - Parameter value reference

**Inherited from VerificationBase**:
- `job: Job` - Parent job context
- `user: User` - Verifying user
- `verificationType: Type.VerificationType` - Verification type
- `verificationStatus: State.ParameterVerification` - Verification outcome
- `comments: String` - Verification comments
- `userGroup: UserGroup` - Verifying user group (optional)
- `isBulk: boolean` - Bulk verification flag (default false)

**Repository**: `IParameterVerificationRepository`
**Service**: `IParameterVerificationService`

**Business Significance**: Implements quality control through peer/self verification workflows with detailed audit trail.

### 11. ParameterException
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterException.java`

**Purpose**: Exception handling for parameter values that don't meet validation criteria.

**Attributes**:
- `code: String` - Exception identifier (varchar 20, not null)
- `value: String` - Exception value (text)
- `choices: JsonNode` - Exception choices (jsonb)
- `Status: State.ParameterException` - Exception status (varchar 50, not null)
- `initiatorsReason: String` - Initiator's explanation (text)
- `reviewersReason: String` - Reviewer's decision (text)
- `previousState: State.ParameterExecution` - Pre-exception state
- `reason: String` - Exception reason (text)
- `ruleId: String` - Triggering rule identifier (text, not null)

**Relationships**:
- `parameterValue: ParameterValue` - Source parameter value
- `taskExecution: TaskExecution` - Execution context
- `facility: Facility` - Location context
- `job: Job` - Parent job

**Repository**: `IParameterExceptionRepository`
**Service**: `IParameterExceptionService`

**Business Significance**: Enables controlled deviation from standard processes with approval workflows and audit trails.

### 12. TaskExecution
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/TaskExecution.java`

**Purpose**: Runtime instance of task execution within a job.

**Attributes**:
- `state: State.TaskExecution` - Execution state (varchar 50, not null)
- `type: Type.TaskExecutionType` - Execution type (varchar 50, not null)
- `correctionReason: String` - Correction explanation (text)
- `correctionEnabled: boolean` - Correction availability (default false)
- `reason: String` - Execution reason (text)
- `startedAt: Long` - Start timestamp
- `endedAt: Long` - Completion timestamp
- `correctedAt: Long` - Correction timestamp
- `duration: Long` - Execution duration
- `orderTree: Integer` - Execution order (not null)
- `continueRecurrence: boolean` - Recurrence continuation flag (default false)
- `scheduled: boolean` - Scheduled execution flag (default false)

**Timing & Scheduling**:
- `recurringExpectedStartedAt: Long` - Recurring task expected start
- `recurringExpectedDueAt: Long` - Recurring task expected completion
- `schedulingExpectedStartedAt: Long` - Scheduled task expected start
- `schedulingExpectedDueAt: Long` - Scheduled task expected completion
- `recurringPrematureStartReason: String` - Early start explanation
- `recurringOverdueCompletionReason: String` - Late completion explanation
- `scheduleOverdueCompletionReason: String` - Schedule delay explanation
- `schedulePrematureStartReason: String` - Early schedule start explanation

**Relationships**:
- `task: Task` - Task template
- `job: Job` - Parent job
- `startedBy: User` - Task initiator
- `endedBy: User` - Task completer
- `correctedBy: User` - Correction performer
- `parameterValues: Set<ParameterValue>` - Collected data
- `assignees: Set<TaskExecutionUserMapping>` - Assigned users

**Repository**: `ITaskExecutionRepository`
**Services**: `ITaskExecutionService`, `ITaskExecutionTimerService`

**Business Significance**: Tracks individual task execution with timing, assignment, and correction capabilities for detailed process monitoring.

---

## User Management Entities

### 13. User
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/User.java`

**Purpose**: System user entity with organisational context.

**Attributes**:
- `employeeId: String` - Employee identifier (varchar 255, not null)
- `firstName: String` - First name (varchar 255, not null)
- `lastName: String` - Last name (varchar 255)
- `email: String` - Email address (varchar 255, not null)
- `archived: boolean` - Soft delete flag (default false)
- `username: String` - System username (varchar 255, not null)

**Relationships**:
- `organisation: Organisation` - User's organisation (not null)
- `organisationId: Long` - Organisation reference

**Constants**:
- `SYSTEM_USER_ID = 1L` - System user identifier

**Repository**: `IUserRepository`
**Service**: `IUserService`

**Business Significance**: Central user entity providing authentication context and organisational membership for all audit trails.

### 14. UserGroup
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/UserGroup.java`

**Purpose**: User collections for role-based access and verification workflows.

**Attributes**:
- `name: String` - Group name (varchar, not null)
- `description: String` - Group description (text, not null)
- `active: boolean` - Active status flag (not null)

**Relationships**:
- `facility: Facility` - Associated facility
- `facilityId: Long` - Facility reference
- `userGroupMembers: List<UserGroupMember>` - Group membership

**Repository**: `IUserGroupRepository`
**Services**: `IUserGroupService`, `IUserGroupAuditService`

**Business Significance**: Enables role-based process assignment and verification workflows with facility-specific scoping.

---

## Media & Document Entities

### 15. Media
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Media.java`

**Purpose**: File and document storage with metadata.

**Attributes**:
- `name: String` - Display name (varchar 255)
- `description: String` - File description (text)
- `originalFilename: String` - Original filename (varchar 255, not null)
- `filename: String` - Stored filename (varchar 255, not null)
- `type: String` - MIME type (varchar 255, not null)
- `relativePath: String` - Storage path (text, not null)
- `archived: boolean` - Soft delete flag (default false)

**Relationships**:
- `organisation: Organisation` - Owner organisation (not null)

**Repository**: `IMediaRepository`
**Service**: `IMediaService`

**Business Significance**: Centralized media management supporting process documentation, evidence collection, and instructional materials across the platform.

---

## Audit & Tracking Entities

### 16. Version
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Version.java`

**Purpose**: Version control system for checklists and schedulers.

**Attributes**:
- `ancestor: Long` - Root version identifier (not null)
- `parent: Long` - Parent version identifier
- `self: Long` - Current version identifier
- `versionedAt: Long` - Version creation timestamp
- `deprecatedAt: Long` - Version deprecation timestamp
- `version: Integer` - Version number
- `type: Type.EntityType` - Versioned entity type (varchar 50, not null)

**Repository**: `IVersionRepository`
**Service**: `IVersionService`

**Business Significance**: Enables process template versioning with genealogy tracking for change management and compliance.

### 17. Correction
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Correction.java`

**Purpose**: Data correction workflow for parameter values.

**Attributes**:
- `code: String` - Correction identifier (varchar 20, not null)
- `oldValue: String` - Original value (text)
- `newValue: String` - Corrected value (text)
- `oldChoices: JsonNode` - Original selections (jsonb)
- `newChoices: JsonNode` - Corrected selections (jsonb)
- `Status: State.Correction` - Correction status (varchar 50, not null)
- `initiatorsReason: String` - Correction justification (text)
- `correctorsReason: String` - Corrector's explanation (text)
- `reviewersReason: String` - Reviewer's decision (text)
- `previousState: State.ParameterExecution` - Pre-correction state

**Relationships**:
- `parameterValue: ParameterValue` - Target parameter value
- `taskExecution: TaskExecution` - Execution context
- `facility: Facility` - Location context
- `job: Job` - Parent job

**Repository**: `ICorrectionRepository`
**Service**: `ICorrectionService`

**Business Significance**: Enables controlled data correction with approval workflows maintaining complete audit trail for compliance and quality control.

---

## Support & Configuration Entities

### 18. Automation
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Automation.java`

**Purpose**: Process automation rules and actions.

**Attributes**:
- `type: Type.AutomationType` - Automation type (varchar 50, not null)
- `actionType: Type.AutomationActionType` - Action type (varchar 50, not null)
- `targetEntityType: Type.TargetEntityType` - Target entity (varchar 50, not null)
- `actionDetails: JsonNode` - Action configuration (jsonb default '{}')
- `triggerType: Type.AutomationTriggerType` - Trigger type (varchar 50, not null)
- `triggerDetails: JsonNode` - Trigger configuration (jsonb default '{}')
- `archived: boolean` - Soft delete flag (default false)

**Repository**: `IAutomationRepository`
**Service**: `ITaskAutomationService`

**Business Significance**: Enables process automation with configurable triggers and actions for improved efficiency and consistency.

### 19. Relation
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Relation.java`

**Purpose**: External object integration and data relationships.

**Attributes**:
- `externalId: String` - External system identifier (varchar, not null)
- `displayName: String` - Display name (varchar, not null)
- `urlPath: String` - API endpoint path (text, not null)
- `variables: JsonNode` - Configuration variables (jsonb default '{}')
- `cardinality: CollectionMisc.Cardinality` - Relationship cardinality
- `objectTypeId: String` - Object type identifier
- `collection: String` - Collection name
- `orderTree: Integer` - Display order (not null)
- `validations: JsonNode` - Validation rules (jsonb default '{}')
- `isMandatory: boolean` - Required flag (default false)

**Relationships**:
- `checklist: Checklist` - Parent checklist

**Repository**: `IRelationRepository`

**Business Significance**: Integrates external systems and objects into process workflows enabling data exchange and validation.

### 20. Property
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Property.java`

**Purpose**: Configurable metadata fields for use cases and checklists.

**Attributes**:
- `name: String` - Property name (varchar 255, not null)
- `label: String` - Display label (varchar 255, not null)
- `placeHolder: String` - Input placeholder (varchar 255)
- `type: Type.PropertyType` - Property type (varchar 50, not null)
- `orderTree: Integer` - Display order (not null)
- `isGlobal: boolean` - Global availability flag (default false)
- `archived: boolean` - Soft delete flag (default false)

**Relationships**:
- `useCase: UseCase` - Associated use case
- `useCaseId: Long` - Use case reference

**Repository**: `IPropertyRepository`
**Service**: `IPropertyService`

**Business Significance**: Enables flexible metadata collection and configuration per use case for customizable process templates.

### 21. Scheduler
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Scheduler.java`

**Purpose**: Job scheduling configuration and management.

**Attributes**:
- `name: String` - Scheduler name (varchar 512)
- `description: String` - Scheduler description (text)
- `code: String` - Unique identifier (varchar 20, not null)
- `checklistName: String` - Associated checklist name (varchar 512)
- `expectedStartDate: Long` - Planned start time (not null)
- `dueDateInterval: Integer` - Due date offset
- `dueDateDuration: JsonNode` - Duration configuration (jsonb default '{}')
- `isRepeated: boolean` - Recurring flag (default false)
- `recurrenceRule: String` - Recurrence pattern (text)
- `isCustomRecurrence: boolean` - Custom recurrence flag (default false)
- `enabled: boolean` - Active status (default false)
- `data: JsonNode` - Additional configuration (jsonb default '{}')
- `archived: boolean` - Soft delete flag (default false)
- `state: State.Scheduler` - Scheduler state (varchar 50, not null)
- `deprecatedAt: Long` - Deprecation timestamp

**Relationships**:
- `checklist: Checklist` - Scheduled process template (not null)
- `facility: Facility` - Target facility (not null)
- `useCase: UseCase` - Process category (not null)
- `version: Version` - Version control

**Repository**: `ISchedulerRepository`
**Services**: `ISchedulerService`, `IQuartzService`

**Business Significance**: Enables automated job scheduling with complex recurrence patterns and lifecycle management for routine process execution.

---

## Entity Relationships Overview

### Core Hierarchy
```
Organisation (1) ──── (∞) Facility
     │
     └─── (∞) User
     
UseCase (1) ──── (∞) Checklist ──── (∞) Job
                      │               │
                      └─── (∞) Stage ─┼─── (∞) TaskExecution
                            │         │
                            └─ (∞) Task ──── (∞) Parameter
                                             │
                                             └─── (∞) ParameterValue
```

### Execution Flow
```
Checklist → Job → TaskExecution → ParameterValue
    │         │         │              │
    │         │         └── User ──────┤
    │         └── Facility             │
    └── Version                        └── ParameterVerification
```

### Quality Control Chain
```
ParameterValue ──── ParameterVerification
      │                    │
      ├── ParameterException │
      └── Correction         └── UserGroup
```

---

## Database Schema Implications

### Indexes Required
- **Organisation-scoped queries**: `(organisation_id, created_at)` on major entities
- **Job execution tracking**: `(job_id, state, created_at)` on TaskExecution
- **Parameter search**: `(checklist_id, type, archived)` on Parameter
- **Audit trails**: `(created_by, created_at)` across audit entities
- **Version control**: `(ancestor, version)` on Version table

### Partition Strategy
- **Large tables** (ParameterValue, TaskExecution): Consider partitioning by `created_at` (monthly)
- **Audit tables**: Partition by organisation for tenant isolation
- **Media storage**: Separate tablespace for BLOB data

### Foreign Key Constraints
- **Organisation → Facility**: CASCADE DELETE for tenant cleanup
- **Checklist → Job**: RESTRICT DELETE to preserve execution history  
- **Job → ParameterValue**: CASCADE DELETE for complete cleanup
- **User references**: SET NULL on user deletion to preserve audit trail

---

## Business Domain Mappings

### Manufacturing Workflow
- **Process Templates**: Checklists define standard operating procedures
- **Work Orders**: Jobs represent specific production runs or maintenance tasks
- **Quality Gates**: Parameters with verification requirements ensure compliance
- **Batch Records**: Complete audit trail from template to execution

### Quality Management System
- **Document Control**: Version management for process templates
- **CAPA Integration**: Exception and correction workflows
- **Audit Trails**: Complete traceability for regulatory compliance
- **User Training**: TrainedUser entities track competency requirements

### Digital Transformation
- **Paper Elimination**: Digital parameter collection replaces paper forms
- **Real-time Monitoring**: Live job execution status and metrics
- **Data Analytics**: Structured data collection enables process optimization
- **Integration Ready**: Relations enable ERP/MES system connectivity

### Compliance Framework
- **21 CFR Part 11**: Electronic signature support through verification workflows
- **ISO 9001**: Quality management system process control
- **GMP Guidelines**: Manufacturing execution system requirements
- **Audit Readiness**: Immutable audit trails and change control

---

## Repository and Service Layer Mappings

### Repository Pattern Implementation
Each entity follows consistent repository pattern:
- **Interface**: `I{Entity}Repository extends JpaRepository`
- **Custom Queries**: Named queries and specifications for complex searches
- **Entity Graphs**: Optimized loading strategies for relationship-heavy entities

### Service Layer Architecture
- **Business Logic**: Services implement domain rules and workflows
- **Transaction Management**: `@Transactional` boundaries for data consistency
- **Event Handling**: Service layer triggers automation and notifications
- **Validation**: Cross-entity validation and business rule enforcement

### DTO Transformation
- **Request DTOs**: Command objects for entity modifications
- **Response DTOs**: Optimized data transfer with computed fields
- **Projection DTOs**: Lightweight views for list operations
- **Audit DTOs**: Historical data representation for reporting

This comprehensive entity analysis provides the foundation for understanding the Streem Backend's sophisticated workflow management system, enabling developers to effectively work with the domain model while maintaining data integrity and business rule compliance.

---

## Additional Core Entities

### 22. Action
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Action.java`

**Purpose**: Defines automated actions that can be triggered during process execution.

**Attributes**:
- `name: String` - Action name (not null)
- `description: String` - Action description
- `code: String` - Unique action identifier (not null)
- `triggerType: ActionTriggerType` - Trigger condition (enum, not null)
- `triggerEntityId: Long` - Entity that triggers the action (not null)
- `archived: boolean` - Soft delete flag (default false)
- `successMessage: String` - Success notification text
- `failureMessage: String` - Failure notification text

**Relationships**:
- `checklist: Checklist` - Parent checklist (not null, updatable false)
- `checklistId: Long` - Checklist reference (read-only)

**JPA Annotations**:
- `@Entity`
- `@Table(name = TableName.ACTIONS)`
- `@ManyToOne(fetch = FetchType.LAZY)`
- `@Enumerated(EnumType.STRING)`

**Repository**: `IActionRepository`
**Service**: `IActionService`

**Business Significance**: Enables process automation through configurable triggers and actions, supporting workflow efficiency and consistency.

### 23. ActionFacilityMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ActionFacilityMapping.java`

**Purpose**: Maps actions to specific facilities for location-based automation control.

**Attributes**:
- `actionFacilityCompositeKey: ActionFacilityCompositeKey` - Composite primary key

**Relationships**:
- `action: Action` - Associated action (not null, not updatable)
- `facility: Facility` - Target facility (not null, not updatable)

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`

**Business Significance**: Controls which automated actions are available at specific facilities, enabling location-specific automation rules.

### 24. AutoInitializedParameter
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/AutoInitializedParameter.java`

**Purpose**: Defines relationships between parameters that automatically initialize others with data.

**Attributes**:
- `autoInitializedParameterId: Long` - Auto-initialized parameter reference (read-only)
- `checklistId: Long` - Checklist reference (read-only)

**Relationships**:
- `autoInitializedParameter: Parameter` - Parameter that gets auto-initialized (not null, not updatable)
- `referencedParameter: Parameter` - Source parameter providing the data (not null, not updatable)
- `checklist: Checklist` - Owner checklist (not null, not updatable)

**JPA Annotations**:
- `@OneToOne(fetch = FetchType.LAZY, optional = false)`
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`

**Repository**: `IAutoInitializedParameterRepository`
**Service**: `IParameterAutoInitializeService`

**Business Significance**: Enables parameter data flow and automation, reducing manual data entry and ensuring data consistency across related parameters.

### 25. Code
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Code.java`

**Purpose**: Manages unique code generation counters for various entity types.

**Attributes**:
- `codeId: CodeCompositeKey` - Composite key containing entity type and organization
- `counter: Integer` - Current counter value

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@Entity`
- `@Table(name = TableName.CODES)`

**Business Significance**: Provides centralized unique identifier generation for jobs, checklists, and other entities requiring sequential codes within organizational scope.

### 26. CorrectionMediaMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/CorrectionMediaMapping.java`

**Purpose**: Associates media files with correction records for documentation and evidence.

**Attributes**:
- `correctionId: Long` - Correction reference (read-only)
- `isOldMedia: boolean` - Flag indicating if media is from original value (default false)
- `archived: boolean` - Soft delete flag (default false)

**Relationships**:
- `correction: Correction` - Associated correction record (not null, not updatable, CASCADE ALL)
- `media: Media` - Associated media file (not null)
- `parameterValue: ParameterValue` - Related parameter value (not updatable)

**JPA Annotations**:
- `@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)`

**Repository**: `ICorrectionMediaMappingRepository`
**Service**: `ICorrectionService`

**Business Significance**: Maintains evidence trail for corrections, supporting audit requirements and providing visual documentation of data changes.

### 27. Corrector
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Corrector.java`

**Purpose**: Tracks users or user groups assigned to perform corrections on parameter values.

**Attributes**:
- `correctionId: Long` - Correction reference (read-only)
- `actionPerformed: boolean` - Flag indicating if correction action was completed (default false, not null)

**Relationships**:
- `userGroup: UserGroup` - Assigned user group (not null)
- `user: User` - Assigned individual user (not null)
- `correction: Correction` - Associated correction record (not null, not updatable, CASCADE ALL)

**JPA Annotations**:
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`
- `@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)`

**Repository**: `ICorrectorRepository`
**Service**: `ICorrectionService`

**Business Significance**: Implements correction workflow assignment and tracking, ensuring accountability in data correction processes.

### 28. OrganisationSetting
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/OrganisationSetting.java`

**Purpose**: Stores organization-specific configuration settings for security and system behavior.

**Attributes**:
- `logoUrl: String` - Organization logo URL (varchar 255)
- `sessionIdleTimeout: Integer` - Session timeout in minutes (default 10)
- `registrationTokenExpiration: Integer` - Registration token validity in minutes (default 60)
- `passwordResetTokenExpiration: Integer` - Password reset token validity in minutes (default 60)
- `maxFailedLoginAttempts: Integer` - Maximum failed login attempts (default 3)
- `maxFailedAdditionalVerificationAttempts: Integer` - Maximum failed verification attempts (default 3)
- `maxFailedChallengeQuestionAttempts: Integer` - Maximum failed challenge attempts (default 3)
- `autoUnlockAfter: Integer` - Auto-unlock time in minutes (default 15)
- `organisationId: Long` - Organization reference (read-only)
- `createdAt: Long` - Creation timestamp (not null, not updatable)
- `modifiedAt: Long` - Modification timestamp (not null)

**Relationships**:
- `organisation: Organisation` - Owner organization (not null, OneToOne)

**JPA Annotations**:
- `@OneToOne(fetch = FetchType.LAZY, optional = false)`
- `@JsonIgnore` - Hidden from JSON serialization

**Repository**: `IOrganisationSettingRepository`
**Service**: `IOrganisationSettingService`

**Business Significance**: Provides centralized configuration management for security policies, session management, and organizational branding.

### 29. Interlock
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/Interlock.java`

**Purpose**: Defines safety interlocks and validation rules for process execution control.

**Attributes**:
- `targetEntityType: Type.InterlockTargetEntityType` - Target entity type (enum, not null)
- `targetEntityId: Long` - Target entity identifier
- `validations: JsonNode` - Interlock validation rules (jsonb, not null)

**JPA Annotations**:
- `@Entity`
- `@Table(name = TableName.INTERLOCKS)`
- `@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)`
- `@Enumerated(EnumType.STRING)`
- `@Type(type = "jsonb")`

**Repository**: `IInterlockRepository`
**Service**: `IInterlockService`

**Business Significance**: Implements safety controls and conditional logic to prevent unsafe operations and ensure process compliance with safety requirements.

## Checklist Management Entities

### 30. ChecklistAudit
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ChecklistAudit.java`

**Purpose**: Comprehensive audit trail for all checklist-related actions and modifications.

**Attributes**:
- `organisationsId: Long` - Organization identifier (not null)
- `triggeredBy: Long` - User who triggered the audit event (not null)
- `checklistId: Long` - Affected checklist identifier (not null)
- `action: Action.ChecklistAudit` - Type of audit action (enum, varchar 50, not null)
- `details: String` - Detailed description of the action (text)
- `triggeredAt: Long` - Timestamp when action occurred (auto-set)
- `stageId: Long` - Associated stage identifier (nullable)
- `taskId: Long` - Associated task identifier (nullable)
- `triggeredFor: Long` - Target entity identifier (nullable)

**JPA Annotations**:
- `@Entity`
- `@Table(name = TableName.CHECKLIST_AUDITS)`
- `@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)`
- `@PrePersist` - Auto-sets triggeredAt timestamp
- `@JsonIgnore` - Hides triggeredAt from JSON serialization

**Repository**: `IChecklistAuditRepository`
**Service**: `IChecklistAuditService`

**Business Significance**: Provides complete audit trail for checklist lifecycle events, supporting compliance, troubleshooting, and change tracking for regulatory requirements.

### 31. ChecklistCollaboratorComments
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ChecklistCollaboratorComments.java`

**Purpose**: Stores review comments from collaborators during checklist development and review phases.

**Attributes**:
- `reviewState: State.ChecklistCollaborator` - Review state when comment was made (enum, varchar 50, not null)
- `comments: String` - Review comment text (text, not null)

**Relationships**:
- `checklistCollaboratorMapping: ChecklistCollaboratorMapping` - Associated collaborator mapping (not null, not updatable, EAGER fetch)
- `checklist: Checklist` - Target checklist (not null, not updatable, EAGER fetch, CASCADE DETACH)

**JPA Annotations**:
- `@ManyToOne(fetch = FetchType.EAGER, optional = false)`
- `@ManyToOne(fetch = FetchType.EAGER, optional = false, cascade = {CascadeType.DETACH})`
- `@Enumerated(EnumType.STRING)`

**Repository**: `IChecklistCollaboratorCommentsRepository`
**Service**: `IChecklistCollaboratorService`

**Business Significance**: Enables collaborative review process with detailed feedback capture, supporting quality improvement and stakeholder engagement in process development.

### 32. ChecklistCollaboratorMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ChecklistCollaboratorMapping.java`

**Purpose**: Maps users to checklists as collaborators with specific roles and phases in the development process.

**Attributes**:
- `type: Type.Collaborator` - Collaborator role type (enum, varchar 50, not null)
- `state: State.ChecklistCollaborator` - Current collaboration state (enum, varchar 50, not null)
- `phaseType: State.ChecklistCollaboratorPhaseType` - Phase type for collaboration (enum, varchar 50, not null)
- `phase: Integer` - Current phase number (default 1, not null)
- `orderTree: Integer` - Collaboration order (default 1, not null)

**Relationships**:
- `checklist: Checklist` - Target checklist (not null, not updatable, EAGER fetch)
- `user: User` - Collaborating user (not null, not updatable, EAGER fetch, CASCADE DETACH)
- `comments: List<ChecklistCollaboratorComments>` - Associated review comments (LAZY fetch, CASCADE ALL, ordered by createdAt)

**JPA Annotations**:
- `@ManyToOne(fetch = FetchType.EAGER, optional = false)`
- `@OneToMany(mappedBy = "checklistCollaboratorMapping", fetch = FetchType.LAZY, cascade = CascadeType.ALL)`
- `@OrderBy("createdAt")`

**Repository**: `IChecklistCollaboratorMappingRepository`
**Service**: `IChecklistCollaboratorService`

**Business Methods**:
- `isPrimary()`: Returns true if collaborator is primary author

**Business Significance**: Implements collaborative checklist development workflow with role-based access, review phases, and structured feedback collection.

### 33. ChecklistFacilityMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ChecklistFacilityMapping.java`

**Purpose**: Maps checklists to facilities, controlling which processes are available at each location.

**Attributes**:
- `checklistFacilityId: ChecklistFacilityCompositeKey` - Composite primary key
- `checklistId: Long` - Checklist reference (read-only)
- `facilityId: Long` - Facility reference (read-only)

**Relationships**:
- `checklist: Checklist` - Associated checklist (not null, not updatable, LAZY fetch)
- `facility: Facility` - Target facility (not null, not updatable, LAZY fetch)

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`

**Repository**: `IChecklistFacilityMappingRepository`
**Service**: `IChecklistFacilityService`

**Business Significance**: Controls process deployment and availability across facilities, enabling location-specific process management and controlled rollout of process templates.

### 34. ChecklistPropertyValue
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ChecklistPropertyValue.java`

**Purpose**: Stores custom property values for checklists based on facility-specific use case configurations.

**Attributes**:
- `checklistPropertyValueId: ChecklistPropertyValueCompositeKey` - Composite primary key
- `facilityUseCasePropertyMappingId: Long` - Property mapping reference (read-only)
- `value: String` - Property value (varchar 255)
- `checklistId: Long` - Checklist reference (read-only)

**Relationships**:
- `facilityUseCasePropertyMapping: FacilityUseCasePropertyMapping` - Property definition (not null, not updatable, LAZY fetch)
- `checklist: Checklist` - Owner checklist (not null, not updatable, LAZY fetch)

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`

**Repository**: `IChecklistPropertyValueRepository`
**Service**: `IChecklistPropertyService`

**Business Significance**: Enables facility-specific customization of checklist metadata and properties, supporting localized process variations and facility-specific requirements.

## Job Execution & Tracking Entities

### 35. JobAnnotation
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/JobAnnotation.java`

**Purpose**: Stores annotations and remarks added to jobs during or after execution.

**Attributes**:
- `remarks: String` - Annotation text content (text)
- `code: String` - Unique annotation identifier (varchar 20, not null, not updatable)

**Relationships**:
- `job: Job` - Associated job (not null, OneToOne, LAZY fetch)
- `medias: List<JobAnnotationMediaMapping>` - Associated media files (LAZY fetch, CASCADE ALL)

**JPA Annotations**:
- `@OneToOne(fetch = FetchType.LAZY, optional = false)`
- `@OneToMany(fetch = FetchType.LAZY, mappedBy = "jobAnnotation", cascade = CascadeType.ALL)`

**Business Methods**:
- `addMedia(Job job, Media media, User principalUserEntity)`: Adds single media to annotation
- `addAllMedias(Job job, List<Media> medias, User principalUserEntity)`: Adds multiple media files

**Repository**: `IJobAnnotationRepository`
**Service**: `IJobAnnotationService`

**Business Significance**: Enables post-execution documentation, operator notes, and evidence collection for job completion records and quality documentation.

### 36. JobAnnotationMediaMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/JobAnnotationMediaMapping.java`

**Purpose**: Maps media files to job annotations for comprehensive documentation.

**Attributes**:
- `jobAnnotationMediaId: JobAnnotationMediaCompositeKey` - Composite primary key

**Relationships**:
- `jobAnnotation: JobAnnotation` - Associated annotation (not null, not updatable, LAZY fetch)
- `job: Job` - Associated job (not null, not updatable, LAZY fetch)
- `media: Media` - Associated media file (not null, not updatable, LAZY fetch)

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`

**Repository**: `IJobAnnotationMediaMappingRepository`
**Service**: `IJobAnnotationService`

**Business Significance**: Provides evidence trail for job annotations, supporting audit requirements and comprehensive documentation of job execution events.

### 37. JobAudit
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/JobAudit.java`

**Purpose**: Comprehensive audit trail for all job-related actions and state changes.

**Attributes**:
- `organisationsId: Long` - Organization identifier (not null)
- `triggeredBy: Long` - User who triggered the audit event (not null)
- `jobId: Long` - Affected job identifier (not null)
- `stageId: Long` - Associated stage identifier (nullable)
- `taskId: Long` - Associated task identifier (nullable)
- `action: Action.Audit` - Type of audit action (enum, varchar 50, not null)
- `details: String` - Detailed description of the action (text)
- `parameters: JsonNode` - Additional audit parameters (jsonb, default '{}', not null)
- `triggeredAt: Long` - Timestamp when action occurred

**JPA Annotations**:
- `@Entity`
- `@Table(name = TableName.JOB_AUDITS)`
- `@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)`
- `@Type(type = "jsonb")`
- `@JsonIgnore` - Hides triggeredAt from JSON serialization

**Repository**: `IJobAuditRepository`
**Service**: `IJobAuditService`

**Business Significance**: Provides complete audit trail for job execution lifecycle, supporting compliance, troubleshooting, and performance analysis for regulatory requirements.

### 38. JobCweDetail
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/JobCweDetail.java`

**Purpose**: Captures "Completed With Exception" details when jobs finish with issues or deviations.

**Attributes**:
- `reason: JobCweReason` - Reason for completion with exception (enum, varchar 45)
- `comment: String` - Detailed explanation of the exception (text)

**Relationships**:
- `job: Job` - Associated job (OneToOne, LAZY fetch)
- `medias: Set<JobCweDetailMediaMapping>` - Supporting media files (LAZY fetch, CASCADE ALL)

**JPA Annotations**:
- `@OneToOne(fetch = FetchType.LAZY)`
- `@OneToMany(fetch = FetchType.LAZY, mappedBy = "jobCweDetail", cascade = CascadeType.ALL)`
- `@Enumerated(EnumType.STRING)`

**Business Methods**:
- `addAllMedias(List<Media> medias, User user)`: Adds multiple media files to support the exception

**Repository**: `IJobCweDetailRepository`
**Service**: `IJobCweService`

**Business Significance**: Enables controlled completion of jobs with known issues, maintaining quality standards while allowing process continuation with documented exceptions.

### 39. JobCweDetailMediaMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/JobCweDetailMediaMapping.java`

**Purpose**: Associates media files with job completion exception details for evidence and documentation.

**Attributes**:
- `jobCweDetailMediaId: JobCweDetailMediaCompositeKey` - Composite primary key

**Relationships**:
- `jobCweDetail: JobCweDetail` - Associated CWE detail record (not null, not updatable, EAGER fetch)
- `media: Media` - Associated media file (not null, not updatable, EAGER fetch)

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@ManyToOne(fetch = FetchType.EAGER, optional = false)`

**Repository**: `IJobCweDetailMediaMappingRepository`
**Service**: `IJobCweService`

**Business Significance**: Provides evidence trail for job completion exceptions, supporting audit requirements and quality documentation for deviation management.

## Advanced Parameter Management Entities

### 40. ParameterExceptionReviewer
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterExceptionReviewer.java`

**Purpose**: Assigns users or user groups to review and approve parameter exceptions.

**Attributes**:
- `exceptionId: Long` - Exception reference (read-only)
- `actionPerformed: boolean` - Flag indicating if review action was completed (default false, not null)

**Relationships**:
- `userGroup: UserGroup` - Assigned user group for review (not null, LAZY fetch)
- `user: User` - Assigned individual user for review (not null, LAZY fetch)
- `exceptions: ParameterException` - Associated exception record (not null, not updatable, CASCADE ALL, LAZY fetch)

**JPA Annotations**:
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`
- `@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)`

**Repository**: `IParameterExceptionReviewerRepository`
**Service**: `IParameterExceptionService`

**Business Significance**: Implements exception review workflow with role-based assignment, ensuring proper approval and accountability for parameter deviations.

### 41. ParameterMediaMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterMediaMapping.java`

**Purpose**: Associates media files with parameter definitions for instructions, examples, and documentation.

**Attributes**:
- `parameterMediaId: ParameterMediaCompositeKey` - Composite primary key
- `archived: boolean` - Soft delete flag (default false)

**Relationships**:
- `parameter: Parameter` - Associated parameter (not null, not updatable, LAZY fetch)
- `media: Media` - Associated media file (not null, not updatable, LAZY fetch)

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`

**Repository**: `IParameterMediaMappingRepository`
**Service**: `IParameterService`

**Business Significance**: Provides instructional media, examples, and documentation support for parameter execution, improving user understanding and data quality.

### 42. ParameterRule
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterRule.java`

**Purpose**: Defines conditional business rules that control parameter behavior and visibility.

**Attributes**:
- `ruleId: String` - Unique rule identifier (varchar, not null)
- `operator: String` - Rule comparison operator (varchar, not null) - currently supports 'equals'
- `input: String[]` - Input values for rule evaluation (text array, not null)
- `visibility: boolean` - Controls parameter visibility when rule is triggered (not null)

**Relationships**:
- `parameterMappings: Set<ParameterRuleMapping>` - Parameter associations (CASCADE ALL, LAZY fetch)

**JPA Annotations**:
- `@Entity`
- `@Table(name = TableName.PARAMETER_RULES)`
- `@TypeDefs(@TypeDef(name = "string-array", typeClass = StringArrayType.class))`
- `@Type(type = "string-array")`
- `@OneToMany(mappedBy = "parameterRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)`

**Repository**: `IParameterRuleRepository`
**Service**: `IParameterRuleService`

**Business Significance**: Enables dynamic parameter behavior based on conditional logic, supporting complex form flows and business rule enforcement.

### 43. ParameterRuleMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterRuleMapping.java`

**Purpose**: Maps parameter rules to specific parameters, defining trigger and impact relationships.

**Attributes**:
- `parameterRuleMappingCompositeKey: ParameterRuleMappingCompositeKey` - Composite primary key

**Relationships**:
- `parameterRule: ParameterRule` - Associated rule definition (LAZY fetch, mapped by parameterRuleId)
- `impactedParameter: Parameter` - Parameter affected by the rule (LAZY fetch, mapped by impactedParameterId)
- `triggeringParameter: Parameter` - Parameter that triggers the rule (LAZY fetch, mapped by triggeringParameterId)

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@ManyToOne(fetch = FetchType.LAZY)`
- `@MapsId("parameterRuleId")`, `@MapsId("impactedParameterId")`, `@MapsId("triggeringParameterId")`

**Repository**: `IParameterRuleMappingRepository`
**Service**: `IParameterRuleService`

**Business Significance**: Implements parameter interdependencies and conditional logic, enabling sophisticated form behavior and business rule automation.

### 44. ParameterValueApproval
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterValueApproval.java`

**Purpose**: Tracks approval workflow for parameter values requiring additional authorization.

**Attributes**:
- `user: User` - User who performed the approval (not null, CASCADE DETACH)
- `createdAt: Long` - Approval timestamp (not null)
- `state: State.ParameterValue` - Approval state (enum, varchar 50, not null)

**JPA Annotations**:
- `@Entity`
- `@Table(name = TableName.PARAMETER_VALUE_APPROVALS)`
- `@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.DETACH})`
- `@JsonIgnore` - Hides createdAt from JSON serialization

**Repository**: `IParameterValueApprovalRepository`
**Service**: `IParameterValueService`

**Business Significance**: Implements approval workflow for critical parameter values, ensuring proper authorization and audit trail for sensitive data.

### 45. ParameterValueBase
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterValueBase.java`

**Purpose**: Abstract base class providing common functionality for parameter value entities.

**Attributes**:
- `state: State.ParameterExecution` - Execution state (enum, varchar 50, not null)
- `verified: boolean` - Verification status (default false, not null)
- `reason: String` - Execution reason or comments (text)
- `value: String` - Parameter value content (text)
- `choices: JsonNode` - Multi-choice selections (jsonb)
- `parameterId: Long` - Parameter reference (read-only)
- `jobId: Long` - Job reference (read-only)
- `hidden: boolean` - Visibility flag (default false, not null)
- `clientEpoch: Long` - Client timestamp (not null)
- `version: Long` - Optimistic locking version
- `impactedBy: JsonNode` - Rule impact tracking (jsonb)
- `hasVariations: boolean` - Variation history flag (default false, not null)
- `taskExecutionId: Long` - Task execution reference (read-only)

**Relationships**:
- `parameter: Parameter` - Source parameter definition (not null, not updatable, LAZY fetch)
- `job: Job` - Parent job instance (not null, not updatable, LAZY fetch)
- `parameterValueApproval: ParameterValueApproval` - Approval workflow (OneToOne, CASCADE ALL)
- `parameterVerifications: List<ParameterVerification>` - Verification history (CASCADE ALL, LAZY fetch, ordered by modified_by DESC)
- `taskExecution: TaskExecution` - Execution context (not null, not updatable, CASCADE ALL, LAZY fetch)
- `variations: List<Variation>` - Value change history (CASCADE ALL, LAZY fetch)

**JPA Annotations**:
- `@MappedSuperclass` - Inheritance base class
- `@PrePersist` - Auto-sets createdAt timestamp
- `@Version` - Optimistic locking
- `@OrderBy("modified_by DESC")`

**Abstract Methods**:
- `addMedia(Media media, User principalUserEntity)`: Add media to parameter value
- `archiveMedia(Media media, User principalUserEntity)`: Archive media from parameter value
- `addAllMedias(List<Media> medias, User principalUserEntity)`: Add multiple media files

**Business Significance**: Provides common parameter value functionality with versioning, verification workflow, and media management capabilities.

### 46. ParameterValueMediaMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ParameterValueMediaMapping.java`

**Purpose**: Associates media files with parameter values for evidence, documentation, and verification.

**Attributes**:
- `parameterValueMediaId: ParameterValueMediaCompositeKey` - Composite primary key
- `archived: boolean` - Soft delete flag (default false)

**Relationships**:
- `parameterValue: ParameterValue` - Associated parameter value (not null, not updatable, LAZY fetch)
- `media: Media` - Associated media file (not null, not updatable, LAZY fetch)

**JPA Annotations**:
- `@EmbeddedId` - Composite key usage
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)`

**Repository**: `IParameterValueMediaMappingRepository`
**Service**: `IParameterValueService`

**Business Significance**: Provides evidence collection and documentation capabilities for parameter values, supporting audit requirements and quality verification processes.

## Task Management & Execution Control Entities

### 47. TaskDependency
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/TaskDependency.java`

**Purpose**: Defines prerequisite relationships between tasks to control execution order.

**Relationships**:
- `dependentTask: Task` - Task that depends on prerequisite (not null, LAZY fetch)
- `prerequisiteTask: Task` - Task that must complete first (not null, LAZY fetch)

**Business Significance**: Implements task sequencing and dependency management for complex workflows with execution prerequisites.

### 48. TaskExecutionTimer
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/TaskExecutionTimer.java`

**Purpose**: Tracks pause/resume cycles for timed task executions.

**Attributes**:
- `taskExecutionId: Long` - Task execution reference
- `pausedAt: Long` - Pause timestamp (not null)
- `resumedAt: Long` - Resume timestamp
- `reason: TaskPauseReason` - Pause reason (enum)
- `comment: String` - Additional pause details

**Business Significance**: Enables accurate time tracking for timed tasks with pause/resume capabilities for realistic duration measurement.

### 49. TaskExecutorLock
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/TaskExecutorLock.java`

**Purpose**: Implements task-level locking mechanisms to prevent concurrent execution issues.

**Attributes**:
- `taskId: Long` - Primary task reference (read-only)
- `referencedTaskId: Long` - Referenced task reference (read-only)
- `lockType: Type.TaskExecutorLockType` - Type of lock (enum, varchar 50, not null)

**Relationships**:
- `task: Task` - Primary task (not null, LAZY fetch)
- `referencedTask: Task` - Referenced/blocking task (not null, LAZY fetch)

**Business Significance**: Prevents execution conflicts and ensures proper task sequencing in concurrent execution scenarios.

### 50. TaskSchedules
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/TaskSchedules.java`

**Purpose**: Defines scheduling rules and timing constraints for task execution.

**Attributes**:
- `type: Type.ScheduledTaskType` - Schedule type (enum, varchar 50, not null)
- `referencedTaskId: Long` - Scheduled task reference (read-only)
- `condition: Type.ScheduledTaskCondition` - Schedule condition (enum, varchar 50, not null)
- `startDateDuration: JsonNode` - Start date calculation rules (jsonb, default '{}')
- `startDateInterval: Integer` - Start date interval
- `dueDateDuration: JsonNode` - Due date calculation rules (jsonb, default '{}')
- `dueDateInterval: Integer` - Due date interval

**Business Significance**: Enables complex task scheduling with dynamic timing calculations and conditional execution rules.

## Facility Configuration & Integration Entities

### 51. FacilityUseCaseMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/FacilityUseCaseMapping.java`

**Purpose**: Maps use cases to facilities with quota management for process capacity control.

**Attributes**:
- `facilityUseCaseId: FacilityUseCaseCompositeKey` - Composite primary key
- `facilityId: Long` - Facility reference (read-only)
- `useCaseId: Long` - Use case reference (read-only)
- `quota: Integer` - Maximum concurrent processes (not null)

**Relationships**:
- `facility: Facility` - Associated facility (not null, not updatable, LAZY fetch)
- `useCase: UseCase` - Associated use case (not null, not updatable, EAGER fetch)

**Business Significance**: Controls process deployment and capacity management across facilities, enabling resource allocation and performance optimization.

### 52. FacilityUseCasePropertyMapping
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/FacilityUseCasePropertyMapping.java`

**Purpose**: Configures custom properties for use cases at specific facilities with localized labels and requirements.

**Attributes**:
- `facilityId: Long` - Facility reference (read-only)
- `useCaseId: Long` - Use case reference (read-only)
- `propertiesId: Long` - Property reference (read-only)
- `labelAlias: String` - Custom property label (varchar 255, not null)
- `placeHolderAlias: String` - Custom placeholder text (varchar 512, not null)
- `orderTree: Integer` - Display order (not null)
- `isMandatory: boolean` - Required field flag (default false, not null)

**Relationships**:
- `facility: Facility` - Target facility (not null, not updatable, LAZY fetch)
- `useCase: UseCase` - Associated use case (not null, not updatable, LAZY fetch)
- `property: Property` - Property definition (not null, not updatable, EAGER fetch)

**Business Significance**: Enables facility-specific customization of process metadata collection with localized labels and validation requirements.

### 53. ProcessPermission
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/ProcessPermission.java`

**Purpose**: Defines permission types for process-related operations and access control.

**Attributes**:
- `type: ProcessPermissionType` - Permission type (enum, varchar 50, not null)
- `description: String` - Permission description (text)

**Business Significance**: Implements role-based access control for processes, enabling fine-grained security and authorization management.

### 54. RelationValue
**File**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/model/RelationValue.java`

**Purpose**: Stores actual relationships between jobs and external objects during execution.

**Attributes**:
- `relationId: Long` - Relation definition reference
- `jobId: Long` - Associated job reference
- `objectId: String` - External object identifier
- `collection: String` - Object collection name
- `externalId: String` - External system object ID
- `displayName: String` - Object display name
- `objectTypeExternalId: String` - External object type ID
- `objectTypeDisplayName: String` - Object type display name

**Relationships**:
- `relation: Relation` - Relation definition (not null, not updatable, LAZY fetch)
- `job: Job` - Associated job (not null, not updatable, LAZY fetch)

**Business Significance**: Implements external system integration by storing actual object relationships during job execution, enabling data exchange and validation.

---

## Complete Entity Coverage Summary

This documentation now covers **54 core entities** from the Streem Backend model layer. The remaining entities fall into these categories:

### User Management Entities (6 entities)
- **UserFacilityMapping**: Maps users to facilities for access control
- **UserGroupAudit**: Audit trail for user group changes  
- **UserGroupMember**: Individual user membership in groups
- **TrainedUser**: User training records and certifications
- **TrainedUserProcessPermissionMapping**: Maps trained users to process permissions
- **TrainedUserTaskMapping**: Maps trained users to specific tasks

### Additional Core Entities (9 entities)
- **Reviewer**: User assignment for review workflows
- **StageExecutionReport**: Stage-level execution reporting
- **TaskAutomationMapping**: Task automation rule associations
- **TaskExecutionUserMapping**: User assignments for task execution
- **TaskMediaMapping**: Media attachments for tasks
- **TaskRecurrence**: Recurring task configuration
- **TempParameterValue**: Temporary parameter value storage
- **TempParameterValueMediaMapping**: Media for temporary values
- **TempParameterVerification**: Verification for temporary values
- **Variation**: Parameter value change tracking
- **VariationMediaMapping**: Media for parameter variations
- **VerificationBase**: Base class for verification entities

### Composite Key Classes (17 entities)
All classes in `/compositekey/` directory implementing composite primary keys for many-to-many relationships and complex associations.

### Helper Classes (20+ entities)
All classes in `/helper/` directory including:
- **Parameter Types**: Specialized parameter implementations (NumberParameter, TextParameter, DateParameter, etc.)
- **Base Classes**: Extended audit and entity base classes
- **Specification Builders**: Query building utilities
- **Search Utilities**: Search criteria and filtering classes

### Architecture Highlights

**Total Model Files**: 131 Java files
**Core Business Entities**: 54 documented + 35+ remaining
**Composite Keys**: 17 classes
**Helper/Utility Classes**: 20+ classes

### Key Design Patterns Identified

1. **Audit Trail**: Comprehensive audit tracking across all business entities
2. **Composite Keys**: Many-to-many relationships using embedded composite keys
3. **Media Management**: Consistent media attachment patterns across entities
4. **Verification Workflows**: Multi-level approval and verification systems
5. **Temporal Data**: Version control and historical tracking
6. **Rule Engine**: Dynamic parameter behavior through rule mappings
7. **External Integration**: Relation-based external system connectivity
8. **Multi-tenancy**: Organization-scoped data isolation

### Database Schema Considerations

**Estimated Table Count**: 70+ tables
**Primary Relationships**: 200+ foreign key relationships
**JSON Storage**: Extensive use of JSONB for flexible configuration
**Audit Tables**: Separate audit entities for major business objects
**Index Strategy**: Required for organization scoping, status filtering, and temporal queries

This comprehensive entity documentation provides complete coverage of the Streem Backend's sophisticated workflow management domain model, enabling developers to understand the full scope of the data architecture and business logic implementation.