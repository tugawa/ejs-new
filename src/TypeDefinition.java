import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TypeDefinition {
	static String quoted;

	static DataType parseLine(String line) {
		Scanner sc = new Scanner(line);
		try {
			sc.findInLine("([a-zA-Z_]+)\\s*:\\s*");
			String name = sc.match().group(1);
			DataType dt = DataType.get(name);

			if (sc.hasNext("\\+.*")) {
				if (dt.parent != null)
					throw new Error("defining sub-sub data type:"+line);
				while (sc.hasNext("\\+[a-zA-Z_].*")) {
					sc.findInLine("\\+([a-zA-Z_]+)");
					MatchResult m = sc.match();
					String subName = m.group(1);
					DataType.add(subName, dt);
				}
			} else {
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

				TypeRepresentation r = new TypeRepresentation(pTagName, pTagValue, pTagLength, hTypeName, hTypeValue);
				dt.addRepresentation(r);

				// structure
				if (sc.hasNext("[a-zA-Z_]+")) {
					sc.findInLine("[a-zA-Z_]+");
					String struct = sc.match().group(0);
					dt.setDataStructure(struct);
				}
			}
			return dt;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("next = "+ sc.next());
			System.exit(1);
			return null;
		}
	}

	static void load(String filename) throws FileNotFoundException {
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

	static String getQuoted() {
		return quoted;
	}

	@Override
	public String toString() {
		String s = "";
		for (DataType dt: DataType.allInSpec()) {
			s += dt + "\n";
		}
		s += quoted;
		return s;
	}

	public static void main(String[] args) throws FileNotFoundException {
		TypeDefinition.load("datatype/new.dtdef");
		System.out.println(DataType.uniquePT(DataType.typeRepresentationOf(Stream.of(
				"fixnum", "string", "array").map(n -> DataType.get(n))),
				DataType.allUsed(false)));
		System.out.println(DataType.uniquePT(DataType.typeRepresentationOf(Stream.of(
				"simple_object",
				"array",
				"function",
				"builtin",
				"iterator",
				"regexp",
				"string_object",
				"number_object",
				"boolean_object",
				"string").map(n -> DataType.get(n))),
				DataType.allUsed(false)));
		Plan p = new Plan();
		new TagPairSynthesiser().synthesise(p);
	}
}
