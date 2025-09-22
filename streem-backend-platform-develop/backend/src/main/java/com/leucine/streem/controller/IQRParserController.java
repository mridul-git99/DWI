package com.leucine.streem.controller;


import com.leucine.streem.collections.parser.QRParser;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.QRParserCreateRequest;
import com.leucine.streem.dto.request.QRParserUpdateRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/qr-parser")
public interface IQRParserController {
  @GetMapping
  Response<Page<QRParser>> getAll(@RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @PostMapping
  Response<QRParser> create(@RequestBody QRParserCreateRequest qrParserCreateRequest);

  @PatchMapping("/update/{id}")
  Response<QRParser> update(@RequestBody QRParserUpdateRequest qrParserUpdateRequest, @PathVariable String id) throws StreemException;

  @PatchMapping("/{id}/archive")
  Response<BasicDto> archive(@PathVariable String id) throws StreemException;

  @PatchMapping("/{id}/un-archive")
  Response<BasicDto> unArchive(@PathVariable String id) throws StreemException;
}
