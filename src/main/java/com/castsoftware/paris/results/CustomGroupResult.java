package com.castsoftware.paris.results;

import com.castsoftware.paris.models.Group.Group;
import com.castsoftware.paris.models.Group.GroupType;

import java.util.List;
import java.util.stream.Collectors;

public class CustomGroupResult {

	public Long id;
	public Boolean active;
	public List<String> categories;
	public Long creationDate;
	public String cypherRequest;
	public String cypherRequestReturn;
	public String description;
	public String groupName;
	public String name;
	public Boolean selected;
	public List<String> types;

	public CustomGroupResult(Long id, Boolean active, List<String> categories, Long creationDate, String cypherRequest, String cypherRequestReturn, String description, String groupName, String name, Boolean selected, List<String> types) {
		this.id = id;
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

	public CustomGroupResult(Group dn) {
		if(dn.getNode() != null) {
			this.id = dn.getNode().getId();
		} else {
			this.id = -1L;
		}
		this.active = dn.getActive();
		this.categories = dn.getCategories();
		this.creationDate = dn.getCreationDate();
		this.cypherRequest = dn.getCypherRequest();
		this.cypherRequestReturn = dn.getCypherRequestReturn();
		this.description = dn.getDescription();
		this.groupName = dn.getGroupName();
		this.name = dn.getName();
		this.selected = dn.getSelected();
		this.types = dn.getTypes().stream().map(GroupType::toString).collect(Collectors.toList());
	}
}
