package com.whizcontrol.core.model.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whizcontrol.core.model.testclasses.Vehicle;

public class SerializationMainTest {
    private static final Logger LOG = LoggerFactory.getLogger(SerializationMainTest.class);
    
    private static ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        BaseJsonModel.registerAllSubtypes(objectMapper);
    }
    
    @Test
    public void testGenericResponse(){
        String withNoTypeStr = "{\"message\":\"m123\",\"success\":true}";
        GenericResponse md = BaseJsonModel.fromString(withNoTypeStr, GenericResponse.class);
        String afterDeserialization = "{\"_type\":\"GenericResponse\",\"message\":\"m123\",\"success\":true}";
        assertEquals(afterDeserialization, md.toString());
        
        String withTypeStr = "{\"_type\":\"GenericResponse\",\"message\":\"m123\",\"success\":true}";
        md = BaseJsonModel.fromString(withTypeStr, GenericResponse.class);
        assertEquals(afterDeserialization, md.toString());
    }

    @Test
    public void testBaseModel(){
        GenericResponse g = new GenericResponse();
        g.setMessage("testMessage");
        g.setSuccess(true);
        
        BaseJsonModel m = new TestMetricEvent("test", 42, g );
        String serialiedStr = m.toString();
        
        BaseJsonModel b = BaseJsonModel.fromString(serialiedStr, BaseJsonModel.class);
        
        assertEquals(((TestMetricEvent) m).getIntProp(), ((TestMetricEvent) b).getIntProp());
        assertEquals(((TestMetricEvent) m).getStrProp(), ((TestMetricEvent) b).getStrProp());
        assertEquals(((TestMetricEvent) m).getObjProp().getMessage(), ((TestMetricEvent) b).getObjProp().getMessage());
        assertEquals(((TestMetricEvent) m).getObjProp().isSuccess(), ((TestMetricEvent) b).getObjProp().isSuccess());

    }

    @Test
    public void testObjectMapperWithList() throws Exception {
        String listStr = "[{\"_type\":\"TestMetricEvent\",\"strProp\":\"s1\",\"intProp\":1,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m1\",\"success\":true}},"
                        + "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s2\",\"intProp\":2,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m2\",\"success\":true}}]";
        TypeReference<ArrayList<BaseJsonModel>> t =  new TypeReference<ArrayList<BaseJsonModel>>() { };
        List<TestMetricEvent> list = objectMapper.readValue(listStr, t);
        
        assertEquals(2, list.size());
        
        assertEquals(1, list.get(0).getIntProp());
        assertEquals("s1", list.get(0).getStrProp());
        assertEquals("m1", list.get(0).getObjProp().getMessage());
        assertTrue(list.get(0).getObjProp().isSuccess());
        
        assertEquals(2, list.get(1).getIntProp());
        assertEquals("s2", list.get(1).getStrProp());
        assertEquals("m2", list.get(1).getObjProp().getMessage());
        assertTrue(list.get(1).getObjProp().isSuccess());
    }
    
    @Test
    public void testCollectionsOfMetricsFullTypeDeserialization() throws Exception {
        String str = "{ \"_type\":\"CollectionsOfMetrics\","
                + "\"list\":"
                +       "[{\"_type\":\"TestMetricEvent\",\"strProp\":\"s1\",\"intProp\":1,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m1\",\"success\":true}},"
                +       "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s2\",\"intProp\":2,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m2\",\"success\":true}}],"
                + "\"map\":"
                + "{\"test\":"
                +       "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s3\",\"intProp\":3,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m3\",\"success\":true}}"
                + "}}";
        
        CollectionsOfMetrics c = BaseJsonModel.fromString(str, CollectionsOfMetrics.class);

        //check list content
        List<TestMetricEvent> list = c.getList();
        assertEquals(2, list.size());
        
        assertEquals(1, list.get(0).getIntProp());
        assertEquals("s1", list.get(0).getStrProp());
        assertEquals("m1", list.get(0).getObjProp().getMessage());
        assertTrue(list.get(0).getObjProp().isSuccess());
        
        assertEquals(2, list.get(1).getIntProp());
        assertEquals("s2", list.get(1).getStrProp());
        assertEquals("m2", list.get(1).getObjProp().getMessage());
        assertTrue(list.get(1).getObjProp().isSuccess());
        
        //check map content
        TestMetricEvent t = c.getMap().get("test");
        
        assertEquals(3, t.getIntProp());
        assertEquals("s3", t.getStrProp());
        assertEquals("m3", t.getObjProp().getMessage());
        assertTrue(t.getObjProp().isSuccess());

    }

    @Test
    public void testCollectionsOfMetricsNoTopLevelTypeDeserialization() throws Exception {
        String str = "{ "
                + "\"list\":"
                +       "[{\"_type\":\"TestMetricEvent\",\"strProp\":\"s1\",\"intProp\":1,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m1\",\"success\":true}},"
                +       "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s2\",\"intProp\":2,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m2\",\"success\":true}}],"
                + "\"map\":"
                + "{\"test\":"
                +       "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s3\",\"intProp\":3,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m3\",\"success\":true}}"
                + "}}";
        
        CollectionsOfMetrics c = BaseJsonModel.fromString(str, CollectionsOfMetrics.class);

        //check list content
        List<TestMetricEvent> list = c.getList();
        assertEquals(2, list.size());
        
        assertEquals(1, list.get(0).getIntProp());
        assertEquals("s1", list.get(0).getStrProp());
        assertEquals("m1", list.get(0).getObjProp().getMessage());
        assertTrue(list.get(0).getObjProp().isSuccess());
        
        assertEquals(2, list.get(1).getIntProp());
        assertEquals("s2", list.get(1).getStrProp());
        assertEquals("m2", list.get(1).getObjProp().getMessage());
        assertTrue(list.get(1).getObjProp().isSuccess());
        
        //check map content
        TestMetricEvent t = c.getMap().get("test");
        
        assertEquals(3, t.getIntProp());
        assertEquals("s3", t.getStrProp());
        assertEquals("m3", t.getObjProp().getMessage());
        assertTrue(t.getObjProp().isSuccess());

    }
    
    @Test
    public void testCollectionsOfMetricsNoListTypeDeserialization() throws Exception {
        String str = "{ "
                + "\"list\":"
                +       "[{\"strProp\":\"s1\",\"intProp\":1,\"objProp\":{\"message\":\"m1\",\"success\":true}},"
                +       "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s2\",\"intProp\":2,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m2\",\"success\":true}}],"
                + "\"map\":"
                + "{\"test\":"
                +       "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s3\",\"intProp\":3,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m3\",\"success\":true}}"
                + "}}";
        
        CollectionsOfMetrics c = BaseJsonModel.fromString(str, CollectionsOfMetrics.class);

        //check list content
        List<TestMetricEvent> list = c.getList();
        assertEquals(2, list.size());
        
        assertEquals(1, list.get(0).getIntProp());
        assertEquals("s1", list.get(0).getStrProp());
        assertEquals("m1", list.get(0).getObjProp().getMessage());
        assertTrue(list.get(0).getObjProp().isSuccess());
        
        assertEquals(2, list.get(1).getIntProp());
        assertEquals("s2", list.get(1).getStrProp());
        assertEquals("m2", list.get(1).getObjProp().getMessage());
        assertTrue(list.get(1).getObjProp().isSuccess());
        
        //check map content
        TestMetricEvent t = c.getMap().get("test");
        
        assertEquals(3, t.getIntProp());
        assertEquals("s3", t.getStrProp());
        assertEquals("m3", t.getObjProp().getMessage());
        assertTrue(t.getObjProp().isSuccess());

    }
    
    @Test
    public void testCollectionsOfMetricsNoTypeDeserialization() throws Exception {
        String str = "{ "
                + "\"list\":"
                +       "[{\"strProp\":\"s1\",\"intProp\":1,\"objProp\":{\"message\":\"m1\",\"success\":true}},"
                +       " {\"strProp\":\"s2\",\"intProp\":2,\"objProp\":{\"message\":\"m2\",\"success\":true}}],"
                + "\"map\":"
                + "{\"test\":"
                +       "{\"strProp\":\"s3\",\"intProp\":3,\"objProp\":{\"message\":\"m3\",\"success\":true}}"
                + "}}";
        
        CollectionsOfMetrics c = BaseJsonModel.fromString(str, CollectionsOfMetrics.class);

        //check list content
        List<TestMetricEvent> list = c.getList();
        assertEquals(2, list.size());
        
        assertEquals(1, list.get(0).getIntProp());
        assertEquals("s1", list.get(0).getStrProp());
        assertEquals("m1", list.get(0).getObjProp().getMessage());
        assertTrue(list.get(0).getObjProp().isSuccess());
        
        assertEquals(2, list.get(1).getIntProp());
        assertEquals("s2", list.get(1).getStrProp());
        assertEquals("m2", list.get(1).getObjProp().getMessage());
        assertTrue(list.get(1).getObjProp().isSuccess());
        
        //check map content
        TestMetricEvent t = c.getMap().get("test");
        
        assertEquals(3, t.getIntProp());
        assertEquals("s3", t.getStrProp());
        assertEquals("m3", t.getObjProp().getMessage());
        assertTrue(t.getObjProp().isSuccess());

    }   
    
    @Test
    public void testCollectionsOfMetricsFullSerialization() throws Exception {
        String str = "{\"_type\":\"CollectionsOfMetrics\","
                + "\"list\":"
                +       "[{\"_type\":\"TestMetricEvent\",\"strProp\":\"s1\",\"intProp\":1,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m1\",\"success\":true}},"
                +       "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s2\",\"intProp\":2,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m2\",\"success\":true}}],"
                + "\"map\":"
                + "{\"test\":"
                +       "{\"_type\":\"TestMetricEvent\",\"strProp\":\"s3\",\"intProp\":3,\"objProp\":{\"_type\":\"GenericResponse\",\"message\":\"m3\",\"success\":true}}"
                + "}}";

        GenericResponse g1 = new GenericResponse();
        g1.setMessage("m1");
        g1.setSuccess(true);
        TestMetricEvent t1 = new TestMetricEvent("s1", 1, g1 );

        GenericResponse g2 = new GenericResponse();
        g2.setMessage("m2");
        g2.setSuccess(true);
        TestMetricEvent t2 = new TestMetricEvent("s2", 2, g2 );

        GenericResponse g3 = new GenericResponse();
        g3.setMessage("m3");
        g3.setSuccess(true);
        TestMetricEvent t3 = new TestMetricEvent("s3", 3, g3 );

        CollectionsOfMetrics con = new CollectionsOfMetrics();
        con.getList().add(t1);
        con.getList().add(t2);
        con.getMap().put("test", t3);
        
        CollectionsOfMetrics c = BaseJsonModel.fromString(str, CollectionsOfMetrics.class);
        assertNotNull(c);
        
        assertEquals(str, objectMapper.writeValueAsString(con));
        
    }
    
    @Test
    public void testListSerialization() throws Exception {
        TypeReference<ArrayList<BaseJsonModel>> tList =  new TypeReference<ArrayList<BaseJsonModel>>() { };
        List<TestMetricEvent> rawListOfMetrics = new ArrayList<>();
        rawListOfMetrics.add(new TestMetricEvent());
        String rawListStr = objectMapper.writerWithType(tList).writeValueAsString(rawListOfMetrics);
        
        ListOfMetrics listOfMetrics = new ListOfMetrics();
        listOfMetrics.add(new TestMetricEvent());
        String listWithTypeStr = objectMapper.writeValueAsString(listOfMetrics);
        
        assertEquals(rawListStr, listWithTypeStr);
    }

    @Test
    public void testMapSerialization() throws Exception {
        TypeReference<HashMap<String, TestMetricEvent>> tMap =  new TypeReference<HashMap<String, TestMetricEvent>>() { };
        Map<String, TestMetricEvent> rawMapOfMetrics = new HashMap<>();
        rawMapOfMetrics.put("test", new TestMetricEvent());
        String rawMapStr = objectMapper.writerWithType(tMap).writeValueAsString(rawMapOfMetrics);
        
        MapOfMetrics mapOfMetrics = new MapOfMetrics();
        mapOfMetrics.put("test", new TestMetricEvent());
        String mapWithTypeStr = objectMapper.writeValueAsString(mapOfMetrics);
        
        assertEquals(rawMapStr, mapWithTypeStr);
    }
    
    @Test
    public void testZipUnzip() throws Exception {
        GenericResponse g1 = new GenericResponse();
        g1.setMessage("m1");
        g1.setSuccess(true);
        TestMetricEvent t1 = new TestMetricEvent("s1", 1, g1 );

        GenericResponse g2 = new GenericResponse();
        g2.setMessage("m2");
        g2.setSuccess(true);
        TestMetricEvent t2 = new TestMetricEvent("s2", 2, g2 );

        GenericResponse g3 = new GenericResponse();
        g3.setMessage("m3");
        g3.setSuccess(true);
        TestMetricEvent t3 = new TestMetricEvent("s3", 3, g3 );

        CollectionsOfMetrics con = new CollectionsOfMetrics();
        con.getList().add(t1);
        con.getList().add(t2);
        con.getMap().put("test", t3);
        
        String jsonStr = con.toString();
        byte[] zippedJson = con.toZippedBytes();
        
        LOG.debug("String len : {} Bytes len : {}", jsonStr.length(), zippedJson.length);
        
        CollectionsOfMetrics restoredCon = CollectionsOfMetrics.fromZippedBytes(zippedJson, CollectionsOfMetrics.class);
        assertNotNull(restoredCon);
        
        String restoredJsonStr = restoredCon.toString();
        
        assertEquals(jsonStr, restoredJsonStr);

    }
    
    @Test
    public void testExtractClass() throws Exception
    {
        Vehicle car = new Vehicle();
        car.setColour("Pink");
        car.setName("My car!");
        car.setNumKm(1000);
        
        String serialized = car.toString();
        
        Class<?> carClazz = BaseJsonModel.extractClass(serialized);
        assertNotNull(carClazz);
        assertEquals(Vehicle.class, carClazz);
    }
    

    /**
     * This is a method to play with - it shows how to properly deal with serialization of containers, like List and Map
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
            
        TypeReference<ArrayList<BaseJsonModel>> tList =  new TypeReference<ArrayList<BaseJsonModel>>() { };
        List<TestMetricEvent> rawListOfMetrics = new ArrayList<>();
        rawListOfMetrics.add(new TestMetricEvent());
        
        System.out.println("Serialized raw list       : " + objectMapper.writeValueAsString(rawListOfMetrics));
        System.out.println("Serialized list with type : " + objectMapper.writerWithType(tList).writeValueAsString(rawListOfMetrics));
        
        ListOfMetrics listOfMetrics = new ListOfMetrics();
        listOfMetrics.add(new TestMetricEvent());
        System.out.println("Serialized ListOfMetrics  : " + objectMapper.writeValueAsString(listOfMetrics));
        
        TypeReference<HashMap<String, TestMetricEvent>> tMap =  new TypeReference<HashMap<String, TestMetricEvent>>() { };
        Map<String, TestMetricEvent> rawMapOfMetrics = new HashMap<>();
        rawMapOfMetrics.put("test", new TestMetricEvent());
        System.out.println("Serialized raw map           : " + objectMapper.writeValueAsString(rawMapOfMetrics));
        System.out.println("Serialized raw map with type : " + objectMapper.writerWithType(tMap).writeValueAsString(rawMapOfMetrics));
        
        MapOfMetrics mapOfMetrics = new MapOfMetrics();
        mapOfMetrics.put("test", new TestMetricEvent());
        System.out.println("Serialized MapOfMetrics      : " + objectMapper.writeValueAsString(mapOfMetrics));
        
    }
    
    public static class TestMetricEvent extends BaseJsonModel{
        private static final long serialVersionUID = 1L;
        
        private String strProp;
        private int intProp;
        private GenericResponse objProp;
        
        public TestMetricEvent() {
        }
        
        public TestMetricEvent(String strProp, int intProp, GenericResponse objProp) {
            this.strProp = strProp;
            this.intProp = intProp;
            this.objProp = objProp;
        }

        public String getStrProp() {
            return strProp;
        }
        public void setStrProp(String strProp) {
            this.strProp = strProp;
        }
        public int getIntProp() {
            return intProp;
        }
        public void setIntProp(int intProp) {
            this.intProp = intProp;
        }
        public GenericResponse getObjProp() {
            return objProp;
        }
        public void setObjProp(GenericResponse objProp) {
            this.objProp = objProp;
        }
        
    }
    
    public static class ListOfMetrics extends ArrayList<TestMetricEvent>{
        private static final long serialVersionUID = 1L;
    }

    public static class MapOfMetrics extends HashMap<String, TestMetricEvent>{
        private static final long serialVersionUID = 1L;
    }
    
    public static class CollectionsOfMetrics extends BaseJsonModel{
        private static final long serialVersionUID = 1L;

        List<TestMetricEvent> list = new ArrayList<>();
        Map<String, TestMetricEvent> map = new HashMap<>();
        public List<TestMetricEvent> getList() {
            return list;
        }
        public void setList(List<TestMetricEvent> list) {
            this.list = list;
        }
        public Map<String, TestMetricEvent> getMap() {
            return map;
        }
        public void setMap(Map<String, TestMetricEvent> map) {
            this.map = map;
        }
        
    }
}
