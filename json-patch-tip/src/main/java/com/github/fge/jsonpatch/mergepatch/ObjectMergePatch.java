/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonpatch.mergepatch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
final class ObjectMergePatch extends JsonMergePatch
{
    private final Set<String> removedMembers;
    private final Map<String, JsonMergePatch> modifiedMembers;

    ObjectMergePatch(final Set<String> removedMembers,
        final Map<String, JsonMergePatch> modifiedMembers)
    {
        this.removedMembers = ImmutableSet.copyOf(removedMembers);
        this.modifiedMembers = ImmutableMap.copyOf(modifiedMembers);
    }

    public JsonNode apply(final JsonNode input, boolean ignoreInvalidPaths)
        throws JsonPatchException
    {
        BUNDLE.checkNotNull(input, "jsonPatch.nullValue");
        /*
         * If the input is an object, we make a deep copy of it
         */
        final ObjectNode ret = input.isObject() ? (ObjectNode) input.deepCopy()
            : JacksonUtils.nodeFactory().objectNode();

        /*
         * Our result is now a JSON Object; first, add (or modify) existing
         * members in the result
         */
        String key;
        JsonNode value;
        for (final Map.Entry<String, JsonMergePatch> entry:
            modifiedMembers.entrySet()) {
            key = entry.getKey();
            /*
             * FIXME: ugly...
             *
             * We treat missing keys as null nodes; this "works" because in
             * the modifiedMembers map, values are JsonMergePatch instances:
             *
             * * if it is a NonObjectMergePatch, the value is replaced
             *   unconditionally;
             * * if it is an ObjectMergePatch, we get back here; the value will
             *   be replaced with a JSON Object anyway before being processed.
             */
            value = Optional.fromNullable(ret.get(key)).or(NullNode.getInstance());
            
            ret.put(key, (ignoreInvalidPaths) ? entry.getValue().applyWhileIgnoreInvalidPaths(value) : entry.getValue().apply(value));
        }

        ret.remove(removedMembers);

        return ret;
    }

    @Override
    public void serialize(final JsonGenerator jgen,
        final SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();

        /*
         * Write removed members as JSON nulls
         */
        for (final String member: removedMembers)
            jgen.writeNullField(member);

        /*
         * Write modified members; delegate to serialization for writing values
         */
        for (final Map.Entry<String, JsonMergePatch> entry:
            modifiedMembers.entrySet()) {
            jgen.writeFieldName(entry.getKey());
            entry.getValue().serialize(jgen, provider);
        }

        jgen.writeEndObject();
    }

    @Override
    public void serializeWithType(final JsonGenerator jgen,
        final SerializerProvider provider, final TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        serialize(jgen, provider);
    }

   @Override
   public JsonNode apply(JsonNode input) throws JsonPatchException 
   {
      return apply(input, false);
   }

   @Override
   public JsonNode applyWhileIgnoreInvalidPaths(JsonNode input) throws JsonPatchException 
   {
      return apply(input, true);
   }
}
