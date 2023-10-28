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

package io.karma.ferrous.manganese.linker;

import java.util.ArrayList;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
public abstract class AbstractLinker implements Linker {
    protected final ArrayList<String> options = new ArrayList<>();

    // @formatter:off
    protected AbstractLinker() {}
    // @formatter:on

    @Override
    public void addOption(final String option) {
        if (options.contains(option)) {
            return;
        }
        options.add(option);
    }

    @Override
    public void clearOptions() {
        options.clear();
    }
}
