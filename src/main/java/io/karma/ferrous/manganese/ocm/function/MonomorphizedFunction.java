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

import io.karma.ferrous.manganese.mangler.Mangler;
import io.karma.ferrous.manganese.ocm.type.Type;
import org.apiguardian.api.API;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 27/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class MonomorphizedFunction extends Function {
    private final List<Type> genericTypes;

    MonomorphizedFunction(final Function function, final List<Type> genericTypes) {
        super(function.getName(),
            function.getCallConv(),
            function.getType(),
            function.getModifiers(),
            function.getTokenSlice(),
            function.getParameters(),
            function.getGenericParameters(),
            function.getAttributeUsages());
        this.genericTypes = genericTypes;
    }

    @Override
    public boolean isMonomorphic() {
        return true;
    }

    @Override
    public MonomorphizedFunction monomorphize(final List<Type> genericTypes) {
        return this;
    }

    // Mangleable

    @Override
    public String getMangledName() {
        final var paramTypes = parameters.stream().map(Parameter::getType).toList();
        return STR."\{getQualifiedName().toInternalName()}<\{Mangler.mangleSequence(genericTypes)}>(\{Mangler.mangleSequence(
            paramTypes)})";
    }

    public List<Type> getGenericTypes() {
        return genericTypes;
    }
}
