package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.changelogs.EntityObjectChangeLog;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.controller.IEntityObjectController;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.ImportSummary;
import com.leucine.streem.dto.request.ArchiveObjectRequest;
import com.leucine.streem.dto.request.EntityObjectValueRequest;
import com.leucine.streem.dto.request.UnarchiveObjectRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.helper.search.SearchFilter;
import com.leucine.streem.service.IEntityObjectChangeLogService;
import com.leucine.streem.service.IEntityObjectExportImportService;
import com.leucine.streem.service.IEntityObjectService;
import com.leucine.streem.util.IdGenerator;
import com.leucine.streem.util.JsonUtils;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class EntityObjectController implements IEntityObjectController {
  private final IEntityObjectService entityObjectService;
  private final IEntityObjectChangeLogService entityObjectChangeLogService;
  private final IEntityObjectExportImportService entityObjectExportImportService;

  @Autowired
  public EntityObjectController(IEntityObjectService entityObjectService, IEntityObjectChangeLogService entityObjectChangeLogService, IEntityObjectExportImportService entityObjectExportImportService) {
    this.entityObjectService = entityObjectService;
    this.entityObjectChangeLogService = entityObjectChangeLogService;
    this.entityObjectExportImportService = entityObjectExportImportService;
  }

  @Override
  public Response<Page<EntityObject>> findAll(String collection, int usageStatus, String propertyExternalId, String propertyValue, String filters, Pageable pageable) {
    return Response.builder().data(entityObjectService.findAllByUsageStatus(collection, usageStatus, propertyExternalId, propertyValue, filters, pageable)).build();
  }

  @Override
  public Response<Page<EntityObject>> findAllObjects(String collection, int usageStatus, String propertyExternalId, String propertyValue, SearchFilter filters, Pageable pageable) throws JsonProcessingException {
    return Response.builder().data(entityObjectService.findAllByUsageStatus(collection, usageStatus, propertyExternalId, propertyValue, JsonUtils.writeValueAsString(filters), pageable)).build();
  }

  @Override
  public Response<Page<PartialEntityObject>> findAllPartial(String collection, int usageStatus, String propertyExternalId, String propertyValue, String filters, String query, Pageable pageable) {
    return Response.builder().data(entityObjectService.findPartialByUsageStatus(collection, usageStatus, propertyExternalId, propertyValue, pageable, filters, query)).build();
  }

  @Override
  public Response<List<EntityObject>> saveObject(EntityObjectValueRequest entityObjectValueRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    EntityObject savedObject = entityObjectService.save(entityObjectValueRequest, entityObjectValueRequest.getInfo());
    return Response.builder().data(savedObject).build();
  }

  @Override
  public Response<EntityObject> updateEntityObject(String objectId, EntityObjectValueRequest entityObjectValueRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(entityObjectService.update(objectId, entityObjectValueRequest, entityObjectValueRequest.getInfo())).build();
  }

  @Override
  public Response<Page<EntityObjectChangeLog>> findAllChangeLogs(String filters, Pageable pageable) {
    return Response.builder().data(entityObjectChangeLogService.findAllChangeLogs(filters, pageable)).build();
  }

  @Override
  public Response<EntityObject> findObjectById(String objectId, String collection) throws ResourceNotFoundException {
    return Response.builder().data(entityObjectService.findById(collection, objectId)).build();
  }

  @Override
  public Response<BasicDto> updateSearchable() {
    return Response.builder().data(entityObjectService.enableSearchable()).build();
  }

  @Override
  public Response<BasicDto> unarchiveObject(UnarchiveObjectRequest unarchiveObjectRequest, String objectId) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    return Response.builder().data(entityObjectService.unarchiveObject(unarchiveObjectRequest, objectId, null)).build();
  }

  @Override
  public Response<BasicDto> archiveObject(ArchiveObjectRequest archiveObjectRequest, String objectId) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(entityObjectService.archiveObject(archiveObjectRequest, objectId, null)).build();
  }

  @Override
  public ResponseEntity<byte[]> exportToExcel(String objectTypeId) throws ResourceNotFoundException, StreemException {
    byte[] excelData = entityObjectExportImportService.exportToExcel(objectTypeId);

    String filename = IdGenerator.getInstance().nextId() + ".xlsx";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", filename);
    headers.setContentLength(excelData.length);

    return ResponseEntity.ok()
        .headers(headers)
        .body(excelData);
  }

  @Override
  public Response<ImportSummary> importFromExcel(String objectTypeId, MultipartFile file)
      throws ResourceNotFoundException, StreemException, JsonProcessingException {
    ImportSummary summary = entityObjectExportImportService.importFromExcel(objectTypeId, file);
    return Response.builder().data(summary).build();
  }

  public ResponseEntity<byte[]> downloadChangeLogs(String filters) throws IOException, ResourceNotFoundException {
    byte[] pdfBytes = entityObjectChangeLogService.downloadChangeLogs(filters);

    // build a proper "attachment" Content-Disposition
    String filename = UUID.randomUUID().toString() + ".pdf";
    ContentDisposition disposition = ContentDisposition
      .attachment()
      .filename(filename)
      .build();

    return ResponseEntity
      .ok()
      .contentType(MediaType.APPLICATION_PDF)
      .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
      .body(pdfBytes);
  }
}
