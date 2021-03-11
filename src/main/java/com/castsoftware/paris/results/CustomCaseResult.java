package com.castsoftware.paris.results;

import com.castsoftware.paris.models.Case.Case;

import java.util.List;

public class CustomCaseResult {

	public Long id;
	public String title;
	public String description;
	public List<String> categories;
	public Boolean active;
	public Boolean selected;

	public CustomCaseResult(Case dc) {
		assert dc.getNode() != null :  "Cannot create a DioResult from a note instantiated DioCase";

		this.id = dc.getNode().getId();
		this.title = dc.getTitle();
		this.description = dc.getDescription();
		this.categories = dc.getCategories();
		this.active = dc.getActive();
		this.selected = dc.getSelected();
	}
}
