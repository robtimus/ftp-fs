/*
 * TransferOptions.java
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
 * The base class of option combinations that support file transfers.
 *
 * @author Rob Spoor
 */
abstract class TransferOptions {

    public final FileType fileType;
    public final FileStructure fileStructure;
    public final FileTransferMode fileTransferMode;

    TransferOptions(FileType fileType, FileStructure fileStructure, FileTransferMode fileTransferMode) {
        this.fileType = fileType;
        this.fileStructure = fileStructure;
        this.fileTransferMode = fileTransferMode;
    }
}
