package com.leucine.streem.service.impl;

import com.leucine.streem.constant.Type;
import com.leucine.streem.exception.UnsupportedReportTypeException;
import com.leucine.streem.service.IPdfReportBuilder;
import com.leucine.streem.service.IPdfReportBuilderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory implementation for creating PDF report builders
 * Uses Spring's dependency injection to automatically discover all available builders
 */
@Component
public class PdfReportBuilderFactoryImpl implements IPdfReportBuilderFactory {
    
    private final Map<Type.PdfType, IPdfReportBuilder> reportBuilders;
    
    /**
     * Constructor that automatically maps all available report builders by their supported types
     * @param builderList List of all available report builders (injected by Spring)
     */
    public PdfReportBuilderFactoryImpl(List<IPdfReportBuilder> builderList) {
        this.reportBuilders = builderList.stream()
            .collect(Collectors.toMap(
                IPdfReportBuilder::getSupportedReportType,
                Function.identity()
            ));
    }
    
    @Override
    public IPdfReportBuilder getReportBuilder(Type.PdfType pdfType) {
        IPdfReportBuilder builder = reportBuilders.get(pdfType);
        if (builder == null) {
            throw new UnsupportedReportTypeException("No builder found for PDF type: " + pdfType);
        }
        return builder;
    }
    
    @Override
    public boolean supportsReportType(Type.PdfType pdfType) {
        return reportBuilders.containsKey(pdfType);
    }
    
    @Override
    public Set<Type.PdfType> getSupportedReportTypes() {
        return Collections.unmodifiableSet(reportBuilders.keySet());
    }
}
