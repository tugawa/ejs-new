/*
*  ECMA-262 5.1 Edition
*  15.4.4.7 Array.prototype.push ( [ item1 [ , item2 [ , … ] ] ] )
*/
Array.prototype.push = function(){
	var elements = [];
	elements = this;
	var elmlen = elements.length;
 // console.log(arguments.length)
 	for (var i=0; i<arguments.length; i++)
		elements[elmlen+i] = arguments[i];
	return elements.length
}

var a = [1, 3, 5];
var na = a.push(1, 2, "aaaa")
console.log(na);
console.log(a)
