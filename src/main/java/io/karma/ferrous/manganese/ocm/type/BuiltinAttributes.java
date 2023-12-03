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

package io.karma.ferrous.manganese.ocm.type;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.kommons.function.Functions;
import org.apiguardian.api.API;

import java.util.Collections;

/**
 * @author Alexander Hinze
 * @since 27/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class BuiltinAttributes {
    public static final UserDefinedType NOMANGLE = create("nomangle");
    public static final UserDefinedType NORETURN = create("noreturn");
    public static final UserDefinedType NODISCARD = create("nodiscard");
    public static final UserDefinedType NOOPT = create("noopt");

    // @formatter:off
    private BuiltinAttributes() {}
    // @formatter:on

    public static void inject(final CompileContext compileContext) {
        final var moduleData = compileContext.getOrCreateModuleData();
        moduleData.addType(NOMANGLE);
        moduleData.addType(NORETURN);
        moduleData.addType(NODISCARD);
        moduleData.addType(NOOPT);
    }

    private static UserDefinedType create(final String name) {
        final var ident = new Identifier(name);
        final var structType = Types.structure(ident,
            false,
            Functions.castingIdentity(),
            Collections.emptyList(),
            TokenSlice.EMPTY,
            Collections.emptyList());
        structType.setEnclosingScope(DefaultScope.GLOBAL);
        return new UserDefinedType(UserDefinedTypeKind.ATTRIBUTE,
            structType,
            Collections.emptyList(),
            TokenSlice.EMPTY);
    }
}
