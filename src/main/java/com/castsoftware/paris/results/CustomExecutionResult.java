package com.castsoftware.paris.results;

import com.castsoftware.paris.models.Group.GroupResult;

public class CustomExecutionResult {

	public Long idGroup;
	public Long objectConcerned;
	public String name;
	public String description;

	public CustomExecutionResult(GroupResult dr) {
		this.idGroup = dr.getIdGroup();
		this.objectConcerned = dr.getObjectConcerned();
		this.name = dr.getName();
		this.description = dr.getDescription();
	}
}
