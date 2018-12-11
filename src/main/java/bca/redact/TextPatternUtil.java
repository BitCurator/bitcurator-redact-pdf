package bca.redact;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

public class TextPatternUtil {

	public static List<ExpressionPattern> loadBEFeatures(File f) {
		try {
			return IOUtils.readLines(new FileReader(f)).parallelStream()
					.filter(line -> !line.startsWith("#"))
					.map(line -> (String)line.split("\t")[1] )
					.map(pat -> new ExpressionPattern(pat, Pattern.quote(pat), Action.Ask) )
					.collect(Collectors.toList());
		} catch (FileNotFoundException e) {
			throw new Error("Not Expected", e);
		} catch (IOException e) {
			throw new Error("Not Expected", e);
		}
	}
	
	public static List<ExpressionPattern> loadExpressionActionList(File f) {
		try {
			return IOUtils.readLines(new FileReader(f)).parallelStream()
					.filter(line -> !line.startsWith("#") && line.contains("\t"))
					.map(line -> {
						String[] foo = line.split("\t");
						StringBuilder label = new StringBuilder();
						if(foo.length > 2) {
							for(int i = 2; i < foo.length; i++) {
								label.append(foo[i]);
							}
						}
						return new ExpressionPattern(label.toString(), foo[0], Action.valueOf(foo[1]));
					})
					.collect(Collectors.toList());
		} catch (FileNotFoundException e) {
			throw new Error("Not Expected", e);
		} catch (IOException e) {
			throw new Error("Not Expected", e);
		}
	}
	
	public static void saveExpressionPatterns(File f, Collection<ExpressionPattern> list) {
		try(PrintWriter pw = new PrintWriter(new FileWriter(f))) {
			pw.println("# Expression patterns saved from BitCurator PDF Redactor");
			pw.println("# Format: <Regex><tab><Redact/Ignore/Ask><tab>a pattern label");
			pw.println("# Example: \\\\d{3}-\\\\d{2}-\\\\d{4}<tab>Redact<tab>Social Security Numbers");
			list.stream().forEach( ep -> {
				String line = MessageFormat.format("{0}\t{1}\t{2}", ep.regex, ep.policy.name(), ep.label);
				pw.println(line);
			});
		} catch (IOException e) {
			throw new Error(e);
		}
	}
}
