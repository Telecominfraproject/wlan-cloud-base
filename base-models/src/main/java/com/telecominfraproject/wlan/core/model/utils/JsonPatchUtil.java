package com.telecominfraproject.wlan.core.model.utils;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * 
 * @author erikvilleneuve
 *
 */
public class JsonPatchUtil 
{
   private static final ObjectMapper objectMapper = new ObjectMapper();
   
   /**
    * 
    * This will return a patch (as a string) that will migrate the "from" instance into
    * the "to" instance.
    * 
    * An exception can be thrown if given invalid Json payloads.
    * 
    * @param from
    * @param to
    * @return
    * @throws IOException 
    */
   public static <T extends BaseJsonModel> String generatePatch(T from, T to) throws JsonPatchException
   {
      return generatePatch(from.toString(), to.toString());
   }

   /**
    * Will apply the patch to the base object and result in the final object.
    * 
    * Essentially, you're patching "object" with patch "patch". 
    * 
    * @param object
    * @param patch
    * @param clazz
    * @return
    * @throws Exception
    */
   public static <T extends BaseJsonModel> T apply(BaseJsonModel object, String patch, Class<T> clazz) throws JsonPatchException
   {
      try
      {
         String patchedObject = apply(object.toString(), patch);
         return BaseJsonModel.fromString(patchedObject, clazz);
      }
      catch(Exception e)
      {
         throw new JsonPatchException(e);
      }
   }
   
   
   
   
   /**
    * Our testable utils
    * @param from
    * @param to
    * @return
    * @throws JsonPatchException
    */
   static String generatePatch(String from, String to) throws JsonPatchException
   {
      try
      {
         JsonPatch patch = JsonDiff.asJsonPatch(JsonLoader.fromString(from), JsonLoader.fromString(to));
         return objectMapper.writeValueAsString(patch);
      }
      catch(IOException e)
      {
         throw new JsonPatchException(e);
      }

   }
   
   static String apply(String object, String patch) throws JsonPatchException
   {
      try
      {
         JsonPatch patchObj = JsonPatch.fromJson(JsonLoader.fromString(patch));
         JsonNode patchNode = patchObj.apply(JsonLoader.fromString(object), true);
         return patchNode.toString();
      }
      catch(com.github.fge.jsonpatch.JsonPatchException | IOException e)
      {
         throw new JsonPatchException(e);
      }
      
   }

    private JsonPatchUtil() {
    }

}
