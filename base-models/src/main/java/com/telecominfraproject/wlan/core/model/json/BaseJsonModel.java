package com.telecominfraproject.wlan.core.model.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.telecominfraproject.wlan.server.exceptions.SerializationException;

/**
 * All sub-classes are registered automatically with the static
 * {@link ObjectMapper} as long as they are in "com.telecominfraproject.wlan" package 
 * or in one of the packages specified by the system property tip.wlan.vendorTopLevelPackages (comma-separated) . 
 * The resulting JSON document will have "model_type: " SimpleClassName" to help with
 * deserialization.
 * <p>
 * All subclass should implement {@link #clone()} {link
 * {@link #hasUnsupportedValue()}.
 * <p>
 * The enumerated field should implement method annotated with
 * {@link JsonCreator} to detect unknown value as result of deserialization.
 *
 */
@JsonSerialize()
@JsonTypeResolver(BaseJsonTypeResolverBuilder.class)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "model_type", visible = true)
public abstract class BaseJsonModel implements Cloneable, Serializable {
    
    private static final long serialVersionUID = -1343089800191978867L;
    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY_WRITTER;

    private static final Logger LOG = LoggerFactory.getLogger(BaseJsonModel.class);

    static {
        registerAllSubtypes(MAPPER);
        PRETTY_WRITTER = MAPPER.writer().withDefaultPrettyPrinter();
    }
    
    /**
     * For NoSQL databases we do not use synthetic keys, but they are still part of the object model.
     * This method allows us to calculate a value of synthetic key from two long elements, for example equipmentId and timestamp.
     * Synthetic key is calculated using this formula: key = part1 &lt;&lt; 42 + timestamp.
     * We should be good until year 2109 (last 42 bits) and we would have a range for 4M (first 22 bits) part1 keys (devices, etc.)
     * 
     * @param part1
     * @param timestamp
     * @return key
     */
    public static long calculateSyntheticKey(long part1, long timestamp){
        return part1 << 42 | timestamp;
    }
    
    /**
     * Check if a given class is annotated correctly to handle unknown {@link Enumeration} value.
     * For enumerated value, all class should have {@link JsonEnumDefaultValue}
     * 
     * @param clazz
     * @param throwException
     * @param logger 
     */
    public static void checkUnknownEnumForClass(Class<? extends BaseJsonModel> clazz, final Boolean throwException, Logger logger) {
        boolean enableException = !Boolean.FALSE.equals(throwException);
        Logger msgLogger = (null == logger) ? LOG : logger;

        // skip abstract class
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return;
        }
        Map<String, String> fieldMap = new HashMap<>();
        Method[] methodList = clazz.getMethods();
        for (Method method : methodList) {
            if (!method.getName().startsWith("set")) {
                continue;
            }
            // marked with JsonIgnore, it could be ignored because getter
            // is ignored
            if (method.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            // take more than one parameter
            Class<?>[] parmList = method.getParameterTypes();
            if (parmList.length != 1) {
                continue;
            }
            // setter for enum
            if (parmList[0].isEnum()) {
                fieldMap.put(method.getName(), generateBadEnumField(method.getName()));
                continue;
            }
            // setter for JsonBaseModel
            if (BaseJsonModel.class.isAssignableFrom(parmList[0])) {
                String fieldDoc = generateBadJsonField(method.getName(), parmList[0]);
                if (null != fieldDoc) {
                    fieldMap.put(method.getName(), fieldDoc);
                }
                continue;
            }
            if (Collection.class.isAssignableFrom(parmList[0])) {
                // setter for generic type
                Type[] types = method.getGenericParameterTypes();
                if (types.length != 1) {
                    continue;
                }
                if (! (types[0] instanceof ParameterizedType)) {
                    continue;
                }
                ParameterizedType pType = (ParameterizedType) types[0];
                Type[] argTypes = pType.getActualTypeArguments();
                if (argTypes.length != 1) {
                    continue;
                }
                Class<?> parmClazz;
                if (argTypes[0] instanceof ParameterizedType) {
                    ParameterizedType targetType = (ParameterizedType) argTypes[0];
                    parmClazz = (Class<?>) targetType.getRawType();
                }
                else if(argTypes[0] instanceof TypeVariable) {
                    TypeVariable<?> targetType = (TypeVariable<?>) argTypes[0];
                    parmClazz = (Class<?>) targetType.getGenericDeclaration();
                }
                else{
                    parmClazz = (Class<?>) argTypes[0];
                }
                if (BaseJsonModel.class.isAssignableFrom(parmClazz)) {
                    String fieldDoc = generateBadJsonCollectionField(method.getName(), parmClazz);
                    if (null != fieldDoc) {
                        fieldMap.put(method.getName(), fieldDoc);
                    }
                }
            }
            
        }
        if (fieldMap.isEmpty()) {
            return;
        }

        // generate a json document with enum fields
        for (Entry<String, String> field : fieldMap.entrySet()) {
            try {
                String jsonStr = generateTestJson(clazz, field.getValue());
                BaseJsonModel result = BaseJsonModel.fromString(jsonStr, clazz);
                if (!result.hasUnsupportedValue()) {
                    throw new SerializationException(clazz.getSimpleName() + "." + field.getKey() +
                            "(BAD_ENUM_VALUE) failed hasUnsupportedValue test. JSON: "
                            + jsonStr + ", RESULT: " + result.toString());
                }
                msgLogger.debug("Class {} passed unknown enum test from {}, result: {}", clazz.getSimpleName(), jsonStr,
                        result);
            } catch (Exception e) {
                msgLogger.info("Failed to handle unknown enum in {}: {}", clazz.getName(), e.getLocalizedMessage());
                if (enableException) {
                    throw e;
                }
            }
        }
    }

    
    /**
     * For NoSQL databases we do not use synthetic keys, but they are still part of the object model.
     * This method allows us to restore values of two long elements (for example equipmentId and timestamp) from which supplied synthetic key was created.
     * Synthetic key is calculated using this formula: key = part1 << 42 + timestamp.
     * We should be good until year 2109 (last 42 bits) and we would have a range for 4M (first 22 bits) part1 keys (devices, etc.)
     * 
     * @param key
     * @return array of two long numbers decoded from the supplied key, first one represents part1 key (max value 4194303), second one represents timestamp (max value May 15 2109)
     */
    public static long[] decodeSyntheticKey(long key){
        long ret[] = new long[2];
        
        ret[0] = key >> 42 & 0x3FFFFFL;
        ret[1] = key & 0x3FFFFFFFFFFL;
        
        return ret;
    }
    
    public static final String vendorTopLevelPackages = System.getProperty("tip.wlan.vendorTopLevelPackages", "");
    public static Reflections getReflections() {
        //scan urls that contain 'com.telecominfraproject.wlan' and vendor-specific top level packages, use the default scanners

        List<URL> urls =  new ArrayList<>();
        urls.addAll(ClasspathHelper.forPackage("com.telecominfraproject.wlan"));
        
        List<String> pkgs = new ArrayList<>();
        pkgs.add("com.telecominfraproject.wlan");
        
        //add vendor packages
        if(vendorTopLevelPackages!=null) {
            String[] vendorPkgs = vendorTopLevelPackages.split(",");
            for(int i=0; i< vendorPkgs.length; i++) {
                if(vendorPkgs[i].trim().isEmpty()) {
                    continue;
                }
                
                urls.addAll(ClasspathHelper.forPackage(vendorPkgs[i]));
                pkgs.add(vendorPkgs[i]);
                
                LOG.info("Registered package {} with BaseJsonModel", vendorPkgs[i]);
            }
        }
                
        Reflections reflections =   new Reflections(new ConfigurationBuilder()
                .filterInputsBy(new FilterBuilder().includePackage(pkgs.toArray(new String[0])))
                .setUrls(urls)
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner() ));
     

        return reflections;
    }
    
    public static Class<?> extractClass(String jsonStr)
    {
        try {
            JsonNode topNode = MAPPER.readTree(jsonStr);
            JsonNode typeNode = topNode.get("model_type");

            Set<Class<? extends BaseJsonModel>> classes = getReflections().getSubTypesOf(BaseJsonModel.class);

            for (Class<? extends BaseJsonModel> subClazz : classes) {
                if (Objects.equals(subClazz.getSimpleName(), typeNode.textValue())) {
                    return subClazz;
                }
            }

            return null;
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    
    /**
     * For unit tests, I'm LOVIN' this method. This simply loads a serialized 
     * objects from a file. Big, bang, boom.
     * 
     * @param filename
     * @param clazz
     * @return object
     * 
     * @throws IOException 
     */
    public static <T extends BaseJsonModel> T fromFile(String filename, Class<T> clazz) throws IOException
    {
       String jsonPayload = com.google.common.io.Files.toString(new File(filename), Charset.defaultCharset());
       return fromString(jsonPayload, clazz);
    }
    
    public static <T extends BaseJsonModel> T fromString(String jsonStr, Class<T> valueType){
        try {
            return MAPPER.readValue(jsonStr, valueType);
        } catch (JsonParseException e) {
            throw new SerializationException(e);
        } catch (JsonMappingException e) {
            throw new SerializationException(e);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    public static <T extends BaseJsonModel> T fromZippedBytes(byte[] zippedBytes, Class<T> valueType){
        
        String partContent = fromZippedBytesAsString(zippedBytes);
        if(partContent != null && !partContent.isEmpty()){
            return fromString(partContent, valueType);
        } else {
            return null;
        }
    }

    public static String fromZippedBytesAsString(byte[] zippedBytes){
        if(zippedBytes == null || zippedBytes.length == 0){
            return null;
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(zippedBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        ZipInputStream zis = new ZipInputStream(bais);
        ZipEntry ze;
        String entryName;
        String partContent = null;
        
        try{

            if((ze=zis.getNextEntry())!=null){
                entryName = ze.getName();
                
                LOG.trace("Processing zipped entry {}", entryName);
                
                copyStream(zis, baos, ze);
                baos.flush();
                partContent = baos.toString("UTF-8");
                
                LOG.trace("Entry text: {}", partContent);
                
            }
            
            zis.close();
        } catch (IOException ex) {
            LOG.info("Error readind zipped json model {} ", ex);
            throw new SerializationException("Error readind zipped json model {} ", ex);
        }

        return partContent;
    }
    
    public static boolean hasUnsupportedValue(BaseJsonModel value) {
        if (null == value) {
            return false;
        }
        return value.hasUnsupportedValue();
    }
    
    
    public static <T extends BaseJsonModel> boolean hasUnsupportedValue(Collection<T> values) {
        if (null == values || values.isEmpty()) {
            return false;
        }
        for (T v : values) {
            if (v.hasUnsupportedValue()) {
                return true;
            }
        }
        return false;
    }
    
    public static <K, T extends BaseJsonModel> boolean hasUnsupportedValue(Map<K, T> value) {
        if (null == value) {
            return false;
        }
        if (hasUnsupportedValue(value.values())) {
            return true;
        }
        return false;
    }
    
    public static <T extends BaseJsonModel> List<T> listFromFile(String filename, Class<T> clazz) throws IOException
    {
       String jsonPayload = com.google.common.io.Files.toString(new File(filename), Charset.defaultCharset());
       return listFromString(jsonPayload, clazz);
    }
    
    public static void main(String[] args) {
        long one = 1;
        long maxEncodable = 0x3FFFFFL;
        
        long ts = System.currentTimeMillis();
        long maxDate = 0x3FFFFFFFFFFL;
        
        long decoded[] = decodeSyntheticKey(calculateSyntheticKey(one, ts));

        System.out.println("Normal");
        System.out.format("original %d %d %n", one, ts);
        System.out.format("decoded %d %d %n", decoded[0], decoded[1]);

        System.out.println("Max part1");
        decoded = decodeSyntheticKey(calculateSyntheticKey(maxEncodable, ts));
        
        System.out.format("original %d %d %n", maxEncodable, ts);
        System.out.format("decoded %d %d %n", decoded[0], decoded[1]);

        System.out.println("Max date "+ new Date(maxDate));
        decoded = decodeSyntheticKey(calculateSyntheticKey(maxEncodable, maxDate));
        
        System.out.format("original %d %d %n", maxEncodable, maxDate);
        System.out.format("decoded %d %d %n", decoded[0], decoded[1]);

        System.out.println("Overflow for part1");
        decoded = decodeSyntheticKey(calculateSyntheticKey(maxEncodable+1, ts));
        
        System.out.format("original %d %d %n", maxEncodable+1, ts);
        System.out.format("decoded %d %d %n", decoded[0], decoded[1]);

        System.out.println("Overflow for timestamp");
        decoded = decodeSyntheticKey(calculateSyntheticKey(maxEncodable, maxDate+1));
        
        System.out.format("original %d %d %n", maxEncodable, maxDate+1);
        System.out.format("decoded %d %d %n", decoded[0], decoded[1]);

        System.out.println("Small number for timestamp");
        decoded = decodeSyntheticKey(calculateSyntheticKey(49961, 0));
        
        System.out.format("original %d %d %n", 49961, 0);
        System.out.format("decoded %d %d %n", decoded[0], decoded[1]);

    }
    
    public static void registerAllSubtypes(ObjectMapper objectMapper){
        //register all subclasses of BaseJsonModel - required by deserializers.
        //without this @JsonSubTypes annotation has to be used, which would 
        //introduce circular dependency between all model projects
        
        //SubTypesScanner
        Set<Class<? extends BaseJsonModel>> modelClasses = 
            getReflections().getSubTypesOf(BaseJsonModel.class);

        for(Class<? extends BaseJsonModel> c : modelClasses){
            objectMapper.registerSubtypes(new NamedType(c, c.getSimpleName()));
        }

    }
    
    public static byte[] toZippedBytesFromString(String str){
        if(str == null){
            return null;
        }
        
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
            ZipOutputStream zos = new ZipOutputStream(baos);
            
            String entryName = "a";
    
            byte[] jsonStringBytes = str.getBytes(StandardCharsets.UTF_8);
            ZipEntry entry = new ZipEntry(entryName);
            entry.setMethod(ZipEntry.DEFLATED);
            
            zos.putNextEntry(entry);               
            zos.write(jsonStringBytes);
            zos.closeEntry();
    
            zos.flush();
            zos.close();
    
            baos.flush();
            byte[] zippedBytes = baos.toByteArray();        
            
            return zippedBytes;
        } catch (Exception e){
            throw new SerializationException(e);
        }
        
    }

    
    private static void copyStream(InputStream in, OutputStream out, ZipEntry entry) throws IOException {
        byte[] buffer = new byte[1024 * 8];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
    }

    
    private static String generateBadEnumField(String setMethod) {
        StringBuilder sb = new StringBuilder();
        sb.append('\"').append(Character.toLowerCase(setMethod.charAt(3))).append(setMethod.substring(4)).append("\":");
        sb.append("\"BAD_ENUM_VALUE\"");
        return sb.toString();
    }

    private static String generateBadJsonField(String setMethod, Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            // location the first sub class has enum if this class is abstract
            Reflections reflections = new Reflections(clazz.getPackage().getName());

            // SubTypesScanner
            Set<?> modelClasses = reflections.getSubTypesOf(clazz);

            for (Object c : modelClasses) {
                if (c instanceof Class) {
                    Class<?> subClazz = (Class<?>) c; 
                    String subJsonStr = generateBadJsonField(setMethod, subClazz);
                    if (null != subJsonStr) {
                        return subJsonStr;
                    }
                }
            }
            return null;
        }
        Method[] methodList = clazz.getMethods();
        for (Method method : methodList) {
            if (!method.getName().startsWith("set")) {
                continue;
            }
            if (method.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            // take more than one parameter
            Class<?>[] parmList = method.getParameterTypes();
            if (parmList.length != 1) {
                continue;
            }
            // setter for enum
            if (parmList[0].isEnum()) {
                String result = generateTestJson(clazz, generateBadEnumField(method.getName()));
                if (null != setMethod) {
                    StringBuilder sb = new StringBuilder();
                    sb.append('\"').append(Character.toLowerCase(setMethod.charAt(3))).append(setMethod.substring(4))
                            .append("\":").append(result);
                    result = sb.toString();
                }
                return result;
            }
        }
        return null;
    }
    
    private static String generateBadJsonCollectionField(String setMethod, Class<?> clazz) {
        // skip abstract class
        String result = generateBadJsonField(null, clazz);

        if (null != result) {
            StringBuilder sb = new StringBuilder();
            if (null != setMethod) {
                sb.append('\"').append(Character.toLowerCase(setMethod.charAt(3))).append(setMethod.substring(4))
                        .append("\":");
            }
            sb.append("[").append(result).append(']');
            result = sb.toString();
        }
        return result;
    }
    
    private static String generateTestJson(Class<?> clazz, String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"_type\":\"").append(clazz.getSimpleName()).append('\"').append(',').append(field).append('}');
        return sb.toString();
    }

    public static <T extends BaseJsonModel> List<T> listFromString(String jsonStr, Class<T> valueType){
        try {
            return MAPPER.readValue(jsonStr, new TypeReference<ArrayList<T>>() { });
            
        } catch (JsonParseException e) {
            throw new SerializationException(e);
        } catch (JsonMappingException e) {
            throw new SerializationException(e);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public Object clone() {        
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone "+ this, e);
        }
    }

    /**
     * Check if this object has unsupported enumeration value.
     * 
     * Test all {@link Enumeration} and {@link BaseJsonModel} members.
     * <p>
     * <blockquote>
     * 
     * <pre>
     *   if (super.hasUnsupportedValue()) {
     *       return true;
     *   }
     *   
     *   if (EnumType.isUnsupported(enumValue) || hasUnsupportedValue(baseJson)) {
     *       return true;
     *   }
     *   return false;
     * }
     * </pre>
     * 
     * </blockquote>
     * </p>
     *
     * All {@link Enumeration} used as member should support parsing unknown
     * value using {@link JsonCreator}.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * enum EnumType {
     *     a, b, UNSUPPORTED;
     * 
     *     &#64;JsonCreator
     *     public static EnumType getByName(String value) {
     *         return JsonDeserializationUtils.deserializEnum(value, EnumType.class, UNSUPPORTED);
     *     }
     *
     *     public static boolean isUnsupported(EnumType value) {
     *         return (UNSUPPORTED.equals(value));
     *     }
     * }
     * 
     * </pre>
     * 
     * </blockquote>
     * </p>
     * 
     * @return true if it does
     */
    public boolean hasUnsupportedValue() {
        return false;
    }

    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }
    
    public String toPrettyString() {
        try {
            return PRETTY_WRITTER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    public byte[] toZippedBytes(){
        return toZippedBytesFromString(toString());
    }
    
    public static String toJsonString(final Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    public static String toPrettyJsonString(final Object value) {
        try {
            return PRETTY_WRITTER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }
}
