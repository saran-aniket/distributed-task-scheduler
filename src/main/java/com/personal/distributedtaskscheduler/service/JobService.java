package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.dto.CreateJobRequestDTO;
import com.personal.distributedtaskscheduler.dto.JobResponseDTO;
import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.repository.JobRepository;
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

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public JobResponseDTO createJob(CreateJobRequestDTO createJobRequestDTO){
        Job job = new Job();
        job.setName(createJobRequestDTO.getName());
        job.setJobType(JobType.valueOf(createJobRequestDTO.getJobType()));
        job.setCronExpression(createJobRequestDTO.getCronExpression());
        job.setBackoffSeconds(30);
        job.setMaxRetries(3);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> payload = objectMapper.readValue(createJobRequestDTO.getPayLoad(), new TypeReference<Map<String, Object>>(){});
        job.setPayload(payload);
        jobRepository.save(job);
        return JobResponseDTO.from(job);
    }

    public JobResponseDTO getJob(String jobId){
        Optional<Job> optionalJob = jobRepository.findById(UUID.fromString(jobId));
        return optionalJob.map(JobResponseDTO::from).orElse(null);
    }


    public List<JobResponseDTO> getAllJobs(Pageable pageRequest){
        Slice<Job> jobSlice = jobRepository.findAll(pageRequest);
        return jobSlice.stream().map(JobResponseDTO::from).collect(Collectors.toList());
    }
}
