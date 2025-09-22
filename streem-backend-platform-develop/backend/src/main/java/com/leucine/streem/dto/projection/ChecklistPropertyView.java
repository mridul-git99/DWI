package com.leucine.streem.dto.projection;

/**
 * Projection view for checklist property data in Excel generation.
 * 
 * @businessCategory Job Management
 * @businessDescription Provides optimized data structure for checklist properties in Excel export
 * @technicalDescription Eliminates Object[] casting and provides type-safe property access
 */
public interface ChecklistPropertyView {
    
    /**
     * Gets the checklist ID.
     * 
     * @businessCategory Job Management
     * @businessDescription Unique identifier for the checklist
     * @technicalDescription Maps to checklist_property_values.checklists_id
     */
    Long getChecklistId();
    
    /**
     * Gets the property label.
     * 
     * @businessCategory Job Management  
     * @businessDescription Human-readable property name for Excel column headers
     * @technicalDescription Maps to facility_use_case_property_mappings.label_alias
     */
    String getPropertyLabel();
    
    /**
     * Gets the property value.
     * 
     * @businessCategory Job Management
     * @businessDescription Actual property value for Excel cell content
     * @technicalDescription Maps to checklist_property_values.value
     */
    String getPropertyValue();
}
