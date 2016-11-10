/*
*  ECMA-262 5.1 Edition
*  15.4.4.7 Array.prototype.push ( [ item1 [ , item2 [ , … ] ] ] )
*/
Array.prototype.pop2 = function() {
	if (this.length === undefined)
    	return undefined;
    else {
    	var a = this[this.length - 1];
    	this.splice(this.length-1, 1);
   	 	return a;
    }
}
 
var a = [1, 2];
var a2 = a.pop2();
console.log(a)
console.log(a2);
