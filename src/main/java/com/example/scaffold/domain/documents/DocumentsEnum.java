package com.example.scaffold.domain.documents;

public enum DocumentsEnum {
	RECEIPT("RECEIPT", "RCP"),
	ARTICLE("ARTICLE", "ART"),
	ORDER("ORDER", "ORD"),
	REPAIR("REPAIR", "RPR");

	private final String targetDestiny;
	private final String prefix;

	DocumentsEnum(String targetDestiny, String prefix) {
		this.targetDestiny = targetDestiny;
		this.prefix = prefix;
	}

	public String getTargetDestiny() {
		return targetDestiny;
	}

	public String getPrefix() {
		return prefix;
	}
}
