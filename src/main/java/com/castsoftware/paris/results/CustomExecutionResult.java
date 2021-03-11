package com.castsoftware.paris.results;

import com.castsoftware.paris.models.Group.GroupResult;

public class CustomExecutionResult {

	public Long idDioGroup;
	public Long objectConcerned;
	public String name;
	public String description;

	public CustomExecutionResult(GroupResult dr) {
		this.idDioGroup = dr.getIdDioGroup();
		this.objectConcerned = dr.getObjectConcerned();
		this.name = dr.getName();
		this.description = dr.getDescription();
	}
}
