/*
 *  Copyright (c) 2017 Works Applications Co., Ltd.
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

package com.worksap.nlp.lucene.sudachi.ja;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

class ResourceUtil {
    static final String RESOURCE_NAME_SYSTEM_DIC = "system_core.dic";

    private ResourceUtil() {}

    static void copy(File destDir) throws IOException {
        copyResource(RESOURCE_NAME_SYSTEM_DIC, destDir, false);
    }

    static void copyResource(String filename, File destDir, boolean fromRoot)
        throws IOException {
        String src = (fromRoot) ? "/" + filename : filename;
        try (InputStream stream = ResourceUtil.class.getResourceAsStream(src)) {
            Files.copy(stream, destDir.toPath().resolve(filename));
        }
    }
}
