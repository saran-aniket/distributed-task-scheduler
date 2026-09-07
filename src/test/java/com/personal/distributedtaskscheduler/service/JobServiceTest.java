package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.dto.CreateJobRequestDTO;
import com.personal.distributedtaskscheduler.dto.JobResponseDTO;
import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.repository.JobRepository;
import com.personal.distributedtaskscheduler.utility.CronExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CronExpressionParser cronExpressionParser;

    @InjectMocks
    private JobService jobService;

    @Test
    void createJob_mapsRequestAndSavesJobWithExpectedFieldValues() {
        CreateJobRequestDTO request = new CreateJobRequestDTO();
        request.setName("nightly-report");
        request.setJobType("HTTP_CALLBACK");
        request.setCronExpression("0 0 * * *");
        request.setPayLoad("{\"url\":\"https://example.com\",\"retries\":2}");
        request.setWebhookUrl("https://example.com/hook");

        // save() is a no-op on the mock, so mimic the DB assigning an id (JobResponseDTO.from needs it).
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        when(cronExpressionParser.nextFireTime(any(), any())).thenReturn(java.time.Instant.now().plusSeconds(3600));

        JobResponseDTO response = jobService.createJob(request);

        // Verify the entity handed to save() carries the mapped values.
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job saved = jobCaptor.getValue();

        assertThat(saved.getName()).isEqualTo("nightly-report");
        assertThat(saved.getJobType()).isEqualTo(JobType.HTTP_CALLBACK);
        assertThat(saved.getCronExpression()).isNull();
        // These defaults are applied by the service, not the request.
        assertThat(saved.getBackoffSeconds()).isEqualTo(30);
        assertThat(saved.getMaxRetries()).isEqualTo(3);
        // Payload JSON string is parsed into a Map.
        assertThat(saved.getPayload())
                .containsEntry("url", "https://example.com")
                .containsEntry("retries", 2);
        // NOTE: the service does not currently map webhookUrl onto the entity.
        // This assertion documents that gap — flip it once the service copies webhookUrl.
        assertThat(saved.getWebhookUrl()).isNull();

        // Verify the returned DTO reflects the saved entity.
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(saved.getId().toString());
        assertThat(response.getName()).isEqualTo("nightly-report");
        assertThat(response.getJobType()).isEqualTo("HTTP_CALLBACK");
        assertThat(response.getCronExpression()).isNull();
    }

    @Test
    void getJob_returnsMappedResponseWhenJobExists() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setName("existing-job");
        job.setJobType(JobType.HTTP_CALLBACK);
        job.setCronExpression("0 0 * * *");
        job.setPayload(Map.of("url", "https://example.com"));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobResponseDTO response = jobService.getJob(jobId.toString());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(jobId.toString());
        assertThat(response.getName()).isEqualTo("existing-job");
        assertThat(response.getJobType()).isEqualTo("HTTP_CALLBACK");
        assertThat(response.getCronExpression()).isEqualTo("0 0 * * *");
        verify(jobRepository).findById(jobId);
    }

    @Test
    void getJob_returnsNullWhenJobDoesNotExist() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        JobResponseDTO response = jobService.getJob(jobId.toString());

        assertThat(response).isNull();
        verify(jobRepository).findById(jobId);
    }

    @Test
    void getAllJobs_mapsSliceToResponseList() {
        Job jobOne = new Job();
        jobOne.setId(UUID.randomUUID());
        jobOne.setName("job-one");
        jobOne.setJobType(JobType.HTTP_CALLBACK);
        jobOne.setCronExpression("0 0 * * *");
        jobOne.setPayload(Map.of("name", "one"));

        Job jobTwo = new Job();
        jobTwo.setId(UUID.randomUUID());
        jobTwo.setName("job-two");
        jobTwo.setJobType(JobType.INTERNAL_JOB);
        jobTwo.setCronExpression("*/15 * * * *");
        jobTwo.setPayload(Map.of("name", "two"));

        when(jobRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(jobOne, jobTwo)));

        List<JobResponseDTO> responses = jobService.getAllJobs(Pageable.unpaged());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("job-one");
        assertThat(responses.get(1).getJobType()).isEqualTo("INTERNAL_JOB");
        verify(jobRepository).findAll(eq(Pageable.unpaged()));
    }
}
