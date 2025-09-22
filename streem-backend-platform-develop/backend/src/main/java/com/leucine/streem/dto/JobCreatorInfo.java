package com.leucine.streem.dto;

import com.leucine.streem.util.Utility;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Internal DTO to hold job creator and timestamp information for batch processing.
 * 
 * @businessCategory Job Management
 * @businessDescription Contains creator and timestamp data for batch processing in Excel generation
 * @technicalDescription Eliminates N+1 queries by pre-fetching creator information in single database call
 */
@Data
@AllArgsConstructor
public class JobCreatorInfo {
    private final String firstName;
    private final String lastName;
    private final String employeeId;
    
    /**
     * Formats creator information as "FirstName LastName (ID: EmployeeId)"
     * 
     * @businessCategory Job Management
     * @businessDescription Provides consistent creator name formatting for Excel export
     * @technicalDescription Pre-formats creator data to eliminate repeated formatting operations
     */
    public String getFormattedCreator() {
        String first = Utility.isEmpty(firstName) ? "" : firstName;
        String last = Utility.isEmpty(lastName) ? "" : lastName;
        String empId = Utility.isEmpty(employeeId) ? "" : employeeId;
        return String.format("%s %s (ID: %s)", first, last, empId);
    }
}
