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

import io.karma.ferrous.manganese.ocm.AttributeUsage;
import io.karma.ferrous.manganese.ocm.type.*;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;

import java.util.*;

/**
 * Intrinsic declarations according to
 * <a href="https://llvm.org/docs/LangRef.html#intrinsic-functions">the LLVM docs</a>.
 *
 * @author Alexander Hinze
 * @since 01/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class IntrinsicFunction extends Function {
    // @formatter:off
    public static final IntrinsicFunction TRAP = new IntrinsicFunction(
        "llvm.trap",
        VoidType.INSTANCE);
    public static final IntrinsicFunction DEBUG_TRAP = new IntrinsicFunction(
        "llvm.debugtrap",
        VoidType.INSTANCE);
    public static final IntrinsicFunction RETURN_ADDRESS = new IntrinsicFunction(
        "llvm.returnaddress",
        VoidType.INSTANCE.asPtr(),
        IntType.I32);
    public static final IntrinsicFunction ADDRESS_OF_RETURN_ADDRESS = new IntrinsicFunction(
        "llvm.addressofreturnaddress",
        VoidType.INSTANCE.asPtr());
    public static final IntrinsicFunction FRAME_ADDRESS = new IntrinsicFunction(
        "llvm.frameaddress",
        VoidType.INSTANCE.asPtr(),
        IntType.I32);
    public static final IntrinsicFunction STACK_SAVE = new IntrinsicFunction(
        "llvm.stacksave.p0",
        VoidType.INSTANCE.asPtr());
    public static final IntrinsicFunction STACK_RESTORE = new IntrinsicFunction(
        "llvm.stackrestore.p0",
        VoidType.INSTANCE,
        VoidType.INSTANCE.asPtr());
    public static final IntrinsicFunction THREAD_POINTER = new IntrinsicFunction(
        "llvm.thread.pointer",
        VoidType.INSTANCE.asPtr());
    public static final IntrinsicFunction SPON_ENTRY = new IntrinsicFunction(
        "llvm.sponentry",
        VoidType.INSTANCE.asPtr());
    public static final IntrinsicFunction PREFETCH = new IntrinsicFunction(
        "llvm.prefetch",
        VoidType.INSTANCE, VoidType.INSTANCE.asPtr(), IntType.I32, IntType.I32, IntType.I32);

    public static final IntrinsicFunction VA_START = new IntrinsicFunction(
        "llvm.va_start",
        VoidType.INSTANCE, VoidType.INSTANCE.asPtr());
    public static final IntrinsicFunction VA_END = new IntrinsicFunction(
        "llvm.va_end",
        VoidType.INSTANCE, VoidType.INSTANCE.asPtr());
    public static final IntrinsicFunction VA_COPY = new IntrinsicFunction(
        "llvm.va_copy",
        VoidType.INSTANCE, VoidType.INSTANCE.asPtr(), VoidType.INSTANCE.asPtr());
    // @formatter:on

    private IntrinsicFunction(final String name, final Type returnType, final Type... paramTypes) {
        super(Identifier.parse(name),
            CallingConvention.CDECL,
            new FunctionType(returnType, false, TokenSlice.EMPTY, Arrays.asList(paramTypes)),
            EnumSet.noneOf(FunctionModifier.class),
            TokenSlice.EMPTY,
            createParameters(paramTypes),
            Collections.emptyList(),
            Collections.singletonList(new AttributeUsage(BuiltinAttributes.NOMANGLE, Collections.emptyMap())));
    }

    private static List<Parameter> createParameters(final Type[] paramTypes) {
        final var numParams = paramTypes.length;
        if (numParams == 0) {
            return Collections.emptyList();
        }
        final var params = new ArrayList<Parameter>(numParams);
        for (var i = 0; i < numParams; i++) {
            final var name = new Identifier(String.format("p%d", i));
            params.add(new Parameter(name, paramTypes[i], false, null));
        }
        return params;
    }
}
