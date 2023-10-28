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

import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;

/**
 * @author Alexander Hinze
 * @since 24/10/2023
 */
public enum ImaginaryType implements NamedType {
    // @formatter:off
    TOKEN   (FerrousLexer.KW_TOKEN),
    IDENT   (FerrousLexer.KW_IDENT),
    EXPR    (FerrousLexer.KW_EXPR),
    LITERAL (FerrousLexer.KW_LITERAL),
    TYPE    (FerrousLexer.KW_TYPE),
    STRING  (FerrousLexer.KW_STRING);
    // @formatter:on

    private final Identifier name;

    ImaginaryType(final int token) {
        name = Identifier.parse(TokenUtils.getLiteral(token));
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
    }

    // Scoped

    @Override
    public Scope getEnclosingScope() {
        return DefaultScope.GLOBAL;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    // Type

    @Override
    public GenericParameter[] getGenericParams() {
        return new GenericParameter[0];
    }

    @Override
    public boolean isImaginary() {
        return true;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        throw new UnsupportedOperationException("Imaginary types cannot be materialized");
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public Type getBaseType() {
        return this;
    }
}
