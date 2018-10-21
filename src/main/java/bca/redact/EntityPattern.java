package bca.redact;

import java.util.regex.Pattern;

public class EntityPattern extends TextPattern {
	public EntityPattern(String text, String type, Action policy) {
		this.text = text;
		this.type = type;
		this.policy = policy;
		this.count = 1;
	}
	public String text;
	public String type;
	
	@Override
	public String getRegex() {
		return Pattern.quote(this.text);
	}

	@Override
	public String getLabel() {
		return this.text;
	}

	@Override
	public String getType() {
		return this.type;
	}
}
