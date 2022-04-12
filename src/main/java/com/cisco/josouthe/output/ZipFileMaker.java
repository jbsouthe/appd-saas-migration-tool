package com.cisco.josouthe.output;

import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZipFileMaker {
    protected static final Logger logger = LogManager.getFormatterLogger();

    public ZipFileMaker( String targetDir, String controllerHostName, List<File> files, ProgressBar progressBar ) {
        if( progressBar != null ) progressBar.setExtraMessage("Create Zip Output File");
        Map<String,String> env = new HashMap<>();
        env.put("create","true");
        URL fileURL = null;
        try {
            fileURL = new File(targetDir, controllerHostName+"-SaaSMigrationExport.zip").toURL();
            if( progressBar != null ) progressBar.step();
        } catch (MalformedURLException e) {
            logger.error("Failed to create valid URL for target file as %s/%s-SaaSMigrationExport.zip, please create it manually", targetDir, controllerHostName);
            return;
        }
        URI uri =  URI.create("jar:file:"+fileURL.getPath().replace(" ", "%20"));

        logger.info("Creating output zip file: %s", uri.getPath());
        try(FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            for( File file : files ) {
                logger.debug("Adding file: %s",file);
                Path externalPath = Paths.get(file.getAbsolutePath());
                Path internalPath = zipfs.getPath(file.getName());
                Files.copy(externalPath, internalPath, StandardCopyOption.REPLACE_EXISTING);
                if( progressBar != null ) progressBar.step();
                logger.info("Added file %s to zip", file);
            }
            zipfs.close();
            logger.info("Finished creating zip file: %s",uri);
        } catch (IOException ioException) {
            logger.error("IOException while compressing a zip package, %s,  for transit to AppDynamics, please create it manually. Exception: %s", fileURL.getFile(), ioException);
        }
        if( progressBar != null ) progressBar.step();
    }

}
