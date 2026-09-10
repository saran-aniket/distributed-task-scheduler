package com.personal.distributedtaskscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
public class DistributedTaskSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedTaskSchedulerApplication.class, args);
    }

}
