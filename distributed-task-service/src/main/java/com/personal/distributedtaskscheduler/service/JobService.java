package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.dto.CreateJobRequestDTO;
import com.personal.distributedtaskscheduler.dto.JobResponseDTO;
import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.Tenant;
import com.personal.distributedtaskscheduler.entity.enums.JobStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.repository.JobRepository;
import com.personal.distributedtaskscheduler.repository.TenantRepository;
import com.personal.distributedtaskscheduler.utility.CronExpressionParser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

import static tools.jackson.databind.type.LogicalType.Map;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final CronExpressionParser cronExpressionParser;
    private final TenantRepository tenantRepository;

    public JobService(JobRepository jobRepository, CronExpressionParser cronExpressionParser, TenantRepository tenantRepository) {
        this.jobRepository = jobRepository;
        this.cronExpressionParser = cronExpressionParser;
        this.tenantRepository = tenantRepository;
    }

    public JobResponseDTO createJob(CreateJobRequestDTO createJobRequestDTO){
        try {
            com.personal.distributedtaskscheduler.entity.Tenant tenant = getAuthorizedTenant();
            Job job = new Job();
            job.setName(createJobRequestDTO.getName());
            job.setJobType(JobType.valueOf(createJobRequestDTO.getJobType()));
            job.setStatus(JobStatus.ACTIVE);
            if(createJobRequestDTO.getCronExpression() != null) {
                String normalizedCronExpression = cronExpressionParser.normalizeCronExpression(createJobRequestDTO.getCronExpression());
                job.setNextFireTime(cronExpressionParser.nextFireTime(normalizedCronExpression, java.time.Instant.now()));
                job.setCronExpression(normalizedCronExpression);
            }
            job.setBackoffSeconds(30);
            job.setMaxRetries(3);
            job.setTenant(tenant);
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
        String tenantId = getAuthorizedTenant().getId().toString();
        Optional<Job> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if(optionalJob.isPresent() && !optionalJob.get().getTenant().getId().toString().equals(tenantId)){
            throw new RuntimeException("Job not found");
        }
        return optionalJob.map(JobResponseDTO::from).orElseThrow(() -> new RuntimeException("Job not found"));
    }


    public List<JobResponseDTO> getAllJobs(Pageable pageRequest){
        String tenantId = getAuthorizedTenant().getId().toString();
        Slice<Job> jobSlice = jobRepository.findAll(pageRequest);
        return jobSlice.stream().filter(job -> job.getTenant().getId().toString().equals(tenantId)).map(JobResponseDTO::from).collect(Collectors.toList());
    }

    public void pauseJob(String jobId) {
        String tenantId = getAuthorizedTenant().getId().toString();
        Optional<Job> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        if(optionalJob.isPresent()){
            Job job = optionalJob.get();
            if(!job.getTenant().getId().toString().equals(tenantId)){
                throw new RuntimeException("Job not found");
            }
            job.setStatus(JobStatus.PAUSED);
            jobRepository.save(job);
        }else{
            throw new IllegalArgumentException("Job not found");
        }
    }

    private Tenant getAuthorizedTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null){
            throw new RuntimeException("Authentication is null");
        }
        Object principal = authentication.getPrincipal();
        if(principal == null){
            throw new RuntimeException("Principal is null");
        }
        return tenantRepository.findById(UUID.fromString(principal.toString())).orElseThrow(() -> new RuntimeException("Tenant not found"));
    }
}
