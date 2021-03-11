package com.castsoftware.paris.procedures;

import com.castsoftware.paris.controllers.ParisGroupController;
import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.ProcedureException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jConnectionError;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.models.Group.Group;
import com.castsoftware.paris.models.Group.GroupResult;
import com.castsoftware.paris.results.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.stream.Stream;

public class GroupProcedures {

  @Context public GraphDatabaseService db;

  @Context public Transaction transaction;

  @Context public Log log;

  @Procedure(value = "paris.groups.get.all", mode = Mode.WRITE)
  @Description("paris.groups.get.all() - Get all the groups present in the database")
  public Stream<CustomGroupResult> getAllDioGroups() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);

      List<Group> groups = ParisGroupController.getAllGroupNodes(nal);
      return groups.stream().map(CustomGroupResult::new);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "paris.groups.get.all.by.category", mode = Mode.WRITE)
  @Description(
      "paris.groups.get.all.by.category(String category) - Get the list group by category")
  public Stream<CustomGroupResult> getDioGroupsByCategory(@Name(value = "Category") String category)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<Group> groups = ParisGroupController.getGroupsByCategory(nal, category);
      return groups.stream().map(CustomGroupResult::new);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "paris.groups.get.all.categories", mode = Mode.WRITE)
  @Description("paris.groups.get.all.categories() - Get the list categories")
  public Stream<OutputMessage> getDioGroupsCategories() throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<String> categories = ParisGroupController.getAllGroupCategories(nal);
      return categories.stream().map(OutputMessage::new);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "paris.groups.delete.by.id", mode = Mode.WRITE)
  @Description("paris.groups.delete.by.id() - Delete a specific groups using its id")
  public Stream<BooleanResult> deleteById(@Name(value = "Id") Long id) throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Boolean deleted = ParisGroupController.deleteById(nal, id);
      return Stream.of(new BooleanResult(deleted));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "paris.groups.create", mode = Mode.WRITE)
  @Description(
      "paris.groups.create(Boolean active, List<String> categories, Long creationDate, String cypherRequest, String cypherRequestReturn, String description, String groupName, String name, Boolean selected, List<String> typesAsList) - Create a new Dio Group")
  public Stream<CustomGroupResult> createDioGroup(
      @Name(value = "Active") Boolean active,
      @Name(value = "Categories") List<String> categories,
      @Name(value = "CreationDate") Long creationDate,
      @Name(value = "CypherRequest") String cypherRequest,
      @Name(value = "CypherRequestReturn") String cypherRequestReturn,
      @Name(value = "Description") String description,
      @Name(value = "GroupName") String groupName,
      @Name(value = "Name") String name,
      @Name(value = "Selected") Boolean selected,
      @Name(value = "Types") List<String> types)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Group group =
          ParisGroupController.createGroup(
              nal,
              active,
              categories,
              creationDate,
              cypherRequest,
              cypherRequestReturn,
              description,
              groupName,
              name,
              selected,
              types);
      return Stream.of(new CustomGroupResult(group));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "paris.groups.update.by.id", mode = Mode.WRITE)
  @Description(
      "paris.groups.update.by.id(Boolean active, List<String> categories, Long creationDate, String cypherRequest, String cypherRequestReturn, String description, String groupName, String name, Boolean selected, List<String> typesAsList) - Delete a specific groups using its id")
  public Stream<CustomGroupResult> updateDioGroup(
      @Name(value = "Id") Long id,
      @Name(value = "Active") Boolean active,
      @Name(value = "Categories") List<String> categories,
      @Name(value = "CreationDate") Long creationDate,
      @Name(value = "CypherRequest") String cypherRequest,
      @Name(value = "CypherRequestReturn") String cypherRequestReturn,
      @Name(value = "Description") String description,
      @Name(value = "GroupName") String groupName,
      @Name(value = "Name") String name,
      @Name(value = "Selected") Boolean selected,
      @Name(value = "Types") List<String> types)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Group group =
          ParisGroupController.updateGroupByID(
              nal,
              id,
              active,
              categories,
              creationDate,
              cypherRequest,
              cypherRequestReturn,
              description,
              groupName,
              name,
              selected,
              types);
      return Stream.of(new CustomGroupResult(group));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

	@Procedure(value = "paris.groups.attach.to.case", mode = Mode.WRITE)
	@Description(
			"paris.groups.attach.to.case(Long IdUseCase, Long IdGroup) - Attach a group to a use case")
	public Stream<RelationshipResult> attachToUseCase(@Name(value = "IdUseCase") Long idUseCase, @Name(value = "IdGroup") Long idGroup)
			throws ProcedureException {

		try {
			Neo4jAL nal = new Neo4jAL(db, transaction, log);
			Relationship rel = ParisGroupController.attachToCase(nal, idGroup, idUseCase);
			return Stream.of(new RelationshipResult(rel));
		} catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
			ProcedureException ex = new ProcedureException(e);
			log.error("An error occurred while executing the procedure", e);
			throw ex;
		}
	}

  @Procedure(value = "paris.groups.detach.from.case", mode = Mode.WRITE)
  @Description(
      "paris.groups.detach.from.case(Long IdUseCase, Long IdGroup) - Detach a group from a use case")
  public Stream<RelationshipResult> detachToUseCase(
      @Name(value = "IdUseCase") Long idUseCase, @Name(value = "IdGroup") Long idGroup)
      throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      Relationship rel = ParisGroupController.detachFromCase(nal, idGroup, idUseCase);
      return Stream.of(new RelationshipResult(rel));
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }

  @Procedure(value = "paris.groups.forecast.all", mode = Mode.WRITE)
  @Description(
          "paris.groups.forecast.all(String application) - Forecast the result of the group on one application")
  public Stream<CustomExecutionResult> forecastAll(@Name(value = "Application") String application)
          throws ProcedureException {

    try {
      Neo4jAL nal = new Neo4jAL(db, transaction, log);
      List<GroupResult> results = ParisGroupController.forecastAllGroups(nal, application);
      return results.stream().map(CustomExecutionResult::new);
    } catch (Exception | Neo4jConnectionError | Neo4jQueryException e) {
      ProcedureException ex = new ProcedureException(e);
      log.error("An error occurred while executing the procedure", e);
      throw ex;
    }
  }
}
