package com.castsoftware.paris.controllers;

import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.models.Case.Case;
import com.castsoftware.paris.models.Group.Group;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ParisCaseController {

	// Get all Diocase

	/**
	 * Return all the dio Cases present in the database
	 * @param neo4jAL Neo4j Access Layer
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<Case> getAllCases(Neo4jAL neo4jAL) throws Neo4jQueryException {
		Iterator<Node> it = neo4jAL.findNodes(Case.getLabelProperty());
		List<Case> retList = new ArrayList<>();
		while (it.hasNext()) {
			retList.add(Case.fromNode(it.next()));
		}
		return retList;
	}

	/**
	 * Get all the categories of DioCase
	 * @param neo4jAL
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<String> getAllCaseCategories(Neo4jAL neo4jAL) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) UNWIND o.%2$s as x RETURN DISTINCT x as cat", Case.getLabelPropertyAsString(), Case.getCategoriesProperty());
		Result result = neo4jAL.executeQuery(req);

		List<String> categories = new ArrayList<>();
		while (result.hasNext()) {
			categories.add((String) result.next().get("cat"));
		}

		return categories;
	}

	/**
	 * Delete the Case node by Id
	 * @param neo4jAL Neo4j Access Layer
	 * @param id Id of the node
	 * @throws Neo4jQueryException
	 */
	public static Boolean deleteById(Neo4jAL neo4jAL, Long id) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) WHERE ID(o)=$id DETACH DELETE o", Case.getLabelPropertyAsString());
		Map<String, Object> params = Map.of("id", id);

		Result res = neo4jAL.executeQuery(req, params);
		if(res.hasNext()) {
			Node n = (Node) res.next().get("node");
			n.delete();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creat a new Case
	 * @param neo4jAL Neo4j Access Layer
	 * @param title Title
	 * @param description Description
	 * @param categories Categories
	 * @param active Active
	 * @param selected Selected
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static Case createCase(Neo4jAL neo4jAL,
								  String title,
								  String description,
								  List<String> categories,
								  Boolean active,
								  Boolean selected) throws Neo4jQueryException {
		Case dc = new Case(title, description, categories, active, selected);
		dc.createNode(neo4jAL);
		return dc;
	}


	/**
	 * Update a Dio Case using its id
	 * @param neo4jAL
	 * @param id
	 * @param title
	 * @param description
	 * @param categories
	 * @param active
	 * @param selected
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static Case updateCase(Neo4jAL neo4jAL,
								  Long id,
								  String title,
								  String description,
								  List<String> categories,
								  Boolean active,
								  Boolean selected) throws Neo4jQueryException {

		String req = String.format("MATCH (o:%1$s) WHERE ID(o)=$id RETURN o as node LIMIT 1", Case.getLabelPropertyAsString());
		Map<String, Object> params = Map.of("id", id);
		Case dc = new Case(title, description, categories, active, selected);

		Result res = neo4jAL.executeQuery(req, params);
		if(res.hasNext()) {
			Node n = (Node) res.next().get("node");
			neo4jAL.detachDelete(n);

			dc.createNode(neo4jAL);
			return dc;
		}

		return null;
	}

	/**
	 * Attach a case to another
	 * @param neo4jAL Neo4j Access Layer
	 * @param idParent Id of the parent Case
	 * @param idChild Id of the Children
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static Relationship attachToCase(Neo4jAL neo4jAL, Long idParent, Long idChild) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) WHERE ID(o)=$idParent " +
						"WITH o " +
						"MATCH (g:%2$s) WHERE ID(g)=$idChild " +
						"MERGE (o)-[r:%3$s]->(g) RETURN r as rel", Case.getLabelPropertyAsString(),
				Case.getLabelPropertyAsString(), Case.getToCaseRelationship()
		);
		Map<String, Object> params = Map.of("idParent", idParent, "idChild", idChild);
		Result res = neo4jAL.executeQuery(req, params);

		if(!res.hasNext()) return null;

		return (Relationship) res.next().get("rel");
	}

	/**
	 * Get all the Case attach to this node
	 * @param neo4jAL Neo4j Access Layer
	 * @param idUseCase Id of the DioCase
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<Case> getAttachedCases(Neo4jAL neo4jAL, Long idUseCase) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s)-[:%2$s]->(g:%3$s) WHERE ID(o)=$idUseCase RETURN g as node",
				Case.getLabelPropertyAsString(),
				Case.getToCaseRelationship() , Case.getLabelPropertyAsString()
		);
		neo4jAL.logInfo("DEBUG: " + req);
		Map<String, Object> params = Map.of("idUseCase", idUseCase);

		List<Case> returnList = new ArrayList<>();
		Result res = neo4jAL.executeQuery(req, params);
		if(res.hasNext()) {
			Node n = (Node) res.next().get("node");
			returnList.add(Case.fromNode(n));
		}

		return returnList;
	}

	/**
	 * Get all the Group attach to this node
	 * @param neo4jAL Neo4j Access Layer
	 * @param idUseCase Id of the DioCase
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<Group> getAttachedGroups(Neo4jAL neo4jAL, Long idUseCase) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s)<-[:%2$s]-(g:%3$s) WHERE ID(o)=$idUseCase RETURN g as node",
				Case.getLabelPropertyAsString(),
				Group.getToDiocaseRelationship() , Group.getLabelPropertyAsString()
		);
		Map<String, Object> params = Map.of("idUseCase", idUseCase);

		List<Group> returnList = new ArrayList<>();
		Result res = neo4jAL.executeQuery(req, params);
		if(res.hasNext()) {
			Node n = (Node) res.next().get("node");
			returnList.add(Group.fromNode(n));
		}

		return returnList;
	}

	/**
	 * Get all the starting DioCase
	 * @param neo4jAL Neo4j Access Layer
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<Case> getRootCase(Neo4jAL neo4jAL) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) WHERE NOT (o)<-[]-(:%1$s) " +
						"RETURN o as node", Case.getLabelPropertyAsString()
		);

		List<Case> returnList = new ArrayList<>();
		Result res = neo4jAL.executeQuery(req);
		while(res.hasNext()) {
			Node n = (Node) res.next().get("node");
			returnList.add(Case.fromNode(n));
		}

		return returnList;
	}

	/**
	 * Detach  a Group  from a Case
	 * @param neo4jAL Neo4j Access Layer
	 * @param idParent Id of the parent Case
	 * @param idChild Id of the Children
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static Relationship detachFromCase(Neo4jAL neo4jAL, Long idParent, Long idChild) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s)-[r:%2$s]->(g:%3$s) WHERE ID(o)=$idParent AND WHERE ID(g)=$idChild " +
				"RETURN r as rel;", Case.getLabelPropertyAsString(),  Case.getToCaseRelationship(), Case.getLabelPropertyAsString());
		Map<String, Object> params = Map.of("idParent", idParent, "idChild", idChild);
		Result res = neo4jAL.executeQuery(req, params);

		if(!res.hasNext()) return null;

		return (Relationship) res.next().get("rel");
	}
}
