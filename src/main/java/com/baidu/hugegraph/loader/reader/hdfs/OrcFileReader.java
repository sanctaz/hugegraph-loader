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

package com.baidu.hugegraph.loader.reader.hdfs;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.Reader;
import org.apache.hadoop.hive.ql.io.orc.RecordReader;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.hdfs.HDFSSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public class OrcFileReader extends HDFSReader {

    private static final Logger LOG = Log.logger(OrcFileReader.class);

    private Reader reader;
    private StructObjectInspector inspector;
    private List<String> names;
    private RecordReader records;

    public OrcFileReader(HDFSSource source) {
        super(source);
        this.reader = null;
        this.inspector = null;
        this.names = null;
        this.records = null;
    }

    @Override
    public void init() {
        try {
            this.reader = OrcFile.createReader(this.hdfs, this.path);
            this.inspector = (StructObjectInspector)
                             this.reader.getObjectInspector();
            List<? extends StructField> fields = this.inspector
                                                     .getAllStructFieldRefs();
            this.names = fields.stream().map(StructField::getFieldName)
                               .collect(Collectors.toList());
            this.records = this.reader.rows();
        } catch (IOException e) {
            throw new LoadException("Failed to init orc file reader", e);
        }
    }

    @Override
    public Line fetch() {
        try {
            if (!this.records.hasNext()) {
                return null;
            }

            Object row = this.records.next(null);
            if (row == null) {
                return null;
            }

            E.checkArgument(row instanceof String,
                            "The orc raw record must be readed as string");
            List<Object> values = this.inspector.getStructFieldsDataAsList(row);
            return new Line((String) row, this.names, values);
        } catch (IOException e) {
            throw new LoadException("Read next line error", e);
        }
    }
}
