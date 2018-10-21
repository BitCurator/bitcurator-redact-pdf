package bca.redact;

public class ExpressionPattern extends TextPattern {
	public String label;
	public String regex;
	
	public ExpressionPattern(String label, String regex, Action policy) {
		this.label = label;
		this.regex = regex;
		this.policy = policy;
		this.count = 1;
	}

	@Override
	public String getRegex() {
		return this.regex;
	}

	@Override
	public String getLabel() {
		return this.label;
	}

	@Override
	public String getType() {
		return "REGEX";
	}

}
