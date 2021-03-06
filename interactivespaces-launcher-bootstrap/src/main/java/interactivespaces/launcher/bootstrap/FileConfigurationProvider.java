/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package interactivespaces.launcher.bootstrap;

import interactivespaces.system.core.configuration.ConfigurationProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A configuration provider from a file.
 *
 * @author Keith M. Hughes
 */
public class FileConfigurationProvider implements ConfigurationProvider {

  /**
   * Where the config files are stored.
   */
  public static final String CONFIG_DIRECTORY = "config";

  /**
   * Extensions on config files.
   */
  private static final String CONFIGURATION_FILES_EXTENSION = ".conf";

  /**
   * Environment variable that indicates the home install directory for interactive spaces.
   */
  private static final String INTERACTIVESPACES_HOME_ENVIRONMENT_KEY = "INTERACTIVESPACES_HOME";

  /**
   * Default directory for the home directory relative to install of this component.
   */
  private static final String INTERACTIVESPACES_HOME_DEFAULT_DIR = "..";

  /**
   * Configuration key for interactive spaces home directory.
   */
  private static final String INTERACTIVESPACES_HOME_CONFIG_KEY = "interactivespaces.home";

  /**
   * The initial configuration
   */
  private File baseInstallFolder;

  /**
   * The current configuration.
   */
  private Map<String, String> currentConfiguration;

  /**
   * Create a new provider.
   *
   * @param baseInstallFolder
   *          base install folder for this component
   */
  public FileConfigurationProvider(File baseInstallFolder) {
    this.baseInstallFolder = baseInstallFolder;
  }

  @Override
  public Map<String, String> getInitialConfiguration() {
    return currentConfiguration;
  }

  /**
   * Load all conf files in the configuration folder.
   */
  public void load() {
    currentConfiguration = new HashMap<String, String>();

    // Calculate the proper home directory for this install of interactive spaces.
    String isHomeEnvPath = System.getenv(INTERACTIVESPACES_HOME_ENVIRONMENT_KEY);
    File isHomeDir = isHomeEnvPath != null ? new File(isHomeEnvPath)
        : new File(baseInstallFolder, INTERACTIVESPACES_HOME_DEFAULT_DIR);
    currentConfiguration.put(INTERACTIVESPACES_HOME_CONFIG_KEY, isHomeDir.getAbsolutePath());

    // Look in the specified bundle directory to create a list
    // of all JAR files to install.
    File[] files = new File(baseInstallFolder, CONFIG_DIRECTORY).listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(CONFIGURATION_FILES_EXTENSION);
      }
    });
    if (files == null || files.length == 0) {
      System.err.format("Couldn't load config files from %s\n", CONFIG_DIRECTORY);
    }

    for (File file : files) {
      Properties props = new Properties();
      try {
        props.load(new FileInputStream(file));
        for (Entry<Object, Object> p : props.entrySet()) {
          currentConfiguration.put((String) p.getKey(), (String) p.getValue());
        }
      } catch (IOException e) {
        System.err.format("Couldn't load config file %s\n", file);
      }
    }
  }
}
