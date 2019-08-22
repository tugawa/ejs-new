def gen_label(insn)
  puts("I_" + insn.upcase + ":")
end

def gen_left_brace
  puts("  {")
end

def gen_right_brace
  puts("  }")
end

def gen_enter_insn
  puts("  ENTER_INSN(__LINE__);")
end

def gen_next_insn(incp)
  if (incp)
    puts("  NEXT_INSN_INCPC();")
  else
    puts("  NEXT_INSN_NOINCPC();")
  end
end

def var_prefix(kind)
  if (kind == "Register")
    "r"
  elsif (kind == "JSValue")
    "v"
  elsif (kind == "Subscript")
    "s"
  elsif (kind == "Displacement")
    "d"
  elsif (kind == "int")
    "i"
  else
    "???"
  end
end

def macro_postfix(kind)
  if (kind == "Register")
    "reg"
  elsif (kind == "JSValue")
    "value"
  elsif (kind == "Subscript")
    "subscr"
  elsif (kind == "Displacement")
    "disp"
  elsif (kind == "int")
    "int"
  else
    "???"
  end
end

def ordinal(n)
  if (n == 0)
    "first"
  elsif (n == 1)
    "second"
  elsif (n == 2)
    "third"
  else
    "???"
  end
end

def gen_assignment(kind, n)
  pre = var_prefix(kind)
  post = macro_postfix(kind)
  ord = ordinal(n)
  vname = pre + n.to_s
  puts("    " + kind + " " + vname + " = get_" + ord + "_operand_" + post + "(insn);")
end

def gen_assignment_smallprimitive(kind)
  puts("    int64_t i1 = get_small_immediate(insn);")
end

def gen_assignment_bigprimitive(kind)
  puts("    Displacement d1 = get_big_disp(insn);")
end

def gen_include(insn)
  puts("#include \"insns/" + insn + ".inc\"")
end

def commentline
end

def labelonly(insn)
  gen_label(insn)
end

def gen_prologue(insn)
  gen_label(insn)
  gen_enter_insn
  gen_left_brace
end

def gen_epilogue(insn, incp)
  gen_include(insn)
  gen_right_brace
  gen_next_insn(incp)
  puts
end

def smallprimitive(insn, op0, op1)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment_smallprimitive(op1)
  gen_epilogue(insn, true)
end

def bigprimitive(insn, op0, op1)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment_bigprimitive(op1)
  gen_epilogue(insn, true)
end

def threeop(insn, op0, op1, op2)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, true)
end

def twoop(insn, op0, op1)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, true)
end

def oneop(insn, op0)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_epilogue(insn, true)
end

def zeroop(insn)
  gen_prologue(insn)
  gen_epilogue(insn, insn != "throw")
end

def uncondjump(insn, op0)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_epilogue(insn, insn == "pushhandler")
end

def condjump(insn, op0, op1)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, true)
end

def getvar(insn, op0, op1, op2)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, true)
end

def setvar(insn, op0, op1, op2)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, true)
end

def makeclosure(insn, op0, op1)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, true)
end

def callop(insn, op0, op1)
  gen_prologue(insn)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, true)
end

def unknownop(insn)
  gen_prologue(insn)
  gen_epilogue(insn, true)
end

def main
  lineno = 1
  while line = STDIN.gets
    if line =~ /^\/\//
      commentline
    elsif /^(?<insn>[a-z]+) +.*LABELONLY/ =~ line
      labelonly(insn)
    elsif /^(?<insn>[a-z]+) +SMALLPRIMITIVE +(?<op0>\w+) +(?<op1>\w+)/ =~ line
      smallprimitive(insn, op0, op1)
    elsif /^(?<insn>[a-z]+) +BIGPRIMITIVE +(?<op0>\w+) +(?<op1>\w+)/ =~ line
      bigprimitive(insn, op0, op1)
    elsif /^(?<insn>[a-z]+) +THREEOP +(?<op0>\w+) +(?<op1>\w+) +(?<op2>\w+)/ =~ line
      threeop(insn, op0, op1, op2)
    elsif /^(?<insn>[a-z]+) +TWOOP +(?<op0>\w+) +(?<op1>\w+)/ =~ line
      twoop(insn, op0, op1)
    elsif /^(?<insn>[a-z]+) +ONEOP +(?<op0>\w+)/ =~ line
      oneop(insn, op0)
    elsif /^(?<insn>[a-z]+) +ZEROOP/ =~ line
      zeroop(insn)
    elsif /^(?<insn>[a-z]+) +UNCONDJUMP +(?<op0>\w+)/ =~ line
      uncondjump(insn, op0)
    elsif /^(?<insn>[a-z]+) +CONDJUMP +(?<op0>\w+) +(?<op1>\w+)/ =~ line
      condjump(insn, op0, op1)
    elsif /^(?<insn>[a-z]+) +GETVAR +(?<op0>\w+) +(?<op1>\w+) +(?<op2>\w+)/ =~ line
      getvar(insn, op0, op1, op2)
    elsif /^(?<insn>[a-z]+) +SETVAR +(?<op0>\w+) +(?<op1>\w+) +(?<op2>\w+)/ =~ line
      setvar(insn, op0, op1, op2)
    elsif /^(?<insn>[a-z]+) +MAKECLOSUREOP +(?<op0>\w+) +(?<op1>\w+)/ =~ line
      makeclosure(insn, op0, op1)
    elsif /^(?<insn>[a-z]+) +CALLOP +(?<op0>\w+) +(?<op1>\w+)/ =~ line
      callop(insn, op0, op1)
    elsif /^(?<insn>[a-z]+) +UNKNOWNOP/ =~ line
      unknownop(insn)
    else
      puts("Line " + lineno.to_s + ": Invalid instruction definition -- " + line)
    end
    lineno = lineno + 1
  end
end

main
