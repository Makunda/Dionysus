package com.castsoftware.paris.models.Group;

public enum GroupType {
	TAG("Tag"),
	STATISTICS("Statistics"),
	ARCHITECTURE_RULE("Architecture Rule");

	private final String name;

	private GroupType(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}

	/**
	 * Get the corresponding enum of a string
	 * @param type
	 * @return
	 */
	public static GroupType getFromString(String type) {
		for(GroupType i : GroupType.values()) {
			if(i.name.equals(type)) return i;
		}

		return null;
	}
}
