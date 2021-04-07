package com.castsoftware.paris.models.Group;

import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.database.Neo4jTypeManager;
import com.castsoftware.paris.exceptions.neo4j.Neo4JTemplateLanguageException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.metaLanguage.MetaLanguageProcessor;
import com.castsoftware.paris.metaLanguage.MetaRequest;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import javax.management.relation.Relation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Group {
  // Label
  private static final String LABEL_PROPERTY = "ParisGroup";

  // RelationShips
  private static final String TO_DIOCASE_RELATIONSHIP = "ADDRESSES";

  // TODO :  Tag of Tags
  private static final String TO_DIOGROUP_RELATIONSHIP = "ADDRESSES";



  // Properties
  private static final String ACTIVE_PROPERTY = "Active";
  private static final String CATEGORIES_PROPERTY = "Categories";
  private static final String CREATION_DATE_PROPERTY = "Creation";
  private static final String CYPHER_REQUEST_PROPERTY = "CypherRequest";
  private static final String CYPHER_REQUEST_RETURN_PROPERTY = "CypherReturn";
  private static final String DESCRIPTION_PROPERTY = "Description";
  private static final String GROUP_NAME_PROPERTY = "GroupName";
  private static final String NAME_PROPERTY = "Name";
  private static final String SELECTED_PROPERTY = "Selected";
  private static final String TYPE_PROPERTY = "Type";

  // Attribute : Neo4j
  private Node node;

  // Attributes : Node
  private Boolean active;
  private List<String> categories;
  private Long creationDate;
  private String cypherRequest;
  private String cypherRequestReturn;
  private String description;
  private String groupName;
  private String name;
  private Boolean selected;
  private List<GroupType> types;

  public Group(
      Boolean active,
      List<String> categories,
      Long creationDate,
      String cypherRequest,
      String cypherRequestReturn,
      String description,
      String groupName,
      String name,
      Boolean selected,
      List<GroupType> types) {
    this.active = active;
    this.categories = categories;
    this.creationDate = creationDate;
    this.cypherRequest = cypherRequest;
    this.cypherRequestReturn = cypherRequestReturn;
    this.description = description;
    this.groupName = groupName;
    this.name = name;
    this.selected = selected;
    this.types = types;
  }

  public Node createNode(Neo4jAL neo4jAL) throws Neo4jQueryException {
    Node node = neo4jAL.createNode(getLabelProperty());

    node.setProperty(getActiveProperty(), active);
    node.setProperty(getCategoriesProperty(), categories.toArray(new String[0]));
    node.setProperty(getCreationDateProperty(), creationDate);
    node.setProperty(getCypherRequestProperty(), cypherRequest);
    node.setProperty(getCypherRequestReturnProperty(), cypherRequestReturn);
    node.setProperty(getDescriptionProperty(), description);
    node.setProperty(getGroupNameProperty(), groupName);
    node.setProperty(getNameProperty(), name);
    node.setProperty(getSelectedProperty(), selected);

    node.setProperty(getTypeProperty(), types.stream().map(GroupType::toString).toArray(String[]::new));

    this.node = node;
    return this.node;
  }

  public static Label getLabelProperty() {
    return Label.label(LABEL_PROPERTY);
  }

  public static String getLabelPropertyAsString() {
    return LABEL_PROPERTY;
  }

  /**
   * Create a DioGroup from a node
   *
   * @param node
   * @return The DioGroup object associated to the node, null if the node doesn't correspond to a DioGroup object
   */
  public static Group fromNode(Node node) {
    assert node != null : "The node used to init the Object cannot be null";
    if(!node.hasLabel(getLabelProperty())) return null;

    Boolean active = Neo4jTypeManager.getAsBoolean(node, getActiveProperty(), false);
    Boolean selected = Neo4jTypeManager.getAsBoolean(node, getSelectedProperty(), false);

    Long creationDate = Neo4jTypeManager.getAsLong(node, getCreationDateProperty(), 0L);
    List<String> categories = Neo4jTypeManager.getAsStringList(node, getCategoriesProperty());
    String cypherRequest = Neo4jTypeManager.getAsString(node, getCypherRequestProperty(), "");
    String cypherRequestReturn =
        Neo4jTypeManager.getAsString(node, getCypherRequestReturnProperty(), "");
    String description = Neo4jTypeManager.getAsString(node, getDescriptionProperty(), "");
    String groupName = Neo4jTypeManager.getAsString(node, getGroupNameProperty(), "");
    String name = Neo4jTypeManager.getAsString(node, getNameProperty(), "");

    // Types
    List<String> typesAsString = Neo4jTypeManager.getAsStringList(node, getTypeProperty());
    List<GroupType> type =
        typesAsString.stream().map(GroupType::getFromString).collect(Collectors.toList());

    Group dn =
        new Group(
            active,
            categories,
            creationDate,
            cypherRequest,
            cypherRequestReturn,
            description,
            groupName,
            name,
            selected,
            type);
    dn.setNode(node);
    return dn;
  }

  public static String getToDiocaseRelationship() {
    return TO_DIOCASE_RELATIONSHIP;
  }

  public static String getActiveProperty() {
    return ACTIVE_PROPERTY;
  }

  public static String getSelectedProperty() {
    return SELECTED_PROPERTY;
  }

  public static String getCreationDateProperty() {
    return CREATION_DATE_PROPERTY;
  }

  public static String getCategoriesProperty() {
    return CATEGORIES_PROPERTY;
  }

  public static String getCypherRequestProperty() {
    return CYPHER_REQUEST_PROPERTY;
  }

  public static String getCypherRequestReturnProperty() {
    return CYPHER_REQUEST_RETURN_PROPERTY;
  }

  public static String getDescriptionProperty() {
    return DESCRIPTION_PROPERTY;
  }

  public static String getGroupNameProperty() {
    return GROUP_NAME_PROPERTY;
  }

  public static String getNameProperty() {
    return NAME_PROPERTY;
  }

  public static String getTypeProperty() {
    return TYPE_PROPERTY;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
    setNodeProperty(ACTIVE_PROPERTY, active);
  }

  /**
   * Set node property if the node exists
   *
   * @param property Key of the property
   * @param value Value of the new property
   */
  private void setNodeProperty(String property, Object value) {
    if (this.node != null) {
      this.node.setProperty(property, value);
    }
  }

  public List<String> getCategories() {
    return categories;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
    setNodeProperty(CATEGORIES_PROPERTY, categories.toArray(new String[0]));
  }

  public Long getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Long creationDate) {
    this.creationDate = creationDate;
    setNodeProperty(CREATION_DATE_PROPERTY, creationDate);
  }

  public String getCypherRequest() {
    return cypherRequest;
  }

  public void setCypherRequest(String cypherRequest) {
    this.cypherRequest = cypherRequest;
    setNodeProperty(CYPHER_REQUEST_PROPERTY, cypherRequest);
  }

  public String getCypherRequestReturn() {
    return cypherRequestReturn;
  }

  public void setCypherRequestReturn(String cypherRequestReturn) {
    this.cypherRequestReturn = cypherRequestReturn;
    setNodeProperty(CYPHER_REQUEST_RETURN_PROPERTY, cypherRequestReturn);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
    setNodeProperty(DESCRIPTION_PROPERTY, description);
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
    setNodeProperty(GROUP_NAME_PROPERTY, groupName);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    setNodeProperty(NAME_PROPERTY, name);
  }

  public Boolean getSelected() {
    return selected;
  }

  public void setSelected(Boolean selected) {
    this.selected = selected;
    setNodeProperty(SELECTED_PROPERTY, selected);
  }

  public List<GroupType> getTypes() {
    return types;
  }

  public void setTypes(List<GroupType> types) {
    this.types = types;
    // Convert Types to string
    setNodeProperty(NAME_PROPERTY, types.stream().map(GroupType::toString).toArray(String[]::new));
  }

  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }


  @Override
  public String toString() {
    return "DioGroup{" +
            "  name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", categories=" + categories +
            ", creationDate=" + creationDate +
            ", cypherRequest='" + cypherRequest + '\'' +
            ", cypherRequestReturn='" + cypherRequestReturn + '\'' +
            ", groupName='" + groupName + '\'' +
            ", selected=" + selected +
            '}';
  }

  /**
   * LOGIC
   */

  /**
   * Apply a tag on the Neo4j element
   * @param neo4jObject Neo4j object to tag
   * @param tag
   * @throws Neo4JTemplateLanguageException
   * @throws Neo4jQueryException
   */
  private void applyTag( Object neo4jObject, String tag) throws Neo4JTemplateLanguageException, Neo4jQueryException {
    // tag a node

    if(neo4jObject instanceof Node) {
      Node node = (Node) neo4jObject;


      if(node.hasProperty("Tags")) {
        List<String> tags = Neo4jTypeManager.getAsStringList(node, "Tags");

        if(!tags.contains(tag)) tags.add(tag);

        node.setProperty("Tags", tags.toArray(new String[0]));
      } else {
        String[] tags = { tag };
        node.setProperty("Tags", tags);
      }

    } else if(neo4jObject instanceof Relationship) { // Tag a rel
      Relationship rel = (Relationship) neo4jObject;
      applyTag(rel.getEndNode(), tag);
      applyTag(rel.getStartNode(), tag);
    }
  }

  private Long executeAsTag(Neo4jAL neo4jAL, String application) throws Neo4JTemplateLanguageException, Neo4jQueryException {
    MetaRequest mr = MetaLanguageProcessor.forgeRequest(this.cypherRequest, this.cypherRequestReturn, application);
    if(mr.getRequest() == null) return 0L;



    Result res = neo4jAL.executeQuery(mr.getRequest());

    // TODO extract to configuration
    String parisPrefix = "p_";



    Long numTagged = 0L;
    while (res.hasNext()) {
      Map<String, Object> returned = res.next();

      applyTag(returned.get(mr.getReturnValue()), parisPrefix+  this.groupName);
      numTagged++;
    }


    neo4jAL.logInfo("Result  : " + numTagged);

    return numTagged;
  }

  // Execute group
  public Long execute(Neo4jAL neo4jAL, String application, GroupType executionType) throws Neo4jQueryException, Neo4JTemplateLanguageException {
    if(executionType == GroupType.TAG) {
      return executeAsTag(neo4jAL, application);
    }

    return 0L;
  }

  /**
   * Forecast the result of a tag
   * @param neo4jAL
   * @param application
   * @return
   * @throws Neo4JTemplateLanguageException
   */
  public GroupResult forecast(Neo4jAL neo4jAL, String application) throws Neo4JTemplateLanguageException {
    if (this.cypherRequest.isBlank()) return null;

    // Build the meta request
    MetaRequest mr = MetaLanguageProcessor.forgeRequest(this.cypherRequest, this.cypherRequestReturn, application);
    if(mr == null) {
      neo4jAL.logError(String.format("It seems that the Meta-Request creation failed for tag : %s", this.toString()));
      return null;
    }

    neo4jAL.logInfo(String.format("DEBUG :: about to execute request : %s ", mr.getRequest()));

    Long numResult = 0L;
    try {
      Result result = neo4jAL.executeQuery(mr.getRequest());
      while (result.hasNext()) {
        result.next();
        numResult ++;
      }

    } catch (Exception | Neo4jQueryException e) {
      neo4jAL.logError(String.format("Failed to execute the request '%s'.", mr.getRequest()));
    }

    return new GroupResult(this, numResult);

  }
}
