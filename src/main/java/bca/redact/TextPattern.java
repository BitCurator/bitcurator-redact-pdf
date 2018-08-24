package bca.redact;

/**
 * TextPattern is an abstract class representing patterns that can be found in text documents.
 * Each TextPattern must generate a regular expression that allows the pattern to be matched.
 * Each TextPattern can keep a count of occurances.
 * @author jansen
 *
 */
public abstract class TextPattern {
	public int count;
	public void incr() {
		this.count++;
	}
	public Action policy;
	public String notes;
	public abstract String getRegex();
	public abstract String getLabel();
	public abstract String getType();
}
