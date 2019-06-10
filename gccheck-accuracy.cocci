// ---------------- preparation ----------------
@initialize:python@
@@

import re

// ---------------- costomize by user ----------------
// User must define type with pointer to object and ADD function, REMOVE function 
// --------------------------------

// type with pointer to object

// --- type list ---
type_list = ["JSValue", "Object*", "FunctionFrame*", "HiddenClass*", "StrCons*",
             "ArrayCell*", "BoxedCell*", "BuiltinCell*", "FlonumCell*", "FunctionCell*",
             "IteratorCell*", "RegexpCell*", "StringCell*",
             "HashCell**"]
// --- akazawa GC_ROOT type list ---
//type_list = ["TmpJSVector*", "JSValue", "FunctionTable*", "FunctionFrame*", "ByteArray*",
//             "HiddenClass*", "Object*", "HashIterator*", "HashKey", "StringCell*"]

// ADD function
add_function = "gc_push_checked"

// REMOVE function
remove_function = "gc_pop_checked"

// specific type that many allocation function have as argument 
type_context = "Context*"

// allocation function without specific type as argument 
allocation = []


// ---------------- end ----------------
// --------------------------------

// element is [file-name, start-line, var-name, out-put-number, end-line, kind]
out_put_list = []
kind = ["miss", "warning"]
pattern = ["MissingAdd", "MissingRemove", "MissingInit",
           "InvalidType", "AlreadyAdd",
           "UnnecessaryRemove", "AlreadyRemove", "RemoveToAlloc"]

def allocation_check(alloc_func):
	str = alloc_func
	if str in allocation:
		return True
	else:
		return False

def type_check(t):
	str = re.sub('\s', "", t)
	if str in type_list:
		return True
	else:
		return False

@script:python pre@
add;
remove;
Context;
@@

coccinelle.add = add_function
coccinelle.remove = remove_function
coccinelle.Context = cocci.make_type(type_context)

// ---------------- main ----------------
// --------------------------------------
// ---------------- check allocation ----------------
@alloc@
identifier falloc, stated_alloc:script:python(){allocation_check(stated_alloc)};
position falloc_p;
type pre.Context;
Context ctx;
@@

(
 falloc@falloc_p(..., ctx,...)
|
 (*falloc)@falloc_p(..., ctx,...)
|
 stated_alloc@falloc_p
)


@var depends on alloc exists@
identifier v, alloc_func = {alloc.falloc, alloc.stated_alloc};
expression e;
statement S;
position alloc.falloc_p, variable_p, start, end;
type T:script:python(){type_check(T)};
@@

{@start
... when != S
(
 T v@variable_p;
|
 T v@variable_p = e;
)
...
alloc_func@falloc_p
...
}@end


@variable depends on var exists@
identifier var.v, alloc_func = {alloc.falloc, alloc.stated_alloc};
expression e;
position alloc.falloc_p, use_p, var.start, var.end;
@@

{@start
...
alloc_func@falloc_p
... when != v = e;
    when any
v@use_p
... when any
}@end


@unuse_variable depends on variable@
identifier var.v, pre.add, pre.remove;
expression e;
position variable.use_p;
@@

(
 v@use_p = e;
|
 add(&v@use_p)
|
 remove(&v@use_p)
)


@check_rside depends on variable@
identifier var.v, alloc_func = {alloc.falloc, alloc.stated_alloc};
position alloc.falloc_p;
@@

v = <+... alloc_func@falloc_p ...+>;


@variable_param depends on alloc exists@
identifier v, f, alloc_func = {alloc.falloc, alloc.stated_alloc};
expression e;
position alloc.falloc_p, variable_p, use_p, start, end;
type T:script:python(){type_check(T)};
@@

f(..., T v@variable_p,...) {@start
...
alloc_func@falloc_p
... when != v = e;
    when any
v@use_p
... when any
}@end


@unuse_variable_param depends on variable_param@
identifier variable_param.v, pre.add, pre.remove;
expression e;
position variable_param.use_p;
@@

(
 v@use_p = e;
|
 add(&v@use_p)
|
 remove(&v@use_p)
)


@check_rside_param depends on variable_param@
identifier variable_param.v, alloc_func = {alloc.falloc, alloc.stated_alloc};
position alloc.falloc_p;
@@

v = <+... alloc_func@falloc_p ...+>;


// ---------------- MissingAdd ----------------
@MissingAdd depends on variable && !unuse_variable && !check_rside exists@
identifier pre.add, var.v, alloc_func = {alloc.falloc, alloc.stated_alloc};
expression e;
position alloc.falloc_p, var.start, var.end, var.variable_p;
type var.T;
@@

{@start
...
(
 T v@variable_p;
|
 T v@variable_p = e;
)
... when != add(&v)
alloc_func@falloc_p
...
}@end




@script:python depends on MissingAdd@
v << var.v;
line << var.variable_p;
alloc_line << alloc.falloc_p;
@@
tmp = [line[0].file, int(line[0].line), v, 0, int(alloc_line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


@MissingAdd_param depends on variable_param && !unuse_variable_param && !check_rside_param exists@
identifier variable_param.v, variable_param.f, pre.add, alloc_func = {alloc.falloc, alloc.stated_alloc};
position alloc.falloc_p, variable_param.variable_p;
type variable_param.T;
@@

f(..., T v@variable_p,...) {
... when != add(&v)
alloc_func@falloc_p
...
}


@script:python depends on MissingAdd_param@
v << variable_param.v;
line << variable_param.variable_p;
alloc_line << alloc.falloc_p;
@@
tmp = [line[0].file, int(line[0].line), v, 0, int(alloc_line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)

// ---------------- MissingRemove ----------------
@add_position@
identifier v, pre.add;
position add_p;
@@

add@add_p(&v)


@MissingRemove_return depends on add_position exists@
identifier pre.add, pre.remove, add_position.v;
statement S;
expression e1, e2;
position add_position.add_p, return_p;
type T:script:python(){type_check(T)};
@@

{
... when != S
(
 T v;
|
 T v = e1;
)
...
add@add_p(&v)
... when != remove(&v)
(
 return@return_p;
|
 return@return_p e2;
)
...
}


@script:python depends on MissingRemove_return@
v << add_position.v;
line << MissingRemove_return.return_p;
add_line << add_position.add_p;
@@
tmp = [line[0].file, int(line[0].line), v, 1, int(add_line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


@MissingRemove_param_return depends on add_position exists@
identifier pre.add, pre.remove, add_position.v, f;
expression e;
position add_position.add_p, return_p;
type T:script:python(){type_check(T)};
@@

f(..., T v,...){
...
add@add_p(&v)
... when != remove(&v)
(
 return@return_p;
|
 return@return_p e;
)
...
}


@script:python depends on MissingRemove_param_return@
v << add_position.v;
line << MissingRemove_param_return.return_p;
add_line << add_position.add_p;
@@
tmp = [line[0].file, int(line[0].line), v, 1, int(add_line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


@MissingRemove depends on add_position exists@
identifier pre.add, pre.remove, add_position.v;
statement S;
expression e1, e2;
position add_position.add_p, ret;
type T:script:python(){type_check(T)};
@@

{
... when != S
(
 T v;
|
 T v = e1;
)
...
add@add_p(&v)
... when != remove(&v)
    when != return;
    when != return e2;
}@ret


@script:python depends on MissingRemove@
v << add_position.v;
line << MissingRemove.ret;
add_line << add_position.add_p;
@@
tmp = [line[0].file, int(line[0].line), v, 1, int(add_line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


@MissingRemove_param depends on add_position exists@
identifier pre.add, pre.remove, add_position.v, f;
expression e;
position add_position.add_p, ret;
type T:script:python(){type_check(T)};
@@

f(..., T v,...){
...
add@add_p(&v)
... when != remove(&v)
    when != return;
    when != return e;
}@ret


@script:python depends on MissingRemove_param@
v << add_position.v;
line << MissingRemove_param.ret;
add_line << add_position.add_p;
@@
tmp = [line[0].file, int(line[0].line), v, 1, int(add_line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


// ---------------- MissingInit ----------------
@MissingInit depends on var exists@
identifier var.v, pre.add, alloc_func = {alloc.falloc, stated_alloc};
expression e1, e2;
position alloc.falloc_p, var.variable_p, var.start, var.end;
type var.T;
@@

{@start
...
T v@variable_p;
... when != v = e1;
add(&v)
... when != v = e2;
    when any
alloc_func@falloc_p
...
}@end

@script:python depends on MissingInit@
v << var.v;
line << var.variable_p;
alloc_line << alloc.falloc_p;
@@
tmp = [line[0].file, int(line[0].line), v, 2, int(alloc_line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)
#print "file-name : %s,  line : %s,  warning : non-init,  var-name : %s" %(line[0].file, line[0].line, v)


// ---------------- InvalidType ----------------
@InvalidType depends on add_position@
identifier add_position.v;
expression e;
position variable_p;
type T:script:python(){type_check(T)};
@@

(
 T v@variable_p;
|
 T v@variable_p = e;
)


@InvalidType_param depends on add_position@
identifier add_position.v, f;
position variable_p;
type T:script:python(){type_check(T)};
@@

f(..., T v@variable_p,...) {...}


@script:python depends on add_position && !InvalidType && !InvalidType_param@
v << add_position.v;
line << add_position.add_p;
@@
tmp = [line[0].file, int(line[0].line), v, 3, -1, kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


// ---------------- AlreadyAdd ----------------
@AlreadyAdd depends on add_position exists@
identifier add_position.v, pre.add, pre.remove;
position add_position.add_p, second_add;
@@

add@add_p(&v)
... when != remove(&v)
add@second_add(&v)


@script:python depends on AlreadyAdd@
v << add_position.v;
line << AlreadyAdd.second_add;
first_add << add_position.add_p;
@@
tmp = [line[0].file, int(first_add[0].line), v, 4, int(line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


// ---------------- UnnecessaryRemove ----------------
@remove_position@
identifier v, pre.remove;
position remove_p;
@@

remove@remove_p(&v)


@UnnecessaryRemove depends on remove_position exists@
identifier pre.add, pre.remove, remove_position.v;
position remove_position.remove_p;
@@

... when != add(&v)
remove@remove_p(&v)


@script:python depends on UnnecessaryRemove@
v << remove_position.v;
line << remove_position.remove_p;
@@
tmp = [line[0].file, int(line[0].line), v, 5, -1, kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


// ---------------- AlreadyRemove ----------------
@AlreadyRemove depends on remove_position exists@
identifier remove_position.v, pre.add, pre.remove;
position remove_position.remove_p, second_remove;
@@

remove@remove_p(&v)
... when != add(&v)
remove@second_remove(&v)


@script:python depends on AlreadyRemove@
v << remove_position.v;
line << AlreadyRemove.second_remove;
first_remove << remove_position.remove_p;
@@
tmp = [line[0].file, int(first_remove[0].line), v, 6, int(line[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)

// ---------------- RemoveToAlloc ----------------
@RemoveToAlloc depends on remove_position exists@
identifier remove_position.v, pre.add, pre.remove, falloc, stated_alloc:script:python(){allocation_check(stated_alloc)};
expression e;
position remove_position.remove_p, alloc_p, use_p;
type pre.Context;
Context ctx;
@@

remove@remove_p(&v)
... when != add(&v)
    when any
(
 falloc@alloc_p(..., ctx,...)
|
 (*falloc)@alloc_p(..., ctx,...)
|
 stated_alloc@alloc_p
)
... when != v = e;
    when any
v@use_p
... when any


@unuse_RemoveToAlloc depends on RemoveToAlloc exists@
identifier remove_position.v, pre.add, pre.remove;
expression e;
position RemoveToAlloc.use_p;
@@

(
 v@use_p = e;
|
 add(&v@use_p)
|
 remove(&v@use_p)
)


@check_RemoveToAlloc depends on RemoveToAlloc@
identifier remove_position.v, alloc_function = {RemoveToAlloc.falloc, RemoveToAlloc.stated_alloc};
position RemoveToAlloc.alloc_p;
@@

v = <+... alloc_function@alloc_p ...+>;


@script:python depends on RemoveToAlloc && !unuse_RemoveToAlloc && !check_RemoveToAlloc@
v << remove_position.v;
line << remove_position.remove_p;
alloc_p << RemoveToAlloc.alloc_p;
@@
tmp = [line[0].file, int(line[0].line), v, 7, int(alloc_p[0].line), kind[0]]
if tmp not in out_put_list:
	out_put_list.append(tmp)


// --------------------------------
@finalize:python@
@@

def rowChange(row_list, row):
	for element in row_list:
		if element[0] == row:
			return element[1]
	return row


out_put_list.sort()
out_put_list_of_file = []
now_file = "init"
tmplist = []


if out_put_list != []:
	for element in out_put_list:
		file = element.pop(0)
		if now_file == "init":
			now_file = file
		elif now_file != file:
			out_put_list_of_file.append([now_file, tmplist])
			now_file = file
			del tmplist[:]
		tmplist.append(element)
	else:
		out_put_list_of_file.append([now_file, tmplist])

	for element in out_put_list_of_file:
		try:
			file = element[0]
			scanfile = open(file)
			data = scanfile.read()
			string = data.split("\n")
			p = re.compile(file.split("/")[-1])
			rowlist = list()
			line = 1

			for n, s in enumerate(string):
				if re.match('\s*#\s*[0-9]+\s".*"', s):
					if re.search(p, s):
						rowlist.append([n+1, line])
						line = int(re.search("[0-9]+", s).group())
						continue
				rowlist.append([n+1, line])
				line += 1
		except Exception as e:
			print (e)
		finally:
			scanfile.close()

		for out_put_info in element[1]:
			pat = pattern[out_put_info[2]]
			if pat in {pattern[0], pattern[2]}:
				print("%s | %s : %s -> var : %s,  decl_line : %d,  alloc_line : %s"
				%(file, out_put_info[4], pat, out_put_info[1],
				  rowChange(rowlist, out_put_info[0]), rowChange(rowlist, out_put_info[3])))
			elif pat == pattern[1]:
				print("%s | %s : %s -> var : %s,  end_line : %d,  ADD_line : %s"
				%(file, out_put_info[4], pat, out_put_info[1],
				  rowChange(rowlist, out_put_info[0]), rowChange(rowlist, out_put_info[3])))
			elif pat == pattern[3]:
				print("%s | %s : %s -> var : %s,  ADD_line : %d"
				%(file, out_put_info[4], pat, out_put_info[1],
				  rowChange(rowlist, out_put_info[0])))
			elif pat == pattern[4]:
				print("%s | %s : %s -> var : %s,  first_ADD_line : %d,  second_ADD_line : %s"
				%(file, out_put_info[4], pat, out_put_info[1],
				  rowChange(rowlist, out_put_info[0]), rowChange(rowlist, out_put_info[3])))
			elif pat == pattern[5]:
				print("%s | %s : %s -> var : %s,  REMOVE_line : %d"
				%(file, out_put_info[4], pat, out_put_info[1],
				  rowChange(rowlist, out_put_info[0])))
			elif pat == pattern[6]:
				print("%s | %s : %s -> var : %s,  first_REMOVE_line : %d,  second_REMOVE_line : %s"
				%(file, out_put_info[4], pat, out_put_info[1],
				  rowChange(rowlist, out_put_info[0]), rowChange(rowlist, out_put_info[3])))
			elif pat == pattern[7]:
				print("%s | %s : %s -> var : %s,  REMOVE_line : %d,  alloc_line : %s"
				%(file, out_put_info[4], pat, out_put_info[1],
				  rowChange(rowlist, out_put_info[0]), rowChange(rowlist, out_put_info[3])))
			else:
				print("unregisterated check pattern")
