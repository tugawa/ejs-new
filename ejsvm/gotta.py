import sys
import re
import argparse
import itertools
import subprocess

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

ofile = sys.stdout

def gen_label(insn):
  ofile.write("I_" + insn.upper() + ":\n")

def gen_left_brace():
  gen_indent(1)
  ofile.write("{\n")

def gen_right_brace():
  gen_indent(1)
  ofile.write("}\n")

def gen_insnlabel_enter(insn):
  gen_label(insn)
  gen_enter_insn()

def gen_enter_insn():
  gen_indent(1)
  ofile.write("ENTER_INSN(__LINE__);\n")

def gen_next_insn(incp):
  gen_indent(1)
  if incp:
    ofile.write("NEXT_INSN_INCPC();\n")
  else:
    ofile.write("NEXT_INSN_NOINCPC();\n")

def gen_indent(n):
  for i in range(0,n):
    ofile.write("  ")

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

def var_name(kind, n):
  pre = var_prefix(kind)
  return pre + str(n)

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

def gen_vardecl(kind, n):
  vname = var_name(kind, n)
  gen_indent(2)
  ofile.write(kind + " " + vname + ";\n")

def gen_var_assignment(n, kind, jsv_kind = None):
  if not jsv_kind: jsv_kind = "_"
  ord = ordinal(n)
  vname = var_name(kind, n)
  if kind == "JSValue" and jsv_kind != "_":
    if jsv_kind in ["fixnum"]:
      right = "cint_to_fixnum(get_" + ord + "_operand_int(insn))"
    elif jsv_kind in ["special"]:
      right = "get_" + ord + "_operand_int(insn)"
    elif jsv_kind in ["string", "flonum"]:
      right = "get_literal(insns, get_" + ord + "_operand_disp(insn))"
    else:
      sys.stderr.write(">>>"+kind+","+jsv_kind+"<<<\n")
  else:
    post = macro_postfix(kind)
    right = "get_" + ord + "_operand_" + post + "(insn)"
  gen_indent(2)
  ofile.write(vname + " = " + right + ";\n")

def gen_goto(label):
  gen_indent(2)
  ofile.write("goto "+label+";\n")

def gen_assignment(kind, n):
  post = macro_postfix(kind)
  ord = ordinal(n)
  vname = var_name(kind, n)
  gen_indent(2)
  ofile.write(kind + " " + vname + " = get_" + ord + "_operand_" + post + "(insn);\n")

def gen_assignment_smallprimitive(kind):
  gen_indent(2)
  ofile.write("int64_t i1 = get_small_immediate(insn);\n")

def gen_assignment_bigprimitive(kind):
  gen_indent(2)
  ofile.write("Displacement d1 = get_big_disp(insn);\n")

def gen_include(insn, *, uselabel = None, deflabel = None):
  if not uselabel: uselabel = insn
  if not deflabel: deflabel = insn
  ofile.write("#define USELABEL(x) " + uselabel + "_ ## x\n")
  ofile.write("#define DEFLABEL(x) " + deflabel + "_ ## x\n")
  ofile.write("#include \"insns/" + insn + ".inc\"\n")
  ofile.write("#undef USELABEL\n")
  ofile.write("#undef DEFLABEL\n")

def gen_dispatch_entry_hook(insn, kinds):
  varlst = [var_name(kind, i)
            for (i, kind) in enumerate(kinds) if kind == "JSValue"]
  macro_name = "INSN_COUNT"+str(len(varlst))
  macro_param = insn+","+(",".join(varlst))
  gen_indent(2)
  ofile.write(macro_name+"("+macro_param+");\n")

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
  ofile.write("\n")

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

def gen_insn_body(insn_name, main_insn, kinds, jsv_types, incpc, sitype):
  gen_insnlabel_enter(insn_name)
  for (i, (kind, jsv_type)) in enumerate(zip(kinds, jsv_types)):
    gen_var_assignment(i, kind, jsv_type)

  if not sitype or sitype == 1:
    gen_include(insn_name)
    gen_next_insn(incpc)
  elif sitype == 2:
    gen_include(insn_name, uselabel = main_insn)
    gen_next_insn(incpc)
  elif sitype == 3:
    gen_include(insn_name)
  elif sitype == 4:
    types = ["any" if x == "_" else x for x in jsv_types if x != "-"]
    gen_dispatch_entry_hook(main_insn, kinds)
    gen_goto("_".join(["TL" + main_insn] + types))
  elif sitype == 5:
    gen_goto(main_insn + "_HEAD")

def anyop(insninfo, sinsns, sitype):
  insn_name = insninfo["insn"]
  incpc = insn_name != "throw"

  gen_left_brace()
  for (i, operand) in enumerate(insninfo["ops"]):
    gen_vardecl(operand, i)

  for sinsninfo in sinsns:
    sinsn_name = sinsninfo["sinsnname"]
    kinds = insninfo["ops"]
    jsv_types = sinsninfo["ops"]
    gen_insn_body(sinsn_name, insn_name, kinds, jsv_types, incpc, sitype)

  kinds = insninfo["ops"]
  jsv_types = ["_" for x in kinds]
  gen_insn_body(insn_name, insn_name, kinds, jsv_types, incpc, None)

  gen_right_brace()
  ofile.write("\n")

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
  argparser.add_argument("--gen-insn-opcode", action = "store_true")
  argparser.add_argument("--gen-insn-table", action = "store_true")
  argparser.add_argument("--gen-insn-label", action = "store_true")
  argparser.add_argument("--gen-vmloop-cases", action = "store_true")
  argparser.add_argument("--gen-pseudo-idef", action = "store", type = str)
  argparser.add_argument("--gen-ot-spec", action = "store", type = str)
  argparser.add_argument("--print-dispatch-order", action = "store", type = str)
  argparser.add_argument("--list-si", action = "store_true")
  argparser.add_argument("--print-original-insn-name", action = "store", type = str)
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

def open_file_if_possible(name, mode, opt):
  if name is None:
    return None
  stream = open(name, mode)
  if stream is None:
    return None
  return stream

def read_insndef(args):
  insns = []
  stream = check_open_file(args.insndef, "r", "insndef")

  lineno = 0

  for line in stream.readlines():
    lineno = lineno + 1

    if re.match(r"^\/\/", line):
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +(?P<type>[A-Z]+) +LABELONLY", line)
    if m:
      insn = m.group('insn')
      type = m.group('type')
      a = { "insn": insn, "type": type, "label": "LABELONLY" }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +SMALLPRIMITIVE +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      a = { "insn": insn, "type": "SMALLPRIMITIVE", "op0": op0, "op1": op1 }
      insns.append(a)
      continue
    
    m = re.search(r"^(?P<insn>[a-z]+) +BIGPRIMITIVE +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')      
      op0 = m.group('op0')
      op1 = m.group('op1')
      a = { "insn": insn, "type": "BIGPRIMITIVE", "op0": op0, "op1": op1 }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +THREEOP +(?P<op0>\w+) +(?P<op1>\w+) +(?P<op2>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      op2 = m.group('op2')
      a = { "insn": insn, "type": "THREEOP", "ops": [op0, op1, op2] }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +TWOOP +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      a = { "insn": insn, "type": "TWOOP", "ops": [op0, op1] }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +ONEOP +(?P<op0>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      a = { "insn": insn, "type": "ONEOP", "ops": [op0] }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +ZEROOP", line)
    if m:
      insn = m.group('insn')
      a = { "insn": insn, "type": "ZEROOP", "ops": [] }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +UNCONDJUMP +(?P<op0>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      a = { "insn": insn, "type": "UNCONDJUMP", "op0": op0 }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +CONDJUMP +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      a = { "insn": insn, "type": "CONDJUMP", "op0": op0, "op1": op1 }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +GETVAR +(?P<op0>\w+) +(?P<op1>\w+) +(?P<op2>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      op2 = m.group('op2')
      a = { "insn": insn, "type": "GETVAR", "op0": op0, "op1": op1, "op2": op2 }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +SETVAR +(?P<op0>\w+) +(?P<op1>\w+) +(?P<op2>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      op2 = m.group('op2')
      a = { "insn": insn, "type": "SETVAR", "op0": op0, "op1": op1, "op2": op2 }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +MAKECLOSUREOP +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      op0 = m.group('op0')
      op1 = m.group('op1')
      insn = m.group('insn')
      a = { "insn": insn, "type": "MAKECLOSUREOP", "op0": op0, "op1": op1 }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +CALLOP +(?P<op0>\w+) +(?P<op1>\w+)", line)
    if m:
      insn = m.group('insn')
      op0 = m.group('op0')
      op1 = m.group('op1')
      a = { "insn": insn, "type": "CALLOP", "op0": op0, "op1": op1 }
      insns.append(a)
      continue

    m = re.search(r"^(?P<insn>[a-z]+) +UNKNOWNOP", line) 
    if m:
      insn = m.group('insn')
      a = { "insn": insn, "type": "UNKNOWNOP", "op0": op0, "op1": op1 }
      insns.append(a)
      continue

    ofile.write("Line " + str(lineno) + ": Invalid instruction definition -- " + line + "\n")

  stream.close()
  return insns    

def read_sinsns(args):
  if not args.sitype:
    return []
  sinsns = []
  stream = check_open_file(args.sispec, "r", "sispec")
  for line in stream.readlines():
    m = re.search(r"(?P<insnname>[a-z]+)\((?P<op1>(\-|_|[a-z]+)),(?P<op2>(\-|_|[a-z]+)),(?P<op3>(\-|_|[a-z]+))\):\s*(?P<sinsnname>[a-z]+)", line)
    if m:
      insnname = m.group('insnname')
      op1 = m.group('op1')
      op2 = m.group('op2')
      op3 = m.group('op3')
      sinsnname = m.group('sinsnname')
      a = { "sinsnname": sinsnname, "insnname": insnname, "ops": [op1, op2, op3] }
      sinsns.append(a)
  stream.close()
  return sinsns

def read_otspec(args):
  otspec = []
  f = check_open_file(args.otspec, "r", "otspec")
  for line in f.readlines():
    line = line.strip()
    if re.match(r"^\s*#.*$", line): continue
    if re.match(r"^\s*$", line): continue
    m = re.match(r"(?P<insnname>[a-z]+)\s*\((?P<ops>[^)]*)\)\s*(?P<action>[a-z]+)", line)
    if not m:
      raise Exception("operand spec file format error:" + line)
    insnname = m.group("insnname")
    ops = re.split(r"\s*,\s*", m.group('ops'))
    action = m.group("action")
    a = { "insnname": insnname, "ops": ops, "action": action }
    otspec.append(a)
  f.close()
  return otspec

def gen_insn_opcode(args):
  insndefs = read_insndef(args)
  sinsns = read_sinsns(args)
  for insninfo in insndefs:
    insn = insninfo["insn"]
    ofile.write(insn.upper() + ",\n")

  for sinsninfo in sinsns:
    sinsnname = sinsninfo["sinsnname"]
    ofile.write(sinsnname.upper() + ",\n")

def gen_insn_table(args):
  insndefs = read_insndef(args)
  sinsns = read_sinsns(args)
  str_none = "NONE"
  str_lit = "LIT"

  def insn_table_entry(insn, type, optype1, optype2, optype3):
    return "  { \"" + insn + "\", " + type + ", " + optype1 + ", " + optype2 + ", " + optype3 + " },\n"

  for insninfo in insndefs:
    insn = insninfo["insn"]
    type = insninfo["type"]
    if type == "SMALLPRIMITIVE":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "BIGPRIMITIVE":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "THREEOP":
      ofile.write(insn_table_entry(insn, type, str_lit, str_lit, str_lit))
    elif type == "TWOOP":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "ONEOP":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "ZEROOP":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "UNCONDJUMP":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "CONDJUMP":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "GETVAR":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "SETVAR":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "MAKECLOSUREOP":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "CALLOP":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))
    elif type == "UNKNOWNOP":
      ofile.write(insn_table_entry(insn, type, str_none, str_none, str_none))

  def get_optype(op):
    if op == "fixnum":
      return str_lit
    elif op == "special":
      return "SPEC"
    elif op == "string":
      return "STR"
    elif op == "flonum":
      return "NUM"
    elif op == "regexp":
      return "REGEXP"
    else:
      return str_lit
  for sinsninfo in sinsns:
    sinsnname = sinsninfo["sinsnname"]
    optype1 = get_optype(sinsninfo["ops"][0])
    optype2 = get_optype(sinsninfo["ops"][1])
    optype3 = get_optype(sinsninfo["ops"][2])
    ofile.write(insn_table_entry(sinsnname, "THREEOP", optype1, optype2, optype3))
    

def gen_insn_label(args):
  insndefs = read_insndef(args)
  sinsns = read_sinsns(args)
  for insninfo in insndefs:
    insn = insninfo["insn"]
    ofile.write("&&I_" + insn.upper() + ",\n")

  for sinsninfo in sinsns:
    sinsnname = sinsninfo["sinsnname"]
    ofile.write("&&I_" + sinsnname.upper() + ",\n")

def gen_vmloop_cases(args):
  insndefs = read_insndef(args)
  sinsns = read_sinsns(args)
  sitype = args.sitype
  if not sitype and len(sinsns) > 0:
    raise Exception("sitype is not specified")

  for insninfo in insndefs:
    insn = insninfo["insn"]
    type = insninfo["type"]

    if "label" in insninfo:
      labelonly(insn)
      continue

    if type == "SMALLPRIMITIVE":
      op0 = insninfo["op0"]
      op1 = insninfo["op1"]
      smallprimitive(insn, op0, op1)
      continue
    
    if type == "BIGPRIMITIVE":
      op0 = insninfo["op0"]
      op1 = insninfo["op1"]
      bigprimitive(insn, op0, op1)
      continue

    if type in ["THREEOP", "TWOOP", "ONEOP", "ZEROOP"]:
      anyop(insninfo, [x for x in sinsns if x["insnname"] == insn], sitype)
      continue

    if type == "UNCONDJUMP":
      op0 = insninfo["op0"]
      uncondjump(insn, op0)
      continue

    if type == "CONDJUMP":
      op0 = insninfo["op0"]
      op1 = insninfo["op1"]
      condjump(insn, op0, op1)
      continue

    if type == "GETVAR":
      op0 = insninfo["op0"]
      op1 = insninfo["op1"]
      op2 = insninfo["op2"]
      getvar(insn, op0, op1, op2)
      continue

    if type == "SETVAR":
      op0 = insninfo["op0"]
      op1 = insninfo["op1"]
      op2 = insninfo["op2"]
      setvar(insn, op0, op1, op2)
      continue

    if type == "MAKECLOSUREOP":
      op0 = insninfo["op0"]
      op1 = insninfo["op1"]
      makeclosure(insn, op0, op1)
      continue

    if type == "CALLOP":
      op0 = insninfo["op0"]
      op1 = insninfo["op1"]
      callop(insn, op0, op1)
      continue

    if type == "UNKNOWNOP":
      unknownop(insn)
      continue

def gen_pseudo_idef(args):
  insn_name = args.gen_pseudo_idef
  insndefs = read_insndef(args)
  vmdatatypes = [
    "fixnum",
    "flonum",
    "string",
    "special",
    "simple_object",
    "builtin",
    "function",
    "array",
    "iterator",
    "regexp",
    "number_object",
    "string_object",
    "boolean_object"
  ]

  # find insninfo
  insninfo = [x for x in insndefs if x["insn"] == insn_name][0]

  # count the number of JSValue operands
  jsvalue_count = 0
  params = []
  for (i, operand) in enumerate(insninfo["ops"]):
    if operand == "JSValue":
      params.append("Value _vv_" + str(jsvalue_count))
      jsvalue_count += 1
    else:
      params.append("%s x%d" % (operand, i))

  # print
  ofile.write("\\inst %s (%s)\n" % (insn_name, ", ".join(params)))
  for oplist in itertools.product(vmdatatypes, repeat=jsvalue_count):
    cond_list = ["_vv_"+str(v)+":"+t for (v, t) in enumerate(oplist)]
    cond = " && ".join(cond_list)
    goto_label = "TL" + insn_name + "_" + "_".join(oplist)
    ofile.write("\\when %s \\{\n" % cond)
    ofile.write("  goto %s;\n" % goto_label)
    ofile.write("\\}\n")

def gen_sinsn_operandspec(args):
  sinsn_name = args.gen_ot_spec
  sinsns = read_sinsns(args)
  otspec = read_otspec(args)

  sinsninfo = [x for x in sinsns if x["sinsnname"] == sinsn_name][0]
  insn_name = sinsninfo["insnname"]
  ops = sinsninfo["ops"]

  def otspec_line(name, ops, action):
    return name + "(" + ",".join(ops) + ") " + action + "\n"

  first_line_ops = ["!"+x if x != "-" and x != "_" else x for x in ops]
  ofile.write(otspec_line(insn_name, first_line_ops, "unspecified"))
  for rec in otspec:
    if rec["insnname"] == insn_name:
      ofile.write(otspec_line(insn_name, rec["ops"], rec["action"]))

def print_dispatch_order(args, sinsns):
  insn_name = args.print_dispatch_order
  jsv_count = 0
  dispatch_order = []
  sinsninfos = [x for x in sinsns if x["insnname"] == insn_name]
  if len(sinsninfos) == 0:
    ofile.write("p0:p1:p2:h0:h1:h2")
  else:
    for (i, operand) in enumerate(sinsninfos[0]["ops"]):
      if operand == "-":
        continue
      dispatch = "p" + str(jsv_count)
      if operand == "_":
        dispatch_order = dispatch_order + [dispatch]
      else:
        dispatch_order = [dispatch] + dispatch_order
      jsv_count += 1
    for i in range(0, jsv_count):
      dispatch = "h" + str(i)
      dispatch_order = dispatch_order + [dispatch]
    ofile.write(":".join(dispatch_order))

def print_original_insn_name(args, sinsns):
  sinsn_name = args.print_original_insn_name
  sinsninfo = [x for x in sinsns if x["sinsnname"] == sinsn_name][0]
  ofile.write(sinsninfo["insnname"] + "\n")

def list_si(args, sinsns):
  for sinsninfo in sinsns:
    ofile.write(sinsninfo["sinsnname"] + "\n")


def main():
  global ofile

  args = process_argv()

  sinsns = read_sinsns(args)
  if args.output_filename:
    ofile = check_open_file(args.output_filename, "w", "output file")

  if args.gen_insn_opcode:
    gen_insn_opcode(args)
  if args.gen_insn_table:
    gen_insn_table(args)
  if args.gen_insn_label:
    gen_insn_label(args)
  
  if args.gen_vmloop_cases:
    gen_vmloop_cases(args)
  if args.gen_pseudo_idef:
    gen_pseudo_idef(args)
  if args.gen_ot_spec:
    gen_sinsn_operandspec(args)
  if args.print_dispatch_order:
    print_dispatch_order(args, sinsns)
  if args.print_original_insn_name:
    print_original_insn_name(args, sinsns)
  if args.list_si:
    list_si(args, sinsns)

  if args.output_filename:
    ofile.close()

main()
