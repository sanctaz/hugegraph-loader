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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.NoSuchElementException;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.file.Compression;
import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.Log;

public abstract class AbstractFileReader implements InputReader {

    private static final Logger LOG = Log.logger(AbstractFileReader.class);

    private static final int BUF_SIZE = 5 * 1024 * 1024;

    private final FileSource source;
    private LineReader reader;
    private LineParser parser;

    private Line nextLine;

    public AbstractFileReader(FileSource source) {
        this.source = source;
        this.nextLine = null;
    }

    public FileSource source() {
        return this.source;
    }

    @Override
    public void init() {
        try {
            this.reader = this.createLineReader(this.source);
            this.parser = this.createLineParser(this.source);
        } catch (Exception e) {
            throw new LoadException("Failed to load input in path '%s'",
                                    e, this.source.path());
        }
        // TODO: FileReader and LineParser interdependent
        this.parser.init(this);
    }

    @Override
    public String line() {
        if (this.nextLine == null) {
            return null;
        }
        return this.nextLine.rawLine();
    }

    @Override
    public boolean hasNext() {
        if (this.nextLine != null) {
            return true;
        }
        this.nextLine = this.fetch();
        return this.nextLine != null;
    }

    protected Line fetch() {
        String rawLine = this.nextLine();
        if (rawLine == null) {
            return null;
        }
        // Skip the comment line
        if (this.isCommentLine(rawLine)) {
            return this.fetch();
        } else {
            return this.parser.parse(rawLine);
        }
    }

    /**
     * package private for internal use only
     */
    protected String nextLine() {
        try {
            return this.reader.nextLine();
        } catch (IOException e) {
            throw new LoadException("Read next line error", e);
        }
    }

    @Override
    public Line next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException("Reach end of file");
        }
        Line line = this.nextLine;
        this.nextLine = null;
        return line;
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    private boolean isCommentLine(String line) {
        return this.source.commentSymbols().stream().anyMatch(line::startsWith);
    }

    private LineReader createLineReader(FileSource source) throws Exception {
        InputStream fis = null;
        try {
            fis = this.open(source);
            Reader csr = this.createCompressReader(fis, source);
            return new LineReader(new BufferedReader(csr, BUF_SIZE));
        } catch (Exception e) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                    LOG.warn("Failed to close file {}", source.path());
                }
            }
            throw e;
        }
    }

    private LineParser createLineParser(FileSource source) {
        FileFormat format = source.format();
        switch (format) {
            case CSV:
                return new CsvLineParser();
            case TEXT:
                return new TextLineParser();
            case JSON:
                return new JsonLineParser();
            default:
                throw new AssertionError(String.format(
                          "Unsupported file format '%s'", source));
        }
    }

    private Reader createCompressReader(InputStream is, FileSource source)
                                        throws Exception {
        Compression compression = source.compression();
        String charset = source.charset();
        switch (compression) {
            case NONE:
                return new InputStreamReader(is, charset);
            case BZ2:
            case GZIP:
            case PACK200:
            case XZ:
            case LZMA:
            case SNAPPY_FRAMED:
            case SNAPPY_RAW:
            case Z:
            case DEFLATE:
                CompressorStreamFactory factory = new CompressorStreamFactory();
                CompressorInputStream cis = factory.createCompressorInputStream(
                                                    compression.string(), is);
                return new InputStreamReader(cis, charset);
            case ZIP:
                InputStream zis = new ZipArchiveInputStream(is);
                return new InputStreamReader(zis, charset);
            case ORC:
                throw new LoadException("Some exception occured, orc compression " +
                                        "should loaded by OrcFileReader instead " +
                                        "AbstractFileReader");
            default:
                throw new LoadException("Unsupported compressed file '%s'",
                                        compression);
        }
    }

    protected abstract InputStream open(FileSource source)
                                                   throws IOException;
}
