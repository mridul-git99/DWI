# PDF Generation System - Complete JavaDoc Documentation

## Architecture Overview

The PDF Generation System is a comprehensive, modular framework designed to generate various types of PDF reports from structured data. The system follows enterprise design patterns and provides extensible architecture for different report types.

### Core Design Patterns

1. **Factory Pattern**: `IPdfReportBuilderFactory` creates appropriate report builders
2. **Strategy Pattern**: Different `IPdfReportBuilder` implementations for various report types
3. **Template Method Pattern**: Common PDF generation flow with customizable report building
4. **Builder Pattern**: HTML content construction using helper classes

### Technology Stack

- **PDF Generation**: iText 7 (`com.itextpdf:html2pdf:6.1.0`, `com.itextpdf:itext7-core:8.0.4`)
- **Template Engine**: Custom HTML template processing
- **Styling**: CSS-based styling with responsive design
- **Data Processing**: Jackson for JSON processing, Spring Framework integration

### System Flow

```
Data Input → Report Builder Factory → Specific Report Builder → HTML Template Engine → PDF Generator → PDF Output
```

## Core Components

### 1. Factory Layer
- `IPdfReportBuilderFactory`: Factory interface for creating report builders
- `PdfReportBuilderFactoryImpl`: Concrete factory implementation

### 2. Builder Layer
- `IPdfReportBuilder`: Common interface for all report builders
- `JobReportBuilder`: Job execution reports
- `JobAuditReportBuilder`: Job audit trail reports
- `JobLogReportBuilder`: Job activity log reports
- `ObjectAuditLogReportBuilder`: Object change audit reports

### 3. Service Layer
- `IPdfBuilderService`: Main orchestration service
- `PdfBuilderService`: Core PDF building implementation
- `PdfGeneratorUtil`: PDF generation utilities

### 4. Helper Layer
- `PdfBuilderServiceHelpers`: Utility methods and HTML templates
- `HtmlTemplateEngine`: HTML generation utilities
- `CssClasses`: CSS class constants

### 5. Data Layer
- `GeneratedPdfDataDto`: Main data transfer object
- `JobAuditPdfDataDto`: Job audit specific data
- Various supporting DTOs

## Supported Report Types

1. **JOB_REPORT**: Complete job execution reports with tasks, parameters, and results
2. **JOB_AUDIT**: Audit trail reports showing job history and changes
3. **JOB_LOGS**: Activity log reports with detailed job execution logs
4. **OBJECT_AUDIT_LOGS**: Object change history reports

## Key Features

- **Modular Architecture**: Easy to extend with new report types
- **Template-Based**: HTML templates for consistent styling
- **Type-Safe**: Enum-based report type management
- **Responsive Design**: CSS-based responsive layouts
- **Rich Content**: Support for tables, images, charts, and complex layouts
- **Internationalization**: Timezone and date format support
- **Error Handling**: Comprehensive exception handling
- **Performance Optimized**: Efficient HTML to PDF conversion

## Usage Examples

### Basic PDF Generation
```java
@Autowired
private PdfGeneratorUtil pdfGeneratorUtil;

// Generate a job report PDF
GeneratedPdfDataDto data = new GeneratedPdfDataDto();
// ... populate data
byte[] pdfBytes = pdfGeneratorUtil.generatePdf(Type.PdfType.JOB_REPORT, data);
```

### Custom Report Builder
```java
@Component
public class CustomReportBuilder implements IPdfReportBuilder {
    @Override
    public String buildReport(GeneratedPdfDataDto data) throws JsonProcessingException {
        // Custom report building logic
        return htmlContent;
    }
    
    @Override
    public Type.PdfType getSupportedReportType() {
        return Type.PdfType.CUSTOM_REPORT;
    }
}
```

## Configuration

### Dependencies
```gradle
implementation 'com.itextpdf:html2pdf:6.1.0'
implementation 'com.itextpdf:itext7-core:8.0.4'
implementation 'com.itextpdf.tool:xmlworker:5.5.13.3'
```

### Template Configuration
Templates are located in `src/main/resources/templates/`:
- `job-pdf-report.html`: Job report template
- `job-audit-report.html`: Job audit template
- `job-logs-report.html`: Job logs template
- `object-audit-logs-report.html`: Object audit template

## Best Practices

1. **Data Preparation**: Ensure all required data is populated before PDF generation
2. **Error Handling**: Always wrap PDF generation in try-catch blocks
3. **Memory Management**: Use streaming for large reports
4. **Template Design**: Keep templates modular and reusable
5. **Performance**: Cache frequently used data and templates
6. **Testing**: Test with various data scenarios and edge cases

## Extension Points

### Adding New Report Types
1. Create new enum value in `Type.PdfType`
2. Implement `IPdfReportBuilder` interface
3. Register builder in factory
4. Create corresponding HTML template
5. Add CSS classes if needed

### Custom HTML Templates
1. Create new template in resources/templates
2. Use placeholder syntax: `{{VARIABLE_NAME}}`
3. Include CSS classes from `CssClasses`
4. Test with various data scenarios

## Troubleshooting

### Common Issues
1. **Unicode Characters**: Use HTML entities or ensure font support
2. **Large Tables**: Implement pagination for better performance
3. **Memory Issues**: Use streaming for large datasets
4. **Template Errors**: Validate HTML syntax and placeholder names
5. **CSS Issues**: Test styles across different PDF viewers

### Performance Optimization
1. **Template Caching**: Cache compiled templates
2. **Data Optimization**: Minimize data processing in templates
3. **Image Optimization**: Compress images before embedding
4. **Pagination**: Implement smart page breaks
5. **Streaming**: Use streaming for large reports

## Security Considerations

1. **Input Validation**: Validate all input data
2. **HTML Sanitization**: Sanitize user-generated content
3. **Access Control**: Implement proper authorization
4. **Resource Limits**: Set limits on report size and generation time
5. **Audit Logging**: Log PDF generation activities

---

*This documentation covers the complete PDF generation system architecture, implementation details, and usage guidelines.*
