package com.castsoftware.dionysus.models.DioGroup;

public enum DioGroupType {
	TAG("Tag"),
	STATISTICS("Statistics"),
	ARCHITECTURE_RULE("Architecture Rule");

	private final String name;

	private DioGroupType(String s) {
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
	public static DioGroupType getFromString(String type) {
		for(DioGroupType i : DioGroupType.values()) {
			if(i.name.equals(type)) return i;
		}

		return null;
	}
}
