package com.bs.travel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * knife4j / springdoc 的文档信息。页面地址：http://localhost:8080/doc.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI travelOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧文旅行程规划系统接口文档")
                        .version("1.0.0")
                        .description("基于大语言模型的智慧文旅行程规划系统 RESTful API。"
                                + "除注册、登录和接口文档外，请求需在 Authorization 请求头携带 Bearer JWT。"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口返回的 token，填写时不用带 Bearer 前缀")));
    }
}
