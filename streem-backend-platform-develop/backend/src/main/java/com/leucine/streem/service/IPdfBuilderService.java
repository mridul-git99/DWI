package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.GeneratedPdfDataDto;

import java.io.IOException;
import java.util.Map;

public interface IPdfBuilderService {
  String buildSection(Type.PdfType pdfFormat, GeneratedPdfDataDto generatedPdfDataDto) throws JsonProcessingException;
}
