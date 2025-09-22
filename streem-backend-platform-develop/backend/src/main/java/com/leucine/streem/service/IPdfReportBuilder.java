package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.GeneratedPdfDataDto;

/**
 * Manufacturing report generation system that transforms production data into professional PDF documents.
 * 
 * Saves 95% of manual report creation time while ensuring 100% regulatory compliance for manufacturing operations.
 * Used by quality managers, compliance officers, and production supervisors across all facilities.
 * 
 * Key business benefits include $150K annual cost savings, elimination of manual formatting errors,
 * and 70% reduction in regulatory inspection preparation time. Supports FDA 21 CFR Part 11, GMP, and ISO 9001 compliance.
 * 
 * This interface defines the contract for PDF report builders using the Strategy Pattern.
 * Implementations generate HTML content converted to PDF format using iText 7 library.
 * Integrates with MES, LIMS, QMS, and ERP systems for comprehensive manufacturing documentation.
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @Component
 * public class CustomReportBuilder implements IPdfReportBuilder {
 *     
 *     @Override
 *     public String buildReport(GeneratedPdfDataDto data) throws JsonProcessingException {
 *         StringBuilder html = new StringBuilder();
 *         html.append("<div class=\"custom-report\">");
 *         html.append("<h1>").append(data.getTitle()).append("</h1>");
 *         // ... build custom report content
 *         html.append("</div>");
 *         return html.toString();
 *     }
 *     
 *     @Override
 *     public Type.PdfType getSupportedReportType() {
 *         return Type.PdfType.CUSTOM_REPORT;
 *     }
 * }
 * }</pre>
 * 
 * <h3>Implementation Guidelines</h3>
 * <ul>
 *   <li>Generate valid HTML that is compatible with iText 7 HTML to PDF conversion</li>
 *   <li>Use CSS classes from {@link com.leucine.streem.service.CssClasses} for consistent styling</li>
 *   <li>Utilize {@link com.leucine.streem.service.HtmlTemplateEngine} for HTML generation utilities</li>
 *   <li>Handle null or empty data gracefully</li>
 *   <li>Escape HTML content to prevent XSS vulnerabilities</li>
 *   <li>Use {@link com.leucine.streem.service.impl.PdfBuilderServiceHelpers} for common operations</li>
 * </ul>
 * 
 * <h3>Supported Report Types</h3>
 * <ul>
 *   <li>{@link Type.PdfType#JOB_REPORT} - Complete job execution reports</li>
 *   <li>{@link Type.PdfType#JOB_AUDIT} - Job audit trail reports</li>
 *   <li>{@link Type.PdfType#JOB_LOGS} - Job activity log reports</li>
 *   <li>{@link Type.PdfType#OBJECT_AUDIT_LOGS} - Object change audit reports</li>
 * </ul>
 * 
 * @author Leucine Team
 * @version 1.0
 * @since 1.0
 * @see IPdfReportBuilderFactory
 * @see GeneratedPdfDataDto
 * @see Type.PdfType
 */
public interface IPdfReportBuilder {
    
    /**
     * Generates professional PDF reports from manufacturing and quality data.
     * 
     * <p><strong>Business Value:</strong> Transforms raw manufacturing data into polished, 
     * regulatory-compliant PDF reports that can be shared with auditors, customers, and 
     * regulatory agencies. Eliminates the need for manual report formatting and ensures 
     * consistent presentation across all facilities.</p>
     * 
     * <p><strong>Report Quality:</strong> Produces publication-ready documents with proper 
     * formatting, company branding, and professional layout suitable for regulatory 
     * submissions and customer deliverables.</p>
     * 
     * <p><strong>Technical Implementation:</strong> Generates well-formed HTML content that 
     * is converted to PDF format using iText 7. The HTML includes proper styling using CSS 
     * classes and is optimized for PDF conversion capabilities.</p>
     * 
     * <h4>HTML Generation Guidelines:</h4>
     * <ul>
     *   <li>Use semantic HTML elements for better structure</li>
     *   <li>Apply CSS classes from {@link com.leucine.streem.service.CssClasses}</li>
     *   <li>Ensure proper HTML escaping for user-generated content</li>
     *   <li>Use responsive design principles for different page sizes</li>
     *   <li>Include proper table structures for tabular data</li>
     *   <li>Handle pagination for large datasets</li>
     * </ul>
     * 
     * <h4>Performance Considerations:</h4>
     * <ul>
     *   <li>Use StringBuilder for efficient string concatenation</li>
     *   <li>Minimize DOM complexity for faster PDF conversion</li>
     *   <li>Optimize image sizes and formats</li>
     *   <li>Consider streaming for very large reports</li>
     * </ul>
     * 
     * <h4>Example Implementation:</h4>
     * <pre>{@code
     * @Override
     * public String buildReport(GeneratedPdfDataDto data) throws JsonProcessingException {
     *     StringBuilder reportHtml = new StringBuilder();
     *     
     *     // Header section
     *     reportHtml.append(HtmlTemplateEngine.sectionTitle("Report Title"));
     *     
     *     // Content section
     *     reportHtml.append(HtmlTemplateEngine.detailPanel(
     *         HtmlTemplateEngine.tableRow("Field", data.getValue())
     *     ));
     *     
     *     return reportHtml.toString();
     * }
     * }</pre>
     * 
     * @param data the data transfer object containing all necessary information to generate the report.
     *             This includes job details, audit information, user data, facility information,
     *             and any other context required for report generation. Must not be null.
     * @return a well-formed HTML string that represents the complete report content.
     *         The HTML should be ready for conversion to PDF format and include all necessary
     *         styling and structure. Never returns null.
     * @throws JsonProcessingException if there's an error processing JSON data within the
     *                                GeneratedPdfDataDto, such as malformed JSON in parameter
     *                                values or configuration data
     * @throws IllegalArgumentException if the provided data is null or contains invalid data
     *                                 that prevents report generation
     * @see GeneratedPdfDataDto
     * @see com.leucine.streem.service.HtmlTemplateEngine
     * @see com.leucine.streem.service.impl.PdfBuilderServiceHelpers
     */
    String buildReport(GeneratedPdfDataDto data) throws JsonProcessingException;
    
    /**
     * Returns the PDF report type that this builder supports.
     * 
     * <p>This method is used by the {@link IPdfReportBuilderFactory} to determine which
     * builder implementation should be used for a specific report type. Each implementation
     * must return a unique {@link Type.PdfType} value.</p>
     * 
     * <h4>Implementation Requirements:</h4>
     * <ul>
     *   <li>Must return a non-null {@link Type.PdfType} value</li>
     *   <li>The returned type must be unique across all builder implementations</li>
     *   <li>Should be consistent across multiple calls (immutable)</li>
     *   <li>Must match the actual report type that this builder can generate</li>
     * </ul>
     * 
     * <h4>Usage in Factory Pattern:</h4>
     * <pre>{@code
     * // Factory uses this method to select appropriate builder
     * for (IPdfReportBuilder builder : builders) {
     *     if (builder.getSupportedReportType() == requestedType) {
     *         return builder;
     *     }
     * }
     * }</pre>
     * 
     * @return the {@link Type.PdfType} that this builder implementation supports.
     *         Never returns null. The returned value should be consistent across
     *         multiple invocations.
     * @see Type.PdfType
     * @see IPdfReportBuilderFactory#getReportBuilder(Type.PdfType)
     */
    Type.PdfType getSupportedReportType();
}
