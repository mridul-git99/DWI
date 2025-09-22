package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.GeneratedPdfDataDto;
import com.leucine.streem.service.IPdfBuilderService;
import com.leucine.streem.service.IPdfReportBuilder;
import com.leucine.streem.service.IPdfReportBuilderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Main PDF Builder Service that acts as a facade for different PDF report builders
 * Uses the Factory pattern to delegate to appropriate report builders
 */
@Service
@RequiredArgsConstructor
public class PdfBuilderService implements IPdfBuilderService {
  
  private final IPdfReportBuilderFactory reportBuilderFactory;

  @Override
  public String buildSection(Type.PdfType pdfFormat, GeneratedPdfDataDto generatedPdfDataDto) throws JsonProcessingException {
    IPdfReportBuilder reportBuilder = reportBuilderFactory.getReportBuilder(pdfFormat);
    return reportBuilder.buildReport(generatedPdfDataDto);
  }
}
