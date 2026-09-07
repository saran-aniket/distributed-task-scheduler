package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.dto.CreateJobRequestDTO;
import com.personal.distributedtaskscheduler.dto.JobResponseDTO;
import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.enums.JobStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.repository.JobRepository;
import com.personal.distributedtaskscheduler.utility.CronExpressionParser;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

import static tools.jackson.databind.type.LogicalType.Map;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final CronExpressionParser cronExpressionParser;

    public JobService(JobRepository jobRepository, CronExpressionParser cronExpressionParser) {
        this.jobRepository = jobRepository;
        this.cronExpressionParser = cronExpressionParser;
    }

    public JobResponseDTO createJob(CreateJobRequestDTO createJobRequestDTO){
        try {
            Job job = new Job();
            job.setName(createJobRequestDTO.getName());
            job.setJobType(JobType.valueOf(createJobRequestDTO.getJobType()));
            if(createJobRequestDTO.getCronExpression() != null) {
                String normalizedCronExpression = cronExpressionParser.normalizeCronExpression(createJobRequestDTO.getCronExpression());
                job.setNextFireTime(cronExpressionParser.nextFireTime(normalizedCronExpression, java.time.Instant.now()));
                job.setCronExpression(normalizedCronExpression);
            }
            job.setBackoffSeconds(30);
            job.setMaxRetries(3);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> payload = objectMapper.readValue(createJobRequestDTO.getPayLoad(), new TypeReference<Map<String, Object>>() {
            });
            job.setPayload(payload);
            jobRepository.save(job);
            return JobResponseDTO.from(job);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create job", e);
        }
    }

    public JobResponseDTO getJob(String jobId){
        Optional<Job> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        return optionalJob.map(JobResponseDTO::from).orElse(null);
    }


    public List<JobResponseDTO> getAllJobs(Pageable pageRequest){
        Slice<Job> jobSlice = jobRepository.findAll(pageRequest);
        return jobSlice.stream().map(JobResponseDTO::from).collect(Collectors.toList());
    }

    public void pauseJob(String jobId) {
        Optional<Job> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if(optionalJob.isPresent()){
            Job job = optionalJob.get();
            job.setStatus(JobStatus.PAUSED);
            jobRepository.save(job);
        }else{
            throw new IllegalArgumentException("Job not found");
        }
    }
}
