package com.leucine.streem.dto.request;

import java.util.Map;

public record CreateReportRequest(String name, Map<String, Object> payload) {
}
