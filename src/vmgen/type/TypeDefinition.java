package vmgen.type;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.MatchResult;

import vmgen.Plan;
import vmgen.synth.TagPairSynthesiser;


public class TypeDefinition {
	static String quoted;

	static void parseLine(String line) {
		Scanner sc = new Scanner(line);
		try {
			sc.findInLine("([a-zA-Z_]+)\\s*:\\s*");
			String name = sc.match().group(1);
			VMDataType dt = VMDataType.get(name, true);

			if (dt != null) {
				while (sc.hasNext("\\+[a-zA-Z_].*")) {
					sc.findInLine("\\+([a-zA-Z_]+)");
					MatchResult m = sc.match();
					String rtName = m.group(1);
					VMRepType rt = VMRepType.get(rtName, true);
					if (rt == null)
						rt = new VMRepType(rtName);
					dt.addVMRepType(rt);
				}
			} else {
				if (sc.hasNext("([a-zA-Z_]+)\\(([01]*)\\)(/([a-zA-Z_]+)\\((\\d+)\\))?")){
					sc.findInLine("([a-zA-Z_]+)\\(([01]*)\\)(/([a-zA-Z_]+)\\((\\d+)\\))?");
					MatchResult m = sc.match();
					String pTagName = m.group(1);
					int pTagValue = 0;
					int pTagLength = 0;
					for (int i = 0; i < m.group(2).length(); i++) {
						pTagLength ++;
						pTagValue <<= 1;
						if (m.group(2).charAt(i) == '1')
							pTagValue += 1;
					}
					String hTypeName = m.group(4);
					int hTypeValue = m.group(5) == null ? 0 : Integer.parseInt(m.group(5));

					VMRepType rt = VMRepType.get(name, true);
					if (rt == null)
						rt = new VMRepType(name);
					
					String struct = null;
					if (sc.hasNext())
						struct = sc.next();
					rt.initialise(pTagName, pTagValue, pTagLength, hTypeName, hTypeValue, struct);
				} else {
					throw new Exception();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("next = "+ sc.next());
			System.exit(1);
		}
	}

	public static void load(String filename) throws FileNotFoundException {
		Scanner sc = new Scanner(new FileInputStream(filename));
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			line = line.split("#")[0];
			if (line.matches("^\\s*$"))
				continue;
			if (line.equals("%%%"))
				break;
			if (line.contains(":"))
				parseLine(line);
		}
		StringBuilder sb = new StringBuilder();
		while (sc.hasNextLine()) {
			sb.append(sc.nextLine());
			sb.append('\n');
		}
		quoted = sb.toString();
	}

	public static String getQuoted() {
		return quoted;
	}

	@Override
	public String toString() {
		String s = "";
		for (VMDataType dt: VMDataType.all()) {
			s += dt + "\n";
		}
		s += quoted;
		return s;
	}

	public static void main(String[] args) throws FileNotFoundException {
		TypeDefinition.load("datatype/new.dtdef");
		Plan p = new Plan();
		new TagPairSynthesiser().synthesise(p);
	}
}
