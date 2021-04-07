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
import com.castsoftware.paris.exceptions.neo4j.Neo4jNoResult;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.results.OutputMessage;
import com.castsoftware.paris.utils.Algorithms;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Exporter {

  // Return message queue
  private static final List<OutputMessage> MESSAGE_QUEUE = new ArrayList<>();

  // Default properties
  private static final String DELIMITER = Configuration.get("io.csv.delimiter");
  private static final String TEMP_VARIABLE_PREFIX = Configuration.get("io.temp_variable_prefix");
  private static final String EXTENSION = Configuration.get("io.csv.csv_extension");
  private static final String INDEX_COL = Configuration.get("io.index_col");
  private static final String INDEX_OUTGOING = Configuration.get("io.index_outgoing");
  private static final String INDEX_INCOMING = Configuration.get("io.index_incoming");
  private static final String RELATIONSHIP_PREFIX =
      Configuration.get("io.file.prefix.relationship");
  private static final String NODE_PREFIX = Configuration.get("io.file.prefix.node");

  private Neo4jAL neo4jAL;
  private String tempVariable;

  public Exporter(Neo4jAL neo4jAL) {
    this.neo4jAL = neo4jAL;

    // Generate temp variables used to assign links
    this.tempVariable = TEMP_VARIABLE_PREFIX + Algorithms.getAlphaNumericString(5);
  }



  /**
   * Appends all the files created during this process to the target zip. Every file appended will
   * be remove once added to the zip.
   *
   * @param zipFileName Name of the ZipFile
   * @throws IOException
   */
  private void createZip(Path folderPath, String zipFileName, List<Path> files) throws FileIOException {
    File f = folderPath.resolve(zipFileName).toFile();

    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(f))) {

      for (Path path : files) {
        File fileToZip = folderPath.resolve(path).toFile();

        try (FileInputStream fileStream = new FileInputStream(fileToZip)) {
          ZipEntry e = new ZipEntry(path.toString());
          zipOut.putNextEntry(e);

          byte[] bytes = new byte[1024];
          int length;
          while ((length = fileStream.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
        } catch (Exception e) {
          log.error("An error occurred trying to zip file with name : ".concat(path.toString()), e);
        }

        if (!fileToZip.delete())
          log.error("Error trying to delete file with name : ".concat(path.toString()));
      }

    } catch (IOException e) {
      log.error("An error occurred trying create zip file with name : ".concat(zipFileName), e);
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
  public Path exportModelToCsv(ExportModel model, Path targetPath) throws Neo4jQueryException {
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

    int uniqueId = 0;
    List<String> tempProperties;
    for(Node n : allNodes) {
      tempProperties = new ArrayList<>();

      // Add Id to value
      tempProperties.add(String.valueOf(uniqueId));
      n.setProperty(INDEX_COL, uniqueId);
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

    // TODO Write to file

    // Assign a temporary value for the relationships
  }

  /**
   * Clean all the remaining tags
   * @param models List models
   * @throws Neo4jQueryException
   */
  public void cleanTempProperty(List<ExportModel> models) throws Neo4jQueryException {
    for(ExportModel em : models) {
      String req = String.format("MATCH (o:%1$s) WHERE EXISTS(%2$s) REMOVE o.%2$s;", em.getLabel(), tempVariable);
      this.neo4jAL.executeQuery(req);
    }
  }

  /**
   * Sanitize the type of the Neo4j database
   * @param n Node where the value will be extracted
   * @param property Name of the property
   * @return The value as a String
   */
  public String neo4jTypeToString(Node n, String property) {
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
