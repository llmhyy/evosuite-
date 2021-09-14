package feature.smartseed.example.truecase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class TrueExample {
	private HashMap map = new HashMap<>();
	private int a[] = new int[]{1, 
			12321, 11312, 112312, 141321, 112312, 134,
			22321, 21312, 212312, 241321, 212312,234,
			32321, 31312, 312312, 341321, 312312,334,
			42321, 41312, 412312, 441321, 412312,434,
			52321, 51312, 512312, 541321, 512312,534,
			62321, 61312, 612312, 641321, 612312,634,
			72321, 71312, 712312, 741321, 712312,734,
			82321, 81312, 812312, 841321, 812312,834,
			92321, 91312, 912312, 941321, 912312,934,
			102321, 101312, 1012312, 41321, 12312,34,
			3};
	public static String[] noise = 
		{"abc0", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8", "abc9", "abc10",
		"5abc0", "5abc1", "5abc2", "5abc3", "5abc4", "5abc5", "5abc6", "5abc7", "5abc8", "5abc9", "5abc10",
		"6abc0", "6abc1", "6abc2", "6abc3", "6abc4", "6abc5", "6abc6", "6abc7", "6abc8", "6abc9", "6abc10",
		"7abc0", "7abc1", "7abc2", "7abc3", "7abc4", "7abc5", "7abc6", "7abc7", "7abc8", "7abc9", "7abc10",
		"8abc0", "8abc1", "8abc2", "8abc3", "8abc4", "8abc5", "8abc6", "8abc7", "8abc8", "8abc9", "8abc10",
		"9abc0", "9abc1", "9abc2", "9abc3", "9abc4", "9abc5", "9abc6", "9abc7", "9abc8", "9abc9", "9abc10",};
	
	public static void test(int x, int y, int z) {
		if(x==123456789) {
			if(y==987654321) {
				if(z==555555555) {
					System.currentTimeMillis();
				}
			}
		}
	}
	
	public static void testArray(int a[], int b) {
		for (int ai : a) {
			if (ai == 11111111) {
				System.currentTimeMillis();
			}
		}

	}
	
	public static void test(Obj o, int a) {
		if(a == 123456789) {
			if(o.getAttribute() == 101312) {
				System.currentTimeMillis();
			}
		}
	}
	
	public static void fieldTest(Obj o, int a) {
		if(a == 123456789) {
			if(o.name.equals("noise".toUpperCase())) {
				System.currentTimeMillis();
			}
		}
	}

	public void paralleltest(double x, int y) {
		if (x == 35.4 * 300) {
			System.currentTimeMillis();
		}
		if (y == -2147483646) {
			System.currentTimeMillis();
		} else if (y == 32700) {
			System.currentTimeMillis();
		} else if (y == 4) {
			System.currentTimeMillis();
		} else if (y == 120) {
			System.currentTimeMillis();
		}
	}
	
	public void stringCompare(String[] args) {
		if ("MERGE".equalsIgnoreCase(args[0])) {
			System.currentTimeMillis();
		}else if ("MIGRATE".equalsIgnoreCase(args[0])) {
			System.currentTimeMillis();
		}
	}
	
	public void specialPoint(String[] arsv) {
		if(arsv.length == 0) return;
		String op = arsv[0];
		if (arsv[arsv.length - 1].equals("-debug")) {
			System.currentTimeMillis();
		} else
			System.currentTimeMillis();

		if (op.equals("-add"))
			System.currentTimeMillis();
		else if (op.equals("-subtract"))
			System.currentTimeMillis();
		else if (op.equals("-multiply"))
			System.currentTimeMillis();
		else if (op.equals("-divide"))
			System.currentTimeMillis();
		else if (op.equals("-addconst"))
			System.currentTimeMillis();
		else if (op.equals("-subtractconst"))
			System.currentTimeMillis();
		else if (op.equals("-multiplyconst"))
			System.currentTimeMillis();
		else if (op.equals("-divideconst"))
			System.currentTimeMillis();
		else if (op.equals("-clear"))
			System.currentTimeMillis();
		else if (op.equals("-clip"))
			System.currentTimeMillis();
		else if (op.equals("-bytesize"))
			System.currentTimeMillis();
		else if (op.equals("-diff"))
			System.currentTimeMillis();
		else {
			System.currentTimeMillis();
		}
	}
	
	static int UNKNOWN_SOURCE = -1;
	public static final String NATIVE_METHOD_STRING = "Native Method";
	public static final String UNKNOWN_SOURCE_STRING = "Unknown Source";
	public static void parseStackTraceElement(String ste)
	{
		if(ste == null)
		{
			return;
		}

		int idx = ste.lastIndexOf('(');
		if(idx < 0)
		{
			return; // not a ste
		}
		int endIdx = ste.lastIndexOf(')');
		if(endIdx < 0)
		{
			return; // not a ste
		}

		String classAndMethod = ste.substring(0, idx);
		String source = ste.substring(idx + 1, endIdx);
		String remainder = ste.substring(endIdx + 1);
		idx = classAndMethod.lastIndexOf('.');
		String clazz = classAndMethod.substring(0, idx);
		String method = classAndMethod.substring(idx + 1, classAndMethod.length());
		idx = source.lastIndexOf(':');
		String file = null;
		int lineNumber = UNKNOWN_SOURCE;
		if(idx != -1)
		{
			file = source.substring(0, idx);
			lineNumber = Integer.parseInt(source.substring(idx + 1, source.length()));
		}
		else
		{
			if(source.equals(NATIVE_METHOD_STRING))
			{
				System.currentTimeMillis();
			}
			else if(!source.equals(UNKNOWN_SOURCE_STRING))
			{
				file = source;
			}
		}
		int vEndIdx = remainder.lastIndexOf(']');
		if(vEndIdx >= 0)
		{
			boolean exact = false;
			String versionStr = null;
			if(remainder.startsWith(" ["))
			{
				exact = true;
				versionStr = remainder.substring(2, vEndIdx);
			}
			else if(remainder.startsWith(" ~["))
			{
				exact = false;
				versionStr = remainder.substring(3, vEndIdx);
			}
			if(versionStr != null)
			{
				int colonIdx = versionStr.indexOf(':');
				if(colonIdx > -1)
				{
					String codeLocation = versionStr.substring(0, colonIdx);
					String version = versionStr.substring(colonIdx + 1);
					if("".equals(codeLocation) || "na".equals(codeLocation))
					{
						codeLocation = null;
					}
					if("".equals(version) || "na".equals(version))
					{
						version = null;
					}
					return;
				}
			}
		}

	}
	
	private int[] array;
	public boolean elementIndex(int position) {
		//element index
		//no pool
		if (position >= array.length)
			return false;
		if ((this.array[position] & Integer.MIN_VALUE) != 0x0)
			return true;
		return false;
	}

	public int[] getArray() {
		return array;
	}

	public void setArray(int[] array) {
		this.array = array;
	}
	
	private Vector _switches = new Vector();

	public void JDKInstrument(String name, String value, boolean overwrite) {
		boolean isContain = _switches.contains(name);
		if (isContain) {
			System.currentTimeMillis();
		}

	}
	
	public void addVector(Collection c) {
		_switches.addAll(c);
	}
	
	public void stringContains(String s) {
		char[] charList = s.toCharArray();
		if (charList[charList.length - 3] == 'L') {
			System.currentTimeMillis();
		}
	}
	
	private List<String> list = new ArrayList<>();
	public void arrayList(List<String> localList,String name) {
//		String value = localList.get(localList.size() - 1);
		String value = "append test";
		String subValue = value.concat("it is a testing work");
		
		//field
		if(list.contains(subValue)) {
			System.currentTimeMillis();
		}
		
		boolean isContain = list.contains(name);
		if (isContain) {
			System.currentTimeMillis();
		}
		
		//array
		if(localList.contains(subValue)) {
			System.currentTimeMillis();
		}
		
		isContain = localList.contains(name);
		if (isContain) {
			System.currentTimeMillis();
		}
	}

	public void setList(List<String> list) {
		this.list = list;
	}
	
	public void addList(String s) {
		list.add(s);
	}
	
	ArrayList<String> identifiers;
	int id;
	public void setIdentifier(String s) {
		this.identifiers.add(s);
	}
	
	public static final String[] jjtNodeName = { "CompilationUnit", "PackageDeclaration", "ImportDeclaration", "Modifiers","Literal","VariableDeclaratorId"};
	public void dump(final String prefix){
		if (this.identifiers.size() > 0) {
			if (TrueExample.jjtNodeName[this.id].equals("Literal")) {
				System.currentTimeMillis();
			}else {
				for (String identifier : this.identifiers) {
					if (!identifier.equals(";") && !identifier.equals("}") && !identifier.equals("{") && !identifier.equals(""))
						
						if (!TrueExample.jjtNodeName[this.id].equals("VariableDeclaratorId") || !identifier.equals(")")) {
							if (identifier.equals("<"))
								System.currentTimeMillis();
							if (identifier.equals(">"))
								System.currentTimeMillis();
							if (identifier.equals(">>"))
								System.currentTimeMillis();
							if (identifier.equals("<<"))
								System.currentTimeMillis();
							if (identifier.equals("<="))
								System.currentTimeMillis();
							if (identifier.equals(">="))
								System.currentTimeMillis();
							if (identifier.equals("&") || identifier.equals("&&"))
								System.currentTimeMillis();
						}
				}
			}
		}
	}

	public TrueExample(int i) {
		this.id = i;
	}
		
	
	public static void difficultForEvoSeed(int a, int b, int c, int d, int e, int f, int g) {
		boolean branchCond1 = (a * Integer.valueOf("10") == Integer.valueOf("1234567890"));
		if (branchCond1) {
			boolean branchCond2 = (b * Integer.valueOf("10") == Integer.valueOf("234567890"));
			if (branchCond2) {
				boolean branchCond3 = (c * Integer.valueOf("10") == Integer.valueOf("34567890"));
				if (branchCond3) {
					boolean branchCond4 = (d * Integer.valueOf("10") == Integer.valueOf("4567890"));
					if (branchCond4) {
						boolean branchCond5 = (e * Integer.valueOf("10") == Integer.valueOf("567890"));
						if (branchCond5) {
							boolean branchCond6 = (f * Integer.valueOf("10") == Integer.valueOf("67890"));
							if (branchCond6) {
								boolean branchCond7 = (g * Integer.valueOf("10") == Integer.valueOf("7890"));
								if (branchCond7) {
									System.currentTimeMillis();
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static String[] magicConstants = new String[] { "hello", "world" };

	public static void difficultMethod(String a, String b) {
		if (a.concat(b).equals(magicConstants[0].concat(magicConstants[1]))) {
			System.currentTimeMillis();
		}
	}
	
}
