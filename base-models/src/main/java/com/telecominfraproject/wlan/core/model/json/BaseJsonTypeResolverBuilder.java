package com.telecominfraproject.wlan.core.model.json;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * @author dtop
 * 
 * This class provides support for deserializing JSON objects with missing model_type property
 */
public class BaseJsonTypeResolverBuilder extends StdTypeResolverBuilder {

    public static class ForgivingAsPropertyTypeDeserializer extends AsPropertyTypeDeserializer {

        /**
         * 
         */
        private static final long serialVersionUID = 3044154758561436481L;

        public ForgivingAsPropertyTypeDeserializer(JavaType bt, TypeIdResolver idRes, String typePropertyName,
                boolean typeIdVisible, JavaType defaultImpl) {
            super(bt, idRes, typePropertyName, typeIdVisible, defaultImpl);
        }
        
        

        public ForgivingAsPropertyTypeDeserializer(ForgivingAsPropertyTypeDeserializer src, BeanProperty property) {
            super(src, property);
        }

        @Override
        protected Object _deserializeTypedUsingDefaultImpl(final JsonParser jp, DeserializationContext ctxt,
                TokenBuffer tb, String priorFailureMsg) throws IOException {
            Object obj = null;
            JsonParser parser = jp;
            try{
                obj = super._deserializeTypedUsingDefaultImpl(parser, ctxt, tb, priorFailureMsg);
            }catch(Exception e){
                JsonDeserializer<Object> deser = ctxt.findContextualValueDeserializer(this._baseType, null);
                if (deser != null) {
                    if (tb != null) {
                        tb.writeEndObject();
                        parser = tb.asParser(parser);
                        // must move to point to the first token:
                        parser.nextToken();
                    }
                    return deser.deserialize(parser, ctxt);
                }
            }

            return obj; 
        }

        @Override
        public TypeDeserializer forProperty(BeanProperty prop) {
            return (prop == _property) ? this : new ForgivingAsPropertyTypeDeserializer(this, prop);
        }
    }

    /**
     * Dtop: we are customizing AsPropertyTypeDeserializer to try to gracefully handle missing model_type properties
     */
    
    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, final JavaType baseType,
            Collection<NamedType> subtypes) {
        
        TypeDeserializer originalDeserializer = super.buildTypeDeserializer(config, baseType, subtypes);
        TypeDeserializer ret = originalDeserializer; 
        if(ret instanceof AsPropertyTypeDeserializer){
            TypeIdResolver idRes = idResolver(config, baseType, BasicPolymorphicTypeValidator.builder().allowIfBaseType(BaseJsonModel.class).build(), subtypes, false, true);
            JavaType defaultImpl;

            if (_defaultImpl == null) {
                defaultImpl = null;
            } else {
                // 20-Mar-2016, tatu: It is important to do specialization go through
                //   TypeFactory to ensure proper resolution; with 2.7 and before, direct
                //   call to JavaType was used, but that can not work reliably with 2.7
                // 20-Mar-2016, tatu: Can finally add a check for type compatibility BUT
                //   if so, need to add explicit checks for marker types. Not ideal, but
                //   seems like a reasonable compromise.
                if ((_defaultImpl == Void.class)
                         || (_defaultImpl == NoClass.class)) {
                    defaultImpl = config.getTypeFactory().constructType(_defaultImpl);
                } else {
                    defaultImpl = config.getTypeFactory()
                        .constructSpecializedType(baseType, _defaultImpl);
                }
            }
            ret = new ForgivingAsPropertyTypeDeserializer(baseType, idRes, _typeProperty, _typeIdVisible, defaultImpl);
        }
        
        return ret;
    }
    
    public void setPropertyName(String property) {
        super._typeProperty = property;
    }
}
