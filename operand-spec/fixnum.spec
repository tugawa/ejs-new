add (-,fixnum,fixnum) accept
add (-,string,string) accept
add (-,_,_) error
bitand (-,fixnum,fixnum) accept
bitand (-,_,_) error
bitor (-,fixnum,fixnum) accept
bitor (-,_,_) error
call (_,-) accept
div (-,fixnum,fixnum) accept
div (-,_,_) error
eq (-,_,_) error
equal (-,fixnum,fixnum) accept
equal (-,_,_) error
getprop (-,_,_) accept
leftshift (-,fixnum,fixnum) accept
leftshift (-,_,_) error
lessthan (-,fixnum,fixnum) accept
lessthan (-,_,_) error
lessthanequal (-,fixnum,fixnum) accept
lessthanequal (-,_,_) error
mod (-,fixnum,fixnum) accept
mod (-,_,_) error
mul (-,fixnum,fixnum) accept
mul (-,_,_) error
new (-,_) accept
rightshift (-,fixnum,fixnum) accept
rightshift (-,_,_) error
setprop (_,_,_) accept
sub (-,fixnum,fixnum) accept
sub (-,_,_) error
tailcall (_,-) accept
unsignedrightshift (-,fixnum,fixnum) accept
unsignedrightshift (-,_,_) error
error(-,-,-) unspecified
fixnum(-,-) accept
geta(-) accept
getarg(-,-,-) accept
geterr(-) unspecified
getglobal(-,string) accept
getglobal(-,_) unspecified
getglobalobj(-) accept
getlocal(-,-,-) accept
instanceof(-,_,_) accept
isobject(-,_) accept
isundef(-,_) accept
jump(-) accept
jumpfalse(special) accept
jumpfalse(_) error
jumptrue(special) accept
jumptrue(_) error
localcall(-) unspecified
makeclosure(-,-) accept
makeiterator(-,_) accept
move(-,_) accept
newframe(-,-) accept
nextpropnameidx(-,_) accept
not(-,_) accept
number(-) accept
pushhandler(-) unspecified
seta(_) accept
setarg(-,-,_) accept
setarray(_,-,_) accept
setfl(-) accept
setglobal(string,_) accept
setglobal(_,_) unspecified
setlocal(-,-,_) accept
specconst(-) accept
typeof(-,-) accept
