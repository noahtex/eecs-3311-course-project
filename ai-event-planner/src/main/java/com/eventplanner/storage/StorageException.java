package com.eventplanner.storage;

/** Safe storage diagnostics only; never includes document contents or environment values. */
public class StorageException extends RuntimeException {
 public StorageException(String message){super(message);}
}
