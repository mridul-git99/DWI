package com.leucine.streem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.MappedRelation;
import com.leucine.streem.collections.PropertyOption;
import com.leucine.streem.collections.PropertyValue;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.AutoInitializeDto;
import com.leucine.streem.dto.ResourceParameterChoiceDto;
import com.leucine.streem.dto.projection.AutoInitializeParameterView;
import com.leucine.streem.dto.request.ParameterExecuteRequest;
import com.leucine.streem.dto.request.ParameterRequest;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.ParameterValueBase;
import com.leucine.streem.model.helper.parameter.ChoiceParameterBase;
import com.leucine.streem.model.helper.parameter.ResourceParameter;
import com.leucine.streem.model.helper.parameter.ValueParameterBase;
import com.leucine.streem.repository.IAutoInitializedParameterRepository;
import com.leucine.streem.repository.IParameterRepository;
import com.leucine.streem.repository.IParameterValueRepository;
import com.leucine.streem.repository.ITempParameterValueRepository;
import com.leucine.streem.service.IEntityObjectService;
import com.leucine.streem.service.IParameterAutoInitializeService;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.leucine.streem.constant.Misc.PARAMETER_EXECUTABLE_STATES;

@Service
@AllArgsConstructor
@Slf4j
public class ParameterAutoInitializeService implements IParameterAutoInitializeService {
  private final IAutoInitializedParameterRepository autoInitializedParameterRepository;
  private final IParameterRepository parameterRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final ITempParameterValueRepository tempParameterValueRepository;
  private final IEntityObjectService entityObjectService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public List<ParameterExecuteRequest> getAllParameterExecuteRequestForParameterToAutoInitialize(Long jobId, List<AutoInitializeParameterView> autoInitializedParameterViewList, boolean isExecutedForCorrection) throws IOException, ResourceNotFoundException {
    List<ParameterExecuteRequest> parameterExecuteRequests = new ArrayList<>();
    for (AutoInitializeParameterView autoInitializeParameterView : autoInitializedParameterViewList) {
      ParameterExecuteRequest parameterExecuteRequest = getParameterExecuteRequestForParameterToAutoInitialize(jobId, autoInitializeParameterView.getAutoInitializedParameterId(), isExecutedForCorrection, autoInitializeParameterView.getReferencedParameterId());
      if (!Utility.isEmpty(parameterExecuteRequest)) {
        parameterExecuteRequests.add(parameterExecuteRequest);
      }
    }

    return parameterExecuteRequests;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ParameterExecuteRequest getParameterExecuteRequestForParameterToAutoInitialize(Long jobId, Long autoInitializedParameterId, boolean isExecutedForCorrection, Long referencedParameterId) throws ResourceNotFoundException, IOException {
    ParameterExecuteRequest parameterExecuteRequest = null;
    boolean autoInitializedParameter = autoInitializedParameterRepository.existsByAutoInitializedParameterId(autoInitializedParameterId);

    if (autoInitializedParameter) {
      Parameter parameter = parameterRepository.getReferenceById(autoInitializedParameterId);
      if (Type.Parameter.CALCULATION.equals(parameter.getType())) {
        boolean isLatestReferencedParameterExecuted;
        if (isExecutedForCorrection) {
          isLatestReferencedParameterExecuted = tempParameterValueRepository.checkIfDependentParametersOfCalculationParameterNotExecuted(jobId, parameter.getId());
        } else {
          isLatestReferencedParameterExecuted = parameterValueRepository.checkIfLatestReferencedParameterIsExecuted(jobId, referencedParameterId);
        }
        if (isLatestReferencedParameterExecuted) {
          parameterExecuteRequest = new ParameterExecuteRequest();
          parameterExecuteRequest.setJobId(jobId);
          ParameterRequest parameterRequest = new ParameterRequest();
          parameterRequest.setData(parameter.getData());
          parameterRequest.setId(parameter.getId());
          parameterRequest.setLabel(parameter.getLabel());
          parameterExecuteRequest.setParameter(parameterRequest);
          parameterExecuteRequest.setReferencedParameterId(referencedParameterId);
        }

      } else {
        JsonNode valueData = null;
        AutoInitializeDto autoInitializeDto = JsonUtils.readValue(parameter.getAutoInitialize().toString(), AutoInitializeDto.class);
        // This is the executed parameter from which we will get the value
        Long executedParameterId = Long.valueOf(autoInitializeDto.getParameterId());

        ParameterValueBase executedResourceParameter;

        if (isExecutedForCorrection) {
          executedResourceParameter = tempParameterValueRepository.getReferenceById(executedParameterId);
        } else {
          executedResourceParameter = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, executedParameterId);
        }
        boolean isResourceParameterHasActiveExceptions = false;
        boolean isResourceParameterExecuted = false;
        Type.VerificationType referencedResourceParameterVerificationType = Type.VerificationType.NONE;
        if(!Utility.isEmpty(executedResourceParameter)) {

          isResourceParameterHasActiveExceptions = parameterValueRepository.checkIfResourceParameterHasActiveExceptions(jobId, executedResourceParameter.getId());
          referencedResourceParameterVerificationType = executedResourceParameter.getParameter().getVerificationType();
          isResourceParameterExecuted = (referencedResourceParameterVerificationType == Type.VerificationType.NONE)
            ? PARAMETER_EXECUTABLE_STATES.contains(executedResourceParameter.getState())
            : executedResourceParameter.getState() == State.ParameterExecution.EXECUTED;
        }
        if (isResourceParameterExecuted && !isResourceParameterHasActiveExceptions) {
          List<ResourceParameterChoiceDto> parameterChoices = JsonUtils.jsonToCollectionType(executedResourceParameter.getChoices(), List.class, ResourceParameterChoiceDto.class);
          // The parameter that is auto initialized for the following switch case parameter types
          // will always refer to a resource parameter, this resource parameter is currently only single select, hence we are fetching 0th element
          // we will pick the value from that resource parameter and set it in the auto initialized parameter
          // now this value can be either value of a property of that selected resource for the resource parameter
          // or it can be a relation of that selected resource for the resource parameter
          if (!parameterChoices.isEmpty()) {
            ResourceParameterChoiceDto resourceParameterChoice = parameterChoices.get(0);
            String objectId = resourceParameterChoice.getObjectId();
            String collection = resourceParameterChoice.getCollection();
            EntityObject entityObject = entityObjectService.findById(collection, objectId);

            switch (parameter.getType()) {
              case RESOURCE -> {
                MappedRelation mappedRelation = new MappedRelation();

              for (MappedRelation mr : entityObject.getRelations()) {
                if (mr.getId().toString().equals(autoInitializeDto.getRelation().getId())) {
                  mappedRelation = mr;
                }
              }

                ResourceParameter resourceParameter = new ResourceParameter();
                List<ResourceParameterChoiceDto> resourceParameterChoiceDtos = new ArrayList<>();
                if (!Utility.isEmpty(mappedRelation.getTargets())) {
                  ResourceParameterChoiceDto rpcd = new ResourceParameterChoiceDto();
                  rpcd.setObjectId(mappedRelation.getTargets().get(0).getId().toString());
                  rpcd.setObjectExternalId(mappedRelation.getTargets().get(0).getExternalId());
                  rpcd.setObjectDisplayName(mappedRelation.getTargets().get(0).getDisplayName());
                  rpcd.setCollection(mappedRelation.getTargets().get(0).getCollection());
                  resourceParameterChoiceDtos.add(rpcd);
                }
                resourceParameter.setChoices(resourceParameterChoiceDtos);
                valueData = JsonUtils.valueToNode(resourceParameter);
              }
              case NUMBER, SINGLE_LINE, MULTI_LINE, DATE, DATE_TIME -> {
                PropertyValue propertyValue = new PropertyValue();

                for (PropertyValue pv : entityObject.getProperties()) {
                  if (pv.getId().toString().equals(autoInitializeDto.getProperty().getId())) {
                    propertyValue = pv;
                  }
                }

                ValueParameterBase valueParameterBase = new ValueParameterBase();
                valueParameterBase.setInput(propertyValue.getValue());
                valueData = JsonUtils.valueToNode(valueParameterBase);
              }
              case SINGLE_SELECT -> {
                JsonNode propertyValue = null;
                for (PropertyValue pv : entityObject.getProperties()) {
                  if (pv.getId().toString().equals(autoInitializeDto.getProperty().getId())) {
                    List<ChoiceParameterBase> ssdChoices = new ArrayList<>();
                    if (!Utility.isEmpty(pv.getChoices())) {
                      PropertyOption propertyOption = pv.getChoices().get(0);
                      ChoiceParameterBase ssdChoice = new ChoiceParameterBase();
                      ssdChoice.setId(propertyOption.getId().toString());
                      ssdChoice.setName(propertyOption.getDisplayName());
                      ssdChoice.setState(State.Selection.SELECTED.toString());
                      ssdChoices.add(ssdChoice);
                    }
                    propertyValue = JsonUtils.valueToNode(ssdChoices);
                  }
                }
                valueData = propertyValue;
              }
            }
          }else {
            log.info("[getParameterExecuteRequestForParameterToAutoInitialize] No choices available for executed parameter with id {}", executedParameterId);
            valueData = JsonUtils.valueToNode(new ValueParameterBase() {{
              setInput(null);
            }});
          }

// TODO change this here and in update log method
          ParameterRequest parameterRequest = new ParameterRequest();
          parameterRequest.setId(parameter.getId());
          parameterRequest.setLabel(parameter.getLabel());
          parameterRequest.setData(valueData);

          parameterExecuteRequest = new ParameterExecuteRequest();
          parameterExecuteRequest.setParameter(parameterRequest);
          parameterExecuteRequest.setJobId(jobId);
          parameterExecuteRequest.setReason("");
        } else {
          log.info("[getParameterExecuteRequestForParameterToAutoInitialize] Parameter with id {} is not in executed state", executedParameterId);
        }

      }
      return parameterExecuteRequest;

    }


    return null;
  }
}
