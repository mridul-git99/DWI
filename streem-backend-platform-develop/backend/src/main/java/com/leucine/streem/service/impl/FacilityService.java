package com.leucine.streem.service.impl;

import com.leucine.streem.config.JaasServiceProperty;
import com.leucine.streem.dto.FacilityDto;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.service.IFacilityService;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityService implements IFacilityService {
  private final RestTemplate jaasRestTemplate;
  private final JaasServiceProperty jaasServiceProperty;

  @Override
  public Response<Object> getAllFacilities(String filters, Pageable pageable) {
    log.info("[getAllFacilities] Request to get all facilitties, filters: {}, pageable: {}", filters, pageable);
    HttpEntity<Response> response = jaasRestTemplate.exchange(
        Utility.toUriString(jaasServiceProperty.getFacilityUrl(), filters, pageable), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Response.class);
    return response.getBody();

  }

  @Override
  public FacilityDto getFacility(long facilityId) {
    Response response = jaasRestTemplate.getForObject(jaasServiceProperty.getFacilityUrl() + "/" + facilityId, Response.class);
    //TODO: Map this using ObjectMapper of JsonUtils, Handle Exception in global jaas exception Handler
    LinkedHashMap<String, String> dataMap = (LinkedHashMap<String, String>) response.getData();
    return mapToFacilityDto(dataMap);
  }

  private FacilityDto mapToFacilityDto(LinkedHashMap<String, String> dataMap) {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setDateFormat( dataMap.get("dateFormat"));
    facilityDto.setTimeFormat(dataMap.get("timeFormat"));
    facilityDto.setDateTimeFormat(dataMap.get("dateTimeFormat"));
    facilityDto.setTimeZone(dataMap.get("timeZone"));
    facilityDto.setId(dataMap.get("id"));
    facilityDto.setName(dataMap.get("name"));
    return facilityDto;
  }
}
