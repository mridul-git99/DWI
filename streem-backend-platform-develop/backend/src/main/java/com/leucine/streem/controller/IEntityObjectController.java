package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.changelogs.EntityObjectChangeLog;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.CollectionKey;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.ImportSummary;
import com.leucine.streem.dto.request.ArchiveObjectRequest;
import com.leucine.streem.dto.request.EntityObjectValueRequest;
import com.leucine.streem.dto.request.UnarchiveObjectRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.search.SearchFilter;
import io.undertow.util.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/objects")
public interface IEntityObjectController {
  @GetMapping()
  @ResponseBody
  Response<Page<EntityObject>> findAll(@RequestParam(name = CollectionKey.COLLECTION) String collection, @RequestParam(name = CollectionKey.USAGE_STATUS, defaultValue = "1") int usageStatus,
                                       @RequestParam(name = CollectionKey.EXTERNAL_ID, defaultValue = "") String propertyExternalId, @RequestParam(name = CollectionKey.VALUE, defaultValue = "") String value,
                                       @RequestParam(name = "filters", required = false) String filters,
                                       @SortDefault(sort = BaseEntity.ID, direction = Sort.Direction.DESC) Pageable pageable);

  @GetMapping("/search")
  @ResponseBody
  Response<Page<EntityObject>> findAllObjects(@RequestParam(name = CollectionKey.COLLECTION) String collection, @RequestParam(name = CollectionKey.USAGE_STATUS, defaultValue = "1") int usageStatus,
                                              @RequestParam(name = CollectionKey.EXTERNAL_ID, defaultValue = "") String propertyExternalId, @RequestParam(name = CollectionKey.VALUE, defaultValue = "") String value,
                                              @RequestBody(required = false) SearchFilter filters,
                                              @SortDefault(sort = BaseEntity.ID, direction = Sort.Direction.DESC) Pageable pageable) throws JsonProcessingException;

  @GetMapping("/partial")
  @ResponseBody
  Response<Page<PartialEntityObject>> findAllPartial(@RequestParam(name = CollectionKey.COLLECTION) String collection, @RequestParam(name = CollectionKey.USAGE_STATUS, defaultValue = "1") int usageStatus,
                                                     @RequestParam(name = CollectionKey.EXTERNAL_ID, defaultValue = "") String propertyExternalId, @RequestParam(name = CollectionKey.VALUE, defaultValue = "") String value, @RequestParam(value = "filters", required = false) String filters,
                                                     @RequestParam(value = "query", required = false) String query,
                                                     @SortDefault(sort = BaseEntity.ID, direction = Sort.Direction.DESC) Pageable pageable);

  @PostMapping()
  @ResponseBody
  Response<List<EntityObject>> saveObject(@RequestBody EntityObjectValueRequest entityObjectValueRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @PatchMapping("/{objectId}")
  @ResponseBody
  Response<EntityObject> updateEntityObject(@PathVariable String objectId, @RequestBody EntityObjectValueRequest entityObjectValueRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/{objectId}")
  @ResponseBody
  Response<EntityObject> findObjectById(@PathVariable String objectId, @RequestParam(name = CollectionKey.COLLECTION) String collection) throws ResourceNotFoundException;

  @PatchMapping("/{objectId}/archive")
  @ResponseBody
  Response<BasicDto> archiveObject(@RequestBody ArchiveObjectRequest archiveObjectRequest, @PathVariable String objectId) throws StreemException, BadRequestException, ResourceNotFoundException, JsonProcessingException;

  @PatchMapping("/{objectId}/unarchive")
  Response<BasicDto> unarchiveObject(@RequestBody UnarchiveObjectRequest unarchiveObjectRequest, @PathVariable String objectId) throws ResourceNotFoundException, StreemException, JsonProcessingException;


  @GetMapping("/change-logs")
  Response<Page<EntityObjectChangeLog>> findAllChangeLogs(@RequestParam(name = "filters", required = false) String filters, @SortDefault(sort = BaseEntity.ID, direction = Sort.Direction.DESC) Pageable pageable);

  @PatchMapping("/searchable")
  Response<BasicDto> updateSearchable();

  @GetMapping("/export")
  ResponseEntity<byte[]> exportToExcel(@RequestParam String objectTypeId) throws ResourceNotFoundException, StreemException;

  @PostMapping("/import")
  Response<ImportSummary> importFromExcel(
      @RequestParam String objectTypeId,
      @RequestParam MultipartFile file
  ) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  @GetMapping("/change-logs/download")
  ResponseEntity<byte[]> downloadChangeLogs(@RequestParam(name = "filters", required = false) String filters) throws ResourceNotFoundException, IOException;
}
