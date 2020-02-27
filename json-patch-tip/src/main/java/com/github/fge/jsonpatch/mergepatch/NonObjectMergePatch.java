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
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Preconditions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

@ParametersAreNonnullByDefault
final class NonObjectMergePatch extends JsonMergePatch
{
    private final JsonNode node;

    NonObjectMergePatch(final JsonNode node)
    {
        this.node = Preconditions.checkNotNull(node);
    }

    public JsonNode apply(final JsonNode input, boolean ignoreInvalidPaths)
        throws JsonPatchException
    {
        BUNDLE.checkNotNull(input, "jsonPatch.nullValue");
        return node;
    }

    @Override
    public void serialize(final JsonGenerator jgen,
        final SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jgen.writeTree(node);
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
