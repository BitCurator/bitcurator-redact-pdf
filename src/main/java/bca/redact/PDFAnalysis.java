package bca.redact;

import java.io.File;
import java.util.Objects;

import bca.redact.PDFAnalysisWorker.AnalysisException;

public class PDFAnalysis {
	public File file;
	public String[][] entities;
	public AnalysisException error;
	PDFAnalysis(File file, String[][] entities) {
		this.file = file;
		this.entities = entities;
	}
	
	PDFAnalysis(File file, AnalysisException error) {
		this.file = file;
		this.error = error;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(file);
	}
}
