package com.telecominfraproject.wlan.core.model.equipment;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.equipment.NeighborScanPacketType;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum NeighborScanPacketType 
{
   ASSOC_REQ(0),
   ASSOC_RESP(1),
   REASSOC_REQ(2),
   REASSOC_RESP(3),
   PROBE_REQ(4),
   PROBE_RESP(5),
   BEACON(8),
   DISASSOC(10),
   AUTH(11),
   DEAUTH(12),
   ACTION(13),
   ACTION_NOACK(14),
   DATA(100),
   OTHER(200),
   
   UNSUPPORTED(-1);

   private final int id;
   private static final Map<Integer, NeighborScanPacketType> ELEMENTS = new HashMap<>();

   private NeighborScanPacketType(int id)
   {
      this.id = id;      
   }

   public int getId(){
      return this.id;
   }

   public static NeighborScanPacketType getById(int enumId){
      if(ELEMENTS.isEmpty()){
         //initialize elements map
         for(NeighborScanPacketType met : NeighborScanPacketType.values()){
            ELEMENTS.put(met.getId(), met);
         }
      }

      return ELEMENTS.get(enumId);
   }

   @JsonCreator
   public static NeighborScanPacketType getByName(String value) {
       return JsonDeserializationUtils.deserializEnum(value, NeighborScanPacketType.class, UNSUPPORTED);
   }
   
   public static boolean isUnsupported(NeighborScanPacketType value) {
       return (UNSUPPORTED.equals(value));
   }
}
