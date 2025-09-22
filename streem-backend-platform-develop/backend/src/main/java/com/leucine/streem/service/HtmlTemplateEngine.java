package com.leucine.streem.service;

import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.leucine.streem.service.CssClasses.*;

/**
 * HTML Template Engine for generating consistent, reusable HTML components
 * Provides atomic, molecular, and organism-level HTML components for PDF generation
 */
public class HtmlTemplateEngine {

    // ==================== ATOMIC COMPONENTS (Basic HTML Tags) ====================
    
    /**
     * Creates a div element with optional CSS class
     */
    public static String div(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<div" + classAttr + ">" + content + "</div>";
    }
    
    /**
     * Creates a span element with optional CSS class
     */
    public static String span(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<span" + classAttr + ">" + content + "</span>";
    }
    
    /**
     * Creates a paragraph element with optional CSS class
     */
    public static String p(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<p" + classAttr + ">" + content + "</p>";
    }
    
    /**
     * Creates an h1 element with optional CSS class
     */
    public static String h1(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<h1" + classAttr + ">" + safeHtmlEscape(content) + "</h1>";
    }
    
    /**
     * Creates an h2 element with optional CSS class
     */
    public static String h2(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<h2" + classAttr + ">" + safeHtmlEscape(content) + "</h2>";
    }
    
    /**
     * Creates an h3 element with optional CSS class
     */
    public static String h3(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<h3" + classAttr + ">" + safeHtmlEscape(content) + "</h3>";
    }
    
    /**
     * Creates an h4 element with optional CSS class
     */
    public static String h4(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<h4" + classAttr + ">" + safeHtmlEscape(content) + "</h4>";
    }
    
    /**
     * Creates an h5 element with optional CSS class
     */
    public static String h5(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<h5" + classAttr + ">" + safeHtmlEscape(content) + "</h5>";
    }
    
    /**
     * Creates a table element with optional CSS class
     */
    public static String table(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<table" + classAttr + ">" + content + "</table>";
    }
    
    /**
     * Creates a table with fixed layout and width
     */
    public static String tableWithStyle(String content, String cssClass, String style) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        String styleAttr = style != null ? " style=\"" + style + "\"" : "";
        return "<table" + classAttr + styleAttr + ">" + content + "</table>";
    }
    
    /**
     * Creates a thead element
     */
    public static String thead(String content) {
        return "<thead>" + content + "</thead>";
    }
    
    /**
     * Creates a tbody element
     */
    public static String tbody(String content) {
        return "<tbody>" + content + "</tbody>";
    }
    
    /**
     * Creates a tr element
     */
    public static String tr(String content) {
        return "<tr>" + content + "</tr>";
    }
    
    /**
     * Creates a td element with optional CSS class
     */
    public static String td(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<td" + classAttr + ">" + content + "</td>";
    }
    
    /**
     * Creates a td element with style
     */
    public static String tdWithStyle(String content, String cssClass, String style) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        String styleAttr = style != null ? " style=\"" + style + "\"" : "";
        return "<td" + classAttr + styleAttr + ">" + content + "</td>";
    }
    
    /**
     * Creates a th element with optional CSS class
     */
    public static String th(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<th" + classAttr + ">" + safeHtmlEscape(content) + "</th>";
    }
    
    /**
     * Creates a th element with style
     */
    public static String thWithStyle(String content, String cssClass, String style) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        String styleAttr = style != null ? " style=\"" + style + "\"" : "";
        return "<th" + classAttr + styleAttr + ">" + safeHtmlEscape(content) + "</th>";
    }
    
    /**
     * Creates a ul element with optional CSS class
     */
    public static String ul(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<ul" + classAttr + ">" + content + "</ul>";
    }
    
    /**
     * Creates a li element with optional CSS class
     */
    public static String li(String content, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<li" + classAttr + ">" + content + "</li>";
    }
    
    /**
     * Creates an anchor element
     */
    public static String a(String href, String text, String cssClass) {
        String classAttr = cssClass != null ? " class=\"" + cssClass + "\"" : "";
        return "<a href=\"" + href + "\"" + classAttr + ">" + safeHtmlEscape(text) + "</a>";
    }
    
    /**
     * Creates a strong element
     */
    public static String strong(String content) {
        return "<strong>" + safeHtmlEscape(content) + "</strong>";
    }
    
    /**
     * Creates a br element
     */
    public static String br() {
        return "<br/>";
    }
    
    /**
     * Creates an hr element with optional style
     */
    public static String hr(String style) {
        String styleAttr = style != null ? " style=\"" + style + "\"" : "";
        return "<hr" + styleAttr + "/>";
    }
    
    /**
     * Creates a sup element (superscript)
     */
    public static String sup(String content, String style) {
        String styleAttr = style != null ? " style=\"" + style + "\"" : "";
        return "<sup" + styleAttr + ">" + safeHtmlEscape(content) + "</sup>";
    }

    // ==================== MOLECULAR COMPONENTS (Composite Components) ====================
    
    /**
     * Creates a simple table row with label and value
     */
    public static String tableRow(String label, String value) {
        return tr(th(label, null) + td(safeHtmlEscape(value), null));
    }
    
    /**
     * Creates a table row with raw HTML value (not escaped)
     */
    public static String tableRowWithRawHtml(String label, String rawHtmlValue) {
        return tr(th(label, null) + td(rawHtmlValue, null));
    }
    
    /**
     * Creates a key-value table from a map
     */
    public static String keyValueTable(Map<String, String> data, String cssClass) {
        String rows = data.entrySet().stream()
            .map(entry -> tableRow(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining());
        return table(rows, cssClass);
    }
    
    /**
     * Creates a table with headers and rows
     */
    public static String tableWithHeaders(List<String> headers, List<List<String>> rows, String cssClass) {
        // Create header row
        String headerCells = headers.stream()
            .map(header -> th(header, null))
            .collect(Collectors.joining());
        String headerRow = thead(tr(headerCells));
        
        // Create data rows
        String dataRows = rows.stream()
            .map(row -> {
                String cells = row.stream()
                    .map(cell -> td(safeHtmlEscape(cell), null))
                    .collect(Collectors.joining());
                return tr(cells);
            })
            .collect(Collectors.joining());
        String bodyRows = tbody(dataRows);
        
        return table(headerRow + bodyRows, cssClass);
    }
    
    /**
     * Creates a bullet list from a list of items
     */
    public static String bulletList(List<String> items, String cssClass) {
        String listItems = items.stream()
            .map(item -> li(safeHtmlEscape(item), null))
            .collect(Collectors.joining());
        return ul(listItems, cssClass);
    }
    
    /**
     * Creates a list of links
     */
    public static String linkList(Map<String, String> linkMap, String cssClass) {
        String listItems = linkMap.entrySet().stream()
            .map(entry -> li(a(entry.getValue(), entry.getKey(), null), null))
            .collect(Collectors.joining());
        return ul(listItems, cssClass);
    }
    
    /**
     * Creates a section with title and content
     */
    public static String section(String title, String content, String titleClass, String contentClass) {
        return h4(title, titleClass) + div(content, contentClass);
    }
    
    /**
     * Creates a panel with content
     */
    public static String panel(String content, String cssClass) {
        return div(content, cssClass);
    }
    
    /**
     * Creates a card with title and content
     */
    public static String card(String title, String content, String cssClass) {
        return div(h4(title, null) + content, cssClass);
    }
    
    /**
     * Creates a page break div
     */
    public static String pageBreak() {
        return div("", CssClasses.PAGE_BREAK);
    }
    
    /**
     * Creates a page break with custom style
     */
    public static String pageBreakWithStyle(String style) {
        return "<div style=\"" + style + "\"></div>";
    }

    // ==================== ORGANISM COMPONENTS (Domain-Specific Templates) ====================
    
    /**
     * Creates a complete section with title and detail panel
     */
    public static String completeSection(String title, String detailContent) {
        String sectionHtml = h4(title, CssClasses.SECTION_TITLE) + div(detailContent, CssClasses.DETAIL_PANEL);
        return div(sectionHtml, "section-wrapper");
    }
    
    /**
     * Creates an annotation box with remarks and optional media
     */
    public static String annotationBox(String remarks, String mediaHtml) {
        StringBuilder content = new StringBuilder();
        content.append(strong("Remarks:")).append("&nbsp;")
               .append(remarks == null ? "-" : safeHtmlEscape(remarks));
        
        if (mediaHtml != null && !mediaHtml.isEmpty()) {
            content.append(br()).append(strong("Medias:")).append(mediaHtml);
        }
        
        return div(content.toString(), CssClasses.ANNOTATION_BOX);
    }
    
    /**
     * Creates a stage header with number and name
     */
    public static String stageHeader(String stageNumber, String stageName) {
        String stageNumberElement = h3("Stage " + stageNumber, CssClasses.STAGE_NUMBER);
        String stageNameElement = div(stageName, CssClasses.STAGE_NAME);
        String separator = div("", CssClasses.STAGE_SEPARATOR);
        
        return div(stageNumberElement + stageNameElement + separator, CssClasses.STAGE_HEADER);
    }
    
    /**
     * Creates a task state paragraph
     */
    public static String taskState(String state) {
        return p("Task State: " + state, CssClasses.TASK_STATE);
    }
    
    /**
     * Creates an activity item for audit trails
     */
    public static String activityItem(String time, String details) {
        String circle = span("", "circle");
        String timeSpan = span(time, "time");
        String detailsSpan = span(details, "details");
        
        return li(circle + timeSpan + detailsSpan, null);
    }
    
    /**
     * Creates a day section for activities
     */
    public static String daySection(String date, int count, String activitiesHtml) {
        String dateSection = div(date + " – " + count + " activities", CssClasses.DATE_SECTION);
        String activitiesList = ul(activitiesHtml, CssClasses.ACTIVITIES);
        
        return dateSection + activitiesList;
    }
    
    /**
     * Creates an early start note
     */
    public static String earlyStartNote(String taskNumber, String reason) {
        String content = "E " + taskNumber + ": Early start for scheduled task: " + reason;
      return div(content, CssClasses.EARLY_START_NOTE);
    }
    
    /**
     * Creates a delayed completion note
     */
    public static String delayedCompletionNote(String reason) {
        String content = "Delayed completion – " + reason;
        return div(content, CssClasses.DELAYED_COMPLETION_NOTE);
    }

    // ==================== UTILITY METHODS ====================
    
    /**
     * Safely escapes HTML content, handling null values
     */
    private static String safeHtmlEscape(String content) {
        return content != null ? HtmlUtils.htmlEscape(content) : "";
    }
    
    /**
     * Creates a filter item for display
     */
    public static String filterItem(int filterNumber, String displayName, String constraint, String value) {
        String filterLabel = "Filter " + filterNumber + " - Where: ";
        String displayNameSpan = span(displayName, "filter-value");
        String constraintSpan = span(constraint, "filter-value");
        String valueSpan = span(value, "filter-value");
        
        String content = filterLabel + displayNameSpan + " Condition: " + constraintSpan + " Value: " + valueSpan;
        return div(content, "filter-item");
    }
    
    /**
     * Creates a bordered span for filter values
     */
    public static String borderedSpan(String content) {
        String style = "display: inline-block; padding: 2px 8px; border: 1px solid #000; margin: 0 5px;";
        return "<span style=\"" + style + "\">" + safeHtmlEscape(content) + "</span>";
    }
}
