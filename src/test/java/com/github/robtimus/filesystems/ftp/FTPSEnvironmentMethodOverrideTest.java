/*
 * FTPSEnvironmentMethodOverrideTest.java
 * Copyright 2017 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.filesystems.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("javadoc")
public class FTPSEnvironmentMethodOverrideTest {

    static Stream<Arguments> testMethodOverride() {
        List<Arguments> arguments = new ArrayList<>();
        for (Method method : FTPEnvironment.class.getMethods()) {
            if (method.getReturnType() == FTPEnvironment.class) {
                arguments.add(arguments(method));
            }
        }
        return arguments.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testMethodOverride(Method parentMethod) throws NoSuchMethodException {
        Method method = FTPSEnvironment.class.getMethod(parentMethod.getName(), parentMethod.getParameterTypes());
        assertEquals(FTPSEnvironment.class, method.getReturnType());
    }
}
