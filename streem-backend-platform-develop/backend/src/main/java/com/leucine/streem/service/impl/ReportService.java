package com.leucine.streem.service.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.quicksight.AmazonQuickSight;
import com.amazonaws.services.quicksight.AmazonQuickSightClientBuilder;
import com.amazonaws.services.quicksight.model.*;
import com.leucine.streem.config.AwsConfig;
import com.leucine.streem.constant.Misc;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.util.IdGenerator;
import com.leucine.streem.collections.Report;
import com.leucine.streem.collections.helper.MongoFilter;
import com.leucine.streem.config.MetabaseProperty;
import com.leucine.streem.dto.ReportDto;
import com.leucine.streem.dto.ReportURIDto;
import com.leucine.streem.dto.mapper.IReportMapper;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.IFacilityRepository;
import com.leucine.streem.repository.IOrganisationRepository;
import com.leucine.streem.repository.IReportRepository;
import com.leucine.streem.service.IReportService;
import com.leucine.streem.util.DateTimeUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService implements IReportService {
  private final IReportRepository reportRepository;
  private final MetabaseProperty metabaseProperty;
  private final IOrganisationRepository organisationRepository;
  private final IFacilityRepository facilityRepository;
  private final IReportMapper reportMapper;
  private final MongoTemplate mongoTemplate;
  private final AmazonQuickSight quickSightClient;
  private final AwsConfig awsConfig;


  @Override
  public Page<ReportDto> getAllReports(String filters, Pageable pageable) {
    log.info("Fetching all reports, filters {}, pageable {}", filters, pageable);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Query query = MongoFilter.buildQueryWithFacilityId(filters, principalUser.getCurrentFacilityId().toString());
    long count = mongoTemplate.count(query, Report.class);
    query.with(pageable);
    List<Report> reportList = mongoTemplate.find(query, Report.class);
    return PageableExecutionUtils.getPage(reportMapper.toDto(reportList), pageable, () -> count);
  }

  @Override
  public ReportURIDto getReportURI(String id, String useCaseId) throws ResourceNotFoundException {
    log.info("Fetching report URI with report id: {}, useCaseId: {}", id, useCaseId);
    Optional<Report> optionalReport = reportRepository.findById(id);
    if (optionalReport.isPresent()) {
      Map<String, Object> claims = new HashMap<>();
      Report report = optionalReport.get();
      Map<String, Object> payload = report.getPayload();
      if (report.isUseParameters()) {
        PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String organisationName = organisationRepository.getReferenceById(principalUser.getOrganisationId()).getName();
        Long facilityId = principalUser.getCurrentFacilityId();
        Map<String, Object> paramsClaim = new HashMap<>();
        if (!(facilityId == null || facilityId == -1)) {
          String facilityName = facilityRepository.getReferenceById(facilityId).getName();
          paramsClaim.put("facility", List.of(facilityName));
        }
        paramsClaim.put("organisation", List.of(organisationName));
        paramsClaim.put("useCaseId", List.of(useCaseId));
        claims.put("params", paramsClaim);
      } else {
        claims.put("params", new HashMap<>());
      }
      claims.put("resource", payload.get("resource"));
      SecretKey key = Keys.hmacShaKeyFor(metabaseProperty.getSecretKey().getBytes());
      String token = Jwts.builder()
        .setId(String.valueOf(IdGenerator.getInstance().nextId()))
        .setClaims(claims)
        .setIssuedAt(new Date())
        .setExpiration(new Date(DateTimeUtils.nowPlusMinutesToEpochMilli(report.getTokenExpiration())))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
      String uri = metabaseProperty.getUrl() + "/embed/dashboard/" + token + "#refresh=" + metabaseProperty.getRefreshRate();
      return new ReportURIDto(report.getId().toString(), uri);
    }
    throw new ResourceNotFoundException(id, ErrorCode.REPORT_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND);
  }
  @Override
  public BasicDto generateQSConsoleUrl() {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String initialPath = "/start";
    String accountId = awsConfig.getAccountId();
    String userArn = awsConfig.getArn() + principalUser.getEmail();
    ArrayList<String> allowedDomains = new ArrayList<>(Arrays.asList(awsConfig.getAllowedDomains()));
    final RegisteredUserEmbeddingExperienceConfiguration experienceConfiguration = new RegisteredUserEmbeddingExperienceConfiguration()
      .withQuickSightConsole(new RegisteredUserQuickSightConsoleEmbeddingConfiguration().withInitialPath(initialPath));
    final GenerateEmbedUrlForRegisteredUserRequest generateEmbedUrlForRegisteredUserRequest = new GenerateEmbedUrlForRegisteredUserRequest();
    generateEmbedUrlForRegisteredUserRequest.setAwsAccountId(accountId);
    generateEmbedUrlForRegisteredUserRequest.setUserArn(userArn);
    generateEmbedUrlForRegisteredUserRequest.setAllowedDomains(allowedDomains);
    generateEmbedUrlForRegisteredUserRequest.setExperienceConfiguration(experienceConfiguration);

    final GenerateEmbedUrlForRegisteredUserResult generateEmbedUrlForRegisteredUserResult = quickSightClient
      .generateEmbedUrlForRegisteredUser(generateEmbedUrlForRegisteredUserRequest);
    String url = generateEmbedUrlForRegisteredUserResult.getEmbedUrl();
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage(url);
    return basicDto;
  }

  @Override
  public BasicDto generateQSDashboardUrl(String dashboardId) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String accountId = awsConfig.getAccountId();
    String userArn = awsConfig.getArn() + principalUser.getEmail();
    ArrayList<String> allowedDomains = new ArrayList<>(Arrays.asList(awsConfig.getAllowedDomains()));
    final RegisteredUserEmbeddingExperienceConfiguration experienceConfiguration = new RegisteredUserEmbeddingExperienceConfiguration()
      .withDashboard(new RegisteredUserDashboardEmbeddingConfiguration().withInitialDashboardId(dashboardId));
    final GenerateEmbedUrlForRegisteredUserRequest generateEmbedUrlForRegisteredUserRequest = new GenerateEmbedUrlForRegisteredUserRequest();
    generateEmbedUrlForRegisteredUserRequest.setAwsAccountId(accountId);
    generateEmbedUrlForRegisteredUserRequest.setUserArn(userArn);
    generateEmbedUrlForRegisteredUserRequest.setAllowedDomains(allowedDomains);
    generateEmbedUrlForRegisteredUserRequest.setExperienceConfiguration(experienceConfiguration);

    final GenerateEmbedUrlForRegisteredUserResult generateEmbedUrlForRegisteredUserResult = quickSightClient
      .generateEmbedUrlForRegisteredUser(generateEmbedUrlForRegisteredUserRequest);
    String url = generateEmbedUrlForRegisteredUserResult.getEmbedUrl();
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage(url);
    return basicDto;
  }

  @Override
  public BasicDto generateQSDashboardIds() {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    SearchDashboardsRequest searchDashboardsRequest = new SearchDashboardsRequest();
    String accountId = awsConfig.getAccountId();
    searchDashboardsRequest.setAwsAccountId(accountId);
    Collection<DashboardSearchFilter> filters = new ArrayList<>();
    DashboardSearchFilter filter = new DashboardSearchFilter();
    filter.setName("QUICKSIGHT_USER");
    filter.setOperator("StringEquals");
    filter.setValue(awsConfig.getArn() +principalUser.getEmail());
    filters.add(filter);
    searchDashboardsRequest.setFilters(filters);
    SearchDashboardsResult searchDashboardsResult = quickSightClient.searchDashboards(searchDashboardsRequest);
    List<String> dashboardIds = new ArrayList<>();
    for (DashboardSummary dashboardSummary : searchDashboardsResult.getDashboardSummaryList()) {
      dashboardIds.add(dashboardSummary.getDashboardId() + ":" + dashboardSummary.getName());
    }
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage(Arrays.toString(dashboardIds.toArray()));
    return basicDto;
  }

  @Override
  public BasicDto reportsEditorOrViewer() {
//    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//    boolean supervisorRoleMatch = principalUser.getRoleNames().stream().anyMatch(Misc.SUPERVISOR_ROLES::contains);
    BasicDto basicDto = new BasicDto();
//    basicDto.setMessage(supervisorRoleMatch ? "author" : "viewer");
    basicDto.setMessage("viewer");
    return basicDto;
  }
}
