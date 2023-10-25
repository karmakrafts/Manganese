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

package io.karma.ferrous.manganese.ocm.access;

import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.NameProvider;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 17/10/2023
 */
@API(status = Status.INTERNAL)
public enum DefaultAccess implements Access {
    // @formatter:off
    PRIVATE     (AccessKind.PRIVATE),
    PUBLIC      (AccessKind.PUBLIC),
    PROTECTED   (AccessKind.PROTECTED),
    MODULE      (AccessKind.MODULE);
    // @formatter:on

    private final AccessKind kind;

    DefaultAccess(final AccessKind kind) {
        this.kind = kind;
    }

    @Override
    public AccessKind getKind() {
        return kind;
    }

    @Override
    public <T extends Scoped & NameProvider> boolean hasAccess(final Compiler compiler, final ScopeStack scopeStack,
                                                               final T target) {
        final var compileContext = compiler.getContext();
        final var capturedScopeStack = new ScopeStack(scopeStack);
        final var targetScope = target.getEnclosingScope();
        final var currentScope = scopeStack.peek();

        if (targetScope == currentScope || this == DefaultAccess.PUBLIC) {
            return true;
        }

        if (this == DefaultAccess.MODULE) {
            Scope currentModuleScope = capturedScopeStack.pop();
            while (currentModuleScope.getType() != ScopeType.MODULE) {
                currentModuleScope = capturedScopeStack.pop();
            }

            final var targetScopeStack = target.rebuildScopeStack();
            Scope targetModuleScope = targetScopeStack.pop();
            while (targetModuleScope.getType() != ScopeType.MODULE) {
                targetModuleScope = targetScopeStack.pop();
            }

            return currentModuleScope == targetModuleScope;
        }

        // TODO: Implement protected

        compileContext.reportError(compileContext.makeError(CompileErrorCode.E5001));
        return false;
    }
}
