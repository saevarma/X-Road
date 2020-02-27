/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.repository;

import ee.ria.xroad.common.SystemProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backups repository
 */
@Slf4j
@Repository
public class BackupsRepository {

    private static final String CONFIGURATION_BACKUP_PATH = SystemProperties.getConfBackupPath();
    // Criteria: cannot start with ".", must contain one or more word characters ([a-zA-Z_0-9]),
    // must end with ".tar"
    private static final String BACKUP_FILENAME_PATTERN = "^(?!\\.)[\\w\\.\\-]+\\.tar$";
    // Set maximum number of levels of directories to visit, subdirectories are excluded
    private static final int DIR_MAX_DEPTH = 1;

    /**
     * Read backup files from configuration backup path
     * @return
     */
    public List<File> getBackupFiles() {
        try (Stream<Path> walk = Files.walk(Paths.get(CONFIGURATION_BACKUP_PATH), DIR_MAX_DEPTH)) {
            return walk.filter(this::isFilenameValid).map(this::getFile).collect(Collectors.toList());
        } catch (IOException ioe) {
            log.error("can't read backup files from configuration path (" + CONFIGURATION_BACKUP_PATH + ")");
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Get the creation date/time of a backup file
     * @param filename
     * @return
     */
    public OffsetDateTime getCreatedAt(String filename) {
        Path path = getFilePath(filename);
        try {
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            return attr.creationTime().toInstant().atOffset(ZoneOffset.UTC);
        } catch (IOException ioe) {
            log.error("can't read backup file's creation time (" + path.toString() + ")");
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Delete a backup file
     * @param filename
     */
    public void deleteBackupFile(String filename) {
        Path path = getFilePath(filename);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ioe) {
            log.error("can't delete backup file (" + path.toString() + ")");
            throw new RuntimeException("deleting backup file failed");
        }
    }

    /**
     * Read backup file's content
     * @param filename
     * @return
     */
    public byte[] readBackupFile(String filename) {
        Path path = getFilePath(filename);
        try {
            return Files.readAllBytes(path);
        } catch (IOException ioe) {
            log.error("can't read backup file's content (" + path.toString() + ")");
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Return configuration backup path with a trailing slash
     * @return
     */
    public String getConfigurationBackupPath() {
        return CONFIGURATION_BACKUP_PATH  + (CONFIGURATION_BACKUP_PATH.endsWith(File.separator) ? "" : File.separator);
    }

    private File getFile(Path path) {
        return new File(path.toString());
    }

    private Path getFilePath(String filename) {
        return Paths.get(getConfigurationBackupPath() + filename);
    }

    /**
     * Check if the given filename is valid and meets the defined criteria
     * @param path
     * @return
     */
    private boolean isFilenameValid(Path path) {
        return Pattern.compile(BACKUP_FILENAME_PATTERN).matcher(path.getFileName().toString()).matches();
    }
}
