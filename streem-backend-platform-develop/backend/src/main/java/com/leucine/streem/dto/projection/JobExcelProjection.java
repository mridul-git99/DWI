package com.leucine.streem.dto.projection;

/**
 * Projection view for job data in Excel generation with filters.
 * 
 * @businessCategory Job Management
 * @businessDescription Provides optimized data structure for job information in Excel export
 * @technicalDescription Eliminates Object[] casting and provides type-safe job data access
 */
public interface JobExcelProjection {
    
    /**
     * Gets the job ID.
     * 
     * @businessCategory Job Management
     * @businessDescription Unique identifier for the job
     * @technicalDescription Maps to jobs.id
     */
    Long getId();
    
    /**
     * Gets the job code.
     * 
     * @businessCategory Job Management
     * @businessDescription Job code for identification
     * @technicalDescription Maps to jobs.code
     */
    String getCode();
    
    /**
     * Gets the job state.
     * 
     * @businessCategory Job Management
     * @businessDescription Current state of the job
     * @technicalDescription Maps to jobs.state
     */
    String getState();
    
    /**
     * Gets the job creation timestamp.
     * 
     * @businessCategory Job Management
     * @businessDescription When the job was created
     * @technicalDescription Maps to jobs.created_at
     */
    Long getCreatedAt();
    
    /**
     * Gets the checklist ID.
     * 
     * @businessCategory Job Management
     * @businessDescription Associated checklist identifier
     * @technicalDescription Maps to jobs.checklists_id
     */
    Long getChecklistsId();
    
    /**
     * Gets the checklist code.
     * 
     * @businessCategory Job Management
     * @businessDescription Associated checklist code
     * @technicalDescription Maps to checklists.code as checklist_code
     */
    String getChecklistCode();
    
    /**
     * Gets the checklist name.
     * 
     * @businessCategory Job Management
     * @businessDescription Associated checklist name
     * @technicalDescription Maps to checklists.name as checklist_name
     */
    String getChecklistName();
    
    /**
     * Gets the creator's first name.
     * 
     * @businessCategory Job Management
     * @businessDescription Creator's first name for Excel display
     * @technicalDescription Maps to users.first_name
     */
    String getFirstName();
    
    /**
     * Gets the creator's last name.
     * 
     * @businessCategory Job Management
     * @businessDescription Creator's last name for Excel display
     * @technicalDescription Maps to users.last_name
     */
    String getLastName();
    
    /**
     * Gets the creator's employee ID.
     * 
     * @businessCategory Job Management
     * @businessDescription Creator's employee ID for identification
     * @technicalDescription Maps to users.employee_id
     */
    String getEmployeeId();
}
