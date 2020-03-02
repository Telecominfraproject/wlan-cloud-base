package com.telecominfraproject.wlan.core.model.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.testclasses.Vehicle;
import com.telecominfraproject.wlan.core.model.utils.JsonPatchUtil;

@RunWith(Parameterized.class)
/**
 * json-patch has a slew of unit tests.
 * 
 * These tests are just to increase our warm-and-fuzzy feelings about using it as a dependency.
 * 
 * 
 * @author erikvilleneuve
 *
 */
public class JsonPatchUtilTests 
{
   private static final Logger LOG = LoggerFactory.getLogger(JsonPatchUtilTests.class);
   
   @Parameters
   /**
    * These are all the 2-tuples we'll test against.
    * 
    * Any additional cases should be added here! 
    * 
    */
   public static Collection<Object[]> data() 
   {
       return Arrays.asList(new Object[][] {     
                { generateVehicle("black", "stinger", 100, "John", "Mary"),  generateVehicle("blue", "stinger", 200, "Mary", "Bob") },  
                { generateVehicle("black", "Kadilactica", 100, "John", "Mary"),  generateVehicle("blue", "stinger", 200) },  
                { generateVehicle("black", "stinger", 100),  generateVehicle("blue", "stinger", 50, "Bob") },  
                { generateVehicle("black", "stinger", 100, "John", "Mary"),  generateVehicle("black", "stinger", 100, "John", "Mary") }  
          });
   }

   
   
   private Vehicle left;
   private Vehicle right;
   
   public JsonPatchUtilTests(Vehicle left, Vehicle right)
   {
      this.left = left;
      this.right = right;
   }
   
   @Test
   public void testLeftToRight() throws Exception
   {
      assertWeCanGoFromLeftToRight(left, right);
   }

   @Test
   public void testRightToLeft() throws Exception
   {
      assertWeCanGoFromLeftToRight(right, left);
   }
   

   private static void assertWeCanGoFromLeftToRight(Vehicle left, Vehicle right) throws Exception 
   {
      String patch = JsonPatchUtil.generatePatch(left, right);
      LOG.debug("Generated patch {}", patch);
      
      Vehicle patched = JsonPatchUtil.apply(left, patch, Vehicle.class);

      assertEquals(right, patched);
   }

   private static Vehicle generateVehicle(String color, String name, int numKm, String ... people)
   {
      Vehicle returnValue = new Vehicle();
      returnValue.setColour(color);
      returnValue.setName(name);
      returnValue.setNumKm(numKm);
      
      List<String> peopleAllow = new ArrayList<>();
      for(String person : people)
      {
         peopleAllow.add(person);
      }
      
      returnValue.setPeopleOnTheLease(peopleAllow);
      
      return returnValue;
   }

}
