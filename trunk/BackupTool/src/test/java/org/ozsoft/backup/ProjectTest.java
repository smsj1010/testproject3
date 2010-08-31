package org.ozsoft.backup;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ozsoft.backup.util.FileHelper;

/**
 * Test suite for the Project class.
 * 
 * @author Oscar Stigter
 */
public class ProjectTest {
    
    /** Temporary test directory. */
    private static final String TEST_DIR = "target/test"; 
    
    /**
     * Sets up the environment before any test is run.
     */
    @BeforeClass
    public static void beforeClass() {
        // Create temporary test directory.
        new File(TEST_DIR).mkdir();
    }
    
    /**
     * Cleans up the environment after all tests have run.
     */
    @AfterClass
    public static void afterClass() {
        // Delete temporary test directory. 
        FileHelper.deleteFile(TEST_DIR);
    }
    
    /**
     * Sets up the environment before each individual test.
     */
    @Before
    public void before() {
        // Clean temporary test directory.
        FileHelper.cleanDir(TEST_DIR);
    }
    
    /**
     * Tests creating a backup.
     * 
     * @throws IOException
     *             In case of an I/O error.
     */
    @Test
    public void backup() throws IOException {
        Project project = new Project("Project1");
        project.addSourceFolder(TEST_DIR + "/folder1");
        project.addSourceFolder(TEST_DIR + "/folder2");
        project.setDestinationFolder(TEST_DIR + "/backup");
        
        // Create some (empty) source folders.
        File folder1 = new File(TEST_DIR, "folder1");
        folder1.mkdir();
        File folder2 = new File(TEST_DIR, "folder2");
        folder2.mkdir();

        // Create backup with ID 1 (no files).
        project.createBackup();
        
        // Test backup.
        Map<Integer, Long> backups = project.getBackups();
        Assert.assertEquals(1, backups.size());
        Assert.assertTrue(backups.containsKey(1));
        Assert.assertFalse(backups.containsKey(2));
        Map<String, BackupFile> backupFiles = project.getBackupFiles();
        Assert.assertEquals(0, backupFiles.size());

        // Create some files.
        FileHelper.writeTextFile(folder1, "file-1001.txt", "1001_v1");
        FileHelper.writeTextFile(folder1, "file-1002.txt", "1002_v1");
        FileHelper.writeTextFile(folder2, "file-2001.txt", "2001_v1");
        FileHelper.writeTextFile(folder2, "file-2002.txt", "2002_v1");
        
        // Create backup with ID 2 (4 new files).
        project.createBackup();

        // Update a file.
        FileHelper.writeTextFile(folder1, "file-1001.txt", "1001_v2");
        // Delete a file.
        FileHelper.deleteFile(new File(folder2, "file-2001.txt"));

        // Create backup with ID 3 (1 updated file).
        project.createBackup();

        // Test backup.
        backups = project.getBackups();
        Assert.assertEquals(3, backups.size());
        Assert.assertTrue(backups.containsKey(1));
        Assert.assertTrue(backups.containsKey(2));
        Assert.assertTrue(backups.containsKey(3));
        Assert.assertFalse(backups.containsKey(4));
        backupFiles = project.getBackupFiles();
        Assert.assertEquals(4, backupFiles.size());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (BackupFile file : backupFiles.values()) {
            System.out.println(file);
            for (BackupFileVersion version : file.getVersions()) {
                System.out.format("  %d, %s, %d, %d\n", version.getBackupId(),
                        dateFormat.format(version.getDate()), version.getOffset(), version.getLength());
            }
        }
        
        // Delete file.
        File file = new File(folder1, "file-1001.txt");
        FileHelper.deleteFile(file);
        

        // Restore files.
        project.restoreFile(file.getAbsolutePath(), -1);
        Assert.assertTrue(FileHelper.readTextFile(file).contains("1001_v2"));

        project.restoreFile(file.getAbsolutePath(), 2);
        Assert.assertTrue(FileHelper.readTextFile(file).contains("1001_v1"));

        project.restoreFile(file.getAbsolutePath(), 3);
        Assert.assertTrue(FileHelper.readTextFile(file).contains("1001_v2"));

        file = new File(folder1, "file-1002.txt");
        project.restoreFile(file.getAbsolutePath(), -1);
        Assert.assertTrue(FileHelper.readTextFile(file).contains("1002_v1"));
    }
    
}
