package com.eventplanner.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Exclusive session lock + checked, flushed, same-directory temporary-file replacement. */
public class JsonPlanRepository implements PlanRepository {
 private final Path file;
 private final ObjectMapper json=StorageJson.mapper();
 private FileChannel channel;
 private FileLock lock;
 private boolean loaded,closed;
 private byte[] expectedDigest;
 public JsonPlanRepository(Path directory){
  file=directory.toAbsolutePath().normalize().resolve("planit.json");
  try{
   Files.createDirectories(file.getParent());
   channel=FileChannel.open(file.resolveSibling("planit.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
   lock=channel.tryLock();
   if(lock==null)throw new StorageException("Planit's data store is already open. Close the other GUI or CLI session and try again. Data: "+file);
  }catch(OverlappingFileLockException ex){closeQuietly();throw new StorageException("Planit's data store is already open. Close the other GUI or CLI session and try again. Data: "+file);}
  catch(IOException|SecurityException ex){closeQuietly();throw new StorageException("Cannot open Planit's local data directory. Check access permissions: "+file.getParent());}
  catch(StorageException ex){closeQuietly();throw ex;}
 }
 public Path file(){return file;}
 @Override public String description(){return "Local JSON: "+file;}
 @Override public synchronized Optional<PlanData> load(){
  ensureOpen();
  if(loaded)throw new StorageException("This data store has already been loaded. Restart to reload external changes.");
  try{
   byte[] bytes=contentsOrNull();
   if(bytes==null){loaded=true;expectedDigest=null;return Optional.empty();}
   PlanData data=json.readValue(bytes,PlanData.class);
   if(data==null)throw new IllegalArgumentException("Empty document.");
   PlanDataMapper.restore(data); // Semantic validation before allowing any writes.
   expectedDigest=digest(bytes);loaded=true;return Optional.of(data);
  }catch(IOException|RuntimeException ex){
   throw new StorageException("Cannot load Planit's saved data (unreadable, invalid, or unsupported format). The existing file has not been changed. Restore a valid backup before restarting: "+file);
  }
 }
 @Override public synchronized void save(PlanData data){
  ensureOpen();if(!loaded)throw new StorageException("Saved data was not successfully loaded. The existing file is protected: "+file);
  Path temporary=null;
  try{
   byte[] contents=contentsOrNull();byte[] current=contents==null?null:digest(contents);
   if(!Arrays.equals(expectedDigest,current))throw new StorageException("Planit's save file changed outside this session. This change was not saved; restart to load the current file: "+file);
   PlanDataMapper.restore(data);
   byte[] bytes=json.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
   temporary=Files.createTempFile(file.getParent(),"planit-",".tmp");
   try(FileChannel output=FileChannel.open(temporary,StandardOpenOption.WRITE)){
    ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())output.write(buffer);output.force(true);
   }
   replace(temporary,file);expectedDigest=digest(bytes);
  }catch(IOException|SecurityException ex){throw new StorageException("The change could not be saved. Previous saved data is retained. Check free space and permissions, then retry: "+file);}
  finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException ignored){/* A leftover temp file never replaces the save file. */}}
 }
 /** Override only for fault-injection tests. */
 protected void replace(Path temporary,Path destination)throws IOException{
  try{Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
  catch(AtomicMoveNotSupportedException ex){Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING);}
 }
 private byte[] contentsOrNull()throws IOException{
  try{return Files.readAllBytes(file);}catch(NoSuchFileException ex){return null;}
 }
 private static byte[] digest(byte[] bytes){try{return MessageDigest.getInstance("SHA-256").digest(bytes);}catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA-256 unavailable");}}
 private void ensureOpen(){if(closed||lock==null||!lock.isValid())throw new StorageException("Planit's data store is closed.");}
 @Override public synchronized void close(){if(closed)return;closed=true;closeQuietly();}
 private void closeQuietly(){try{if(lock!=null)lock.release();}catch(IOException ignored){}finally{try{if(channel!=null)channel.close();}catch(IOException ignored){}}}
}
