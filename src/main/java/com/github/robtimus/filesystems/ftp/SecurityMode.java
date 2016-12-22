/*
 * SecurityMode.java
 * Copyright 2016 Rob Spoor
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

/**
 * The possible FTPS security modes.
 *
 * @author Rob Spoor
 */
public enum SecurityMode {
    /** Indicates <a href="https://en.wikipedia.org/wiki/FTPS#Implicit">implicit</a> security should be used. */
    IMPLICIT(true),
    /** Indicates <a href="https://en.wikipedia.org/wiki/FTPS#Explicit">explicit</a> security should be used. */
    EXPLICIT(false),
    ;

    final boolean isImplicit;

    SecurityMode(boolean isImplicit) {
        this.isImplicit = isImplicit;
    }
}
