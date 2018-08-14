package com.github.huksley.camunda.task;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Implements service task from BPM process.
 */
@Component
public class RecordCakeTask implements JavaDelegate {
    private static final Logger log = LoggerFactory.getLogger(RecordCakeTask.class);

    @Override
    public void execute(DelegateExecution execution) {
        Map<String,Object> cakeDecision = (Map<String, Object>) execution.getVariable("cakeDecision");
        log.info("Based on your selected color {} suggested cake is {}",
            execution.getVariable("favoriteColor"),
            cakeDecision.get("suggestedCake"));
    }
}
