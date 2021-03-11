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

package com.castsoftware.paris.utils;

import com.castsoftware.paris.configuration.Configuration;
import com.castsoftware.paris.configuration.NodeConfiguration;
import com.castsoftware.paris.configuration.UserConfiguration;

import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.file.MissingFileException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jBadRequestException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Workspace {

  /**
   * Change the Workspace of paris
   *
   * @param directoryPath New directory path
   * @return
   * @throws MissingFileException
   */
  public static List<String> setWorkspacePath(Neo4jAL neo4jAL, String directoryPath) throws MissingFileException {
    Path newDirectory = Path.of(directoryPath);

    if (!Files.exists(newDirectory)) {
      return List.of(
          String.format(
              "'%s' is not a valid path. Make sure the target folder exists and retry.",
              directoryPath));
    }


    try {
      NodeConfiguration nc = NodeConfiguration.getInstance(neo4jAL);
      nc.updateWorkspace(newDirectory.toAbsolutePath().toString());
    } catch (Neo4jQueryException | Neo4jBadRequestException e) {
      neo4jAL.logError("Failed to save the new workspace to the Neo4j base");
    }

    // Temporary set
    Configuration.set("paris.workspace.folder", newDirectory.toAbsolutePath().toString());

    // Validate the workspace
    List<String> outputMessages = Workspace.validateWorkspace(neo4jAL);

    // Generate Workspace


    // Reload User configuration
    UserConfiguration.reload(neo4jAL);

    outputMessages.add(
        String.format("paris workspace folder was successfully changed to '%s'.",  newDirectory));
    return outputMessages;
  }

  /**
   * Validate if the workspace is valid and contains all the paris mandatory files
   *
   * @return List of message to be displayed
   */
  public static List<String> validateWorkspace(Neo4jAL neo4jAL) {
    List<String> messageOutputList = new ArrayList<>();

    Path workspacePath = Workspace.getWorkspacePath(neo4jAL);
    Path dataFolder = workspacePath.resolve(Configuration.get("paris.install_data.folder"));
    Path installData =
        dataFolder.resolve(Configuration.get("paris.install_data.paris_group_file"));

    // Check if the folder is valid
    if (!Files.exists(workspacePath)) {
      messageOutputList.add(
          String.format(
              "ERROR : %s does not exist. Please specify an existing directory.",
              workspacePath.toString()));
      return messageOutputList;
    }


    // Check the existent of the user configuration file
    if (!Files.exists(installData)) {
      messageOutputList.add(
          String.format(
              "ERROR :  Data initialization zip '%s' is missing. The initialization will not work without this file.",
              Configuration.get("paris.install_data.paris_group_file")));
    }



    return messageOutputList;
  }

  /**
   * Get the current path of the current paris Workspace
   *
   * @return Path of the workspace
   */
  public static Path getWorkspacePath(Neo4jAL neo4jAL) {
      try {
        return NodeConfiguration.getWorkspaceNodeConf(neo4jAL);
      } catch (Neo4jQueryException | Neo4jBadRequestException e) {
        neo4jAL.logError("Failed to retrieve the workspace path set in the configuration node");
        return Path.of(Configuration.get("paris.workspace.folder"));
      }

  }

  /**
   * Check if the folder exist. If not, create it
   *
   * @param folderPath Path of the folder to check
   * @param name Name of the folder
   * @return
   */
  private static List<String> checkOrCreateFolder(Path folderPath, String name) {
    List<String> messageOutputList = new ArrayList<>();
    // Check main folders and create if necessary
    if (!Files.exists(folderPath)) {
      try {
        Files.createDirectory(folderPath);
        messageOutputList.add(String.format("%s was missing and has been created.", name));
      } catch (IOException e) {
        messageOutputList.add(
            String.format(
                "ERROR : %s is missing and its creation failed : %s", name, e.getMessage()));
      }
    }

    return messageOutputList;
  }


  /**
   * Get the supposed path of the initialization data file
   *
   * @return
   */
  public static Path getInitDataZip(Neo4jAL neo4jAL) {
    Path workspace = getWorkspacePath(neo4jAL);
    Path dataFolder = workspace.resolve(Configuration.get("paris.install_data.folder"));
    return dataFolder.resolve(Configuration.get("paris.install_data.paris_group_file"));
  }

  /**
   * Get the path  of the user configuration file
   * @param neo4jAL Neo4j Access Layer
   * @return
   */
  public static Path getUserConfigPath(Neo4jAL neo4jAL) {
    Path workspace = getWorkspacePath(neo4jAL);
    return workspace.resolve(Configuration.get("paris.config.user.conf_file"));
  }


}
