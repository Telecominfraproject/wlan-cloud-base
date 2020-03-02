package com.telecominfraproject.wlan.core.model.utils;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.utils.JWTUtil;

public class JWTUtilTests 
{
   
   private static String JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImF6cCI6Im15bGl0dGxlY2xpZW50aWQifQ.nauJ6jNIFDGrxHPwS5iKdgS-7IdxT7ZMe6wTnZoXqHk";
   private static final String expectedClientId = "mylittleclientid";
   
   @Test
   public void testGetClientId() throws Exception {
       Map<String, Object> jsonMap = JWTUtil.getJWTBodyAsJSON(JWT);
       assertEquals(expectedClientId, (String)jsonMap.get(JWTUtil.JWT_CLIENT_ID_KEY));
   }
}
