package com.telecominfraproject.wlan.hierarchical.datastore;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HdsCommonTests {
	
    private static final Logger LOG = LoggerFactory.getLogger(HierarchicalDatastore.class);

    public static void removeAllHdsFiles(File dsRootDir){

    	if(dsRootDir.getAbsolutePath().equals("/")) {
    		throw new IllegalArgumentException("attempting to delete / - please make sure your dsRootDirName and ds Prefix are not empty strings!");
    	}
    	
    	try {
			Files.walkFileTree(dsRootDir.toPath(), new SimpleFileVisitor<Path>() {
			    @Override
			    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			        throws IOException
			    {
			        Files.delete(file);
			        return FileVisitResult.CONTINUE;
			    }
			    @Override
			    public FileVisitResult postVisitDirectory(Path dir, IOException e)
			        throws IOException
			    {
			        if (e == null) {
			            Files.delete(dir);
			            return FileVisitResult.CONTINUE;
			        } else {
			            // directory iteration failed
			            throw e;
			        }
			    }
			});
		} catch (IOException e) {
			LOG.error("Exception when deleting files ", e);
		}
    	    	
    	dsRootDir.delete();
    }   

}
