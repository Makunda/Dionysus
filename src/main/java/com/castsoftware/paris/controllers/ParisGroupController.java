package com.castsoftware.paris.controllers;

import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.neo4j.Neo4JTemplateLanguageException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.models.Case.Case;
import com.castsoftware.paris.models.Group.Group;
import com.castsoftware.paris.models.Group.GroupType;
import com.castsoftware.paris.models.Group.GroupResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParisGroupController {

	/**
	 * Return all the Group Nodes
	 * @param neo4jAL Neo4j access Layer
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<Group> getAllGroupNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
		Iterator<Node> it = neo4jAL.findNodes(Group.getLabelProperty());
		List<Group> retList = new ArrayList<>();
		while (it.hasNext()) {
			retList.add(Group.fromNode(it.next()));
		}
		return retList;
	}

	/**
	 * Return all the Group related to one specific category
	 * @param neo4jAL
	 * @param category
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<Group> getGroupsByCategory(Neo4jAL neo4jAL, String category) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) WHERE o.%2$s CONTAINS $category RETURN o as node", Group.getLabelPropertyAsString(), Group.getCategoriesProperty());
		Map<String, Object> params = Map.of("category", category);

		Result res = neo4jAL.executeQuery(req, params);
		List<Group> retList = new ArrayList<>();

		while(res.hasNext()) {
			retList.add(Group.fromNode((Node) res.next().get("node")));
		}

		return retList;
	}

	public static List<String> getAllGroupCategories(Neo4jAL neo4jAL) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) UNWIND o.%2$s as x RETURN DISTINCT x as cat", Group.getLabelPropertyAsString(), Group.getCategoriesProperty());
		Result result = neo4jAL.executeQuery(req);

		List<String> categories = new ArrayList<>();
		while (result.hasNext()) {
			categories.add((String) result.next().get("cat"));
		}

		return categories;
	}

	/**
	 * Delete the Group node by Id
	 * @param neo4jAL Neo4j Access Layer
	 * @param id Id of the node
	 * @throws Neo4jQueryException
	 */
	public static Boolean deleteById(Neo4jAL neo4jAL, Long id) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) WHERE ID(o)=$id DETACH DELETE o", Group.getLabelPropertyAsString());
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
	 * Add a  group to the configuration
	 * @param neo4jAL Neo4j Access Layer
	 * @param active Active
	 * @param categories Categories of the group
	 * @param creationDate Date of the creation
	 * @param cypherRequest Cypher request
	 * @param cypherRequestReturn
	 * @param description
	 * @param groupName
	 * @param name
	 * @param selected
	 * @param typesAsList
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static Group createGroup(Neo4jAL neo4jAL,
									Boolean active,
									List<String> categories,
									Long creationDate,
									String cypherRequest,
									String cypherRequestReturn,
									String description,
									String groupName,
									String name,
									Boolean selected,
									List<String> typesAsList) throws Neo4jQueryException {
		List<GroupType> types = typesAsList.stream().map(GroupType::getFromString).collect(Collectors.toList());
		Group dn = new Group(active, categories, creationDate, cypherRequest, cypherRequestReturn, description, groupName, name, selected, types);
		dn.createNode(neo4jAL);
		return dn;
	}

	/**
	 * Update a group by its id
	 * @param neo4jAL Neo4j Access Layer
	 * @param id Id of the Dionysus node
	 * @param active
	 * @param categories
	 * @param creationDate
	 * @param cypherRequest
	 * @param cypherRequestReturn
	 * @param description
	 * @param groupName
	 * @param name
	 * @param selected
	 * @param typesAsList
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static Group updateGroupByID(Neo4jAL neo4jAL, Long id,
										Boolean active,
										List<String> categories,
										Long creationDate,
										String cypherRequest,
										String cypherRequestReturn,
										String description,
										String groupName,
										String name,
										Boolean selected,
										List<String> typesAsList) throws Neo4jQueryException {

		List<GroupType> types = typesAsList.stream().map(GroupType::getFromString).collect(Collectors.toList());

		String req = String.format("MATCH (o:%1$s) WHERE ID(o)=$id RETURN o as node LIMIT 1", Group.getLabelPropertyAsString());
		Map<String, Object> params = Map.of("id", id);

		Group dn = new Group(active, categories, creationDate, cypherRequest, cypherRequestReturn, description, groupName, name, selected, types);
		Result res = neo4jAL.executeQuery(req, params);
		if(res.hasNext()) {
			Node n = (Node) res.next().get("node");
			n.delete();

			dn.createNode(neo4jAL);
			return dn;
		}

		return null;
	}



	/**
	 * Attach a specified DioGroup to the corresponding DioCase	( Merge links )
	 * @param neo4jAL Neo4j Access Layer
	 * @param idDioGroup Id of the Dio Group
	 * @param idDioCase Id of the Dio Case
	 * @return The relationship attached to the DioCase, null if the operation failed to create the relationship
	 * @throws Neo4jQueryException
	 */
	public static Relationship attachToCase(Neo4jAL neo4jAL, Long idDioGroup, Long idDioCase) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) WHERE ID(o)=$idDioCase " +
						"WITH o " +
						"MATCH (g:%2$s) WHERE ID(g)=$idDioGroup " +
						"MERGE (o)<-[r:%3$s]-(g) RETURN r as rel", Case.getLabelPropertyAsString(),
				Group.getLabelPropertyAsString(), Group.getToDiocaseRelationship()
				);
		Map<String, Object> params = Map.of("idDioCase", idDioCase, "idDioGroup", idDioGroup);
		Result res = neo4jAL.executeQuery(req, params);

		if(!res.hasNext()) return null;

		return (Relationship) res.next().get("rel");
	}

	/**
	 * Detach  a Dio Group  from a Case
	 * @param neo4jAL Neo4j Access Layer
	 * @param idDioGroup Id of the Dio Group
	 * @param idDioCase Id Of tje
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static Relationship detachFromCase(Neo4jAL neo4jAL, Long idDioGroup, Long idDioCase) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s)<-[r:%2$s]-(g:%3$s) WHERE ID(o)=$idDioCase AND WHERE ID(g)=$idDioGroup " +
						"RETURN r as rel;", Case.getLabelPropertyAsString(),  Group.getToDiocaseRelationship(), Group.getLabelPropertyAsString());
		Map<String, Object> params = Map.of("idDioCase", idDioCase, "idDioGroup", idDioGroup);
		Result res = neo4jAL.executeQuery(req, params);

		if(!res.hasNext()) return null;

		return (Relationship) res.next().get("rel");
	}

	/**
	 * Forecast the Dio results
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<GroupResult> forecastAllGroups(Neo4jAL neo4jAL, String application) throws Neo4jQueryException {
		List<Group> groups = getAllGroupNodes(neo4jAL);
		List<GroupResult> results = new ArrayList<>();

		try {
			for (Group dg : groups) {
				results.add(dg.forecast(neo4jAL, application));
			}
		} catch (Neo4JTemplateLanguageException ignored) {
		}

		return results;
	}




}
