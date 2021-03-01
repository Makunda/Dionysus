package com.castsoftware.dionysus.models.DioGroup;

import com.castsoftware.dionysus.database.Neo4jAL;
import com.castsoftware.dionysus.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DioGroupUtils {

	/**
	 * Return all the Dio Group Nodes
	 * @param neo4jAL Neo4j access Layer
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<DioGroup> getAllDioGroupNodes(Neo4jAL neo4jAL) throws Neo4jQueryException {
		Iterator<Node> it = neo4jAL.findNodes(DioGroup.getLabelProperty());
		List<DioGroup> retList = new ArrayList<>();
		while (it.hasNext()) {
			retList.add(DioGroup.fromNode(it.next()));
		}
		return retList;
	}

	/**
	 * Return all the Dio Group specific to one specific category
	 * @param neo4jAL
	 * @param category
	 * @return
	 * @throws Neo4jQueryException
	 */
	public static List<DioGroup> getDioGroupByCategory(Neo4jAL neo4jAL, String category) throws Neo4jQueryException {
		String req = String.format("MATCH (o:%1$s) WHERE o.%2$s CONTAINS $category RETURN o as node");
		Map<String, Object> params = Map.of("category", category);

		Result res = neo4jAL.executeQuery(req, params);
		List<DioGroup> retList = new ArrayList<>();

		while(res.hasNext()) {
			retList.add(DioGroup.fromNode((Node) res.next().get("node")));
		}

		return retList;
	}

	public static List<DioGroup> updateDioGroupByID(Neo4jAL neo4jAL, Long id,
													Boolean active,
													List<String> categories,
													Long creationDate,
													String cypherRequest,
													String cypherRequestReturn,
													String description,
													String groupName,
													String name,
													Boolean selected,
													List<DioGroupType> types) throws Neo4jQueryException {


		String req = String.format("MATCH (o:%1$s) WHERE ID(o)=$id RETURN o as node LIMIT 1");
		Map<String, Object> params = Map.of("id", id);

		Result res = neo4jAL.executeQuery(req, params);
		List<DioGroup> retList = new ArrayList<>();

		while(res.hasNext()) {
			retList.add(DioGroup.fromNode((Node) res.next().get("node")));
		}

		return retList;
	}
}
