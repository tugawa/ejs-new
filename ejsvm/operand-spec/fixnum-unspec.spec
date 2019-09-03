add (-,fixnum,fixnum) accept
add (-,string,string) accept
add (-,_,_) unspecified
bitand (-,fixnum,fixnum) accept
bitand (-,_,_) unspecified
bitor (-,fixnum,fixnum) accept
bitor (-,_,_) unspecified
call (_,-) accept
div (-,fixnum,fixnum) accept
div (-,_,_) unspecified
eq (-,_,_) unspecified
equal (-,fixnum,fixnum) accept
equal (-,_,_) unspecified
getprop (-,_,_) accept
leftshift (-,fixnum,fixnum) accept
leftshift (-,_,_) unspecified
lessthan (-,fixnum,fixnum) accept
lessthan (-,_,_) unspecified
lessthanequal (-,fixnum,fixnum) accept
lessthanequal (-,_,_) unspecified
mod (-,fixnum,fixnum) accept
mod (-,_,_) unspecified
mul (-,fixnum,fixnum) accept
mul (-,_,_) unspecified
new (-,_) accept
rightshift (-,fixnum,fixnum) accept
rightshift (-,_,_) unspecified
setprop (_,_,_) accept
sub (-,fixnum,fixnum) accept
sub (-,_,_) unspecified
tailcall (_,-) accept
unsignedrightshift (-,fixnum,fixnum) accept
unsignedrightshift (-,_,_) unspecified
error (-,-) accept
unspecified(-,-,-) unspecified
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
jumpfalse(_) unspecified
jumptrue(special) accept
jumptrue(_) unspecified
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
end () accept
localret () accept
nop () accept
pophandler () accept
poplocal () accept
ret () accept
throw () accept
unknown () accept
