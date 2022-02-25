package com.cisco.josouthe;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GenerateTestDataTest {

    @Test
    void main() throws IOException {
        GenerateTestData.main("90days-southerland-test-application");
    }
}