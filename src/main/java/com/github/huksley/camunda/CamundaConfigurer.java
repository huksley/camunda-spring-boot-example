package com.github.huksley.camunda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permission;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resource;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.filter.Filter;
import org.camunda.bpm.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Configure basic filters in Tasklist
 */
@Component
public class CamundaConfigurer {
	private static final Logger log = LoggerFactory.getLogger(CamundaConfigurer.class);

	@Autowired
	ProcessEngine engine;

	@Autowired
	Environment env;

	@Value("${camunda.bpm.admin-user.id:user}")
	String user = "user";

	public String createFilter(String name, int priority, String description, TaskQuery query, String... properties) {
		Filter existingFilter = (Filter) engine.getFilterService().createFilterQuery().filterName(name).singleResult();
		if (existingFilter != null) {
			return existingFilter.getId();
		} else {
			HashMap<String,Object> filterProperties = new HashMap<>();
			filterProperties.put("description", description);
			filterProperties.put("priority", Integer.valueOf(priority));
			ArrayList<Object> v = new ArrayList<Object>();
			
			for (int i = 0; i < properties.length; i++) {
				String key = properties[i];
				Object value = null;
				if (key.indexOf("=") > 0) {
				    value = key.substring(key.indexOf("=") + 1).trim();
				    key = key.substring(0, key.indexOf("=")).trim();
				} else {
				    value = true;
				}
				
				if (key.equals("var")) {
				    Map<String,String> var = new HashMap<>();
				    var.put("name", String.valueOf(value));
		            var.put("label", String.valueOf(value));
		            v.add(var);
				} else {
					filterProperties.put(key, value);
				}
			}
			
			log.info("Creating Camunda filter {} description {}", name, description);
			
            filterProperties.put("variables", v);
			Filter f = engine.getFilterService()
					.newTaskFilter()
					.setName(name)
					.setProperties(filterProperties)
					.setOwner(user)
					.setQuery(query);
			engine.getFilterService().saveFilter(f);
			return f.getId();
		}
	}

	@EventListener
	public void recreateFilters(ApplicationStartedEvent ev) {
		Map<String, Filter> filters = new HashMap<>();
        List<Filter> l = engine.getFilterService().createFilterQuery().list();
        for (Filter f: l) {
            log.info("Found existing filter: {}", f.getId());
            filters.put(f.getName(), f);
        }

		String defaultProps = env.getProperty("camunda.filter.default.properties", "");
		String[] properties = env.getProperty("FILTER_TASK_GROUP_PROPS", defaultProps).split("[ ]*\\,[ ]*");
        String filterName = env.getProperty("FILTER_TASK_GROUP_NAME", "Group tasks");
        if (filters.get(filterName) == null) {
            createFilter(filterName, 20,
                env.getProperty("FILTER_TASK_GROUP_DESCRIPTION", "Tasks for all groups"),
                engine.getTaskService().createTaskQuery().taskCandidateGroupInExpression("${currentUserGroups()}").taskUnassigned(),
                properties);
        }

		properties = env.getProperty("FILTER_TASK_MY_PROPS", defaultProps).split("[ ]*\\,[ ]*");
        filterName = env.getProperty("FILTER_TASK_MY_NAME", "Current tasks");
        if (filters.get(filterName) == null) {
            createFilter(filterName, 10,
                env.getProperty("FILTER_TASK_MY_DESCRIPTION", "My current tasks"),
                engine.getTaskService().createTaskQuery().taskAssigneeExpression("${currentUser()}"),
                properties);
        }

		properties = env.getProperty("FILTER_TASK_ALL_PROPS", defaultProps).split("[ ]*\\,[ ]*");
		filterName = env.getProperty("FILTER_TASK_ALL_NAME", "All tasks");
		if (filters.get(filterName) == null) {
            createFilter(filterName, 30,
                env.getProperty("FILTER_TASK_ALL_DESCRIPTION", "All Tasks (not recommended to be used in production)"),
                engine.getTaskService().createTaskQuery(), properties);
        }
	}
}
