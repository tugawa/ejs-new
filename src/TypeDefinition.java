package vmgen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;

public class TypeDefinition {
	class DataType {
		DataType(String line) {
			Scanner sc = new Scanner(line);
			try {
				sc.findInLine("([a-zA-Z_]+)\\s*:\\s*");
				jsType = sc.match().group(1);
			
				reprs = new ArrayList<Representation>();
				while (sc.hasNext("[a-zA-Z_].*")) {
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
					
					Representation r = new Representation(pTagName, pTagValue, pTagLength, hTypeName, hTypeValue);
					reprs.add(r);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("next = "+ sc.next());
			}
		}
		String jsType;
		List<Representation> reprs;
		@Override
		public String toString() {
			String s = jsType + " =";
			for (Representation r : reprs) {
				s += " " + r;
			}
			return s;
		}
		class Representation {
			Representation(String pTagName, int pTagValue, int pTagLength, String hTypeName, int hTypeValue) {
				this.pTagName = pTagName;
				this.pTagValue = pTagValue;
				this.pTagLength = pTagLength;
				this.hasHType = hTypeName != null;
				this.hTypeName = hTypeName;
				this.hTypeValue = hTypeValue;
			}
			String pTagName;
			int pTagValue;
			int pTagLength;
			boolean hasHType;
			String hTypeName;
			int hTypeValue;	
			@Override
			public String toString() {
				String pt = String.format("%s(%d/%d)", pTagName, pTagValue, pTagLength);
				String ht = hasHType ? String.format("/%s(%d)", hTypeName, hTypeValue) : "";
				return pt + ht;
			}
		}
	}
	
	Map<String, DataType> dataTypes = new HashMap<String, DataType>();
	String quoted;
	
	void load(String filename) throws FileNotFoundException {
		Scanner sc = new Scanner(new FileInputStream(filename));
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			line = line.split("#")[0];
			if (line.matches("^¥s*$"))
				continue;
			if (line.equals("%%%"))
				break;
			DataType tr = new DataType(line);
			dataTypes.put(tr.jsType, tr);
		}
		StringBuilder sb = new StringBuilder();
		while (sc.hasNextLine()) {
			sb.append(sc.nextLine());
			sb.append('\n');
		}
		quoted = sb.toString();
	}

	/*
	 * Covert a list of sets of jsTypeNames to the list of sets of
	 * pointer tag names that correspond to jsTypeNames.
	 */
	List<Set<String>> dispatcherSimpleOne(List<Set<String>> rule) {
		List<Set<String>> pTagDispatcher = new ArrayList<Set<String>>(rule.size());
		for (Set<String> jsTypes: rule) {
			Set<String> ptags = new HashSet<String>();
			for (String jsType: jsTypes) {
				DataType dt = dataTypes.get(jsType);
				for (DataType.Representation r: dt.reprs) {
					if (!r.hasHType)
						ptags.add(r.pTagName);
				}
			}
		}
		return pTagDispatcher;
	}

	@Override
	public String toString() {
		String s = "";
		for (DataType dt: dataTypes.values()) {
			s += dt + "\n";
		}
		s += quoted;
		return s;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		TypeDefinition td = new TypeDefinition();
		td.load("datatype.def");
		System.out.println(td);
	}
}
