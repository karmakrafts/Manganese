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

package io.karma.ferrous.manganese.parser;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.*;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.*;
import io.karma.kommons.function.Functions;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Collections;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public final class TypeParser extends ParseAdapter {
    private final ScopeStack capturedScopeStack;
    private Type type;

    public TypeParser(final Compiler compiler, final CompileContext compileContext,
                      final ScopeStack capturedScopeStack) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
    }

    @Override
    public void enterType(final TypeContext context) {
        if (type != null) {
            return;
        }
        // Handle user defined types
        final var moduleData = compileContext.getOrCreateModuleData();
        final var qualifiedIdentContext = context.qualifiedIdent();
        if (qualifiedIdentContext != null) {
            if (type != null) {
                return;
            }
            final var name = Identifier.parse(qualifiedIdentContext);
            type = moduleData.findCompleteType(name, capturedScopeStack.getScopeName());
            if (type == null) {
                type = Types.incomplete(name,
                    Functions.castingIdentity(),
                    TokenSlice.from(compileContext, context),
                    Collections.emptyList());
            }
        }
        final var identContext = context.ident();
        if (identContext != null) {
            if (type != null) {
                return;
            }
            final var name = Identifier.parse(identContext);
            type = moduleData.findCompleteType(name, capturedScopeStack.getScopeName());
            if (type == null) {
                type = Types.incomplete(name,
                    Functions.castingIdentity(),
                    TokenSlice.from(compileContext, context),
                    Collections.emptyList());
            }
        }
        // Handle derived types recursively
        final var typeContext = context.type();
        if (typeContext == null) {
            return;
        }
        final var type = Types.parse(compiler, compileContext, capturedScopeStack, typeContext);
        if (type == null) {
            return; // TODO: handle error
        }
        if (context.ASTERISK() != null) {
            if (type.isReference()) {
                compileContext.reportError(context.start, CompileErrorCode.E3007);
                return;
            }
            this.type = type.asPtr();
        }
        if (context.AMP() != null) {
            if (type.isReference()) {
                compileContext.reportError(context.start, CompileErrorCode.E3002);
                return;
            }
            this.type = type.asRef();
        }
    }

    @Override
    public void enterMiscType(final MiscTypeContext context) {
        if (type != null) {
            return;
        }
        if (context.KW_VOID() != null) {
            type = VoidType.INSTANCE;
        }
        else if (context.KW_BOOL() != null) {
            type = BoolType.INSTANCE;
        }
        else if (context.KW_CHAR() != null) {
            type = CharType.INSTANCE;
        }
        else if (context.KW_STRING() != null) {
            type = ImaginaryType.STRING;
        }
    }

    @Override
    public void enterSintType(final SintTypeContext context) {
        if (type != null) {
            return;
        }
        if (context.KW_ISIZE() != null) {
            type = SizeType.ISIZE;
            return;
        }
        type = Types.integer(Integer.parseInt(context.getText().substring(1)), false, Functions.castingIdentity());
    }

    @Override
    public void enterUintType(final UintTypeContext context) {
        if (type != null) {
            return;
        }
        if (context.KW_USIZE() != null) {
            type = SizeType.USIZE;
            return;
        }
        type = Types.integer(Integer.parseInt(context.getText().substring(1)), true, Functions.castingIdentity());
    }

    @Override
    public void enterFloatType(final FloatTypeContext context) {
        if (type != null) {
            return;
        }
        if (context.KW_F16() != null) {
            type = RealType.F16;
        }
        else if (context.KW_F32() != null) {
            type = RealType.F32;
        }
        else if (context.KW_F64() != null) {
            type = RealType.F64;
        }
        else if (context.KW_F128() != null) {
            type = RealType.F128;
        }
    }

    public Type getType() {
        return type;
    }
}
