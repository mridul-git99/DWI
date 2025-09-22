# JobRepository - Comprehensive Hibernate Removal Documentation

## Repository Overview

**Repository Interface**: `IJobRepository`  
**File Location**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/repository/IJobRepository.java`  
**Entity**: `Job`  
**Primary Table**: `jobs`  
**Business Domain**: Digital Work Instructions Job Execution Management  
**Repository Hierarchy**: Extends `JpaRepository<Job, Long>`, `JpaSpecificationExecutor<Job>`

### Business Context
The JobRepository manages job execution instances in the Streem Digital Work Instructions platform. Jobs represent specific execution instances of checklists (process templates) by users, containing task executions, parameter values, and workflow state management. This repository handles job lifecycle from creation through completion, including user assignments, approvals, scheduling, and reporting.

### Entity Relationships Overview
```
Job (N) ←→ (1) Checklist
Job (N) ←→ (1) Facility  
Job (N) ←→ (1) Organisation
Job (N) ←→ (1) UseCase
Job (1) ←→ (N) TaskExecution ←→ (N) TaskExecutionUserMapping
Job (1) ←→ (N) ParameterValue ←→ (N) ParameterVerification
Job (1) ←→ (N) RelationValue
Job (N) ←→ (1) User (startedBy, endedBy)
Job (1) ←→ (1) Scheduler
Job (1) ←→ (N) JobAudit
Job (1) ←→ (N) JobAnnotation  
Job (1) ←→ (N) JobCweDetail
```

### Key Business States
- **UNASSIGNED**: Job created but no users assigned to tasks
- **ASSIGNED**: Users assigned to tasks but execution not started
- **IN_PROGRESS**: Job execution in progress with active tasks
- **COMPLETED**: All tasks completed successfully
- **COMPLETED_WITH_EXCEPTION**: Completed with quality exceptions
- **BEING_BUILT**: Job in construction phase

---

## Method Documentation

### 1. findAll(Specification, Pageable) - Override

**Method Signature:**
```java
@Override
Page<Job> findAll(@Nullable Specification<Job> specification, Pageable pageable);
```

**Input Parameters:**
- `specification` (Specification<Job>): Dynamic query criteria using Spring Data JPA Specifications
  - Validation: Can be null (returns all records)
  - Purpose: Enables complex filtering with AND/OR conditions on job properties
  - Common Filters: organisationId, facilityId, state, checklistId, userId, dateRanges
- `pageable` (Pageable): Pagination and sorting configuration
  - Validation: Cannot be null
  - Purpose: Controls page size, offset, and sorting
  - Common Sorts: createdAt DESC, modifiedAt DESC, code ASC

**Return Type:**
- `Page<Job>`: Paginated results with metadata
- Contains: List of Job entities, total count, page information
- Edge Cases: Empty page if no results match criteria

**Generated SQL Query:**
```sql
SELECT j.id, j.code, j.state, j.checklists_id, j.facilities_id, j.organisations_id,
       j.use_cases_id, j.started_at, j.started_by, j.ended_at, j.ended_by,
       j.is_scheduled, j.schedulers_id, j.expected_start_date, j.expected_end_date,
       j.checklist_ancestor_id, j.created_at, j.modified_at, j.created_by, j.modified_by
FROM jobs j 
WHERE [specification conditions]
ORDER BY [pageable sort fields]
LIMIT [page_size] OFFSET [page_number * page_size];

-- Count query for pagination metadata
SELECT COUNT(j.id) FROM jobs j WHERE [specification conditions];
```

**Database Execution Plan:**
- Primary Index: Uses `jobs_pkey` for basic lookups
- Organization Index: `idx_jobs_organisations_id` for tenant isolation
- Facility Index: `idx_jobs_facilities_id` for facility-scoped queries
- State Index: `idx_jobs_state` for workflow state filtering
- Performance: O(log n) for indexed fields, O(n) for complex specifications

**Entity Hydration Details:**
- **Lazy Loading**: All associations are FetchType.LAZY (checklist, facility, organisation, taskExecutions, parameterValues)
- **Eager Loading**: None by default
- **Hydrated Fields**: All scalar fields (id, code, state, timestamps, foreign keys)
- **Not Hydrated**: checklist, facility, organisation, useCase, taskExecutions, parameterValues, relationValues, parameterVerifications

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED (default)
- **Transaction Required**: READ_ONLY transaction sufficient
- **Locking**: No explicit locks, relies on MVCC

**Caching Behavior:**
- **L1 Cache**: Entities cached in Hibernate Session
- **L2 Cache**: Not configured for Job entity due to high mutation rate
- **Query Cache**: Potentially cached if enabled globally, but not recommended for job queries

**Business Logic Integration:**
- Called from: JobService.searchJobs(), JobController.getJobs()
- Usage Pattern: Dashboard job listings, administrative job management
- Common Specifications: Filter by organisation, facility, state, assigned users, date ranges

**DAO Conversion Strategy:**
```java
public class JobDAO {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    
    public PageResult<Job> findAll(JobSearchCriteria criteria, Pageable pageable) {
        // Build dynamic WHERE clause
        StringBuilder whereClause = new StringBuilder("WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        
        if (criteria.getOrganisationId() != null) {
            whereClause.append(" AND j.organisations_id = :organisationId");
            params.put("organisationId", criteria.getOrganisationId());
        }
        
        if (criteria.getFacilityId() != null) {
            whereClause.append(" AND j.facilities_id = :facilityId");
            params.put("facilityId", criteria.getFacilityId());
        }
        
        if (criteria.getStates() != null && !criteria.getStates().isEmpty()) {
            whereClause.append(" AND j.state IN (:states)");
            params.put("states", criteria.getStates().stream().map(Enum::name).collect(toList()));
        }
        
        if (criteria.getChecklistId() != null) {
            whereClause.append(" AND j.checklists_id = :checklistId");
            params.put("checklistId", criteria.getChecklistId());
        }
        
        if (StringUtils.hasText(criteria.getCodeFilter())) {
            whereClause.append(" AND j.code ILIKE :codeFilter");
            params.put("codeFilter", "%" + criteria.getCodeFilter() + "%");
        }
        
        // Date range filters
        if (criteria.getStartedAtFrom() != null) {
            whereClause.append(" AND j.started_at >= :startedAtFrom");
            params.put("startedAtFrom", criteria.getStartedAtFrom());
        }
        
        if (criteria.getStartedAtTo() != null) {
            whereClause.append(" AND j.started_at <= :startedAtTo");
            params.put("startedAtTo", criteria.getStartedAtTo());
        }
        
        // Build ORDER BY clause
        String orderBy = buildOrderByClause(pageable.getSort());
        
        // Count query
        String countSql = "SELECT COUNT(*) FROM jobs j " + whereClause.toString();
        Long totalCount = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
        
        // Data query with pagination
        String dataSql = """
            SELECT j.id, j.code, j.state, j.checklists_id, j.facilities_id, j.organisations_id,
                   j.use_cases_id, j.started_at, j.started_by, j.ended_at, j.ended_by,
                   j.is_scheduled, j.schedulers_id, j.expected_start_date, j.expected_end_date,
                   j.checklist_ancestor_id, j.created_at, j.modified_at, j.created_by, j.modified_by
            FROM jobs j 
            """ + whereClause.toString() + " " + orderBy + 
            " LIMIT :limit OFFSET :offset";
            
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());
        
        List<Job> jobs = namedJdbcTemplate.query(dataSql, params, new JobRowMapper());
        
        return new PageResult<>(jobs, totalCount, pageable);
    }
    
    private String buildOrderByClause(Sort sort) {
        if (sort.isEmpty()) {
            return "ORDER BY j.created_at DESC";
        }
        
        List<String> orderClauses = new ArrayList<>();
        for (Sort.Order order : sort) {
            String property = mapPropertyToColumn(order.getProperty());
            String direction = order.getDirection().name();
            orderClauses.add("j." + property + " " + direction);
        }
        
        return "ORDER BY " + String.join(", ", orderClauses);
    }
    
    private String mapPropertyToColumn(String property) {
        return switch (property) {
            case "checklistId" -> "checklists_id";
            case "facilityId" -> "facilities_id";
            case "organisationId" -> "organisations_id";
            case "useCaseId" -> "use_cases_id";
            case "schedulerId" -> "schedulers_id";
            case "startedAt" -> "started_at";
            case "startedBy" -> "started_by";
            case "endedAt" -> "ended_at";
            case "endedBy" -> "ended_by";
            case "isScheduled" -> "is_scheduled";
            case "expectedStartDate" -> "expected_start_date";
            case "expectedEndDate" -> "expected_end_date";
            case "checklistAncestorId" -> "checklist_ancestor_id";
            case "createdAt" -> "created_at";
            case "modifiedAt" -> "modified_at";
            case "createdBy" -> "created_by";
            case "modifiedBy" -> "modified_by";
            default -> property;
        };
    }
}

public class JobRowMapper implements RowMapper<Job> {
    @Override
    public Job mapRow(ResultSet rs, int rowNum) throws SQLException {
        Job job = new Job();
        job.setId(rs.getLong("id"));
        job.setCode(rs.getString("code"));
        job.setState(State.Job.valueOf(rs.getString("state")));
        job.setChecklistId(rs.getLong("checklists_id"));
        job.setFacilityId(rs.getLong("facilities_id"));
        job.setOrganisationId(rs.getLong("organisations_id"));
        job.setUseCaseId(rs.getLong("use_cases_id"));
        
        Long startedAt = rs.getLong("started_at");
        job.setStartedAt(rs.wasNull() ? null : startedAt);
        
        Long startedBy = rs.getLong("started_by");
        job.setStartedBy(rs.wasNull() ? null : createUserProxy(startedBy));
        
        Long endedAt = rs.getLong("ended_at");
        job.setEndedAt(rs.wasNull() ? null : endedAt);
        
        Long endedBy = rs.getLong("ended_by");
        job.setEndedBy(rs.wasNull() ? null : createUserProxy(endedBy));
        
        job.setScheduled(rs.getBoolean("is_scheduled"));
        
        Long schedulerId = rs.getLong("schedulers_id");
        job.setSchedulerId(rs.wasNull() ? null : schedulerId);
        
        Long expectedStartDate = rs.getLong("expected_start_date");
        job.setExpectedStartDate(rs.wasNull() ? null : expectedStartDate);
        
        Long expectedEndDate = rs.getLong("expected_end_date");
        job.setExpectedEndDate(rs.wasNull() ? null : expectedEndDate);
        
        Long checklistAncestorId = rs.getLong("checklist_ancestor_id");
        job.setChecklistAncestorId(rs.wasNull() ? null : checklistAncestorId);
        
        job.setCreatedAt(rs.getLong("created_at"));
        job.setModifiedAt(rs.getLong("modified_at"));
        job.setCreatedBy(rs.getLong("created_by"));
        job.setModifiedBy(rs.getLong("modified_by"));
        
        return job;
    }
    
    private User createUserProxy(Long userId) {
        User user = new User();
        user.setId(userId);
        return user; // Lazy proxy
    }
}
```

**Performance Considerations:**
- **Indexes Required**: 
  - `idx_jobs_organisations_id_facilities_id` (composite)
  - `idx_jobs_state_created_at` (composite for state + ordering)
  - `idx_jobs_checklist_id` (for checklist filtering)
- **Avoid**: N+1 queries when accessing lazy associations
- **Optimize**: Use batch fetching for frequently accessed associations

**Testing Strategy:**
```java
@Test
void testFindAllWithSpecificationAndPageable() {
    // Given
    JobSearchCriteria criteria = new JobSearchCriteria()
        .setOrganisationId(1L)
        .setFacilityId(1L)
        .setStates(Set.of(State.Job.IN_PROGRESS, State.Job.ASSIGNED));
    
    Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
    
    // When
    PageResult<Job> result = jobDAO.findAll(criteria, pageable);
    
    // Then
    assertThat(result.getContent()).hasSize(10);
    assertThat(result.getTotalElements()).isGreaterThan(0);
    assertThat(result.getContent().get(0).getCreatedAt())
        .isGreaterThanOrEqualTo(result.getContent().get(1).getCreatedAt());
}
```

---

### 2. count(Specification) - Override

**Method Signature:**
```java
@Override
long count(@Nullable Specification<Job> specification);
```

**Input Parameters:**
- `specification` (Specification<Job>): Dynamic query criteria
  - Validation: Can be null (counts all records)
  - Purpose: Same filtering logic as findAll but optimized for counting

**Return Type:**
- `long`: Total count of matching records
- Range: 0 to Long.MAX_VALUE

**Generated SQL Query:**
```sql
SELECT COUNT(j.id) 
FROM jobs j 
WHERE [specification conditions];
```

**Database Execution Plan:**
- **Index Usage**: Same as findAll but count-optimized
- **Performance**: O(log n) with proper indexes, O(n) for complex filters
- **Optimization**: Uses covering indexes when possible

**Entity Hydration Details:**
- **No Hydration**: Only count aggregation, no entity objects created
- **Memory Efficient**: Minimal memory footprint

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient
- **Transaction Mode**: READ_ONLY
- **Locking**: No locks required

**Business Logic Integration:**
- **Use Case**: Dashboard counters, pagination metadata
- **Called From**: JobService.getJobCount(), administrative dashboards

**DAO Conversion Strategy:**
```java
public long count(JobSearchCriteria criteria) {
    StringBuilder whereClause = new StringBuilder("WHERE 1=1");
    Map<String, Object> params = new HashMap<>();
    
    // Same filtering logic as findAll
    buildWhereClause(criteria, whereClause, params);
    
    String sql = "SELECT COUNT(*) FROM jobs j " + whereClause.toString();
    return namedJdbcTemplate.queryForObject(sql, params, Long.class);
}
```

---

### 3. updateJobToUnassignedIfNoUserAssigned() - Bulk Update

**Method Signature:**
```java
@Transactional(rollbackFor = Exception.class)
@Modifying(clearAutomatically = true)
@Query(value = Queries.SET_JOB_TO_UNASSIGNED_IF_NO_USER_IS_ASSIGNED, nativeQuery = true)
void updateJobToUnassignedIfNoUserAssigned();
```

**Input Parameters:**
- **None**: This is a parameterless bulk operation

**Return Type:**
- `void`: No return value, modifies database state

**Native SQL Query:**
```sql
UPDATE jobs job
SET state = 'UNASSIGNED'
WHERE job.id IN (
    SELECT te.jobs_id
    FROM task_execution_user_mapping teum
    RIGHT OUTER JOIN task_executions te ON teum.task_executions_id = te.id
    WHERE te.id IN (
        SELECT tex.id
        FROM task_executions tex
        INNER JOIN jobs j ON j.id = tex.jobs_id AND j.state = 'ASSIGNED'
    )
    GROUP BY te.jobs_id
    HAVING COUNT(teum.task_executions_id) = 0
);
```

**Database Execution Plan:**
- **Query Type**: Complex multi-join with RIGHT OUTER JOIN and subqueries
- **Index Requirements**:
  - `idx_task_executions_jobs_id` (critical for join performance)
  - `idx_task_execution_user_mapping_task_executions_id`
  - `idx_jobs_state` (for filtering ASSIGNED jobs)
- **Performance**: O(n * log n) where n = number of task executions
- **Execution Strategy**: 
  1. Find ASSIGNED jobs
  2. Identify task executions for those jobs
  3. Check for user assignments via RIGHT OUTER JOIN
  4. Group and count to find unassigned task executions
  5. Update parent jobs to UNASSIGNED state

**Business Logic Context:**
- **Purpose**: Cleanup operation to fix jobs that were marked ASSIGNED but have no users actually assigned to any tasks
- **When Called**: Scheduled maintenance, data integrity fixes
- **Business Rule**: Jobs should be UNASSIGNED if no task executions have user assignments
- **Data Consistency**: Ensures job state accurately reflects user assignment status

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED minimum
- **Transaction Required**: REQUIRED (modifying operation)
- **Rollback**: Full rollback on any exception
- **Lock Duration**: Row-level locks held until transaction commit
- **Deadlock Risk**: Medium - updates jobs table with complex WHERE clause

**Entity Cache Implications:**
- **L1 Cache**: Cleared automatically due to @Modifying(clearAutomatically = true)
- **L2 Cache**: Would need manual eviction if enabled
- **Query Cache**: Invalidated for related job queries

**DAO Conversion Strategy:**
```java
@Transactional(rollbackFor = Exception.class)
public int updateJobToUnassignedIfNoUserAssigned() {
    String sql = """
        UPDATE jobs 
        SET state = 'UNASSIGNED',
            modified_at = :modifiedAt,
            modified_by = :modifiedBy
        WHERE id IN (
            SELECT DISTINCT te.jobs_id
            FROM task_executions te
            INNER JOIN jobs j ON j.id = te.jobs_id 
            WHERE j.state = 'ASSIGNED'
              AND NOT EXISTS (
                  SELECT 1 
                  FROM task_execution_user_mapping teum 
                  WHERE teum.task_executions_id = te.id
              )
        )
        """;
    
    Map<String, Object> params = Map.of(
        "modifiedAt", System.currentTimeMillis(),
        "modifiedBy", getCurrentUserId() // From security context
    );
    
    int updatedRows = namedJdbcTemplate.update(sql, params);
    
    // Clear relevant caches
    cacheManager.getCache("jobs").clear();
    
    log.info("Updated {} jobs from ASSIGNED to UNASSIGNED due to no user assignments", updatedRows);
    
    return updatedRows;
}
```

**Performance Considerations:**
- **Index Strategy**: Composite index on (jobs_id, state) for task_executions table
- **Batch Size**: Consider chunking for large datasets
- **Monitoring**: Log execution time and affected row count
- **Off-Peak**: Should be run during low-traffic periods

**Testing Strategy:**
```java
@Test
@Transactional
void testUpdateJobToUnassignedIfNoUserAssigned() {
    // Given - Create ASSIGNED job with no user assignments
    Job job = createTestJob(State.Job.ASSIGNED);
    TaskExecution taskExecution = createTestTaskExecution(job);
    // Intentionally no user mappings created
    
    // When
    int updatedRows = jobDAO.updateJobToUnassignedIfNoUserAssigned();
    
    // Then
    assertThat(updatedRows).isEqualTo(1);
    Job updatedJob = jobDAO.findById(job.getId()).orElseThrow();
    assertThat(updatedJob.getState()).isEqualTo(State.Job.UNASSIGNED);
}
```

---

### 4. findByChecklistIdWhereStateNotIn() - State Validation

**Method Signature:**
```java
@Query(value = Queries.IS_ACTIVE_JOB_EXIST_FOR_GIVEN_CHECKLIST)
boolean findByChecklistIdWhereStateNotIn(@Param("checklistId") Long checklistId, 
                                       @Param("jobStates") Set<State.Job> jobStates);
```

**Input Parameters:**
- `checklistId` (Long): Unique identifier of the checklist
  - Validation: Cannot be null, must be valid checklist ID
  - Business Rule: Must be an existing, non-archived checklist
  - Purpose: Check for active jobs for this checklist
- `jobStates` (Set<State.Job>): Set of job states to exclude from check
  - Validation: Cannot be null or empty
  - Common Values: {COMPLETED, COMPLETED_WITH_EXCEPTION} to check for active jobs
  - Purpose: Define what constitutes "inactive" jobs

**Return Type:**
- `boolean`: True if active jobs exist, false otherwise
- **True**: Checklist has jobs not in the excluded states
- **False**: No active jobs or no jobs at all

**Generated JPQL Query:**
```sql
SELECT CASE WHEN COUNT(j.id) > 0 THEN true ELSE false END 
FROM Job j 
WHERE j.checklist.id = :checklistId 
  AND j.state NOT IN :jobStates;
```

**Translated SQL:**
```sql
SELECT CASE WHEN COUNT(j.id) > 0 THEN 1 ELSE 0 END 
FROM jobs j 
WHERE j.checklists_id = ? 
  AND j.state NOT IN (?, ?, ?);
```

**Database Execution Plan:**
- **Index Usage**: 
  - Primary: `idx_jobs_checklists_id` for checklist filtering
  - Secondary: `idx_jobs_state` for state filtering
  - Optimal: Composite index `idx_jobs_checklists_id_state`
- **Performance**: O(log n) with proper indexes
- **Execution Strategy**: Index scan with COUNT aggregation

**Entity Hydration Details:**
- **No Hydration**: Only boolean result, no entities loaded
- **Memory Efficient**: Minimal memory usage
- **Aggregation Only**: Uses COUNT function optimization

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient
- **Transaction Mode**: READ_ONLY
- **Consistency**: Point-in-time check, may change immediately after

**Business Logic Integration:**
- **Use Case**: Checklist validation before deletion/archiving
- **Called From**: ChecklistService.canDeleteChecklist(), ChecklistService.canArchiveChecklist()
- **Business Rule**: Checklists with active jobs cannot be deleted or archived
- **Workflow**: Pre-condition check in checklist lifecycle management

**DAO Conversion Strategy:**
```java
public boolean hasActiveJobsForChecklist(Long checklistId, Set<State.Job> excludedStates) {
    if (checklistId == null || excludedStates == null || excludedStates.isEmpty()) {
        throw new IllegalArgumentException("ChecklistId and excludedStates cannot be null/empty");
    }
    
    String sql = """
        SELECT CASE WHEN COUNT(j.id) > 0 THEN 1 ELSE 0 END 
        FROM jobs j 
        WHERE j.checklists_id = :checklistId 
          AND j.state NOT IN (:excludedStates)
        """;
    
    Map<String, Object> params = Map.of(
        "checklistId", checklistId,
        "excludedStates", excludedStates.stream().map(Enum::name).collect(toList())
    );
    
    Integer result = namedJdbcTemplate.queryForObject(sql, params, Integer.class);
    return result != null && result > 0;
}
```

**Performance Considerations:**
- **Index Required**: Composite index `(checklists_id, state)` for optimal performance
- **Query Optimization**: Uses COUNT with CASE for boolean result
- **Cache Candidate**: Result could be cached with TTL for frequently checked checklists

**Testing Strategy:**
```java
@Test
void testHasActiveJobsForChecklist() {
    // Given
    Long checklistId = 1L;
    createTestJob(checklistId, State.Job.IN_PROGRESS);
    createTestJob(checklistId, State.Job.COMPLETED);
    
    Set<State.Job> excludedStates = Set.of(State.Job.COMPLETED, State.Job.COMPLETED_WITH_EXCEPTION);
    
    // When
    boolean hasActiveJobs = jobDAO.hasActiveJobsForChecklist(checklistId, excludedStates);
    
    // Then
    assertThat(hasActiveJobs).isTrue(); // IN_PROGRESS job should be found
}

@Test 
void testHasActiveJobsForChecklistNoActiveJobs() {
    // Given
    Long checklistId = 1L;
    createTestJob(checklistId, State.Job.COMPLETED);
    
    Set<State.Job> excludedStates = Set.of(State.Job.COMPLETED, State.Job.COMPLETED_WITH_EXCEPTION);
    
    // When
    boolean hasActiveJobs = jobDAO.hasActiveJobsForChecklist(checklistId, excludedStates);
    
    // Then
    assertThat(hasActiveJobs).isFalse(); // Only COMPLETED job should be excluded
}
```

---

### 5. findJobProcessInfo() - Process Information Projection

**Method Signature:**
```java
@Query(value = Queries.FIND_JOB_PROCESS_INFO, nativeQuery = true)
JobProcessInfoView findJobProcessInfo(@Param("jobId") Long jobId);
```

**Input Parameters:**
- `jobId` (Long): Unique identifier of the job
  - Validation: Cannot be null, must be valid job ID
  - Business Rule: Must be an existing job
  - Purpose: Retrieve process information for the job

**Return Type:**
- `JobProcessInfoView`: Projection interface containing process details
  - `getJobId()`: String representation of job ID
  - `getJobCode()`: Job's unique code
  - `getProcessName()`: Associated checklist/process name
  - `getProcessId()`: String representation of checklist ID
  - `getProcessCode()`: Checklist's unique code
- **Null**: If job doesn't exist

**Native SQL Query:**
```sql
SELECT j.id as jobId, 
       j.code as jobCode, 
       c.name as processName, 
       c.id as processId, 
       c.code as processCode
FROM jobs j
INNER JOIN checklists c ON j.checklists_id = c.id
WHERE j.id = :jobId;
```

**Database Execution Plan:**
- **Index Usage**:
  - Primary: `jobs_pkey` on jobs.id (unique lookup)
  - Foreign Key: Automatic index on `j.checklists_id` for join
- **Join Type**: Nested loop join (optimal for single-row lookup)
- **Performance**: O(log n) - very efficient for single job lookup
- **Result**: Single row or empty result set

**Entity Hydration Details:**
- **No Entity Hydration**: Returns projection interface, not full entities
- **Lightweight**: Only required fields loaded
- **Memory Efficient**: Minimal object creation
- **Projection Mapping**: Spring Data maps result set to interface methods

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient
- **Transaction Mode**: READ_ONLY appropriate
- **Consistency**: Point-in-time snapshot of job-process relationship

**Business Logic Integration:**
- **Use Case**: Job detail displays, process navigation, audit logging
- **Called From**: JobService.getJobProcessInfo(), JobController.getJobDetails()
- **Display Context**: User interfaces showing job context within process
- **Navigation**: Breadcrumb generation, process hierarchy displays

**DAO Conversion Strategy:**
```java
public JobProcessInfo findJobProcessInfo(Long jobId) {
    if (jobId == null) {
        throw new IllegalArgumentException("JobId cannot be null");
    }
    
    String sql = """
        SELECT j.id as job_id, 
               j.code as job_code, 
               c.name as process_name, 
               c.id as process_id, 
               c.code as process_code
        FROM jobs j
        INNER JOIN checklists c ON j.checklists_id = c.id
        WHERE j.id = :jobId
        """;
    
    Map<String, Object> params = Map.of("jobId", jobId);
    
    try {
        return namedJdbcTemplate.queryForObject(sql, params, new JobProcessInfoRowMapper());
    } catch (EmptyResultDataAccessException e) {
        return null; // Job not found
    }
}

public static class JobProcessInfo {
    private final String jobId;
    private final String jobCode;
    private final String processName;
    private final String processId;
    private final String processCode;
    
    // Constructor, getters, equals, hashCode
}

public static class JobProcessInfoRowMapper implements RowMapper<JobProcessInfo> {
    @Override
    public JobProcessInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new JobProcessInfo(
            rs.getString("job_id"),
            rs.getString("job_code"),
            rs.getString("process_name"),
            rs.getString("process_id"),
            rs.getString("process_code")
        );
    }
}
```

**Performance Considerations:**
- **Very Fast**: Single-row lookup with indexed join
- **Cache Candidate**: Process info rarely changes, good for caching
- **No N+1 Risk**: Single query with join

**Testing Strategy:**
```java
@Test
void testFindJobProcessInfo() {
    // Given
    Checklist checklist = createTestChecklist("Process A", "PROC-001");
    Job job = createTestJob("JOB-001", checklist);
    
    // When
    JobProcessInfo processInfo = jobDAO.findJobProcessInfo(job.getId());
    
    // Then
    assertThat(processInfo).isNotNull();
    assertThat(processInfo.getJobId()).isEqualTo(job.getId().toString());
    assertThat(processInfo.getJobCode()).isEqualTo("JOB-001");
    assertThat(processInfo.getProcessName()).isEqualTo("Process A");
    assertThat(processInfo.getProcessCode()).isEqualTo("PROC-001");
}

@Test
void testFindJobProcessInfoNotFound() {
    // When
    JobProcessInfo processInfo = jobDAO.findJobProcessInfo(999L);
    
    // Then
    assertThat(processInfo).isNull();
}
```

---

### 6. isJobExistsBySchedulerIdAndDateGreaterThanOrEqualToExpectedStartDate() - Scheduler Validation

**Method Signature:**
```java
@Query(value = Queries.IS_JOB_EXISTS_BY_SCHEDULER_ID_AND_DATE_GREATER_THAN_EXPECTED_START_DATE)
boolean isJobExistsBySchedulerIdAndDateGreaterThanOrEqualToExpectedStartDate(
    @Param("schedulerId") Long schedulerId, 
    @Param("date") Long date);
```

**Input Parameters:**
- `schedulerId` (Long): Unique identifier of the scheduler
  - Validation: Cannot be null, must be valid scheduler ID
  - Purpose: Check for jobs created by this scheduler
  - Business Context: Scheduler instance creating recurring jobs
- `date` (Long): Timestamp to compare against expected start date
  - Validation: Cannot be null, must be valid Unix timestamp
  - Purpose: Find jobs scheduled at or after this date
  - Business Context: Prevent duplicate job creation for same schedule period

**Return Type:**
- `boolean`: True if matching jobs exist, false otherwise
- **True**: Scheduler has jobs with expected start date >= provided date
- **False**: No matching jobs found

**Generated JPQL Query:**
```sql
SELECT CASE WHEN COUNT(j.id) > 0 THEN true ELSE false END 
FROM Job j 
WHERE j.schedulerId = :schedulerId 
  AND j.expectedStartDate >= :date;
```

**Translated SQL:**
```sql
SELECT CASE WHEN COUNT(j.id) > 0 THEN 1 ELSE 0 END 
FROM jobs j 
WHERE j.schedulers_id = ? 
  AND j.expected_start_date >= ?;
```

**Database Execution Plan:**
- **Index Usage**:
  - Primary: `idx_jobs_schedulers_id` for scheduler filtering
  - Secondary: Range scan on `expected_start_date`
  - Optimal: Composite index `idx_jobs_schedulers_id_expected_start_date`
- **Performance**: O(log n) with proper composite index
- **Query Type**: Range query with COUNT aggregation

**Business Logic Context:**
- **Purpose**: Prevent duplicate scheduled job creation
- **Use Case**: Scheduler validation before creating new jobs
- **Business Rule**: Don't create jobs if scheduler already has jobs for the same or later time period
- **Workflow**: Called during scheduled job creation process

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient for validation
- **Transaction Mode**: READ_ONLY
- **Race Condition**: Potential for duplicate creation in high-concurrency scenarios
- **Mitigation**: Should be combined with unique constraints

**DAO Conversion Strategy:**
```java
public boolean hasScheduledJobsAfterDate(Long schedulerId, Long date) {
    if (schedulerId == null || date == null) {
        throw new IllegalArgumentException("SchedulerId and date cannot be null");
    }
    
    String sql = """
        SELECT CASE WHEN COUNT(j.id) > 0 THEN 1 ELSE 0 END 
        FROM jobs j 
        WHERE j.schedulers_id = :schedulerId 
          AND j.expected_start_date >= :date
        """;
    
    Map<String, Object> params = Map.of(
        "schedulerId", schedulerId,
        "date", date
    );
    
    Integer result = namedJdbcTemplate.queryForObject(sql, params, Integer.class);
    return result != null && result > 0;
}
```

**Performance Considerations:**
- **Index Required**: Composite index `(schedulers_id, expected_start_date)` for optimal performance
- **Query Efficiency**: COUNT with early termination after finding first match
- **Concurrent Access**: May need row-level locking for scheduler entity

**Testing Strategy:**
```java
@Test
void testHasScheduledJobsAfterDate() {
    // Given
    Long schedulerId = 1L;
    Long baseDate = System.currentTimeMillis();
    
    createScheduledJob(schedulerId, baseDate + 3600000); // 1 hour later
    createScheduledJob(schedulerId, baseDate - 3600000); // 1 hour earlier
    
    // When - Check for jobs after base date
    boolean hasJobs = jobDAO.hasScheduledJobsAfterDate(schedulerId, baseDate);
    
    // Then
    assertThat(hasJobs).isTrue(); // Should find the job 1 hour later
}

@Test
void testHasScheduledJobsAfterDateNoMatches() {
    // Given
    Long schedulerId = 1L;
    Long baseDate = System.currentTimeMillis();
    
    createScheduledJob(schedulerId, baseDate - 3600000); // 1 hour earlier
    
    // When - Check for jobs after base date
    boolean hasJobs = jobDAO.hasScheduledJobsAfterDate(schedulerId, baseDate + 1000);
    
    // Then
    assertThat(hasJobs).isFalse(); // Should not find any jobs
}
```

---

### 7. getAllPendingForApprovalParameters() - Complex Approval Query

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PENDING_FOR_APPROVAL_PARAMETER_STATUS, nativeQuery = true)
Page<PendingForApprovalStatusView> getAllPendingForApprovalParameters(
    @Param("facilityId") long facilityId,
    @Param("parameterName") String parameterName,
    @Param("processName") String processName, 
    @Param("objectId") String objectId,
    @Param("jobId") String jobId, 
    @Param("userId") String userId, 
    @Param("useCaseId") Long useCaseId,
    @Param("showAllException") boolean showAllException, 
    @Param("requestedBy") Long requestedBy, 
    Pageable pageable);
```

**Input Parameters:**
- `facilityId` (long): Facility identifier for scoping results
  - Validation: Must be valid facility ID
  - Purpose: Multi-tenant data isolation
- `parameterName` (String): Optional filter for parameter label
  - Validation: Can be null for no filtering
  - Purpose: Search by parameter name using ILIKE
- `processName` (String): Optional filter for process/checklist name
  - Validation: Can be null for no filtering  
  - Purpose: Search by process name using ILIKE
- `objectId` (String): Optional filter for resource object ID
  - Validation: Can be null for no filtering
  - Purpose: Filter by JSONB choices containing specific objectId
- `jobId` (String): Optional filter for specific job
  - Validation: Can be null for no filtering
  - Purpose: Scope to single job's parameters
- `userId` (String): User ID for permission filtering
  - Validation: Can be null for admin access
  - Purpose: Show only parameters user can review
- `useCaseId` (Long): Optional use case filter
  - Validation: Can be null for no filtering
  - Purpose: Scope to specific use case
- `showAllException` (boolean): Permission flag for viewing all exceptions
  - Purpose: Admin users can see all, regular users see only their own
- `requestedBy` (Long): Optional filter for exception initiator
  - Validation: Can be null for no filtering
  - Purpose: Filter by who requested the exception
- `pageable` (Pageable): Pagination configuration

**Return Type:**
- `Page<PendingForApprovalStatusView>`: Paginated approval queue
- Projection fields:
  - `parameterValueId`: ID of parameter value needing approval
  - `parameterId`: ID of the parameter definition
  - `jobId`: Job containing the parameter
  - `parameterName`: Display name of parameter
  - `taskName`: Task containing the parameter
  - `processName`: Process/checklist name
  - `modifiedAt`: When parameter was last modified
  - `jobCode`: Job's unique code
  - `taskId`: Task ID
  - `taskExecutionId`: Task execution instance ID
  - `createdAt`: When exception was created
  - `exceptionInitiatedBy`: User who initiated the exception
  - `rulesId`: Rule that triggered the exception

**Complex Native SQL Query:**
```sql
SELECT DISTINCT ON (e.id, e.rules_id)
       pv.id          AS parameterValueId,
       p.id           AS parameterId,
       pv.jobs_id     AS jobId,
       p.label        AS parameterName,
       t.name         AS taskName,
       cl.name        AS processName,
       pv.modified_at AS modifiedAt,
       j.code         AS jobCode,
       t.id           AS taskId,
       te.id          AS taskExecutionId,
       pv.created_at  AS createdAt,
       e.created_by   AS exceptionInitiatedBy,
       rules_id       AS rulesId

FROM exceptions e
         JOIN parameter_values pv ON e.parameter_values_id = pv.id
         JOIN jobs j ON e.jobs_id = j.id
         JOIN parameters p ON pv.parameters_id = p.id
         JOIN exception_reviewers er ON e.id = er.exceptions_id
         JOIN checklists cl ON j.checklists_id = cl.id
         INNER JOIN users u ON u.id = e.created_by
         LEFT JOIN tasks t ON t.id = p.tasks_id
         LEFT JOIN task_executions te ON pv.task_executions_id = te.id
WHERE e.facilities_id = :facilityId
  AND pv.has_exceptions = TRUE
  AND (pv.state = 'PENDING_FOR_APPROVAL')
  AND e.status = 'INITIATED'
  AND (j.state = 'IN_PROGRESS' or j.state = 'ASSIGNED')
  AND (CAST(:jobId AS VARCHAR) IS NULL OR j.id = :jobId)
  AND (:requestedBy IS NULL OR e.created_by = :requestedBy)
  AND (:showAllException = TRUE OR :userId IN (er.users_id, e.created_by))
  AND ((CAST(:parameterName AS VARCHAR) IS NULL OR p.label ILIKE CONCAT('%', CAST(:parameterName AS VARCHAR), '%'))
    OR (CAST(:processName AS VARCHAR) IS NULL OR cl.name ILIKE CONCAT('%', CAST(:processName AS VARCHAR), '%')))
  AND (CAST(:objectId AS VARCHAR) IS NULL OR j.id IN (SELECT pv.jobs_id
                                                      FROM parameter_values pv
                                                               INNER JOIN parameters p ON p.id = pv.parameters_id
                                                               CROSS JOIN JSONB_ARRAY_ELEMENTS(pv.choices) AS choice
                                                      WHERE choice ->> 'objectId' = :objectId
                                                        AND p.type = 'RESOURCE'))
AND (:useCaseId IS NULL OR j.use_cases_id = :useCaseId)
ORDER BY e.id, e.rules_id;
```

**Database Execution Plan:**
- **Complexity**: Very high - multiple JOINs, subqueries, JSONB operations
- **Primary Indexes**:
  - `idx_exceptions_facilities_id_status` (critical)
  - `idx_parameter_values_state_has_exceptions` (critical)
  - `idx_jobs_state` for job state filtering
- **Join Strategy**: Hash joins for large result sets
- **JSONB Performance**: GIN index on `parameter_values.choices` required
- **DISTINCT ON**: PostgreSQL-specific, requires careful ordering
- **Performance**: O(n * log n) where n = exceptions in facility

**Entity Hydration Details:**
- **No Entity Hydration**: Returns projection interface only
- **Complex Joins**: Data from 7+ tables aggregated into projection
- **JSONB Parsing**: choices field parsed for objectId matching
- **Permission Filtering**: User access control built into query

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient
- **Transaction Mode**: READ_ONLY
- **Consistency**: Complex multi-table snapshot
- **Performance Impact**: Long-running query, consider timeout

**Business Logic Context:**
- **Purpose**: Exception approval queue for quality control
- **Use Case**: QA dashboards, supervisor approval workflows
- **Business Rules**:
  - Only show parameters pending approval
  - Respect user permissions for viewing exceptions
  - Filter by facility for multi-tenant isolation
  - Only show active jobs (IN_PROGRESS, ASSIGNED)
- **Workflow Integration**: Central to quality exception handling process

**DAO Conversion Strategy:**
```java
public Page<PendingForApprovalStatus> getAllPendingForApprovalParameters(
        PendingApprovalSearchCriteria criteria, Pageable pageable) {
    
    // Build base query with all joins
    StringBuilder sql = new StringBuilder("""
        SELECT e.id as exception_id,
               pv.id as parameter_value_id,
               p.id as parameter_id,
               pv.jobs_id as job_id,
               p.label as parameter_name,
               t.name as task_name,
               cl.name as process_name,
               pv.modified_at,
               j.code as job_code,
               t.id as task_id,
               te.id as task_execution_id,
               pv.created_at,
               e.created_by as exception_initiated_by,
               e.rules_id
        FROM exceptions e
        INNER JOIN parameter_values pv ON e.parameter_values_id = pv.id
        INNER JOIN jobs j ON e.jobs_id = j.id
        INNER JOIN parameters p ON pv.parameters_id = p.id
        INNER JOIN exception_reviewers er ON e.id = er.exceptions_id
        INNER JOIN checklists cl ON j.checklists_id = cl.id
        INNER JOIN users u ON u.id = e.created_by
        LEFT JOIN tasks t ON t.id = p.tasks_id
        LEFT JOIN task_executions te ON pv.task_executions_id = te.id
        WHERE e.facilities_id = :facilityId
          AND pv.has_exceptions = TRUE
          AND pv.state = 'PENDING_FOR_APPROVAL'
          AND e.status = 'INITIATED'
          AND j.state IN ('IN_PROGRESS', 'ASSIGNED')
        """);
    
    Map<String, Object> params = new HashMap<>();
    params.put("facilityId", criteria.getFacilityId());
    
    // Add optional filters
    addOptionalFilters(sql, params, criteria);
    
    // Add permission filtering
    if (!criteria.isShowAllException()) {
        sql.append(" AND (:userId = er.users_id OR :userId = e.created_by)");
        params.put("userId", criteria.getUserId());
    }
    
    // Count query
    String countSql = buildCountQuery(sql.toString());
    Long totalCount = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
    
    // Add ordering and pagination
    sql.append(" ORDER BY pv.modified_at DESC LIMIT :limit OFFSET :offset");
    params.put("limit", pageable.getPageSize());
    params.put("offset", pageable.getOffset());
    
    List<PendingForApprovalStatus> results = namedJdbcTemplate.query(
        sql.toString(), params, new PendingForApprovalStatusRowMapper());
    
    return new PageImpl<>(results, pageable, totalCount);
}

private void addOptionalFilters(StringBuilder sql, Map<String, Object> params, 
                               PendingApprovalSearchCriteria criteria) {
    if (StringUtils.hasText(criteria.getParameterName())) {
        sql.append(" AND p.label ILIKE :parameterName");
        params.put("parameterName", "%" + criteria.getParameterName() + "%");
    }
    
    if (StringUtils.hasText(criteria.getProcessName())) {
        sql.append(" AND cl.name ILIKE :processName");
        params.put("processName", "%" + criteria.getProcessName() + "%");
    }
    
    if (StringUtils.hasText(criteria.getObjectId())) {
        sql.append("""
             AND j.id IN (
                 SELECT pv_sub.jobs_id 
                 FROM parameter_values pv_sub
                 INNER JOIN parameters p_sub ON p_sub.id = pv_sub.parameters_id
                 WHERE p_sub.type = 'RESOURCE'
                   AND pv_sub.choices @> :objectIdJson
             )
            """);
        params.put("objectIdJson", String.format("[{\"objectId\":\"%s\"}]", criteria.getObjectId()));
    }
    
    // Add other optional filters...
}
```

**Performance Considerations:**
- **Critical Indexes**:
  - `idx_exceptions_facilities_id_status_created_at` (composite)
  - `idx_parameter_values_state_has_exceptions_modified_at` (composite)
  - GIN index on `parameter_values.choices` for JSONB queries
- **Query Optimization**:
  - Consider materialized view for complex aggregations
  - Partition exceptions table by facility_id if large
  - Use connection pooling for long-running queries
- **Monitoring**: Track query execution time and optimize slow cases

**Testing Strategy:**
```java
@Test
void testGetAllPendingForApprovalParameters() {
    // Given - Create test data
    createPendingApprovalScenario();
    
    PendingApprovalSearchCriteria criteria = new PendingApprovalSearchCriteria()
        .setFacilityId(1L)
        .setUserId(100L)
        .setShowAllException(false);
    
    Pageable pageable = PageRequest.of(0, 10);
    
    // When
    Page<PendingForApprovalStatus> results = 
        jobDAO.getAllPendingForApprovalParameters(criteria, pageable);
    
    // Then
    assertThat(results.getContent()).isNotEmpty();
    assertThat(results.getContent().get(0).getState()).isEqualTo("PENDING_FOR_APPROVAL");
}
```

---

### 8. findAllByChecklistId() - Simple Finder

**Method Signature:**
```java
List<Job> findAllByChecklistId(Long checklistId);
```

**Input Parameters:**
- `checklistId` (Long): Unique identifier of the checklist
  - Validation: Cannot be null
  - Purpose: Find all jobs created from this checklist
  - Business Context: Jobs are instances of checklist execution

**Return Type:**
- `List<Job>`: All jobs for the checklist
- **Empty List**: If no jobs exist for checklist
- **Ordered**: Natural ordering (typically by ID/creation time)

**Generated SQL Query:**
```sql
SELECT j.id, j.code, j.state, j.checklists_id, j.facilities_id, j.organisations_id,
       j.use_cases_id, j.started_at, j.started_by, j.ended_at, j.ended_by,
       j.is_scheduled, j.schedulers_id, j.expected_start_date, j.expected_end_date,
       j.checklist_ancestor_id, j.created_at, j.modified_at, j.created_by, j.modified_by
FROM jobs j 
WHERE j.checklists_id = ?
ORDER BY j.id;
```

**Database Execution Plan:**
- **Index Usage**: `idx_jobs_checklists_id` for efficient lookup
- **Performance**: O(log n + k) where k = number of matching jobs
- **Query Type**: Simple equality filter with potential for many results

**Entity Hydration Details:**
- **Full Entity**: Complete Job entities loaded
- **Lazy Associations**: All related entities remain as lazy proxies
- **Memory Impact**: Proportional to number of jobs for checklist

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient
- **Transaction Mode**: READ_ONLY appropriate
- **Consistency**: Snapshot of jobs at query time

**Business Logic Integration:**
- **Use Case**: Checklist analytics, job history, bulk operations
- **Called From**: ChecklistService.getChecklistJobs(), reporting services
- **Common Pattern**: Often followed by job-specific operations

**DAO Conversion Strategy:**
```java
public List<Job> findAllByChecklistId(Long checklistId) {
    if (checklistId == null) {
        throw new IllegalArgumentException("ChecklistId cannot be null");
    }
    
    String sql = """
        SELECT j.id, j.code, j.state, j.checklists_id, j.facilities_id, j.organisations_id,
               j.use_cases_id, j.started_at, j.started_by, j.ended_at, j.ended_by,
               j.is_scheduled, j.schedulers_id, j.expected_start_date, j.expected_end_date,
               j.checklist_ancestor_id, j.created_at, j.modified_at, j.created_by, j.modified_by
        FROM jobs j 
        WHERE j.checklists_id = :checklistId
        ORDER BY j.created_at DESC
        """;
    
    Map<String, Object> params = Map.of("checklistId", checklistId);
    
    return namedJdbcTemplate.query(sql, params, new JobRowMapper());
}
```

**Performance Considerations:**
- **Index Required**: `idx_jobs_checklists_id` for efficient lookup
- **Large Result Sets**: Consider pagination for checklists with many jobs
- **Memory Usage**: Full entity loading - monitor for large job counts

**Testing Strategy:**
```java
@Test
void testFindAllByChecklistId() {
    // Given
    Long checklistId = 1L;
    createTestJob(checklistId, State.Job.IN_PROGRESS);
    createTestJob(checklistId, State.Job.COMPLETED);
    createTestJob(2L, State.Job.IN_PROGRESS); // Different checklist
    
    // When
    List<Job> jobs = jobDAO.findAllByChecklistId(checklistId);
    
    // Then
    assertThat(jobs).hasSize(2);
    assertThat(jobs).allMatch(job -> job.getChecklistId().equals(checklistId));
}
```

---

### 9. getMyJobs() - Complex User-Specific Query

**Method Signature:**
```java
@Query(value = Queries.GET_MY_JOBS, nativeQuery = true)
List<IdView> getMyJobs(@Param("organisationId") Long organisationId, 
                       @Param("facilityId") Long facilityId, 
                       @Param("usecaseId") Long usecaseId, 
                       @Param("jobStates") List<String> jobStates, 
                       @Param("taskExecutionStates") List<String> taskExecutionStates, 
                       @Param("userId") Long userId, 
                       @Param("objectId") String objectId, 
                       @Param("pom") boolean pom, 
                       @Param("checklistAncestorId") Long checklistAncestorId, 
                       @Param("name") String name, 
                       @Param("code") String code, 
                       @Param("limit") int limit, 
                       @Param("offset") long offset);
```

**Input Parameters:**
- `organisationId` (Long): Organisation scope for multi-tenancy
  - Validation: Cannot be null
  - Purpose: Data isolation by organisation
- `facilityId` (Long): Facility scope within organisation
  - Validation: Cannot be null
  - Purpose: Further scope data by facility
- `usecaseId` (Long): Use case filter
  - Validation: Cannot be null
  - Purpose: Filter jobs by business use case
- `jobStates` (List<String>): Allowed job states
  - Validation: Cannot be null or empty
  - Purpose: Filter jobs by current state
  - Common Values: ["IN_PROGRESS", "ASSIGNED"]
- `taskExecutionStates` (List<String>): Allowed task execution states
  - Validation: Cannot be null or empty
  - Purpose: Find jobs with tasks in specific states
  - Common Values: ["IN_PROGRESS", "ASSIGNED", "NOT_STARTED"]
- `userId` (Long): User ID for assignment filtering
  - Validation: Cannot be null
  - Purpose: Show only jobs assigned to this user
- `objectId` (String): Optional resource object filter
  - Validation: Can be null
  - Purpose: Filter by JSONB resource parameters
- `pom` (boolean): "Parameters Only Mode" flag
  - Purpose: Filter out task executions without parameters
- `checklistAncestorId` (Long): Optional checklist family filter
  - Validation: Can be null
  - Purpose: Filter by checklist hierarchy
- `name` (String): Optional checklist name filter
  - Validation: Can be null
  - Purpose: Search by checklist name
- `code` (String): Optional job code filter
  - Validation: Can be null
  - Purpose: Search by job code
- `limit` (int): Maximum results to return
  - Validation: Must be positive
  - Purpose: Pagination limit
- `offset` (long): Number of results to skip
  - Validation: Must be non-negative
  - Purpose: Pagination offset

**Return Type:**
- `List<IdView>`: Lightweight projection with only ID and creation timestamp
- Purpose: Optimized for dashboard job lists requiring minimal data

**Complex Native SQL Query:**
```sql
SELECT DISTINCT j.id as id, j.created_at as createdAt
FROM jobs j
         JOIN task_executions te ON te.jobs_id = j.id
         JOIN task_execution_user_mapping teum ON teum.task_executions_id = te.id
         JOIN checklists c ON j.checklists_id = c.id
WHERE j.organisations_id = :organisationId
  AND j.facilities_id = :facilityId
  AND j.use_cases_id = :usecaseId
  AND teum.users_id = :userId
  AND te.state IN :taskExecutionStates
  AND j.state IN :jobStates
  AND (CAST(:checklistAncestorId as VARCHAR) is NULL or j.checklist_ancestor_id = :checklistAncestorId)
  AND (CAST(:name as VARCHAR) is NULL or c.name ilike CONCAT('%', CAST(:name AS VARCHAR), '%'))
  AND (CAST(:code as VARCHAR) is NULL or j.code ilike CONCAT('%', CAST(:code AS VARCHAR), '%'))
  AND (:pom = false or te.id NOT IN (SELECT id
                                     FROM (SELECT te.id
                                           FROM task_executions te
                                           WHERE te.jobs_id = j.id
                                             AND NOT EXISTS (SELECT 1
                                                             FROM parameter_values pv
                                                             WHERE pv.task_executions_id = te.id
                                                               AND pv.hidden = false)) as tpi))
  AND (CAST(:objectId AS VARCHAR) IS NULL or j.id in (SELECT pv.jobs_id
                                                      FROM parameter_values pv
                                                               inner join parameters p on p.id = pv.parameters_id
                                                               cross join jsonb_array_elements(pv.choices) AS choice
                                                      WHERE choice ->> 'objectId' = :objectId
                                                        and p.type = 'RESOURCE'
                                                        and p.target_entity_type = 'PROCESS'))
order by j.created_at desc
LIMIT :limit OFFSET :offset;
```

**Database Execution Plan:**
- **Complexity**: Very high - multiple JOINs, subqueries, JSONB operations
- **Critical Indexes**:
  - `idx_jobs_org_facility_usecase_state` (composite)
  - `idx_task_executions_jobs_id_state` (composite)
  - `idx_task_execution_user_mapping_user_id_state` (composite)
  - GIN index on `parameter_values.choices` for JSONB queries
- **Join Strategy**: Hash joins for large intermediate results
- **Performance**: O(n * log n) where n = task executions for user
- **Distinct Operation**: Expensive, requires sorting/hashing

**Entity Hydration Details:**
- **Minimal Projection**: Only ID and createdAt loaded
- **No Entity Objects**: Returns lightweight projection interface
- **Memory Efficient**: Minimal memory footprint per result

**Business Logic Context:**
- **Purpose**: User dashboard showing "My Jobs" - jobs assigned to current user
- **Use Case**: Primary dashboard query for operators and supervisors
- **Complex Filtering**: Supports advanced search and filtering capabilities
- **Performance Critical**: This is a frequently executed, user-facing query

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient
- **Transaction Mode**: READ_ONLY
- **Consistency**: Multi-table snapshot for user's current job assignments
- **Timeout**: Consider query timeout for complex filters

**DAO Conversion Strategy:**
```java
public List<UserJobSummary> getMyJobs(MyJobsSearchCriteria criteria) {
    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT j.id, j.created_at, j.code, j.state
        FROM jobs j
        INNER JOIN task_executions te ON te.jobs_id = j.id
        INNER JOIN task_execution_user_mapping teum ON teum.task_executions_id = te.id
        INNER JOIN checklists c ON j.checklists_id = c.id
        WHERE j.organisations_id = :organisationId
          AND j.facilities_id = :facilityId
          AND j.use_cases_id = :usecaseId
          AND teum.users_id = :userId
          AND te.state IN (:taskExecutionStates)
          AND j.state IN (:jobStates)
        """);
    
    Map<String, Object> params = new HashMap<>();
    params.put("organisationId", criteria.getOrganisationId());
    params.put("facilityId", criteria.getFacilityId());
    params.put("usecaseId", criteria.getUsecaseId());
    params.put("userId", criteria.getUserId());
    params.put("taskExecutionStates", criteria.getTaskExecutionStates());
    params.put("jobStates", criteria.getJobStates());
    
    // Add optional filters
    addMyJobsFilters(sql, params, criteria);
    
    // Add ordering and pagination
    sql.append(" ORDER BY j.created_at DESC LIMIT :limit OFFSET :offset");
    params.put("limit", criteria.getLimit());
    params.put("offset", criteria.getOffset());
    
    return namedJdbcTemplate.query(sql.toString(), params, new UserJobSummaryRowMapper());
}

private void addMyJobsFilters(StringBuilder sql, Map<String, Object> params, 
                             MyJobsSearchCriteria criteria) {
    if (criteria.getChecklistAncestorId() != null) {
        sql.append(" AND j.checklist_ancestor_id = :checklistAncestorId");
        params.put("checklistAncestorId", criteria.getChecklistAncestorId());
    }
    
    if (StringUtils.hasText(criteria.getName())) {
        sql.append(" AND c.name ILIKE :name");
        params.put("name", "%" + criteria.getName() + "%");
    }
    
    if (StringUtils.hasText(criteria.getCode())) {
        sql.append(" AND j.code ILIKE :code");
        params.put("code", "%" + criteria.getCode() + "%");
    }
    
    if (criteria.isPom()) {
        sql.append("""
             AND te.id NOT IN (
                 SELECT te_sub.id
                 FROM task_executions te_sub
                 WHERE te_sub.jobs_id = j.id
                   AND NOT EXISTS (
                       SELECT 1 FROM parameter_values pv
                       WHERE pv.task_executions_id = te_sub.id
                         AND pv.hidden = false
                   )
             )
            """);
    }
    
    if (StringUtils.hasText(criteria.getObjectId())) {
        sql.append("""
             AND j.id IN (
                 SELECT pv.jobs_id 
                 FROM parameter_values pv
                 INNER JOIN parameters p ON p.id = pv.parameters_id
                 WHERE p.type = 'RESOURCE'
                   AND p.target_entity_type = 'PROCESS'
                   AND pv.choices @> :objectIdJson
             )
            """);
        params.put("objectIdJson", 
            String.format("[{\"objectId\":\"%s\"}]", criteria.getObjectId()));
    }
}
```

**Performance Considerations:**
- **Critical Indexes**:
  - `idx_jobs_org_facility_usecase_created_at` (covering index)
  - `idx_task_execution_user_mapping_user_state_task` (composite)
  - GIN index on `parameter_values.choices`
- **Query Optimization**:
  - Consider materialized view for frequently accessed combinations
  - Use partial indexes for common state combinations
  - Monitor DISTINCT operation performance
- **Caching Strategy**: Cache results per user with short TTL

**Testing Strategy:**
```java
@Test
void testGetMyJobs() {
    // Given - Create complex test scenario
    createMyJobsTestScenario();
    
    MyJobsSearchCriteria criteria = new MyJobsSearchCriteria()
        .setOrganisationId(1L)
        .setFacilityId(1L)
        .setUsecaseId(1L)
        .setUserId(100L)
        .setJobStates(List.of("IN_PROGRESS", "ASSIGNED"))
        .setTaskExecutionStates(List.of("IN_PROGRESS", "ASSIGNED"))
        .setLimit(10)
        .setOffset(0);
    
    // When
    List<UserJobSummary> myJobs = jobDAO.getMyJobs(criteria);
    
    // Then
    assertThat(myJobs).isNotEmpty();
    assertThat(myJobs).hasSize(3); // Based on test data
    
    // Verify ordering
    for (int i = 0; i < myJobs.size() - 1; i++) {
        assertThat(myJobs.get(i).getCreatedAt())
            .isGreaterThanOrEqualTo(myJobs.get(i + 1).getCreatedAt());
    }
}
```

---

### 10. countMyJob() - Count for getMyJobs

**Method Signature:**
```java
@Query(value = Queries.GET_MY_JOBS_COUNT, nativeQuery = true)
Long countMyJob(@Param("organisationId") Long organisationId, 
                @Param("facilityId") Long facilityId, 
                @Param("usecaseId") Long usecaseId, 
                @Param("jobStates") List<String> jobStates, 
                @Param("taskExecutionStates") List<String> taskExecutionStates, 
                @Param("userId") Long userId, 
                @Param("objectId") String objectId, 
                @Param("pom") boolean pom, 
                @Param("checklistAncestorId") Long checklistAncestorId, 
                @Param("name") String name, 
                @Param("code") String code);
```

**Input Parameters:**
- **Identical to getMyJobs()** except no limit/offset parameters
- Purpose: Count total results for pagination metadata

**Return Type:**
- `Long`: Total count of matching jobs
- Used for: Pagination total count, dashboard counters

**Native SQL Query:**
```sql
SELECT COUNT(distinct j.id)
FROM jobs j
         JOIN task_executions te ON te.jobs_id = j.id
         JOIN task_execution_user_mapping teum ON teum.task_executions_id = te.id
         JOIN checklists c ON j.checklists_id = c.id
WHERE j.organisations_id = :organisationId
  AND j.facilities_id = :facilityId
  AND j.use_cases_id = :usecaseId
  AND teum.users_id = :userId
  AND te.state IN :taskExecutionStates
  -- ... same filters as getMyJobs
  AND j.state IN :jobStates;
```

**Database Execution Plan:**
- **Same complexity as getMyJobs()** but optimized for COUNT
- **COUNT DISTINCT**: Expensive operation, consider alternatives
- **Performance**: Similar to getMyJobs but no result materialization

**DAO Conversion Strategy:**
```java
public Long countMyJobs(MyJobsSearchCriteria criteria) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT j.id)
        FROM jobs j
        INNER JOIN task_executions te ON te.jobs_id = j.id
        INNER JOIN task_execution_user_mapping teum ON teum.task_executions_id = te.id
        INNER JOIN checklists c ON j.checklists_id = c.id
        WHERE j.organisations_id = :organisationId
          AND j.facilities_id = :facilityId
          AND j.use_cases_id = :usecaseId
          AND teum.users_id = :userId
          AND te.state IN (:taskExecutionStates)
          AND j.state IN (:jobStates)
        """);
    
    Map<String, Object> params = new HashMap<>();
    // Same parameter mapping as getMyJobs
    buildMyJobsParams(params, criteria);
    
    // Add same optional filters as getMyJobs
    addMyJobsFilters(sql, params, criteria);
    
    return namedJdbcTemplate.queryForObject(sql.toString(), params, Long.class);
}
```

---

### 11. findJobsByIdInOrderBy() - Batch Retrieval with Ordering

**Method Signature:**
```java
@Query(value = "SELECT j.* FROM jobs j WHERE j.id IN :ids ORDER BY created_at DESC", nativeQuery = true)
List<Job> findJobsByIdInOrderBy(@Param("ids") Set<Long> ids);
```

**Input Parameters:**
- `ids` (Set<Long>): Set of job IDs to retrieve
  - Validation: Cannot be null or empty
  - Purpose: Batch retrieval of specific jobs
  - Common Use: After filtering operations that return ID lists

**Return Type:**
- `List<Job>`: Jobs ordered by creation time (newest first)
- **Empty List**: If no IDs match existing jobs
- **Partial Results**: Returns only jobs that exist, ignores non-existent IDs

**Native SQL Query:**
```sql
SELECT j.* FROM jobs j 
WHERE j.id IN (?, ?, ?, ...) 
ORDER BY created_at DESC;
```

**Database Execution Plan:**
- **Index Usage**: Primary key index `jobs_pkey` for IN clause
- **Performance**: O(k * log n) where k = number of IDs
- **IN Clause**: Efficient for small to medium ID sets
- **Ordering**: Additional sort operation after index lookups

**Entity Hydration Details:**
- **Full Entities**: Complete Job objects loaded
- **Lazy Associations**: All relationships remain as lazy proxies
- **Memory Impact**: Proportional to number of IDs requested

**Business Logic Context:**
- **Use Case**: Batch operations after ID filtering
- **Pattern**: Often follows complex queries that return ID lists
- **Ordering**: Consistent ordering for UI display

**DAO Conversion Strategy:**
```java
public List<Job> findJobsByIdInOrderBy(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) {
        return Collections.emptyList();
    }
    
    String sql = """
        SELECT j.id, j.code, j.state, j.checklists_id, j.facilities_id, j.organisations_id,
               j.use_cases_id, j.started_at, j.started_by, j.ended_at, j.ended_by,
               j.is_scheduled, j.schedulers_id, j.expected_start_date, j.expected_end_date,
               j.checklist_ancestor_id, j.created_at, j.modified_at, j.created_by, j.modified_by
        FROM jobs j 
        WHERE j.id IN (:ids) 
        ORDER BY j.created_at DESC
        """;
    
    Map<String, Object> params = Map.of("ids", ids);
    
    return namedJdbcTemplate.query(sql, params, new JobRowMapper());
}
```

**Performance Considerations:**
- **Batch Size**: Optimal for sets up to ~1000 IDs
- **Large Sets**: Consider chunking for very large ID sets
- **Index Usage**: Primary key lookups are very efficient

---

### 12. getChecklistIdByJobId() - Simple Projection

**Method Signature:**
```java
@Query(value = """
  SELECT j.checklists_id
  FROM jobs j
  WHERE j.id = :jobId
  """, nativeQuery = true)
Long getChecklistIdByJobId(@Param("jobId") Long jobId);
```

**Input Parameters:**
- `jobId` (Long): Job identifier
  - Validation: Cannot be null
  - Purpose: Retrieve associated checklist ID

**Return Type:**
- `Long`: Checklist ID for the job
- **Null**: If job doesn't exist

**Database Execution Plan:**
- **Index Usage**: Primary key lookup on jobs table
- **Performance**: O(1) - single row lookup
- **Very Efficient**: Minimal data transfer

**DAO Conversion Strategy:**
```java
public Long getChecklistIdByJobId(Long jobId) {
    String sql = "SELECT j.checklists_id FROM jobs j WHERE j.id = :jobId";
    Map<String, Object> params = Map.of("jobId", jobId);
    
    try {
        return namedJdbcTemplate.queryForObject(sql, params, Long.class);
    } catch (EmptyResultDataAccessException e) {
        return null;
    }
}
```

---

### 13. getStateByJobId() - State Projection

**Method Signature:**
```java
@Query(value = Queries.GET_JOB_STATE_BY_JOB_ID)
State.Job getStateByJobId(@Param("jobId") Long jobId);
```

**Return Type:**
- `State.Job`: Current job state enum
- **Null**: If job doesn't exist

**JPQL Query:**
```sql
SELECT j.state FROM Job j WHERE j.id = :jobId;
```

**DAO Conversion Strategy:**
```java
public State.Job getStateByJobId(Long jobId) {
    String sql = "SELECT j.state FROM jobs j WHERE j.id = :jobId";
    Map<String, Object> params = Map.of("jobId", jobId);
    
    try {
        String stateString = namedJdbcTemplate.queryForObject(sql, params, String.class);
        return State.Job.valueOf(stateString);
    } catch (EmptyResultDataAccessException e) {
        return null;
    }
}
```

---

### 14. findAllByIdIn() - Batch Retrieval

**Method Signature:**
```java
@Query(value = """
    SELECT * FROM jobs WHERE id IN :ids ORDER BY id DESC
  """, nativeQuery = true)
List<Job> findAllByIdIn(@Param("ids") Set<Long> ids);
```

**Similar to findJobsByIdInOrderBy()** but orders by ID instead of created_at.

---

### 15. getFacilityIdByJobId() - Facility Projection

**Method Signature:**
```java
@Query(value = """
  SELECT j.facilities_id
  FROM jobs j
  WHERE j.id = :jobId
  """, nativeQuery = true)
Long getFacilityIdByJobId(@Param("jobId") Long jobId);
```

**Purpose**: Retrieve facility ID for job - used for authorization and scoping.

---

### 16. findJobsForExcelDownload() - Complex Reporting Query

**Method Signature:**
```java
@Query(value = Queries.BULK_LOAD_JOBS_WITH_FILTERS_STATIC, nativeQuery = true)
List<JobExcelProjection> findJobsForExcelDownload(
    @Param("organisationId") Long organisationId,
    @Param("facilityId") Long facilityId,
    @Param("stateFilter") List<String> stateFilter,
    @Param("useCaseIdFilter") Long useCaseIdFilter,
    @Param("checklistAncestorIdFilter") Long checklistAncestorIdFilter,
    @Param("codeFilter") String codeFilter,
    @Param("checklistNameFilter") String checklistNameFilter,
    @Param("expectedEndDateLt") Long expectedEndDateLt,
    @Param("expectedStartDateGt") Long expectedStartDateGt,
    @Param("expectedStartDateLt") Long expectedStartDateLt,
    @Param("expectedStartDateIsNull") Boolean expectedStartDateIsNull,
    @Param("startedAtGte") Long startedAtGte,
    @Param("startedAtLte") Long startedAtLte,
    @Param("objectIdChoicesJson") String objectIdChoicesJson,
    @Param("createdById") Long createdById
);
```

**Complex Native SQL Query:**
```sql
WITH filtered_jobs AS (
    SELECT 
        j.id,
        j.code,
        j.state,
        j.created_at AS createdAt,           
        j.checklists_id AS checklistsId,    
        c.code AS checklistCode,             
        c.name AS checklistName,            
        u.first_name AS firstName,          
        u.last_name AS lastName,            
        u.employee_id AS employeeId
    FROM jobs j
    INNER JOIN checklists c ON j.checklists_id = c.id  
    INNER JOIN users u ON j.created_by = u.id
    WHERE j.organisations_id = :organisationId
      AND (:facilityId = -1 OR j.facilities_id = :facilityId)
      -- Multiple complex filter conditions
      AND (j.state IN :stateFilter)
      AND (:useCaseIdFilter IS NULL OR j.use_cases_id = :useCaseIdFilter)
      -- Date range filters
      AND (:expectedEndDateLt IS NULL OR j.expected_end_date < CAST(:expectedEndDateLt AS BIGINT))
      -- JSONB filtering for objectId
      AND (CAST(:objectIdChoicesJson AS VARCHAR) IS NULL OR j.id IN (
          SELECT pv.jobs_id 
          FROM parameter_values pv 
          INNER JOIN parameters p ON p.id = pv.parameters_id 
          WHERE p.type = 'RESOURCE'
            AND pv.choices @> CAST(:objectIdChoicesJson AS jsonb)
      ))
    ORDER BY j.id desc
)
SELECT * FROM filtered_jobs;
```

**Business Logic Context:**
- **Purpose**: Generate Excel reports with complex filtering
- **Performance Critical**: Large data exports require optimization
- **Complex Filters**: Supports extensive filtering options for reporting

**DAO Conversion Strategy:**
```java
public List<JobExcelData> findJobsForExcelDownload(JobExcelSearchCriteria criteria) {
    StringBuilder sql = new StringBuilder("""
        SELECT j.id, j.code, j.state, j.created_at,
               j.checklists_id, c.code as checklist_code, c.name as checklist_name,
               u.first_name, u.last_name, u.employee_id
        FROM jobs j
        INNER JOIN checklists c ON j.checklists_id = c.id
        INNER JOIN users u ON j.created_by = u.id
        WHERE j.organisations_id = :organisationId
        """);
    
    Map<String, Object> params = new HashMap<>();
    params.put("organisationId", criteria.getOrganisationId());
    
    // Add complex filtering logic
    buildExcelFilters(sql, params, criteria);
    
    sql.append(" ORDER BY j.id DESC");
    
    return namedJdbcTemplate.query(sql.toString(), params, new JobExcelDataRowMapper());
}
```

---

## Performance Summary

### Critical Indexes Required:
```sql
-- Core job indexes
CREATE INDEX idx_jobs_organisations_id_facilities_id ON jobs(organisations_id, facilities_id);
CREATE INDEX idx_jobs_state_created_at ON jobs(state, created_at);
CREATE INDEX idx_jobs_checklists_id ON jobs(checklists_id);
CREATE INDEX idx_jobs_schedulers_id_expected_start_date ON jobs(schedulers_id, expected_start_date);

-- Task execution indexes
CREATE INDEX idx_task_executions_jobs_id_state ON task_executions(jobs_id, state);
CREATE INDEX idx_task_execution_user_mapping_user_state ON task_execution_user_mapping(users_id, state);

-- Parameter value indexes
CREATE INDEX idx_parameter_values_jobs_id ON parameter_values(jobs_id);
CREATE INDEX idx_parameter_values_state_has_exceptions ON parameter_values(state, has_exceptions);
CREATE INDEX gin_parameter_values_choices ON parameter_values USING gin(choices);

-- Exception handling indexes
CREATE INDEX idx_exceptions_facilities_id_status ON exceptions(facilities_id, status);
```

### Query Performance Characteristics:
- **Simple lookups**: O(1) - Primary key operations
- **Filtered queries**: O(log n) - With proper indexes
- **Complex joins**: O(n * log n) - getMyJobs, getAllPendingForApprovalParameters
- **Bulk operations**: O(n) - updateJobToUnassignedIfNoUserAssigned

### Caching Strategy:
- **L1 Cache**: Entity-level caching in session
- **Query Cache**: Not recommended for job queries due to high mutation rate
- **Application Cache**: Cache projection results with short TTL
- **Redis**: Consider for frequently accessed job summaries

### Monitoring Requirements:
- Track execution time for complex queries (getMyJobs, getAllPendingForApprovalParameters)
- Monitor DISTINCT operation performance
- Alert on slow bulk update operations
- Track JSONB query performance

This comprehensive documentation provides the complete blueprint for replacing JobRepository with custom DAO implementations while maintaining all functionality and improving performance through optimized SQL and proper indexing strategies.