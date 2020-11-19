import org.teavm.jso.JSBody;

import java.math.BigDecimal;

public class VM {
	@JSBody(params = {"opcode", "data"}, script = "Module.ccall('runWithString', null, [\"number\", \"string\"], [opcode, data]);")
	private static native void runWithString(byte opcode, String data);

	@JSBody(params = {"opcode", "data"}, script = "Module.ccall('runWithNumber', null, [\"number\", \"string\"], [opcode, data]);")
	private static native void runWithDouble(byte opcode, String data);

	@JSBody(params = {"opcode", "data"}, script = "Module.ccall('runWithBool', null, [\"number\", \"boolean\"], [opcode, data]);")
	private static native void runWithBoolean(byte opcode, boolean data);

	@JSBody(params = {"opcode", "data"}, script = "return Module.ccall('disassembleWithString', 'string', [\"number\", \"string\"], [opcode, data]);")
	private static native String disassembleWithString(byte opcode, String data);

	@JSBody(params = {"opcode", "data"}, script = "return Module.ccall('disassembleWithNumber', 'string', [\"number\", \"string\"], [opcode, data]);")
	private static native String disassembleWithDouble(byte opcode, String data);

	@JSBody(params = {"opcode", "data"}, script = "return Module.ccall('disassembleWithBool', 'string', [\"number\", \"boolean\"], [opcode, data]);")
	private static native String disassembleWithBoolean(byte opcode, boolean data);

	@JSBody(script = "return Module.ccall('pop', 'string', null, null);")
	public static native String pop();

	@JSBody(params = {"opcode"}, script = "Module.ccall('run', null, [\"number\"], [opcode]);")
	public static native void run(byte opcode);
	public void run(byte opcode, BigDecimal data) {
		runWithDouble(opcode, data + "");
	}
	public void run(byte opcode, String data) {
		runWithString(opcode, data);
	}
	public void run(byte opcode, boolean data) {
		runWithBoolean(opcode, data);
	}
	public String disassemble(byte opcode, BigDecimal data) {
		return disassembleWithDouble(opcode, data + "");
	}
	public String disassemble(byte opcode) {
		return disassembleWithString(opcode, null);
	}
	public String disassemble(byte opcode, String data) {
		return disassembleWithString(opcode, data);
	}
	public String disassemble(byte opcode, boolean data) {
		return disassembleWithBoolean(opcode, data);
	}

	public static final byte EXIT = 0;
	public static final byte PUT = 1;
	public static final byte POP = 2;
	public static final byte ADD = 15;
	public static final byte SUB = 16;
	public static final byte MUL = 17;
	public static final byte DIV = 18;
	public static final byte MOD = 19;
	public static final byte EQ = 20;
	public static final byte FEQ = 21;
	public static final byte GT = 22;
	public static final byte GE = 23;
	public static final byte LT = 24;
	public static final byte LE = 25;
	public static final byte LAND = 26;
	public static final byte LOR = 27;
	public static final byte AND = 28;
	public static final byte OR = 29;
	public static final byte NOT = 30;
	public static final byte LNOT = 31;
	public static final byte LSHIFT = 32;
	public static final byte RSHIFT = 33;
	public static final byte XOR = 34;
	public static final byte NEG = 35;
	public static final byte MEMSET = 50;
	public static final byte MEMGET = 51;
	public static final byte MEMSIZE = 52;
	public static final byte MEMPUT = 53;
	public static final byte MEMINS = 54;
	public static final byte MEMDEL = 55;
	public static final byte TONUM = 65;
	public static final byte TOTXT = 66;
	public static final byte ISNUM = 67;
	public static final byte CANNUM = 68;
	public static final byte TOBOOL = 69;
	public static final byte REC = 75;
	public static final byte END = 76;
	public static final byte RUN = 77;
	public static final byte REPEAT = 78;
	public static final byte BREAK = 79;
	public static final byte IFTRUN = 80;
	public static final byte IFFRUN = 81;
	public static final byte WTRUN = 82;
	public static final byte WFRUN = 83;
	public static final byte THREAD = 84;
	public static final byte SKIP = 85;
	public static final byte IFSKIP = 86;
	public static final byte MKFN = 87;
	public static final byte CALLFN = 88;
	public static final byte EXITFN = 89;
	public static final byte DLCALL = 100;
	public static final byte PRINT = 101;
}
