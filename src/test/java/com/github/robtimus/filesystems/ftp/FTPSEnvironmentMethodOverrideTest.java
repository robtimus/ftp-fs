/*
 * FTPSEnvironmentSetterTest.java
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

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SuppressWarnings("javadoc")
public class FTPSEnvironmentMethodOverrideTest {

    private final Method parentMethod;

    public FTPSEnvironmentMethodOverrideTest(Method parentMethod) {
        this.parentMethod = parentMethod;
    }

    @Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        List<Object[]> parameters = new ArrayList<>();
        for (Method method : FTPEnvironment.class.getMethods()) {
            if (method.getReturnType() == FTPEnvironment.class) {
                parameters.add(new Object[] { method });
            }
        }
        return parameters;
    }

    @Test
    public void testMethodOverride() throws NoSuchMethodException {
        Method method = FTPSEnvironment.class.getMethod(parentMethod.getName(), parentMethod.getParameterTypes());
        assertEquals(FTPSEnvironment.class, method.getReturnType());
    }
}
