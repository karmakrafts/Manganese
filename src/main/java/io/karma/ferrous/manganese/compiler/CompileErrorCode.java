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

package io.karma.ferrous.manganese.compiler;

/**
 * @author Alexander Hinze
 * @since 24/10/2023
 */
public enum CompileErrorCode {
    // @formatter:off
    // IO Errors
    E0000("Source file could not be read", CompileStatus.IO_ERROR),
    E0001("Target file could not be written", CompileStatus.IO_ERROR),
    E0002("Could not create character stream while analyzing file", CompileStatus.IO_ERROR),
    E0003("Could not open file input stream while analyzing file", CompileStatus.IO_ERROR),
    E0004("Could not open file output stream while compiling file", CompileStatus.IO_ERROR),
    E0005("Could not create target directory", CompileStatus.IO_ERROR),
    // Verification Errors
    E1000("Translation unit module could not be verified", CompileStatus.VERIFY_ERROR),
    E1001("Builtin module could not be verified", CompileStatus.VERIFY_ERROR),
    E1002("Dynamic module could not be verified", CompileStatus.VERIFY_ERROR),
    // Syntax Errors
    E2000("Encountered syntax error while parsing", CompileStatus.SYNTAX_ERROR),
    E2001("Encountered error while parsing expression", CompileStatus.SYNTAX_ERROR),
    // Type errors
    E3000("The given type is already defined within the same scope", CompileStatus.TYPE_ERROR),
    E3001("The given type cannot have more than one level of reference", CompileStatus.TYPE_ERROR),
    E3002("The given type could not be found", CompileStatus.TYPE_ERROR),
    E3003("The given aliased type cannot be resolved", CompileStatus.TYPE_ERROR),
    E3004("The given field type cannot be resolved", CompileStatus.TYPE_ERROR),
    E3005("The given signature type cannot be resolved", CompileStatus.TYPE_ERROR),
    E3006("The given type cannot be assigned or casted implicitly", CompileStatus.TYPE_ERROR),
    // Semantic errors
    E4000("The given calling convention does not exist", CompileStatus.SEMANTIC_ERROR),
    E4001("The given element cannot be accessed from this scope", CompileStatus.SEMANTIC_ERROR),
    E4002("A parameter cannot have the kind void", CompileStatus.SEMANTIC_ERROR),
    E4003("The given function is already defined in the same scope", CompileStatus.SEMANTIC_ERROR),
    E4004("Function that is not virtual, abstract or extern has no body", CompileStatus.SEMANTIC_ERROR),
    E4005("Function never returns", CompileStatus.SEMANTIC_ERROR),
    E4006("Typeless variable requires explicit initialization", CompileStatus.SEMANTIC_ERROR),
    E4007("Function does not exist", CompileStatus.SEMANTIC_ERROR),
    E4008("Subscript operator requires at least one argument", CompileStatus.SEMANTIC_ERROR),
    E4009("Cannot resolve function with the given parameter types", CompileStatus.SEMANTIC_ERROR),
    E4010("Immutable variable cannot be uninitialized", CompileStatus.SEMANTIC_ERROR),
    E4011("Immutable variable cannot be reassigned", CompileStatus.SEMANTIC_ERROR),
    // Translation errors
    E5000("Could not find function during translation", CompileStatus.TRANSLATION_ERROR),
    E5001("Unknown unary operator", CompileStatus.TRANSLATION_ERROR),
    E5002("Unknown binary operator", CompileStatus.TRANSLATION_ERROR),
    // Link errors
    E6000("Could not find linker command", CompileStatus.LINK_ERROR),
    E6001("Could not spawn linker process", CompileStatus.LINK_ERROR),
    E6002("Linker exited with abnormal exit code", CompileStatus.LINK_ERROR),
    E6003("Linker process was interrupted unexpectedly", CompileStatus.LINK_ERROR),
    E6004("Target architecture not supported by linker", CompileStatus.LINK_ERROR),
    E6005("Target platform not supported by linker", CompileStatus.LINK_ERROR),
    E6006("Target link model required dynamic code relocation", CompileStatus.LINK_ERROR),
    E6007("Could not find dynamic linker binary", CompileStatus.LINK_ERROR),
    E6008("Link target doesn't support system runtime", CompileStatus.LINK_ERROR),
    E6009("Could not find CRT object while linking", CompileStatus.LINK_ERROR),
    // Unknown error
    E9999("Encountered an unhandled error", CompileStatus.UNKNOWN_ERROR)
    // @formatter:on
    ;

    private final String message;
    private final CompileStatus status;

    CompileErrorCode(final String message, final CompileStatus status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public CompileStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", name(), message);
    }
}
