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
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.*;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.*;
import io.karma.kommons.function.Functions;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public final class TypeParser extends ParseAdapter {
    private final ScopeStack capturedScopeStack;
    private Type type;

    public TypeParser(final CompileContext compileContext, final ScopeStack capturedScopeStack) {
        super(compileContext);
        this.capturedScopeStack = capturedScopeStack;
    }

    @Override
    public void enterPrimaryType(final PrimaryTypeContext context) {
        if (type != null) {
            return;
        }
        final var moduleData = compileContext.getOrCreateModuleData();
        // Qualified ident
        final var qualifiedIdentContext = context.qualifiedIdent();
        if (qualifiedIdentContext != null) {
            final var name = Identifier.parse(qualifiedIdentContext);
            type = moduleData.findCompleteType(name, capturedScopeStack.getScopeName());
            if (type == null) {
                type = Types.incomplete(name,
                    capturedScopeStack::applyEnclosingScopes,
                    TokenSlice.from(compileContext, qualifiedIdentContext));
            }
            return;
        }
        // Ident
        final var identContext = context.IDENT();
        if (identContext != null) {
            final var name = Identifier.parse(identContext.getText());
            type = moduleData.findCompleteType(name, capturedScopeStack.getScopeName());
            if (type == null) {
                type = Types.incomplete(name,
                    capturedScopeStack::applyEnclosingScopes,
                    TokenSlice.from(compileContext, identContext));
            }
        }
    }

    @Override
    public void enterType(final TypeContext context) {
        if (type != null) {
            return;
        }
        // Handle derived types recursively
        final var typeContext = context.type();
        if (typeContext == null) {
            return;
        }
        final var type = Types.parse(compileContext, capturedScopeStack, typeContext);
        if (type == null) {
            compileContext.reportError(context.start, CompileErrorCode.E3002);
            return;
        }
        if (context.ASTERISK() != null) {
            if (type.isRef()) {
                compileContext.reportError(context.start, CompileErrorCode.E3007);
                return;
            }
            final var mods = TypeModifier.parse(compileContext, context.typeMod());
            this.type = type.derive(TypeAttribute.POINTER, mods.toArray(TypeModifier[]::new));
        }
        if (context.AMP() != null) {
            if (type.isRef()) {
                compileContext.reportError(context.start, CompileErrorCode.E3001);
                return;
            }
            final var mods = TypeModifier.parse(compileContext, context.typeMod());
            this.type = type.derive(TypeAttribute.REFERENCE, mods.toArray(TypeModifier[]::new));
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

    public @Nullable Type getType() {
        return type;
    }
}
