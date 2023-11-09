/*
 * Copyright 2023 Karma Krafts & associates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.karma.ferrous.manganese.ocm.function;

import io.karma.ferrous.manganese.ocm.type.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 09/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionReference {
    private final FunctionResolver resolver;
    private Type[] contextualParamTypes;

    public FunctionReference(final FunctionResolver resolver) {
        this.resolver = resolver;
    }

    public @Nullable Function resolve() {
        if (contextualParamTypes == null) {
            throw new IllegalStateException("Cannot resolve function without contextual type information");
        }
        return resolver.resolve(contextualParamTypes);
    }

    public Type[] getContextualParamTypes() {
        return contextualParamTypes;
    }

    public void setContextualParamTypes(Type... contextualParamTypes) {
        this.contextualParamTypes = contextualParamTypes;
    }

    public FunctionResolver getResolver() {
        return resolver;
    }
}
