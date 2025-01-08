package com.etendorx.entities.mapper.lib;

/**
 * The PostSyncService interface defines a contract for a service that handles post-synchronization tasks.
 * It provides methods to add tasks and to execute all added tasks.
 */
public interface PostSyncService {

  /**
   * Adds a task to be executed after the synchronization process.
   *
   * @param entity a Runnable representing the task to be added
   */
  void add(Runnable entity);

  /**
   * Executes all the tasks that have been added via the add method.
   */
  void flush();
}
