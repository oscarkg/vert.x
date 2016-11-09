package org.vertx.java.platform.configuration;

import org.vertx.java.core.json.JsonObject;

/**
 * This interface is the one to be implemented if you want to create your own configuration loader.
 * It will receive the path to the configuration and has to return a json string which is the configuration
 * correctly loaded.
 */
public interface ConfigurationLoader {

  /**
   * This method reads a file and returns its content as a string.
   * @param configFilePath the full path to the file
   * @return the file content as string
   */
  JsonObject load(String configFilePath) throws Exception;
    
}
