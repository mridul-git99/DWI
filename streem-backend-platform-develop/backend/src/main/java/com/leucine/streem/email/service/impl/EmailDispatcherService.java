package com.leucine.streem.email.service.impl;

import com.leucine.streem.config.MediaConfig;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.leucine.streem.config.AwsSQSConfig;
import com.leucine.streem.config.AwsSQSConfig;
import com.leucine.streem.email.config.EmailProperties;
import com.leucine.streem.email.dto.EmailRequest;
import com.leucine.streem.email.dto.PreparedEmail;
import com.leucine.streem.email.exception.EmailException;
import com.leucine.streem.email.exception.FreeMarkerException;
import com.leucine.streem.email.repository.IEmailTemplateRepository;
import com.leucine.streem.email.service.IEmailAuditService;
import com.leucine.streem.email.service.IEmailDispatcherService;
import com.leucine.streem.email.util.FreeMarkerUtil;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.JsonUtils;
import freemarker.template.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class EmailDispatcherService implements IEmailDispatcherService {
  private static final String FAILED_TO_SEND_EMAIL = "Failed to send email";
  private final JavaMailSender mailSender;
  private final Configuration freemarkerConfig;
  private final EmailProperties emailProperties;
  private final IEmailAuditService emailAuditService;
  private final PolicyFactory emailAttributeSanitizerPolicy;
  private final RestTemplate restTemplate;
  private final MediaConfig mediaConfig;
  private final AwsSQSConfig awsSQSConfig;
  private final IEmailTemplateRepository emailTemplateRepository;

  @Autowired
  public EmailDispatcherService(final JavaMailSender javaMailSender, final Configuration configuration,
                                final EmailProperties emailProperties, final IEmailAuditService emailAuditService, MediaConfig mediaConfig, AwsSQSConfig awsSQSConfig,IEmailTemplateRepository emailTemplateRepository) {
    this.mailSender = javaMailSender;
    this.freemarkerConfig = configuration;
    this.emailProperties = emailProperties;
    this.emailAuditService = emailAuditService;
    this.mediaConfig = mediaConfig;
    this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    this.awsSQSConfig = awsSQSConfig;
    this.emailTemplateRepository = emailTemplateRepository;
    this.emailAttributeSanitizerPolicy = new HtmlPolicyBuilder()
      .allowElements() // No elements allowed
      .toFactory();
  }

  @Override
  public String sendMail(EmailRequest emailRequest) {
    if (emailTemplateRepository.existsByNameAndArchivedTrue(emailRequest.getTemplateName())) {
      log.warn("[sendMail] Email Template: {} is archived – e-mail skipped",emailRequest.getTemplateName());
      return FAILED_TO_SEND_EMAIL;
    }
    try {
      var preparedEmail = prepareMail(emailRequest);
      List<PreparedEmail> batchEmails = splitPreparedEmails(preparedEmail);
      log.info("[sendMail] Prepared email: {}", JsonUtils.writeValueAsString(preparedEmail));
      for (PreparedEmail email : batchEmails) {
        SendMessageResult sendMessageResult = awsSQSConfig.amazonSQS().sendMessage(awsSQSConfig.getAwsConfig().getQueueName(), JsonUtils.writeValueAsString(email));
        log.info(sendMessageResult.toString());
//      mailSender.send(prepareMimeMessage(preparedEmail));
        writeToAudit(email, sendMessageResult.getMessageId());
      }
    } catch (Exception ex) {
      log.error("[sendMail] Failed to send email", ex);
      return FAILED_TO_SEND_EMAIL;
    }
    return SUCCESS_MESSAGE;
  }

  public List<PreparedEmail> splitPreparedEmails(PreparedEmail email) {
    List<PreparedEmail> splitEmails = new ArrayList<>();

    // Get the original "to" field
    String[] toAddresses = email.getTo();

    if (toAddresses == null || toAddresses.length == 0) {
      return splitEmails; // No addresses to process
    }

    int batchSize = 50; // Max emails per PreparedEmail
    for (int i = 0; i < toAddresses.length; i += batchSize) {
      // Create a new PreparedEmail object
      PreparedEmail splitEmail = new PreparedEmail();

      // Copy fields from the original email
      splitEmail.setFrom(email.getFrom());
      splitEmail.setCc(email.getCc());
      splitEmail.setBcc(email.getBcc());
      splitEmail.setSubject(email.getSubject());
      splitEmail.setBody(email.getBody());
      splitEmail.setAttachments(email.getAttachments());
      splitEmail.setAttributes(email.getAttributes());

      // Add a batch of "to" addresses
      int end = Math.min(i + batchSize, toAddresses.length);
      String[] batch = new String[end - i];
      System.arraycopy(toAddresses, i, batch, 0, end - i);
      splitEmail.setTo(batch);

      // Add to the list of split emails
      splitEmails.add(splitEmail);
    }

    return splitEmails;
  }

  PreparedEmail prepareMail(EmailRequest emailRequest) throws IOException, FreeMarkerException {
    var preparedEmail = new PreparedEmail();
    String fromAddress = preparedEmail.getFrom() == null ? emailProperties.getFromAddress() : preparedEmail.getFrom();
    preparedEmail.setFrom(fromAddress);
    if (null != emailRequest.getTo()) {
      preparedEmail.setTo(emailRequest.getTo().stream().filter(Objects::nonNull).toArray(String[]::new));
    }
    if (null != emailRequest.getCc()) {
      preparedEmail.setCc(emailRequest.getCc().stream().filter(Objects::nonNull).toArray(String[]::new));
    }
    if (null != emailRequest.getBcc()) {
      preparedEmail.setBcc(emailRequest.getBcc().stream().filter(Objects::nonNull).toArray(String[]::new));
    }
    preparedEmail.setSubject(emailRequest.getSubject());

    // Sanitize each attribute value in the map.
    Map<String, String> sanitizedAttributes = new HashMap<>();
    for (Map.Entry<String, String> entry : emailRequest.getAttributes().entrySet()) {
      String sanitizedValue = emailAttributeSanitizerPolicy.sanitize(entry.getValue());
      sanitizedAttributes.put(entry.getKey(), sanitizedValue);
    }

    var template = freemarkerConfig.getTemplate(emailRequest.getTemplateName());
    String html = FreeMarkerUtil.processTemplate(template, sanitizedAttributes);
    preparedEmail.setBody(html);
    return preparedEmail;
  }

  private MimeMessage prepareMimeMessage(PreparedEmail preparedEmail) throws MessagingException {
    var message = mailSender.createMimeMessage();
    var helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
    helper.setFrom(preparedEmail.getFrom());
    if (null != preparedEmail.getTo()) {
      helper.setTo(preparedEmail.getTo());
    }
    if (null != preparedEmail.getCc()) {
      helper.setCc(preparedEmail.getCc());
    }
    if (null != preparedEmail.getBcc()) {
      helper.setBcc(preparedEmail.getBcc());
    }
    helper.setText(preparedEmail.getBody(), true);
    helper.setSubject(preparedEmail.getSubject());
    //TODO host image to avoid no image show in emails ?
    if (Utility.isEmpty(mediaConfig.getLogoUrl())) {
      helper.addInline("leucine-blue-logo", new ClassPathResource("leucine-blue-logo.png"));
    } else {
      byte[] imageBytes = restTemplate.getForObject(mediaConfig.getLogoUrl(), byte[].class);
      ByteArrayResource imageResource = new ByteArrayResource(imageBytes);
      helper.addInline("leucine-blue-logo", imageResource, "image/png");
    }
    return message;
  }

  private void writeToAudit(PreparedEmail preparedEmail, String messageId) {
    emailAuditService.writeToAudit(preparedEmail, messageId);
  }
}
