import sys
import re
import argparse
import shutil

# input
# --sispec sispec-file
# --otspec operand-specification-file
# --insndef instructions-definition
# --sitype method-number

# output
# --gen-insn-opcode (1)
# --gen-insn-table (2)
# --gen-insn-label (3)
# --gen-vmloop-cases (4)
# --gen-psuedo-idef (5)
# --gen-sinsn-operandspec (6)
# --gen-vmgen-params (7)
# --list-si (8)
# -o filename

def gen_label(insn):
  print("I_" + insn.upper() + ":")

def gen_left_brace():
  gen_indent(1)
  print("{")

def gen_right_brace():
  gen_indent(1)
  print("}")

def gen_enter_insn():
  gen_indent(1)
  print("ENTER_INSN(__LINE__);")

def gen_next_insn(incp):
  gen_indent(1)
  if incp:
    print("NEXT_INSN_INCPC();")
  else:
    print("NEXT_INSN_NOINCPC();")

def gen_indent(n):
  for i in range(0,n):
    sys.stdout.write("  ")

def var_prefix(kind):
  if kind == "Register":
    return "r"
  elif kind == "JSValue":
    return "v"
  elif kind == "Subscript":
    return "s"
  elif kind == "Displacement":
    return "d"
  elif kind == "int":
    return "i"
  else:
    raise Exception

def macro_postfix(kind):
  if kind == "Register":
    return "reg"
  elif kind == "JSValue":
    return "value"
  elif kind == "Subscript":
    return "subscr"
  elif kind == "Displacement":
    return "disp"
  elif kind == "int":
    return "int"
  else:
    raise Exception

def ordinal(n):
  if n == 0:
    return "first"
  elif n == 1:
    return "second"
  elif n == 2:
    return "third"
  else:
    raise Exception

def gen_assignment(kind, n):
  pre = var_prefix(kind)
  post = macro_postfix(kind)
  ord = ordinal(n)
  vname = pre + str(n)
  gen_indent(2)
  print(kind + " " + vname + " = get_" + ord + "_operand_" + post + "(insn);")

def gen_assignment_smallprimitive(kind):
  gen_indent(2)
  print("int64_t i1 = get_small_immediate(insn);")

def gen_assignment_bigprimitive(kind):
  gen_indent(2)
  print("Displacement d1 = get_big_disp(insn);")

def gen_include(insn):
  print("#include \"insns/" + insn + ".inc\"")

def labelonly(insn):
  gen_label(insn)

def gen_prologue(insn):
  gen_label(insn)
  gen_enter_insn()
  gen_left_brace()

def gen_epilogue(insn, incp):
  gen_include(insn)
  gen_right_brace()
  gen_next_insn(incp)
  print("")

def smallprimitive(insn, op0, op1):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment_smallprimitive(op1)
  gen_epilogue(insn, True)

def bigprimitive(insn, op0, op1):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment_bigprimitive(op1)
  gen_epilogue(insn, True)

def threeop(insn, op0, op1, op2):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, True)

def twoop(insn, op0, op1):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, True)

def oneop(insn, op0):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_epilogue(insn, True)

def zeroop(insn):
  gen_prologue(insn)
  gen_epilogue(insn, insn != "throw")

def uncondjump(insn, op0):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_epilogue(insn, insn == "pushhandler")

def condjump(insn, op0, op1):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, True)

def getvar(insn, op0, op1, op2):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, True)

def setvar(insn, op0, op1, op2):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, True)

def makeclosure(insn, op0, op1):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, True)

def callop(insn, op0, op1):
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, True)

def unknownop(insn):
  gen_prologue(insn)
  gen_epilogue(insn, True)

def process_argv():
  argparser = argparse.ArgumentParser()
  argparser.add_argument("--sispec", action = "store", type = str)
  argparser.add_argument("--otspec", action = "store", type = str)
  argparser.add_argument("--insndef", action = "store", type = str)
  argparser.add_argument("--sitype", action = "store", type = int)
  argparser.add_argument("--list-si", action = "store_true")
  argparser.add_argument("--print-original-insn-name", action = "store", type = str)

  argparser.add_argument("--gen-psudo-idef", action = "store", type = str)

  argparser.add_argument("--gen-insn-opcode", action = "store_true")
  argparser.add_argument("--gen-insn-table", action = "store_true")
  argparser.add_argument("--gen-insn-label", action = "store_true")
  argparser.add_argument("--gen-ot-spec", action = "store", type = str)
  argparser.add_argument("--print-dispatch-order", action = "store", type = str)
  
  argparser.add_argument("--gen-vmloop-cases", action = "store_true")
  argparser.add_argument("-o", action = "store", dest = "output_filename", type = str)
  args = argparser.parse_args()
  return args

def check_open_file(name, mode, opt):
  if name is None:
    sys.stderr.write("--" + opt + " is missing\n")
    sys.exit(1)
  stream = open(name, mode)
  if stream is None:
    sys.stderr.write("opening " + name + " failed\n")
    sys.exit(1)
  return stream
  
def gen_insn_opcode(args):
  sys.stderr.write("gen_insn_opcode is not implemented yet\n")

def gen_insn_table(args):
  sys.stderr.write("gen_insn_table is not implemented yet\n")

def gen_insn_label(args):
  sys.stderr.write("gen_insn_label is not implemented yet\n")

def gen_vmloop_cases(args):
  stream = check_open_file(args.insndef, "r", "insndef")

  lineno = 0

  for line in stream.readlines():
    lineno = lineno + 1

    if re.match(r"^\/\/", line):
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +.*LABELONLY", line)
    if m:
      insn = m.group('insn')
      labelonly(insn)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +SMALLPRIMITIVE +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      smallprimitive(insn, op0, op1)
      continue
    
    m = re.search(r"^(?P<insn>[a-z]+) +BIGPRIMITIVE +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')      
      op0 = m.group('op0')
      op1 = m.group('op1')
      bigprimitive(insn, op0, op1)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +THREEOP +(?P<op0>\w+) +(?P<op1>\w+) +(?P<op2>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      op2 = m.group('op2')
      threeop(insn, op0, op1, op2)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +TWOOP +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      twoop(insn, op0, op1)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +ONEOP +(?P<op0>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      oneop(insn, op0)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +ZEROOP", line)
    if m:
      insn = m.group('insn')
      zeroop(insn)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +UNCONDJUMP +(?P<op0>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      uncondjump(insn, op0)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +CONDJUMP +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      condjump(insn, op0, op1)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +GETVAR +(?P<op0>\w+) +(?P<op1>\w+) +(?P<op2>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      op2 = m.group('op2')
      getvar(insn, op0, op1, op2)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +SETVAR +(?P<op0>\w+) +(?P<op1>\w+) +(?P<op2>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      op2 = m.group('op2')
      setvar(insn, op0, op1, op2)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +MAKECLOSUREOP +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      op0 = m.group('op0')
      op1 = m.group('op1')
      insn = m.group('insn')
      makeclosure(insn, op0, op1)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +CALLOP +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      callop(insn, op0, op1)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +UNKNOWNOP", line) 
    if m:
      insn = m.group('insn')
      unknownop(insn)
      continue

    print("Line " + lineno.to_s + ": Invalid instruction definition -- " + line)

    stream.close()

def gen_psuedo_idef(args):
  sys.stderr.write("gen_psuedo_idef is not implemented yet\n")  

def gen_sinsn_operandspec(args):
  sys.stderr.write("gen_sinsn_operandspec is not implemented yet\n")

def gen_vmgen_params(args):
  sys.stderr.write("gen_vmgen_params is not implemented yet\n")

def list_si(args, stream):
  for line in stream.readlines():
    m = re.search(r"(?P<insnname>[a-z]+)\((?P<op1>(\-|_|[a-z]+)),(?P<op2>(\-|_|[a-z]+)),(?P<op3>(\-|_|[a-z]+))\):\s*(?P<sinsnname>[a-z]+)", line)
    if m:
      insnname = m.group('insnname')
      op1 = m.group('op1')
      op2 = m.group('op2')
      op3 = m.group('op3')
      sinsnname = m.group('sinsnname')
      print(sinsnname)




def main():
  args = process_argv()

  if args.list_si:
    print("addregfix")
  elif args.print_original_insn_name:
    print("add")
  elif args.gen_insn_opcode:
    with open(args.output_filename, "w") as f:
      print_opcode(f)
  elif args.gen_insn_table:
    with open(args.output_filename, "w") as f:
      print_table(f)
  elif args.gen_insn_label:
    with open(args.output_filename, "w") as f:
      print_label(f)
  elif args.gen_ot_spec:
    with open(args.output_filename, "w") as f:
      print_otspec(f)
  elif args.gen_psudo_idef:
    shutil.copyfile("../insns-label-def/add.idef", args.output_filename)
  elif args.print_dispatch_order:
    print("p0:p1:h0:h1")
      
def print_otspec(f):
  f.write('''add (-,_,!fixnum) unspecified
add (-,_,_) accept
''')

def print_opcode(f):
  f.write('''FIXNUM,
SPECCONST,
STRING,
REGEXP,
NUMBER,
ADD,
SUB,
MUL,
DIV,
MOD,
BITAND,
BITOR,
LEFTSHIFT,
RIGHTSHIFT,
UNSIGNEDRIGHTSHIFT,
LESSTHAN,
LESSTHANEQUAL,
EQ,
EQUAL,
GETARG,
SETARG,
GETPROP,
SETPROP,
SETARRAY,
GETGLOBAL,
SETGLOBAL,
INSTANCEOF,
MOVE,
TYPEOF,
NOT,
NEW,
ISUNDEF,
ISOBJECT,
SETFL,
SETA,
GETA,
GETERR,
GETGLOBALOBJ,
NEWFRAME,
RET,
NOP,
JUMP,
JUMPTRUE,
JUMPFALSE,
GETLOCAL,
SETLOCAL,
MAKECLOSURE,
MAKESIMPLEITERATOR,
NEXTPROPNAMEIDX,
SEND,
NEWSEND,
CALL,
TAILSEND,
TAILCALL,
PUSHHANDLER,
POPHANDLER,
THROW,
LOCALCALL,
LOCALRET,
POPLOCAL,
ERROR,
UNKNOWN,
END,
''')

def print_table(f):
  f.write('''  { "fixnum", SMALLPRIMITIVE },
  { "specconst", SMALLPRIMITIVE },
  { "string", BIGPRIMITIVE },
  { "regexp", BIGPRIMITIVE },
  { "number", BIGPRIMITIVE },
  { "add", THREEOP },
  { "sub", THREEOP },
  { "mul", THREEOP },
  { "div", THREEOP },
  { "mod", THREEOP },
  { "bitand", THREEOP },
  { "bitor", THREEOP },
  { "leftshift", THREEOP },
  { "rightshift", THREEOP },
  { "unsignedrightshift", THREEOP },
  { "lessthan", THREEOP },
  { "lessthanequal", THREEOP },
  { "eq", THREEOP },
  { "equal", THREEOP },
  { "getarg", THREEOP },
  { "setarg", THREEOP },
  { "getprop", THREEOP },
  { "setprop", THREEOP },
  { "setarray", THREEOP },
  { "getglobal", TWOOP },
  { "setglobal", TWOOP },
  { "instanceof", THREEOP },
  { "move", TWOOP },
  { "typeof", TWOOP },
  { "not", TWOOP },
  { "new", TWOOP },
  { "isundef", TWOOP },
  { "isobject", TWOOP },
  { "setfl", ONEOP },
  { "seta", ONEOP },
  { "geta", ONEOP },
  { "geterr", ONEOP },
  { "getglobalobj", ONEOP },
  { "newframe", TWOOP },
  { "ret", ZEROOP },
  { "nop", ZEROOP },
  { "jump", UNCONDJUMP },
  { "jumptrue", CONDJUMP },
  { "jumpfalse", CONDJUMP },
  { "getlocal", GETVAR },
  { "setlocal", SETVAR },
  { "makeclosure", MAKECLOSUREOP },
  { "makesimpleiterator", TWOOP },
  { "nextpropnameidx", TWOOP },
  { "send", CALLOP },
  { "newsend", CALLOP },
  { "call", CALLOP },
  { "tailsend", CALLOP },
  { "tailcall", CALLOP },
  { "pushhandler", UNCONDJUMP },
  { "pophandler", ZEROOP },
  { "throw", ZEROOP },
  { "localcall", UNCONDJUMP },
  { "localret", ZEROOP },
  { "poplocal", ZEROOP },
  { "error", BIGPRIMITIVE },
  { "unknown", UNKNOWNOP },
  { "end", ZEROOP },
''')

def print_label(f):
  f.write('''&&I_FIXNUM,
&&I_SPECCONST,
&&I_STRING,
&&I_REGEXP,
&&I_NUMBER,
&&I_ADD,
&&I_SUB,
&&I_MUL,
&&I_DIV,
&&I_MOD,
&&I_BITAND,
&&I_BITOR,
&&I_LEFTSHIFT,
&&I_RIGHTSHIFT,
&&I_UNSIGNEDRIGHTSHIFT,
&&I_LESSTHAN,
&&I_LESSTHANEQUAL,
&&I_EQ,
&&I_EQUAL,
&&I_GETARG,
&&I_SETARG,
&&I_GETPROP,
&&I_SETPROP,
&&I_SETARRAY,
&&I_GETGLOBAL,
&&I_SETGLOBAL,
&&I_INSTANCEOF,
&&I_MOVE,
&&I_TYPEOF,
&&I_NOT,
&&I_NEW,
&&I_ISUNDEF,
&&I_ISOBJECT,
&&I_SETFL,
&&I_SETA,
&&I_GETA,
&&I_GETERR,
&&I_GETGLOBALOBJ,
&&I_NEWFRAME,
&&I_RET,
&&I_NOP,
&&I_JUMP,
&&I_JUMPTRUE,
&&I_JUMPFALSE,
&&I_GETLOCAL,
&&I_SETLOCAL,
&&I_MAKECLOSURE,
&&I_MAKESIMPLEITERATOR,
&&I_NEXTPROPNAMEIDX,
&&I_SEND,
&&I_NEWSEND,
&&I_CALL,
&&I_TAILSEND,
&&I_TAILCALL,
&&I_PUSHHANDLER,
&&I_POPHANDLER,
&&I_THROW,
&&I_LOCALCALL,
&&I_LOCALRET,
&&I_POPLOCAL,
&&I_ERROR,
&&I_UNKNOWN,
&&I_END,
''')
  # stream = check_open_file(args.sispec, "r", "sispec")

  # if args.gen_insn_opcode:
  #   gen_insn_opcode(args)
  # if args.gen_insn_table:
  #   gen_insn_table(args)
  # if args.gen_insn_label:
  #   gen_insn_label(args)
  # if args.gen_vmloop_cases:
  #   gen_vmloop_cases(args)
  # if args.gen_psuedo_idef:
  #   gen_psuedo_idef(args)
  # if args.gen_sinsn_operandspec:
  #   gen_sinsn_operandspec(args)
  # if args.gen_vmgen_params:
  #   gen_vmgen_params(args)
  # if args.list_si:
  #   list_si(args, stream)

  # stream.close()

main()
