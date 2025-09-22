package com.leucine.streem.service;

import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Job;

public interface IJobNotificationEmailDispatchService {

  void addJobDelayEmailDispatchEvent(Job job) throws StreemException;


  void addJobOverDueEmailDispatchEvent(Job job) throws StreemException;


}
