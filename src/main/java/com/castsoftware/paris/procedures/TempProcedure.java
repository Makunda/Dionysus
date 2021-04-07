package com.castsoftware.paris.procedures;

import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.ProcedureException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.results.OutputMessage;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TempProcedure {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "paris.microservice.shared", mode = Mode.WRITE)
  @Description(
      "paris.microservice.shared(String archiName) - Get all the case present in the database")
  public Stream<OutputMessage> temp(@Name(value = "ArchiName") String archiName)
      throws ProcedureException {

    try {
      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      // Get all node controllers
      String req =
          "MATCH (obj:Object:TriPOD) WHERE obj.Name CONTAINS 'Controller' AND obj.Level='C# Presentation' RETURN obj as controller";
      Result res = neo4jAL.executeQuery(req);

      List<Node> controllers = new ArrayList<>();
      while (res.hasNext()) {
        controllers.add((Node) res.next().get("controller"));
      }

      neo4jAL.logInfo(String.format("Detected %d controllers", controllers.size()));

      long processed = 0L;
      String property = "Service";
      for (Node con : controllers) {
        String microserviceName = ((String) con.getProperty("Name")).replace("Controller", "");
        neo4jAL.logInfo(String.format("Treating node %s", con.getProperty("Name")));
        List<Long> visitedID = new ArrayList<>();
        Stack<Node> toVisit = new Stack<>();

        // Init to visit
        for (Relationship rel : con.getRelationships()) {
          Node out = rel.getEndNode();
          toVisit.add(out);
        }
        con.setProperty(property, microserviceName);
        visitedID.add(con.getId());

        // Parse relationship outgoing
        while (!toVisit.empty()) {

          Node treat = toVisit.pop();
          // Ignore if visited
          if (visitedID.contains(treat.getId())) continue;
          visitedID.add(treat.getId());

          if (treat.hasProperty(property)) {
            if (!((String) treat.getProperty(property)).equals(microserviceName)) {
              treat.setProperty(property, "shared");
            }
          } else {
            treat.setProperty(property, microserviceName);
          }

          // Add relationships
          for (Relationship rel : treat.getRelationships(Direction.OUTGOING)) {
            Node out = rel.getEndNode();
            toVisit.add(out);
          }
          processed++;
        }
      }

      // Flag with architecture.
      String reqArchi =
          String.format(
              "MATCH (obj:Object:TriPOD) WHERE EXISTS(obj.%1$s) "
                  + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN [('$a_%2$s$'+obj.%1$s)] ELSE obj.Tags + ('$a_%2$s$'+obj.%1$s) END;",
              property, archiName);
      neo4jAL.executeQuery(reqArchi);

      return Stream.of(new OutputMessage("processed :" + processed));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "paris.microservice.archiOfArchi", mode = Mode.WRITE)
  @Description("paris.microservice.archiOfArchi - Get all the case present in the database")
  public Stream<OutputMessage> archiOfArchProc(@Name(value = "ArchiName") String archiName)
      throws ProcedureException {

    try {
      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);
      archiOfArchi(neo4jAL, archiName);

      return Stream.of(new OutputMessage("processed :" + archiName));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  public void archiOfArchi(Neo4jAL neo4jAL, String name) throws Neo4jQueryException {
    String archiName = "GLOBAL " + name;

    // Get Archi models linked
    String getServices =
        "MATCH (n:ArchiModel:TriPOD) WHERE n.Name CONTAINS $prefix AND NOT n.Name=$archiName RETURN DISTINCT n as service";
    Map<String, Object> paramCount = Map.of("prefix", name, "archiName", archiName);

    List<Node> services = new ArrayList<>();
    Result resServices = neo4jAL.executeQuery(getServices, paramCount);

    Long count = 0L;
    while (resServices.hasNext()) {
      services.add((Node) resServices.next().get("service"));
    }

    // Create new Architecture
    String req =
        "MERGE (n:ArchiModel:`TriPOD` { Type:'archimodel', Color:'rgb(34,199,214)',Name:$groupName} ) "
            + "SET n.Count=CASE WHEN EXISTS(n.Count) THEN n.Count + $count ELSE $count END SET n.ModelId=0 "
            + "RETURN n as node;";
    Map<String, Object> params =
        Map.of("groupName", archiName + " global", "count", new Long(count));
    Result result = neo4jAL.executeQuery(req, params);

    if (!result.hasNext()) {
      neo4jAL.logError("Failed to create the Archi of Archi");
    }
    Node archiNode = (Node) result.next().get("node");

    String subsetIdReq =
        String.format("MATCH (n:Subset:`%s`) RETURN n.SubsetId as subsetId", "TriPOD");
    Result resSubsetId = neo4jAL.executeQuery(subsetIdReq);
    String tempSub;
    Long maxIdSub = 1L;
    while (resSubsetId.hasNext()) {
      try {
        tempSub = (String) resSubsetId.next().get("subsetId");
        if (Long.parseLong(tempSub) >= maxIdSub) {
          maxIdSub = Long.parseLong(tempSub) + 1;
        }
      } catch (ClassCastException | NumberFormatException ignored) {
      }
    }

    ;
    // Create the subsets regrouping all the nodes under the other Archi
    for (Node service : services) {
      neo4jAL.logInfo(String.format("INSERTING %s", service.getProperty("Name")));
      List<Node> nodeUnderService = getObjectsUnderArchiModel(neo4jAL, service.getId(), "TriPOD");

      // Subset parameters
      Map<String, Object> paramsSubset =
          Map.of(
              "groupName",
              service.getProperty("Name"),
              "idArchi",
              archiNode.getId(),
              "maxModelID",
              maxIdSub.toString(),
              "subsetID",
              "0",
              "count",
              nodeUnderService.size());

      String reqSubset =
          String.format(
              "CREATE (n:Subset:`%s` { Type:'subset', Color:'rgb(34,199,214)',Name:$groupName } ) "
                  + "SET n.ModelId= $maxModelID "
                  + "SET n.Count=CASE WHEN EXISTS(n.Count) THEN n.Count + $count ELSE $count END SET n.SubsetId=$subsetID "
                  + "WITH n as node "
                  + "MATCH (archi:ArchiModel:`%1$s`) WHERE ID(archi)=$idArchi MERGE (archi)-[:Contains]->(node) "
                  + "RETURN node as node",
              "TriPOD");

      Result resSubset = neo4jAL.executeQuery(reqSubset, paramsSubset);

      Node subsetNode = (Node) resSubset.next().get("node");
      addObjectToSubset(
          neo4jAL,
          subsetNode.getId(),
          (String) subsetNode.getProperty("Name"),
          nodeUnderService,
          "TriPOD");
    }
  }

  /**
   * Get the objects under the architecture model
   *
   * @param neo4jAL
   * @param idArchi
   * @param applicationContext
   * @return
   * @throws Neo4jQueryException
   */
  public List<Node> getObjectsUnderArchiModel(
      Neo4jAL neo4jAL, Long idArchi, String applicationContext) throws Neo4jQueryException {
    // Match
    String req =
        String.format(
            "MATCH (n:ArchiModel:`%1$s`)-->(:Subset)-->(o:Object) "
                + "WHERE ID(n)=$id "
                + "RETURN DISTINCT o as node;",
            applicationContext);
    Map<String, Object> params = Map.of("id", idArchi);

    List<Node> nodes = new ArrayList<>();
    Result res = neo4jAL.executeQuery(req, params);
    while (res.hasNext()) {
      nodes.add((Node) res.next().get("node"));
    }

    return nodes;
  }

  public void addObjectToSubset(
      Neo4jAL neo4jAL,
      Long idSubset,
      String nameSubset,
      List<Node> nodeList,
      String applicationContext)
      throws Neo4jQueryException {
    // Add the objects and the SubObjects to the subset
    Map<String, Object> paramsNode;
    for (Node rObject : nodeList) {
      // Link objects
      paramsNode = Map.of("idObj", rObject.getId(), "idSubset", idSubset, "subsetName", nameSubset);

      String reObj =
          String.format(
              "MATCH (o:Object:`%s`) WHERE ID(o)=$idObj "
                  + "SET o.Subset = CASE WHEN o.Subset IS NULL THEN [$subsetName] ELSE o.Subset + $subsetName END "
                  + "WITH o as obj "
                  + "MATCH (newS:Subset) WHERE ID(newS)=$idSubset "
                  + "MERGE (newS)-[:Contains]->(obj) ",
              applicationContext);

      String subObj =
          String.format(
              "MATCH (o:Object:`%s`)<-[:BELONGTO]-(j:SubObject) WHERE ID(o)=$idObj "
                  + "SET j.Subset = CASE WHEN j.Subset IS NULL THEN [$subsetName] ELSE o.Subset + $subsetName END "
                  + "WITH j "
                  + "MATCH (newS:Subset) WHERE ID(newS)=$idSubset MERGE (newS)-[:Contains]->(j)  ",
              applicationContext);

      neo4jAL.executeQuery(reObj, paramsNode);
      neo4jAL.executeQuery(subObj, paramsNode);
    }
  }

  private static void mergeRelationships(Neo4jAL neo4jAL, Long idSource, Long idTarget) throws Neo4jQueryException {
  	String req = "MATCH (o:TriPOD) WHERE ID(o)=$idSource " +
			"WITH o " +
			"MATCH (t:TriPOD) WHERE ID(t)=$idTarget " +
			"MERGE (o)-[:Contains]->(t);";
	  Map<String, Object> params = Map.of("idSource", idSource, "idTarget", idTarget);
	  neo4jAL.executeQuery(req, params);
  }

  @Procedure(value = "paris.datacallgraphes", mode = Mode.WRITE)
  @Description("paris.datacallgraphes - Get all the case present in the database")
  public Stream<OutputMessage> datacallgraphes()
      throws ProcedureException {

    try {
      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);
      String getDataCall = "MATCH (d:DataGraph:TriPOD) RETURN d as dataGraph";
      Result resDataCall = neo4jAL.executeQuery(getDataCall);

      List<Node> dataCall = new ArrayList<>();
      while (resDataCall.hasNext()) {
        dataCall.add((Node) resDataCall.next().get("dataGraph"));
      }

      neo4jAL.logInfo(String.format("%d data call were identified.", dataCall.size()));
      int countGraph = 0;

      for (Node n : dataCall) {
        countGraph++;

        String getTransaction =
            	"MATCH (d:DataGraph:TriPOD)-->(t:Object:TriPOD) WHERE t.Type CONTAINS \"SQL\" AND ID(d)=$idData "
                + "WITH d, t "
                + "MATCH (o:Object:TriPOD)-[]->(t) WHERE o.FullName CONTAINS \".Models.\" AND o.Type CONTAINS \"C# Class\" "
                + "WITH DISTINCT d, o "
                + "MATCH (o)-[]-(:Transaction)-->(other:Object) "
                + "MERGE (d)-[:Contains]->(o) "
                + "RETURN o as target, COLLECT( DISTINCT other) as otherNodes ";

        Map<String, Object> params = Map.of("idData", n.getId());
        Result resOtherNodes = neo4jAL.executeQuery(getTransaction, params);
        if(!resOtherNodes.hasNext()) continue;

        Map<String, Object> mapRes = resOtherNodes.next();
		List<Node> otherNodes = (List<Node>) mapRes.get("otherNodes");
		List<Long> otherNodesID = otherNodes.stream().map(Node::getId).collect(Collectors.toList());;
		Node targetNode = (Node) mapRes.get("target");

		neo4jAL.logInfo("Computing now : " + n.getProperty("Name"));
		neo4jAL.logInfo("Number of node to be investigated : " + otherNodes.size());
		int counter = 0;
        int added = 0;


        // Start from bottom
        Stack<Node> toVisit = new Stack<>();
        List<Long> visitedId = new ArrayList<>();
        toVisit.add(targetNode);

        while (!toVisit.empty()) {

          counter ++;

          if(counter %100 == 0) {
            neo4jAL.logInfo("Investigation still pending : "+counter);
          }

          Node itNode = toVisit.pop();
          // Ignore if already visited
          if(visitedId.contains(itNode.getId())) continue;

          visitedId.add(itNode.getId());

          Iterator<Relationship> itRel = itNode.getRelationships(Direction.INCOMING).iterator();
          while (itRel.hasNext()) {
            Relationship rel = itRel.next();

            if(otherNodesID.contains(rel.getStartNodeId())) {
              // the node is in the list and directly linked
              mergeRelationships(neo4jAL, n.getId(), rel.getStartNodeId());

              // Add to visit
              toVisit.add(rel.getStartNode());
              added ++;
            }
          }

        }

        neo4jAL.logInfo("Merge for : " + n.getProperty("Name") + " : Objects : " + added);
        neo4jAL.logInfo("STEP FINISHED: " + countGraph + " on " + dataCall.size());

      }

      return Stream.of(new OutputMessage("processed :" + dataCall.size()));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "paris.microservice.isolated", mode = Mode.WRITE)
  @Description(
      "paris.microservice.isolated(String prefix) - Get all the case present in the database")
  public Stream<OutputMessage> tempIsolated(@Name(value = "ArchiName") String archiName)
      throws ProcedureException {

    try {
      Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);

      // Get all node controllers
      String req =
          "MATCH (obj:Object:TriPOD) WHERE obj.Name CONTAINS 'Controller' AND obj.Level='C# Presentation' RETURN obj as controller ORDER BY obj.Name";
      Result res = neo4jAL.executeQuery(req);

      List<Node> controllers = new ArrayList<>();
      while (res.hasNext()) {
        controllers.add((Node) res.next().get("controller"));
      }

      neo4jAL.logInfo(String.format("Detected %d controllers", controllers.size()));

      List<String> createdArchiModels = new ArrayList<>();
      long processed = 0L;
      long uniqueId = 0L;
      for (Node con : controllers) {
        // forge name
        String uniqueArchi = String.format("%s-%d", archiName, uniqueId);
        String microserviceName = ((String) con.getProperty("Name")).replace("Controller", "");
        String microserviceFullName = uniqueArchi + " " + microserviceName + " Microservice$";

        processed += flagNode(neo4jAL, microserviceFullName, con, Direction.OUTGOING).size();
        processed += flagNode(neo4jAL, microserviceFullName, con, Direction.INCOMING).size();
        uniqueId++;

        createdArchiModels.add(microserviceFullName);
      }

      return Stream.of(new OutputMessage("processed :" + processed));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  public List<Node> flagNode(
      Neo4jAL neo4jAL, String microserviceFullName, Node startingNode, Direction direction)
      throws Neo4jQueryException {
    long processedOne = 0L;
    neo4jAL.logInfo(
        String.format("Processing %s controller.", ((String) startingNode.getProperty("Name"))));

    List<Node> visited = new ArrayList<>();
    List<Long> visitedID = new ArrayList<>();
    Stack<Node> toVisit = new Stack<>();

    // Get neighbors of controllers
    for (Relationship r : startingNode.getRelationships()) {
      toVisit.add(r.getOtherNode(startingNode));
    }
    visitedID.add(startingNode.getId());

    // Init to visit To bottom
    // Apply prop on controller
    String tag = "$a_" + microserviceFullName + "Controller";
    String reqArchi =
        "MATCH (obj:Object:TriPOD) WHERE ID(obj)=$Id SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN [$tag] ELSE obj.Tags + $tag END;";
    Map<String, Object> params = Map.of("Id", startingNode.getId(), "tag", tag);
    neo4jAL.executeQuery(reqArchi, params);

    // Parse relationship outgoing
    while (!toVisit.empty()) {

      Node treat = toVisit.pop();
      // Ignore if visited
      if (visitedID.contains(treat.getId())) continue;

      // Add to visited nodes
      visitedID.add(treat.getId());
      visited.add(treat);
      // flag for microservice

      // Flag with architecture
      if (treat.hasLabel(Label.label("Object")) && treat.hasProperty("Level")) {
        tag = "$a_" + microserviceFullName + treat.getProperty("Level");
        reqArchi =
            String.format(
                "MATCH (obj:Object:TriPOD) WHERE ID(obj)=$Id "
                    + "SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN [$tag] ELSE obj.Tags + $tag END;");
        params = Map.of("Id", treat.getId(), "tag", tag);
        neo4jAL.executeQuery(reqArchi, params);

        // Get all linked objects
      }

      // Skip if the node isn't an object or a subObject

      // Add relationships
      for (Relationship rel : treat.getRelationships(direction)) {
        Node out = rel.getOtherNode(treat);
        toVisit.add(out);
      }
      processedOne++;
    }
    neo4jAL.logInfo(
        String.format(
            "IN %s controller : %d", ((String) startingNode.getProperty("Name")), processedOne));

    return visited;
  }
}
