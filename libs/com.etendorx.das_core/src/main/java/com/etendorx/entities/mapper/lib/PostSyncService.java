package com.etendorx.entities.mapper.lib;

public interface PostSyncService {

  void add(Runnable entity);

  void flush();
}
