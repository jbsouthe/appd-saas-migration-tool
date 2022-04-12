package com.cisco.josouthe.output;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZipFileMakerTest extends TestCase {

    public ZipFileMakerTest() {}

    @Test
    public void testZipGeneration() throws Exception {
        new File("southerland-test-SaasMigrationExport.zip").delete();
        List<File> fileList = new ArrayList<>();
        fileList.add( new File("README.md"));
        fileList.add( new File("NOTICE.txt"));
        fileList.add( new File("LICENSE"));
        new ZipFileMaker(".", "southerland-test", fileList, null);
        assert new File("southerland-test-SaasMigrationExport.zip").exists();
    }
}