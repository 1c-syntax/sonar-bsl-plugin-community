/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2020
 * Nikita Gryzlov <nixel2007@gmail.com>
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * SonarQube 1C (BSL) Community Plugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * SonarQube 1C (BSL) Community Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with SonarQube 1C (BSL) Community Plugin.
 */
package com.github._1c_syntax.bsl.sonar;

import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Tools {

    public static InputFile inputFileBSL(String name, File baseDir) {

        File file = new File(baseDir.getAbsoluteFile(), name);
        String content;
        try {
            content = readFile(file.toPath().toString());
        } catch (IOException e) {
            content = "Значение = 1; Значение2 = 1;";
        }

        DefaultInputFile inputFile = TestInputFileBuilder.create("moduleKey", name)
                .setModuleBaseDir(baseDir.toPath())
                //.setCharset(StandardCharsets.UTF_8)
                .setType(InputFile.Type.MAIN)
                .setLanguage(BSLLanguage.KEY)
                .initMetadata(content)
                .build();
        return inputFile;
    }

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }
}
