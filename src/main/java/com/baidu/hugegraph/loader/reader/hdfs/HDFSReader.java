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
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.file.FileReader;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.source.hdfs.HDFSSource;
import com.baidu.hugegraph.util.Log;

public class HDFSReader extends FileReader {

    private static final Logger LOG = Log.logger(HDFSReader.class);

    protected final Configuration conf;
    protected FileSystem hdfs;
    protected Path path;

    public HDFSReader(HDFSSource source) {
        super(source);
        this.conf = loadConfiguration();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (this.hdfs != null) {
            this.hdfs.close();
        }
    }

    @Override
    protected InputStream open(FileSource source) throws IOException {
        LOG.info("Opening HDFS file {}", source.path());
        URI hdfsUri = URI.create(source.path());
        try {
            this.hdfs = FileSystem.get(hdfsUri, this.conf);
        } catch (IOException e) {
            throw new LoadException("Failed to create load file system", e);
        }

        this.path = new Path(source.path());
        checkPath(this.path, this.hdfs);

        return this.hdfs.open(this.path);
    }

    private static Configuration loadConfiguration() {
        Configuration conf = new Configuration();
        String hadoopHome = System.getenv("HADOOP_HOME");
        LOG.info("Get HADOOP_HOME {}", hadoopHome);
        String path = Paths.get(hadoopHome, "etc", "hadoop").toString();
        conf.addResource(path(path, "/core-site.xml"));
        conf.addResource(path(path, "/hdfs-site.xml"));
        conf.addResource(path(path, "/mapred-site.xml"));
        conf.addResource(path(path, "/yarn-site.xml"));
        return conf;
    }

    private static void checkPath(Path path, FileSystem fs) throws IOException {
        if (!fs.exists(path) || !fs.isFile(path)) {
            throw new LoadException("Invalid hdfs file path %s", path);
        }
    }

    private static Path path(String configPath, String configFile) {
        return new Path(Paths.get(configPath, configFile).toString());
    }
}
