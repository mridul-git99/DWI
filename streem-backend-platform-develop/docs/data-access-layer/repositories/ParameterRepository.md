# ParameterRepository - Comprehensive Hibernate Removal Documentation

## Repository Overview

**Repository Interface**: `IParameterRepository`  
**File Location**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/repository/IParameterRepository.java`  
**Implementation**: `ParameterRepositoryImpl`  
**File Location**: `/Users/vaibhavverma/leucine/dwi/streem-backend/backend/src/main/java/com/leucine/streem/repository/impl/ParameterRepositoryImpl.java`  
**Entity**: `Parameter`  
**Primary Table**: `parameters`  
**Business Domain**: Digital Work Instructions Data Collection and Validation  
**Repository Hierarchy**: Extends `JpaRepository<Parameter, Long>`, `JpaSpecificationExecutor<Parameter>`

### Business Context
The ParameterRepository is the core data access layer for parameters in the Streem Digital Work Instructions platform. Parameters are the fundamental data collection points that define what information needs to be captured during job execution. This repository handles complex JSONB operations for validation rules, automation triggers, calculations, property filters, and parameter dependencies across the entire workflow system.

### Entity Relationships Overview
```
Parameter (N) ←→ (1) Task ←→ (1) Stage ←→ (1) Checklist
Parameter (N) ←→ (1) Checklist (direct relationship)
Parameter (1) ←→ (N) ParameterValue (job execution data)
Parameter (1) ←→ (N) ParameterVerification (quality control)
Parameter (1) ←→ (N) ParameterException (execution issues)
Parameter (1) ←→ (N) ParameterMediaMapping ←→ (1) Media
Parameter (1) ←→ (N) ParameterRuleMapping (rule dependencies)
Parameter (N) ←→ (N) Property (via JSONB data.propertyFilters)
Parameter (N) ←→ (N) Relation (via JSONB data.relationFilters)
Parameter (N) ←→ (N) Automation (via JSONB actionDetails.configuration)
```

### JSONB Fields Structure
```json
{
  "data": {
    "objectTypeId": "string",
    "propertyFilters": {
      "fields": [{"referencedParameterId": "123", "operator": "EQ"}]
    },
    "variables": {
      "var1": {"parameterId": "456", "displayName": "Variable 1"}
    },
    "leastCount": "parameterId"
  },
  "validations": [
    {
      "criteriaValidations": [{"referencedParameterId": "789"}],
      "resourceParameterValidations": [{"referencedParameterId": "101"}],
      "dateTimeValidations": [{"referencedParameterId": "112"}]
    }
  ],
  "autoInitialize": {
    "referencedParameters": ["113", "114"]
  },
  "rules": {
    "show": [{"referencedParameterId": "115"}],
    "hide": [{"referencedParameterId": "116"}]
  },
  "metadata": {
    "customFields": {}
  }
}
```

### Critical Performance Considerations
- **JSONB Indexes**: Requires GIN indexes on data, validations, rules, autoInitialize columns
- **Order Tree Operations**: Frequent reordering requires careful transaction management
- **Parameter Dependencies**: Complex queries traverse multiple JSONB paths
- **Bulk Operations**: Custom implementation handles batch inserts with JSONB casting
- **Cache Strategy**: Parameter rules and validations should be cached due to frequent access

---

## Method Documentation

### 1. findByTaskIdInOrderByOrderTree()

**Method Signature:**
```java
@Query(value = Queries.GET_PARAMETERS_BY_TASK_ID_IN_AND_ORDER_BY_ORDER_TREE)
List<Parameter> findByTaskIdInOrderByOrderTree(@Param("taskIds") Set<Long> taskIds);
```

**Input Parameters:**
- `taskIds` (Set<Long>): Set of task IDs to retrieve parameters for
  - Validation: Cannot be null or empty
  - Business Context: Used for multi-task parameter retrieval in job execution
  - Size Limit: Typically 1-50 tasks per call

**Return Type:**
- `List<Parameter>`: Ordered list of parameters across multiple tasks
- Ordering: stage.orderTree → task.orderTree → parameter.orderTree
- Edge Cases: Empty list if no parameters found or all tasks archived

**Generated SQL Query:**
```sql
SELECT p.id, p.type, p.target_entity_type, p.label, p.description, p.order_tree,
       p.is_mandatory, p.archived, p.data, p.tasks_id, p.validations, p.checklists_id,
       p.is_auto_initialized, p.auto_initialize, p.rules, p.hidden, p.verification_type,
       p.metadata, p.created_at, p.modified_at, p.created_by, p.modified_by
FROM parameters p 
INNER JOIN tasks t ON p.tasks_id = t.id 
INNER JOIN stages s ON t.stages_id = s.id
WHERE p.tasks_id IN (?, ?, ...)
  AND s.archived = false 
  AND t.archived = false 
  AND p.archived = false
ORDER BY s.order_tree, t.order_tree, p.order_tree;
```

**Database Execution Plan:**
- **Primary Access**: Index scan on `parameters_tasks_id_idx`
- **Joins**: Nested loop joins with tasks and stages tables
- **Ordering**: Filesort operation on composite ordering
- **Performance**: O(n log n) where n = total parameters in tasks
- **Indexes Required**: 
  - `parameters_tasks_id_idx`
  - `tasks_stages_id_idx`
  - Composite index on `(stages_id, order_tree)` for optimal sorting

**Entity Hydration Details:**
- **Lazy Loading**: Task, Checklist associations not hydrated
- **JSONB Fields**: data, validations, autoInitialize, rules, metadata fully loaded
- **Hydrated Fields**: All scalar fields (id, type, label, orderTree, etc.)
- **Collections**: parameterValues, medias, impactedByRules not loaded

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED sufficient
- **Transaction Required**: No explicit transaction needed (read-only)
- **Locking**: No locks required, uses MVCC

**Caching Behavior:**
- **L1 Cache**: Entities cached in Hibernate Session by ID
- **Query Cache**: Not typically cached due to dynamic task sets
- **Application Cache**: Consider caching for frequently accessed task combinations

**Business Logic Integration:**
- **Primary Usage**: Job execution parameter loading, task completion flows
- **Called From**: ParameterService.getParametersByTaskIds(), TaskExecutionHandler
- **Performance Critical**: Called during every job step execution

**DAO Conversion Strategy:**
```java
public class ParameterDAO {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public List<ParameterEntity> findByTaskIdInOrderByOrderTree(Set<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        String sql = """
            SELECT p.id, p.type, p.target_entity_type, p.label, p.description, 
                   p.order_tree, p.is_mandatory, p.archived, 
                   CAST(p.data AS TEXT) as data_json,
                   p.tasks_id, 
                   CAST(p.validations AS TEXT) as validations_json,
                   p.checklists_id, p.is_auto_initialized,
                   CAST(p.auto_initialize AS TEXT) as auto_initialize_json,
                   CAST(p.rules AS TEXT) as rules_json,
                   p.hidden, p.verification_type,
                   CAST(p.metadata AS TEXT) as metadata_json,
                   p.created_at, p.modified_at, p.created_by, p.modified_by
            FROM parameters p 
            INNER JOIN tasks t ON p.tasks_id = t.id 
            INNER JOIN stages s ON t.stages_id = s.id
            WHERE p.tasks_id IN (%s)
              AND s.archived = false 
              AND t.archived = false 
              AND p.archived = false
            ORDER BY s.order_tree, t.order_tree, p.order_tree
            """.formatted(String.join(",", Collections.nCopies(taskIds.size(), "?")));
        
        return jdbcTemplate.query(sql, 
            taskIds.toArray(),
            new ParameterRowMapper());
    }
}
```

**Performance Monitoring:**
- **Query Time**: Target < 100ms for typical task sets
- **Index Usage**: Monitor index scan vs table scan
- **Memory Usage**: Parameters with large JSONB data can impact memory

---

### 2. getEnabledParametersCountByTypeAndIdIn()

**Method Signature:**
```java
@Query(value = Queries.GET_ENABLED_PARAMETERS_COUNT_BY_PARAMETER_TYPE_IN_AND_ID_IN)
Integer getEnabledParametersCountByTypeAndIdIn(@Param("parameterIds") Set<Long> parameterIds, 
                                              @Param("types") Set<Type.Parameter> types);
```

**Input Parameters:**
- `parameterIds` (Set<Long>): Set of parameter IDs to check
  - Validation: Cannot be null, typically 1-100 IDs
  - Business Context: Validation of parameter availability for operations
- `types` (Set<Type.Parameter>): Parameter types to filter by
  - Validation: Cannot be null, valid enum values
  - Common Values: VALUE, CHOICE, MEDIA, RESOURCE, CALCULATION

**Return Type:**
- `Integer`: Count of enabled parameters matching criteria
- Range: 0 to parameterIds.size()
- Null Safety: Returns 0 if no matches found

**Generated SQL Query:**
```sql
SELECT COUNT(p.id) 
FROM parameters p 
WHERE p.id IN (?, ?, ?, ...)
  AND p.type IN (?, ?, ?)
  AND p.archived = false;
```

**Database Execution Plan:**
- **Primary Access**: Index scan on `parameters_pkey`
- **Type Filter**: Additional filter on type column (indexed)
- **Performance**: O(log n) per parameter ID lookup
- **Optimal Index**: Composite index on `(id, type, archived)`

**Entity Hydration Details:**
- **No Entity Loading**: Only returns count, no objects hydrated
- **Minimal I/O**: Only accesses indexed columns

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Transaction Required**: None (simple count query)
- **Consistency**: Snapshot isolation prevents phantom reads

**Business Logic Integration:**
- **Usage**: Parameter validation, bulk operation confirmation
- **Called From**: ParameterService.validateParameterAvailability()
- **Validation Context**: Pre-operation checks, permission validation

**DAO Conversion Strategy:**
```java
public Integer getEnabledParametersCountByTypeAndIdIn(Set<Long> parameterIds, 
                                                     Set<Type.Parameter> types) {
    if (parameterIds == null || parameterIds.isEmpty() || 
        types == null || types.isEmpty()) {
        return 0;
    }
    
    String sql = """
        SELECT COUNT(p.id) 
        FROM parameters p 
        WHERE p.id IN (%s)
          AND p.type IN (%s)
          AND p.archived = false
        """.formatted(
            String.join(",", Collections.nCopies(parameterIds.size(), "?")),
            String.join(",", Collections.nCopies(types.size(), "?"))
        );
    
    Object[] params = Stream.concat(
        parameterIds.stream().map(Object.class::cast),
        types.stream().map(type -> type.toString())
    ).toArray();
    
    return jdbcTemplate.queryForObject(sql, params, Integer.class);
}
```

---

### 3. getParametersByChecklistIdAndTargetEntityType()

**Method Signature:**
```java
@Query(value = Queries.GET_PARAMETERS_BY_CHECKLIST_ID_AND_TARGET_ENTITY_TYPE)
List<Parameter> getParametersByChecklistIdAndTargetEntityType(@Param("checklistId") Long checklistId, 
                                                             @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType);
```

**Input Parameters:**
- `checklistId` (Long): Checklist ID to retrieve parameters for
  - Validation: Cannot be null, must exist in checklists table
  - Business Context: Process-level parameter retrieval
- `targetEntityType` (Type.ParameterTargetEntityType): Target entity type filter
  - Values: PROCESS, TASK, STAGE
  - Business Context: Determines parameter scope and usage

**Return Type:**
- `List<Parameter>`: Ordered list of parameters for the checklist and entity type
- Ordering: By orderTree (sequential parameter order)
- Edge Cases: Empty list if checklist has no parameters of specified type

**Generated SQL Query:**
```sql
SELECT p.id, p.type, p.target_entity_type, p.label, p.description, p.order_tree,
       p.is_mandatory, p.archived, p.data, p.tasks_id, p.validations, p.checklists_id,
       p.is_auto_initialized, p.auto_initialize, p.rules, p.hidden, p.verification_type,
       p.metadata, p.created_at, p.modified_at, p.created_by, p.modified_by
FROM parameters p 
WHERE p.archived = false 
  AND p.checklists_id = ?
  AND p.target_entity_type = ?
ORDER BY p.order_tree;
```

**Database Execution Plan:**
- **Primary Access**: Index scan on `parameters_checklists_id_target_entity_type_idx`
- **Ordering**: Additional sort operation on order_tree
- **Performance**: O(log n + m) where m = matching parameters
- **Optimal Index**: Composite index on `(checklists_id, target_entity_type, archived, order_tree)`

**Entity Hydration Details:**
- **Full Parameter Load**: All parameter fields including JSONB columns
- **Lazy Associations**: Task, Checklist, parameterValues not loaded
- **JSONB Processing**: data, validations, rules parsed by Hibernate

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Transaction Required**: READ_ONLY transaction
- **Data Consistency**: Ensures consistent view of parameter hierarchy

**Business Logic Integration:**
- **Primary Usage**: Process configuration, parameter hierarchy display
- **Called From**: ChecklistService.getProcessParameters(), ParameterService
- **UI Integration**: Parameter forms, configuration wizards

**DAO Conversion Strategy:**
```java
public List<ParameterEntity> getParametersByChecklistIdAndTargetEntityType(
        Long checklistId, Type.ParameterTargetEntityType targetEntityType) {
    
    String sql = """
        SELECT p.id, p.type, p.target_entity_type, p.label, p.description, 
               p.order_tree, p.is_mandatory, p.archived,
               CAST(p.data AS TEXT) as data_json, p.tasks_id,
               CAST(p.validations AS TEXT) as validations_json,
               p.checklists_id, p.is_auto_initialized,
               CAST(p.auto_initialize AS TEXT) as auto_initialize_json,
               CAST(p.rules AS TEXT) as rules_json,
               p.hidden, p.verification_type,
               CAST(p.metadata AS TEXT) as metadata_json,
               p.created_at, p.modified_at, p.created_by, p.modified_by
        FROM parameters p 
        WHERE p.archived = false 
          AND p.checklists_id = ?
          AND p.target_entity_type = ?
        ORDER BY p.order_tree
        """;
    
    return jdbcTemplate.query(sql, 
        new Object[]{checklistId, targetEntityType.toString()},
        new ParameterRowMapper());
}
```

---

### 4. getArchivedParametersByReferencedParameterIds()

**Method Signature:**
```java
@Query(value = Queries.GET_ARCHIVED_PARAMETERS_BY_REFERENCED_PARAMETER_ID, nativeQuery = true)
List<Parameter> getArchivedParametersByReferencedParameterIds(@Param("referencedParameterIds") List<Long> referencedParameterIds);
```

**Input Parameters:**
- `referencedParameterIds` (List<Long>): List of parameter IDs to find archived versions
  - Validation: Cannot be null, typically small list (1-20 IDs)
  - Business Context: Historical parameter recovery, audit trails

**Return Type:**
- `List<Parameter>`: Archived parameters matching the provided IDs
- Content: Full parameter entities with all JSONB data
- Edge Cases: Empty list if no archived parameters found

**Generated SQL Query:**
```sql
SELECT p.id, p.type, p.target_entity_type, p.label, p.description, p.order_tree,
       p.is_mandatory, p.archived, p.data, p.tasks_id, p.validations, p.checklists_id,
       p.is_auto_initialized, p.auto_initialize, p.rules, p.hidden, p.verification_type,
       p.metadata, p.created_at, p.modified_at, p.created_by, p.modified_by
FROM parameters p 
WHERE p.archived = true 
  AND p.id IN (?, ?, ?);
```

**Database Execution Plan:**
- **Primary Access**: Index scan on `parameters_pkey`
- **Archive Filter**: Additional filter on archived column
- **Performance**: O(log n) per parameter ID
- **Index Usage**: Primary key index with archived condition

**Entity Hydration Details:**
- **Complete Entity**: All fields loaded including large JSONB columns
- **Historical Data**: Preserves complete parameter configuration state
- **Memory Impact**: Archived parameters may have large validation/rule sets

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Historical Consistency**: Snapshot isolation for audit integrity
- **Transaction Required**: READ_ONLY transaction sufficient

**Business Logic Integration:**
- **Usage**: Parameter history, configuration comparison, rollback operations
- **Called From**: AuditService, ParameterHistoryService
- **Administrative**: Version comparison, change tracking

**DAO Conversion Strategy:**
```java
public List<ParameterEntity> getArchivedParametersByReferencedParameterIds(
        List<Long> referencedParameterIds) {
    if (referencedParameterIds == null || referencedParameterIds.isEmpty()) {
        return Collections.emptyList();
    }
    
    String sql = """
        SELECT p.id, p.type, p.target_entity_type, p.label, p.description, 
               p.order_tree, p.is_mandatory, p.archived,
               CAST(p.data AS TEXT) as data_json, p.tasks_id,
               CAST(p.validations AS TEXT) as validations_json,
               p.checklists_id, p.is_auto_initialized,
               CAST(p.auto_initialize AS TEXT) as auto_initialize_json,
               CAST(p.rules AS TEXT) as rules_json,
               p.hidden, p.verification_type,
               CAST(p.metadata AS TEXT) as metadata_json,
               p.created_at, p.modified_at, p.created_by, p.modified_by
        FROM parameters p 
        WHERE p.archived = true 
          AND p.id IN (%s)
        """.formatted(String.join(",", Collections.nCopies(referencedParameterIds.size(), "?")));
    
    return jdbcTemplate.query(sql, 
        referencedParameterIds.toArray(),
        new ParameterRowMapper());
}
```

---

### 5. updateParametersTargetEntityType() - Bulk Update

**Method Signature:**
```java
@Modifying(clearAutomatically = true)
@Transactional(rollbackFor = Exception.class)
@Query(value = Queries.UPDATE_PARAMETER_TARGET_ENTITY_TYPE_BY_CHECKLIST_ID_AND_TARGET_ENTITY_TYPE)
void updateParametersTargetEntityType(@Param("checklistId") Long checklistId, 
                                      @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType, 
                                      @Param("updatedTargetEntityType") Type.ParameterTargetEntityType updatedTargetEntityType);
```

**Input Parameters:**
- `checklistId` (Long): Checklist containing parameters to update
  - Validation: Cannot be null, must exist in checklists table
- `targetEntityType` (Type.ParameterTargetEntityType): Current entity type to match
  - Validation: Valid enum value (PROCESS, TASK, STAGE)
- `updatedTargetEntityType` (Type.ParameterTargetEntityType): New entity type to set
  - Validation: Valid enum value, different from targetEntityType

**Return Type:**
- `void`: No return value (bulk update operation)
- Side Effects: Updates multiple parameter records
- Audit: Modified timestamp and user automatically updated

**Generated SQL Query:**
```sql
UPDATE parameters p 
SET target_entity_type = ?,
    modified_at = ?,
    modified_by = ?
WHERE p.checklists_id = ?
  AND p.target_entity_type = ?;
```

**Database Execution Plan:**
- **Update Strategy**: Index scan on `parameters_checklists_id_target_entity_type_idx`
- **Row Locking**: Exclusive locks on affected rows during update
- **Performance**: O(log n + m) where m = matching parameters
- **Transaction Log**: Generates undo/redo entries for all affected rows

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED with row-level locking
- **Transaction Required**: REQUIRED (automatically created by @Transactional)
- **Rollback**: Complete rollback on any exception
- **Deadlock Risk**: Minimal due to consistent locking order

**Entity Management:**
- **Hibernate Cache**: @Modifying(clearAutomatically = true) evicts L1 cache
- **Dirty Checking**: Bypassed due to native query
- **Cascade Effects**: No cascading updates to related entities

**Business Logic Integration:**
- **Usage**: Process restructuring, parameter scope changes
- **Called From**: ChecklistService.reorganizeParameters()
- **Validation**: Pre-update validation ensures data consistency

**DAO Conversion Strategy:**
```java
@Transactional(rollbackFor = Exception.class)
public void updateParametersTargetEntityType(Long checklistId, 
                                           Type.ParameterTargetEntityType targetEntityType,
                                           Type.ParameterTargetEntityType updatedTargetEntityType,
                                           Long modifiedBy) {
    
    String sql = """
        UPDATE parameters 
        SET target_entity_type = ?,
            modified_at = ?,
            modified_by = ?
        WHERE checklists_id = ?
          AND target_entity_type = ?
        """;
    
    int rowsUpdated = jdbcTemplate.update(sql,
        updatedTargetEntityType.toString(),
        System.currentTimeMillis(),
        modifiedBy,
        checklistId,
        targetEntityType.toString());
    
    log.info("Updated {} parameters from {} to {} for checklist {}", 
        rowsUpdated, targetEntityType, updatedTargetEntityType, checklistId);
}
```

**Performance Monitoring:**
- **Update Count**: Track number of affected rows
- **Transaction Time**: Monitor for long-running updates
- **Lock Contention**: Watch for deadlocks during concurrent updates

---

### 6. updateParametersTargetEntityType() - Targeted Update

**Method Signature:**
```java
@Modifying(clearAutomatically = true)
@Transactional(rollbackFor = Exception.class)
@Query(value = Queries.UPDATE_PARAMETERS_TARGET_ENTITY_TYPE)
Integer updateParametersTargetEntityType(@Param("parameterIds") Set<Long> parameterIds, 
                                        @Param("targetEntityType") Type.ParameterTargetEntityType targetEntityType);
```

**Input Parameters:**
- `parameterIds` (Set<Long>): Specific parameter IDs to update
  - Validation: Cannot be null or empty, typically 1-50 parameters
  - Business Context: Targeted parameter scope changes
- `targetEntityType` (Type.ParameterTargetEntityType): New target entity type
  - Validation: Valid enum value
  - Business Impact: Changes parameter scope and availability

**Return Type:**
- `Integer`: Number of parameters actually updated
- Range: 0 to parameterIds.size()
- Business Logic: Used for validation and confirmation

**Generated SQL Query:**
```sql
UPDATE parameters p 
SET target_entity_type = ?,
    modified_at = ?,
    modified_by = ?
WHERE p.id IN (?, ?, ?, ...)
RETURNING COUNT(*);
```

**Database Execution Plan:**
- **Update Strategy**: Multiple index lookups on `parameters_pkey`
- **Row Locking**: Individual row locks per parameter
- **Performance**: O(log n) per parameter ID
- **Batch Processing**: Updates processed as single transaction

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED with explicit row locking
- **Transaction Required**: REQUIRED (managed by Spring)
- **Consistency**: All parameters updated or none (atomicity)
- **Recovery**: Full rollback on partial failure

**Entity Management:**
- **Cache Eviction**: L1 cache cleared for affected entities
- **Optimistic Locking**: May conflict with concurrent parameter updates
- **Validation**: Parameter existence validated during update

**Business Logic Integration:**
- **Usage**: Selective parameter scope modification
- **Called From**: ParameterService.updateParameterScopes()
- **Validation**: Pre/post update count validation

**DAO Conversion Strategy:**
```java
@Transactional(rollbackFor = Exception.class)
public Integer updateParametersTargetEntityType(Set<Long> parameterIds,
                                              Type.ParameterTargetEntityType targetEntityType,
                                              Long modifiedBy) {
    if (parameterIds == null || parameterIds.isEmpty()) {
        return 0;
    }
    
    String sql = """
        UPDATE parameters 
        SET target_entity_type = ?,
            modified_at = ?,
            modified_by = ?
        WHERE id IN (%s)
        """.formatted(String.join(",", Collections.nCopies(parameterIds.size(), "?")));
    
    Object[] params = Stream.concat(
        Stream.of(targetEntityType.toString(), System.currentTimeMillis(), modifiedBy),
        parameterIds.stream().map(Object.class::cast)
    ).toArray();
    
    return jdbcTemplate.update(sql, params);
}
```

---

### 7. reorderParameter()

**Method Signature:**
```java
@Transactional(rollbackFor = Exception.class)
@Modifying
@Query(value = Queries.UPDATE_PARAMETER_ORDER, nativeQuery = true)
void reorderParameter(@Param("parameterId") Long parameterId, 
                     @Param("order") Integer order, 
                     @Param("userId") Long userId, 
                     @Param("modifiedAt") Long modifiedAt);
```

**Input Parameters:**
- `parameterId` (Long): Parameter to reorder
  - Validation: Cannot be null, must exist
- `order` (Integer): New order tree position
  - Validation: Must be positive, within valid range for task
- `userId` (Long): User performing the reorder operation
  - Audit: Tracks who made the change
- `modifiedAt` (Long): Timestamp of modification
  - Audit: When the change occurred

**Return Type:**
- `void`: No return value
- Side Effects: Updates single parameter order and audit fields
- Consistency: May require additional order tree adjustments

**Generated SQL Query:**
```sql
UPDATE parameters 
SET order_tree = ?, 
    modified_by = ?, 
    modified_at = ? 
WHERE id = ?;
```

**Database Execution Plan:**
- **Update Strategy**: Direct primary key lookup
- **Performance**: O(1) - single row update
- **Locking**: Row-level exclusive lock
- **Index Usage**: Primary key index for instant access

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Transaction Required**: REQUIRED (must be in transaction)
- **Consistency**: Part of larger reordering operation
- **Deadlock Prevention**: Consistent parameter ordering for updates

**Order Tree Management:**
- **Gap Management**: May create gaps in order sequence
- **Duplicate Handling**: Application must ensure unique orders within task
- **Reordering Strategy**: Often paired with increaseOrderTreeByOneAfterParameter()

**Business Logic Integration:**
- **Usage**: Parameter reordering in task configuration
- **Called From**: ParameterService.reorderParameters()
- **UI Interaction**: Drag-and-drop parameter ordering

**DAO Conversion Strategy:**
```java
@Transactional(rollbackFor = Exception.class)
public void reorderParameter(Long parameterId, Integer order, Long userId, Long modifiedAt) {
    String sql = """
        UPDATE parameters 
        SET order_tree = ?, 
            modified_by = ?, 
            modified_at = ? 
        WHERE id = ?
        """;
    
    int rowsUpdated = jdbcTemplate.update(sql, order, userId, modifiedAt, parameterId);
    
    if (rowsUpdated == 0) {
        throw new EntityNotFoundException("Parameter not found: " + parameterId);
    }
}
```

**Order Tree Consistency:**
- **Before Reorder**: Validate new position is valid
- **After Reorder**: May need to adjust other parameter positions
- **Gap Handling**: Consider compacting order tree periodically

---

### 8. updateParameterVisibility()

**Method Signature:**
```java
@Modifying
@Transactional(rollbackFor = Exception.class)
@Query(value = Queries.UPDATE_VISIBILITY_OF_PARAMETERS)
void updateParameterVisibility(@Param("hiddenParameterIds") Set<Long> hiddenParameterIds, 
                              @Param("visibleParameterIds") Set<Long> visibleParameterIds);
```

**Input Parameters:**
- `hiddenParameterIds` (Set<Long>): Parameter IDs to hide
  - Validation: Can be null or empty
  - Business Context: Parameters to make invisible in UI
- `visibleParameterIds` (Set<Long>): Parameter IDs to show
  - Validation: Can be null or empty
  - Business Context: Parameters to make visible in UI

**Return Type:**
- `void`: No return value
- Side Effects: Updates hidden flag for multiple parameters
- Batch Operation: Handles both hide and show operations atomically

**Generated SQL Query:**
```sql
UPDATE parameters p
SET hidden = CASE 
    WHEN p.id IN (?, ?, ?) THEN true     -- hiddenParameterIds
    WHEN p.id IN (?, ?, ?) THEN false    -- visibleParameterIds
    ELSE p.hidden
END,
modified_at = ?,
modified_by = ?
WHERE p.id IN (?, ?, ?, ?, ?, ?);        -- union of both sets
```

**Database Execution Plan:**
- **Update Strategy**: Batch update with CASE expression
- **Performance**: O(log n) per parameter ID
- **Locking**: Row-level locks on all affected parameters
- **Efficiency**: Single statement handles both hide and show operations

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Transaction Required**: REQUIRED (atomic visibility changes)
- **Consistency**: All visibility changes applied together
- **Rollback**: Complete rollback if any parameter update fails

**Business Logic Integration:**
- **Usage**: Conditional parameter display, rule-based hiding/showing
- **Called From**: ParameterService.updateVisibility(), RuleService
- **Rule Engine**: Often triggered by parameter rule evaluation

**DAO Conversion Strategy:**
```java
@Transactional(rollbackFor = Exception.class)
public void updateParameterVisibility(Set<Long> hiddenParameterIds, 
                                     Set<Long> visibleParameterIds,
                                     Long modifiedBy) {
    Set<Long> allParameterIds = new HashSet<>();
    if (hiddenParameterIds != null) allParameterIds.addAll(hiddenParameterIds);
    if (visibleParameterIds != null) allParameterIds.addAll(visibleParameterIds);
    
    if (allParameterIds.isEmpty()) {
        return;
    }
    
    String sql = """
        UPDATE parameters 
        SET hidden = CASE 
            WHEN id IN (%s) THEN true
            WHEN id IN (%s) THEN false
            ELSE hidden
        END,
        modified_at = ?,
        modified_by = ?
        WHERE id IN (%s)
        """.formatted(
            hiddenParameterIds != null && !hiddenParameterIds.isEmpty() 
                ? String.join(",", Collections.nCopies(hiddenParameterIds.size(), "?"))
                : "NULL",
            visibleParameterIds != null && !visibleParameterIds.isEmpty()
                ? String.join(",", Collections.nCopies(visibleParameterIds.size(), "?"))
                : "NULL",
            String.join(",", Collections.nCopies(allParameterIds.size(), "?"))
        );
    
    List<Object> params = new ArrayList<>();
    if (hiddenParameterIds != null) params.addAll(hiddenParameterIds);
    if (visibleParameterIds != null) params.addAll(visibleParameterIds);
    params.add(System.currentTimeMillis());
    params.add(modifiedBy);
    params.addAll(allParameterIds);
    
    jdbcTemplate.update(sql, params.toArray());
}
```

---

### 9. isLinkedParameterExistsByParameterId()

**Method Signature:**
```java
@Query(value = Queries.IS_LINKED_PARAMETER_EXISTS_BY_PARAMETER_ID, nativeQuery = true)
boolean isLinkedParameterExistsByParameterId(@Param("checklistId") Long checklistId, 
                                           @Param("parameterId") String parameterId);
```

**Input Parameters:**
- `checklistId` (Long): Checklist scope for parameter search
  - Validation: Cannot be null
  - Business Context: Limits search to specific process
- `parameterId` (String): Parameter ID to search for in JSONB data
  - Validation: Cannot be null, typically numeric string
  - Business Context: Referenced parameter ID in JSONB fields

**Return Type:**
- `boolean`: true if parameter is referenced, false otherwise
- Performance: Optimized existence check (stops at first match)
- Business Logic: Used for dependency validation before deletion

**Generated SQL Query:**
```sql
SELECT EXISTS(
    SELECT 1
    FROM parameters p
    WHERE p.checklists_id = ?
      AND p.archived = false
      AND (
          -- Check in data.variables
          p.data -> 'variables' ? ?
          OR
          -- Check in data.propertyFilters.fields
          EXISTS (
              SELECT 1 
              FROM jsonb_array_elements(p.data -> 'propertyFilters' -> 'fields') AS field
              WHERE field ->> 'referencedParameterId' = ?
          )
          OR
          -- Check in validations array
          EXISTS (
              SELECT 1
              FROM jsonb_array_elements(p.validations) AS validation,
                   jsonb_array_elements(validation -> 'criteriaValidations') AS criteria
              WHERE criteria ->> 'referencedParameterId' = ?
          )
          OR
          -- Check in autoInitialize.referencedParameters
          p.auto_initialize -> 'referencedParameters' ? ?
          OR
          -- Check in rules.show/hide
          p.rules -> 'show' @> ?::jsonb
          OR p.rules -> 'hide' @> ?::jsonb
      )
);
```

**Database Execution Plan:**
- **JSONB Operations**: Uses GIN index on JSONB columns
- **Early Termination**: EXISTS stops at first match
- **Performance**: O(log n) with proper JSONB indexes
- **Index Requirements**: GIN indexes on data, validations, autoInitialize, rules

**JSONB Query Optimization:**
- **Operator Usage**: 
  - `?` for key existence
  - `@>` for containment
  - `->>` for text extraction
- **Index Strategy**: GIN indexes support all JSONB operators
- **Query Plan**: Bitmap heap scans on JSONB indexes

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Snapshot isolation prevents phantom reads
- **Transaction Required**: None (read-only existence check)

**Business Logic Integration:**
- **Usage**: Parameter dependency validation, safe deletion checks
- **Called From**: ParameterService.canDeleteParameter()
- **Validation Context**: Pre-deletion dependency checks

**DAO Conversion Strategy:**
```java
public boolean isLinkedParameterExistsByParameterId(Long checklistId, String parameterId) {
    String sql = """
        SELECT EXISTS(
            SELECT 1
            FROM parameters p
            WHERE p.checklists_id = ?
              AND p.archived = false
              AND (
                  -- Check in data.variables
                  p.data -> 'variables' ? ?
                  OR
                  -- Check in data.propertyFilters.fields
                  EXISTS (
                      SELECT 1 
                      FROM jsonb_array_elements(p.data -> 'propertyFilters' -> 'fields') AS field
                      WHERE field ->> 'referencedParameterId' = ?
                  )
                  OR
                  -- Check in validations array
                  EXISTS (
                      SELECT 1
                      FROM jsonb_array_elements(p.validations) AS validation,
                           jsonb_array_elements(validation -> 'criteriaValidations') AS criteria
                      WHERE criteria ->> 'referencedParameterId' = ?
                  )
                  OR
                  -- Check in autoInitialize.referencedParameters
                  p.auto_initialize -> 'referencedParameters' ? ?
                  OR
                  -- Check in rules.show/hide
                  p.rules -> 'show' @> ?::jsonb
                  OR p.rules -> 'hide' @> ?::jsonb
              )
        )
        """;
    
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(sql, Boolean.class,
            checklistId, parameterId, parameterId, parameterId, 
            parameterId, "[{\"referencedParameterId\":\"" + parameterId + "\"}]",
            "[{\"referencedParameterId\":\"" + parameterId + "\"}]")
    );
}
```

**Performance Considerations:**
- **JSONB Index**: Crucial for query performance
- **Complex Query**: Multiple OR conditions may be expensive
- **Caching**: Consider caching results for frequently checked parameters

---

### 10. getChecklistIdsByObjectTypeInData()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_CHECKLIST_IDS_BY_OBJECT_TYPE_IN_DATA, nativeQuery = true)
Set<Long> getChecklistIdsByObjectTypeInData(@Param("objectTypeId") String objectTypeId);
```

**Input Parameters:**
- `objectTypeId` (String): Object type ID to search for in parameter data
  - Validation: Cannot be null
  - Business Context: External system object type reference
  - Format: Typically UUID or structured identifier

**Return Type:**
- `Set<Long>`: Unique checklist IDs containing parameters with specified object type
- Distinctness: Automatically deduplicates checklist IDs
- Edge Cases: Empty set if no parameters reference the object type

**Generated SQL Query:**
```sql
SELECT DISTINCT p.checklists_id
FROM parameters p 
WHERE p.data ->> 'objectTypeId' = ?
  AND p.archived = false;
```

**Database Execution Plan:**
- **JSONB Access**: Uses GIN index on data column
- **Text Extraction**: `->>` operator extracts text value
- **Distinctness**: Hash aggregate for unique checklist IDs
- **Performance**: O(log n) with GIN index, O(n) without

**JSONB Index Requirements:**
```sql
CREATE INDEX idx_parameters_data_gin ON parameters USING GIN (data);
-- Or more specific:
CREATE INDEX idx_parameters_object_type_id ON parameters 
USING GIN ((data ->> 'objectTypeId'));
```

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Snapshot isolation for consistent results
- **Transaction Required**: None (read-only query)

**Business Logic Integration:**
- **Usage**: Cross-system dependency tracking, object type impact analysis
- **Called From**: ObjectTypeService.getAffectedChecklists()
- **Integration Context**: External system changes, object type lifecycle

**DAO Conversion Strategy:**
```java
public Set<Long> getChecklistIdsByObjectTypeInData(String objectTypeId) {
    String sql = """
        SELECT DISTINCT p.checklists_id
        FROM parameters p 
        WHERE p.data ->> 'objectTypeId' = ?
          AND p.archived = false
        """;
    
    List<Long> checklistIds = jdbcTemplate.queryForList(sql, Long.class, objectTypeId);
    return new HashSet<>(checklistIds);
}
```

**Performance Monitoring:**
- **JSONB Index Usage**: Monitor for bitmap heap scans
- **Query Performance**: Target < 50ms for typical datasets
- **Memory Usage**: Result set size depends on object type usage

---

### 11. getResourceParametersByObjectTypeIdAndChecklistId()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PARAMETER_BY_CHECKLIST_ID_AND_OBJECT_TYPE_ID, nativeQuery = true)
List<ParameterView> getResourceParametersByObjectTypeIdAndChecklistId(@Param("objectTypeId") String objectTypeId, 
                                                                      @Param("checklistIds") List<Long> checklistIds);
```

**Input Parameters:**
- `objectTypeId` (String): Object type ID filter
  - Validation: Cannot be null
  - Business Context: External resource type
- `checklistIds` (List<Long>): List of checklist IDs to search within
  - Validation: Cannot be null or empty
  - Business Context: Scope limitation for performance

**Return Type:**
- `List<ParameterView>`: Projection containing id, data, checklistId
- Lightweight: Only essential fields for resource parameter processing
- Data Format: JSONB data cast to text for application processing

**Generated SQL Query:**
```sql
SELECT p.id AS id,
       CAST(p.data AS TEXT) AS data,
       p.checklists_id AS checklistId
FROM parameters p
WHERE p.data ->> 'objectTypeId' = ?
  AND p.checklists_id IN (?, ?, ?)
  AND p.archived = false
  AND p.type = 'RESOURCE'
ORDER BY p.checklists_id, p.order_tree;
```

**Database Execution Plan:**
- **JSONB Filter**: GIN index scan on data column
- **Checklist Filter**: Additional index scan on checklists_id
- **Type Filter**: Uses index on type column
- **Composite Strategy**: May use multiple index scans with bitmap operations

**Optimal Index Strategy:**
```sql
CREATE INDEX idx_parameters_resource_lookup ON parameters 
USING GIN (data) 
WHERE type = 'RESOURCE' AND archived = false;

CREATE INDEX idx_parameters_checklist_type ON parameters 
(checklists_id, type) 
WHERE archived = false;
```

**Projection Benefits:**
- **Reduced I/O**: Only loads necessary columns
- **Memory Efficiency**: Smaller result set size
- **Network Optimization**: Less data transfer to application

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Consistent view across multiple checklists
- **Transaction Required**: READ_ONLY transaction

**Business Logic Integration:**
- **Usage**: Resource parameter discovery, external system integration
- **Called From**: ResourceService.getParametersByObjectType()
- **Integration**: Property resolution, resource validation

**DAO Conversion Strategy:**
```java
public List<ParameterView> getResourceParametersByObjectTypeIdAndChecklistId(
        String objectTypeId, List<Long> checklistIds) {
    
    if (checklistIds == null || checklistIds.isEmpty()) {
        return Collections.emptyList();
    }
    
    String sql = """
        SELECT p.id AS id,
               CAST(p.data AS TEXT) AS data,
               p.checklists_id AS checklistId
        FROM parameters p
        WHERE p.data ->> 'objectTypeId' = ?
          AND p.checklists_id IN (%s)
          AND p.archived = false
          AND p.type = 'RESOURCE'
        ORDER BY p.checklists_id, p.order_tree
        """.formatted(String.join(",", Collections.nCopies(checklistIds.size(), "?")));
    
    Object[] params = Stream.concat(
        Stream.of(objectTypeId),
        checklistIds.stream().map(Object.class::cast)
    ).toArray();
    
    return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
        ParameterView view = new ParameterView();
        view.setId(rs.getLong("id"));
        view.setData(rs.getString("data"));
        view.setChecklistId(rs.getLong("checklistId"));
        return view;
    });
}
```

---

### 12. getAllParametersWhereParameterIsUsedInRules()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_RULES, nativeQuery = true)
List<IdView> getAllParametersWhereParameterIsUsedInRules(@Param("hideRulesJson") String hideRulesJson, 
                                                        @Param("showRulesJson") String showRulesJson, 
                                                        @Param("parameterId") Long parameterId);
```

**Input Parameters:**
- `hideRulesJson` (String): JSON structure for hide rules matching
  - Format: `[{"referencedParameterId":"123"}]`
  - Business Context: Hide rule dependency lookup
- `showRulesJson` (String): JSON structure for show rules matching
  - Format: `[{"referencedParameterId":"123"}]`
  - Business Context: Show rule dependency lookup
- `parameterId` (Long): Parameter ID being referenced
  - Validation: Cannot be null
  - Business Context: Parameter whose dependencies we're finding

**Return Type:**
- `List<IdView>`: List containing only parameter IDs
- Lightweight: Minimal projection for performance
- Usage: Dependency tracking, rule impact analysis

**Generated SQL Query:**
```sql
SELECT ? AS id
FROM parameters p
WHERE (p.rules @> ?::jsonb          -- hideRulesJson containment
       OR p.rules @> ?::jsonb)      -- showRulesJson containment
   OR (
       -- Additional rule structure checks
       EXISTS (
           SELECT 1
           FROM jsonb_array_elements(p.rules -> 'show') AS show_rule
           WHERE show_rule ->> 'referencedParameterId' = ?::text
       )
       OR EXISTS (
           SELECT 1
           FROM jsonb_array_elements(p.rules -> 'hide') AS hide_rule
           WHERE hide_rule ->> 'referencedParameterId' = ?::text
       )
   )
   AND p.archived = false;
```

**JSONB Query Operations:**
- **Containment (`@>`)**: Checks if rules JSONB contains specified structure
- **Array Expansion**: `jsonb_array_elements()` for array rule structures
- **Text Extraction (`->>`)**: Extracts referencedParameterId as text
- **Existence Checks**: EXISTS for array element matching

**Database Execution Plan:**
- **JSONB Index**: GIN index scan on rules column
- **Containment Search**: Efficient with GIN index
- **Array Processing**: May require additional processing for complex rules
- **Performance**: O(log n) with proper JSONB indexing

**Rule Structure Examples:**
```json
{
  "show": [
    {"referencedParameterId": "123", "operator": "EQ", "value": "true"}
  ],
  "hide": [
    {"referencedParameterId": "456", "operator": "NE", "value": "false"}
  ]
}
```

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Snapshot isolation for rule evaluation
- **Transaction Required**: None (read-only dependency check)

**Business Logic Integration:**
- **Usage**: Rule dependency validation, parameter deletion checks
- **Called From**: RuleService.findParameterDependencies()
- **Rule Engine**: Impact analysis before parameter changes

**DAO Conversion Strategy:**
```java
public List<IdView> getAllParametersWhereParameterIsUsedInRules(
        String hideRulesJson, String showRulesJson, Long parameterId) {
    
    String sql = """
        SELECT DISTINCT ? AS id
        FROM parameters p
        WHERE p.archived = false
          AND (
              p.rules @> ?::jsonb
              OR p.rules @> ?::jsonb
              OR EXISTS (
                  SELECT 1
                  FROM jsonb_array_elements(p.rules -> 'show') AS show_rule
                  WHERE show_rule ->> 'referencedParameterId' = ?::text
              )
              OR EXISTS (
                  SELECT 1
                  FROM jsonb_array_elements(p.rules -> 'hide') AS hide_rule
                  WHERE hide_rule ->> 'referencedParameterId' = ?::text
              )
          )
        """;
    
    return jdbcTemplate.query(sql, 
        new Object[]{parameterId, hideRulesJson, showRulesJson, 
                    parameterId.toString(), parameterId.toString()},
        (rs, rowNum) -> {
            IdView view = new IdView();
            view.setId(rs.getLong("id"));
            return view;
        });
}
```

**Performance Optimization:**
- **JSONB Index**: Essential for containment operations
- **Rule Caching**: Consider caching frequently accessed rule dependencies
- **Query Complexity**: Monitor execution time for complex rule structures

---

### 13. getAllParametersWhereParameterIsUsedInPropertyFilters()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_PROPERTY_FILTERS, nativeQuery = true)
List<IdView> getAllParametersWhereParameterIsUsedInPropertyFilters(@Param("parameterId") String parameterId);
```

**Input Parameters:**
- `parameterId` (String): Parameter ID to search for in property filters
  - Validation: Cannot be null
  - Business Context: Referenced parameter in property filter expressions
  - Format: Typically numeric string representation

**Return Type:**
- `List<IdView>`: Parameter IDs that reference the given parameter in property filters
- Dependency Chain: Parameters that depend on the input parameter
- Usage: Property filter dependency analysis

**Generated SQL Query:**
```sql
SELECT p.id AS id
FROM parameters p
JOIN LATERAL jsonb_array_elements(p.data -> 'propertyFilters' -> 'fields') AS fields_element ON TRUE
WHERE fields_element ->> 'referencedParameterId' = ?
  AND p.archived = false;
```

**JSONB Structure (propertyFilters):**
```json
{
  "data": {
    "propertyFilters": {
      "fields": [
        {
          "referencedParameterId": "123",
          "operator": "EQ",
          "value": "some_value",
          "propertyId": "property_uuid"
        }
      ]
    }
  }
}
```

**Database Execution Plan:**
- **LATERAL Join**: Expands JSONB array elements for processing
- **Array Processing**: `jsonb_array_elements()` creates row per array element
- **Text Extraction**: `->>` operator for referencedParameterId
- **Performance**: Requires GIN index on data column

**JSONB Index Optimization:**
```sql
CREATE INDEX idx_parameters_property_filters ON parameters 
USING GIN ((data -> 'propertyFilters' -> 'fields'));

-- Expression index for specific path
CREATE INDEX idx_parameters_referenced_param_id ON parameters 
USING GIN ((data -> 'propertyFilters' -> 'fields'));
```

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Snapshot isolation for dependency analysis
- **Transaction Required**: None (read-only dependency tracking)

**Business Logic Integration:**
- **Usage**: Property filter dependency validation, parameter impact analysis
- **Called From**: PropertyService.getParameterDependencies()
- **Filter Engine**: Property-based filtering system dependencies

**DAO Conversion Strategy:**
```java
public List<IdView> getAllParametersWhereParameterIsUsedInPropertyFilters(String parameterId) {
    String sql = """
        SELECT p.id AS id
        FROM parameters p,
             jsonb_array_elements(p.data -> 'propertyFilters' -> 'fields') AS fields_element
        WHERE fields_element ->> 'referencedParameterId' = ?
          AND p.archived = false
        """;
    
    return jdbcTemplate.query(sql, new Object[]{parameterId}, (rs, rowNum) -> {
        IdView view = new IdView();
        view.setId(rs.getLong("id"));
        return view;
    });
}
```

**Performance Considerations:**
- **Array Expansion**: Can be expensive for parameters with many filter fields
- **Index Strategy**: GIN index crucial for acceptable performance
- **Query Optimization**: Consider materializing frequently accessed dependencies

---

### 14. getAllParametersWhereParameterIsUsedInPropertyValidations()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_PROPERTY_VALIDATIONS, nativeQuery = true)
List<IdView> getAllParametersWhereParameterIsUsedInPropertyValidations(@Param("parameterId") String parameterId);
```

**Input Parameters:**
- `parameterId` (String): Parameter ID referenced in property validations
  - Validation: Cannot be null
  - Business Context: Parameter used in validation rules
  - Usage: Validation dependency tracking

**Return Type:**
- `List<IdView>`: Parameters containing property validations that reference the input parameter
- Dependencies: Used for validation rule impact analysis
- Safety: Prevents deletion of parameters used in validations

**Generated SQL Query:**
```sql
SELECT DISTINCT p.id AS id
FROM parameters p,
     jsonb_array_elements(p.validations) AS validation,
     jsonb_array_elements(validation -> 'propertyValidations') AS propertyValidation
WHERE propertyValidation ->> 'referencedParameterId' = ?
  AND p.archived = false;
```

**JSONB Structure (propertyValidations):**
```json
{
  "validations": [
    {
      "propertyValidations": [
        {
          "referencedParameterId": "123",
          "operator": "GT",
          "value": "100",
          "propertyId": "property_uuid",
          "errorMessage": "Value must be greater than referenced parameter"
        }
      ]
    }
  ]
}
```

**Database Execution Plan:**
- **Nested Array Expansion**: Two levels of `jsonb_array_elements()`
- **Complex Processing**: Multiple array traversals per parameter
- **Distinctness**: DISTINCT eliminates duplicate parameter IDs
- **Performance**: Expensive without proper JSONB indexes

**Advanced JSONB Indexing:**
```sql
CREATE INDEX idx_parameters_validations_gin ON parameters 
USING GIN (validations);

-- Expression-based index for performance
CREATE INDEX idx_parameters_property_validations ON parameters 
USING GIN ((validations #> '{0,propertyValidations}'));
```

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Ensures complete dependency view
- **Transaction Required**: None (dependency analysis only)

**Business Logic Integration:**
- **Usage**: Validation rule dependency management
- **Called From**: ValidationService.getParameterDependencies()
- **Safety Checks**: Pre-deletion validation dependency verification

**DAO Conversion Strategy:**
```java
public List<IdView> getAllParametersWhereParameterIsUsedInPropertyValidations(String parameterId) {
    String sql = """
        SELECT DISTINCT p.id AS id
        FROM parameters p,
             jsonb_array_elements(p.validations) AS validation,
             jsonb_array_elements(validation -> 'propertyValidations') AS propertyValidation
        WHERE propertyValidation ->> 'referencedParameterId' = ?
          AND p.archived = false
        """;
    
    return jdbcTemplate.query(sql, new Object[]{parameterId}, (rs, rowNum) -> {
        IdView view = new IdView();
        view.setId(rs.getLong("id"));
        return view;
    });
}
```

**Performance Warning:**
- **Complex Query**: Nested array expansion can be very expensive
- **Index Requirements**: Multiple GIN indexes may be needed
- **Query Time**: Monitor for queries exceeding 1 second

---

### 15. getAllParametersWhereParameterIsUsedInResourceValidations()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_RESOURCE_VALIDATION, nativeQuery = true)
List<IdView> getAllParametersWhereParameterIsUsedInResourceValidations(@Param("parameterId") String parameterId);
```

**Input Parameters:**
- `parameterId` (String): Parameter ID used in resource validations
  - Validation: Cannot be null
  - Business Context: External resource validation references
  - Usage: Resource-based validation dependency tracking

**Return Type:**
- `List<IdView>`: Parameters with resource validations referencing the input parameter
- Dependencies: Critical for resource validation integrity
- Impact: Used for resource parameter change impact analysis

**Generated SQL Query:**
```sql
SELECT DISTINCT ? AS id
FROM parameters p,
     jsonb_array_elements(p.validations) AS validation,
     jsonb_array_elements(validation -> 'resourceParameterValidations') AS resourceValidation
WHERE p.archived = false
  AND resourceValidation ->> 'referencedParameterId' = ?;
```

**JSONB Structure (resourceParameterValidations):**
```json
{
  "validations": [
    {
      "resourceParameterValidations": [
        {
          "referencedParameterId": "123",
          "resourceType": "EQUIPMENT",
          "validationType": "AVAILABILITY_CHECK",
          "operator": "EXISTS"
        }
      ]
    }
  ]
}
```

**Database Execution Plan:**
- **Resource Validation Processing**: Specialized validation type
- **Triple Nested Query**: Parameters → validations → resourceParameterValidations
- **Resource Integration**: Links to external resource systems
- **Performance**: Most complex validation dependency query

**Business Logic Integration:**
- **Usage**: Resource validation dependency management
- **Called From**: ResourceValidationService.getDependencies()
- **External Systems**: Integration with equipment, material, personnel systems

**DAO Conversion Strategy:**
```java
public List<IdView> getAllParametersWhereParameterIsUsedInResourceValidations(String parameterId) {
    String sql = """
        SELECT DISTINCT p.id AS id
        FROM parameters p,
             jsonb_array_elements(p.validations) AS validation,
             jsonb_array_elements(validation -> 'resourceParameterValidations') AS resourceValidation
        WHERE p.archived = false
          AND resourceValidation ->> 'referencedParameterId' = ?
        """;
    
    return jdbcTemplate.query(sql, new Object[]{parameterId}, (rs, rowNum) -> {
        IdView view = new IdView();
        view.setId(rs.getLong("id"));
        return view;
    });
}
```

---

### 16. getNonHiddenAutoInitialisedParametersByTaskExecutionId()

**Method Signature:**
```java
@Query(value = Queries.GET_NON_HIDDEN_AUTO_INITIALISED_PARAMETERS_BY_TASK_EXECUTION_ID, nativeQuery = true)
List<AutoInitializeParameterView> getNonHiddenAutoInitialisedParametersByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);
```

**Input Parameters:**
- `taskExecutionId` (Long): Task execution instance ID
  - Validation: Cannot be null, must be active task execution
  - Business Context: Runtime parameter auto-initialization
  - Scope: Single task execution context

**Return Type:**
- `List<AutoInitializeParameterView>`: Projection with autoInitializedParameterId, referencedParameterId, orderTree
- Auto-initialization: Parameters that should be automatically populated
- Visibility: Only non-hidden parameters included

**Generated SQL Query:**
```sql
SELECT DISTINCT p.id AS autoInitializedParameterId,
                aip.referenced_parameters_id AS referencedParameterId,
                p.order_tree
FROM parameter_values pv
INNER JOIN parameters p ON p.id = pv.parameters_id
INNER JOIN auto_initialized_parameters aip ON aip.auto_initialized_parameters_id = p.id
INNER JOIN task_executions te ON te.id = pv.task_executions_id
WHERE te.id = ?
  AND p.archived = false
  AND p.hidden = false
  AND p.is_auto_initialized = true
ORDER BY p.order_tree;
```

**Complex Join Analysis:**
- **parameter_values**: Links to task execution
- **parameters**: Main parameter data
- **auto_initialized_parameters**: Auto-initialization mapping table
- **task_executions**: Execution context
- **Multi-table Complexity**: Requires careful index strategy

**Auto-Initialization Process:**
- **Trigger**: Task execution start
- **Dependencies**: Referenced parameters must have values
- **Order**: Processed in orderTree sequence
- **Visibility**: Only visible parameters processed

**Database Execution Plan:**
- **Join Strategy**: Hash joins on foreign keys
- **Performance**: O(log n) per table with proper indexes
- **Index Requirements**: 
  - `parameter_values_task_executions_id_idx`
  - `auto_initialized_parameters_auto_id_idx`
  - `parameters_auto_init_idx (is_auto_initialized, hidden, archived)`

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Consistent view of task execution state
- **Transaction Required**: READ_ONLY transaction

**Business Logic Integration:**
- **Usage**: Task execution auto-initialization process
- **Called From**: TaskExecutionHandler.initializeParameters()
- **Execution Flow**: Critical path in job execution

**DAO Conversion Strategy:**
```java
public List<AutoInitializeParameterView> getNonHiddenAutoInitialisedParametersByTaskExecutionId(
        Long taskExecutionId) {
    
    String sql = """
        SELECT DISTINCT p.id AS autoInitializedParameterId,
                        aip.referenced_parameters_id AS referencedParameterId,
                        p.order_tree
        FROM parameter_values pv
        INNER JOIN parameters p ON p.id = pv.parameters_id
        INNER JOIN auto_initialized_parameters aip ON aip.auto_initialized_parameters_id = p.id
        INNER JOIN task_executions te ON te.id = pv.task_executions_id
        WHERE te.id = ?
          AND p.archived = false
          AND p.hidden = false
          AND p.is_auto_initialized = true
        ORDER BY p.order_tree
        """;
    
    return jdbcTemplate.query(sql, new Object[]{taskExecutionId}, (rs, rowNum) -> {
        AutoInitializeParameterView view = new AutoInitializeParameterView();
        view.setAutoInitializedParameterId(rs.getLong("autoInitializedParameterId"));
        view.setReferencedParameterId(rs.getLong("referencedParameterId"));
        view.setOrderTree(rs.getInt("order_tree"));
        return view;
    });
}
```

**Performance Critical:**
- **Execution Path**: Called during every task execution
- **Response Time**: Must complete within 100ms
- **Index Strategy**: Composite indexes crucial for performance

---

### 17. isParameterUsedInAutoInitialization()

**Method Signature:**
```java
@Query(value = Queries.IS_PARAMETER_USED_IN_AUTOINITIALISATION, nativeQuery = true)
boolean isParameterUsedInAutoInitialization(@Param("parameterId") Long parameterId);
```

**Input Parameters:**
- `parameterId` (Long): Parameter ID to check for auto-initialization usage
  - Validation: Cannot be null
  - Business Context: Dependency check for parameter deletion
  - Usage: Safety validation before parameter operations

**Return Type:**
- `boolean`: true if parameter is used in auto-initialization, false otherwise
- Safety Check: Prevents deletion of parameters used in auto-initialization
- Performance: Optimized existence check

**Generated SQL Query:**
```sql
SELECT EXISTS(
    SELECT 1 
    FROM auto_initialized_parameters 
    WHERE auto_initialized_parameters_id = ? 
       OR referenced_parameters_id = ?
);
```

**Database Execution Plan:**
- **Existence Check**: Stops at first match for performance
- **Dual Role Check**: Parameter can be auto-initialized or referenced
- **Index Access**: Uses indexes on both foreign key columns
- **Performance**: O(log n) with proper indexing

**Auto-Initialization Mapping:**
- **Table**: `auto_initialized_parameters`
- **Relationships**: Many-to-many mapping between parameters
- **auto_initialized_parameters_id**: Parameter that gets auto-initialized
- **referenced_parameters_id**: Parameter whose value is used for initialization

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Snapshot isolation for dependency checking
- **Transaction Required**: None (simple existence check)

**Business Logic Integration:**
- **Usage**: Pre-deletion safety check, dependency validation
- **Called From**: ParameterService.canDeleteParameter()
- **Safety Critical**: Prevents breaking auto-initialization chains

**DAO Conversion Strategy:**
```java
public boolean isParameterUsedInAutoInitialization(Long parameterId) {
    String sql = """
        SELECT EXISTS(
            SELECT 1 
            FROM auto_initialized_parameters 
            WHERE auto_initialized_parameters_id = ? 
               OR referenced_parameters_id = ?
        )
        """;
    
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(sql, Boolean.class, parameterId, parameterId)
    );
}
```

**Index Requirements:**
```sql
CREATE INDEX idx_auto_init_params_auto_id ON auto_initialized_parameters (auto_initialized_parameters_id);
CREATE INDEX idx_auto_init_params_ref_id ON auto_initialized_parameters (referenced_parameters_id);
```

---

### 18. getAllParameterIdsWhereParameterIsUsedInCalculation()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_CALCULATION, nativeQuery = true)
List<IdView> getAllParameterIdsWhereParameterIsUsedInCalculation(@Param("parameterId") String parameterId, 
                                                                @Param("checklistId") Long checklistId);
```

**Input Parameters:**
- `parameterId` (String): Parameter ID used in calculation variables
  - Validation: Cannot be null
  - Business Context: Parameter referenced in calculation formulas
- `checklistId` (Long): Checklist scope for calculation parameters
  - Validation: Cannot be null
  - Performance: Limits search scope for efficiency

**Return Type:**
- `List<IdView>`: Calculation parameters that reference the input parameter
- Dependencies: Critical for calculation integrity
- Formula Impact: Used for formula dependency analysis

**Generated SQL Query:**
```sql
SELECT DISTINCT ? AS id
FROM parameters p,
     jsonb_each(p.data -> 'variables') AS vars(key, value)
WHERE value ->> 'parameterId' = ?
  AND p.type = 'CALCULATION'
  AND p.checklists_id = ?
  AND p.archived = false;
```

**JSONB Structure (calculation variables):**
```json
{
  "data": {
    "variables": {
      "x": {"parameterId": "123", "displayName": "Variable X"},
      "y": {"parameterId": "456", "displayName": "Variable Y"}
    },
    "formula": "x + y * 2"
  }
}
```

**Database Execution Plan:**
- **JSONB Object Expansion**: `jsonb_each()` creates key-value pairs
- **Variable Processing**: Each variable becomes a row for processing
- **Type Filter**: Restricts to CALCULATION parameters only
- **Scope Limitation**: Checklist filter for performance

**Calculation Dependencies:**
- **Formula Variables**: Parameters used as variables in calculations
- **Dependency Chain**: Changes to referenced parameters affect calculations
- **Recalculation**: When referenced parameters change, calculations must update

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Consistent view of calculation dependencies
- **Transaction Required**: None (dependency analysis only)

**Business Logic Integration:**
- **Usage**: Calculation dependency tracking, formula validation
- **Called From**: CalculationService.getParameterDependencies()
- **Formula Engine**: Mathematical expression evaluation system

**DAO Conversion Strategy:**
```java
public List<IdView> getAllParameterIdsWhereParameterIsUsedInCalculation(
        String parameterId, Long checklistId) {
    
    String sql = """
        SELECT DISTINCT p.id AS id
        FROM parameters p,
             jsonb_each(p.data -> 'variables') AS vars(key, value)
        WHERE value ->> 'parameterId' = ?
          AND p.type = 'CALCULATION'
          AND p.checklists_id = ?
          AND p.archived = false
        """;
    
    return jdbcTemplate.query(sql, 
        new Object[]{parameterId, checklistId}, 
        (rs, rowNum) -> {
            IdView view = new IdView();
            view.setId(rs.getLong("id"));
            return view;
        });
}
```

**Performance Considerations:**
- **JSONB Expansion**: Can be expensive for calculations with many variables
- **Index Strategy**: GIN index on data column essential
- **Calculation Caching**: Consider caching calculation dependencies

---

### 19. getAllParametersWhereParameterIsUsedInCreateObjectAutomations()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_CREATE_OBJECT_AUTOMATION_MAPPING, nativeQuery = true)
List<IdView> getAllParametersWhereParameterIsUsedInCreateObjectAutomations(@Param("parameterId") String parameterId);
```

**Input Parameters:**
- `parameterId` (String): Parameter ID used in automation mappings
  - Validation: Cannot be null
  - Business Context: Parameter referenced in object creation automations
  - Usage: Automation dependency tracking

**Return Type:**
- `List<IdView>`: Automation IDs that reference the parameter in object creation
- Dependencies: Critical for automation integrity
- Object Creation: Parameters used in automated object creation workflows

**Generated SQL Query:**
```sql
SELECT a.id
FROM automations a
WHERE jsonb_array_length(a.action_details -> 'configuration') > 0
  AND EXISTS (
      SELECT 1
      FROM jsonb_array_elements(a.action_details -> 'configuration') AS config
      WHERE config ->> 'referencedParameterId' = ?
        OR config -> 'mappings' @> ?::jsonb
  );
```

**JSONB Structure (automation configuration):**
```json
{
  "action_details": {
    "configuration": [
      {
        "objectTypeId": "equipment_uuid",
        "referencedParameterId": "123",
        "mappings": [
          {"sourceParameterId": "123", "targetPropertyId": "property_uuid"}
        ]
      }
    ]
  }
}
```

**Database Execution Plan:**
- **Automation Processing**: Complex JSONB traversal in automations table
- **Configuration Analysis**: Array element expansion and nested object search
- **Existence Check**: EXISTS for performance optimization
- **Cross-table Impact**: Parameter changes affect automation configurations

**Automation Integration:**
- **Object Creation**: Automated creation of external objects
- **Parameter Mapping**: Maps parameter values to object properties
- **Workflow Automation**: Triggered by parameter value changes
- **External Systems**: Integration with ERP, CMMS, other systems

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Cross-system Consistency**: Impacts external system integrations
- **Transaction Required**: None (dependency analysis only)

**Business Logic Integration:**
- **Usage**: Automation dependency validation, parameter impact analysis
- **Called From**: AutomationService.getParameterDependencies()
- **Workflow Management**: Automated business process integration

**DAO Conversion Strategy:**
```java
public List<IdView> getAllParametersWhereParameterIsUsedInCreateObjectAutomations(String parameterId) {
    String sql = """
        SELECT a.id
        FROM automations a
        WHERE jsonb_array_length(a.action_details -> 'configuration') > 0
          AND EXISTS (
              SELECT 1
              FROM jsonb_array_elements(a.action_details -> 'configuration') AS config
              WHERE config ->> 'referencedParameterId' = ?
                OR config -> 'mappings' @> ?::jsonb
          )
        """;
    
    String mappingJson = String.format("[{\"sourceParameterId\":\"%s\"}]", parameterId);
    
    return jdbcTemplate.query(sql, 
        new Object[]{parameterId, mappingJson}, 
        (rs, rowNum) -> {
            IdView view = new IdView();
            view.setId(rs.getLong("id"));
            return view;
        });
}
```

**Performance Impact:**
- **Complex JSONB Query**: Multiple levels of JSONB processing
- **Cross-table Dependencies**: Spans parameters and automations tables
- **Automation Scale**: Performance degrades with automation complexity

---

### 20. getParameterIdWhereParameterIsUsedInLeastCount()

**Method Signature:**
```java
@Query(value = Queries.GET_PARAMETERS_USED_IN_LEAST_COUNT, nativeQuery = true)
List<IdView> getParameterIdWhereParameterIsUsedInLeastCount(@Param("parameterId") String parameterId, 
                                                           @Param("checklistId") Long checklistId);
```

**Input Parameters:**
- `parameterId` (String): Parameter ID used as least count reference
  - Validation: Cannot be null
  - Business Context: Precision/measurement parameter reference
- `checklistId` (Long): Checklist scope for search
  - Validation: Cannot be null
  - Performance: Limits search scope

**Return Type:**
- `List<IdView>`: Parameters that use the input parameter for least count definition
- Precision: Parameters that reference another parameter for measurement precision
- Measurement: Used in numerical parameter precision control

**Generated SQL Query:**
```sql
SELECT DISTINCT ? AS id
FROM parameters p
WHERE p.checklists_id = ?
  AND p.archived = false
  AND p.data ->> 'leastCount' = ?;
```

**JSONB Structure (leastCount):**
```json
{
  "data": {
    "leastCount": "123",  // Referenced parameter ID for precision
    "dataType": "NUMBER",
    "unitOfMeasurement": "mm"
  }
}
```

**Database Execution Plan:**
- **JSONB Text Extraction**: `->>` operator for leastCount field
- **Scope Filter**: Checklist and archived filters
- **Simple Structure**: Single-level JSONB access
- **Performance**: Requires GIN index on data column

**Least Count Concept:**
- **Precision Reference**: Uses another parameter's value for decimal precision
- **Measurement Accuracy**: Defines smallest measurable increment
- **Dynamic Precision**: Precision can change based on referenced parameter
- **Validation**: Ensures measurement accuracy in manufacturing processes

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Consistent precision reference view
- **Transaction Required**: None (dependency lookup only)

**Business Logic Integration:**
- **Usage**: Measurement precision validation, parameter dependency tracking
- **Called From**: MeasurementService.getPrecisionDependencies()
- **Quality Control**: Critical for measurement accuracy in manufacturing

**DAO Conversion Strategy:**
```java
public List<IdView> getParameterIdWhereParameterIsUsedInLeastCount(
        String parameterId, Long checklistId) {
    
    String sql = """
        SELECT DISTINCT p.id AS id
        FROM parameters p
        WHERE p.checklists_id = ?
          AND p.archived = false
          AND p.data ->> 'leastCount' = ?
        """;
    
    return jdbcTemplate.query(sql, 
        new Object[]{checklistId, parameterId}, 
        (rs, rowNum) -> {
            IdView view = new IdView();
            view.setId(rs.getLong("id"));
            return view;
        });
}
```

---

### 21. getParameterIdWhereParameterIsUsedInNumberCriteriaValidation()

**Method Signature:**
```java
@Query(value = Queries.GET_PARAMETERS_USED_IN_NUMBER_CRITERIA_VALIDATION, nativeQuery = true)
List<IdView> getParameterIdWhereParameterIsUsedInNumberCriteriaValidation(@Param("parameterId") String parameterId, 
                                                                         @Param("checklistId") Long checklistId);
```

**Input Parameters:**
- `parameterId` (String): Parameter ID used in number criteria validations
  - Validation: Cannot be null
  - Business Context: Referenced parameter in numerical validation rules
- `checklistId` (Long): Checklist scope for validation search
  - Validation: Cannot be null
  - Performance: Scoped search for efficiency

**Return Type:**
- `List<IdView>`: Parameters with number criteria validations referencing the input parameter
- Validation Dependencies: Critical for numerical validation integrity
- Quality Control: Used in manufacturing quality validation rules

**Generated SQL Query:**
```sql
SELECT DISTINCT ? AS id
FROM parameters p,
     jsonb_array_elements(p.validations) AS validation,
     jsonb_array_elements(validation -> 'criteriaValidations') AS criteriaValidation
WHERE p.checklists_id = ?
  AND p.archived = false
  AND criteriaValidation ->> 'referencedParameterId' = ?;
```

**JSONB Structure (criteriaValidations):**
```json
{
  "validations": [
    {
      "criteriaValidations": [
        {
          "referencedParameterId": "123",
          "operator": "GT",
          "value": "100",
          "toleranceType": "ABSOLUTE",
          "tolerance": "5"
        }
      ]
    }
  ]
}
```

**Database Execution Plan:**
- **Triple Nested Processing**: parameters → validations → criteriaValidations
- **Complex Array Expansion**: Multiple `jsonb_array_elements()` calls
- **Performance Impact**: Most expensive validation dependency query
- **Index Requirements**: Multiple GIN indexes needed

**Validation Logic:**
- **Numerical Criteria**: Comparative validations between parameters
- **Tolerance Handling**: Supports absolute and percentage tolerances
- **Quality Gates**: Manufacturing quality control validation
- **Dynamic Validation**: Validation criteria based on other parameter values

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Consistency**: Complete validation dependency view
- **Transaction Required**: None (validation analysis only)

**Business Logic Integration:**
- **Usage**: Quality control validation, parameter dependency management
- **Called From**: ValidationService.getNumberCriteriaDependencies()
- **Manufacturing**: Critical for production quality assurance

**DAO Conversion Strategy:**
```java
public List<IdView> getParameterIdWhereParameterIsUsedInNumberCriteriaValidation(
        String parameterId, Long checklistId) {
    
    String sql = """
        SELECT DISTINCT p.id AS id
        FROM parameters p,
             jsonb_array_elements(p.validations) AS validation,
             jsonb_array_elements(validation -> 'criteriaValidations') AS criteriaValidation
        WHERE p.checklists_id = ?
          AND p.archived = false
          AND criteriaValidation ->> 'referencedParameterId' = ?
        """;
    
    return jdbcTemplate.query(sql, 
        new Object[]{checklistId, parameterId}, 
        (rs, rowNum) -> {
            IdView view = new IdView();
            view.setId(rs.getLong("id"));
            return view;
        });
}
```

**Performance Critical:**
- **Complex Query**: Monitor execution time carefully
- **Index Strategy**: Consider materialized views for frequently accessed dependencies
- **Query Optimization**: May need query plan optimization for large datasets

---

### 22. increaseOrderTreeByOneAfterParameter()

**Method Signature:**
```java
@Transactional
@Modifying(clearAutomatically = true)
@Query(value = Queries.INCREASE_ORDER_TREE_BY_ONE_AFTER_PARAMETER, nativeQuery = true)
void increaseOrderTreeByOneAfterParameter(@Param("taskId") Long taskId, 
                                         @Param("orderTree") Integer orderTree, 
                                         @Param("newElementId") Long newElementId);
```

**Input Parameters:**
- `taskId` (Long): Task containing parameters to reorder
  - Validation: Cannot be null, must exist
  - Scope: Limits reordering to specific task
- `orderTree` (Integer): Order position threshold
  - Business Logic: Parameters after this position get incremented
- `newElementId` (Long): ID of new parameter being inserted
  - Exclusion: Prevents updating the newly inserted parameter

**Return Type:**
- `void`: No return value
- Side Effects: Updates multiple parameter order_tree values
- Order Management: Maintains sequential ordering after insertion

**Generated SQL Query:**
```sql
UPDATE parameters p
SET order_tree = p.order_tree + 1
WHERE p.tasks_id = ?
  AND p.order_tree > ?
  AND p.id != ?;
```

**Database Execution Plan:**
- **Bulk Update**: Updates multiple parameters in single transaction
- **Range Update**: Affects parameters with order_tree > threshold
- **Exclusion Filter**: Prevents updating newly inserted parameter
- **Performance**: O(m) where m = affected parameters

**Order Tree Management:**
- **Gap Creation**: Creates space for new parameter insertion
- **Sequential Integrity**: Maintains sequential ordering
- **Insertion Process**: 
  1. Increment existing parameters
  2. Insert new parameter at desired position
- **Consistency**: Atomic operation prevents ordering conflicts

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED with row-level locking
- **Transaction Required**: REQUIRED (part of larger insertion transaction)
- **Atomicity**: All order updates succeed or fail together
- **Deadlock Prevention**: Processes parameters in consistent order

**Business Logic Integration:**
- **Usage**: Parameter insertion, order management
- **Called From**: ParameterService.insertParameterAtPosition()
- **UI Integration**: Drag-and-drop parameter reordering

**DAO Conversion Strategy:**
```java
@Transactional(rollbackFor = Exception.class)
public void increaseOrderTreeByOneAfterParameter(Long taskId, Integer orderTree, Long newElementId) {
    String sql = """
        UPDATE parameters 
        SET order_tree = order_tree + 1,
            modified_at = ?,
            modified_by = ?
        WHERE tasks_id = ?
          AND order_tree > ?
          AND id != ?
        """;
    
    int rowsUpdated = jdbcTemplate.update(sql,
        System.currentTimeMillis(),
        getCurrentUserId(), // Assume method exists
        taskId,
        orderTree,
        newElementId);
    
    log.debug("Incremented order_tree for {} parameters in task {}", rowsUpdated, taskId);
}
```

**Order Consistency Validation:**
- **Pre-insertion**: Validate order_tree values are sequential
- **Post-insertion**: Verify no gaps or duplicates in ordering
- **Error Recovery**: Ability to recalculate order_tree if corruption occurs

---

### 23. getAllParametersWhereParameterIsUsedDateAndDateTimeValidations()

**Method Signature:**
```java
@Query(value = Queries.GET_ALL_PARAMETERS_USED_IN_DATE_DATE_TIME_VALIDATIONS, nativeQuery = true)
List<IdView> getAllParametersWhereParameterIsUsedDateAndDateTimeValidations(@Param("parameterId") String parameterId);
```

**Input Parameters:**
- `parameterId` (String): Parameter ID used in date/datetime validations
  - Validation: Cannot be null
  - Business Context: Referenced parameter in temporal validation rules
  - Usage: Date comparison validation dependencies

**Return Type:**
- `List<IdView>`: Parameters with date/datetime validations referencing the input parameter
- Temporal Dependencies: Critical for date-based validation rules
- Scheduling: Used in time-sensitive manufacturing processes

**Generated SQL Query:**
```sql
SELECT DISTINCT CAST(dateTimeValidation ->> 'referencedParameterId' AS bigint) AS id
FROM parameters p,
     jsonb_array_elements(p.validations) AS validation,
     jsonb_array_elements(validation -> 'dateTimeValidations') AS dateTimeValidation
WHERE p.archived = false
  AND dateTimeValidation ->> 'referencedParameterId' IS NOT NULL
  AND dateTimeValidation ->> 'referencedParameterId' = ?;
```

**JSONB Structure (dateTimeValidations):**
```json
{
  "validations": [
    {
      "dateTimeValidations": [
        {
          "referencedParameterId": "123",
          "operator": "AFTER",
          "offsetDays": 7,
          "offsetHours": 0,
          "validationType": "SCHEDULE_CHECK"
        }
      ]
    }
  ]
}
```

**Database Execution Plan:**
- **Triple Nested Array Processing**: Complex JSONB traversal
- **Type Casting**: CAST for referencedParameterId conversion
- **Null Handling**: IS NOT NULL check for data integrity
- **Performance**: Requires comprehensive JSONB indexing

**Temporal Validation Logic:**
- **Date Comparisons**: Before/after date validations
- **Offset Calculations**: Days/hours offset from reference date
- **Schedule Validation**: Manufacturing schedule compliance
- **Time Windows**: Valid time ranges for operations

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Temporal Consistency**: Consistent view of date dependencies
- **Transaction Required**: None (dependency analysis only)

**Business Logic Integration:**
- **Usage**: Temporal validation rule management, scheduling validation
- **Called From**: DateTimeValidationService.getParameterDependencies()
- **Manufacturing**: Production scheduling and timing validation

**DAO Conversion Strategy:**
```java
public List<IdView> getAllParametersWhereParameterIsUsedDateAndDateTimeValidations(String parameterId) {
    String sql = """
        SELECT DISTINCT CAST(dateTimeValidation ->> 'referencedParameterId' AS bigint) AS id
        FROM parameters p,
             jsonb_array_elements(p.validations) AS validation,
             jsonb_array_elements(validation -> 'dateTimeValidations') AS dateTimeValidation
        WHERE p.archived = false
          AND dateTimeValidation ->> 'referencedParameterId' IS NOT NULL
          AND dateTimeValidation ->> 'referencedParameterId' = ?
        """;
    
    return jdbcTemplate.query(sql, new Object[]{parameterId}, (rs, rowNum) -> {
        IdView view = new IdView();
        view.setId(rs.getLong("id"));
        return view;
    });
}
```

**Performance Considerations:**
- **Complex JSONB Query**: Most expensive temporal dependency query
- **Index Requirements**: GIN indexes on validations column essential
- **Query Optimization**: Consider caching for frequently accessed temporal dependencies

---

### 24. bulkInsertIntoParameters() - ParameterRepositoryImpl

**Method Signature:**
```java
public void bulkInsertIntoParameters(List<Parameter> parameters, User user);
```

**Input Parameters:**
- `parameters` (List<Parameter>): List of parameters to insert
  - Validation: Cannot be null, typically 1-1000 parameters
  - Business Context: Batch parameter creation for process templates
  - Performance: Optimized for large parameter sets
- `user` (User): User performing the insertion
  - Audit: Sets created_by and modified_by fields
  - Validation: Cannot be null

**Return Type:**
- `void`: No return value
- Side Effects: Inserts multiple parameter records with JSONB data
- Performance: Significantly faster than individual inserts

**Generated SQL Query:**
```sql
INSERT INTO public.parameters 
(id, archived, order_tree, "data", "label", is_mandatory, "type", 
 created_at, modified_at, created_by, modified_by, tasks_id, description, 
 validations, target_entity_type, checklists_id, is_auto_initialized, 
 auto_initialize, rules, hidden, verification_type, metadata) 
VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?::jsonb);
-- Repeated for each parameter in batch
```

**Batch Processing Details:**
- **PreparedStatement**: Uses BatchPreparedStatementSetter for efficiency
- **JSONB Casting**: Explicit `::jsonb` casting for JSONB columns
- **ID Generation**: Auto-generates IDs using IdGenerator if not provided
- **Audit Fields**: Automatically sets timestamps and user references

**JSONB Handling:**
```java
// JSONB column handling in BatchPreparedStatementSetter
ps.setString(4, param.getData() != null ? param.getData().toString() : null);
ps.setString(14, param.getValidations() != null ? param.getValidations().toString() : null);
ps.setString(18, param.getAutoInitialize() != null ? param.getAutoInitialize().toString() : null);
ps.setString(19, param.getRules() != null ? param.getRules().toString() : null);
ps.setString(22, param.getMetadata() != null ? param.getMetadata().toString() : null);
```

**Database Execution Plan:**
- **Batch Execution**: Single transaction for all inserts
- **JSONB Processing**: PostgreSQL processes JSONB validation and storage
- **Index Updates**: All relevant indexes updated in batch
- **Performance**: O(n) where n = number of parameters

**Transaction Context:**
- **Isolation Level**: READ_COMMITTED
- **Transaction Required**: REQUIRES_NEW (new transaction for bulk operation)
- **Atomicity**: All parameters inserted or none (rollback on failure)
- **Lock Duration**: Minimal lock time due to batch processing

**Performance Optimization:**
- **Batch Size**: Optimal batch size 100-1000 parameters
- **Memory Usage**: Manages memory efficiently with streaming
- **JSONB Optimization**: Pre-validates JSONB before insertion
- **Index Impact**: Batch updates to indexes more efficient than individual

**Business Logic Integration:**
- **Usage**: Process template creation, parameter migration, bulk operations
- **Called From**: ChecklistService.createProcessTemplate(), MigrationService
- **Performance Critical**: Used for large-scale parameter creation

**Error Handling:**
```java
try {
    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        // Implementation details
    });
} catch (DataAccessException e) {
    log.error("Bulk parameter insertion failed", e);
    throw new ParameterBulkInsertException("Failed to insert parameters in batch", e);
}
```

**Validation and Constraints:**
- **Foreign Key Validation**: tasks_id and checklists_id must exist
- **JSONB Validation**: PostgreSQL validates JSONB structure
- **Constraint Checking**: All table constraints checked during insertion
- **Data Integrity**: Maintains referential integrity across all relationships

**Memory Management:**
- **Streaming**: Processes parameters in configurable batch sizes
- **JSONB Serialization**: Efficient JsonNode to String conversion
- **Connection Pooling**: Reuses database connections for efficiency
- **Garbage Collection**: Minimizes object creation during processing

**DAO Implementation Best Practices:**
```java
@Transactional(rollbackFor = Exception.class)
public void bulkInsertIntoParameters(List<Parameter> parameters, User user) {
    if (parameters == null || parameters.isEmpty()) {
        return;
    }
    
    // Process in batches to manage memory
    int batchSize = 1000;
    for (int i = 0; i < parameters.size(); i += batchSize) {
        int end = Math.min(i + batchSize, parameters.size());
        List<Parameter> batch = parameters.subList(i, end);
        
        processBatch(batch, user);
    }
}

private void processBatch(List<Parameter> batch, User user) {
    String sql = """
        INSERT INTO parameters 
        (id, archived, order_tree, data, label, is_mandatory, type, 
         created_at, modified_at, created_by, modified_by, tasks_id, 
         description, validations, target_entity_type, checklists_id, 
         is_auto_initialized, auto_initialize, rules, hidden, 
         verification_type, metadata) 
        VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, 
                ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?::jsonb)
        """;
    
    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            Parameter param = batch.get(i);
            
            // Generate ID if not present
            if (param.getId() == null) {
                param.setId(idGenerator.nextId());
            }
            
            long now = System.currentTimeMillis();
            
            // Set all parameter values with proper JSONB handling
            ps.setLong(1, param.getId());
            ps.setBoolean(2, param.isArchived());
            ps.setInt(3, param.getOrderTree());
            ps.setString(4, toJsonString(param.getData()));
            ps.setString(5, param.getLabel());
            ps.setBoolean(6, param.isMandatory());
            ps.setString(7, param.getType().toString());
            ps.setLong(8, param.getCreatedAt() != null ? param.getCreatedAt() : now);
            ps.setLong(9, param.getModifiedAt() != null ? param.getModifiedAt() : now);
            ps.setLong(10, user.getId());
            ps.setLong(11, user.getId());
            ps.setObject(12, param.getTask() != null ? param.getTask().getId() : null);
            ps.setString(13, param.getDescription());
            ps.setString(14, toJsonString(param.getValidations()));
            ps.setString(15, param.getTargetEntityType().toString());
            ps.setLong(16, param.getChecklistId());
            ps.setBoolean(17, param.isAutoInitialized());
            ps.setString(18, toJsonString(param.getAutoInitialize()));
            ps.setString(19, toJsonString(param.getRules()));
            ps.setBoolean(20, param.isHidden());
            ps.setString(21, param.getVerificationType().toString());
            ps.setString(22, toJsonString(param.getMetadata()));
        }
        
        @Override
        public int getBatchSize() {
            return batch.size();
        }
    });
}

private String toJsonString(JsonNode jsonNode) {
    return jsonNode != null ? jsonNode.toString() : null;
}
```

---

## Summary

The ParameterRepository is the most complex and critical repository in the Streem Digital Work Instructions platform. It handles sophisticated JSONB operations, complex parameter dependencies, and performance-critical bulk operations. Key considerations for Hibernate removal:

### Critical Success Factors:

1. **JSONB Index Strategy**: Comprehensive GIN indexes on all JSONB columns
2. **Dependency Management**: Complex parameter interdependencies must be preserved
3. **Performance Optimization**: Bulk operations and complex queries require careful optimization
4. **Transaction Management**: Proper isolation levels for consistency
5. **Error Handling**: Robust error handling for JSONB operations and batch processing

### Migration Priority:

1. **High Priority**: Core parameter CRUD operations, dependency checking
2. **Medium Priority**: Complex JSONB queries, validation dependencies
3. **Low Priority**: Specialized queries, reporting functions

### Testing Requirements:

1. **JSONB Operations**: Comprehensive testing of all JSONB query patterns
2. **Dependency Integrity**: Validation of all parameter dependency chains
3. **Performance Testing**: Load testing for bulk operations and complex queries
4. **Data Consistency**: Transaction isolation and consistency testing

The successful migration of this repository is critical for the overall system functionality and requires careful planning and extensive testing.