package com.github.huksley.camunda;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;

/**
 * Appropriate forwarding and processing inside system.
 */
@Controller
@RequestMapping("/")
@Configuration
@Api(description = "Browser forwarding and utility endpoints")
public class WebPageForward implements WebMvcConfigurer {
    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    Environment env;
    
    @Autowired
    ApplicationContext context;

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.
            addResourceHandler("/index.html").
            addResourceLocations("classpath:/static/index.html").
            resourceChain(false);
    }

    @ApiOperation("Redirects / elsewhere")
    @GetMapping("/")
    public void root(HttpServletResponse response) throws IOException {
        response.sendRedirect(env.getProperty("forward.root", "/launchpad.html"));
    }
}