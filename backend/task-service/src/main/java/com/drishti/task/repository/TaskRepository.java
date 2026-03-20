package com.drishti.task.repository;

import com.drishti.task.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task>     findByDriverId(String driverId);
    List<Task>     findByStatus(Task.TaskStatus status);
    Optional<Task> findByReportId(String reportId);
    List<Task>     findByDriverIdAndStatus(String driverId, Task.TaskStatus status);
}
