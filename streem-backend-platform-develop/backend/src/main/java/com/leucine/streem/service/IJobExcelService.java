package com.leucine.streem.service;

import com.leucine.streem.exception.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Service interface for Job Excel generation functionality.
 * 
 * @businessCategory Job Management
 * @businessDescription Handles Excel export functionality for jobs with comprehensive data including
 *                     job details, checklist properties, and CJF parameters in a flat table format.
 * @functionalRequirement Generate Excel files containing job data with mandatory properties and CJF parameters
 * @performanceRequirement Support large datasets (300k+ jobs) with memory-efficient processing
 * @securityRequirement Enforce facility-based access control for job data export
 */
public interface IJobExcelService {

  /**
   * Generates an Excel file containing job data with mandatory properties and CJF parameters.
   * 
   * @businessDescription Creates a comprehensive Excel export of job data in flat table format
   *                     with one row per job, including all checklist properties and CJF parameters.
   * @functionalBehavior 
   *   - Retrieves jobs based on provided filters with organization/facility access control
   *   - Includes all checklist properties as dynamic columns (mandatory)
   *   - Includes all CJF parameters formatted as multi-line text (mandatory)
   *   - Formats job creator as "FirstName LastName (id: EmployeeId)"
   *   - Calculates pending tasks count for each job
   *   - Uses streaming workbook for large datasets (>10k jobs) for memory efficiency
   *   - Supports object-based filtering with display name resolution in filter details
   * @dataFlow
   *   1. Apply filters and security constraints to retrieve job list
   *   2. Batch fetch related data (properties, CJF, pending tasks) to avoid N+1 queries
   *   3. Generate Excel with fixed columns + dynamic property columns + CJF column
   *   4. Apply professional styling and formatting
   *   5. Return Excel file as byte array
   * @errorHandling Throws ResourceNotFoundException for invalid job references, IOException for Excel generation errors
   * @performanceConsiderations Uses batch queries and streaming workbook for large datasets
   * 
   * @param filters JSON string containing filter criteria for jobs (optional)
   * @param objectId MongoDB ObjectId for entity object filtering (optional)
   * @return ByteArrayInputStream containing the generated Excel file
   * @throws IOException If there's an error generating the Excel file
   * @throws ResourceNotFoundException If required resources are not found
   */
  ByteArrayInputStream generateJobsExcel(String filters, String objectId) throws IOException, ResourceNotFoundException;


}
