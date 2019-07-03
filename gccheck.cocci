// ---------------- preparation ----------------
@initialize:python@
@@

import re
import os.path

// ---------------- costomize by user ----------------
// User must define type with pointer to object and PUSH function, POP function
// --------------------------------

// type with pointer to object

// --- type list ---
type_list = ["JSValue", "Object*", "FunctionFrame*", "HiddenClass*", "StrCons*",
             "ArrayCell*", "BoxedCell*", "BuiltinCell*", "FlonumCell*", "FunctionCell*",
             "IteratorCell*", "RegexpCell*", "StringCell*",
             "HashCell**"]

// PUSH function
push_function = "gc_push_checked"

// POP function
pop_function = "gc_pop_checked"

// specific type that many allocation function have as argument 
type_context = "Context*"

// allocation function without specific type as argument 
allocation = []


// ---------------- end ----------------
// --------------------------------

// element is [file-name, start-line, var-name, out-put-number, end-line, kind]
// (file, start_line, end_line, var_name, pat_name)
report = []

def add_report(loc1, loc2, loc3, v, pattern):
        file = loc1[0].file
        line1 = get_src_line_no(loc1)
        line2 = get_src_line_no(loc2) if loc2 else -1
        line3 = get_src_line_no(loc3) if loc3 else -1
        e = (file, line1, v, pattern, line2, line3)
        if e not in report:
                report.append(e)

def is_alloc(fun):
        return fun in allocation

def is_pointer(t):
        return re.sub('\s', "", t) in type_list

def line_mapping(filename):
    mapping = []
    with open(filename) as f:
        pp_begin = None
        for (line_no, line) in enumerate(f.readlines()):
            line_no += 1  # convert to 1-origin
            m = re.match(r'^#\s*(\d+)\s+"(.*)".*', line)
            if m:
                # linemarker
                marker_line = int(m.group(1))
                marker_file = m.group(2)
                if pp_begin:
                    mapping.append((pp_begin, line_no, src_file, src_line))
                    pp_begin = None
                if marker_file == os.path.basename(filename):
                    pp_begin = line_no + 1
                    src_line = marker_line
                    src_file = marker_file
        if pp_begin:
            mapping.append((pp_begin, line_no + 1, src_file, src_line))
        elif len(mapping) == 0:
            mapping.append((1, line_no + 1, filename, 1))
    return mapping

line_mappings = {}
def get_src_line_no(pos):
    filename = pos[0].file
    line_no = int(pos[0].line)
    if not filename in line_mappings:
        line_mappings[filename] = line_mapping(filename)
    for (begin, end, src_file, src_line) in line_mappings[filename]:
        if begin <= line_no and line_no < end:
            d = line_no - begin
            return src_line + d
    return line_no


@script:python pre@
push;
pop;
Context;
@@
coccinelle.push = push_function
coccinelle.pop = pop_function
coccinelle.Context = cocci.make_type(type_context)

// ---------------- main ----------------
// --------------------------------------
// ---------------- check gc ----------------
@gc@
expression gc_fun;
position gc_p;
type pre.Context;
Context ctx;
@@

gc_fun@gc_p(..., ctx,...)


@decl@
identifier v;
expression e;
position decl_p;
type T:script:python(){is_pointer(T)};
@@

(
 T v@decl_p;
|
 T v@decl_p = e;
)


//@script:python depends on decl@
//v << decl.v;
//t << decl.T;
//decl_p << decl.decl_p;
//@@
//print("%s %s@%d" % (t, v, get_src_line_no(decl_p)))


@use depends on gc && decl@
identifier decl.v;
expression e, gc.gc_fun;
position gc.gc_p, use_p;
@@

gc_fun@gc_p
... when != v = e
    when exists    // neither gc_fun or v is allowed in between because "any" is not specified
v@use_p


@falseuse depends on use@
identifier decl.v, pre.push, pre.pop;
expression e;
position use.use_p;
@@

(
 v@use_p = e
|
 push(&v@use_p)
|
 pop(&v@use_p)
)

//@script:python depends on falseuse@
//v << decl.v;
//gc_p << gc.gc_p;
//fun << gc.gc_fun;
//use_p << use.use_p;
//@@
//print("NOTUSE %s: call %d (%s) use %d" % ( v, get_src_line_no(gc_p), fun, get_src_line_no(use_p)))

@immass depends on use@
identifier decl.v;
expression gc.gc_fun;
position gc.gc_p;
@@

v = <+... gc_fun@gc_p ...+>


//@script:python depends on immass@
//v << decl.v;
//gc_p << gc.gc_p;
//fun << gc.gc_fun;
//@@
//print("IMMASS %s: %d (%s)" % ( v, get_src_line_no(gc_p), fun))


@use_param depends on gc@
identifier v, f;
expression e, gc.gc_fun;
position gc.gc_p, decl_p, use_p, start_p, end_p;
type T:script:python(){is_pointer(T)};
@@

f(..., T v@decl_p,...) {@start_p
... when exists
gc_fun@gc_p
... when != v = e
    when != v        // find first use
    when exists,any  // any is to ignore second gc_fun and v occurrences.
v@use_p
... when exists,any
}@end_p


@falseuse_param depends on use_param@
identifier use_param.v, pre.push, pre.pop;
expression e;
position use_param.use_p;
@@

(
 v@use_p = e;
|
 push(&v@use_p)
|
 pop(&v@use_p)
)


@immass_param depends on use_param@
identifier use_param.v;
expression gc.gc_fun;
position gc.gc_p;
@@
v = <+... gc_fun@gc_p ...+>

// ---------------- Address ----------------
@Address depends on decl@
identifier decl.v, x;
position addr_p;
@@
x = &v@addr_p

@script:python depends on Address@
v << decl.v;
decl_loc << decl.decl_p;
addr_loc << Address.addr_p;
@@

add_report(decl_loc, addr_loc, None, v, "Address")


// ---------------- MissingPush ----------------
@MissingPush depends on use && !falseuse && !immass@
identifier pre.push, decl.v;
expression e, gc.gc_fun;
position gc.gc_p, decl.decl_p;
type decl.T;
@@
(
 T v@decl_p;
|
 T v@decl_p = e;
)
... when != push(&v)
    when exists
gc_fun@gc_p


@script:python depends on MissingPush@
v << decl.v;
decl_loc << decl.decl_p;
gc_loc << gc.gc_p;
use_loc << use.use_p;
fun << gc.gc_fun;
@@

add_report(decl_loc, gc_loc, use_loc, v, "missing push")


@MissingPush_param depends on use_param && !falseuse_param && !immass_param@
identifier use_param.v, use_param.f, pre.push;
expression gc.gc_fun;
position gc.gc_p, use_param.decl_p;
type use_param.T;
@@

f(..., T v@decl_p,...) {
... when != push(&v)
    when exists
gc_fun@gc_p
... when exists
}


@script:python depends on MissingPush_param@
v << use_param.v;
decl_loc << use_param.decl_p;
gc_loc << gc.gc_p;
use_loc << use_param.use_p;
@@
add_report(decl_loc, gc_loc, use_loc, v, "missing push")


// ---------------- MissingPop ----------------
@push_position@
identifier v, pre.push;
position push_p;
@@

push@push_p(&v)


@MissingPop_return depends on push_position@
identifier pre.push, pre.pop, push_position.v;
expression e1, e2;
position push_position.push_p, return_p;
type T:script:python(){is_pointer(T)};
@@

(
 T v;
|
 T v = e1;
)
... when exists // "any" is not needed because "push" is a specific occurrence
push@push_p(&v)
... when != pop(&v)
    when exists
(
 return@return_p;
|
 return@return_p e2;
)

@script:python depends on MissingPop_return@
v << push_position.v;
ret_loc << MissingPop_return.return_p;
push_loc << push_position.push_p;
@@
add_report(ret_loc, push_loc, None, v, "missing pop")


@MissingPop_param_return depends on push_position exists@
identifier pre.push, pre.pop, push_position.v, f;
expression e;
position push_position.push_p, return_p;
type T:script:python(){is_pointer(T)};
@@

f(..., T v,...){
...
push@push_p(&v)
... when != pop(&v)
(
 return@return_p;
|
 return@return_p e;
)
...
}


@script:python depends on MissingPop_param_return@
v << push_position.v;
ret_loc << MissingPop_param_return.return_p;
push_loc << push_position.push_p;
@@
add_report(ret_loc, push_loc, None, v, "MissignPop")


@MissingPop depends on push_position exists@
identifier pre.push, pre.pop, push_position.v;
expression e1, e2;
position push_position.push_p, ret;
type T:script:python(){is_pointer(T)};
@@

{
...
(
 T v;
|
 T v = e1;
)
...
push@push_p(&v)
... when != pop(&v)
    when != return;
    when != return e2;
}@ret


@script:python depends on MissingPop@
v << push_position.v;
ret_loc << MissingPop.ret;
push_loc << push_position.push_p;
@@
add_report(ret_loc, push_loc, None, v, "missing pop")

@MissingPop_param depends on push_position exists@
identifier pre.push, pre.pop, push_position.v, f;
expression e;
position push_position.push_p, ret;
type T:script:python(){is_pointer(T)};
@@

f(..., T v,...){
...
push@push_p(&v)
... when != pop(&v)
    when != return;
    when != return e;
}@ret


@script:python depends on MissingPop_param@
v << push_position.v;
ret_loc << MissingPop_param.ret;
push_loc << push_position.push_p;
@@
add_report(ret_loc, push_loc, None, v, "missing pop")


// ---------------- MissingInit ----------------
@MissingInit depends on gc && decl@
identifier decl.v, pre.push;
expression e1, e2, gc.gc_fun;
position gc.gc_p, decl.decl_p;
type decl.T;
@@

T v@decl_p;
... when != v = e1
    when exists
push(&v)
... when != v = e2
    when exists
gc_fun@gc_p

@script:python depends on MissingInit@
v << decl.v;
decl_loc << decl.decl_p;
gc_loc << gc.gc_p;
@@
add_report(decl_loc, gc_loc, None, v, "missing init")


// ---------------- InvalidType ----------------
@InvalidType depends on push_position@
identifier push_position.v;
expression e;
position decl_p;
type T:script:python(){is_pointer(T)};
@@

(
 T v@decl_p;
|
 T v@decl_p = e;
)


@InvalidType_param depends on push_position@
identifier push_position.v, f;
position decl_p;
type T:script:python(){is_pointer(T)};
@@

f(..., T v@decl_p,...) {...}


@script:python depends on push_position && !InvalidType && !InvalidType_param@
v << push_position.v;
push_loc << push_position.push_p;
@@
add_report(push_loc, None, None, v, "invalid type")


// ---------------- AlreadyPush ----------------
@AlreadyPush depends on push_position exists@
identifier push_position.v, pre.push, pre.pop;
position push_position.push_p, second_push;
@@

push@push_p(&v)
... when != pop(&v)
push@second_push(&v)


@script:python depends on AlreadyPush@
v << push_position.v;
push_loc << AlreadyPush.second_push;
first_loc << push_position.push_p;
@@
add_report(push_loc, first_loc, None, v, "double push")


// ---------------- UnnecessaryPop ----------------
@pop_position@
identifier v, pre.pop;
position pop_p;
@@

pop@pop_p(&v)


@UnnecessaryPop depends on pop_position exists@
identifier pre.push, pre.pop, pop_position.v;
position pop_position.pop_p;
@@

... when != push(&v)
pop@pop_p(&v)


@script:python depends on UnnecessaryPop@
v << pop_position.v;
rm_loc << pop_position.pop_p;
@@
add_report(rm_loc, None, None, v, "push pop mismatch")


// ---------------- AlreadyPop ----------------
@AlreadyPop depends on pop_position exists@
identifier pop_position.v, pre.push, pre.pop;
position pop_position.pop_p, second_pop;
@@

pop@pop_p(&v)
... when != push(&v)
pop@second_pop(&v)


@script:python depends on AlreadyPop@
v << pop_position.v;
rm_loc << AlreadyPop.second_pop;
first_loc << pop_position.pop_p;
@@
add_report(rm_loc, first_loc, None, v, "double pop")


// ---------------- PopToAlloc ----------------
@PopToAlloc depends on pop_position exists@
identifier pop_position.v, pre.push, pre.pop;
expression e, gc_fun;
position pop_position.pop_p, gc_p, use_p;
type pre.Context;
Context ctx;
@@

pop@pop_p(&v)
... when != push(&v)
    when any
 gc_fun@gc_p(..., ctx,...)
... when != v = e;
    when any
v@use_p
... when any


@unuse_PopToAlloc depends on PopToAlloc exists@
identifier pop_position.v, pre.push, pre.pop;
expression e;
position PopToAlloc.use_p;
@@

(
 v@use_p = e;
|
 push(&v@use_p)
|
 pop(&v@use_p)
)


@check_PopToAlloc depends on PopToAlloc@
identifier pop_position.v;
expression PopToAlloc.gc_fun;
position PopToAlloc.gc_p;
@@

v = <+... gc_fun@gc_p ...+>


@script:python depends on PopToAlloc && !unuse_PopToAlloc && !check_PopToAlloc@
v << pop_position.v;
rm_loc << pop_position.pop_p;
gc_loc << PopToAlloc.gc_p;
@@
add_report(rm_loc, gc_loc, None, v, "early pop")


// --------------------------------
@finalize:python@
@@

prev = None
for (file, start_line, var_name, pattern, end_line, use_line) in sorted(report):
        d = {"f": os.path.basename(file),
             "l1": start_line,
             "l2": end_line,
             "l3": use_line,
             "v": var_name,
             "p": pattern
        }

        if prev == (file, start_line, var_name, pattern):
                continue
        prev = (file, start_line, var_name, pattern)

        pfix = "%(f)s | miss : %(p)s -> var : %(v)s,  "%d

        print("%(p)s, %(f)s, %(l1)d, %(l2)d, %(l3)s, %(v)s" % d)

        # if pattern == "missing push":
        #         print(pfix + ("decl_line : %(l1)d,  gc_line : %(l2)d"%d))
        # elif pattern == "missing pop":
        #         print(pfix + ("end_line : %(l1)d,  PUSH_line : %(l2)d"%d))
        # elif pattern == "missing init":
        #         print(pfix + ("decl_line : %(l1)d,  gc_line : %(l2)d"%d))
        # elif pattern == "invalid type":
        #         print(pfix + ("PUSH_line : %(l1)d"%d))
        # elif pattern == "double push":
        #         print(pfix + ("first_PUSH_line : %(l1)d,  second_PUSH_line : %(l2)s"%d))
        # elif pattern == "push pop mismatch":
        #         print(pfix + ("POP_line : %(l1)d"%d))
        # elif pattern == "double pop":
        #         print(pfix + ("first_POP_line : %(l1)d,  second_POP_line : %(l2)d"%d))
        # elif pattern == "early pop":
        #         print(pfix + ("POP_line : %(l1)d,  gc_line : %(l2)d"%d))
        # else:
        #         print("Unknown pattern: " + pattern)
