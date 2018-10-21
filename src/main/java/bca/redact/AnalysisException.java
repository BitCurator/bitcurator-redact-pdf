package bca.redact;

import java.io.File;

public class AnalysisException extends Exception {
	private static final long serialVersionUID = 1L;
	File file;
	Throwable cause;
	AnalysisException(File f, Throwable e) {
		file=f;
		cause=e;
	}
}