package com.castsoftware.paris.io;

import java.util.ArrayList;
import java.util.List;

public class ExportModel {

	private String label;
	private List<String> columns;
	private List<String> pk;
	private List<String> neighborsLabel;

	public ExportModel(String label, List<String> columns) {
		this.label = label;
		this.columns = columns;
		this.pk = new ArrayList<>();
		this.neighborsLabel = new ArrayList<>();
	}

	public List<String> getNeighborsLabel() {
		return neighborsLabel;
	}

	public void setNeighborsLabel(List<String> neighborsLabel) {
		this.neighborsLabel = neighborsLabel;
	}

	/**
	 * Set the primary key of the model. The "Primary key" will be used to merge the node with the same value on the property.
	 * @param pk Primary key as a list ( multiple value can be used ).
	 */
	public void setPk(List<String> pk) {
		this.pk = pk;
	}

	public String getLabel() {
		return label;
	}

	public List<String> getColumns() {
		return columns;
	}
}
