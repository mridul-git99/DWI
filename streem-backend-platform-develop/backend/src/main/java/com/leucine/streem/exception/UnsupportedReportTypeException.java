package com.leucine.streem.exception;

/**
 * Exception thrown when an unsupported PDF report type is requested.
 * 
 * <p>This exception is part of the PDF Generation System's robust error handling mechanism,
 * ensuring that only valid and supported report types are processed. This contributes to
 * system reliability and provides clear feedback when invalid requests are made.</p>
 * 
 * <p>This exception prevents system crashes and provides clear error messages when 
 * unsupported report types are requested, improving user experience and reducing support 
 * tickets. It ensures that only validated report types are processed, maintaining data 
 * integrity and system stability.</p>
 * 
 * <p><strong>Usage Example:</strong><br>
 * When a user or system attempts to generate a PDF report with an invalid or unsupported 
 * report type (e.g., requesting a "CUSTOM_REPORT" type that doesn't exist), this exception 
 * is thrown with a descriptive message indicating the issue.</p>
 * 
 * <p><strong>Performance Impact:</strong><br>
 * Minimal performance impact - exception creation is lightweight and only occurs during 
 * error conditions. Prevents expensive processing of invalid requests.</p>
 * 
 * <p><strong>Design Pattern:</strong><br>
 * Follows the standard Java exception pattern with message and cause constructors. 
 * Extends RuntimeException for unchecked exception behavior, allowing callers to handle 
 * or propagate as appropriate.</p>
 * 
 * @since 1.0
 * @author PDF Generation System Team
 */
public class UnsupportedReportTypeException extends RuntimeException {
    
    /**
     * Constructs a new UnsupportedReportTypeException with the specified detail message.
     * 
     * <p>Use this constructor when you have a descriptive message about the unsupported 
     * report type, such as "Report type 'INVALID_TYPE' is not supported. Supported types 
     * are: JOB_REPORT, AUDIT_REPORT, LOG_REPORT"</p>
     * 
     * @param message the detail message explaining which report type was unsupported
     *                and why the request failed
     */
    public UnsupportedReportTypeException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new UnsupportedReportTypeException with the specified detail message
     * and cause.
     * 
     * <p>Use this constructor when the unsupported report type exception is caused by 
     * another exception, such as a configuration loading error or validation failure.
     * This preserves the full error context, enabling better debugging and root cause 
     * analysis when report generation fails.</p>
     * 
     * @param message the detail message explaining the unsupported report type
     * @param cause the cause of this exception (which is saved for later retrieval
     *              by the {@link #getCause()} method)
     */
    public UnsupportedReportTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
