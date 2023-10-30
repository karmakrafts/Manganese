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
    // Type errors
    E3000("The given type is already defined within the same scope", CompileStatus.TYPE_ERROR),
    E3001("The given type cannot have more than one level of reference", CompileStatus.TYPE_ERROR),
    E3002("The given type could not be found", CompileStatus.TYPE_ERROR),
    E3003("The given aliased type cannot be resolved", CompileStatus.TYPE_ERROR),
    E3004("The given field type cannot be resolved", CompileStatus.TYPE_ERROR),
    // Translation errors
    E4000("Could not materialize the underlying function type", CompileStatus.TRANSLATION_ERROR),
    // Semantic errors
    E5000("The given calling convention does not exist", CompileStatus.SEMANTIC_ERROR),
    E5001("The given element cannot be accessed from this scope", CompileStatus.SEMANTIC_ERROR),
    E5002("A parameter cannot have the type void", CompileStatus.SEMANTIC_ERROR),
    // Link errors
    E6000("Could not find linker command", CompileStatus.LINK_ERROR),
    E6001("Could not spawn linker process", CompileStatus.LINK_ERROR),
    E6002("Linker exited with abnormal exit code", CompileStatus.LINK_ERROR),
    E6003("Linker process was interrupted unexpectedly", CompileStatus.LINK_ERROR),
    E6004("Target architecture not supported by linker", CompileStatus.LINK_ERROR),
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
