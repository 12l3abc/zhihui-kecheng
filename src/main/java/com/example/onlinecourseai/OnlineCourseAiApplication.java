package com.example.onlinecourseai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.onlinecourseai.mapper")
public class OnlineCourseAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineCourseAiApplication.class, args);
    }

}
