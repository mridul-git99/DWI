package com.leucine.streem.repository;

import com.leucine.streem.collections.Report;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IReportRepository extends MongoRepository<Report, String> {
}
