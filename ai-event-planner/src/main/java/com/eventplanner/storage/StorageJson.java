package com.eventplanner.storage;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.IOException;
import java.time.*;

/** Strict ISO Java-time support using the project's existing Jackson dependency. */
final class StorageJson {
 private StorageJson(){}
 static ObjectMapper mapper(){
  var times=new SimpleModule();times.addSerializer(LocalDateTime.class,ToStringSerializer.instance);times.addSerializer(Instant.class,ToStringSerializer.instance);
  times.addDeserializer(LocalDateTime.class,new JsonDeserializer<>(){
   @Override public LocalDateTime deserialize(JsonParser p,DeserializationContext context)throws IOException{
    if(!p.hasToken(JsonToken.VALUE_STRING))throw JsonMappingException.from(p,"Expected ISO local date/time.");
    try{return LocalDateTime.parse(p.getText());}catch(DateTimeException ex){throw JsonMappingException.from(p,"Invalid local date/time.");}
   }
  });
  times.addDeserializer(Instant.class,new JsonDeserializer<>(){
   @Override public Instant deserialize(JsonParser p,DeserializationContext context)throws IOException{
    if(!p.hasToken(JsonToken.VALUE_STRING))throw JsonMappingException.from(p,"Expected ISO instant.");
    try{return Instant.parse(p.getText());}catch(DateTimeException ex){throw JsonMappingException.from(p,"Invalid instant.");}
   }
  });
  return com.fasterxml.jackson.databind.json.JsonMapper.builder().addModule(times)
   .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
   .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).build().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
 }
}
