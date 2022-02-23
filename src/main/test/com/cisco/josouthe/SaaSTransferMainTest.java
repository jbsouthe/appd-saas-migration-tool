package com.cisco.josouthe;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SaaSTransferMainTest {
    private static final Logger logger = LogManager.getFormatterLogger();

    @Before
    public void setUp() {
        Configurator.setAllLevels("", Level.ALL);
    }
    @org.junit.jupiter.api.Test
    void main() throws Exception {
        String configFileName = "test-db.xml";
        Configuration config = null;
        logger.info("parsing: %s",configFileName);
        config = new Configuration(configFileName);

    }
}