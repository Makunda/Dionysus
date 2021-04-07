package com.castsoftware.paris.models.Case;

import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.database.Neo4jTypeManager;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.List;

public class Case {
	// Label
	public static final String LABEL_PROPERTY = "ParisCase";

	// RelationShips
	private static final String TO_CASE_RELATIONSHIP = "INCLUDES";

	// Properties
	private static final String TITLE_PROPERTY = "Title";
	private static final String DESCRIPTION_PROPERTY = "Description";
	private static final String ACTIVE_PROPERTY = "Active";
	private static final String SELECTED_PROPERTY = "Selected";
	private static final String CATEGORIES_PROPERTY = "Categories";

	// Attribute : Neo4j
	private Node node;

	// Attributes : Node
	private String title;
	private String description;
	private List<String> categories;
	private Boolean active;
	private Boolean selected;

	public Case(String title, String description, List<String> categories, Boolean active, Boolean selected) {
		this.title = title;
		this.description = description;
		this.categories = categories;
		this.active = active;
		this.selected = selected;
	}

	/**
	 * Create a node based on the parameters provided
	 * @param neo4jAL Neo4j Access Layer
	 * @return
	 * @throws Neo4jQueryException
	 */
	public Node createNode(Neo4jAL neo4jAL) throws Neo4jQueryException {
		Node node = neo4jAL.createNode(getLabelProperty());

		node.setProperty(getActiveProperty(), active);
		node.setProperty(getTitleProperty(), title);
		node.setProperty(getDescriptionProperty(), description);
		node.setProperty(getActiveProperty(), active);
		node.setProperty(getSelectedProperty(), selected);

		this.node = node;
		return this.node;
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

	/**
	 * Create a DioCase Object from a node
	 * @param node
	 * @return The DioCase object, null if the node doesn't correspond to a DioCase object
	 */
	public static Case fromNode(Node node) {
		assert node != null : "The node used to init the Object cannot be null";
		if(!node.hasLabel(getLabelProperty())) return null;

		Boolean active = Neo4jTypeManager.getAsBoolean(node, getActiveProperty(), false);
		Boolean selected = Neo4jTypeManager.getAsBoolean(node, getSelectedProperty(), false);

		String title = Neo4jTypeManager.getAsString(node, getTitleProperty(), "");
		String description = Neo4jTypeManager.getAsString(node, getDescriptionProperty(), "");
		List<String> categories = Neo4jTypeManager.getAsStringList(node, getCategoriesProperty());

		Case dc = new Case(title, description, categories, active, selected);
		dc.setNode(node);

		return dc;
	}

	public static String getToCaseRelationship() {
		return TO_CASE_RELATIONSHIP;
	}

	public static Label getLabelProperty() {
		return Label.label(LABEL_PROPERTY);
	}

	public static String getLabelPropertyAsString() {
		return LABEL_PROPERTY;
	}

	public static String getTitleProperty() {
		return TITLE_PROPERTY;
	}

	public static String getDescriptionProperty() {
		return DESCRIPTION_PROPERTY;
	}

	public static String getCategoriesProperty() {
		return CATEGORIES_PROPERTY;
	}

	public static String getActiveProperty() {
		return ACTIVE_PROPERTY;
	}

	public static String getSelectedProperty() {
		return SELECTED_PROPERTY;
	}

	public Node getNode() {
		return node;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public List<String> getCategories() {
		return categories;
	}

	public Boolean getActive() {
		return active;
	}

	public Boolean getSelected() {
		return selected;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public void setTitle(String title) {
		this.title = title;
		setNodeProperty(getTitleProperty(), title);
	}

	public void setDescription(String description) {
		this.description = description;
    	node.setProperty(getDescriptionProperty(), description);
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
		node.setProperty(getCategoriesProperty(), categories.toArray(new String[0]));
	}

	public void setActive(Boolean active) {
		this.active = active;
		node.setProperty(getActiveProperty(), active);
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
		node.setProperty(getSelectedProperty(), selected);
	}
}
