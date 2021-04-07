package com.castsoftware.paris.models.Group;

public class GroupResult {
	private Long idGroup;
	private Long objectConcerned;
	private String name;
	private String description;

	public Long getObjectConcerned() {
		return objectConcerned;
	}

	public void setObjectConcerned(Long objectConcerned) {
		this.objectConcerned = objectConcerned;
	}

	public String getName() {
		return name;
	}


	public String getDescription() {
		return description;
	}



	public Long getIdGroup() {
		return idGroup;
	}


	public GroupResult(Group dg, Long objectConcerned) {
		assert dg.getNode() != null: "Cannot create a GroupResult from a not instantiated Group Node";
		this.idGroup = dg.getNode().getId();
		this.objectConcerned = objectConcerned;
		this.name = dg.getName();
		this.description = dg.getDescription();
	}
}
