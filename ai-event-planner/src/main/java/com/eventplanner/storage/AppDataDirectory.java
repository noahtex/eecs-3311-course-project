package com.eventplanner.storage;

import java.nio.file.Path;
import java.util.Locale;

public final class AppDataDirectory {
 private AppDataDirectory(){}
 public static Path resolve(){
  String override=System.getProperty("planit.dataDir");if(override==null||override.isBlank())override=System.getenv("PLANIT_DATA_DIR");
  if(override!=null&&!override.isBlank())return Path.of(override).toAbsolutePath().normalize();
  String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);Path home=Path.of(System.getProperty("user.home"));
  if(os.contains("win")){
   String local=System.getenv("LOCALAPPDATA");return (local==null||local.isBlank()?home.resolve("AppData/Local"):Path.of(local)).resolve("Planit");
  }
  if(os.contains("mac"))return home.resolve("Library/Application Support/Planit");
  String xdg=System.getenv("XDG_DATA_HOME");return (xdg==null||xdg.isBlank()?home.resolve(".local/share"):Path.of(xdg)).resolve("planit");
 }
}
