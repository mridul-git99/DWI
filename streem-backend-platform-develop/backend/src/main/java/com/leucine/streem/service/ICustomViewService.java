package com.leucine.streem.service;

import com.leucine.streem.ObjectTypeCustomView;
import com.leucine.streem.collections.CustomView;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.CustomViewRequest;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.helper.JobLogColumn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICustomViewService {
  List<CustomView> getAllCustomViews(String filters);

  CustomView getCustomViewById(String customViewId) throws ResourceNotFoundException;

  CustomView createCustomView(Long checklistId, CustomViewRequest customViewRequest) throws ResourceNotFoundException, StreemException;

  CustomView createCustomView(CustomViewRequest customViewRequest) throws StreemException;

  CustomView editCustomView(String customViewId, CustomViewRequest customViewRequest) throws ResourceNotFoundException;

  BasicDto archiveCustomView(String customViewId) throws ResourceNotFoundException;

  void reConfigureCustomView(Long checklistId, List<JobLogColumn> jobLogColumns);

  Page<ObjectTypeCustomView> getAllObjectTypeCustomViews(String filters, Pageable pageable);

  ObjectTypeCustomView createObjectTypeCustomView(String objectTypeId, CustomViewRequest customViewRequest) throws StreemException;

  ObjectTypeCustomView editObjectTypeCustomView(String customViewId, CustomViewRequest customViewRequest) throws ResourceNotFoundException;

  BasicDto archiveObjectTypeCustomView(String customViewId) throws ResourceNotFoundException;

  BasicDto unarchiveObjectTypeCustomView(String customViewId) throws ResourceNotFoundException;
}
