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

package io.karma.ferrous.manganese.module;

import io.karma.ferrous.manganese.ocm.field.Field;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.statement.LetStatement;
import io.karma.ferrous.manganese.ocm.type.AliasedType;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.profiler.Profiler;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.ScopeUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
@API(status = Status.STABLE)
public final class ModuleData {
    private final String name;
    // @formatter:off
    // Non-synchronized data
    private final LinkedHashMap<Identifier, Type> types = new LinkedHashMap<>();
    private final HashMap<Identifier, HashMap<FunctionType, Function>> functions = new HashMap<>();
    private final HashMap<Function, LinkedHashMap<Identifier, LetStatement>> locals = new HashMap<>();
    private final LinkedHashMap<Identifier, Field> globalFields = new LinkedHashMap<>();
    private BufferedTokenStream tokenStream;
    private FileContext fileContext;
    private FerrousLexer lexer;
    private FerrousParser parser;
    // @formatter:on

    public ModuleData(final String name) {
        this.name = name;
    }

    public synchronized FerrousLexer getLexer() {
        return lexer;
    }

    @API(status = Status.INTERNAL)
    public synchronized void setLexer(final FerrousLexer lexer) {
        this.lexer = lexer;
    }

    public synchronized FerrousParser getParser() {
        return parser;
    }

    @API(status = Status.INTERNAL)
    public synchronized void setParser(final FerrousParser parser) {
        this.parser = parser;
    }

    public synchronized FileContext getFileContext() {
        return fileContext;
    }

    @API(status = Status.INTERNAL)
    public synchronized void setFileContext(final FileContext fileContext) {
        this.fileContext = fileContext;
    }

    public synchronized BufferedTokenStream getTokenStream() {
        return tokenStream;
    }

    @API(status = Status.INTERNAL)
    public synchronized void setTokenStream(final BufferedTokenStream tokenStream) {
        this.tokenStream = tokenStream;
    }

    public String getName() {
        return name;
    }

    // Non-synchronized data

    public void addType(final Type type) {
        final var name = type.getQualifiedName();
        if (types.containsKey(name)) {
            throw new IllegalStateException("Type already exists");
        }
        types.put(name, type);
    }

    public LinkedHashMap<Identifier, Type> getTypes() {
        return types;
    }

    public HashMap<Identifier, HashMap<FunctionType, Function>> getFunctions() {
        return functions;
    }

    public LinkedHashMap<Identifier, Field> getGlobalFields() {
        return globalFields;
    }

    public HashMap<Function, LinkedHashMap<Identifier, LetStatement>> getLocals() {
        return locals;
    }

    public @Nullable Type findCompleteType(final Identifier name, final Identifier scopeName) {
        Profiler.INSTANCE.push();
        Type type = ScopeUtils.findInScope(types, name, scopeName);
        if (type == null) {
            return null;
        }
        while (type instanceof AliasedType alias) {
            type = alias.getBackingType();
        }
        if (!type.isComplete()) {
            final var typeName = type.getQualifiedName();
            type = ScopeUtils.findInScope(types, typeName, scopeName);
        }
        Profiler.INSTANCE.pop();
        return type;
    }

    public @Nullable Type findCompleteType(final Type type) {
        if (type.isComplete()) {
            return type;
        }
        return findCompleteType(type.getName(), type.getScopeName());
    }

    public @Nullable Type findType(final Identifier name, final Identifier scopeName) {
        try {
            Profiler.INSTANCE.push();
            return ScopeUtils.findInScope(types, name, scopeName);
        }
        finally {
            Profiler.INSTANCE.pop();
        }
    }

    // TODO: implement matching against C-variadic functions
    public @Nullable Function findFunction(final Identifier name, final Identifier scopeName,
                                           final List<Type> paramTypes) {
        try {
            Profiler.INSTANCE.push();
            final var overloadSet = ScopeUtils.findInScope(functions, name, scopeName);
            if (overloadSet == null) {
                return null;
            }
            final var types = overloadSet.keySet();
            for (final var type : types) {
                if (!type.getParamTypes().equals(paramTypes)) {
                    continue;
                }
                return overloadSet.get(type);
            }
            return null;
        }
        finally {
            Profiler.INSTANCE.pop();
        }
    }

    public @Nullable Function findFunction(final Identifier name, final Identifier scopeName, final FunctionType type) {
        return findFunction(name, scopeName, type.getParamTypes());
    }

    public boolean functionExists(final Identifier name, final Identifier scopeName) {
        return ScopeUtils.findInScope(functions, name, scopeName) != null;
    }

    public @Nullable Field findGlobalFieldInScope(final Identifier name, final Identifier scopeName) {
        return ScopeUtils.findInScope(globalFields, name, scopeName);
    }

    public Map<Identifier, LetStatement> getLocalsFor(final Function function) {
        final var map = locals.get(function);
        if (map != null) {
            return map;
        }
        return Collections.emptyMap();
    }

    public LetStatement findLocalIn(final Function function, final Identifier name, final Identifier scopeName) {
        return ScopeUtils.findInScope(getLocalsFor(function), name, scopeName);
    }
}
