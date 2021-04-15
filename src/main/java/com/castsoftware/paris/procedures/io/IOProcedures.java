package com.castsoftware.paris.procedures.io;

import com.castsoftware.paris.controllers.ParisCaseController;
import com.castsoftware.paris.controllers.io.IOController;
import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.ProcedureException;
import com.castsoftware.paris.exceptions.file.FileIOException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.models.Case.Case;
import com.castsoftware.paris.results.CustomCaseResult;
import com.castsoftware.paris.results.OutputMessage;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class IOProcedures {

	@Context
	public GraphDatabaseService db;

	@Context public Transaction transaction;

	@Context public Log log;

	@Procedure(value = "paris.export.all", mode = Mode.WRITE)
	@Description("paris.export.all(String pathToFolder, String zipFileName) - Export all the configuration of Paris")
	public Stream<OutputMessage> export(@Name(value = "PathToFile") String path, @Name(value = "FileName") String zipfileName) throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			Path zipPath = IOController.exportNodes(nal, path, zipfileName);

			log.info("DEBUG : Export done");
			return Stream.of(new OutputMessage(zipPath.toString()));
		} catch (NullPointerException e) {
			log.error("Null Pointer", e);
			log.error("Null Pointer", e.getLocalizedMessage());

			throw e;
		}catch (Exception | Neo4jConnectionError | Neo4jQueryException | FileIOException e) {

			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}



}
