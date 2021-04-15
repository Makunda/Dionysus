package com.castsoftware.paris.procedures;

import com.castsoftware.paris.controllers.ParisCypherRequestsController;
import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.ProcedureException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.paris.results.BooleanResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class CypherProcedures {

	@Context
	public GraphDatabaseService db;

	@Context public Transaction transaction;

	@Context public Log log;

	@Procedure(value = "paris.cypher.test.request", mode = Mode.WRITE)
	@Description("paris.cypher.test.request(String request, String awaitedReturn) - Get all the case present in the database")
	public Stream<BooleanResult> testRequest(@Name(value = "Request") String request, @Name(value = "AwaitedReturn") String awaitedReturn) throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);

			Boolean validity = ParisCypherRequestsController.checkValidityRequest(nal, request, awaitedReturn);
			return Stream.of(new BooleanResult(validity));
		} catch (Exception | Neo4jConnectionError e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}


}
