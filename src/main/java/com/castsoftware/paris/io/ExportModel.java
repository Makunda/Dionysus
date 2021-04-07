package com.castsoftware.paris.io;

import java.util.List;

public class ExportModel {

	private String label;
	private List<String> columns;
	private List<String> pk;

	public ExportModel(String label, List<String> columns) {
		this.label = label;
		this.columns = columns;
		this.pk = null;
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
