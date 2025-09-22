package com.leucine.streem.service;

import com.leucine.streem.constant.Type;
import java.util.Set;

/**
 * Factory interface for creating PDF report builders based on report type.
 * 
 * <p>This interface implements the <strong>Factory Pattern</strong> to provide a centralized
 * mechanism for creating appropriate {@link IPdfReportBuilder} instances based on the
 * requested {@link Type.PdfType}. The factory abstracts the complexity of builder selection
 * and instantiation from client code.</p>
 * 
 * <h3>Design Pattern</h3>
 * <p>This interface follows the <strong>Abstract Factory Pattern</strong>, providing an
 * interface for creating families of related objects (PDF report builders) without
 * specifying their concrete classes.</p>
 * 
 * <h3>Architecture Benefits</h3>
 * <ul>
 *   <li><strong>Loose Coupling</strong>: Clients depend on the factory interface, not concrete builders</li>
 *   <li><strong>Extensibility</strong>: New report types can be added without modifying existing code</li>
 *   <li><strong>Type Safety</strong>: Compile-time checking of supported report types</li>
 *   <li><strong>Single Responsibility</strong>: Each builder handles only one report type</li>
 *   <li><strong>Centralized Management</strong>: All builder creation logic is centralized</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @Service
 * public class PdfGenerationService {
 *     
 *     @Autowired
 *     private IPdfReportBuilderFactory builderFactory;
 *     
 *     public byte[] generateReport(Type.PdfType reportType, GeneratedPdfDataDto data) {
 *         // Factory selects appropriate builder
 *         IPdfReportBuilder builder = builderFactory.getReportBuilder(reportType);
 *         
 *         // Generate HTML content
 *         String htmlContent = builder.buildReport(data);
 *         
 *         // Convert to PDF
 *         return convertToPdf(htmlContent);
 *     }
 *     
 *     public boolean canGenerateReport(Type.PdfType reportType) {
 *         return builderFactory.supportsReportType(reportType);
 *     }
 * }
 * }</pre>
 * 
 * <h3>Implementation Guidelines</h3>
 * <ul>
 *   <li>Maintain a registry of all available {@link IPdfReportBuilder} implementations</li>
 *   <li>Use dependency injection to automatically discover builder implementations</li>
 *   <li>Provide fast lookup mechanisms for builder selection</li>
 *   <li>Handle unsupported report types gracefully with appropriate exceptions</li>
 *   <li>Cache builder instances for performance optimization</li>
 *   <li>Support runtime discovery of new builder implementations</li>
 * </ul>
 * 
 * <h3>Supported Report Types</h3>
 * <p>The factory should support all report types defined in {@link Type.PdfType}:</p>
 * <ul>
 *   <li>{@link Type.PdfType#JOB_REPORT} - Complete job execution reports with tasks and parameters</li>
 *   <li>{@link Type.PdfType#JOB_AUDIT} - Job audit trail reports showing history and changes</li>
 *   <li>{@link Type.PdfType#JOB_LOGS} - Job activity log reports with detailed execution logs</li>
 *   <li>{@link Type.PdfType#OBJECT_AUDIT_LOGS} - Object change audit reports for entity modifications</li>
 * </ul>
 * 
 * <h3>Error Handling</h3>
 * <p>The factory should handle various error scenarios:</p>
 * <ul>
 *   <li>Unsupported report types should throw {@link com.leucine.streem.exception.UnsupportedReportTypeException}</li>
 *   <li>Missing builder implementations should be detected at startup</li>
 *   <li>Duplicate builders for the same type should be flagged as configuration errors</li>
 *   <li>Builder instantiation failures should be properly logged and handled</li>
 * </ul>
 * 
 * @author Leucine Team
 * @version 1.0
 * @since 1.0
 * @see IPdfReportBuilder
 * @see Type.PdfType
 * @see com.leucine.streem.exception.UnsupportedReportTypeException
 */
public interface IPdfReportBuilderFactory {
    
    /**
     * Creates and returns a PDF report builder for the specified report type.
     * 
     * <p>This method is the core of the factory pattern implementation. It selects
     * the appropriate {@link IPdfReportBuilder} implementation based on the requested
     * {@link Type.PdfType} and returns a ready-to-use builder instance.</p>
     * 
     * <h4>Builder Selection Process:</h4>
     * <ol>
     *   <li>Validate the input report type is not null</li>
     *   <li>Look up the appropriate builder from the internal registry</li>
     *   <li>Return the builder instance (may be cached or newly created)</li>
     *   <li>Throw exception if no suitable builder is found</li>
     * </ol>
     * 
     * <h4>Performance Considerations:</h4>
     * <ul>
     *   <li>Builder lookup should be O(1) complexity using hash-based lookups</li>
     *   <li>Consider caching builder instances to avoid repeated instantiation</li>
     *   <li>Use lazy initialization for builders that are rarely used</li>
     *   <li>Minimize reflection usage for better performance</li>
     * </ul>
     * 
     * <h4>Thread Safety:</h4>
     * <p>This method should be thread-safe and support concurrent access from
     * multiple threads without synchronization issues.</p>
     * 
     * <h4>Usage Example:</h4>
     * <pre>{@code
     * // Get builder for job reports
     * IPdfReportBuilder jobBuilder = factory.getReportBuilder(Type.PdfType.JOB_REPORT);
     * String jobReportHtml = jobBuilder.buildReport(jobData);
     * 
     * // Get builder for audit reports
     * IPdfReportBuilder auditBuilder = factory.getReportBuilder(Type.PdfType.JOB_AUDIT);
     * String auditReportHtml = auditBuilder.buildReport(auditData);
     * }</pre>
     * 
     * @param pdfType the type of PDF report for which a builder is needed.
     *                Must be a valid {@link Type.PdfType} enum value and not null.
     *                The type determines which specific builder implementation will be returned.
     * @return a {@link IPdfReportBuilder} instance capable of generating reports of the
     *         specified type. Never returns null - if no suitable builder is found,
     *         an exception is thrown instead.
     * @throws com.leucine.streem.exception.UnsupportedReportTypeException if no builder
     *         implementation is available for the specified report type, or if the
     *         report type is not supported by the current system configuration
     * @throws IllegalArgumentException if the pdfType parameter is null
     * @see IPdfReportBuilder
     * @see Type.PdfType
     * @see #supportsReportType(Type.PdfType)
     */
    IPdfReportBuilder getReportBuilder(Type.PdfType pdfType);
    
    /**
     * Checks whether the factory supports creating builders for the specified report type.
     * 
     * <p>This method provides a way to check report type support without triggering
     * exceptions. It's useful for conditional logic, validation, and providing user
     * feedback about available report types.</p>
     * 
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>Validating user input before attempting report generation</li>
     *   <li>Building dynamic UI menus showing available report types</li>
     *   <li>Implementing fallback logic for unsupported types</li>
     *   <li>Configuration validation during application startup</li>
     *   <li>API endpoint validation for report generation requests</li>
     * </ul>
     * 
     * <h4>Performance:</h4>
     * <p>This method should be very fast (O(1)) as it's likely to be called
     * frequently for validation purposes. It should not trigger any expensive
     * operations like builder instantiation.</p>
     * 
     * <h4>Consistency:</h4>
     * <p>The result of this method should be consistent with {@link #getReportBuilder(Type.PdfType)}.
     * If this method returns {@code true} for a given type, then {@code getReportBuilder}
     * should successfully return a builder for that type.</p>
     * 
     * <h4>Usage Example:</h4>
     * <pre>{@code
     * // Check support before attempting generation
     * if (factory.supportsReportType(Type.PdfType.JOB_REPORT)) {
     *     IPdfReportBuilder builder = factory.getReportBuilder(Type.PdfType.JOB_REPORT);
     *     // ... generate report
     * } else {
     *     throw new UnsupportedOperationException("Job reports are not available");
     * }
     * 
     * // Build list of available report types
     * List<Type.PdfType> availableTypes = Arrays.stream(Type.PdfType.values())
     *     .filter(factory::supportsReportType)
     *     .collect(Collectors.toList());
     * }</pre>
     * 
     * @param pdfType the report type to check for support. If null, this method
     *                should return {@code false} rather than throwing an exception.
     * @return {@code true} if the factory can create a builder for the specified
     *         report type, {@code false} otherwise. Returns {@code false} for null input.
     * @see #getReportBuilder(Type.PdfType)
     * @see #getSupportedReportTypes()
     */
    boolean supportsReportType(Type.PdfType pdfType);
    
    /**
     * Returns a set of all PDF report types that this factory can handle.
     * 
     * <p>This method provides introspection capabilities, allowing clients to
     * discover all available report types at runtime. This is particularly useful
     * for building dynamic user interfaces, configuration validation, and
     * system monitoring.</p>
     * 
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>Building dynamic menus or dropdowns for report selection</li>
     *   <li>System health checks and configuration validation</li>
     *   <li>API documentation generation showing available endpoints</li>
     *   <li>Administrative interfaces showing system capabilities</li>
     *   <li>Integration testing to verify all expected types are supported</li>
     * </ul>
     * 
     * <h4>Return Value Characteristics:</h4>
     * <ul>
     *   <li>Never returns null - returns empty set if no types are supported</li>
     *   <li>Returns an immutable set to prevent external modification</li>
     *   <li>Set contents should be consistent with {@link #supportsReportType(Type.PdfType)}</li>
     *   <li>May be cached for performance if the supported types don't change at runtime</li>
     * </ul>
     * 
     * <h4>Performance Considerations:</h4>
     * <ul>
     *   <li>Consider caching the result if supported types are static</li>
     *   <li>Use efficient set implementations for large numbers of types</li>
     *   <li>Avoid expensive operations like reflection in this method</li>
     * </ul>
     * 
     * <h4>Usage Example:</h4>
     * <pre>{@code
     * // Get all supported types for UI display
     * Set<Type.PdfType> supportedTypes = factory.getSupportedReportTypes();
     * 
     * // Create dropdown options
     * List<SelectOption> options = supportedTypes.stream()
     *     .map(type -> new SelectOption(type.name(), type.getDisplayName()))
     *     .collect(Collectors.toList());
     * 
     * // Validate configuration
     * Set<Type.PdfType> requiredTypes = Set.of(
     *     Type.PdfType.JOB_REPORT, 
     *     Type.PdfType.JOB_AUDIT
     * );
     * 
     * if (!supportedTypes.containsAll(requiredTypes)) {
     *     throw new ConfigurationException("Missing required report builders");
     * }
     * }</pre>
     * 
     * @return an immutable set containing all {@link Type.PdfType} values that this
     *         factory can handle. Never returns null. May be empty if no report types
     *         are currently supported (though this would indicate a configuration issue).
     * @see Type.PdfType
     * @see #supportsReportType(Type.PdfType)
     */
    Set<Type.PdfType> getSupportedReportTypes();
}
