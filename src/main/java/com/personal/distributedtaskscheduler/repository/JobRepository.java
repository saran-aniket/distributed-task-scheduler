package com.personal.distributedtaskscheduler.repository;

import com.personal.distributedtaskscheduler.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query(value = "Select * FROM jobs j WHERE j.status = :status AND j.next_fire_time <= :now " +
            "ORDER BY j.next_fire_time LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Job> findByStatusAndNextFireTimeLessThanEqual(@Param("status") String status, @Param("now") Instant now, @Param("batchSize") int batchSize);
}
