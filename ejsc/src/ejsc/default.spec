%% instruction def
fixnum SMALLPRIMITIVE Register int
specconst SMALLPRIMITIVE Register int
string BIGPRIMITIVE LABELONLY
regexp BIGPRIMITIVE LABELONLY
number BIGPRIMITIVE Register Displacement
add THREEOP Register JSValue JSValue
sub THREEOP Register JSValue JSValue
mul THREEOP Register JSValue JSValue
div THREEOP Register JSValue JSValue
mod THREEOP Register JSValue JSValue
bitand THREEOP Register JSValue JSValue
bitor THREEOP Register JSValue JSValue
leftshift THREEOP Register JSValue JSValue
rightshift THREEOP Register JSValue JSValue
unsignedrightshift THREEOP Register JSValue JSValue
lessthan THREEOP Register JSValue JSValue
lessthanequal THREEOP Register JSValue JSValue
eq THREEOP Register JSValue JSValue
equal THREEOP Register JSValue JSValue
getarg THREEOP Register int Subscript
setarg THREEOP int Subscript JSValue
getprop THREEOP Register JSValue JSValue
setprop THREEOP JSValue JSValue JSValue
setarray THREEOP JSValue Subscript JSValue
getglobal TWOOP Register JSValue
setglobal TWOOP JSValue JSValue
instanceof THREEOP Register JSValue JSValue
move TWOOP Register JSValue
typeof TWOOP Register Register
not TWOOP Register JSValue
new TWOOP Register JSValue
isundef TWOOP Register JSValue
isobject TWOOP Register JSValue
setfl ONEOP int
seta ONEOP JSValue
geta ONEOP Register
geterr ONEOP Register
getglobalobj ONEOP Register
newframe TWOOP int int
ret ZEROOP
nop ZEROOP
jump UNCONDJUMP Displacement
jumptrue CONDJUMP JSValue Displacement
jumpfalse CONDJUMP JSValue Displacement
getlocal GETVAR Register int Subscript
setlocal SETVAR int Subscript JSValue
makeclosure MAKECLOSUREOP Register Subscript
makeiterator TWOOP Register JSValue
nextpropnameidx TWOOP Register JSValue
send CALLOP LABELONLY
newsend CALLOP LABELONLY
call CALLOP JSValue int
tailsend CALLOP LABELONLY
tailcall CALLOP JSValue int
pushhandler UNCONDJUMP Displacement
pophandler ZEROOP
throw ZEROOP
localcall UNCONDJUMP Displacement
localret ZEROOP
poplocal ZEROOP
error BIGPRIMITIVE Register Displacement
unknown UNKNOWNOP
end ZEROOP
%% superinstruction spec
