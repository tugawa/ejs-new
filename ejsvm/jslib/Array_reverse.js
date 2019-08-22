/*
*  ECMA-262 5.1 Edition
*  15.4.4.7 Array.prototype.push ( [ item1 [ , item2 [ , … ] ] ] )
*/
Array.prototype.reverse = function() {
  var
    a = this,
    count = 1;
  for (var i=0; i<a.length/2; i++) {
    var swap = a[i];
    a[i] = a[a.length-count]
    a[a.length-count] = swap;
    count++;
  }
  return a;
}
 
var a = [2, "a"];
a.reverse();
console.log(a)

