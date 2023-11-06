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

import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.util.TypeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class TestTypeUtils extends AbstractTest {
    @Test
    void testFindCommonType() {
        Assertions.assertNull(TypeUtils.findCommonType());
        Assertions.assertEquals(TypeUtils.findCommonType(BuiltinType.I32), BuiltinType.I32);
    }
}
