/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.loader.reader.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.Log;

public class FileReader extends AbstractFileReader {

    private static final Logger LOG = Log.logger(FileReader.class);

    public FileReader(FileSource source) {
        super(source);
    }

    @Override
    protected InputStream open(FileSource source) throws IOException {
        LOG.info("Opening file {}", source.path());
        File file = FileUtils.getFile(source.path());
        checkFile(file);
        return new FileInputStream(file);
    }

    private static void checkFile(File file) {
        if (!file.exists()) {
            throw new LoadException("Please ensure the file exist: '%s'", file);
        }
        if (!file.isFile()) {
            throw new LoadException("Please ensure the file is indeed a file " +
                                    "instead of a directory: '%s'", file);
        }
        if (!file.canRead()) {
            throw new LoadException("Please ensure the file is readable: '%s'",
                                    file);
        }
    }
}
