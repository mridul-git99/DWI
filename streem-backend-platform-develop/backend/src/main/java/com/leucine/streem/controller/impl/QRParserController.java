package com.leucine.streem.controller.impl;

import com.leucine.streem.collections.parser.QRParser;
import com.leucine.streem.controller.IQRParserController;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.QRParserCreateRequest;
import com.leucine.streem.dto.request.QRParserUpdateRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.IQRParserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class QRParserController implements IQRParserController {
  private final IQRParserService qrParserService;

  @Override
  public Response<Page<QRParser>> getAll(String filters, Pageable pageable) {
    return Response.builder().data(qrParserService.getAll(filters, pageable)).build();
  }

  @Override
  public Response<QRParser> create(QRParserCreateRequest qrParserCreateRequest) {
    return Response.builder().data(qrParserService.create(qrParserCreateRequest)).build();
  }

  @Override
  public Response<QRParser> update(QRParserUpdateRequest qrParserUpdateRequest, String id) throws StreemException {
    return Response.builder().data(qrParserService.update(qrParserUpdateRequest, id)).build();
  }

  @Override
  public Response<BasicDto> archive(String id) throws StreemException {
    return Response.builder().data(qrParserService.archive(id)).build();
  }

  @Override
  public Response<BasicDto> unArchive(String id) throws StreemException {
    return Response.builder().data(qrParserService.unArchive(id)).build();
  }
}
