package com.personal.distributedtaskscheduler.controller;

import com.personal.distributedtaskscheduler.dto.CreateJobRequestDTO;
import com.personal.distributedtaskscheduler.dto.JobResponseDTO;
import com.personal.distributedtaskscheduler.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<List<JobResponseDTO>> getAllJobs(@PageableDefault(size = 10) Pageable pageable){
        return ResponseEntity.ok().body(jobService.getAllJobs(pageable));
    }

    @GetMapping(value = "/{jobId}")
    public ResponseEntity<JobResponseDTO> getByJobId(@PathVariable String jobId){
        return ResponseEntity.ok().body(jobService.getJob(jobId));
    }

    @PostMapping
    public ResponseEntity<JobResponseDTO> createJob(@Valid @RequestBody CreateJobRequestDTO createJobRequestDTO){
        return ResponseEntity.ok().body(jobService.createJob(createJobRequestDTO));
    }

    @PatchMapping(value = "/{jobId}/pause")
    public ResponseEntity<Void> pauseJob(@PathVariable String jobId){
        jobService.pauseJob(jobId);
        return ResponseEntity.ok().build();
    }
}
