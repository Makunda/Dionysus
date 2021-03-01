/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.dionysus.configuration;

import com.castsoftware.dionysus.database.Neo4jAL;
import com.castsoftware.dionysus.exceptions.file.MissingFileException;
import com.castsoftware.dionysus.utils.Workspace;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Retrieve the Configuration of the user config file */
public class UserConfiguration {

  private static Properties PROPERTIES;

  /**
   * Get the corresponding value for the specified key as a String If the configuration file doesn't
   * exist, returns Null
   *
   * @param key
   * @see this.getAsObject to get the value as an object
   * @return <code>String</code> value for the key as a String
   */
  public static String get(Neo4jAL neo4jAL, String key) {
    if (PROPERTIES == null) {
      PROPERTIES = loadConfiguration(neo4jAL);
    }

    assert PROPERTIES != null: "Failed to load the user configuration";

    return (String) PROPERTIES.get(key);
  }

  /**
   * Load the user configuration file
   *
   * @return The list properties found in the configuration file.
   */
  private static Properties loadConfiguration(Neo4jAL neo4jAL) {
    Path configurationPath = Workspace.getUserConfigPath(neo4jAL);

    if (!Files.exists(configurationPath)) {
      System.err.printf("No configuration file found at path : %s%n", configurationPath.toString());
      return null;
    }

    try (InputStream input = new FileInputStream(configurationPath.toFile())) {
      Properties prop = new Properties();

      if (input == null) {
        throw new MissingFileException(
            "No file 'artemis.properties' was found.",
            "resources/procedure.properties",
            "CONFxLOAD1");
      }

      // load a properties file from class path, inside static method
      prop.load(input);
      return prop;
    } catch (IOException | MissingFileException ex) {
      System.err.println(ex.getMessage());
      return null;
    }
  }

  /**
   * Verify if the key is present
   *
   * @param key
   * @return
   */
  public static Boolean has(Neo4jAL neo4jAL, String key) {
    if (PROPERTIES == null) {
      PROPERTIES = loadConfiguration(neo4jAL);
    }

    if (PROPERTIES == null) return false;
    return PROPERTIES.contains(key);
  }

  public static String set(Neo4jAL neo4jAL, String key, String value) throws MissingFileException {
    if (PROPERTIES == null) {
      PROPERTIES = loadConfiguration(neo4jAL);
    }

    assert PROPERTIES != null: "Failed to load the configuration";
    PROPERTIES.setProperty(key, value);
    saveAndReload(neo4jAL);
    PROPERTIES = loadConfiguration(neo4jAL);
    return PROPERTIES.get(key).toString();
  }

  /**
   * Save the configuration and reload it
   *
   * @throws FileNotFoundException
   */
  public static void saveAndReload(Neo4jAL neo4jAL) throws MissingFileException {

    if (PROPERTIES == null) {
      PROPERTIES = loadConfiguration(neo4jAL);
    }

    Path configurationPath = Workspace.getUserConfigPath(neo4jAL);

    try (FileOutputStream file = new FileOutputStream(configurationPath.toFile())) {

      file.write("# Artemis Configuration properties".concat(System.lineSeparator()).getBytes());
      file.write(
          "# For more information please refer to the documentation on Github : https://github.com/CAST-Extend/com.castsoftware.uc.artemis/wiki"
              .concat(System.lineSeparator())
              .getBytes());

      SortedMap sortedSystemProperties = new TreeMap(PROPERTIES);
      Set keySet = sortedSystemProperties.keySet();
      Iterator iterator = keySet.iterator();

      String lastIdentifier = null;
      String prop;
      while (iterator.hasNext()) {
        String propertyName = (String) iterator.next();
        String propertyValue = PROPERTIES.getProperty(propertyName);

        String[] currentId = propertyName.split("\\.");
        // Add space between categories
        if (lastIdentifier != null && currentId.length > 0) {
          if (!lastIdentifier.equals(currentId[0])) {
            file.write(System.lineSeparator().getBytes());
          }
        }

        if (currentId.length > 0) {
          lastIdentifier = currentId[0];
        }

        prop = propertyName + "=" + propertyValue + System.lineSeparator();
        file.write(prop.getBytes());
      }

      file.flush();
    } catch (IOException e) {
      throw new MissingFileException(
          "No file 'artemis.properties' was found.", configurationPath.toString(), "CONFxLOAD1");
    }
  }

  /**
   * Get the corresponding value for the specified key as an object. If the configuration file
   * doesn't exist, returns Null
   *
   * @param key
   * @return <Object>String</code> value for the key as a string
   */
  public static Object getAsObject(Neo4jAL neo4jAL, String key) {
    if (PROPERTIES == null) {
      PROPERTIES = loadConfiguration(neo4jAL);
    }

    return PROPERTIES.get(key);
  }

  /**
   * Check if the properties file if valid
   *
   * @return
   */
  public static Boolean isLoaded() {
    return PROPERTIES != null;
  }

  public static Set<Object> getKeySet() {
    return PROPERTIES.keySet();
  }

  /**
   * Check the presence of a key
   *
   * @param key
   * @return
   */
  public static boolean isKey(Neo4jAL neo4jAL, String key) {
    try {
      if (PROPERTIES == null) {
        PROPERTIES = loadConfiguration(neo4jAL);
      }

      return PROPERTIES.containsKey(key);
    } catch (NullPointerException e) {
      return false;
    }
  }

  /**
   * Reload the configuration from the file
   *
   * @return
   */
  public static Properties reload(Neo4jAL neo4jAL) {
    PROPERTIES = loadConfiguration(neo4jAL);
    return PROPERTIES;
  }
}
