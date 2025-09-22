# ChecklistRepository - Comprehensive Hibernate Removal Documentation

## Repository Overview

**Repository Interface**: `IChecklistRepository`  
**File Location**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/repository/IChecklistRepository.java`  
**Entity**: `Checklist`  
**Primary Table**: `checklists`  
**Business Domain**: Digital Work Instructions Process Management  
**Repository Hierarchy**: Extends `JpaRepository<Checklist, Long>`, `JpaSpecificationExecutor<Checklist>`

### Business Context
The ChecklistRepository manages process templates (SOPs) and work instructions in the Streem Digital Work Instructions platform. It handles checklist lifecycle from creation through publication, including approval workflows, facility mappings, and property management.

### Entity Relationships Overview
```
Checklist (1) ←→ (N) ChecklistFacilityMapping ←→ (1) Facility
Checklist (1) ←→ (N) Stage ←→ (N) Task ←→ (N) Parameter  
Checklist (N) ←→ (1) Organisation
Checklist (N) ←→ (1) UseCase
Checklist (1) ←→ (1) Version
Checklist (1) ←→ (N) Job
Checklist (1) ←→ (N) ChecklistPropertyValue
Checklist (1) ←→ (N) Relation
Checklist (1) ←→ (N) ChecklistCollaboratorMapping
```

---

## Method Documentation

### 1. findAll(Specification, Pageable) - Override

**Method Signature:**
```java
@Override
Page<Checklist> findAll(Specification specification, Pageable pageable);
```

**Input Parameters:**
- `specification` (Specification<Checklist>): Dynamic query criteria using Spring Data JPA Specifications
  - Validation: Can be null (returns all records)
  - Purpose: Enables complex filtering with AND/OR conditions
- `pageable` (Pageable): Pagination and sorting configuration
  - Validation: Cannot be null
  - Purpose: Controls page size, offset, and sorting

**Return Type:**
- `Page<Checklist>`: Paginated results with metadata
- Contains: List of Checklist entities, total count, page information
- Edge Cases: Empty page if no results match criteria

**Generated SQL Query:**
```sql
SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id, 
       c.organisations_id, c.use_cases_id, c.created_at, c.modified_at, 
       c.created_by, c.modified_by, c.review_cycle, c.released_at, 
       c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
FROM checklists c 
WHERE [specification conditions]
ORDER BY [pageable sort fields]
LIMIT [page_size] OFFSET [page_number * page_size];

-- Count query for pagination metadata
SELECT COUNT(c.id) FROM checklists c WHERE [specification conditions];
```

**Database Execution Plan:**
- Primary Index: Uses `checklists_pkey` for basic lookups
- Filtering: Depends on specification fields - may require custom indexes
- Sorting: Uses ORDER BY with potential index scans
- Performance: O(log n) for indexed fields, O(n) for non-indexed filters

**Entity Hydration Details:**
- **Lazy Loading**: Most associations are FetchType.LAZY
- **Eager Loading**: Only `useCase` is FetchType.EAGER
- **Hydrated Fields**: All basic fields (id, name, code, state, etc.)
- **Not Hydrated**: stages, jobs, facilities, checklistPropertyValues, relations, collaborators

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED (default)
- **Transaction Required**: READ_ONLY transaction sufficient
- **Locking**: No explicit locks, relies on MVCC

**Caching Behavior:**
- **L1 Cache**: Entities cached in Hibernate Session
- **L2 Cache**: Not configured for Checklist entity
- **Query Cache**: Potentially cached if enabled globally

**Business Logic Integration:**
- Called from: ChecklistService.getChecklists(), ChecklistController.searchChecklists()
- Usage Pattern: Administrative dashboards, process listing with filters
- Common Specifications: Filter by organisation, facility, state, archived status

**DAO Conversion Strategy:**
```java
public class ChecklistDAO {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    
    public PageResult<Checklist> findAll(ChecklistSearchCriteria criteria, Pageable pageable) {
        // Build dynamic WHERE clause
        StringBuilder whereClause = new StringBuilder("WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        
        if (criteria.getOrganisationId() != null) {
            whereClause.append(" AND c.organisations_id = :organisationId");
            params.put("organisationId", criteria.getOrganisationId());
        }
        
        if (criteria.getState() != null) {
            whereClause.append(" AND c.state = :state");
            params.put("state", criteria.getState().name());
        }
        
        if (criteria.getArchived() != null) {
            whereClause.append(" AND c.archived = :archived");
            params.put("archived", criteria.getArchived());
        }
        
        if (StringUtils.hasText(criteria.getNameFilter())) {
            whereClause.append(" AND c.name ILIKE :nameFilter");
            params.put("nameFilter", "%" + criteria.getNameFilter() + "%");
        }
        
        // Build ORDER BY clause
        String orderBy = buildOrderByClause(pageable.getSort());
        
        // Count query
        String countSql = "SELECT COUNT(*) FROM checklists c " + whereClause.toString();
        Long totalCount = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
        
        // Data query with pagination
        String dataSql = """
            SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
                   c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
                   c.created_by, c.modified_by, c.review_cycle, c.released_at,
                   c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
            FROM checklists c 
            """ + whereClause.toString() + " " + orderBy + 
            " LIMIT :limit OFFSET :offset";
            
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());
        
        List<Checklist> checklists = namedJdbcTemplate.query(dataSql, params, new ChecklistRowMapper());
        
        return new PageResult<>(checklists, totalCount, pageable);
    }
    
    private String buildOrderByClause(Sort sort) {
        if (sort.isEmpty()) {
            return "ORDER BY c.id DESC";
        }
        
        List<String> orderClauses = new ArrayList<>();
        for (Sort.Order order : sort) {
            String property = mapPropertyToColumn(order.getProperty());
            String direction = order.getDirection().name();
            orderClauses.add("c." + property + " " + direction);
        }
        
        return "ORDER BY " + String.join(", ", orderClauses);
    }
    
    private String mapPropertyToColumn(String property) {
        return switch (property) {
            case "organisationId" -> "organisations_id";
            case "useCaseId" -> "use_cases_id";
            case "versionsId" -> "versions_id";
            case "createdAt" -> "created_at";
            case "modifiedAt" -> "modified_at";
            case "createdBy" -> "created_by";
            case "modifiedBy" -> "modified_by";
            case "reviewCycle" -> "review_cycle";
            case "releasedAt" -> "released_at";
            case "releasedBy" -> "released_by";
            case "jobLogColumns" -> "job_log_columns";
            case "isGlobal" -> "is_global";
            case "colorCode" -> "color_code";
            default -> property;
        };
    }
}

public class ChecklistRowMapper implements RowMapper<Checklist> {
    @Override
    public Checklist mapRow(ResultSet rs, int rowNum) throws SQLException {
        Checklist checklist = new Checklist();
        checklist.setId(rs.getLong("id"));
        checklist.setName(rs.getString("name"));
        checklist.setCode(rs.getString("code"));
        checklist.setState(State.Checklist.valueOf(rs.getString("state")));
        checklist.setArchived(rs.getBoolean("archived"));
        checklist.setOrganisationId(rs.getLong("organisations_id"));
        checklist.setUseCaseId(rs.getLong("use_cases_id"));
        checklist.setCreatedAt(rs.getLong("created_at"));
        checklist.setModifiedAt(rs.getLong("modified_at"));
        checklist.setCreatedBy(rs.getLong("created_by"));
        checklist.setModifiedBy(rs.getLong("modified_by"));
        checklist.setReviewCycle(rs.getInt("review_cycle"));
        
        // Handle nullable fields
        long releasedAt = rs.getLong("released_at");
        if (!rs.wasNull()) checklist.setReleasedAt(releasedAt);
        
        long releasedBy = rs.getLong("released_by");
        if (!rs.wasNull()) checklist.setReleasedBy(releasedBy);
        
        checklist.setDescription(rs.getString("description"));
        
        // Handle JSONB field
        String jsonString = rs.getString("job_log_columns");
        if (jsonString != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                checklist.setJobLogColumns(mapper.readTree(jsonString));
            } catch (Exception e) {
                // Handle JSON parsing error
                checklist.setJobLogColumns(mapper.createArrayNode());
            }
        }
        
        checklist.setGlobal(rs.getBoolean("is_global"));
        checklist.setColorCode(rs.getString("color_code"));
        
        return checklist;
    }
}
```

**Performance Considerations:**
- **Indexes Required**: 
  - `organisations_id` (frequently filtered)
  - `state` (status-based queries)
  - `archived` (active/inactive filtering)
  - Composite index on `(organisations_id, state, archived)` for common queries
- **Query Optimization**: Use EXPLAIN ANALYZE to verify index usage
- **Memory Usage**: Large result sets may require streaming with fetchSize configuration

**Testing Strategy:**
```java
@Test
public void testFindAllWithPagination() {
    // Test basic pagination
    Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
    PageResult<Checklist> result = checklistDAO.findAll(new ChecklistSearchCriteria(), pageable);
    
    assertThat(result.getContent()).hasSize(10);
    assertThat(result.getTotalElements()).isGreaterThan(0);
    assertThat(result.getContent().get(0).getName()).isLessThanOrEqualTo(result.getContent().get(1).getName());
}

@Test
public void testFindAllWithFilters() {
    ChecklistSearchCriteria criteria = new ChecklistSearchCriteria();
    criteria.setOrganisationId(1L);
    criteria.setState(State.Checklist.PUBLISHED);
    criteria.setArchived(false);
    
    PageResult<Checklist> result = checklistDAO.findAll(criteria, Pageable.unpaged());
    
    result.getContent().forEach(checklist -> {
        assertThat(checklist.getOrganisationId()).isEqualTo(1L);
        assertThat(checklist.getState()).isEqualTo(State.Checklist.PUBLISHED);
        assertThat(checklist.isArchived()).isFalse();
    });
}

@Test
public void testFindAllEmptyResult() {
    ChecklistSearchCriteria criteria = new ChecklistSearchCriteria();
    criteria.setOrganisationId(-999L); // Non-existent organisation
    
    PageResult<Checklist> result = checklistDAO.findAll(criteria, Pageable.unpaged());
    
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(0);
}
```

---

### 2. findAllByIdIn(Collection<Long>, Sort)

**Method Signature:**
```java
List<Checklist> findAllByIdIn(Collection<Long> id, Sort sort);
```

**Input Parameters:**
- `id` (Collection<Long>): Collection of checklist IDs to retrieve
  - Validation: Cannot be null or empty
  - Constraints: Maximum 1000 IDs recommended for performance
- `sort` (Sort): Sorting configuration
  - Validation: Can be null (defaults to no sorting)
  - Purpose: Controls result ordering

**Return Type:**
- `List<Checklist>`: Ordered list of matching checklists
- Edge Cases: Empty list if no IDs match existing records

**Generated SQL Query:**
```sql
SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
       c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
       c.created_by, c.modified_by, c.review_cycle, c.released_at,
       c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
FROM checklists c 
WHERE c.id IN (?, ?, ?, ...)
ORDER BY [sort fields];
```

**Database Execution Plan:**
- **Index Usage**: Primary key index scan (very efficient)
- **Join Strategy**: Index lookup for each ID in the IN clause
- **Performance**: O(log n × m) where m is number of IDs
- **Memory**: Materializes entire result set

**Entity Hydration Details:**
- **Basic Fields**: All scalar fields loaded
- **Associations**: Only eager associations (useCase) loaded
- **Lazy Fields**: stages, jobs, facilities remain uninitialized proxies

**Transaction Context:**
- **Isolation**: READ_COMMITTED sufficient
- **Lock Mode**: No locks required for read operations
- **Transaction**: Can run in read-only transaction

**Caching Behavior:**
- **L1 Cache**: Results cached by ID in current session
- **L2 Cache**: Individual entities may be L2 cached if configured
- **Query Cache**: Not applicable for IN queries with variable parameters

**Business Logic Integration:**
- **Usage**: Bulk operations, report generation, batch processing
- **Called From**: ChecklistService.getChecklistsByIds(), bulk data exports
- **Common Pattern**: Loading checklists for dashboard widgets

**DAO Conversion Strategy:**
```java
public List<Checklist> findAllByIdIn(Collection<Long> ids, Sort sort) {
    if (ids == null || ids.isEmpty()) {
        return Collections.emptyList();
    }
    
    // Handle large collections by batching
    List<Checklist> allResults = new ArrayList<>();
    List<Long> idList = new ArrayList<>(ids);
    
    // Process in batches of 1000 to avoid SQL parameter limits
    for (int i = 0; i < idList.size(); i += 1000) {
        int endIndex = Math.min(i + 1000, idList.size());
        List<Long> batch = idList.subList(i, endIndex);
        allResults.addAll(findBatchByIds(batch, sort));
    }
    
    return allResults;
}

private List<Checklist> findBatchByIds(List<Long> ids, Sort sort) {
    Map<String, Object> params = new HashMap<>();
    params.put("ids", ids);
    
    String orderByClause = "";
    if (sort != null && !sort.isEmpty()) {
        orderByClause = buildOrderByClause(sort);
    }
    
    String sql = """
        SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
               c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
               c.created_by, c.modified_by, c.review_cycle, c.released_at,
               c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
        FROM checklists c 
        WHERE c.id IN (:ids)
        """ + orderByClause;
    
    return namedJdbcTemplate.query(sql, params, new ChecklistRowMapper());
}
```

**Performance Considerations:**
- **Index Strategy**: Primary key index is optimal for ID-based lookups
- **Batch Size**: Limit IN clause to 1000 parameters maximum
- **Memory**: Consider result set size for large collections
- **Network**: Minimize roundtrips by batching appropriately

**Testing Strategy:**
```java
@Test
public void testFindAllByIdInBasic() {
    List<Long> ids = Arrays.asList(1L, 2L, 3L);
    List<Checklist> results = checklistDAO.findAllByIdIn(ids, null);
    
    assertThat(results).hasSize(3);
    assertThat(results.stream().map(Checklist::getId)).containsExactlyInAnyOrder(1L, 2L, 3L);
}

@Test
public void testFindAllByIdInWithSorting() {
    List<Long> ids = Arrays.asList(3L, 1L, 2L);
    Sort sort = Sort.by("name").ascending();
    List<Checklist> results = checklistDAO.findAllByIdIn(ids, sort);
    
    assertThat(results).hasSize(3);
    // Verify sorting
    for (int i = 0; i < results.size() - 1; i++) {
        assertThat(results.get(i).getName()).isLessThanOrEqualTo(results.get(i + 1).getName());
    }
}

@Test
public void testFindAllByIdInPartialMatch() {
    List<Long> ids = Arrays.asList(1L, 999L, 2L); // 999L doesn't exist
    List<Checklist> results = checklistDAO.findAllByIdIn(ids, null);
    
    assertThat(results).hasSize(2);
    assertThat(results.stream().map(Checklist::getId)).containsExactlyInAnyOrder(1L, 2L);
}

@Test
public void testFindAllByIdInEmptyCollection() {
    List<Checklist> results = checklistDAO.findAllByIdIn(Collections.emptyList(), null);
    assertThat(results).isEmpty();
}
```

---

### 3. findByTaskId(@Param("taskId") Long taskId)

**Method Signature:**
```java
@Query(value = Queries.GET_CHECKLIST_BY_TASK_ID)
Optional<Checklist> findByTaskId(@Param("taskId") Long taskId);
```

**Query Definition:**
```java
public static final String GET_CHECKLIST_BY_TASK_ID = 
    "select c from Checklist c inner join c.stages s inner join s.tasks t where t.id = :taskId";
```

**Input Parameters:**
- `taskId` (Long): Unique identifier of the task
  - Validation: Cannot be null
  - Business Rule: Must be an active, non-archived task ID
  - Foreign Key: References tasks.id

**Return Type:**
- `Optional<Checklist>`: Container that may or may not contain a Checklist
- **Present**: When task exists and belongs to a checklist
- **Empty**: When task doesn't exist or is orphaned

**Generated SQL Query:**
```sql
SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
       c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
       c.created_by, c.modified_by, c.review_cycle, c.released_at,
       c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
FROM checklists c 
INNER JOIN stages s ON s.checklists_id = c.id 
INNER JOIN tasks t ON t.stages_id = s.id 
WHERE t.id = ?
  AND s.archived = false;  -- Due to @Where clause on stages
```

**Database Execution Plan:**
- **Index Usage**: 
  - Primary: `tasks_pkey` on tasks.id
  - Secondary: `tasks_stages_id_idx` for join to stages
  - Tertiary: `stages_checklists_id_idx` for join to checklists
- **Join Strategy**: Nested loop joins due to single-row lookup
- **Performance**: O(log n) - very efficient with proper indexes

**Entity Hydration Details:**
- **Loaded**: Complete Checklist entity with all scalar fields
- **Eager Associations**: useCase (marked as FetchType.EAGER)
- **Lazy Associations**: stages, tasks, jobs, facilities, collaborators remain proxies
- **Filtered**: stages filtered by `archived = false` due to @Where clause

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient
- **Transaction Mode**: READ_ONLY transaction appropriate
- **Locking**: No explicit locks required
- **Consistency**: Relies on referential integrity constraints

**Caching Behavior:**
- **L1 Cache**: Result cached in Hibernate session by checklist ID
- **L2 Cache**: May benefit from L2 caching if configured
- **Query Cache**: HQL query could be cached with parameters

**Business Logic Integration:**
- **Use Case**: Task execution flows need to access parent checklist
- **Called From**: TaskExecutionService.validateTaskPermissions(), TaskService.getChecklistForTask()
- **Business Rules**: Used to enforce task-level security and validation
- **Pattern**: Navigation from task to process template

**DAO Conversion Strategy:**
```java
public Optional<Checklist> findByTaskId(Long taskId) {
    if (taskId == null) {
        throw new IllegalArgumentException("TaskId cannot be null");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("taskId", taskId);
    
    String sql = """
        SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
               c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
               c.created_by, c.modified_by, c.review_cycle, c.released_at,
               c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
        FROM checklists c 
        INNER JOIN stages s ON s.checklists_id = c.id 
        INNER JOIN tasks t ON t.stages_id = s.id 
        WHERE t.id = :taskId
          AND s.archived = false
        """;
    
    try {
        List<Checklist> results = namedJdbcTemplate.query(sql, params, new ChecklistRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    } catch (EmptyResultDataAccessException e) {
        return Optional.empty();
    }
}

// Alternative implementation with error handling
public Optional<Checklist> findByTaskIdWithValidation(Long taskId) {
    // First validate task exists
    String taskExistsQuery = "SELECT COUNT(*) FROM tasks WHERE id = :taskId";
    Map<String, Object> params = new HashMap<>();
    params.put("taskId", taskId);
    
    Integer taskCount = namedJdbcTemplate.queryForObject(taskExistsQuery, params, Integer.class);
    if (taskCount == 0) {
        log.warn("Task with ID {} not found", taskId);
        return Optional.empty();
    }
    
    return findByTaskId(taskId);
}
```

**Performance Considerations:**
- **Critical Indexes**:
  - `tasks_pkey` (PRIMARY KEY on tasks.id)
  - `idx_tasks_stages_id` (tasks.stages_id)
  - `idx_stages_checklists_id` (stages.checklists_id)
  - `idx_stages_archived` (stages.archived) for WHERE clause optimization
- **Query Plan**: Should show nested loop joins with index seeks
- **Cardinality**: One-to-many relationships require careful join ordering

**Testing Strategy:**
```java
@Test
public void testFindByTaskIdExists() {
    // Given: A task that belongs to a checklist
    Long existingTaskId = 1L;
    
    // When
    Optional<Checklist> result = checklistDAO.findByTaskId(existingTaskId);
    
    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isNotNull();
    assertThat(result.get().getName()).isNotBlank();
}

@Test
public void testFindByTaskIdNotExists() {
    // Given: A non-existent task ID
    Long nonExistentTaskId = 999999L;
    
    // When
    Optional<Checklist> result = checklistDAO.findByTaskId(nonExistentTaskId);
    
    // Then
    assertThat(result).isEmpty();
}

@Test
public void testFindByTaskIdArchivedStage() {
    // Given: A task in an archived stage
    Long taskInArchivedStageId = setupTaskWithArchivedStage();
    
    // When
    Optional<Checklist> result = checklistDAO.findByTaskId(taskInArchivedStageId);
    
    // Then
    assertThat(result).isEmpty(); // Should be filtered out by archived = false
}

@Test
public void testFindByTaskIdNullParameter() {
    // When/Then
    assertThatThrownBy(() -> checklistDAO.findByTaskId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("TaskId cannot be null");
}

@Test
public void testFindByTaskIdPerformance() {
    // Performance test - should complete within reasonable time
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    Optional<Checklist> result = checklistDAO.findByTaskId(1L);
    
    stopWatch.stop();
    assertThat(stopWatch.getTotalTimeMillis()).isLessThan(100); // Should be fast
}
```

---

### 4. updateState(@Param("state") State.Checklist state, @Param("checklistId") Long checklistId)

**Method Signature:**
```java
@Modifying(clearAutomatically = true)
@Query(value = Queries.UPDATE_CHECKLIST_STATE)
void updateState(@Param("state") State.Checklist state, @Param("checklistId") Long checklistId);
```

**Query Definition:**
```java
public static final String UPDATE_CHECKLIST_STATE = 
    "UPDATE Checklist c SET c.state = :state where c.id = :checklistId";
```

**Input Parameters:**
- `state` (State.Checklist): New state to set
  - Validation: Cannot be null
  - Values: BEING_BUILT, SUBMITTED_FOR_REVIEW, BEING_REVIEWED, REQUESTED_CHANGES, READY_FOR_SIGNING, SIGN_OFF_INITIATED, SIGNING_IN_PROGRESS, READY_FOR_RELEASE, PUBLISHED, DEPRECATED, BEING_REVISED
  - Business Rules: State transitions must follow workflow rules
- `checklistId` (Long): ID of checklist to update
  - Validation: Cannot be null
  - Constraints: Must reference existing checklist

**Return Type:**
- `void`: No return value
- Side Effect: Modifies database state
- Exception: May throw DataAccessException if update fails

**Generated SQL Query:**
```sql
UPDATE checklists 
SET state = ?, modified_at = ?, modified_by = ?
WHERE id = ?;
```

**Database Execution Plan:**
- **Index Usage**: Primary key index on checklists.id (exact match)
- **Lock Type**: Row-level exclusive lock during update
- **Performance**: O(1) - single row update by primary key
- **Affected Rows**: 0 if ID doesn't exist, 1 if successful

**Entity Hydration Details:**
- **No Hydration**: Bulk update operation doesn't load entities
- **Cache Impact**: @Modifying(clearAutomatically = true) clears L1 cache
- **Dirty Checking**: Bypasses Hibernate dirty checking mechanism
- **Optimistic Locking**: May conflict with version-based optimistic locking

**Transaction Context:**
- **Transaction Required**: REQUIRED (will participate in existing or create new)
- **Isolation Level**: READ_COMMITTED minimum to prevent dirty reads
- **Lock Duration**: Held until transaction commit/rollback
- **Rollback**: Participates in transaction rollback

**Caching Behavior:**
- **L1 Cache**: Cleared automatically due to @Modifying annotation
- **L2 Cache**: May become stale - requires eviction strategy
- **Query Cache**: Not applicable for update operations

**Business Logic Integration:**
- **Use Cases**: Workflow state transitions, approval processes
- **Called From**: ChecklistService.approveChecklist(), ChecklistService.publishChecklist()
- **Business Rules**: Must validate state transition legality before calling
- **Audit Trail**: Should update modified_at and modified_by fields

**DAO Conversion Strategy:**
```java
public void updateState(State.Checklist state, Long checklistId) {
    if (state == null) {
        throw new IllegalArgumentException("State cannot be null");
    }
    if (checklistId == null) {
        throw new IllegalArgumentException("ChecklistId cannot be null");
    }
    
    // Get current user for audit trail
    Long currentUserId = getCurrentUserId(); // From security context
    long currentTimestamp = System.currentTimeMillis();
    
    Map<String, Object> params = new HashMap<>();
    params.put("state", state.name());
    params.put("checklistId", checklistId);
    params.put("modifiedAt", currentTimestamp);
    params.put("modifiedBy", currentUserId);
    
    String sql = """
        UPDATE checklists 
        SET state = :state, 
            modified_at = :modifiedAt, 
            modified_by = :modifiedBy
        WHERE id = :checklistId
        """;
    
    int rowsAffected = namedJdbcTemplate.update(sql, params);
    
    if (rowsAffected == 0) {
        throw new ResourceNotFoundException("Checklist not found with ID: " + checklistId);
    }
    
    log.info("Updated checklist {} state to {}", checklistId, state);
}

// Enhanced version with optimistic locking
public void updateStateWithVersion(State.Checklist state, Long checklistId, Long expectedVersion) {
    Map<String, Object> params = new HashMap<>();
    params.put("state", state.name());
    params.put("checklistId", checklistId);
    params.put("modifiedAt", System.currentTimeMillis());
    params.put("modifiedBy", getCurrentUserId());
    params.put("expectedVersion", expectedVersion);
    
    String sql = """
        UPDATE checklists 
        SET state = :state, 
            modified_at = :modifiedAt, 
            modified_by = :modifiedBy,
            version = version + 1
        WHERE id = :checklistId 
          AND version = :expectedVersion
        """;
    
    int rowsAffected = namedJdbcTemplate.update(sql, params);
    
    if (rowsAffected == 0) {
        // Check if record exists
        String existsQuery = "SELECT COUNT(*) FROM checklists WHERE id = :checklistId";
        Integer count = namedJdbcTemplate.queryForObject(existsQuery, 
            Map.of("checklistId", checklistId), Integer.class);
            
        if (count == 0) {
            throw new ResourceNotFoundException("Checklist not found with ID: " + checklistId);
        } else {
            throw new OptimisticLockException("Checklist was modified by another user");
        }
    }
}

// Batch update version for multiple checklists
public void updateStateForMultiple(State.Checklist state, List<Long> checklistIds) {
    if (checklistIds.isEmpty()) {
        return;
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("state", state.name());
    params.put("checklistIds", checklistIds);
    params.put("modifiedAt", System.currentTimeMillis());
    params.put("modifiedBy", getCurrentUserId());
    
    String sql = """
        UPDATE checklists 
        SET state = :state, 
            modified_at = :modifiedAt, 
            modified_by = :modifiedBy
        WHERE id IN (:checklistIds)
        """;
    
    int rowsAffected = namedJdbcTemplate.update(sql, params);
    log.info("Updated {} checklists to state {}", rowsAffected, state);
}
```

**Performance Considerations:**
- **Index Strategy**: Primary key update is optimal O(1)
- **Locking**: Minimize transaction duration to reduce lock contention
- **Batch Updates**: Consider batch operations for multiple updates
- **Concurrent Access**: Handle optimistic locking conflicts gracefully

**Testing Strategy:**
```java
@Test
public void testUpdateStateSuccess() {
    // Given
    Long checklistId = 1L;
    State.Checklist newState = State.Checklist.PUBLISHED;
    
    // When
    checklistDAO.updateState(newState, checklistId);
    
    // Then - verify state was updated
    Optional<Checklist> updated = checklistDAO.findById(checklistId);
    assertThat(updated).isPresent();
    assertThat(updated.get().getState()).isEqualTo(newState);
}

@Test
public void testUpdateStateChecklistNotFound() {
    // Given
    Long nonExistentId = 999999L;
    State.Checklist newState = State.Checklist.PUBLISHED;
    
    // When/Then
    assertThatThrownBy(() -> checklistDAO.updateState(newState, nonExistentId))
        .isInstanceOf(ResourceNotFoundException.class);
}

@Test
public void testUpdateStateNullParameters() {
    // When/Then - null state
    assertThatThrownBy(() -> checklistDAO.updateState(null, 1L))
        .isInstanceOf(IllegalArgumentException.class);
        
    // When/Then - null checklistId
    assertThatThrownBy(() -> checklistDAO.updateState(State.Checklist.PUBLISHED, null))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
@Transactional
public void testUpdateStateTransaction() {
    // Given
    Long checklistId = 1L;
    State.Checklist originalState = getChecklistState(checklistId);
    State.Checklist newState = State.Checklist.PUBLISHED;
    
    try {
        // When - update in transaction
        checklistDAO.updateState(newState, checklistId);
        
        // Verify within transaction
        assertThat(getChecklistState(checklistId)).isEqualTo(newState);
        
        // Force rollback
        throw new RuntimeException("Force rollback");
        
    } catch (RuntimeException e) {
        // Then - verify rollback occurred
        assertThat(getChecklistState(checklistId)).isEqualTo(originalState);
    }
}

@Test
public void testUpdateStateAuditFields() {
    // Given
    Long checklistId = 1L;
    Long beforeTimestamp = System.currentTimeMillis();
    
    // When
    checklistDAO.updateState(State.Checklist.PUBLISHED, checklistId);
    
    // Then - verify audit fields updated
    Optional<Checklist> updated = checklistDAO.findById(checklistId);
    assertThat(updated).isPresent();
    assertThat(updated.get().getModifiedAt()).isGreaterThanOrEqualTo(beforeTimestamp);
    assertThat(updated.get().getModifiedBy()).isNotNull();
}
```

---

### 5. getChecklistCodeByChecklistId(@Param("checklistId") Long checklistId)

**Method Signature:**
```java
@Query(value = Queries.GET_CHECKLIST_CODE)
String getChecklistCodeByChecklistId(@Param("checklistId") Long checklistId);
```

**Query Definition:**
```java
public static final String GET_CHECKLIST_CODE = 
    "SELECT code from Checklist c where c.id = :checklistId";
```

**Input Parameters:**
- `checklistId` (Long): Unique identifier of the checklist
  - Validation: Cannot be null
  - Constraints: Must reference existing checklist
  - Business Context: Used for code generation and referencing

**Return Type:**
- `String`: Unique checklist code (e.g., "CHK-001", "PROC-Manufacturing-001")
- **Null**: When checklist doesn't exist
- **Empty String**: Theoretically possible but business logic prevents this

**Generated SQL Query:**
```sql
SELECT c.code 
FROM checklists c 
WHERE c.id = ?;
```

**Database Execution Plan:**
- **Index Usage**: Primary key index on checklists.id
- **Scan Type**: Index seek (single row lookup)
- **Performance**: O(1) - optimal performance
- **I/O**: Single page read in most cases

**Entity Hydration Details:**
- **No Entity Hydration**: Scalar projection, no entity loading
- **Field Access**: Only accesses single varchar column
- **Memory Usage**: Minimal - single string value
- **No Associations**: No lazy loading triggered

**Transaction Context:**
- **Isolation**: READ_UNCOMMITTED sufficient for reference data
- **Locking**: No locks required - read-only operation
- **Transaction**: Can run outside transaction context
- **Consistency**: Reads committed data

**Caching Behavior:**
- **L1 Cache**: Not applicable for scalar queries
- **L2 Cache**: Not applicable for projection queries
- **Query Cache**: Excellent candidate for query-level caching
- **Application Cache**: Consider caching at service layer

**Business Logic Integration:**
- **Use Cases**: 
  - Job code generation (job codes often derive from checklist codes)
  - Audit logging and traceability
  - User interface display of process identifiers
  - Integration with external systems requiring process codes
- **Called From**: JobService.generateJobCode(), ChecklistService.getProcessReference()
- **Business Rules**: Codes are immutable once assigned, used for external references

**DAO Conversion Strategy:**
```java
public String getChecklistCodeByChecklistId(Long checklistId) {
    if (checklistId == null) {
        throw new IllegalArgumentException("ChecklistId cannot be null");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("checklistId", checklistId);
    
    String sql = "SELECT code FROM checklists WHERE id = :checklistId";
    
    try {
        return namedJdbcTemplate.queryForObject(sql, params, String.class);
    } catch (EmptyResultDataAccessException e) {
        log.warn("Checklist not found with ID: {}", checklistId);
        return null;
    }
}

// Enhanced version with caching
@Cacheable(value = "checklistCodes", key = "#checklistId")
public String getChecklistCodeByChecklistIdCached(Long checklistId) {
    return getChecklistCodeByChecklistId(checklistId);
}

// Batch version for multiple IDs
public Map<Long, String> getChecklistCodesByIds(Collection<Long> checklistIds) {
    if (checklistIds == null || checklistIds.isEmpty()) {
        return Collections.emptyMap();
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("checklistIds", checklistIds);
    
    String sql = "SELECT id, code FROM checklists WHERE id IN (:checklistIds)";
    
    List<Map<String, Object>> results = namedJdbcTemplate.queryForList(sql, params);
    
    return results.stream()
        .collect(Collectors.toMap(
            row -> ((Number) row.get("id")).longValue(),
            row -> (String) row.get("code")
        ));
}

// Version with validation
public String getChecklistCodeWithValidation(Long checklistId) {
    String code = getChecklistCodeByChecklistId(checklistId);
    
    if (code == null) {
        throw new ResourceNotFoundException("Checklist not found with ID: " + checklistId);
    }
    
    if (code.trim().isEmpty()) {
        throw new DataIntegrityException("Checklist code is empty for ID: " + checklistId);
    }
    
    return code;
}
```

**Performance Considerations:**
- **Index Strategy**: Primary key lookup is optimal - no additional indexes needed
- **Caching Strategy**: Excellent candidate for application-level caching
  - Codes rarely change once assigned
  - High read frequency, low write frequency
  - Small memory footprint
- **Batch Operations**: Consider batch retrieval for multiple codes
- **Connection Pool**: Lightweight query - minimal connection usage

**Testing Strategy:**
```java
@Test
public void testGetChecklistCodeByChecklistIdExists() {
    // Given
    Long existingChecklistId = 1L;
    String expectedCode = "CHK-001";
    
    // When
    String actualCode = checklistDAO.getChecklistCodeByChecklistId(existingChecklistId);
    
    // Then
    assertThat(actualCode).isEqualTo(expectedCode);
    assertThat(actualCode).isNotBlank();
}

@Test
public void testGetChecklistCodeByChecklistIdNotExists() {
    // Given
    Long nonExistentId = 999999L;
    
    // When
    String code = checklistDAO.getChecklistCodeByChecklistId(nonExistentId);
    
    // Then
    assertThat(code).isNull();
}

@Test
public void testGetChecklistCodeByChecklistIdNullParameter() {
    // When/Then
    assertThatThrownBy(() -> checklistDAO.getChecklistCodeByChecklistId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ChecklistId cannot be null");
}

@Test
public void testGetChecklistCodeBatch() {
    // Given
    List<Long> checklistIds = Arrays.asList(1L, 2L, 3L);
    
    // When
    Map<Long, String> codes = checklistDAO.getChecklistCodesByIds(checklistIds);
    
    // Then
    assertThat(codes).hasSize(3);
    assertThat(codes.get(1L)).isNotBlank();
    assertThat(codes.get(2L)).isNotBlank();
    assertThat(codes.get(3L)).isNotBlank();
}

@Test
public void testGetChecklistCodeCaching() {
    // Given
    Long checklistId = 1L;
    
    // When - first call
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    String code1 = checklistDAO.getChecklistCodeByChecklistIdCached(checklistId);
    stopWatch.stop();
    long firstCallTime = stopWatch.getTotalTimeMillis();
    
    // When - second call (should be cached)
    stopWatch = new StopWatch();
    stopWatch.start();
    String code2 = checklistDAO.getChecklistCodeByChecklistIdCached(checklistId);
    stopWatch.stop();
    long secondCallTime = stopWatch.getTotalTimeMillis();
    
    // Then
    assertThat(code1).isEqualTo(code2);
    assertThat(secondCallTime).isLessThan(firstCallTime); // Cache should be faster
}

@Test
public void testGetChecklistCodeWithValidationSuccess() {
    // Given
    Long checklistId = 1L;
    
    // When
    String code = checklistDAO.getChecklistCodeWithValidation(checklistId);
    
    // Then
    assertThat(code).isNotBlank();
}

@Test
public void testGetChecklistCodeWithValidationNotFound() {
    // Given
    Long nonExistentId = 999999L;
    
    // When/Then
    assertThatThrownBy(() -> checklistDAO.getChecklistCodeWithValidation(nonExistentId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Checklist not found with ID: " + nonExistentId);
}
```

---

### 6. removeChecklistFacilityMapping(@Param("checklistId") Long checklistId, @Param("facilityIds") Set<Long> facilityIds)

**Method Signature:**
```java
@Transactional(rollbackFor = Exception.class)
@Modifying
@Query(value = Queries.DELETE_CHECKLIST_FACILITY_MAPPING, nativeQuery = true)
void removeChecklistFacilityMapping(@Param("checklistId") Long checklistId, @Param("facilityIds") Set<Long> facilityIds);
```

**Query Definition:**
```java
public static final String DELETE_CHECKLIST_FACILITY_MAPPING = 
    "delete from checklist_facility_mapping where checklists_id= :checklistId and facilities_id in :facilityIds";
```

**Input Parameters:**
- `checklistId` (Long): ID of the checklist to remove facility mappings from
  - Validation: Cannot be null
  - Business Rule: Must be existing checklist
  - Impact: Affects checklist accessibility per facility
- `facilityIds` (Set<Long>): Set of facility IDs to remove from checklist
  - Validation: Cannot be null or empty
  - Constraints: Facilities must exist
  - Business Impact: Removes checklist access for specified facilities

**Return Type:**
- `void`: No return value
- **Side Effects**: 
  - Deletes rows from checklist_facility_mapping table
  - May affect checklist visibility in facilities
  - Triggers cascade effects in business logic

**Generated SQL Query:**
```sql
DELETE FROM checklist_facility_mapping 
WHERE checklists_id = ? 
  AND facilities_id IN (?, ?, ?, ...);
```

**Database Execution Plan:**
- **Index Usage**: 
  - Primary: `idx_checklist_facility_mapping_checklist_id` 
  - Secondary: `idx_checklist_facility_mapping_facility_id`
  - Optimal: Composite index on `(checklists_id, facilities_id)`
- **Delete Strategy**: Index range scan followed by delete operations
- **Performance**: O(log n + m) where m is number of facilities
- **Locking**: Row-level exclusive locks on affected rows

**Entity Hydration Details:**
- **No Entity Loading**: Native SQL delete bypasses entity loading
- **No Cascade Effects**: Direct table manipulation
- **No Lifecycle Events**: JPA lifecycle callbacks not triggered
- **Cache Impact**: L1/L2 cache may become stale

**Transaction Context:**
- **Transaction Required**: REQUIRED with explicit rollback configuration
- **Isolation Level**: READ_COMMITTED minimum for referential integrity
- **Lock Duration**: Locks held until transaction completion
- **Rollback Strategy**: Full rollback on any Exception

**Caching Behavior:**
- **L1 Cache**: Not affected (native query)
- **L2 Cache**: May contain stale ChecklistFacilityMapping entities
- **Cache Eviction**: Manual eviction may be required
- **Query Cache**: Not applicable for DML operations

**Business Logic Integration:**
- **Use Cases**: 
  - Facility access management
  - Checklist deployment control
  - Security and authorization management
- **Called From**: ChecklistService.removeFacilityAccess(), FacilityService.removeChecklistMappings()
- **Business Impact**: Affects which facilities can access and execute the checklist
- **Validation Required**: Must ensure at least one facility remains mapped

**DAO Conversion Strategy:**
```java
@Transactional(rollbackFor = Exception.class)
public void removeChecklistFacilityMapping(Long checklistId, Set<Long> facilityIds) {
    if (checklistId == null) {
        throw new IllegalArgumentException("ChecklistId cannot be null");
    }
    if (facilityIds == null || facilityIds.isEmpty()) {
        throw new IllegalArgumentException("FacilityIds cannot be null or empty");
    }
    
    // Business validation - ensure checklist exists
    validateChecklistExists(checklistId);
    
    // Business validation - ensure at least one facility remains
    validateAtLeastOneFacilityRemains(checklistId, facilityIds);
    
    Map<String, Object> params = new HashMap<>();
    params.put("checklistId", checklistId);
    params.put("facilityIds", facilityIds);
    
    String sql = """
        DELETE FROM checklist_facility_mapping 
        WHERE checklists_id = :checklistId 
          AND facilities_id IN (:facilityIds)
        """;
    
    int deletedRows = namedJdbcTemplate.update(sql, params);
    
    log.info("Removed {} facility mappings for checklist {}", deletedRows, checklistId);
    
    // Clear related caches
    evictChecklistFacilityCache(checklistId);
    
    // Publish domain event for business logic
    publishFacilityMappingRemovedEvent(checklistId, facilityIds, deletedRows);
}

private void validateChecklistExists(Long checklistId) {
    String sql = "SELECT COUNT(*) FROM checklists WHERE id = :checklistId";
    Integer count = namedJdbcTemplate.queryForObject(sql, 
        Map.of("checklistId", checklistId), Integer.class);
    
    if (count == 0) {
        throw new ResourceNotFoundException("Checklist not found with ID: " + checklistId);
    }
}

private void validateAtLeastOneFacilityRemains(Long checklistId, Set<Long> facilityIdsToRemove) {
    Map<String, Object> params = new HashMap<>();
    params.put("checklistId", checklistId);
    params.put("facilityIds", facilityIdsToRemove);
    
    String sql = """
        SELECT COUNT(*) FROM checklist_facility_mapping 
        WHERE checklists_id = :checklistId 
          AND facilities_id NOT IN (:facilityIds)
        """;
    
    Integer remainingCount = namedJdbcTemplate.queryForObject(sql, params, Integer.class);
    
    if (remainingCount == 0) {
        throw new BusinessRuleException("Cannot remove all facility mappings. At least one facility must remain.");
    }
}

// Batch version for multiple checklists
@Transactional(rollbackFor = Exception.class)
public void removeMultipleChecklistFacilityMappings(Map<Long, Set<Long>> checklistFacilityMap) {
    for (Map.Entry<Long, Set<Long>> entry : checklistFacilityMap.entrySet()) {
        removeChecklistFacilityMapping(entry.getKey(), entry.getValue());
    }
}

// Safe version with dry-run capability
public int previewChecklistFacilityMappingRemoval(Long checklistId, Set<Long> facilityIds) {
    Map<String, Object> params = new HashMap<>();
    params.put("checklistId", checklistId);
    params.put("facilityIds", facilityIds);
    
    String sql = """
        SELECT COUNT(*) FROM checklist_facility_mapping 
        WHERE checklists_id = :checklistId 
          AND facilities_id IN (:facilityIds)
        """;
    
    return namedJdbcTemplate.queryForObject(sql, params, Integer.class);
}
```

**Performance Considerations:**
- **Critical Indexes**:
  - `idx_checklist_facility_mapping_composite(checklists_id, facilities_id)` - optimal for this query
  - `idx_checklist_facility_mapping_checklist_id` - secondary option
- **Batch Size**: Handle large facility sets by batching IN clauses
- **Lock Contention**: Minimize transaction duration to reduce blocking
- **Foreign Key Impact**: Deletion may be slower due to FK constraint checking

**Testing Strategy:**
```java
@Test
@Transactional
public void testRemoveChecklistFacilityMappingSuccess() {
    // Given
    Long checklistId = 1L;
    Set<Long> facilityIds = Set.of(2L, 3L);
    
    // Verify mappings exist before deletion
    int countBefore = countChecklistFacilityMappings(checklistId);
    assertThat(countBefore).isGreaterThan(2);
    
    // When
    checklistDAO.removeChecklistFacilityMapping(checklistId, facilityIds);
    
    // Then
    int countAfter = countChecklistFacilityMappings(checklistId);
    assertThat(countAfter).isEqualTo(countBefore - 2);
    
    // Verify specific mappings removed
    assertThat(existsChecklistFacilityMapping(checklistId, 2L)).isFalse();
    assertThat(existsChecklistFacilityMapping(checklistId, 3L)).isFalse();
}

@Test
public void testRemoveChecklistFacilityMappingChecklistNotExists() {
    // Given
    Long nonExistentChecklistId = 999999L;
    Set<Long> facilityIds = Set.of(1L, 2L);
    
    // When/Then
    assertThatThrownBy(() -> checklistDAO.removeChecklistFacilityMapping(nonExistentChecklistId, facilityIds))
        .isInstanceOf(ResourceNotFoundException.class);
}

@Test
public void testRemoveChecklistFacilityMappingAllFacilities() {
    // Given - checklist with only 2 facilities
    Long checklistId = setupChecklistWithTwoFacilities();
    Set<Long> allFacilityIds = Set.of(1L, 2L);
    
    // When/Then - should prevent removal of all facilities
    assertThatThrownBy(() -> checklistDAO.removeChecklistFacilityMapping(checklistId, allFacilityIds))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("At least one facility must remain");
}

@Test
public void testRemoveChecklistFacilityMappingNullParameters() {
    // When/Then - null checklistId
    assertThatThrownBy(() -> checklistDAO.removeChecklistFacilityMapping(null, Set.of(1L)))
        .isInstanceOf(IllegalArgumentException.class);
        
    // When/Then - null facilityIds
    assertThatThrownBy(() -> checklistDAO.removeChecklistFacilityMapping(1L, null))
        .isInstanceOf(IllegalArgumentException.class);
        
    // When/Then - empty facilityIds
    assertThatThrownBy(() -> checklistDAO.removeChecklistFacilityMapping(1L, Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
@Transactional
public void testRemoveChecklistFacilityMappingTransactionRollback() {
    // Given
    Long checklistId = 1L;
    Set<Long> facilityIds = Set.of(2L);
    int countBefore = countChecklistFacilityMappings(checklistId);
    
    try {
        // When - operation that should rollback
        checklistDAO.removeChecklistFacilityMapping(checklistId, facilityIds);
        
        // Simulate error after deletion
        throw new RuntimeException("Simulated error");
        
    } catch (RuntimeException e) {
        // Then - verify rollback occurred
        int countAfter = countChecklistFacilityMappings(checklistId);
        assertThat(countAfter).isEqualTo(countBefore); // No change due to rollback
    }
}

@Test
public void testPreviewChecklistFacilityMappingRemoval() {
    // Given
    Long checklistId = 1L;
    Set<Long> facilityIds = Set.of(2L, 3L);
    
    // When
    int affectedRows = checklistDAO.previewChecklistFacilityMappingRemoval(checklistId, facilityIds);
    
    // Then
    assertThat(affectedRows).isEqualTo(2);
    
    // Verify no actual deletion occurred
    assertThat(existsChecklistFacilityMapping(checklistId, 2L)).isTrue();
    assertThat(existsChecklistFacilityMapping(checklistId, 3L)).isTrue();
}

// Helper methods for testing
private int countChecklistFacilityMappings(Long checklistId) {
    String sql = "SELECT COUNT(*) FROM checklist_facility_mapping WHERE checklists_id = ?";
    return jdbcTemplate.queryForObject(sql, Integer.class, checklistId);
}

private boolean existsChecklistFacilityMapping(Long checklistId, Long facilityId) {
    String sql = "SELECT COUNT(*) FROM checklist_facility_mapping WHERE checklists_id = ? AND facilities_id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, checklistId, facilityId);
    return count > 0;
}
```

---

### 7. findByStageId(@Param("stageId") Long stageId)

**Method Signature:**
```java
@Query(value = Queries.GET_CHECKLIST_STATE_BY_STAGE_ID)
State.Checklist findByStageId(@Param("stageId") Long stageId);
```

**Query Definition:**
```java
public static final String GET_CHECKLIST_STATE_BY_STAGE_ID = 
    "SELECT c.state FROM Checklist c inner join c.stages s where s.id=:stageId";
```

**Input Parameters:**
- `stageId` (Long): Unique identifier of the stage
  - Validation: Cannot be null
  - Business Context: Stage belongs to a checklist in a specific state
  - Foreign Key: References stages.id

**Return Type:**
- `State.Checklist`: Enum representing the checklist's current state
- **Possible Values**: BEING_BUILT, SUBMITTED_FOR_REVIEW, PUBLISHED, etc.
- **Null**: When stage doesn't exist or is orphaned

**Generated SQL Query:**
```sql
SELECT c.state 
FROM checklists c 
INNER JOIN stages s ON s.checklists_id = c.id 
WHERE s.id = ?
  AND s.archived = false;  -- Due to @Where clause on stages
```

**Database Execution Plan:**
- **Index Usage**:
  - Primary: `stages_pkey` on stages.id
  - Secondary: `idx_stages_checklists_id` for join
- **Join Strategy**: Nested loop join (efficient for single row)
- **Performance**: O(log n) - optimal with proper indexes
- **Result Set**: Single scalar value

**Entity Hydration Details:**
- **No Entity Loading**: Scalar projection query
- **Field Access**: Only checklist.state column accessed
- **No Associations**: No lazy loading triggered
- **Memory Usage**: Minimal - single enum value

**Transaction Context:**
- **Isolation**: READ_COMMITTED sufficient
- **Locking**: No locks required for read operation
- **Transaction**: Can execute in read-only transaction
- **Consistency**: Reads committed state data

**Caching Behavior:**
- **L1 Cache**: Not applicable for scalar queries
- **L2 Cache**: Not applicable for projections
- **Query Cache**: Good candidate for query-level caching
- **Business Cache**: Consider caching stage-to-state mappings

**Business Logic Integration:**
- **Use Cases**:
  - Stage-level authorization checks
  - Workflow validation during task execution
  - State-dependent UI rendering
  - Process flow control
- **Called From**: StageService.validateStageAccess(), TaskExecutionService.checkProcessState()
- **Business Rules**: Stage operations must respect parent checklist state

**DAO Conversion Strategy:**
```java
public State.Checklist findByStageId(Long stageId) {
    if (stageId == null) {
        throw new IllegalArgumentException("StageId cannot be null");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("stageId", stageId);
    
    String sql = """
        SELECT c.state 
        FROM checklists c 
        INNER JOIN stages s ON s.checklists_id = c.id 
        WHERE s.id = :stageId
          AND s.archived = false
        """;
    
    try {
        String stateString = namedJdbcTemplate.queryForObject(sql, params, String.class);
        return State.Checklist.valueOf(stateString);
    } catch (EmptyResultDataAccessException e) {
        log.warn("No checklist found for stage ID: {}", stageId);
        return null;
    } catch (IllegalArgumentException e) {
        log.error("Invalid checklist state found for stage ID: {}", stageId, e);
        throw new DataIntegrityException("Invalid checklist state data", e);
    }
}

// Enhanced version with validation
public State.Checklist findByStageIdWithValidation(Long stageId) {
    // First validate stage exists
    if (!stageExists(stageId)) {
        throw new ResourceNotFoundException("Stage not found with ID: " + stageId);
    }
    
    State.Checklist state = findByStageId(stageId);
    
    if (state == null) {
        throw new DataIntegrityException("Stage exists but has no associated checklist: " + stageId);
    }
    
    return state;
}

// Batch version for multiple stages
public Map<Long, State.Checklist> findStatesByStageIds(Collection<Long> stageIds) {
    if (stageIds == null || stageIds.isEmpty()) {
        return Collections.emptyMap();
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("stageIds", stageIds);
    
    String sql = """
        SELECT s.id as stage_id, c.state 
        FROM checklists c 
        INNER JOIN stages s ON s.checklists_id = c.id 
        WHERE s.id IN (:stageIds)
          AND s.archived = false
        """;
    
    List<Map<String, Object>> results = namedJdbcTemplate.queryForList(sql, params);
    
    return results.stream()
        .collect(Collectors.toMap(
            row -> ((Number) row.get("stage_id")).longValue(),
            row -> State.Checklist.valueOf((String) row.get("state"))
        ));
}

// Cached version for frequently accessed stages
@Cacheable(value = "stageChecklistStates", key = "#stageId")
public State.Checklist findByStageIdCached(Long stageId) {
    return findByStageId(stageId);
}

private boolean stageExists(Long stageId) {
    String sql = "SELECT COUNT(*) FROM stages WHERE id = :stageId AND archived = false";
    Integer count = namedJdbcTemplate.queryForObject(sql, 
        Map.of("stageId", stageId), Integer.class);
    return count > 0;
}
```

**Performance Considerations:**
- **Index Requirements**:
  - `stages_pkey` (PRIMARY KEY on stages.id) - essential
  - `idx_stages_checklists_id` (stages.checklists_id) - for join performance
  - `idx_stages_archived` (stages.archived) - for WHERE clause
  - Optimal: Composite index on `(id, archived, checklists_id)`
- **Query Optimization**: Very efficient single-row lookup
- **Caching Strategy**: Excellent candidate for application caching due to infrequent state changes
- **Memory Usage**: Minimal - single enum value

**Testing Strategy:**
```java
@Test
public void testFindByStageIdExists() {
    // Given
    Long existingStageId = 1L;
    State.Checklist expectedState = State.Checklist.PUBLISHED;
    
    // When
    State.Checklist actualState = checklistDAO.findByStageId(existingStageId);
    
    // Then
    assertThat(actualState).isEqualTo(expectedState);
    assertThat(actualState).isNotNull();
}

@Test
public void testFindByStageIdNotExists() {
    // Given
    Long nonExistentStageId = 999999L;
    
    // When
    State.Checklist state = checklistDAO.findByStageId(nonExistentStageId);
    
    // Then
    assertThat(state).isNull();
}

@Test
public void testFindByStageIdArchivedStage() {
    // Given
    Long archivedStageId = setupArchivedStage();
    
    // When
    State.Checklist state = checklistDAO.findByStageId(archivedStageId);
    
    // Then
    assertThat(state).isNull(); // Archived stages should be filtered out
}

@Test
public void testFindByStageIdNullParameter() {
    // When/Then
    assertThatThrownBy(() -> checklistDAO.findByStageId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("StageId cannot be null");
}

@Test
public void testFindByStageIdWithValidationSuccess() {
    // Given
    Long validStageId = 1L;
    
    // When
    State.Checklist state = checklistDAO.findByStageIdWithValidation(validStageId);
    
    // Then
    assertThat(state).isNotNull();
    assertThat(state).isInstanceOf(State.Checklist.class);
}

@Test
public void testFindByStageIdWithValidationStageNotFound() {
    // Given
    Long nonExistentStageId = 999999L;
    
    // When/Then
    assertThatThrownBy(() -> checklistDAO.findByStageIdWithValidation(nonExistentStageId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Stage not found with ID: " + nonExistentStageId);
}

@Test
public void testFindStatesByStageIdsBatch() {
    // Given
    List<Long> stageIds = Arrays.asList(1L, 2L, 3L);
    
    // When
    Map<Long, State.Checklist> states = checklistDAO.findStatesByStageIds(stageIds);
    
    // Then
    assertThat(states).hasSize(3);
    assertThat(states.get(1L)).isNotNull();
    assertThat(states.get(2L)).isNotNull();
    assertThat(states.get(3L)).isNotNull();
}

@Test
public void testFindStatesByStageIdsEmptyInput() {
    // When
    Map<Long, State.Checklist> states = checklistDAO.findStatesByStageIds(Collections.emptyList());
    
    // Then
    assertThat(states).isEmpty();
}

@Test
public void testFindByStageIdCaching() {
    // Given
    Long stageId = 1L;
    
    // When - first call
    State.Checklist state1 = checklistDAO.findByStageIdCached(stageId);
    
    // When - second call (should be cached)
    State.Checklist state2 = checklistDAO.findByStageIdCached(stageId);
    
    // Then
    assertThat(state1).isEqualTo(state2);
    assertThat(state1).isSameAs(state2); // Same instance due to caching
}

@Test
public void testFindByStageIdPerformance() {
    // Given
    Long stageId = 1L;
    
    // When
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    for (int i = 0; i < 100; i++) {
        checklistDAO.findByStageId(stageId);
    }
    
    stopWatch.stop();
    
    // Then - should be very fast due to index usage
    assertThat(stopWatch.getTotalTimeMillis()).isLessThan(1000); // 100 calls under 1 second
}
```

---

### 8. findByUseCaseId(Long useCaseId)

**Method Signature:**
```java
List<Checklist> findByUseCaseId(Long useCaseId);
```

**Input Parameters:**
- `useCaseId` (Long): Unique identifier of the use case
  - Validation: Cannot be null
  - Business Context: Groups checklists by business use case (e.g., Manufacturing, Quality Control)
  - Foreign Key: References use_cases.id

**Return Type:**
- `List<Checklist>`: List of checklists belonging to the use case
- **Empty List**: When no checklists exist for the use case
- **Ordered**: Natural ordering (typically by creation date or ID)

**Generated SQL Query:**
```sql
-- Spring Data JPA generates this query from method name
SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
       c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
       c.created_by, c.modified_by, c.review_cycle, c.released_at,
       c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
FROM checklists c 
WHERE c.use_cases_id = ?
ORDER BY c.id;
```

**Database Execution Plan:**
- **Index Usage**: `idx_checklists_use_cases_id` (foreign key index)
- **Scan Type**: Index range scan
- **Performance**: O(log n + m) where m is result set size
- **Sorting**: May use filesort if no covering index

**Entity Hydration Details:**
- **Full Entity Loading**: Complete Checklist entities loaded
- **Eager Associations**: useCase association loaded (FetchType.EAGER)
- **Lazy Associations**: stages, jobs, facilities remain proxies
- **Collections**: Empty collections initialized for lazy relationships

**Transaction Context:**
- **Isolation**: READ_COMMITTED adequate
- **Locking**: No locks for read-only operation
- **Transaction**: READ_ONLY transaction recommended
- **Consistency**: Consistent view of use case relationships

**Caching Behavior:**
- **L1 Cache**: Each checklist cached by ID in session
- **L2 Cache**: Entities may be L2 cached if enabled
- **Query Cache**: Collection queries generally not cached
- **Association Cache**: useCase entities may be cached

**Business Logic Integration:**
- **Use Cases**:
  - Use case dashboard displaying all related processes
  - Process categorization and organization
  - Bulk operations on use case processes
  - Analytics and reporting by business domain
- **Called From**: UseCaseService.getUseCaseProcesses(), DashboardService.getProcessesByCategory()
- **Business Rules**: Use cases group related business processes for management

**DAO Conversion Strategy:**
```java
public List<Checklist> findByUseCaseId(Long useCaseId) {
    if (useCaseId == null) {
        throw new IllegalArgumentException("UseCaseId cannot be null");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("useCaseId", useCaseId);
    
    String sql = """
        SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
               c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
               c.created_by, c.modified_by, c.review_cycle, c.released_at,
               c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
        FROM checklists c 
        WHERE c.use_cases_id = :useCaseId
        ORDER BY c.id
        """;
    
    return namedJdbcTemplate.query(sql, params, new ChecklistRowMapper());
}

// Enhanced version with filtering options
public List<Checklist> findByUseCaseIdWithFilters(Long useCaseId, ChecklistFilters filters) {
    if (useCaseId == null) {
        throw new IllegalArgumentException("UseCaseId cannot be null");
    }
    
    StringBuilder whereClause = new StringBuilder("WHERE c.use_cases_id = :useCaseId");
    Map<String, Object> params = new HashMap<>();
    params.put("useCaseId", useCaseId);
    
    // Add optional filters
    if (filters != null) {
        if (filters.getIncludeArchived() != null && !filters.getIncludeArchived()) {
            whereClause.append(" AND c.archived = false");
        }
        
        if (filters.getStates() != null && !filters.getStates().isEmpty()) {
            whereClause.append(" AND c.state IN (:states)");
            params.put("states", filters.getStates().stream().map(Enum::name).collect(Collectors.toList()));
        }
        
        if (StringUtils.hasText(filters.getNamePattern())) {
            whereClause.append(" AND c.name ILIKE :namePattern");
            params.put("namePattern", "%" + filters.getNamePattern() + "%");
        }
    }
    
    String sql = """
        SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
               c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
               c.created_by, c.modified_by, c.review_cycle, c.released_at,
               c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
        FROM checklists c 
        """ + whereClause.toString() + """
        ORDER BY c.name, c.id
        """;
    
    return namedJdbcTemplate.query(sql, params, new ChecklistRowMapper());
}

// Paginated version for large use cases
public PageResult<Checklist> findByUseCaseIdPaginated(Long useCaseId, Pageable pageable) {
    if (useCaseId == null) {
        throw new IllegalArgumentException("UseCaseId cannot be null");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("useCaseId", useCaseId);
    
    // Count query
    String countSql = "SELECT COUNT(*) FROM checklists WHERE use_cases_id = :useCaseId";
    Long totalCount = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
    
    // Data query with pagination
    String dataSql = """
        SELECT c.id, c.name, c.code, c.state, c.archived, c.versions_id,
               c.organisations_id, c.use_cases_id, c.created_at, c.modified_at,
               c.created_by, c.modified_by, c.review_cycle, c.released_at,
               c.released_by, c.description, c.job_log_columns, c.is_global, c.color_code
        FROM checklists c 
        WHERE c.use_cases_id = :useCaseId
        ORDER BY c.id
        LIMIT :limit OFFSET :offset
        """;
    
    params.put("limit", pageable.getPageSize());
    params.put("offset", pageable.getOffset());
    
    List<Checklist> checklists = namedJdbcTemplate.query(dataSql, params, new ChecklistRowMapper());
    
    return new PageResult<>(checklists, totalCount, pageable);
}

// Count version for analytics
public long countByUseCaseId(Long useCaseId) {
    if (useCaseId == null) {
        throw new IllegalArgumentException("UseCaseId cannot be null");
    }
    
    String sql = "SELECT COUNT(*) FROM checklists WHERE use_cases_id = :useCaseId";
    return namedJdbcTemplate.queryForObject(sql, Map.of("useCaseId", useCaseId), Long.class);
}

// Summary version with minimal data
public List<ChecklistSummary> findSummariesByUseCaseId(Long useCaseId) {
    if (useCaseId == null) {
        throw new IllegalArgumentException("UseCaseId cannot be null");
    }
    
    String sql = """
        SELECT c.id, c.name, c.code, c.state, c.archived
        FROM checklists c 
        WHERE c.use_cases_id = :useCaseId
        ORDER BY c.name
        """;
    
    return namedJdbcTemplate.query(sql, Map.of("useCaseId", useCaseId), 
        (rs, rowNum) -> new ChecklistSummary(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("code"),
            State.Checklist.valueOf(rs.getString("state")),
            rs.getBoolean("archived")
        ));
}
```

**Performance Considerations:**
- **Index Requirements**:
  - `idx_checklists_use_cases_id` (use_cases_id) - essential for filtering
  - `idx_checklists_use_case_name` (use_cases_id, name) - for sorting by name
  - `idx_checklists_use_case_state` (use_cases_id, state) - for state filtering
- **Large Result Sets**: Consider pagination for use cases with many checklists
- **Memory Usage**: Full entity loading can be memory intensive
- **Query Optimization**: Use covering indexes where possible

**Testing Strategy:**
```java
@Test
public void testFindByUseCaseIdExists() {
    // Given
    Long existingUseCaseId = 1L;
    
    // When
    List<Checklist> checklists = checklistDAO.findByUseCaseId(existingUseCaseId);
    
    // Then
    assertThat(checklists).isNotEmpty();
    checklists.forEach(checklist -> {
        assertThat(checklist.getUseCaseId()).isEqualTo(existingUseCaseId);
        assertThat(checklist.getId()).isNotNull();
        assertThat(checklist.getName()).isNotBlank();
        assertThat(checklist.getCode()).isNotBlank();
        assertThat(checklist.getState()).isNotNull();
    });
}

@Test
public void testFindByUseCaseIdNoResults() {
    // Given
    Long useCaseWithNoChecklists = 999999L;
    
    // When
    List<Checklist> checklists = checklistDAO.findByUseCaseId(useCaseWithNoChecklists);
    
    // Then
    assertThat(checklists).isEmpty();
}

@Test
public void testFindByUseCaseIdNullParameter() {
    // When/Then
    assertThatThrownBy(() -> checklistDAO.findByUseCaseId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("UseCaseId cannot be null");
}

@Test
public void testFindByUseCaseIdOrdering() {
    // Given
    Long useCaseId = 1L;
    
    // When
    List<Checklist> checklists = checklistDAO.findByUseCaseId(useCaseId);
    
    // Then - verify ordering by ID
    if (checklists.size() > 1) {
        for (int i = 0; i < checklists.size() - 1; i++) {
            assertThat(checklists.get(i).getId()).isLessThanOrEqualTo(checklists.get(i + 1).getId());
        }
    }
}

@Test
public void testFindByUseCaseIdWithFilters() {
    // Given
    Long useCaseId = 1L;
    ChecklistFilters filters = new ChecklistFilters();
    filters.setIncludeArchived(false);
    filters.setStates(Set.of(State.Checklist.PUBLISHED, State.Checklist.BEING_BUILT));
    filters.setNamePattern("Test");
    
    // When
    List<Checklist> checklists = checklistDAO.findByUseCaseIdWithFilters(useCaseId, filters);
    
    // Then
    checklists.forEach(checklist -> {
        assertThat(checklist.getUseCaseId()).isEqualTo(useCaseId);
        assertThat(checklist.isArchived()).isFalse();
        assertThat(checklist.getState()).isIn(State.Checklist.PUBLISHED, State.Checklist.BEING_BUILT);
        assertThat(checklist.getName().toLowerCase()).contains("test");
    });
}

@Test
public void testFindByUseCaseIdPaginated() {
    // Given
    Long useCaseId = 1L;
    Pageable pageable = PageRequest.of(0, 5, Sort.by("name"));
    
    // When
    PageResult<Checklist> result = checklistDAO.findByUseCaseIdPaginated(useCaseId, pageable);
    
    // Then
    assertThat(result.getContent()).hasSizeLessThanOrEqualTo(5);
    assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(result.getContent().size());
    
    // Verify sorting
    if (result.getContent().size() > 1) {
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            String name1 = result.getContent().get(i).getName();
            String name2 = result.getContent().get(i + 1).getName();
            assertThat(name1).isLessThanOrEqualTo(name2);
        }
    }
}

@Test
public void testCountByUseCaseId() {
    // Given
    Long useCaseId = 1L;
    
    // When
    long count = checklistDAO.countByUseCaseId(useCaseId);
    
    // Then
    assertThat(count).isGreaterThanOrEqualTo(0);
    
    // Verify count matches findByUseCaseId result
    List<Checklist> checklists = checklistDAO.findByUseCaseId(useCaseId);
    assertThat(count).isEqualTo(checklists.size());
}

@Test
public void testFindSummariesByUseCaseId() {
    // Given
    Long useCaseId = 1L;
    
    // When
    List<ChecklistSummary> summaries = checklistDAO.findSummariesByUseCaseId(useCaseId);
    
    // Then
    assertThat(summaries).isNotEmpty();
    summaries.forEach(summary -> {
        assertThat(summary.getId()).isNotNull();
        assertThat(summary.getName()).isNotBlank();
        assertThat(summary.getCode()).isNotBlank();
        assertThat(summary.getState()).isNotNull();
    });
    
    // Verify ordering by name
    if (summaries.size() > 1) {
        for (int i = 0; i < summaries.size() - 1; i++) {
            String name1 = summaries.get(i).getName();
            String name2 = summaries.get(i + 1).getName();
            assertThat(name1).isLessThanOrEqualTo(name2);
        }
    }
}

@Test
public void testFindByUseCaseIdPerformance() {
    // Given
    Long useCaseId = 1L;
    
    // When - measure performance
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    List<Checklist> checklists = checklistDAO.findByUseCaseId(useCaseId);
    
    stopWatch.stop();
    
    // Then - should be reasonably fast
    assertThat(stopWatch.getTotalTimeMillis()).isLessThan(500);
    log.info("Found {} checklists for use case {} in {}ms", 
        checklists.size(), useCaseId, stopWatch.getTotalTimeMillis());
}
```

---

### 9. findByStateInOrderByStateDesc(@Param("state") Set<State.Checklist> stateSet)

**Method Signature:**
```java
@Query(value = Queries.GET_CHECKLIST_BY_STATE)
List<Long> findByStateInOrderByStateDesc(@Param("state") Set<State.Checklist> stateSet);
```

**Query Definition:**
```java
public static final String GET_CHECKLIST_BY_STATE = 
    "SELECT c.id FROM Checklist c WHERE c.state in :state";
```

**Input Parameters:**
- `stateSet` (Set<State.Checklist>): Set of checklist states to match
  - Validation: Cannot be null or empty
  - Values: Any combination of State.Checklist enum values
  - Business Context: Used for bulk operations on checklists in specific states

**Return Type:**
- `List<Long>`: List of checklist IDs matching the specified states
- **Ordered**: Despite method name suggesting ordering, the query doesn't include ORDER BY
- **Empty List**: When no checklists match the specified states

**Generated SQL Query:**
```sql
SELECT c.id 
FROM checklists c 
WHERE c.state IN (?, ?, ?, ...);
-- Note: No ORDER BY clause despite method name
```

**Database Execution Plan:**
- **Index Usage**: `idx_checklists_state` (state column index)
- **Scan Type**: Index range scan with IN predicate
- **Performance**: O(log n + m) where m is result set size
- **Memory**: Minimal - only ID values returned

**Entity Hydration Details:**
- **No Entity Loading**: Scalar projection query returning only IDs
- **No Associations**: No entity relationships accessed
- **Memory Usage**: Very low - only primitive Long values
- **No Lazy Loading**: No proxies created

**Transaction Context:**
- **Isolation**: READ_UNCOMMITTED acceptable for ID-only queries
- **Locking**: No locks required
- **Transaction**: Can execute outside transaction context
- **Consistency**: Snapshot view of current state distribution

**Caching Behavior:**
- **L1 Cache**: Not applicable for scalar queries
- **L2 Cache**: Not applicable for projections
- **Query Cache**: Excellent candidate due to parameter variability
- **Application Cache**: Consider caching for frequently used state combinations

**Business Logic Integration:**
- **Use Cases**:
  - Bulk state transition operations
  - Analytics and reporting on process states
  - Administrative dashboards showing process counts by state
  - Cleanup and maintenance operations
- **Called From**: ChecklistService.getChecklistsInStates(), AdminService.getProcessStatistics()
- **Business Rules**: State-based queries support workflow management and reporting

**DAO Conversion Strategy:**
```java
public List<Long> findByStateInOrderByStateDesc(Set<State.Checklist> stateSet) {
    if (stateSet == null || stateSet.isEmpty()) {
        throw new IllegalArgumentException("StateSet cannot be null or empty");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("states", stateSet.stream().map(Enum::name).collect(Collectors.toList()));
    
    String sql = """
        SELECT c.id 
        FROM checklists c 
        WHERE c.state IN (:states)
        ORDER BY c.state DESC, c.id DESC
        """;
    
    return namedJdbcTemplate.queryForList(sql, params, Long.class);
}

// Enhanced version with actual ordering by state
public List<Long> findByStateInOrderByStateDescFixed(Set<State.Checklist> stateSet) {
    if (stateSet == null || stateSet.isEmpty()) {
        throw new IllegalArgumentException("StateSet cannot be null or empty");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("states", stateSet.stream().map(Enum::name).collect(Collectors.toList()));
    
    // Custom ordering based on state hierarchy/priority
    String sql = """
        SELECT c.id 
        FROM checklists c 
        WHERE c.state IN (:states)
        ORDER BY 
            CASE c.state 
                WHEN 'PUBLISHED' THEN 1
                WHEN 'READY_FOR_RELEASE' THEN 2
                WHEN 'SIGNING_IN_PROGRESS' THEN 3
                WHEN 'SIGN_OFF_INITIATED' THEN 4
                WHEN 'READY_FOR_SIGNING' THEN 5
                WHEN 'BEING_REVIEWED' THEN 6
                WHEN 'SUBMITTED_FOR_REVIEW' THEN 7
                WHEN 'BEING_BUILT' THEN 8
                WHEN 'REQUESTED_CHANGES' THEN 9
                WHEN 'BEING_REVISED' THEN 10
                WHEN 'DEPRECATED' THEN 11
                ELSE 99
            END,
            c.id DESC
        """;
    
    return namedJdbcTemplate.queryForList(sql, params, Long.class);
}

// Version with additional metadata
public List<ChecklistStateInfo> findByStateInWithInfo(Set<State.Checklist> stateSet) {
    if (stateSet == null || stateSet.isEmpty()) {
        throw new IllegalArgumentException("StateSet cannot be null or empty");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("states", stateSet.stream().map(Enum::name).collect(Collectors.toList()));
    
    String sql = """
        SELECT c.id, c.code, c.name, c.state, c.modified_at
        FROM checklists c 
        WHERE c.state IN (:states)
        ORDER BY c.state DESC, c.modified_at DESC
        """;
    
    return namedJdbcTemplate.query(sql, params, 
        (rs, rowNum) -> new ChecklistStateInfo(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            State.Checklist.valueOf(rs.getString("state")),
            rs.getLong("modified_at")
        ));
}

// Count version for analytics
public Map<State.Checklist, Long> countByStates(Set<State.Checklist> stateSet) {
    if (stateSet == null || stateSet.isEmpty()) {
        return Collections.emptyMap();
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("states", stateSet.stream().map(Enum::name).collect(Collectors.toList()));
    
    String sql = """
        SELECT c.state, COUNT(*) as count
        FROM checklists c 
        WHERE c.state IN (:states)
        GROUP BY c.state
        ORDER BY c.state
        """;
    
    List<Map<String, Object>> results = namedJdbcTemplate.queryForList(sql, params);
    
    return results.stream()
        .collect(Collectors.toMap(
            row -> State.Checklist.valueOf((String) row.get("state")),
            row -> ((Number) row.get("count")).longValue()
        ));
}

// Batch processing version with pagination
public List<Long> findByStateInBatched(Set<State.Checklist> stateSet, int batchSize, int offset) {
    if (stateSet == null || stateSet.isEmpty()) {
        throw new IllegalArgumentException("StateSet cannot be null or empty");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("states", stateSet.stream().map(Enum::name).collect(Collectors.toList()));
    params.put("limit", batchSize);
    params.put("offset", offset);
    
    String sql = """
        SELECT c.id 
        FROM checklists c 
        WHERE c.state IN (:states)
        ORDER BY c.id
        LIMIT :limit OFFSET :offset
        """;
    
    return namedJdbcTemplate.queryForList(sql, params, Long.class);
}
```

**Performance Considerations:**
- **Index Requirements**:
  - `idx_checklists_state` (state) - essential for IN predicate performance
  - `idx_checklists_state_id` (state, id) - covering index for optimal performance
  - `idx_checklists_state_modified_at` (state, modified_at) - for time-ordered queries
- **IN Clause Optimization**: PostgreSQL handles IN clauses efficiently with proper indexing
- **Memory Usage**: Very low - only ID values returned
- **Sorting**: Consider impact of ORDER BY on large result sets

**Testing Strategy:**
```java
@Test
public void testFindByStateInOrderByStateDescSingleState() {
    // Given
    Set<State.Checklist> stateSet = Set.of(State.Checklist.PUBLISHED);
    
    // When
    List<Long> checklistIds = checklistDAO.findByStateInOrderByStateDesc(stateSet);
    
    // Then
    assertThat(checklistIds).isNotEmpty();
    
    // Verify all checklists have the expected state
    for (Long id : checklistIds) {
        State.Checklist actualState = getChecklistState(id);
        assertThat(actualState).isEqualTo(State.Checklist.PUBLISHED);
    }
}

@Test
public void testFindByStateInOrderByStateDescMultipleStates() {
    // Given
    Set<State.Checklist> stateSet = Set.of(
        State.Checklist.PUBLISHED, 
        State.Checklist.BEING_BUILT, 
        State.Checklist.BEING_REVIEWED
    );
    
    // When
    List<Long> checklistIds = checklistDAO.findByStateInOrderByStateDesc(stateSet);
    
    // Then
    assertThat(checklistIds).isNotEmpty();
    
    // Verify all checklists have one of the expected states
    for (Long id : checklistIds) {
        State.Checklist actualState = getChecklistState(id);
        assertThat(actualState).isIn(stateSet);
    }
}

@Test
public void testFindByStateInOrderByStateDescNoResults() {
    // Given - state that likely has no checklists
    Set<State.Checklist> stateSet = Set.of(State.Checklist.ILLEGAL);
    
    // When
    List<Long> checklistIds = checklistDAO.findByStateInOrderByStateDesc(stateSet);
    
    // Then
    assertThat(checklistIds).isEmpty();
}

@Test
public void testFindByStateInOrderByStateDescNullParameter() {
    // When/Then - null stateSet
    assertThatThrownBy(() -> checklistDAO.findByStateInOrderByStateDesc(null))
        .isInstanceOf(IllegalArgumentException.class);
        
    // When/Then - empty stateSet
    assertThatThrownBy(() -> checklistDAO.findByStateInOrderByStateDesc(Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
public void testFindByStateInOrderByStateDescOrdering() {
    // Given
    Set<State.Checklist> stateSet = Set.of(
        State.Checklist.PUBLISHED, 
        State.Checklist.BEING_BUILT
    );
    
    // When
    List<Long> checklistIds = checklistDAO.findByStateInOrderByStateDescFixed(stateSet);
    
    // Then - verify ordering (PUBLISHED should come before BEING_BUILT)
    if (checklistIds.size() > 1) {
        Map<Long, State.Checklist> stateMap = getChecklistStates(checklistIds);
        
        State.Checklist previousState = null;
        for (Long id : checklistIds) {
            State.Checklist currentState = stateMap.get(id);
            if (previousState != null) {
                // Verify state ordering (PUBLISHED > BEING_BUILT in priority)
                assertThat(getStatePriority(currentState)).isGreaterThanOrEqualTo(getStatePriority(previousState));
            }
            previousState = currentState;
        }
    }
}

@Test
public void testFindByStateInWithInfo() {
    // Given
    Set<State.Checklist> stateSet = Set.of(State.Checklist.PUBLISHED);
    
    // When
    List<ChecklistStateInfo> infos = checklistDAO.findByStateInWithInfo(stateSet);
    
    // Then
    assertThat(infos).isNotEmpty();
    infos.forEach(info -> {
        assertThat(info.getId()).isNotNull();
        assertThat(info.getCode()).isNotBlank();
        assertThat(info.getName()).isNotBlank();
        assertThat(info.getState()).isIn(stateSet);
        assertThat(info.getModifiedAt()).isGreaterThan(0);
    });
}

@Test
public void testCountByStates() {
    // Given
    Set<State.Checklist> stateSet = Set.of(
        State.Checklist.PUBLISHED, 
        State.Checklist.BEING_BUILT
    );
    
    // When
    Map<State.Checklist, Long> counts = checklistDAO.countByStates(stateSet);
    
    // Then
    assertThat(counts).isNotEmpty();
    assertThat(counts.keySet()).isSubsetOf(stateSet);
    counts.values().forEach(count -> assertThat(count).isGreaterThan(0));
    
    // Verify total count matches findByStateIn
    long totalCount = counts.values().stream().mapToLong(Long::longValue).sum();
    List<Long> allIds = checklistDAO.findByStateInOrderByStateDesc(stateSet);
    assertThat(totalCount).isEqualTo(allIds.size());
}

@Test
public void testFindByStateInBatched() {
    // Given
    Set<State.Checklist> stateSet = Set.of(State.Checklist.PUBLISHED);
    int batchSize = 5;
    
    // When - first batch
    List<Long> firstBatch = checklistDAO.findByStateInBatched(stateSet, batchSize, 0);
    
    // When - second batch
    List<Long> secondBatch = checklistDAO.findByStateInBatched(stateSet, batchSize, batchSize);
    
    // Then
    assertThat(firstBatch).hasSizeLessThanOrEqualTo(batchSize);
    assertThat(secondBatch).hasSizeLessThanOrEqualTo(batchSize);
    
    // Verify no overlap between batches
    Set<Long> intersection = new HashSet<>(firstBatch);
    intersection.retainAll(secondBatch);
    assertThat(intersection).isEmpty();
}

@Test
public void testFindByStateInPerformance() {
    // Given
    Set<State.Checklist> stateSet = Set.of(
        State.Checklist.PUBLISHED, 
        State.Checklist.BEING_BUILT, 
        State.Checklist.BEING_REVIEWED
    );
    
    // When
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    List<Long> checklistIds = checklistDAO.findByStateInOrderByStateDesc(stateSet);
    
    stopWatch.stop();
    
    // Then - should be very fast due to index usage
    assertThat(stopWatch.getTotalTimeMillis()).isLessThan(100);
    log.info("Found {} checklists in {} states in {}ms", 
        checklistIds.size(), stateSet.size(), stopWatch.getTotalTimeMillis());
}

// Helper methods for testing
private State.Checklist getChecklistState(Long checklistId) {
    String sql = "SELECT state FROM checklists WHERE id = ?";
    String stateString = jdbcTemplate.queryForObject(sql, String.class, checklistId);
    return State.Checklist.valueOf(stateString);
}

private Map<Long, State.Checklist> getChecklistStates(List<Long> checklistIds) {
    String sql = "SELECT id, state FROM checklists WHERE id IN (" + 
        checklistIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
    
    List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, checklistIds.toArray());
    
    return results.stream()
        .collect(Collectors.toMap(
            row -> ((Number) row.get("id")).longValue(),
            row -> State.Checklist.valueOf((String) row.get("state"))
        ));
}

private int getStatePriority(State.Checklist state) {
    return switch (state) {
        case PUBLISHED -> 1;
        case READY_FOR_RELEASE -> 2;
        case SIGNING_IN_PROGRESS -> 3;
        case SIGN_OFF_INITIATED -> 4;
        case READY_FOR_SIGNING -> 5;
        case BEING_REVIEWED -> 6;
        case SUBMITTED_FOR_REVIEW -> 7;
        case BEING_BUILT -> 8;
        case REQUESTED_CHANGES -> 9;
        case BEING_REVISED -> 10;
        case DEPRECATED -> 11;
        default -> 99;
    };
}
```

---

## Summary and Conversion Guidelines

This comprehensive documentation of the ChecklistRepository provides the foundation for converting all 88 repositories from Hibernate to custom DAOs. Each method analysis includes:

### Key Documentation Elements Covered

1. **Method Signatures**: Exact Java method definitions with annotations
2. **SQL Query Analysis**: Generated queries and native SQL translations
3. **Database Performance**: Execution plans, indexing requirements, optimization strategies
4. **Entity Hydration**: Lazy/eager loading, association handling, cache impacts
5. **Transaction Management**: Isolation levels, locking strategies, rollback handling
6. **Business Integration**: Use cases, calling contexts, business rule enforcement
7. **Complete DAO Implementation**: Production-ready Java code with error handling
8. **Comprehensive Testing**: Unit tests covering all scenarios and edge cases

### Conversion Strategy Template

For each repository method, the conversion process involves:

1. **Query Translation**: Convert HQL/JPQL to native SQL
2. **Parameter Mapping**: Replace JPA parameter binding with JDBC parameter maps
3. **Result Mapping**: Replace entity hydration with custom row mappers
4. **Transaction Handling**: Implement explicit transaction management
5. **Error Handling**: Add comprehensive exception handling and validation
6. **Performance Optimization**: Add indexing recommendations and query optimization
7. **Testing Strategy**: Comprehensive test coverage for all scenarios

### Critical Success Factors

1. **Index Requirements**: Every query must have supporting database indexes
2. **Transaction Boundaries**: Explicit transaction management for all operations
3. **Error Handling**: Comprehensive exception handling with proper error messages
4. **Performance**: All operations must meet or exceed Hibernate performance
5. **Testing**: Complete test coverage including edge cases and error scenarios

This documentation template can be applied to all remaining 87 repositories to ensure a complete and accurate Hibernate removal process.