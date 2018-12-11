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
	public String sentence;
	
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
	
	public String getExampleSentence() {
		return this.sentence;
	}
	
	public void setExampleSentence(String sentence) {
		this.sentence = sentence;
	}

}
