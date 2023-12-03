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

import io.karma.ferrous.manganese.ocm.Mangleable;
import io.karma.ferrous.manganese.ocm.constant.VoidConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;

/**
 * @author Alexander Hinze
 * @since 03/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class VoidType implements Type, Mangleable {
    public static final char SEQUENCE_PREFIX = '\'';
    public static final VoidType INSTANCE = new VoidType();
    private static final Identifier NAME = new Identifier(TokenUtils.getLiteral(FerrousLexer.KW_VOID));
    private static final String MANGLED_NAME = "V";

    // @formatter:off
    private VoidType() {}
    // @formatter:on

    @Override
    public char getMangledSequencePrefix() {
        return SEQUENCE_PREFIX;
    }

    @Override
    public String getMangledName() {
        return MANGLED_NAME;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.VOID;
    }

    @Override
    public Identifier getName() {
        return NAME;
    }

    @Override
    public Scope getEnclosingScope() {
        return DefaultScope.GLOBAL;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return LLVMCore.LLVMVoidType();
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public Expression makeDefaultValue(final TargetMachine targetMachine) {
        return new VoidConstant();
    }

    @Override
    public String toString() {
        return getName().toString();
    }
}
