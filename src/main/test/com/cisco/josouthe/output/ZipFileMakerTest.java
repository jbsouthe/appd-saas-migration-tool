package com.cisco.josouthe.output;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ZipFileMakerTest extends TestCase {

    public ZipFileMakerTest() {}

    @Test
    public void testZipGeneration() throws Exception {
        List<File> fileList = new ArrayList<>();
        fileList.add( new File("README.md"));
        fileList.add( new File("NOTICE.txt"));
        fileList.add( new File("LICENSE"));
        new ZipFileMaker(".", "southerland-test", fileList);
    }
}