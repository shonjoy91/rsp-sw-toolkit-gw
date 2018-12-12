/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class FileHelper {


    public static void copy(Path _from, Path _to) throws IOException {
        Files.walkFileTree(_from, new CopyDirVisitor(_from, _to, StandardCopyOption.REPLACE_EXISTING));
    }

    public static class CopyDirVisitor extends SimpleFileVisitor<Path> {
        private final Path from;
        private final Path to;
        private final CopyOption option;

        public CopyDirVisitor(Path _from, Path _to, CopyOption _option) {
            from = _from;
            to = _to;
            option = _option;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path _dir, BasicFileAttributes _bfa) throws IOException {
            Path targetPath = to.resolve(from.relativize(_dir));
            if (!Files.exists(targetPath)) {
                Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, to.resolve(from.relativize(file)), option);
            return FileVisitResult.CONTINUE;
        }
    }

}
