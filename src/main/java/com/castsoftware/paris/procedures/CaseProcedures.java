package com.castsoftware.paris.procedures;

import com.castsoftware.paris.controllers.ParisCaseController;
import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.ProcedureException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.models.Case.Case;
import com.castsoftware.paris.models.Group.Group;
import com.castsoftware.paris.results.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class CaseProcedures {


	@Context
	public GraphDatabaseService db;

	@Context public Transaction transaction;

	@Context public Log log;

	@Procedure(value = "paris.cases.get.all", mode = Mode.WRITE)
	@Description("paris.cases.get.all() - Get all the case present in the database")
	public Stream<CustomCaseResult> getAllCases() throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);

			List<Case> aCase = ParisCaseController.getAllCases(nal);
			return aCase.stream().map(CustomCaseResult::new);
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}


	@Procedure(value = "paris.cases.get.all.categories", mode = Mode.WRITE)
	@Description("paris.cases.get.all.categories() - Get the list of use case categories")
	public Stream<OutputMessage> getCasesCategories() throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			List<String> categories = ParisCaseController.getAllCaseCategories(nal);
			return categories.stream().map(OutputMessage::new);
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "paris.cases.delete.by.id", mode = Mode.WRITE)
	@Description("paris.cases.delete.by.id(Long id) - Delete a specific case using its id")
	public Stream<BooleanResult> deleteById(@Name(value = "Id") Long id) throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			Boolean deleted = ParisCaseController.deleteById(nal, id);
			return Stream.of(new BooleanResult(deleted));
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "paris.cases.create", mode = Mode.WRITE)
	@Description(
			"paris.cases.create( String Title, String Description, List<String> Categories, Boolean active, Boolean selected) - Create a new Dio Case")
	public Stream<CustomCaseResult> createGroup(
			@Name(value = "Title") String title,
			@Name(value = "Description") String description,
			@Name(value = "Categories") List<String> categories,
			@Name(value = "Active") Boolean active,
			@Name(value = "Selected") Boolean selected)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			Case aCase =
					ParisCaseController.createCase(
							nal, title, description, categories, active, selected);
			return Stream.of(new CustomCaseResult(aCase));
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "paris.cases.update.by.id", mode = Mode.WRITE)
	@Description(
			"paris.cases.update.by.id( String Title, String Description, List<String> Categories, Boolean active, Boolean selected) - Create a new Dio Case")
	public Stream<CustomCaseResult> updateGroup(
			@Name(value = "Id") Long id,
			@Name(value = "Title") String title,
			@Name(value = "Description") String description,
			@Name(value = "Categories") List<String> categories,
			@Name(value = "Active") Boolean active,
			@Name(value = "Selected") Boolean selected)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			Case aCase =
					ParisCaseController.updateCase(
							nal, id, title, description, categories, active, selected);
			return Stream.of(new CustomCaseResult(aCase));
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "paris.cases.attach.to.case", mode = Mode.WRITE)
	@Description(
			"paris.cases.attach.to.case(Long IdParent, Long IdChild) - Attach two use case")
	public Stream<RelationshipResult> attachToUseCase(@Name(value = "IdParent") Long idParent, @Name(value = "IdChild") Long idChild)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			Relationship rel = ParisCaseController.attachToCase(nal, idParent, idChild);
			return Stream.of(new RelationshipResult(rel));
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "paris.cases.detach.from.case", mode = Mode.WRITE)
	@Description(
			"paris.cases.detach.from.case(Long IdParent, Long IdChild) - Detach to use case")
	public Stream<RelationshipResult> detachToUseCase(@Name(value = "IdParent") Long idParent, @Name(value = "IdChild") Long idChild)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			Relationship rel = ParisCaseController.detachFromCase(nal, idParent, idChild);
			return Stream.of(new RelationshipResult(rel));
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "paris.cases.get.roots", mode = Mode.WRITE)
	@Description("paris.cases.get.roots() - Get all the root cases present in the database")
	public Stream<CustomCaseResult> getRootCase() throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);

			List<Case> aCase = ParisCaseController.getRootCase(nal);
			return aCase.stream().map(CustomCaseResult::new);
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "paris.cases.get.attached.cases", mode = Mode.WRITE)
	@Description("paris.cases.cases.get.attached.cases(Long idCase) - Get all the cases attached to one UseCase")
	public Stream<CustomCaseResult> getAttachedCases(@Name(value = "IdCase") Long idCase) throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);

			List<Case> aCase = ParisCaseController.getAttachedCases(nal, idCase);
			return aCase.stream().map(CustomCaseResult::new);
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

	@Procedure(value = "paris.cases.get.attached.groups", mode = Mode.WRITE)
	@Description("paris.cases.cases.get.attached.groups(Long idCase) - Get all the cases attached to one UseCase")
	public Stream<CustomGroupResult> getAttachedGroups(@Name(value = "IdCase") Long idCase) throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);

			List<Group> groups = ParisCaseController.getAttachedGroups(nal, idCase);
			return groups.stream().map(CustomGroupResult::new);
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

}
