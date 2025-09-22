package com.leucine.streem.util;

import com.leucine.streem.collections.JobLog;
import com.leucine.streem.collections.JobLogData;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.FacilityDto;
import com.leucine.streem.dto.GeneratedPdfDataDto;
import com.leucine.streem.dto.UserAuditDto;
import com.leucine.streem.model.Facility;
import com.leucine.streem.repository.IFacilityRepository;
import com.leucine.streem.repository.IOrganisationSettingRepository;
import com.leucine.streem.service.IPdfBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.ConverterProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfGeneratorUtil {
  private final IPdfBuilderService pdfBuilderService;
  private final IOrganisationSettingRepository organisationSettingRepository;
  private final IFacilityRepository facilityRepository;

  @Value("${medias.logo-url}")
  private String leucineLogo;

  /**
   * Builds HTML from a template and data
   * @param pdfFormat The type of PDF to generate
   * @param generatedPdfDataDto The data to use for the PDF
   * @return The HTML string
   * @throws IOException If there is an error reading the template file
   */
  public String buildHtml(Type.PdfType pdfFormat, GeneratedPdfDataDto generatedPdfDataDto) throws IOException {
    String clientLogo = getClientLogoUrl(generatedPdfDataDto);
    
    String template = readTemplateFile(pdfFormat);
    String content  = pdfBuilderService.buildSection(pdfFormat, generatedPdfDataDto);
    String finalHtml = template
      .replace("{{CLIENT_LOGO}}", clientLogo)
      .replace("{{LEUCINE_LOGO}}", leucineLogo)
      .replace("{{content}}", content)
      .replace("{{GENERATED_AT}}", getFormattedGeneratedDateTime(generatedPdfDataDto))
      .replace("{{USER}}", !Utility.isEmpty(generatedPdfDataDto.getUserFullName()) ? generatedPdfDataDto.getUserFullName() : "")
      .replace("{{USER_ID}}", !Utility.isEmpty(generatedPdfDataDto.getUserId()) ? generatedPdfDataDto.getUserId() : "")
      .replace("{{FACILITY}}", getFacilityName(generatedPdfDataDto))
      .replace("{{APP_LABEL}}", appLabel(generatedPdfDataDto));

    return finalHtml;
  }

  /**
   * Generates a PDF from data
   * @param pdfFormat The type of PDF to generate
   * @param generatedPdfDataDto The data to use for the PDF
   * @return The PDF as a byte array
   * @throws IOException If there is an error generating the PDF
   */
  public byte[] generatePdf(Type.PdfType pdfFormat, GeneratedPdfDataDto generatedPdfDataDto) throws IOException {
    String populatedHtml = buildHtml(pdfFormat, generatedPdfDataDto);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      HtmlConverter.convertToPdf(populatedHtml, out, new ConverterProperties());
      return out.toByteArray();
    } catch (IOException e) {
      log.error("PDF generation failed", e);
      throw new RuntimeException("Failed to create PDF", e);
    }
  }


  private String appLabel(GeneratedPdfDataDto generatedPdfDataDto){
    String appLabel = "Leucine App";
    if (generatedPdfDataDto != null && generatedPdfDataDto.getFacility() != null && !Utility.isEmpty(generatedPdfDataDto.getFacility().getId())) {
        Long facilityId = Long.parseLong(generatedPdfDataDto.getFacility().getId());
        Facility facility = facilityRepository.findById(facilityId).orElse(null);
        if (facility != null && facility.getOrganisation() != null && !Utility.isEmpty(facility.getOrganisation().getFqdn())) {
          String fqdn = facility.getOrganisation().getFqdn();
          appLabel = parseAppLabelFromFqdn(fqdn);
        }
      }
    return appLabel;
  }
  /**
   * Parses the fqdn to generate the app label for the PDF footer.
   * Examples:
   *   amneal.uat.platform.leucinetech.com -> "amneal uat"
   *   client.platform.leucine.tech -> "client platform"
   */
  private String parseAppLabelFromFqdn(String fqdn) {
    if (fqdn == null || fqdn.isEmpty()) return "";
    // Remove protocol if present
    fqdn = fqdn.replaceFirst("^https?://", "");
    String[] parts = fqdn.split("\\.");
    List<String> filtered = new ArrayList<>();
    for (String p : parts) if (!p.isEmpty()) filtered.add(p);

    if (filtered.size() >= 2) {
      return filtered.get(0) + " " + filtered.get(1);
    } else if (filtered.size() == 1) {
      return filtered.get(0);
    } else {
      return fqdn;
    }
  }

  private String readTemplateFile(Type.PdfType pdfType) throws IOException {
    String path;
    switch (pdfType) {
      case JOB_REPORT:
        path = "templates/job-pdf-report.html";
        break;
      case JOB_AUDIT:
        path = "templates/job-audit-report.html";
        break;
      case JOB_LOGS:
        path = "templates/job-logs-report.html";
        break;
      case OBJECT_AUDIT_LOGS:
        path = "templates/object-audit-logs-report.html";
        break;
      case PROCESS_TEMPLATE:
        path = "templates/job-pdf-report.html"; // Reuse job report template
        break;
      default:
        throw new IllegalStateException("Template not found: " + pdfType);
    }
    Resource res = new ClassPathResource(path);
    byte[] bytes = res.getInputStream().readAllBytes();
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static void addPair(StringBuilder sb, String key, Object value) {
    sb.append("<p><strong>").append(key).append(":</strong> ")
      .append(value == null ? "" : value.toString())
      .append("</p>");
  }

  public static String userName(UserAuditDto u) {
    return u == null ? "" : (u.getFirstName() + " " + u.getLastName()).trim();
  }

  /**
   * Formats the generatedOn timestamp according to the facility's date and time format
   * @param data The GeneratedPdfDataDto
   * @return The formatted date and time string or an empty string if not available
   */
  private String getFormattedGeneratedDateTime(GeneratedPdfDataDto data) {
    if (Utility.isEmpty(data) || Utility.isEmpty(data.getGeneratedOn())) {
      return "";
    }

    try {
      Long timestamp = data.getGeneratedOn();
      FacilityDto facility = data.getFacility();

      if (!Utility.isEmpty(facility)) {
        String dateTimeFormat = facility.getDateTimeFormat();
        String timeZone = facility.getTimeZone();

        if (!Utility.isEmpty(dateTimeFormat) && !Utility.isEmpty(timeZone)) {
          // Convert timestamp to facility timezone and format using facility datetime format
          long zonedTimestamp = DateTimeUtils.convertUTCEpochToZoneEpoch(timestamp, timeZone);
          return DateTimeUtils.getFormattedDateTimeOfPattern(zonedTimestamp, dateTimeFormat);
        }
      }

      // Fallback to default formatting if facility info is not available
      return DateTimeUtils.getFormattedDateTime(timestamp);
    } catch (Exception e) {
      log.error("Error formatting generated date time", e);
      return "";
    }
  }

  /**
   * Safely extracts the facility name from the data
   * @param data The GeneratedPdfDataDto
   * @return The facility name or an empty string if not available
   */
  private String getFacilityName(GeneratedPdfDataDto data) {
    if (Utility.isEmpty(data) || Utility.isEmpty(data.getFacility())) {
      return "";
    }

    try {
      FacilityDto facility = data.getFacility();
      return !Utility.isEmpty(facility.getName()) ? facility.getName() : "";
    } catch (Exception e) {
      log.error("Error getting facility name", e);
      return "";
    }
  }

  /**
   * Gets the client logo URL from organisation settings or returns empty string
   * @param data The GeneratedPdfDataDto
   * @return The logo URL to use for the client logo, or empty string if not found
   */
  private String getClientLogoUrl(GeneratedPdfDataDto data) {
    try {
      if (data != null && data.getFacility() != null && !Utility.isEmpty(data.getFacility().getId())) {
        // Get the full facility entity from repository to access organisation
        Long facilityId = Long.parseLong(data.getFacility().getId());
        Facility facility = facilityRepository.findById(facilityId).orElse(null);

        if (facility != null && facility.getOrganisation() != null) {
          Long organisationId = facility.getOrganisation().getId();
          String logoUrl = organisationSettingRepository.findLogoUrlByOrganisationId(organisationId)
              .orElse(null);
          if (!Utility.isEmpty(logoUrl)) {
            log.debug("Using organisation logo URL: {} for organisation ID: {}", logoUrl, organisationId);
            return logoUrl;
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to fetch organisation logo, returning empty string", e);
    }

    log.debug("No organisation logo found, returning empty string");
    return "";
  }

  /** Build one big map that holds *every* value we can find for the row */
  public static Map<String,Object> buildRowMap(JobLog log) {
    Map<String,Object> m = new HashMap<>();

    if (log.getParameterValues() != null) m.putAll(log.getParameterValues());

    if (log.getLogs() != null) {
      for (JobLogData d : log.getLogs()) {
        if (d.getTriggerType() != null)
          m.put(d.getTriggerType().name(), d.getValue());
      }
    }

    for (Field f : JobLog.class.getDeclaredFields()) {
      f.setAccessible(true);
      try { m.putIfAbsent(f.getName().toLowerCase(), f.get(log)); }
      catch (IllegalAccessException ignored) { }
    }
    return m;
  }
}
