package com.etendorx.das.externalid;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

import com.etendorx.entities.mapper.lib.PostSyncService;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation of the PostSyncService interface.
 * This class manages post-synchronization tasks using a thread-local queue.
 */
@Component
@Log4j2
public class PostSyncServiceImpl implements PostSyncService {
  private final ThreadLocal<Queue<Runnable>> currentEntity = new ThreadLocal<>();

  /**
   * Constructor for the PostSyncServiceImpl class.
   * Initializes the thread-local queue for storing tasks.
   */
  public PostSyncServiceImpl() {
    super();
  }

  /**
   * Adds a task to the thread-local queue to be executed after synchronization.
   *
   * @param entity a Runnable representing the task to be added
   */
  @Override
  public void add(Runnable entity) {
    synchronized (this) {
      if (currentEntity.get() == null) {
        currentEntity.set(new ConcurrentLinkedDeque<>());
      }
      currentEntity.get().add(entity);
    }
  }

  /**
   * Executes all tasks in the thread-local queue and then clears the queue.
   */
  @Override
  public void flush() {
    synchronized (this) {
      if (currentEntity.get() == null) {
        currentEntity.remove();
        return;
      }
      Runnable entity;
      while (currentEntity.get() != null && (entity = currentEntity.get().poll()) != null) {
        entity.run();
      }
      currentEntity.remove();
    }
  }
}
