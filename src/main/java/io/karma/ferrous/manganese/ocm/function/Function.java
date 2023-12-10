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

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.AttributeUsage;
import io.karma.ferrous.manganese.ocm.Mangleable;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.BuiltinAttributes;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Mangler;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.kommons.tuple.Pair;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMCore;

import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public class Function implements Scoped, Mangleable {
    protected final Identifier name;
    protected final CallingConvention callConv;
    protected final EnumSet<FunctionModifier> modifiers;
    protected final List<Parameter> parameters;
    protected final List<GenericParameter> genericParams;
    protected final List<AttributeUsage> attributeUsages;
    protected final TokenSlice tokenSlice;
    protected final FunctionType type;
    protected final HashMap<String, MonomorphizedFunction> monomorphizationCache = new HashMap<>();
    protected final Map<Identifier, ParameterStorage> paramStorages;
    protected FunctionBody body;
    protected Scope enclosingScope;
    protected long materializedPrototype;

    public Function(final Identifier name, final CallingConvention callConv, final FunctionType type,
                    final EnumSet<FunctionModifier> modifiers, final TokenSlice tokenSlice,
                    final List<Parameter> params, final List<GenericParameter> genericParams,
                    final List<AttributeUsage> attributeUsages) {
        this.name = name;
        this.callConv = callConv;
        this.modifiers = modifiers;
        this.type = type;
        this.tokenSlice = tokenSlice;
        this.parameters = params;
        this.genericParams = genericParams;
        this.attributeUsages = attributeUsages;

        // @formatter:off
        paramStorages = parameters.stream()
            .map(param -> Pair.of(param.getName(), new ParameterStorage(param,
                compileContext -> {
                    final var module = compileContext.getModule(compileContext.getCurrentModuleName());
                    final var targetMachine = compileContext.getCompiler().getTargetMachine();
                    final var fnAddress = materialize(module, targetMachine);
                    return LLVMCore.LLVMGetParam(fnAddress, parameters.indexOf(param));
                }))
            )
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        // @formatter:on
    }

    public @Nullable ParameterStorage getParamStorage(final Identifier name) {
        return paramStorages.get(name);
    }

    public Map<Identifier, ParameterStorage> getParamStorages() {
        return paramStorages;
    }

    public void createBody(final List<Statement> statements) {
        if (body != null) {
            throw new IllegalStateException("Body already exists for this function");
        }
        body = new FunctionBody(this, statements);
    }

    public CallingConvention getCallConv() {
        return callConv;
    }

    public boolean shouldMangle() {
        for (final var usage : attributeUsages) {
            if (usage.attribute() != BuiltinAttributes.NOMANGLE) {
                continue;
            }
            return false;
        }
        return true;
    }

    public boolean isMonomorphic() {
        return genericParams.isEmpty();
    }

    public boolean isIntrinsic() {
        return false;
    }

    public @Nullable FunctionBody getBody() {
        return body;
    }

    public FunctionType getType() {
        return type;
    }

    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    public EnumSet<FunctionModifier> getModifiers() {
        return modifiers;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public List<GenericParameter> getGenericParameters() {
        return genericParams;
    }

    public List<AttributeUsage> getAttributeUsages() {
        return attributeUsages;
    }

    public long materialize(final Module module, final TargetMachine targetMachine) {
        if (materializedPrototype != NULL) {
            return materializedPrototype;
        }
        final var address = LLVMAddFunction(module.getAddress(), getMangledName(), type.materialize(targetMachine));
        final var numParams = parameters.size();
        for (var i = 0; i < numParams; i++) {
            LLVMSetValueName2(LLVMGetParam(address, i), parameters.get(i).getName().toString());
        }
        LLVMSetLinkage(address, modifiers.contains(FunctionModifier.EXTERN) ? LLVMExternalLinkage : 0);
        LLVMSetFunctionCallConv(address, callConv.getLLVMValue(targetMachine));
        return materializedPrototype = address;
    }

    public long emit(final CompileContext compileContext, final Module module, final TargetMachine targetMachine) {
        if (body != null) {
            body.append(compileContext, module, targetMachine);
            return materializedPrototype; // This won't be NULL at this point
        }
        return materialize(module, targetMachine);
    }

    public void delete() {
        if (materializedPrototype == NULL) {
            return;
        }
        LLVMDeleteFunction(materializedPrototype);
        materializedPrototype = NULL;
    }

    public MonomorphizedFunction monomorphize(final List<Type> genericTypes) {
        if (isMonomorphic()) {
            throw new IllegalStateException("Monomorphic function cannot be monomorphized again");
        }
        return monomorphizationCache.computeIfAbsent(Mangler.mangleSequence(genericTypes),
            key -> new MonomorphizedFunction(this, genericTypes));
    }

    // Mangleable

    @Override
    public String getMangledName() {
        if (!shouldMangle()) {
            return getName().toInternalName();
        }
        return String.format("%s(%s)",
            getQualifiedName().toInternalName(),
            Mangler.mangleSequence(parameters.stream().map(Parameter::getType).toList()));
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
    }

    // Scoped

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(name, callConv, modifiers, type, enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Function function) { // @formatter:off
            return name.equals(function.name)
                && callConv == function.callConv
                && modifiers.equals(function.modifiers)
                && type.equals(function.type)
                && Objects.equals(enclosingScope, function.enclosingScope);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s %s(%s)", type.getReturnType(), name, parameters);
    }
}
