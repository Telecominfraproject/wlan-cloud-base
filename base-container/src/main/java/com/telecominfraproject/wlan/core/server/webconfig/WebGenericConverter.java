/**
 * 
 */
package com.telecominfraproject.wlan.core.server.webconfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;

import com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtoptygin
 *
 */
public class WebGenericConverter implements GenericConverter {
    private static final Logger LOG = LoggerFactory.getLogger(WebGenericConverter.class);
    private Set<ConvertiblePair> convertiblePairs;
    
    private EnumWithIdConverterFactory enumWithIdConverterFactory = new EnumWithIdConverterFactory();

    /**
     * Format the typeDescriptor for logging
     * @param typeDescriptor
     * @return
     */
    public static String getDataType(TypeDescriptor typeDescriptor) {
        if (typeDescriptor == null) {
            return "";
        }
        if (typeDescriptor.isCollection()) {
            return typeDescriptor.getName() + "<" + getDataType(typeDescriptor.getElementTypeDescriptor()) + ">";
        }
        if (typeDescriptor.isArray()) {
            return getDataType(typeDescriptor.getElementTypeDescriptor()) + "[]";
        }
        return typeDescriptor.getName();
    }

    /**
     * Constructor
     */
    public WebGenericConverter() {
        convertiblePairs = new HashSet<>();
        convertiblePairs.add(new ConvertiblePair(String.class, BaseJsonModel.class));
        convertiblePairs.add(new ConvertiblePair(String.class, EnumWithId.class));
        convertiblePairs.add(new ConvertiblePair(String.class, List.class));
        convertiblePairs.add(new ConvertiblePair(String.class, Set.class));
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return convertiblePairs;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        // dtop: this is ugly and needs to be generalized, but it works for
        // List/Set of Integer/Long/BaseJsonModel/String
        if (LOG.isTraceEnabled()) {
            LOG.trace("Attempting to convert '{}' from {} to {}", source, getDataType(sourceType),
                    getDataType(targetType));
        }

        Object ret = null;
        if (targetType.isCollection()) {
            if (targetType.getName().equals(List.class.getName())) {
                ret = new ArrayList();
            } else if (targetType.getName().equals(Set.class.getName())) {
                ret = new HashSet();
            } else {
                throw new IllegalStateException("Unsupported collection type " + targetType.getName());
            }
        }

        if (targetType.isAssignableTo(TypeDescriptor.valueOf(String.class)) || targetType
                .isAssignableTo(TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(String.class)))) {
            LOG.trace("Proceeding with conversion of String ... ");
            
            if (targetType.isCollection()) {

                if (sourceType.isArray() || sourceType.isCollection()) {
                    for (Object obj : (Iterable<Object>) source) {
                        ((Collection) ret).add((String) obj);
                    }
                } else {
                    for (String str : ((String) source).split(",")) {
                        str = str.trim();
                        if (str.isEmpty()) {
                            continue;
                        }
                        ((Collection) ret).add(str);
                    }
                }
            }

            return ret;
        }

        if (targetType.isAssignableTo(TypeDescriptor.valueOf(Integer.class)) || targetType
                .isAssignableTo(TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(Integer.class)))) {
            LOG.trace("Proceeding with conversion of Integer ... ");

            if (targetType.isCollection()) {

                if (sourceType.isArray() || sourceType.isCollection()) {
                    for (Object obj : (Iterable<Object>) source) {
                        ((Collection) ret).add(Integer.parseInt((String) obj));
                    }
                } else {
                    for (String str : ((String) source).split(",")) {
                        str = str.trim();
                        if (str.isEmpty()) {
                            continue;
                        }
                        ((Collection) ret).add(Integer.parseInt(str));
                    }
                }
            }

            return ret;
        }

        if (targetType.isAssignableTo(TypeDescriptor.valueOf(Long.class)) || targetType
                .isAssignableTo(TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(Long.class)))) {
            LOG.trace("Proceeding with conversion of Long ... ");

            if (targetType.isCollection()) {

                if (sourceType.isArray() || sourceType.isCollection()) {
                    for (Object obj : (Iterable<Object>) source) {
                        ((Collection) ret).add(Long.parseLong((String) obj));
                    }
                } else {
                    for (String str : ((String) source).split(",")) {
                        str = str.trim();
                        if (str.isEmpty()) {
                            continue;
                        }
                        ((Collection) ret).add(Long.parseLong(str));
                    }
                }
            }

            return ret;
        }

        if (targetType.isAssignableTo(TypeDescriptor.valueOf(EnumWithId.class)) || targetType
                .isAssignableTo(TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(EnumWithId.class)))) {
            LOG.trace("Proceeding with conversion of EnumWithId ... ");
            
            if (targetType.isCollection()) {
                Converter<String, ? extends EnumWithId> converter = enumWithIdConverterFactory.getConverter((Class<? extends EnumWithId>) targetType.getElementTypeDescriptor().getType());

                if (sourceType.isArray() || sourceType.isCollection()) {
                    for (Object obj : (Iterable<Object>) source) {
                        ((Collection) ret).add(converter.convert((String) obj));
                    }
                } else {
                    for (String str : ((String) source).split(",")) {
                        str = str.trim();
                        if (str.isEmpty()) {
                            continue;
                        }
                        ((Collection) ret).add(converter.convert(str));
                    }
                }
            } else {
                Converter<String, ? extends EnumWithId> converter = enumWithIdConverterFactory.getConverter((Class<? extends EnumWithId>) targetType.getType());
                ret = converter.convert((String) source);
            }

            return ret;
        }
        
        if (!targetType.isAssignableTo(TypeDescriptor.valueOf(BaseJsonModel.class)) && !targetType.isAssignableTo(
                TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(BaseJsonModel.class)))) {
            throw new IllegalStateException(
                    "WC GenericConverter only handles BaseJsonModel and its collections and its descendants, not "
                            + targetType.getName());
        }

        LOG.trace("Proceeding with conversion of BaseJsonModel ... ");

        LOG.debug("Attempting to convert {} from {} to {}", source, sourceType.getName(), targetType.getName());

        if (targetType.isCollection()) {

            if (sourceType.isArray() || sourceType.isCollection()) {
                if (source != null) {
                    for (Object obj : (Iterable<Object>) source) {
                        ((Collection) ret).add(BaseJsonModel.fromString((String) obj, BaseJsonModel.class));
                    }
                }
            } else {
                if (source != null && !((String) source).isEmpty()) {
                    ((Collection) ret).addAll(BaseJsonModel.listFromString((String) source, BaseJsonModel.class));
                }
            }
        } else {
            if (source != null && ((String) source).startsWith("[{")) {
                // DT: should not ever get here
                ret = BaseJsonModel.listFromString((String) source, BaseJsonModel.class);
            } else {
                if (source == null || "".equals(source)) {
                    return null;
                }
                ret = BaseJsonModel.fromString((String) source, BaseJsonModel.class);
            }
        }

        return ret;
    }
}
