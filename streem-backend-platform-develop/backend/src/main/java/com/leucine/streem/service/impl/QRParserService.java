package com.leucine.streem.service.impl;

import com.leucine.streem.collections.helper.MongoFilter;
import com.leucine.streem.collections.parser.QRParser;
import com.leucine.streem.collections.parser.SplitDataRuleDto;
import com.leucine.streem.constant.UsageStatus;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.request.QRParserCreateRequest;
import com.leucine.streem.dto.request.QRParserUpdateRequest;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.IQRParserRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.IQRParserService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.CaseUtils;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class QRParserService implements IQRParserService {
  private final MongoTemplate mongoTemplate;
  private final IQRParserRepository qrParserRepository;
  private final IUserRepository userRepository;
  private final IUserMapper userMapper;

  @Override
  public Page<QRParser> getAll(String filters, Pageable pageable) {
    log.info("Fetching QRParser with filters: {}", filters);
    Query query = MongoFilter.buildQuery(filters);
    long count = mongoTemplate.count(query, QRParser.class);
    query.with(pageable);
    List<QRParser> qrParserList = mongoTemplate.find(query, QRParser.class);
    return PageableExecutionUtils.getPage(qrParserList, pageable, () -> count);
  }

  @Override
  public BasicDto unArchive(String id) throws StreemException {
    log.info("Un-archiving QRParser: {}", id);
    QRParser qrParser = qrParserRepository.findById(id).orElseThrow(() -> new StreemException(ErrorCode.QR_PARSER_NOT_FOUND.getCode()));

    if (qrParser.getUsageStatus() == UsageStatus.ACTIVE.getCode()) {
      ValidationUtils.invalidate(id, ErrorCode.QR_PARSER_ALREADY_ACTIVE);
    }
    qrParser.setUsageStatus(UsageStatus.ACTIVE.getCode());
    qrParserRepository.save(qrParser);
    return new BasicDto(qrParser.getId().toString(), null, null);
  }

  @Override
  public BasicDto archive(String id) throws StreemException {
    log.info("Archiving QRParser: {}", id);
    QRParser qrParser = qrParserRepository.findById(id).orElseThrow(() -> new StreemException(ErrorCode.QR_PARSER_NOT_FOUND.getCode()));

    if (qrParser.getUsageStatus() == UsageStatus.DEPRECATED.getCode()) {
      ValidationUtils.invalidate(id, ErrorCode.QR_PARSER_ALREADY_ARCHIVED);
    }
    qrParser.setUsageStatus(UsageStatus.DEPRECATED.getCode());
    qrParserRepository.save(qrParser);
    return new BasicDto(qrParser.getId().toString(), null, null);
  }

  @Override
  public QRParser update(QRParserUpdateRequest qrParserUpdateRequest, String id) throws StreemException {
    log.info("Updating QRParser: {}", qrParserUpdateRequest);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User userPrincipalEntity = userRepository.getReferenceById(principalUser.getId());

    QRParser qrParser = qrParserRepository.findById(id).orElseThrow(() -> new StreemException(ErrorCode.QR_PARSER_NOT_FOUND.getCode()));

    qrParser.setDisplayName(qrParserUpdateRequest.getDisplayName());


    //TODO: add validations to check if the rules are same or not

    List<SplitDataRuleDto> qrParserRules = qrParserUpdateRequest.getRules();

    Map<ObjectId, SplitDataRuleDto> rulesMap = qrParser.getRules().stream().collect(Collectors.toMap(SplitDataRuleDto::getId, Function.identity()));
    for (SplitDataRuleDto qrParserRule : qrParserRules) {
      rulesMap.put(qrParserRule.getId(), qrParserRule);
    }

    qrParser.setModifiedAt(DateTimeUtils.now());
    qrParser.setModifiedBy(userMapper.toUserAuditDto(userPrincipalEntity));
    qrParser.setRules(rulesMap.values().stream().toList());
    return qrParserRepository.save(qrParser);

  }

  @Override
  public QRParser create(QRParserCreateRequest qrParserCreateRequest) {
    log.info("Creating QRParser: {}", qrParserCreateRequest);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User userPrincipalEntity = userRepository.getReferenceById(principalUser.getId());
    String externalId = CaseUtils.toCamelCase(qrParserCreateRequest.getDisplayName(), false);
    QRParser qrParser = qrParserRepository.findByObjectTypeIdAndExternalId(qrParserCreateRequest.getObjectTypeId(), externalId);

    if (Utility.isEmpty(qrParser)) {
      qrParser = new QRParser();
    }

    String rawData = qrParserCreateRequest.getRawData();
    String delimiter = qrParserCreateRequest.getDelimiter();
    qrParser.setDelimiter(delimiter);
    qrParser.setRawData(rawData);
    qrParser.setExternalId(externalId);
    qrParser.setDisplayName(qrParserCreateRequest.getDisplayName());
    qrParser.setObjectTypeId(qrParserCreateRequest.getObjectTypeId());

    qrParser.setRules(qrParserCreateRequest.getRules());
    qrParser.setCreatedAt(DateTimeUtils.now());
    qrParser.setCreatedBy(userMapper.toUserAuditDto(userPrincipalEntity));
    qrParser.setModifiedAt(DateTimeUtils.now());
    qrParser.setModifiedBy(userMapper.toUserAuditDto(userPrincipalEntity));
    qrParser.setUsageStatus(UsageStatus.ACTIVE.getCode());
    return qrParserRepository.save(qrParser);
  }
}
