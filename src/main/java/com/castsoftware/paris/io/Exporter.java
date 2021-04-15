/*
 *  Friendly exporter for Neo4j - Copyright (C) 2020  Hugo JOBY
 *
 *      This library is free software; you can redistribute it and/or modify it under the terms
 *      of the GNU Lesser General Public License as published by the Free Software Foundation;
 *      either version 2.1 of the License, or (at your option) any later version.
 *      This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *      without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *      See the GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License along with this library;
 *      If not, see <https://www.gnu.org/licenses/>.
 */

package com.castsoftware.paris.io;

import com.castsoftware.paris.configuration.Configuration;
import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.ProcedureException;
import com.castsoftware.paris.exceptions.file.FileIOException;
import com.castsoftware.paris.exceptions.file.FilePermissionException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.results.OutputMessage;
import com.castsoftware.paris.utils.Algorithms;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Exporter {

  // Default properties
  private static final String DELIMITER = Configuration.get("io.csv.delimiter");
  private static final String TEMP_VARIABLE_PREFIX = Configuration.get("io.temp_variable_prefix");
  private static final String EXTENSION = Configuration.get("io.csv.csv_extension");
  private static final String INDEX_COL = Configuration.get("io.index_col");

  private static final String INDEX_SRC = Configuration.get("io.index_outgoing");
  private static final String INDEX_TARGET = Configuration.get("io.index_incoming");
  private static final String RELATIONSHIP_NAME_COL = Configuration.get("io.relationship_prop");
  private static final String REL_VAL_COL = Configuration.get("io.rel_val_prop");
  private static final String DIRECTION_COL = Configuration.get("io.direction_prop");


  private static final String RELATIONSHIP_PREFIX =
      Configuration.get("io.file.prefix.relationship");
  private static final String NODE_PREFIX = Configuration.get("io.file.prefix.node");

  private final Neo4jAL neo4jAL;
  private final String tempVariable;

  public Exporter(Neo4jAL neo4jAL) {
    this.neo4jAL = neo4jAL;

    // Generate temp variables used to assign links
    this.tempVariable = TEMP_VARIABLE_PREFIX + Algorithms.getAlphaNumericString(5);
  }

  /**
   * Export all the model selected to a zipFile
   * @param models List of models
   * @param targetPath Path to the zip file
   * @return
   */
  public Path export(List<ExportModel> models, Path targetPath, String zipName) throws FileIOException, Neo4jQueryException {
    // Check if the path of the directory exist
    File file = targetPath.toFile();

    // Create the target directory
    if(!file.isDirectory() && !file.mkdirs()) {
      throw new FileIOException(String.format("Failed to create the directory for the export at %s.",
              targetPath.toString()), "EXPOxEXPO1");
    }

    List<Path> createdFiles = new ArrayList<>();

    // Export models
    for(ExportModel em : models) {
      createdFiles.add(exportModelToCsv(em, targetPath));
    }

    // Export relationships
    for(ExportModel em : models) {
      List<Path> createdRelFiles = exportRelationshipToCsv(em, targetPath);
      if(createdRelFiles == null) continue;

      createdFiles.addAll(createdRelFiles);
    }

    neo4jAL.logInfo(String.format("%d files were created and are about to be zipped...", createdFiles.size()));

    // Clean temporary variable
    cleanTempProperty(models);

    // Create the zip file
    return createZip(targetPath, zipName, createdFiles);
  }


  /**
   * Appends all the files created during this process to the target zip. Every file appended will
   * be remove once added to the zip.
   *
   * @param zipFileName Name of the ZipFile
   * @return The path of the created zipile
   * @throws FileIOException
   */
  private Path createZip(Path folderPath, String zipFileName, List<Path> files) throws FileIOException {
    String filename = zipFileName+".zip";
    File f = folderPath.resolve(filename).toFile();

    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(f))) {

      for (Path path : files) {
        File fileToZip = folderPath.resolve(path).toFile();

        try (FileInputStream fileStream = new FileInputStream(fileToZip)) {
          ZipEntry e = new ZipEntry(path.getFileName().toString());
          zipOut.putNextEntry(e);

          byte[] bytes = new byte[1024];
          int length;
          while ((length = fileStream.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
        } catch (Exception e) {
          this.neo4jAL.logError("An error occurred trying to zip file with name : ".concat(path.toString()), e);
        }

        if (!fileToZip.delete())
          this.neo4jAL.logError("Error trying to delete file with name : ".concat(path.toString()));
      }

      return f.toPath();

    } catch (IOException e) {
      this.neo4jAL.logError("An error occurred trying create zip file with name : ".concat(zipFileName), e);
      throw new FileIOException(
          "An error occurred trying create zip file with name.", e, "SAVExCZIP01");
    }
  }


  /**
   * Export the model to a csv file
   * @param model Model to export
   * @param targetPath Path of the export
   * @return The Path of the created file
   */
  private Path exportModelToCsv(ExportModel model, Path targetPath) throws Neo4jQueryException, FileIOException {
    StringBuilder csvBuilder = new StringBuilder();

    Label nodeLabel = Label.label(model.getLabel());
    String getAllReq = String.format("MATCH (o:%s) RETURN o as node", model.getLabel());
    Result res = this.neo4jAL.executeQuery(getAllReq);

    // Retrieve all the nodes with the specified label
    List<Node> allNodes = new ArrayList<>();
    while (res.hasNext()) allNodes.add((Node) res.next().get("node"));

    // Add Id column
    csvBuilder.append(INDEX_COL).append(DELIMITER);
    // Write the columns to the csv
    csvBuilder.append(String.join(DELIMITER, model.getColumns())).append("\n");

    long uniqueId = 0L;
    List<String> tempProperties;
    for(Node n : allNodes) {
      tempProperties = new ArrayList<>();

      // Add Id to value
      tempProperties.add(String.valueOf(uniqueId));
      n.setProperty(tempVariable, uniqueId);
      uniqueId ++;

      // Extract the desired values in the nodes
      for (String col : model.getColumns()) {
        try {
          tempProperties.add(neo4jTypeToString(n, col));
        } catch (Exception ex) {
          // Ignore exception and add empty val
          tempProperties.add("");
        }
      }

      // Write the values to the csv
      csvBuilder.append(String.join(DELIMITER, tempProperties)).append("\n");
    }

    // Add label
    String filename = String.format("%s%s%s", NODE_PREFIX, model.getLabel(), EXTENSION);
    File file = targetPath.resolve(filename).toFile();
    try ( FileWriter fw = new FileWriter(file) ) {
      fw.write(csvBuilder.toString());
      fw.flush();
    } catch (IOException eo) {
      throw new FileIOException(String.format("Failed to create temporary .csv file at %s", targetPath.toString()), eo, "EXPOxEMTC1");
    }

    return file.toPath();
  }

  /**
   * Export the list of the neighbors nodes attached to a model. The neighbors models need to be exported first
   * @param model Model to export
   * @param targetPath Directory where the file will be created
   * @return
   * @throws Neo4jQueryException
   * @throws FileIOException
   */
  private List<Path> exportRelationshipToCsv(ExportModel model, Path targetPath) throws Neo4jQueryException, FileIOException {

    // If Nothing to export
    if(model.getNeighborsLabel().isEmpty()) return null;

    List<Path> createdFile = new ArrayList<>();
    Label toExplore = Label.label(model.getLabel());

    // Get all the nodes to explore
    List<Node> nodesToExplore = this.neo4jAL.findNodes(toExplore).stream().collect(Collectors.toList());

    // Create a pair per label
    for(String label : model.getNeighborsLabel()) {
      Label toSearch = Label.label(label);

      // Init the string builder and add the columns
      StringBuilder sb = new StringBuilder();
      List<String> columns = List.of(INDEX_SRC, INDEX_TARGET, DIRECTION_COL, RELATIONSHIP_NAME_COL, REL_VAL_COL);
      sb.append(String.join(DELIMITER, columns)).append("\n");

      for(Node n : nodesToExplore) {
        for(Relationship r : n.getRelationships()) {
          try {

            Node otherNode = r.getOtherNode(n);
            if(!otherNode.hasLabel(toSearch)) continue;

            // Get temp variable
            if(!n.hasProperty(tempVariable) || !otherNode.hasProperty(tempVariable) ) continue;
            String idTempSource = ((Long) n.getProperty(tempVariable)).toString();
            String idTempTarget = ((Long) otherNode.getProperty(tempVariable)).toString();

            // Get direction
            String direction = ( otherNode.getId() == r.getStartNodeId()) ? "INCOMING" : "OUTGOING";

            String type = r.getType().name();

            // convert all the properties to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(r.getAllProperties());

            List<String> props = List.of(idTempSource, idTempTarget, direction, type, json);
            sb.append(String.join(DELIMITER, props)).append("\n");

          } catch (Exception e) {
            this.neo4jAL.logError(String.format("Failed to save the relationship with id : %d", r.getId()), e);
          }
        }
      }

      // Save to .csv
      String filename = String.format("%s%s_to_%s%s", RELATIONSHIP_PREFIX, model.getLabel(), toSearch.toString(), EXTENSION);

      File file = targetPath.resolve(filename).toFile();
      try ( FileWriter fw = new FileWriter(file) ) {
        fw.write(sb.toString());
        fw.flush();
      } catch (IOException eo) {
        throw new FileIOException(String.format("Failed to create temporary relationship .csv file at %s",
                targetPath.toString()), eo, "EXPOxEMTC1");
      }

      createdFile.add(file.toPath());
    }

    return createdFile;
  }

  /**
   * Clean all the remaining tags
   * @param models List models
   * @throws Neo4jQueryException
   */
  private void cleanTempProperty(List<ExportModel> models) throws Neo4jQueryException {
    for(ExportModel em : models) {
      String req = String.format("MATCH (o:%1$s) WHERE EXISTS(o.%2$s) REMOVE o.%2$s;", em.getLabel(), tempVariable);
      this.neo4jAL.executeQuery(req);
    }
  }

  /**
   * Sanitize the type of the Neo4j database
   * @param n Node where the value will be extracted
   * @param property Name of the property
   * @return The value as a String
   */
  private String neo4jTypeToString(Node n, String property) {
    if (!n.hasProperty(property)) {
      return "\"\"";
    }

    Object obj = n.getProperty(property);

    if (obj instanceof String[]) {
      String[] temp = (String[]) obj;
      return String.format("\"[%s]\"", String.join(", ", temp));
    }

    return String.format("\"%s\"", obj);
  }
}
