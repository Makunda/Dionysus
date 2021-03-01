package com.castsoftware.dionysus.models.DioGroup;

import com.castsoftware.dionysus.database.Neo4jAL;
import com.castsoftware.dionysus.database.Neo4jTypeManager;
import com.castsoftware.dionysus.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.List;
import java.util.stream.Collectors;

public class DioGroup {
  // Label
  private static final String LABEL_PROPERTY = "DioGroup";

  // RelationShips

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
  private List<DioGroupType> types;

  public DioGroup(
      Boolean active,
      List<String> categories,
      Long creationDate,
      String cypherRequest,
      String cypherRequestReturn,
      String description,
      String groupName,
      String name,
      Boolean selected,
      List<DioGroupType> types) {
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
    node.setProperty(getCategoriesProperty(), categories);
    node.setProperty(getCreationDateProperty(), creationDate);
    node.setProperty(getCypherRequest(), cypherRequest);
    node.setProperty(getCypherRequestProperty(), cypherRequestReturn);
    node.setProperty(getDescription(), description);
    node.setProperty(getGroupName(), groupName);
    node.setProperty(getNameProperty(), name);
    node.setProperty(getSelectedProperty(), selected);
    node.setProperty(getSelectedProperty(), types.stream().map(DioGroupType::toString).toArray());

    this.node = node;
    return this.node;
  }

  public static Label getLabelProperty() {
    return Label.label(LABEL_PROPERTY);
  }

  /**
   * Create a DioGroup from a node
   *
   * @param node
   * @return
   */
  static DioGroup fromNode(Node node) {
    assert node != null : "The node used to init the Object cannot be null";

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
    List<DioGroupType> type =
        typesAsString.stream().map(DioGroupType::getFromString).collect(Collectors.toList());

    DioGroup dn =
        new DioGroup(
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
    setNodeProperty(CATEGORIES_PROPERTY, categories);
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

  public List<DioGroupType> getTypes() {
    return types;
  }

  public void setTypes(List<DioGroupType> types) {
    this.types = types;
    // Convert Types to string
    setNodeProperty(NAME_PROPERTY, types.stream().map(DioGroupType::toString).toArray());
  }

  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }
}
