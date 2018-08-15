package com.github.huksley.camunda;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.catalina.ssi.ByteArrayServletOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Configures and properly proxies Swagger UI and Swagger JSON.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig implements WebMvcConfigurer  {
    private static final Logger log = LoggerFactory.getLogger(SwaggerConfig.class);

    @Autowired
    Environment env;

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.
            addResourceHandler("/swagger.json").
            addResourceLocations("classpath:/META-INF/resources/swagger.json").
            resourceChain(false).
            addTransformer(new ResourceTransformer() {
                @Override
                public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
                    log.info("Transforming JSON");
                    String s = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                    return new ByteArrayResource(s.getBytes(StandardCharsets.UTF_8)) {
                        @Override
                        public String getFilename() {
                            return "swagger.json";
                        }

                        @Override
                        public long lastModified() throws IOException {
                            return System.currentTimeMillis();
                        }
                    };
                }
            });
    }

	@SuppressWarnings("deprecation")
    @Bean
	public SecurityConfiguration security() {
		return new SecurityConfiguration(null, null, null, null, null, ApiKeyVehicle.HEADER, "X-Auth-Token", ",");
	}
	
    @Bean
    public Docket api() { 
        return new Docket(DocumentationType.SWAGGER_2)  
          .select().paths(
              Predicates.and(
                  // Exclude error
                  Predicates.not((s) -> s.equals("/error")),
                  // Exclude management APIs
                  Predicates.not((s) -> s.equals("/management")),
                  Predicates.not((s) -> s.equals("/management.json")),
                  Predicates.not((s) -> s.startsWith("/management/"))
              )
          )
          .build()
          .apiInfo(apiInfo())
          // Always produce localhost, it will be removed by filter
          .host("localhost");
    }

    @Bean
    public FilterRegistrationBean createApiFilter() {
        FilterRegistrationBean b = new FilterRegistrationBean(new Filter() {
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;

                String json = null;
                try (ByteArrayServletOutputStream s = new ByteArrayServletOutputStream()) {
                    HttpServletResponseWrapper w = new HttpServletResponseWrapper(res) {
                        @Override
                        public PrintWriter getWriter() throws IOException {
                            throw new IOException("Expecting only binary output");
                        }

                        public ServletOutputStream getOutputStream() throws IOException {
                            return s;
                        }
                    };
                    chain.doFilter(request, w);
                    json = new String(s.toByteArray(), "UTF-8");
                }

                log.trace("Got JSON {} chars", json.length());
                int port = env.getProperty("server.port", Integer.class, 8080);
                String path = env.getProperty("server.contextPath", "/");
                String apiPath = env.getProperty("spring.jersey.application-path", "/engine-rest");
                if (path.endsWith("/")) {
                    apiPath = path.substring(0, path.length() - 1) + apiPath;
                } else {
                    apiPath = path + apiPath;
                }

                // Change Swagger JSON - remove host && modify basePath
                json = json.replace(",\"host\":\"localhost\",", ",\"host\":\"localhost:" + port + "\",");
                json = json.replace(",\"basePath\":\"/\",", ",\"basePath\":\"" + path + "\",");

                // Camunda Swagger Spec - remove host && modify basePath
                json = json.replace("\"host\" : \"localhost:8080\",", "\"host\" : \"localhost:" + port + "\",");
                json = json.replace("\"basePath\" : \"/engine-rest/engine/default\",", "\"basePath\" : \"" + apiPath + "\",");
                byte[] bb = json.getBytes("UTF-8");
                res.setContentLength(bb.length);
                res.getOutputStream().write(bb);
            }

            @Override
            public void destroy() {
            }
        });
		b.setName("SwaggerJSONFilter");
        b.setUrlPatterns(Arrays.asList("/v2/api-docs", "/swagger.json"));
        return b;
    }
    
    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
            env.getProperty("swagger.title", "API"),
            env.getProperty("swagger.description", "API"),
            env.getProperty("swagger.version", "0.1"),
            null,
            new springfox.documentation.service.Contact(
                env.getProperty("swagger.contact", "Example company"), 
                env.getProperty("swagger.contact.url", "https://example.com"), 
                env.getProperty("swagger.contact.email", "contact@example.com")
            ),
            null,
            null,
                Collections.emptyList() // should not be null
        );
        return apiInfo;
    }
}