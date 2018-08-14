package com.github.huksley.camunda;

import com.github.huksley.camunda.task.RecordCakeTask;
import com.github.huksley.camunda.task.RecordColorTask;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.scenario.ProcessScenario;
import org.camunda.bpm.scenario.Scenario;
import org.camunda.bpm.spring.boot.starter.test.helper.AbstractProcessEngineRuleTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.extension.mockito.DelegateExpressions.autoMock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ensure the sample.bpmn Process is working correctly.
 */
@Deployment(resources = { "bpmn/sample.bpmn", "bpmn/decideOnColor.dmn" })
public class SampleProcessTest extends AbstractProcessEngineRuleTest {
    @Mock
    private ProcessScenario sampleApplication;

    @Before
    public void defineScenarioActions() {
        MockitoAnnotations.initMocks(this);
        Mocks.register("recordColorTask", new RecordColorTask());
        Mocks.register("recordCakeTask", new RecordCakeTask());

        when(sampleApplication.waitsAtUserTask("UserTask_1")).thenReturn((task) ->
            task.complete(var("favoriteColor", "brown"))
        );

        when(sampleApplication.waitsAtServiceTask("ExternalTask_1")).thenReturn((externalTask) -> {
            externalTask.complete();
        });

        when(sampleApplication.waitsAtUserTask("UserTask_2")).thenReturn((task) ->
            task.complete()
        );

        when(sampleApplication.waitsAtUserTask("UserTask_3")).thenReturn((task) ->
            task.complete()
        );
    }

    Map<String,Object> var(String name, String value) {
        Map<String,Object> m = new HashMap<>();
        m.put(name, value);
        return m;
    }

    @Test
    public void fullRunProcess() {
        Scenario.run(sampleApplication).startByKey("Sample").execute();
        verify(sampleApplication, times(1)).hasFinished("EndEventOk");
    }
}
