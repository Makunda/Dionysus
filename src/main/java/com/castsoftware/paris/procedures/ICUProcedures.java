package com.castsoftware.paris.procedures;

import com.castsoftware.paris.controllers.ParisGroupController;
import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.ProcedureException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.models.Group.Group;
import com.castsoftware.paris.results.CustomGroupResult;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ICUProcedures {

	@Context
	public GraphDatabaseService db;

	@Context public Transaction transaction;

	@Context public Log log;

	@Procedure(value = "paris.temp.icu", mode = Mode.WRITE)
	@Description("paris.temp.icu() - Get all the groups present in the database")
	public void getAllDioGroups() throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);

		  	String getPagesMethods =
			  "MATCH (page:Object:ICU_Census)-[r]->(s:SubObject:ICU_Census)  "
				  + "WHERE page.Type='Active Server PageX' AND s.Type='C# Method' "
				  + "WITH page, s "
				  + "MATCH (s)<-[:Contains]-(t:Transaction)-[:EndsWith]->(endPoint:TransactionNode) "
				  + "WITH page, s, t as tran, endPoint "
				  + "MATCH (tran:Transaction)-[:Contains]->(o:Object) "
				  + "WITH page, s, tran, endPoint, COLLECT(DISTINCT o) as objectContained "
				  + "OPTIONAL MATCH (endPoint)<-[:OUT]-(oEnd:Object) "
				  + "OPTIONAL MATCH (endPoint)<-[:OUT]-(sub:SubObject)-[:BELONGTO]->(oEnd:Object) "
				  + "RETURN page as page, s as method, objectContained as fullList,  COLLECT(DISTINCT oEnd) as endPoints  ";

		  	Result results = nal.executeQuery(getPagesMethods);

			System.out.println("Page Name; Method Name; Number of End points; End Points;");

		  	while (results.hasNext()) {
		  		Map<String, Object> res = results.next();

				Node page = (Node) res.get("page");
				String pageName = (String) page.getProperty("Name");

				Node methods = (Node) res.get("method");
				String methodName = (String) methods.getProperty("Name");

				List<Node> fullList = (List<Node>) res.get("fullList");
				List<Long> idFullList = fullList.stream().map(Node::getId).collect(Collectors.toList());

				List<Node> endPoints = (List<Node>) res.get("endPoints");
				List<Long> endPointsID = endPoints.stream().map(Node::getId).collect(Collectors.toList());

				// Start with the methods and parse the objects in the fullList of objects

				Node voyager = methods;
				Node tempNode = null;

				Stack<Node> toVisit  = new Stack<>();

				for(Relationship rel : voyager.getRelationships(Direction.OUTGOING)) {
					tempNode = rel.getEndNode();
					if(idFullList.contains(tempNode.getId())) {
						toVisit.add(tempNode);
					}
				}

				List<Node> endPointsRestricted = new ArrayList<>();
				List<Long> visitedId  = new ArrayList<>();
				visitedId.add(voyager.getId());

				while (!toVisit.isEmpty()) {
					voyager = toVisit.pop();

					// Skip if already visited
					if(visitedId.contains(voyager.getId())) continue;

					for(Relationship rel : voyager.getRelationships(Direction.OUTGOING)) {
						tempNode = rel.getEndNode();

						// If in the list of the transaction
						if(idFullList.contains(tempNode.getId())) {
							toVisit.add(tempNode);
						}

						if(endPointsID.contains(tempNode.getId())) {
							endPointsRestricted.add(tempNode);
						}
					}

					visitedId.add(voyager.getId());
				}

				List<Node> uniqueEndpointsNode = new ArrayList<>();
				List<Long> uniqueEndpoints = new ArrayList<>();

				for(Node n : endPointsRestricted) {
					if(uniqueEndpoints.contains(n.getId())) continue;

					uniqueEndpointsNode.add(n);
					uniqueEndpoints.add(n.getId());
				}

				String endpointsName = uniqueEndpointsNode.stream().map(x -> "Name : " +
						(String) x.getProperty("Name") +
						" - Type : " +
						(String) x.getProperty("Type")

				).collect(Collectors.joining(", "));



        		System.out.printf("%s; %s; %d; \"[ %s ]\"%n", pageName, methodName, uniqueEndpointsNode.size(), endpointsName);
			}




		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

}
