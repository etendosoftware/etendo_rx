package com.etendorx.eventhandler.transaction;

public interface RestCallTransactionHandler {
  void begin();

  void commit();
}
