require 'optparse'

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

def gen_assignment_smallprimitive
  puts("    i1 = get_small_immediate(insn);")
end

def gen_assignment_bigprimitive
  puts("    d1 = get_big_disp(insn);")
end

def kind_type(kind)
  case kind
  when "JSValue", "Fixnum", "Number", "String", "Special" then
    "JSValue"
  when "int" then
    "int64_t"
  else
    "#{kind}"
  end
end

def cast_type(kind, value)
  case kind
  when "Fixnum" then
    "cint_to_fixnum(" + value + ")"
  when "Number", "String" then
    "insns[" + value + "].code"
  else
    value
  end
end

def var_prefix(kind)
  case kind
  when "Register" then
    "r"
  when "JSValue", "Fixnum", "Number", "String", "Special" then
    "v"
  when "Subscript" then
    "s"
  when "Displacement" then
    "d"
  when "int" then
    "i"
  else
    "???"
  end
end

def macro_postfix(kind)
  case kind
  when "Register" then
    "reg"
  when "JSValue" then
    "value"
  when "Subscript" then
    "subscr"
  when "Displacement", "Number", "String" then
    "disp"
  when "int", "Fixnum", "Special" then
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
  puts("    " + vname + " = " + cast_type(kind, "get_" + ord + "_operand_" + post + "(insn)") + ";")
end

def gen_include(insn)
  puts("#include \"insns/" + insn + ".inc\"")
end

def gen_goto_tail_label(insn)
  puts("L_#{insn}_NEXT_INSN:")
end

def gen_goto_tail(insn)
  puts("goto L_#{insn}_NEXT_INSN;")
end

def gen_goto_head(insn)
  puts("goto #{insn}_HEAD;")
end

def gen_define_uselabel(useinsn)
  puts("#define USELABEL(x) #{useinsn}_ ## x")
end
def gen_define_deflabel(definsn)
  puts("#define DEFLABEL(x) #{definsn}_ ## x")
end
def gen_undef_uselabel
  puts("#undef USELABEL")
end
def gen_undef_deflabel
  puts("#undef DEFLABEL")
end

def commentline
end

def labelonly(insn)
  gen_label(insn)
end

def operand_spec_type(kind)
  case kind
  when "reg" then
    "any"
  when "fix" then
    "fixnum"
  when "str" then
    "string"
  when "num" then
    "flonum"
  when "spec" then
    "special"
  else
    ""
  end
end

def gen_superinsn_jump_only(s)
  if s[:op3].nil?
    puts("goto TL#{s[:insn]}_#{operand_spec_type(s[:op1])}_#{operand_spec_type(s[:op2])};")
  else
    puts("goto TL#{s[:insn]}_#{operand_spec_type(s[:op1])}_#{operand_spec_type(s[:op2])}_#{operand_spec_type(s[:op3])};")
  end
end

def gen_variable_decl(op0, op1, op2)
  puts "  #{kind_type(op0)} #{var_prefix(op0)}0;" unless op0.nil?
  puts "  #{kind_type(op1)} #{var_prefix(op1)}1;" unless op1.nil?
  puts "  #{kind_type(op2)} #{var_prefix(op2)}2;" unless op2.nil?
end

def gen_prologue(insn, op0 = nil, op1 = nil, op2 = nil)
  gen_left_brace
  gen_variable_decl(op0, op1, op2)
  gen_label(insn)
  gen_enter_insn
end

def gen_epilogue(useinsn, definsn, incp, jump_origin_insn = nil)
  gen_define_deflabel(definsn)
  gen_define_uselabel(useinsn)
  gen_include(definsn)
  gen_undef_uselabel
  gen_undef_deflabel
  gen_right_brace
  gen_goto_tail_label(definsn)
  if !jump_origin_insn.nil?
    gen_goto_tail(jump_origin_insn)
  else
    gen_next_insn(incp)
  end
  puts
end

def smallprimitive(insn, op0, op1)
  gen_prologue(insn, op0, op1)
  gen_assignment(op0, 0)
  gen_assignment_smallprimitive
  gen_epilogue(insn, insn, true)
end

def bigprimitive(insn, op0, op1)
  gen_prologue(insn, op0, op1)
  gen_assignment(op0, 0)
  gen_assignment_bigprimitive
  gen_epilogue(insn, insn, true)
end

def threeop_with_superinsn(insn, op0, op1, op2, buildtype, sinsnspec_lst)
  gen_left_brace
  gen_variable_decl(op0, op1, op2)
  sinsnspec_lst.each do |s|
    if s[:op3].nil?
      gen_label(s[:sinsn])
      gen_enter_insn
      gen_assignment("Register", 0)
      gen_assignment(op_type(s[:op1]), 1)
      gen_assignment(op_type(s[:op2]), 2)
    else
      gen_label(s[:sinsn])
      gen_enter_insn
      gen_assignment(op_type(s[:op1]), 0)
      gen_assignment(op_type(s[:op2]), 1)
      gen_assignment(op_type(s[:op3]), 2)
    end
    if buildtype == 1
      gen_goto_head(insn)
    elsif buildtype == 2 || buildtype == 3
      gen_define_deflabel(s[:sinsn])
      gen_define_uselabel(insn)
      gen_include(s[:sinsn])
      gen_undef_uselabel
      gen_undef_deflabel
      gen_next_insn(true) unless buildtype == 3
    elsif buildtype == 4 || buildtype == 5
      gen_superinsn_jump_only(s)
    end
  end
  gen_label(insn)
  gen_enter_insn
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, insn, true)
end

def threeop(insnuse, insndef, op0, op1, op2, buildtype = -1)
  gen_prologue(insndef, op0, op1, op2)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  if buildtype == 6
    gen_epilogue(insndef, insndef, true, insnuse)
  elsif buildtype == 0
    gen_epilogue(insndef, insndef, true)
  else
    gen_epilogue(insnuse, insndef, true)
  end
end

def twoop(insn, op0, op1)
  gen_prologue(insn, op0, op1)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, insn, true)
end

def oneop(insn, op0)
  gen_prologue(insn, op0)
  gen_assignment(op0, 0)
  gen_epilogue(insn, insn, true)
end

def zeroop(insn)
  gen_prologue(insn)
  gen_epilogue(insn, insn, insn != "throw")
end

def uncondjump(insn, op0)
  gen_prologue(insn, op0)
  gen_assignment(op0, 0)
  gen_epilogue(insn, insn, insn == "pushhandler")
end

def condjump(insn, op0, op1)
  gen_prologue(insn, op0, op1)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, insn, true)
end

def getvar(insn, op0, op1, op2)
  gen_prologue(insn, op0, op1, op2)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, insn, true)
end

def setvar(insn, op0, op1, op2)
  gen_prologue(insn, op0, op1, op2)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_assignment(op2, 2)
  gen_epilogue(insn, insn, true)
end

def makeclosure(insn, op0, op1)
  gen_prologue(insn, op0, op1)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, insn, true)
end

def callop(insn, op0, op1)
  gen_prologue(insn, op0, op1)
  gen_assignment(op0, 0)
  gen_assignment(op1, 1)
  gen_epilogue(insn, insn, true)
end

def unknownop(insn)
  gen_prologue(insn)
  gen_epilogue(insn, insn, true)
end

def main(superinsn_spec, buildtype)
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
      if buildtype == 1 || buildtype == 2 || buildtype == 3 || buildtype == 4 || buildtype == 5
        next unless superinsn_spec[insn].nil? # skip superinsn
        insn_with_superinsn_spec_lst = []
        superinsn_spec.each { |k, v| insn_with_superinsn_spec_lst << v if v[:insn] == insn }
        if insn_with_superinsn_spec_lst.empty?
          threeop(insn, insn, op0, op1, op2)
        else
          threeop_with_superinsn(insn, op0, op1, op2, buildtype, insn_with_superinsn_spec_lst)
        end
      elsif buildtype == 0 || buildtype == 6
        if !superinsn_spec[insn].nil? # superinsn
          threeop(superinsn_spec[insn][:insn], insn, op0, op1, op2, buildtype)
        else
          threeop(insn, insn, op0, op1, op2)
        end
      else
        threeop(insn, insn, op0, op1, op2)
      end
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

def op_type(kind)
  case kind
  when "reg" then
    "JSValue"
  when "fixnum" then
    "Fixnum"
  when "string" then
    "String"
  when "flonum" then
    "Number"
  when "special" then
    "Special"
  else
    ""
  end
end

def load_suerinsnspec(specfile)
  s = {}
  File.open(specfile) do |f|
    f.each_line do |line|
      line.gsub!(/\s/, "")
      if line =~ /^\/\//
        next
      elsif /^(?<insn>[a-z]+)\s*\(\s*(?<op1>(\-|_|[a-z]+))\s*,\s*(?<op2>(\-|_|[a-z]+))\s*,\s*(?<op3>(\-|_|[a-z]+))\s*\)\s*:\s*(?<sinsn>[a-z]+)\s*/ =~ line
        ops = []
        if op1 != '-'
          if op1 == '_'
            ops.push('reg')
          else
            ops.push(op1)
          end
        end
        if op2 != '-'
          if op2 == '_'
            ops.push('reg')
          else
            ops.push(op2)
          end
        end
        if op3 != '-'
          if op3 == '_'
            ops.push('reg')
          else
            ops.push(op3)
          end
        end
        s[sinsn] = { insn: insn, op1: ops[0], op2: ops[1], op3: ops.length == 3 ? ops[2] : nil, sinsn: sinsn }
      elsif /^(?<insn>[a-z]+)\s*\(\s*(?<op1>(\-|_|[a-z]+))\s*,\s*(?<op2>(\-|_|[a-z]+))\s*\)\s*:\s*(?<sinsn>[a-z]+)\s*/ =~ line
        s[sinsn] = { insn: insn, op1: op1, op2: op2, op3: nil, sinsn: sinsn }
      else
        puts("Line " + lineno.to_s + ": Invalid instruction definition -- " + line)
      end
    end
  end
  s
end

if __FILE__ == $0
  option={}
  OptionParser.new do |opt|
    opt.on('-s', '--super-insn=SUPERINSNSPEC',
           'Generate super instructions') {|v| option[:s] = v}
    opt.on('-t', '--opt-type=OPTTYPE',
           'Generate super instructions') {|v| option[:t] = v.to_i}
    opt.parse!(ARGV)
  end
  superinsn_spec = {}
  unless option[:s].nil?
    superinsn_spec = load_suerinsnspec(option[:s])
  end
  main(superinsn_spec, option[:t])
end
