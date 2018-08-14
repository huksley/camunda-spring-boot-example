package com.github.huksley.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Obviously should be other microservice to handle external tasks,
 * but for the sake of example is implemented here.
 */
@Controller
public class ExternalTaskExecutor {
    static final Logger log = LoggerFactory.getLogger(ExternalTaskExecutor.class);

    @Autowired
    private RestTemplate restTemplate;

    private String workerId = "sampleWorker";

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    static class TaskInfo {
        String id;
    }

    @Data
    @Builder
    static class FetchRequest {
        String workerId;
        int maxTasks;
        FetchTopicRequest[] topics;
    }

    @Data
    @Builder
    static class FetchTopicRequest {
        String topicName;
        long lockDuration;
        String[] variables;
    }

    @Data
    @Builder
    static class VariableReference {
        String value;
    }

    @Data
    @Builder
    static class CompleteRequest {
        String workerId;
        Map<String, VariableReference> variables;
    }

    @Scheduled(initialDelay = 5000, fixedRate = 5000)
    public void periodicalTaskExecutor() {
        FetchRequest r = FetchRequest.builder().
            maxTasks(1).
            workerId(workerId).
            topics(new FetchTopicRequest[]{
                FetchTopicRequest.builder().
                    lockDuration(10000).
                    topicName("approveExternal").
                    variables(new String[]{"favoriteColor"}).
                    build()
            }).
            build();

        try {
            String fetchUrl = "http://localhost:8087/engine-rest/external-task/fetchAndLock";
            TaskInfo[] fetchResponse = restTemplate.postForObject(fetchUrl, r, TaskInfo[].class);
            log.info("Got {} external tasks to execute", fetchResponse.length);
            for (TaskInfo task : fetchResponse) {
                log.info("Marking {} as complete", task.getId());
                String completeUrl = "http://localhost:8087/engine-rest/external-task/" + task.getId() + "/complete";
                CompleteRequest completeRequest = CompleteRequest.builder().
                    workerId(workerId).
                    variables(new HashMap<>()).
                    build();
                completeRequest.variables.put("approved", VariableReference.builder().value("Certified fresh!").build());
                restTemplate.postForEntity(completeUrl, completeRequest, null);
            }
        } catch (Exception e) {
            log.warn("Failed too check for tasks: {}", e.getMessage(), e);
        }
    }
}
