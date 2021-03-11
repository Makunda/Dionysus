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
import java.util.stream.Stream;

public class TempProcedure {



	@Context
	public GraphDatabaseService db;

	@Context public Transaction transaction;

	@Context public Log log;

	@Procedure(value = "paris.microservice.shared", mode = Mode.WRITE)
	@Description("paris.microservice.shared(String archiName) - Get all the case present in the database")
	public Stream<OutputMessage> temp(@Name(value = "ArchiName") String archiName) throws ProcedureException {

		try {
			Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);


			// Get all node controllers
			String req= "MATCH (obj:Object:TriPOD) WHERE obj.Name CONTAINS 'Controller' AND obj.Level='C# Presentation' RETURN obj as controller";
			Result res = neo4jAL.executeQuery(req);

			List<Node> controllers = new ArrayList<>();
			while (res.hasNext()) {
				controllers.add((Node) res.next().get("controller"));
			}

			neo4jAL.logInfo(String.format("Detected %d controllers", controllers.size()));

			long processed = 0L;
			String property  = "Service";
			for (Node con : controllers) {
				String microserviceName = ((String) con.getProperty("Name")).replace("Controller", "");
				neo4jAL.logInfo(String.format("Treating node %s", con.getProperty("Name")));
				List<Long> visitedID = new ArrayList<>();
				Stack<Node> toVisit = new Stack<>();

				// Init to visit
				for(Relationship rel : con.getRelationships()) {
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
			String reqArchi = String.format("MATCH (obj:Object:TriPOD) WHERE EXISTS(obj.%1$s) " +
								"SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN [('$a_%2$s$'+obj.%1$s)] ELSE obj.Tags + ('$a_%2$s$'+obj.%1$s) END;", property, archiName);
			neo4jAL.executeQuery(reqArchi);

			return Stream.of(new OutputMessage("processed :"+processed));
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}



	@Procedure(value = "paris.microservice.isolated", mode = Mode.WRITE)
	@Description("paris.microservice.isolated(String prefix) - Get all the case present in the database")
	public Stream<OutputMessage> tempIsolated(@Name(value = "ArchiName") String archiName) throws ProcedureException {

		try {
			Neo4jAL neo4jAL = new Neo4jAL(db, transaction, log);


			// Get all node controllers
			String req= "MATCH (obj:Object:TriPOD) WHERE obj.Name CONTAINS 'Controller' AND obj.Level='C# Presentation' RETURN obj as controller";
			Result res = neo4jAL.executeQuery(req);

			List<Node> controllers = new ArrayList<>();
			while (res.hasNext()) {
				controllers.add((Node) res.next().get("controller"));
			}

			neo4jAL.logInfo(String.format("Detected %d controllers", controllers.size()));

			long processed = 0L;
			for (Node con : controllers) {
				neo4jAL.logInfo(String.format("Processing %s controller.", ((String) con.getProperty("Name"))));
				String microserviceName = ((String) con.getProperty("Name")).replace("Controller", "");

				List<Long> visitedID = new ArrayList<>();
				Stack<Node> toVisit = new Stack<>();

				// Init to visit
				for(Relationship rel : con.getRelationships(Direction.OUTGOING)) {
					Node out = rel.getEndNode();
					toVisit.add(out);
				}
				visitedID.add(con.getId());

				// Apply prop on controller
				String microserviceFullName = archiName + microserviceName + " Microservice$" + ((String) con.getProperty("Level"));
				String reqArchi = String.format("MATCH (obj:Object:TriPOD) WHERE ID(obj)=$Id " +
						"SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['$a_%1$s'] ELSE obj.Tags + '$a_%1$s' END;", microserviceFullName);
				neo4jAL.logInfo("TAG applied : " + reqArchi);
				Map<String, Object> params = Map.of("Id", con.getId());
				neo4jAL.executeQuery(reqArchi, params);

				// Parse relationship outgoing
				while (!toVisit.empty()) {

					Node treat = toVisit.pop();
					// Ignore if visited
					if (visitedID.contains(treat.getId())) continue;
					visitedID.add(treat.getId());

					// flag for microservice

					// Flag with architecture.
					if(treat.hasLabel(Label.label("Object")) && treat.hasProperty("Level")) {
						microserviceFullName = archiName + microserviceName + " Microservice$" + ((String) treat.getProperty("Level"));
						reqArchi = String.format("MATCH (obj:Object:TriPOD) WHERE ID(obj)=$Id " +
								"SET obj.Tags = CASE WHEN obj.Tags IS NULL THEN ['$a_%1$s'] ELSE obj.Tags + '$a_%1$s' END;", microserviceFullName);
						params = Map.of("Id", treat.getId());
						neo4jAL.executeQuery(reqArchi, params);

						// Get all linked objects

					}

					// Skip if the node isn't an object or a subobject
					if(treat.hasLabel(Label.label("Object")) || treat.hasLabel(Label.label("SubObject"))) continue;


					// Add relationships
					for (Relationship rel : treat.getRelationships(Direction.OUTGOING)) {
						Node out = rel.getEndNode();
						toVisit.add(out);
					}
					processed++;
				}

			}



			return Stream.of(new OutputMessage("processed :"+processed));
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}


}
