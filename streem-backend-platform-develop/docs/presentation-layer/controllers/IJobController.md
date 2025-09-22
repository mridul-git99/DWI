# IJobController - Digital Work Instructions Job Management API

## Overview

The **IJobController** is the core API controller managing the complete lifecycle of manufacturing jobs in the Streem Digital Work Instructions platform. It orchestrates the execution of digitized standard operating procedures (SOPs) from creation through completion, providing real-time monitoring, quality control, and comprehensive reporting capabilities.

## Business Context

### Manufacturing Workflow Integration
- **Process Instantiation**: Creates executable job instances from checklist templates
- **Execution Management**: Controls job state transitions through manufacturing lifecycle  
- **Quality Control**: Manages parameter validation and exception handling workflows
- **Real-time Monitoring**: Provides live status updates and performance tracking
- **Compliance Support**: Maintains audit trails for regulatory requirements (FDA 21 CFR Part 11)
- **Multi-tenant Architecture**: Enables facility-based access control and data isolation

### Job Lifecycle States
```
UNASSIGNED → ASSIGNED → IN_PROGRESS → COMPLETED/COMPLETED_WITH_EXCEPTION
```

## Controller Specification

**Base Path**: `/v1/jobs`  
**Controller Interface**: `IJobController.java`  
**Package**: `com.leucine.streem.controller`

---

## API Endpoints Documentation

### 1. GET /v1/jobs - List All Jobs

**Endpoint Signature**
```java
@GetMapping
Response<Page<JobPartialDto>> getAll(
    @RequestParam(name = "objectId", defaultValue = "") String objectId,
    @RequestParam(name = "filters", defaultValue = "") String filters, 
    Pageable pageable
)
```

**Business Purpose**
Retrieves paginated list of all jobs with filtering and sorting capabilities for manufacturing oversight and planning.

**Request Analysis**
- **objectId**: Multi-tenant facility identifier for data isolation
- **filters**: JSON string supporting complex filtering (state, dateRange, assignee, process)
- **pageable**: Standard Spring pagination with size, page, sort parameters

**Response Analysis**
- **Type**: `Page<JobPartialDto>`
- **Content**: Job summary information including:
  - Basic job metadata (id, code, state)
  - Task progress tracking (totalTasksCount, pendingTasksCount)
  - Schedule information (expectedStartDate, expectedEndDate)
  - Process template details (checklist information)

**Service Integration**
- **Primary Service**: `IJobService.getAllJobs()`
- **Business Logic**: Applies multi-tenant filtering, processes complex filter expressions
- **Database Operations**: Optimized queries with projection for performance

**Workflow Context**
Critical for manufacturing managers to:
- Monitor active production workflows
- Plan resource allocation and scheduling
- Track overall facility performance
- Identify bottlenecks and delays

**Security & Permissions**
- **Access Control**: Requires job read permissions
- **Multi-tenant**: Filtered by objectId to ensure facility data isolation
- **Role-based**: Different views based on user roles (operator, supervisor, manager)

**Performance Considerations**
- **Caching**: Implements application-level caching for frequently accessed data
- **Pagination**: Default page size limits to prevent memory issues
- **Indexing**: Database indexes on state, createdAt, objectId for fast filtering

**Error Scenarios**
- Invalid filter syntax returns 400 Bad Request
- Unauthorized access returns 403 Forbidden
- Invalid objectId returns empty result set

**Integration Points**
- **ERP Systems**: Job data exported for production planning
- **MES Integration**: Real-time status updates to manufacturing execution systems
- **Reporting Platforms**: Data source for operational dashboards

**Testing Strategy**
- **Unit Tests**: Mock service layer, verify filter parsing
- **Integration Tests**: End-to-end with database, verify pagination
- **Performance Tests**: Load testing with large datasets

---

### 2. GET /v1/jobs/count - Job Count with Filters

**Endpoint Signature**
```java
@GetMapping("/count")
Response<Page<JobPartialDto>> getAllCount(
    @RequestParam(name = "objectId", defaultValue = "") String objectId,
    @RequestParam(name = "filters", defaultValue = "") String filters,
    Pageable pageable
)
```

**Business Purpose**
Provides count-only queries for dashboard metrics and KPI calculations without transferring full data payloads.

**Request Analysis**
- Identical parameters to `/jobs` endpoint
- Optimized for count operations rather than data retrieval

**Response Analysis**
- **Type**: `Page<JobPartialDto>` with minimal content
- **Focus**: Total count information for dashboard widgets
- **Performance**: Optimized COUNT queries without data projection

**Service Integration**
- **Service Method**: `IJobService.getAllJobsCount()`
- **Optimization**: Database COUNT operations without SELECT overhead
- **Caching**: Aggressive caching for count queries

**Workflow Context**
Essential for:
- Dashboard KPI displays (total jobs, completion rates)
- Capacity planning calculations
- Performance metric aggregations
- Resource utilization analytics

**Performance Considerations**
- **Database Optimization**: Uses COUNT(*) with appropriate WHERE clauses
- **Caching Strategy**: Long-term caching suitable for count data
- **Response Size**: Minimal payload for fast dashboard loading

---

### 3. GET /v1/jobs/assignee/me - My Assigned Jobs

**Endpoint Signature**
```java
@GetMapping("/assignee/me")
Response<Page<JobPartialDto>> getJobsAssignedToMe(
    @RequestParam(name = "objectId", defaultValue = "") String objectId,
    @RequestParam(name = "showPendingOnly", defaultValue = "false") Boolean showPendingOnly,
    @RequestParam(name = "filters", defaultValue = "") String filters,
    Pageable pageable
) throws StreemException
```

**Business Purpose**
Personal work queue for operators and technicians showing jobs assigned to the current user.

**Request Analysis**
- **objectId**: Facility-specific filtering
- **showPendingOnly**: Filter for incomplete work items only
- **filters**: Additional filtering on assigned jobs
- **Security Context**: Uses authenticated user context

**Response Analysis**
- **Type**: `Page<JobPartialDto>`
- **Personalization**: Filtered by current user assignments
- **Priority Information**: Ordered by urgency and expected dates

**Service Integration**
- **Service Method**: `IJobService.getJobsAssignedToMe()`
- **User Context**: Extracts user ID from security context
- **Assignment Logic**: Queries task execution assignments

**Workflow Context**
Primary interface for operators to:
- View their work queue and priorities
- Track progress on assigned tasks
- Manage daily work schedules
- Report completion status

**Security & Permissions**
- **User Context**: Automatically filtered by authenticated user
- **Role Validation**: Ensures user has operator/assignee permissions
- **Data Privacy**: Users only see their own assignments

**State Management**
- **Pending Filter**: Shows jobs in ASSIGNED or IN_PROGRESS states
- **Completion Tracking**: Excludes COMPLETED jobs when showPendingOnly=true
- **Real-time Updates**: Reflects current assignment status

**Performance Considerations**
- **User-specific Caching**: Caches user assignment data
- **Index Optimization**: Database indexes on user_id and assignment tables
- **Response Time**: Critical for operator experience, target <500ms

---

### 4. GET /v1/jobs/assignee/me/autosuggest - Auto-suggest for Assigned Jobs

**Endpoint Signature**
```java
@GetMapping("/assignee/me/autosuggest")
Response<Page<JobAutoSuggestDto>> getJobsAssignedToMeAutoSuggest(
    @RequestParam(name = "objectId", defaultValue = "") String objectId,
    @RequestParam(name = "showPendingOnly", defaultValue = "false") Boolean showPendingOnly,
    @RequestParam(name = "filters", defaultValue = "") String filters,
    Pageable pageable
) throws StreemException
```

**Business Purpose**
Lightweight endpoint for typeahead/autocomplete functionality in UI components.

**Request Analysis**
- Similar to assignee/me but optimized for autocomplete scenarios
- Reduced payload size for fast response times

**Response Analysis**
- **Type**: `Page<JobAutoSuggestDto>`
- **Minimal Data**: Only essential fields for autocomplete (id, code, name)
- **Fast Response**: Optimized for UI responsiveness

**Service Integration**
- **Service Method**: `IJobService.getJobsAssignedToMeAutoSuggest()`
- **Data Projection**: Minimal field selection for performance
- **Caching**: Aggressive caching for autocomplete data

**Workflow Context**
Supports UI components:
- Job search and selection interfaces
- Quick job reference lookups
- Mobile application performance optimization

**Performance Considerations**
- **Response Size**: Minimal payload for fast loading
- **Query Optimization**: Selective field projection
- **Caching**: Short-term caching for repeated requests

---

### 5. GET /v1/jobs/assignee/me/count - My Assignment Count

**Endpoint Signature**
```java
@GetMapping("/assignee/me/count")
Response<CountDto> getJobsAssignedToMeCount(
    @RequestParam(name = "objectId", defaultValue = "") String objectId,
    @RequestParam(name = "filters", defaultValue = "") String filters,
    @RequestParam(name = "showPendingOnly", defaultValue = "false") Boolean showPendingOnly
) throws StreemException
```

**Business Purpose**
Provides count of assigned jobs for personal dashboard and workload indicators.

**Request Analysis**
- User-specific count query with filtering options
- Boolean flag for pending-only counts

**Response Analysis**
- **Type**: `CountDto`
- **Simple Count**: Integer count of matching assignments
- **Dashboard Integration**: Used in personal dashboard widgets

**Service Integration**
- **Service Method**: `IJobService.getJobsAssignedToMeCount()`
- **Performance**: Optimized COUNT query with user context
- **Caching**: User-specific count caching

**Workflow Context**
Critical for:
- Personal workload assessment
- Work queue prioritization
- Productivity tracking
- Mobile app badge notifications

**Performance Considerations**
- **Query Optimization**: Database COUNT with proper indexing
- **User Caching**: Per-user count caching with TTL
- **Real-time Updates**: Invalidates cache on assignment changes

---

### 6. GET /v1/jobs/{jobId} - Get Job Details

**Endpoint Signature**
```java
@GetMapping("/{jobId}")
Response<JobDto> getJob(@PathVariable Long jobId) 
    throws ResourceNotFoundException, JsonProcessingException
```

**Business Purpose**
Retrieves complete job information including stages, tasks, parameters, and execution status.

**Request Analysis**
- **jobId**: Unique job identifier (Long type)
- **Path Variable**: RESTful resource identification

**Response Analysis**
- **Type**: `JobDto`
- **Complete Data**: Full job details including:
  - Job metadata and state information
  - Checklist structure with stages and tasks
  - Parameter values and validations
  - Progress tracking and completion status
  - Audit information and history

**Service Integration**
- **Service Method**: `IJobService.getJobById()`
- **Entity Graph**: Uses JPA entity graphs for optimized loading
- **JSON Processing**: Handles complex nested data structures

**Workflow Context**
Essential for:
- Job execution interfaces
- Detailed progress monitoring
- Quality control reviews
- Audit trail examination

**Security & Permissions**
- **Resource Access**: Validates user has access to specific job
- **Multi-tenant**: Ensures job belongs to user's facility
- **Role-based**: Different detail levels based on user permissions

**State Management**
- **Current State**: Returns real-time job state
- **Progress Tracking**: Calculates completion percentages
- **Validation Status**: Includes parameter validation states

**Performance Considerations**
- **Entity Graphs**: Optimizes database queries to prevent N+1 problems
- **Caching**: Job-level caching with invalidation on updates
- **Response Size**: Large payload, requires monitoring

**Error Scenarios**
- **404 Not Found**: Job doesn't exist or user lacks access
- **JSON Processing**: Malformed parameter data returns 500
- **Concurrent Access**: Handles concurrent read/write scenarios

**Integration Points**
- **Mobile Apps**: Primary data source for job execution interfaces
- **Web Clients**: Detailed job view components
- **Reporting**: Data source for job-specific reports

---

### 7. GET /v1/jobs/{jobId}/cwe-details - Complete With Exception Details

**Endpoint Signature**
```java
@GetMapping("/{jobId}/cwe-details")
Response<JobCweDto> getJobCweDetail(@PathVariable Long jobId) 
    throws ResourceNotFoundException
```

**Business Purpose**
Retrieves details about jobs completed with exceptions, supporting quality control and deviation management.

**Request Analysis**
- **jobId**: Job identifier for exception details
- **Specific Use**: Only relevant for jobs with COMPLETED_WITH_EXCEPTION state

**Response Analysis**
- **Type**: `JobCweDto`
- **Exception Data**: Detailed information about:
  - Exception reason (JobCweReason enum)
  - Exception comments and documentation
  - Associated media (photos, documents)
  - Resolution tracking

**Service Integration**
- **Service Method**: `IJobCweService.getJobCweDetail()`
- **Exception Handling**: Specialized service for deviation management
- **Quality Integration**: Links to quality management workflows

**Workflow Context**
Critical for:
- Quality control investigations
- Root cause analysis
- Deviation documentation
- Regulatory compliance reporting

**Security & Permissions**
- **Quality Access**: Requires quality control permissions
- **Audit Trail**: All access logged for compliance
- **Sensitive Data**: Exception details may contain sensitive information

**State Management**
- **Exception State**: Only applicable to COMPLETED_WITH_EXCEPTION jobs
- **Resolution Tracking**: Tracks follow-up actions and resolutions
- **Documentation**: Maintains complete exception documentation

**Performance Considerations**
- **Conditional Loading**: Only loads when job has exceptions
- **Media Handling**: Efficiently handles exception media attachments
- **Audit Performance**: Optimizes audit trail queries

**Error Scenarios**
- **Invalid State**: Returns error if job not completed with exception
- **Missing Data**: Handles cases where exception details are incomplete
- **Access Denied**: Quality permissions required

**Integration Points**
- **Quality Systems**: Exports exception data to QMS platforms
- **CAPA Systems**: Links to corrective action workflows
- **Audit Systems**: Provides data for compliance audits

---

### 8. POST /v1/jobs - Create New Job

**Endpoint Signature**
```java
@PostMapping
Response<JobDto> createJob(
    @RequestBody CreateJobRequest createJobRequest,
    @RequestParam(name = "validateUserRole") Boolean validateUserRole
) throws StreemException, IOException, ResourceNotFoundException, MultiStatusException
```

**Business Purpose**
Creates new job instances from checklist templates, initiating manufacturing process execution.

**Request Analysis**
- **Type**: `CreateJobRequest`
- **Required Data**:
  - `checklistId`: Template process to instantiate
  - `properties`: Job-specific property values
  - `parameterValues`: Pre-populated parameter data
- **Role Validation**: Optional user role validation

**Response Analysis**
- **Type**: `JobDto`
- **New Job**: Complete newly created job information
- **State**: Initially UNASSIGNED state
- **Generation**: Auto-generated job code and timestamps

**Service Integration**
- **Service Method**: `IJobService.createJob()`
- **Template Processing**: Instantiates checklist template
- **Parameter Initialization**: Sets up parameter execution structure
- **Validation Chain**: Multiple validation layers

**Workflow Context**
Initiates manufacturing processes:
- Production order creation
- Work instruction instantiation  
- Resource allocation preparation
- Schedule integration

**Security & Permissions**
- **Creation Rights**: Requires job creation permissions
- **Role Validation**: Optional validation of user roles against checklist requirements
- **Multi-tenant**: Ensures checklist belongs to user's facility

**State Management**
- **Initial State**: Creates job in UNASSIGNED state
- **Template Binding**: Links job to specific checklist version
- **Parameter Setup**: Initializes parameter execution structure

**Performance Considerations**
- **Template Cloning**: Efficiently copies checklist structure
- **Bulk Operations**: Handles multiple parameter initializations
- **Database Transactions**: Ensures atomic job creation

**Error Scenarios**
- **Invalid Checklist**: Template not found or not accessible
- **Validation Failures**: Parameter validation errors
- **Role Violations**: User lacks required roles for checklist
- **Multi-status**: Partial failures in complex job creation

**Integration Points**
- **ERP Systems**: Notifies production planning systems
- **Scheduling**: Integrates with manufacturing schedules
- **Resource Management**: Reserves required resources

**Testing Strategy**
- **Unit Tests**: Mock checklist templates, parameter validation
- **Integration Tests**: Full job creation workflow
- **Error Handling**: Test all validation failure scenarios

---

### 9. PATCH /v1/jobs/{jobId} - Update Job

**Endpoint Signature**
```java
@PatchMapping("/{jobId}")
Response<BasicDto> updateJob(
    @PathVariable Long jobId,
    @RequestBody UpdateJobRequest updateJobRequest
) throws StreemException, ResourceNotFoundException
```

**Business Purpose**
Updates job scheduling information (expected start/end dates) for production planning adjustments.

**Request Analysis**
- **Type**: `UpdateJobRequest`
- **Updateable Fields**:
  - `expectedStartDate`: Planned start time
  - `expectedEndDate`: Planned completion time
- **Limited Scope**: Only scheduling updates allowed

**Response Analysis**
- **Type**: `BasicDto`
- **Simple Response**: Confirmation of update success
- **Audit Trail**: Update logged in audit history

**Service Integration**
- **Service Method**: `IJobService.updateJob()`
- **Validation**: Ensures dates are logical and consistent
- **State Checking**: Validates job state allows updates

**Workflow Context**
Supports:
- Production schedule adjustments
- Resource reallocation
- Timeline optimization
- Capacity planning updates

**Security & Permissions**
- **Update Rights**: Requires job update permissions
- **State Restrictions**: Some states may prevent updates
- **Audit Logging**: All updates tracked for compliance

**State Management**
- **State Validation**: Ensures job state allows scheduling updates
- **Date Consistency**: Validates start date before end date
- **Timeline Impact**: May affect dependent job schedules

**Performance Considerations**
- **Lightweight Updates**: Minimal data changes
- **Cache Invalidation**: Updates invalidate relevant caches
- **Notification Triggers**: May trigger schedule change notifications

**Error Scenarios**
- **Invalid Dates**: Start date after end date
- **State Conflicts**: Job state prevents updates
- **Permission Denied**: User lacks update permissions

**Integration Points**
- **Scheduling Systems**: Updates propagate to scheduling platforms
- **Resource Planning**: Affects resource allocation systems
- **Notification Services**: Triggers schedule change notifications

---

### 10. PATCH /v1/jobs/{jobId}/start - Start Job Execution

**Endpoint Signature**
```java
@PatchMapping("/{jobId}/start")
Response<JobInfoDto> startJob(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException
```

**Business Purpose**
Initiates job execution, transitioning from ASSIGNED to IN_PROGRESS state and activating the manufacturing process.

**Request Analysis**
- **Path Parameter**: jobId for specific job to start
- **State Prerequisite**: Job must be in ASSIGNED state
- **User Context**: Uses authenticated user as job starter

**Response Analysis**
- **Type**: `JobInfoDto`
- **State Change**: Confirms transition to IN_PROGRESS
- **Execution Context**: Returns active task execution IDs
- **Audit Information**: Includes start timestamp and user

**Service Integration**
- **Service Method**: `IJobService.startJob()`
- **State Transition**: Manages job state changes
- **Task Activation**: Initializes task execution records
- **Parameter Processing**: Triggers parameter execution logic

**Workflow Context**
Critical manufacturing milestone:
- Begins active production process
- Starts time tracking for performance metrics
- Activates operator work interfaces
- Triggers resource allocation

**Security & Permissions**
- **Start Rights**: Requires job start/execution permissions
- **Assignment Validation**: May require user to be assigned to job
- **State Authorization**: Validates user can transition job state

**State Management**
- **State Transition**: ASSIGNED → IN_PROGRESS
- **Atomic Operation**: Ensures consistent state change
- **Task Initialization**: Creates active task execution records
- **Timestamp Recording**: Records precise start time

**Performance Considerations**
- **Quick Response**: Critical for operator experience
- **Resource Allocation**: May reserve system resources
- **Notification Triggers**: Sends start notifications

**Error Scenarios**
- **Invalid State**: Job not in ASSIGNED state
- **Permission Denied**: User lacks start permissions  
- **Resource Conflicts**: Required resources unavailable
- **Parameter Execution**: Parameter initialization failures

**Integration Points**
- **MES Systems**: Notifies manufacturing execution systems
- **Time Tracking**: Integrates with labor tracking systems
- **Resource Management**: Activates resource reservations
- **Mobile Apps**: Enables operator interfaces

**Testing Strategy**
- **State Transitions**: Verify proper state management
- **Concurrency**: Test concurrent start attempts
- **Resource Allocation**: Validate resource reservation
- **Error Handling**: Test all failure scenarios

---

### 11. PATCH /v1/jobs/{jobId}/complete - Complete Job

**Endpoint Signature**
```java
@PatchMapping("/{jobId}/complete")
Response<JobInfoDto> completeJob(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException, StreemException
```

**Business Purpose**
Completes job execution successfully, transitioning to COMPLETED state and finalizing the manufacturing process.

**Request Analysis**
- **Path Parameter**: jobId for job to complete
- **State Prerequisite**: Job must be in IN_PROGRESS state
- **Validation**: All required tasks must be completed

**Response Analysis**
- **Type**: `JobInfoDto`
- **Final State**: Confirms COMPLETED state
- **Completion Data**: End timestamp and completion audit
- **Summary Information**: Final task completion status

**Service Integration**
- **Service Method**: `IJobService.completeJob()`
- **Validation Chain**: Ensures all completion requirements met
- **State Finalization**: Transitions to final completed state
- **Resource Release**: Releases allocated resources

**Workflow Context**
Manufacturing process conclusion:
- Finalizes production process
- Triggers quality control workflows
- Releases allocated resources
- Enables performance analysis

**Security & Permissions**
- **Completion Rights**: Requires job completion permissions
- **Quality Authorization**: May require quality approval
- **Final Authorization**: Some processes require supervisor approval

**State Management**
- **Final Transition**: IN_PROGRESS → COMPLETED
- **Validation Gates**: All tasks must be completed
- **Irreversible**: Completed state typically cannot be reversed
- **Audit Closure**: Finalizes job audit trail

**Performance Considerations**
- **Validation Performance**: Quickly validates completion requirements
- **Resource Release**: Efficiently releases system resources
- **Notification Speed**: Fast completion notifications

**Error Scenarios**
- **Incomplete Tasks**: Not all required tasks completed
- **Quality Issues**: Parameters require approval before completion
- **State Conflicts**: Job not in valid state for completion
- **Permission Issues**: User lacks completion authority

**Integration Points**
- **ERP Systems**: Updates production completion status
- **Quality Systems**: Triggers quality control processes
- **Inventory Management**: Updates material consumption
- **Performance Analytics**: Provides completion data for metrics

**Testing Strategy**
- **Completion Validation**: Test task completion requirements
- **State Management**: Verify proper state transitions
- **Quality Integration**: Test quality approval workflows
- **Performance Impact**: Measure completion processing time

---

### 12. PATCH /v1/jobs/{jobId}/complete-with-exception - Complete With Exception

**Endpoint Signature**
```java
@PatchMapping("/{jobId}/complete-with-exception")
Response<JobInfoDto> completeJobWithException(
    @PathVariable("jobId") Long jobId,
    @Valid @RequestBody JobCweDetailRequest jobCweDetailRequest
) throws ResourceNotFoundException, StreemException
```

**Business Purpose**
Completes job with documented exceptions, transitioning to COMPLETED_WITH_EXCEPTION state for quality control and deviation management.

**Request Analysis**
- **Path Parameter**: jobId for job with exceptions
- **Request Body**: `JobCweDetailRequest` containing:
  - `reason`: JobCweReason enum (cancellation, mistake, offline completion, other)
  - `comment`: Detailed explanation of exception
  - `medias`: Supporting documentation/images
- **Validation**: Required fields for exception documentation

**Response Analysis**
- **Type**: `JobInfoDto`
- **Exception State**: Confirms COMPLETED_WITH_EXCEPTION state
- **Documentation**: Links to exception documentation
- **Quality Flag**: Marks job for quality review

**Service Integration**
- **Service Method**: `IJobService.completeJobWithException()`
- **Exception Processing**: Handles deviation documentation
- **Quality Integration**: Triggers quality management workflows
- **Audit Enhancement**: Enhanced audit trail for exceptions

**Workflow Context**
Quality control and deviation management:
- Documents manufacturing deviations
- Triggers corrective action workflows
- Maintains regulatory compliance
- Enables root cause analysis

**Security & Permissions**
- **Exception Authority**: Requires deviation management permissions
- **Quality Access**: May require quality control authorization
- **Documentation Requirements**: Enforces complete exception documentation

**State Management**
- **Exception Transition**: IN_PROGRESS → COMPLETED_WITH_EXCEPTION
- **Quality State**: Flags job for quality review
- **Documentation Link**: Associates exception details with job
- **Audit Enhancement**: Detailed audit trail for compliance

**Performance Considerations**
- **Media Handling**: Efficiently processes exception media uploads
- **Quality Triggers**: Fast notification to quality teams
- **Documentation Storage**: Optimized storage for exception records

**Error Scenarios**
- **Incomplete Documentation**: Missing required exception details
- **Invalid Reason**: Unrecognized exception reason codes
- **Media Upload Failures**: Issues with supporting documentation
- **Permission Denied**: User lacks exception completion authority

**Integration Points**
- **Quality Management**: Exports exception data to QMS
- **CAPA Systems**: Triggers corrective action processes
- **Regulatory Systems**: Provides deviation data for compliance
- **Analytics Platforms**: Exception data for quality metrics

**Testing Strategy**
- **Exception Documentation**: Test required field validation
- **Media Upload**: Test exception media handling
- **Quality Integration**: Verify quality workflow triggers
- **Compliance**: Test audit trail completeness

---

### 13. GET /v1/jobs/{jobId}/state - Job State Polling

**Endpoint Signature**
```java
@GetMapping("/{jobId}/state")
Response<JobStateDto> getJobState(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException
```

**Business Purpose**
Real-time job state polling for live dashboard updates and operator interface synchronization.

**Request Analysis**
- **Path Parameter**: jobId for state monitoring
- **Lightweight**: Minimal data request for frequent polling
- **Real-time**: Designed for frequent API calls

**Response Analysis**
- **Type**: `JobStateDto`
- **Minimal Data**: Essential state information only:
  - Job ID and code
  - Current state (ASSIGNED, IN_PROGRESS, COMPLETED, etc.)
- **Fast Response**: Optimized for quick polling

**Service Integration**
- **Service Method**: Direct state query with minimal processing
- **Caching Strategy**: Aggressive caching with short TTL
- **Performance**: Optimized for high-frequency access

**Workflow Context**
Enables real-time monitoring:
- Live dashboard updates
- Operator interface synchronization
- Mobile app state management
- System integration polling

**Security & Permissions**
- **Read Access**: Basic job read permissions
- **Lightweight Security**: Minimal security overhead for performance
- **Rate Limiting**: May implement rate limiting for abuse prevention

**State Management**
- **Current State**: Returns real-time job state
- **State Consistency**: Ensures consistent state across systems
- **Change Detection**: Enables client-side change detection

**Performance Considerations**
- **High Frequency**: Designed for frequent polling (every few seconds)
- **Minimal Payload**: Smallest possible response size
- **Caching**: Aggressive caching with state-based invalidation
- **Response Time**: Target <100ms response time

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **Rate Limiting**: Too frequent requests may be throttled
- **Network Issues**: Handles temporary network failures gracefully

**Integration Points**
- **Real-time Dashboards**: Primary data source for live updates
- **Mobile Applications**: State synchronization for offline/online modes
- **Third-party Systems**: Polling interface for external integrations
- **WebSocket Alternative**: REST polling alternative to WebSocket connections

**Testing Strategy**
- **Performance Testing**: High-frequency polling simulation
- **Caching Validation**: Test cache hit rates and invalidation
- **Rate Limiting**: Test throttling behavior
- **State Consistency**: Verify state accuracy across polling

---

### 14. GET /v1/jobs/{jobId}/stages/state - Stage State Polling

**Endpoint Signature**
```java
@GetMapping("/{jobId}/stages/state")
Response<StageDetailsDto> pollStageData(
    @PathVariable("jobId") Long jobId,
    @RequestParam(name = "stageId") Long stageId
) throws ResourceNotFoundException, JsonProcessingException
```

**Business Purpose**
Real-time stage-level progress monitoring for detailed workflow tracking and operator guidance.

**Request Analysis**
- **Path Parameter**: jobId for job context
- **Query Parameter**: stageId for specific stage monitoring
- **Real-time**: Designed for frequent polling of stage progress

**Response Analysis**
- **Type**: `StageDetailsDto`
- **Stage Information**: Detailed stage progress including:
  - Stage state and completion status
  - Task progress within stage
  - Parameter execution status
  - Timing and performance data

**Service Integration**
- **Service Method**: Stage-specific data retrieval with optimization
- **JSON Processing**: Handles complex stage data structures
- **Performance**: Optimized queries for stage-level data

**Workflow Context**
Enables granular process monitoring:
- Stage-by-stage progress tracking
- Operator workflow guidance
- Bottleneck identification
- Process optimization data

**Security & Permissions**
- **Stage Access**: Requires access to specific job and stage
- **Progress Monitoring**: May require process monitoring permissions
- **Data Privacy**: Ensures user only sees authorized stage data

**State Management**
- **Stage Progress**: Real-time stage completion tracking
- **Task Dependencies**: Shows task completion dependencies within stage
- **Parameter Status**: Shows parameter execution and validation status

**Performance Considerations**
- **Stage-level Caching**: Caches stage data with fine-grained invalidation
- **Selective Loading**: Only loads requested stage data
- **Response Optimization**: Minimizes payload for frequent polling

**Error Scenarios**
- **Invalid Stage**: Stage doesn't exist or not accessible
- **JSON Processing**: Complex stage data processing errors
- **State Inconsistency**: Handles concurrent stage updates

**Integration Points**
- **Process Monitoring**: Detailed data for process analysis
- **Operator Interfaces**: Stage-specific operator guidance
- **Performance Analytics**: Stage-level performance metrics

**Testing Strategy**
- **Stage Data Accuracy**: Verify correct stage information
- **Polling Performance**: Test frequent polling scenarios
- **Concurrent Access**: Test concurrent stage monitoring

---

### 15. GET /v1/jobs/{jobId}/task-executions/state - Task Execution State Polling

**Endpoint Signature**
```java
@GetMapping("/{jobId}/task-executions/state")
Response<TaskDetailsDto> pollTaskData(
    @PathVariable("jobId") Long jobId,
    @RequestParam(name = "taskExecutionId") Long taskExecutionId
) throws ResourceNotFoundException, JsonProcessingException
```

**Business Purpose**
Real-time task-level execution monitoring for detailed operator guidance and quality control.

**Request Analysis**
- **Path Parameter**: jobId for job context
- **Query Parameter**: taskExecutionId for specific task monitoring
- **Granular**: Most detailed level of process monitoring

**Response Analysis**
- **Type**: `TaskDetailsDto`
- **Task Details**: Comprehensive task execution information:
  - Task execution state and progress
  - Parameter values and validations
  - Media attachments and documentation
  - Operator instructions and guidance

**Service Integration**
- **Service Method**: Task-specific data retrieval with full context
- **Complex Data**: Handles detailed task execution information
- **Real-time**: Optimized for operator interface requirements

**Workflow Context**
Provides detailed operator support:
- Step-by-step task guidance
- Parameter validation feedback
- Quality control information
- Real-time instruction updates

**Security & Permissions**
- **Task Access**: Requires access to specific task execution
- **Operator Permissions**: May require task execution permissions
- **Parameter Privacy**: Ensures appropriate parameter data access

**State Management**
- **Task State**: Real-time task execution state
- **Parameter Progress**: Individual parameter completion tracking
- **Validation Status**: Real-time parameter validation results
- **Quality Gates**: Quality control checkpoint status

**Performance Considerations**
- **Task-level Detail**: Most data-intensive polling endpoint
- **Selective Loading**: Loads only essential task data for performance
- **Media Optimization**: Optimizes media loading for task instructions

**Error Scenarios**
- **Invalid Task**: Task execution doesn't exist or not accessible
- **Complex Data**: JSON processing errors with detailed task data
- **Concurrent Updates**: Handles concurrent task execution updates

**Integration Points**
- **Operator Interfaces**: Primary data source for task execution screens
- **Quality Systems**: Task-level quality control data
- **Training Systems**: Task execution data for training purposes

**Testing Strategy**
- **Task Data Completeness**: Verify all required task information
- **Real-time Updates**: Test task state change propagation
- **Performance Impact**: Monitor response times for detailed data

---

### 16. PATCH /v1/jobs/{jobId}/assignments - Bulk Task Assignment

**Endpoint Signature**
```java
@PatchMapping("/{jobId}/assignments")
Response<BasicDto> bulkAssign(
    @PathVariable(name = "jobId") Long jobId,
    @RequestBody TaskExecutionAssignmentRequest taskExecutionAssignmentRequest,
    @RequestParam(required = false, defaultValue = "false") boolean notify
) throws ResourceNotFoundException, StreemException, MultiStatusException
```

**Business Purpose**
Bulk assignment of tasks within a job to specific operators, supporting efficient workforce management and skill-based assignment.

**Request Analysis**
- **Path Parameter**: jobId for job context
- **Request Body**: `TaskExecutionAssignmentRequest` containing:
  - Task execution IDs to assign
  - User IDs for assignments
  - Assignment metadata
- **Notification Flag**: Optional notification to assigned users

**Response Analysis**
- **Type**: `BasicDto`
- **Assignment Confirmation**: Confirms bulk assignment completion
- **Multi-status Handling**: May return partial success information

**Service Integration**
- **Service Method**: `IJobAssignmentService.bulkAssign()`
- **Assignment Logic**: Handles complex assignment validation
- **Notification Service**: Optional notification dispatch
- **Multi-status**: Handles partial assignment failures

**Workflow Context**
Workforce management functionality:
- Skill-based task assignment
- Workload balancing
- Schedule optimization  
- Resource allocation

**Security & Permissions**
- **Assignment Authority**: Requires task assignment permissions
- **User Validation**: Validates assigned users have required skills/roles
- **Resource Access**: Ensures assigned users can access job resources

**State Management**
- **Assignment State**: Updates task execution assignment status
- **User Workload**: Tracks user assignment workload
- **Skill Matching**: Validates assignments against skill requirements

**Performance Considerations**
- **Bulk Operations**: Optimized for multiple assignment operations
- **Notification Batching**: Batches notifications for efficiency
- **Assignment Validation**: Efficient skill and availability checking

**Error Scenarios**
- **Invalid Assignments**: Users lack required skills or availability
- **Partial Failures**: Some assignments succeed, others fail
- **Notification Failures**: Assignment succeeds but notification fails
- **Multi-status Exception**: Complex error handling for bulk operations

**Integration Points**
- **HR Systems**: Integrates with workforce management systems
- **Notification Services**: Sends assignment notifications
- **Scheduling Systems**: Updates operator schedules
- **Mobile Apps**: Pushes assignment updates to mobile devices

**Testing Strategy**
- **Bulk Operations**: Test large-scale assignment operations
- **Partial Failures**: Test multi-status exception handling
- **Skill Validation**: Test skill-based assignment logic
- **Notification Testing**: Verify notification dispatch

---

### 17. GET /v1/jobs/{jobId}/assignments - Get Job Assignees

**Endpoint Signature**
```java
@GetMapping("/{jobId}/assignments")
Response<List<TaskExecutionAssigneeDetailsView>> getAssignees(
    @PathVariable(name = "jobId") Long jobId
)
```

**Business Purpose**
Retrieves comprehensive assignment information for a job, showing all task assignments and assignee details.

**Request Analysis**
- **Path Parameter**: jobId for assignment query
- **Simple Request**: No additional parameters required

**Response Analysis**
- **Type**: `List<TaskExecutionAssigneeDetailsView>`
- **Assignment Details**: Complete assignment information including:
  - Task execution assignments
  - Assignee user information
  - Assignment timestamps
  - Task progress status

**Service Integration**
- **Service Method**: Assignment query with user details
- **Projection**: Optimized view projection for assignment data
- **User Information**: Includes relevant assignee details

**Workflow Context**
Supports assignment management:
- Assignment overview and tracking
- Workload distribution analysis
- Progress monitoring by assignee
- Resource utilization reporting

**Security & Permissions**
- **Assignment Visibility**: Requires job assignment view permissions
- **User Privacy**: Returns appropriate level of user information
- **Multi-tenant**: Filtered by facility access

**State Management**
- **Current Assignments**: Shows real-time assignment status
- **Progress Tracking**: Includes task completion progress
- **Assignment History**: May include assignment change history

**Performance Considerations**
- **View Projection**: Uses database views for optimized queries
- **User Data**: Efficiently joins user information
- **Response Size**: Moderate payload size for assignment lists

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **No Assignments**: Returns empty list for jobs without assignments
- **Permission Denied**: User lacks assignment view permissions

**Integration Points**
- **Management Dashboards**: Assignment oversight interfaces
- **HR Systems**: Workforce allocation reporting
- **Mobile Apps**: Assignment status for supervisors

**Testing Strategy**
- **Assignment Completeness**: Verify all assignments returned
- **User Data Accuracy**: Test user information completeness
- **Permission Testing**: Verify access control enforcement

---

### 18. GET /v1/jobs/{jobId}/reports - Job Report Generation

**Endpoint Signature**
```java
@GetMapping("/{jobId}/reports")
Response<JobReportDto> getJobReport(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException, JsonProcessingException
```

**Business Purpose**
Generates comprehensive job execution reports for analysis, compliance, and performance review.

**Request Analysis**
- **Path Parameter**: jobId for report generation
- **Report Scope**: Complete job execution report

**Response Analysis**
- **Type**: `JobReportDto`
- **Comprehensive Data**: Complete job report including:
  - Job execution summary and metrics
  - Stage and task completion details
  - Parameter values and validations
  - Performance timing and duration
  - Assignee participation and signatures
  - Exception and deviation information
  - Audit trail and compliance data

**Service Integration**
- **Service Method**: `IJobService.getJobReport()`
- **Report Generation**: Complex data aggregation and formatting
- **JSON Processing**: Handles complex report data structures

**Workflow Context**
Critical for:
- Performance analysis and improvement
- Regulatory compliance reporting
- Quality control documentation
- Process optimization analysis
- Management reporting and KPIs

**Security & Permissions**
- **Reporting Access**: Requires job reporting permissions
- **Data Privacy**: Appropriate level of detail based on user role
- **Compliance**: Ensures report data meets regulatory requirements

**State Management**
- **Report Completeness**: Different report details based on job state
- **Historical Data**: Includes complete execution history
- **Audit Information**: Comprehensive audit trail inclusion

**Performance Considerations**
- **Complex Aggregation**: May involve complex database queries
- **Report Caching**: Caches completed job reports
- **Response Size**: Large payload for comprehensive reports

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **Incomplete Data**: Handles jobs with missing or incomplete data
- **JSON Processing**: Complex report data processing errors

**Integration Points**
- **BI Systems**: Data source for business intelligence platforms
- **Document Management**: Report storage and archival systems
- **Compliance Systems**: Regulatory reporting integration
- **Performance Analytics**: Data source for performance dashboards

**Testing Strategy**
- **Report Completeness**: Verify all required report sections
- **Data Accuracy**: Test report data against source records
- **Performance Testing**: Monitor report generation performance
- **Format Validation**: Verify report structure and formatting

---

### 19. GET /v1/jobs/{jobId}/reports/print - Printable Job Report

**Endpoint Signature**
```java
@GetMapping("/{jobId}/reports/print")
Response<JobReportDto> printJobReport(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException, JsonProcessingException
```

**Business Purpose**
Generates print-optimized job reports with formatting suitable for physical documentation and archival.

**Request Analysis**
- **Path Parameter**: jobId for print report generation
- **Print Optimization**: Formatted for physical printing requirements

**Response Analysis**
- **Type**: `JobReportDto`
- **Print-optimized**: Same data as regular report but with:
  - Print-friendly formatting
  - Page break considerations
  - Enhanced readability for physical documents
  - Signature blocks and approval sections

**Service Integration**
- **Service Method**: Print-specific report generation
- **Formatting**: Additional formatting for print requirements
- **Template Processing**: May use print-specific templates

**Workflow Context**
Supports:
- Physical documentation requirements
- Regulatory compliance documentation
- Archive and record keeping
- Quality system documentation

**Security & Permissions**
- **Print Access**: May require specific printing permissions
- **Document Control**: Tracks print report generation for audit
- **Compliance**: Ensures printed reports meet regulatory standards

**State Management**
- **Print Version**: May include print-specific metadata
- **Document Control**: Version control for printed documents
- **Signature Requirements**: Print reports may require signatures

**Performance Considerations**
- **Print Formatting**: Additional processing for print optimization
- **Template Rendering**: Print template processing overhead
- **Document Size**: Optimized for print document size limits

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **Print Formatting**: Print template processing errors
- **Document Generation**: Issues with print document creation

**Integration Points**
- **Document Management**: Integration with document control systems
- **Print Services**: Connection to enterprise printing systems
- **Archive Systems**: Long-term storage of printed report versions

**Testing Strategy**
- **Print Quality**: Verify print formatting and readability
- **Document Integrity**: Test print version completeness
- **Template Testing**: Validate print template rendering

---

### 20. GET /v1/jobs/{jobId}/print - Binary Job Print

**Endpoint Signature**
```java
@GetMapping("/{jobId}/print")
ResponseEntity<byte[]> printJob(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException, IOException
```

**Business Purpose**
Generates binary print document (PDF) for direct printing or download of job documentation.

**Request Analysis**
- **Path Parameter**: jobId for print document generation
- **Binary Output**: Returns binary document data

**Response Analysis**
- **Type**: `ResponseEntity<byte[]>`
- **Binary Document**: PDF or other binary format containing:
  - Complete job documentation
  - Print-ready formatting
  - Embedded media and signatures
  - Compliance-ready layout

**Service Integration**
- **Service Method**: Binary document generation service
- **Document Processing**: PDF generation with complex layouts
- **Media Embedding**: Includes images and attachments

**Workflow Context**
Enables:
- Direct document download
- Automated printing workflows
- Document archival systems
- Compliance documentation

**Security & Permissions**
- **Print Authorization**: Requires document printing permissions
- **Content Protection**: May include watermarking or security features
- **Access Logging**: Tracks document generation for audit

**State Management**
- **Document Version**: Consistent document versioning
- **Content Finalization**: Ensures document content is complete
- **Security Marking**: May include security markings or classifications

**Performance Considerations**
- **Document Generation**: Complex PDF generation processing
- **Binary Size**: Large binary responses for complex documents
- **Caching**: May cache generated documents for performance

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **Document Generation**: PDF generation failures
- **Binary Processing**: Issues with binary document creation
- **IO Exceptions**: File system or network issues

**Integration Points**
- **Print Services**: Direct integration with printing systems
- **Document Storage**: Binary document storage systems
- **Download Services**: File download and delivery systems

**Testing Strategy**
- **Document Quality**: Verify PDF generation quality
- **Binary Integrity**: Test binary document completeness
- **Performance Testing**: Monitor document generation performance
- **Print Testing**: Verify actual print output quality

---

### 21. GET /v1/jobs/{jobId}/correction-print - Correction Print Report

**Endpoint Signature**
```java
@GetMapping("/{jobId}/correction-print")
Response<List<CorrectionPrintDto>> printJobCorrections(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException, JsonProcessingException
```

**Business Purpose**
Generates correction reports showing parameter corrections and modifications for quality control and audit purposes.

**Request Analysis**
- **Path Parameter**: jobId for correction report generation
- **Correction Focus**: Specifically targets parameter corrections and changes

**Response Analysis**
- **Type**: `List<CorrectionPrintDto>`
- **Correction Data**: Detailed correction information including:
  - Original parameter values
  - Corrected parameter values
  - Correction timestamps and reasons
  - Correction authorizations
  - Impact analysis

**Service Integration**
- **Service Method**: Correction-specific report generation
- **Change Tracking**: Analyzes parameter value changes
- **Audit Integration**: Includes audit trail for corrections

**Workflow Context**
Critical for:
- Quality control processes
- Correction documentation
- Regulatory compliance
- Process improvement analysis
- Audit trail maintenance

**Security & Permissions**
- **Correction Access**: Requires correction view permissions
- **Quality Authorization**: May require quality control permissions
- **Audit Access**: Ensures proper audit trail access

**State Management**
- **Correction History**: Complete history of parameter corrections
- **Authorization Tracking**: Tracks correction approvals
- **Change Impact**: Shows impact of corrections on job outcomes

**Performance Considerations**
- **Change Analysis**: Complex queries to identify all corrections
- **Report Generation**: May involve significant data processing
- **Audit Performance**: Optimized audit trail queries

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **No Corrections**: Returns empty list for jobs without corrections
- **Processing Errors**: Issues with correction data analysis

**Integration Points**
- **Quality Systems**: Correction data for quality management
- **CAPA Systems**: Input for corrective action processes
- **Audit Systems**: Correction documentation for compliance

**Testing Strategy**
- **Correction Accuracy**: Verify correction data completeness
- **Change Tracking**: Test correction identification logic
- **Report Quality**: Validate correction report formatting

---

### 22. GET /v1/jobs/by/resource/{objectId} - Jobs by Resource

**Endpoint Signature**
```java
@GetMapping("/by/resource/{objectId}")
Response<Page<JobPartialDto>> getAllByResource(
    @PathVariable("objectId") String objectId,
    @RequestParam("filters") String filters,
    Pageable pageable
)
```

**Business Purpose**
Retrieves jobs filtered by specific resource/facility for multi-tenant resource management and facility-specific reporting.

**Request Analysis**
- **Path Parameter**: objectId for resource/facility identification
- **Query Parameter**: filters for additional filtering
- **Pagination**: Standard pagination support

**Response Analysis**
- **Type**: `Page<JobPartialDto>`
- **Resource-filtered**: Jobs specific to requested resource/facility
- **Partial Data**: Summary information suitable for listing views

**Service Integration**
- **Service Method**: Resource-specific job query
- **Multi-tenant**: Enforces resource-based data isolation
- **Filtering**: Applies additional filters within resource scope

**Workflow Context**
Supports:
- Facility-specific job management
- Resource utilization analysis
- Multi-tenant data access
- Facility performance monitoring

**Security & Permissions**
- **Resource Access**: Validates user access to specified resource
- **Multi-tenant Security**: Enforces resource-based data isolation
- **Facility Permissions**: Ensures appropriate facility access

**State Management**
- **Resource Association**: Shows jobs associated with specific resources
- **Facility Context**: Maintains facility-specific job context
- **Access Control**: Resource-based access control enforcement

**Performance Considerations**
- **Resource Indexing**: Database indexes on resource/facility identifiers
- **Multi-tenant Performance**: Optimized queries for multi-tenant architecture
- **Filtering Performance**: Efficient filtering within resource scope

**Error Scenarios**
- **Invalid Resource**: Resource doesn't exist or not accessible
- **Permission Denied**: User lacks access to specified resource
- **Filter Errors**: Invalid filter syntax for resource queries

**Integration Points**
- **Facility Management**: Integration with facility management systems
- **Resource Planning**: Data for resource utilization planning
- **Multi-tenant Platforms**: Resource-specific data access

**Testing Strategy**
- **Multi-tenant Testing**: Verify resource-based data isolation
- **Access Control**: Test resource access permissions
- **Performance Testing**: Monitor resource-filtered query performance

---

### 23. GET /v1/jobs/{jobId}/info - Job Information

**Endpoint Signature**
```java
@GetMapping("/{jobId}/info")
Response<JobInformationDto> getJobInformation(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException
```

**Business Purpose**
Retrieves essential job information for dashboard widgets and summary displays without full job detail overhead.

**Request Analysis**
- **Path Parameter**: jobId for information query
- **Lightweight**: Optimized for summary information needs

**Response Analysis**
- **Type**: `JobInformationDto`
- **Summary Data**: Essential job information including:
  - Basic job metadata
  - Current state and progress
  - Key performance indicators
  - Summary statistics

**Service Integration**
- **Service Method**: Optimized information query
- **Lightweight Processing**: Minimal data processing for performance
- **Summary Generation**: Calculates summary metrics efficiently

**Workflow Context**
Ideal for:
- Dashboard widgets and summaries
- Mobile app overview screens
- Quick reference information
- Performance indicator displays

**Security & Permissions**
- **Information Access**: Basic job information permissions
- **Summary Data**: Appropriate level of detail for user role
- **Performance**: Minimal security overhead for lightweight queries

**State Management**
- **Current Information**: Real-time job information
- **Summary Metrics**: Calculated performance indicators
- **Progress Status**: Current progress and completion status

**Performance Considerations**
- **Optimized Queries**: Lightweight database queries
- **Summary Caching**: Caches job summary information
- **Fast Response**: Target response time for dashboard widgets

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **Permission Denied**: User lacks job information access
- **Data Unavailable**: Handles cases with incomplete job data

**Integration Points**
- **Dashboard Systems**: Primary data source for job widgets
- **Mobile Applications**: Summary information for mobile interfaces
- **Notification Systems**: Job information for notifications

**Testing Strategy**
- **Information Accuracy**: Verify summary information correctness
- **Performance Testing**: Monitor response times for dashboard use
- **Data Completeness**: Test information availability across job states

---

### 24. GET /v1/jobs/approvals - Pending Approval Parameters

**Endpoint Signature**
```java
@GetMapping("/approvals")
Response<Page<PendingForApprovalStatusDto>> getPendingForApprovalParameters(
    @RequestParam(value = "processName", required = false, defaultValue = "") String processName,
    @RequestParam(value = "parameterName", required = false, defaultValue = "") String parameterName,
    @RequestParam(value = "objectId", required = false) String objectId,
    @RequestParam(value = "jobId", required = false) String jobId,
    @RequestParam(value = "userId", required = false) String userId,
    @RequestParam(value = "useCaseId", required = true) String useCaseId,
    @RequestParam(value = "showAllException", required = false, defaultValue = "false") boolean showAllException,
    @RequestParam(required = false) Long requestedBy,
    @SortDefault(sort = ParameterValue.DEFAULT_SORT, direction = Sort.Direction.DESC) Pageable pageable
)
```

**Business Purpose**
Quality control interface for managing parameter approvals, supporting quality management workflows and parameter validation processes.

**Request Analysis**
- **Multiple Filters**: Comprehensive filtering options:
  - `processName`: Filter by process/checklist name
  - `parameterName`: Filter by specific parameter types
  - `objectId`: Multi-tenant facility filtering
  - `jobId`: Job-specific parameter approvals
  - `userId`: User-specific approval queues
  - `useCaseId`: Required use case context
  - `showAllException`: Include exception parameters
  - `requestedBy`: Filter by approval requester
- **Pagination**: Standard pagination with default sorting

**Response Analysis**
- **Type**: `Page<PendingForApprovalStatusDto>`
- **Approval Queue**: Parameters requiring approval including:
  - Parameter execution details
  - Approval request information
  - Quality control status
  - Requester and approver information

**Service Integration**
- **Service Method**: Quality-focused parameter approval queries
- **Complex Filtering**: Advanced filtering for quality workflows
- **Use Case Context**: Use case-specific approval processes

**Workflow Context**
Core quality control functionality:
- Quality control approval workflows
- Parameter validation processes
- Exception review and approval
- Quality management system integration

**Security & Permissions**
- **Quality Access**: Requires quality control or approval permissions
- **Use Case Authorization**: Validates user access to specific use cases
- **Multi-tenant**: Facility-based access control

**State Management**
- **Approval States**: Tracks parameter approval states
- **Quality Gates**: Manages quality control checkpoints
- **Exception Handling**: Special handling for exception parameters

**Performance Considerations**
- **Complex Filtering**: Optimized queries for multiple filter combinations
- **Quality Queries**: Database optimization for quality control workflows
- **Approval Performance**: Fast response for quality control interfaces

**Error Scenarios**
- **Missing Use Case**: Required useCaseId parameter missing
- **Invalid Filters**: Incorrect filter parameter values
- **Permission Denied**: User lacks quality control permissions

**Integration Points**
- **Quality Management**: Integration with QMS platforms
- **Approval Workflows**: Connection to approval workflow engines
- **Notification Systems**: Approval request notifications

**Testing Strategy**
- **Filter Combinations**: Test various filter parameter combinations
- **Quality Workflows**: Test approval process integration
- **Performance Testing**: Monitor complex query performance

---

### 25. GET /v1/jobs/{jobId}/activity/print - Activity Print Report

**Endpoint Signature**
```java
@GetMapping("/{jobId}/activity/print")
ResponseEntity<byte[]> printJobActivity(
    @PathVariable("jobId") Long jobId,
    @RequestParam(name = "filters", defaultValue = "") String filters
) throws ResourceNotFoundException, IOException
```

**Business Purpose**
Generates printable activity reports showing detailed job execution activities, task completions, and operator actions.

**Request Analysis**
- **Path Parameter**: jobId for activity report generation
- **Query Parameter**: filters for activity filtering (date ranges, users, activities)
- **Binary Output**: Returns binary document (PDF)

**Response Analysis**
- **Type**: `ResponseEntity<byte[]>`
- **Activity Report**: Comprehensive activity documentation including:
  - Task execution timeline
  - Operator actions and timestamps
  - Parameter value changes
  - State transitions and approvals
  - Exception handling activities

**Service Integration**
- **Service Method**: Activity-focused report generation
- **Activity Logging**: Integrates with job activity logging systems
- **Print Generation**: Binary document generation with activity data

**Workflow Context**
Supports:
- Activity audit and review
- Performance analysis
- Training and process improvement
- Compliance documentation

**Security & Permissions**
- **Activity Access**: Requires activity reporting permissions
- **Audit Trail**: May require audit access permissions
- **Print Authorization**: Printing permissions for activity reports

**State Management**
- **Activity History**: Complete activity timeline
- **State Changes**: Activity-driven state transitions
- **Audit Integration**: Activities linked to audit trail

**Performance Considerations**
- **Activity Queries**: Complex queries for activity timeline
- **Report Generation**: Binary document generation overhead
- **Filter Performance**: Efficient activity filtering

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **No Activities**: Jobs without activity may return empty reports
- **Generation Errors**: Binary document generation failures

**Integration Points**
- **Activity Logging**: Integration with activity logging systems
- **Audit Systems**: Activity data for audit compliance
- **Performance Analytics**: Activity data for process optimization

**Testing Strategy**
- **Activity Completeness**: Verify all activities included in report
- **Timeline Accuracy**: Test activity timeline correctness
- **Print Quality**: Validate printed report format and content

---

### 26. GET /v1/jobs/{jobId}/assignees - Job Assignee Search

**Endpoint Signature**
```java
@GetMapping("/{jobId}/assignees")
Response<List<JobAssigneeView>> getAllJobAssignees(
    @PathVariable("jobId") Long jobId,
    @RequestParam(value = "query", required = false) String query,
    @RequestParam(value = "roles", required = false) List<String> roles,
    Pageable pageable
)
```

**Business Purpose**
Searches and retrieves job assignees with filtering capabilities for assignment management and user selection interfaces.

**Request Analysis**
- **Path Parameter**: jobId for assignee context
- **Query Parameter**: Optional text search query for assignee names
- **Role Filter**: Optional role-based filtering
- **Pagination**: Standard pagination support

**Response Analysis**
- **Type**: `List<JobAssigneeView>`
- **Assignee Information**: Job assignee details including:
  - User identification and contact information
  - Role and skill information
  - Assignment status and availability
  - Performance metrics

**Service Integration**
- **Service Method**: Assignee search with filtering
- **User Integration**: Integrates with user management systems
- **Role Validation**: Role-based assignee filtering

**Workflow Context**
Supports:
- Assignment management interfaces
- User selection for task assignment
- Skill-based assignment decisions
- Workload balancing

**Security & Permissions**
- **Assignee Access**: Requires assignee view permissions
- **User Privacy**: Appropriate level of user information disclosure
- **Role Filtering**: Ensures role-based access control

**State Management**
- **Assignment Status**: Current assignee assignment status
- **Availability**: Assignee availability for additional work
- **Skills**: Assignee skill and certification information

**Performance Considerations**
- **Search Performance**: Optimized user search queries
- **Role Filtering**: Efficient role-based filtering
- **User Data**: Optimized user information retrieval

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **No Assignees**: Returns empty list for jobs without assignees
- **Search Errors**: Invalid search query parameters

**Integration Points**
- **User Management**: Integration with user directory services
- **HR Systems**: Employee information and skills data
- **Assignment Systems**: User selection for task assignments

**Testing Strategy**
- **Search Functionality**: Test assignee search capabilities
- **Role Filtering**: Verify role-based filtering accuracy
- **User Data**: Test user information completeness and privacy

---

### 27. GET /v1/jobs/{jobId}/assigned - Current User Assignment Status

**Endpoint Signature**
```java
@GetMapping("/{jobId}/assigned")
Response<JobAssigneeDto> isCurrentUserAssignedToJob(@PathVariable("jobId") Long jobId) 
    throws ResourceNotFoundException
```

**Business Purpose**
Checks if the current authenticated user is assigned to the specified job, supporting user interface personalization and access control.

**Request Analysis**
- **Path Parameter**: jobId for assignment check
- **User Context**: Uses authenticated user context
- **Simple Query**: Boolean-style assignment check

**Response Analysis**
- **Type**: `JobAssigneeDto`
- **Assignment Status**: Current user's assignment status including:
  - Assignment confirmation (true/false)
  - Assignment details and role
  - Task-specific assignments
  - Permission information

**Service Integration**
- **Service Method**: User-specific assignment query
- **Security Context**: Extracts user from authentication context
- **Assignment Validation**: Checks user assignment status

**Workflow Context**
Enables:
- User interface personalization
- Access control decisions
- Assignment-based feature enabling
- Operator interface customization

**Security & Permissions**
- **User Context**: Uses authenticated user automatically
- **Assignment Privacy**: Users can only check their own assignments
- **Access Control**: Supports role-based UI features

**State Management**
- **Current Assignment**: Real-time assignment status
- **Role Context**: Assignment role and permissions
- **Task Access**: Task-level assignment information

**Performance Considerations**
- **Fast Query**: Optimized for quick assignment checks
- **User Caching**: Caches user assignment information
- **Response Time**: Critical for UI responsiveness

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **Authentication Required**: Requires authenticated user context
- **Permission Denied**: User lacks job access permissions

**Integration Points**
- **User Interfaces**: Primary data for UI personalization
- **Mobile Apps**: Assignment-based feature control
- **Access Control**: Integration with authorization systems

**Testing Strategy**
- **Assignment Accuracy**: Verify assignment status correctness
- **User Context**: Test authenticated user context handling
- **Performance**: Monitor response times for UI integration

---

### 28. GET /v1/jobs/{jobId}/job-lite - Lightweight Job Data

**Endpoint Signature**
```java
@GetMapping("/{jobId}/job-lite")
Response<JobLiteDto> getJobLite(@PathVariable Long jobId) 
    throws ResourceNotFoundException, JsonProcessingException
```

**Business Purpose**
Retrieves essential job information with minimal data payload for mobile applications and performance-critical interfaces.

**Request Analysis**
- **Path Parameter**: jobId for lightweight job query
- **Performance Focus**: Optimized for minimal data transfer

**Response Analysis**
- **Type**: `JobLiteDto`
- **Minimal Data**: Essential job information only:
  - Basic job identification (id, code, state)
  - Critical progress information
  - Essential checklist information
  - Key timestamps

**Service Integration**
- **Service Method**: Lightweight job query with minimal processing
- **Data Projection**: Selective field loading for performance
- **JSON Optimization**: Minimal JSON processing overhead

**Workflow Context**
Ideal for:
- Mobile application interfaces
- Performance-critical dashboard widgets
- Quick reference displays
- Network-constrained environments

**Security & Permissions**
- **Basic Access**: Standard job read permissions
- **Performance Security**: Minimal security overhead
- **Data Privacy**: Limited data exposure for privacy

**State Management**
- **Essential State**: Critical job state information only
- **Progress Summary**: Basic progress indicators
- **Status Information**: Key status information

**Performance Considerations**
- **Minimal Payload**: Smallest possible data transfer
- **Fast Queries**: Optimized database queries for essential data
- **Caching**: Aggressive caching for lite data
- **Network Optimization**: Optimized for mobile networks

**Error Scenarios**
- **Job Not Found**: Invalid job ID returns 404
- **JSON Processing**: Minimal JSON processing errors
- **Permission Denied**: User lacks basic job access

**Integration Points**
- **Mobile Applications**: Primary data source for mobile job interfaces
- **Performance Dashboards**: Lightweight widgets and indicators
- **Third-party Systems**: Minimal data exchange interfaces

**Testing Strategy**
- **Data Completeness**: Verify essential information included
- **Performance Testing**: Monitor response times and payload sizes
- **Mobile Testing**: Test mobile application integration

---

### 29. GET /v1/jobs/download - Excel Export

**Endpoint Signature**
```java
@GetMapping("/download")
void downloadJobsExcel(
    @RequestParam(name = "filters", required = false) String filters,
    @RequestParam(name = "objectId", required = false) String objectId,
    HttpServletResponse response
) throws IOException, ResourceNotFoundException
```

**Business Purpose**
Exports job data to Excel format for external analysis, reporting, and data integration with other systems.

**Request Analysis**
- **Query Parameters**: 
  - `filters`: Optional filtering for export data
  - `objectId`: Multi-tenant facility filtering
- **Response Stream**: Direct HTTP response stream for file download

**Response Analysis**
- **Type**: Direct file download (Excel binary)
- **Export Data**: Comprehensive job export including:
  - Job metadata and status information
  - Progress and completion data
  - Parameter values and results
  - Performance metrics
  - Assignment information

**Service Integration**
- **Service Method**: `IJobExcelService.downloadJobsExcel()`
- **Excel Generation**: Specialized Excel generation service
- **Data Export**: Complex data aggregation for export

**Workflow Context**
Supports:
- External reporting and analysis
- Data integration with other systems
- Backup and archival processes
- Business intelligence platforms

**Security & Permissions**
- **Export Permissions**: Requires data export permissions
- **Multi-tenant**: Facility-based data filtering
- **Data Privacy**: Ensures appropriate data access levels

**State Management**
- **Export Scope**: Exports current job data based on filters
- **Data Consistency**: Ensures consistent export data
- **Version Control**: May include export timestamp information

**Performance Considerations**
- **Large Data Sets**: Handles large job data exports efficiently
- **Memory Management**: Streaming export for memory efficiency
- **Generation Time**: May involve significant processing time

**Error Scenarios**
- **Filter Errors**: Invalid filter parameters
- **Export Failures**: Excel generation failures
- **Permission Denied**: User lacks export permissions
- **Data Unavailable**: No jobs match export criteria

**Integration Points**
- **Business Intelligence**: Data source for BI platforms
- **ERP Systems**: Job data integration with enterprise systems
- **Reporting Tools**: External reporting and analysis tools

**Testing Strategy**
- **Export Completeness**: Verify all relevant data included
- **Excel Quality**: Test Excel file format and readability
- **Performance Testing**: Monitor export generation performance
- **Filter Testing**: Test various filter combinations

---

## Architecture Integration

### Service Layer Integration
- **Primary Service**: `IJobService` - Core job management operations
- **Specialized Services**: 
  - `IJobAssignmentService` - Assignment management
  - `IJobCweService` - Completion with exception handling
  - `IJobExcelService` - Excel export functionality
  - `IJobNotificationService` - Notification dispatch

### Data Layer Integration
- **Entity**: `Job` - Primary job entity with complex relationships
- **Repositories**: Optimized queries for job operations
- **Projections**: Performance-optimized views for specific endpoints

### Cross-cutting Concerns
- **Security**: Multi-tenant access control and role-based permissions
- **Caching**: Strategic caching for performance-critical endpoints
- **Audit**: Comprehensive audit trail for all job operations
- **Notifications**: Real-time notifications for job state changes

### Performance Optimization
- **Entity Graphs**: JPA entity graphs for optimized data loading
- **Caching Strategy**: Multi-level caching with appropriate TTL
- **Database Indexing**: Strategic indexes for common query patterns
- **Response Optimization**: Payload optimization for different use cases

### Error Handling
- **Exception Hierarchy**: Structured exception handling with appropriate HTTP status codes
- **Multi-status Operations**: Complex error handling for bulk operations
- **Validation**: Comprehensive input validation with meaningful error messages
- **Resource Management**: Proper resource cleanup and error recovery

### Integration Points
- **Manufacturing Systems**: ERP, MES, and production planning integration
- **Quality Management**: QMS integration for quality control workflows
- **User Management**: JAAS service integration for authentication/authorization
- **Notification Systems**: Multi-channel notification delivery
- **Business Intelligence**: Data export for reporting and analytics

## Testing Strategy

### Unit Testing
- **Controller Testing**: Mock service layer, verify request/response handling
- **Validation Testing**: Input validation and error scenarios
- **Security Testing**: Access control and permission validation

### Integration Testing
- **End-to-End Workflows**: Complete job lifecycle testing
- **Database Integration**: Repository and query testing
- **Service Integration**: Cross-service communication testing

### Performance Testing
- **Load Testing**: High-volume job operations
- **Polling Performance**: Real-time polling endpoint performance
- **Export Performance**: Large data export testing

### Quality Assurance
- **API Contract Testing**: OpenAPI specification compliance
- **Error Scenario Testing**: Comprehensive error handling validation
- **Multi-tenant Testing**: Data isolation and security validation

---

## Conclusion

The **IJobController** represents the heart of the Streem Digital Work Instructions platform, providing comprehensive job lifecycle management from creation through completion. This controller demonstrates advanced patterns in REST API design, real-time system integration, quality control processes, and manufacturing workflow orchestration.

This documentation serves as the definitive template for documenting all 36 controllers in the Streem platform, showcasing the depth of business context, technical implementation, and integration patterns required for enterprise manufacturing software documentation.