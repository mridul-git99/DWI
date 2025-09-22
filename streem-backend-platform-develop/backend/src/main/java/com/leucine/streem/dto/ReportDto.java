package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.bson.types.ObjectId;

import static com.leucine.streem.constant.CollectionMisc.ReportType;

public record ReportDto(@JsonSerialize(using = ToStringSerializer.class) ObjectId id, String name, ReportType type) {
}
