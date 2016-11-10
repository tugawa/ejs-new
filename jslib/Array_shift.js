/*
*  ECMA-262 5.1 Edition
*  15.4.4.7 Array.prototype.push ( [ item1 [ , item2 [ , … ] ] ] )
*/
Array.prototype.shift = function() {
    var a = this[0];
    for (var i=0; i<this.length; i++)
    	this[i] = this[i+1];
    delete this[this.length-1]
    return a;
}
 
var a = [1, 2, 3, 4];
var a2 = a.shift();
console.log(a);
console.log(a2);