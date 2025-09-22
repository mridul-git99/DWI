package com.leucine.streem.service;

import com.leucine.streem.collections.parser.QRParser;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.QRParserCreateRequest;
import com.leucine.streem.dto.request.QRParserUpdateRequest;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IQRParserService {
  Page<QRParser> getAll(String filters, Pageable pageable);

  QRParser create(QRParserCreateRequest qrParserCreateRequest);

  QRParser update(QRParserUpdateRequest qrParserUpdateRequest, String id) throws StreemException;

  BasicDto archive(String id) throws StreemException;

  BasicDto unArchive(String id) throws StreemException;
}
