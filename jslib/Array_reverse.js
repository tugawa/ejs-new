Array.prototype.reverse2 = function() {
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
a.reverse2();
console.log(a)

