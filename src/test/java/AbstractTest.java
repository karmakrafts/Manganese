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

import io.karma.ferrous.manganese.Manganese;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.target.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
public abstract class AbstractTest {
    private static AbstractTest INSTANCE;
    protected CompileContext compileContext;
    protected TargetMachine targetMachine;
    protected Module module;

    protected AbstractTest() {
        INSTANCE = this;
    }

    private void initInstance() {
        compileContext = new CompileContext();
        targetMachine = new TargetMachine(Target.getHostTarget(),
            "",
            OptimizationLevel.DEFAULT,
            Relocation.DEFAULT,
            CodeModel.DEFAULT,
            "");
        module = INSTANCE.targetMachine.createModule("test");
    }

    private void disposeInstance() {
        compileContext.dispose();
        targetMachine.dispose();
        module.dispose();
    }

    @BeforeAll
    static void init() {
        Manganese.init();
        INSTANCE.initInstance();
    }

    @AfterAll
    static void dispose() {
        INSTANCE.disposeInstance();
    }
}
