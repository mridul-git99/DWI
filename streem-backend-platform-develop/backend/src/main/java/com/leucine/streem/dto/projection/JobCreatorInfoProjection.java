package com.leucine.streem.dto.projection;

/**
 * Projection view for job creator information in Excel generation.
 * 
 * @businessCategory Job Management
 * @businessDescription Provides optimized data structure for job creator info in Excel export
 * @technicalDescription Eliminates Object[] casting and provides type-safe creator access
 */
public interface JobCreatorInfoProjection {
    
    /**
     * Gets the job ID.
     * 
     * @businessCategory Job Management
     * @businessDescription Unique identifier for the job
     * @technicalDescription Maps to jobs.id
     */
    Long getJobId();
    
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
