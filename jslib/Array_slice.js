/*
*  ECMA-262 5.1 Edition
*  15.4.4.7 Array.prototype.push ( [ item1 [ , item2 [ , … ] ] ] )
*/
Array.prototype.slice2 = function(begin, end) {
  if (begin === undefined)
    begin = 0;
  else if (begin > this.length)
  	begin = this.length;
  else if (begin < 0)
  	begin = this.length + begin;
    
  if (end === undefined)
    end = this.length; 
  else if (end > this.length)
  	end = this.length;    
  else if (end < 0)
  	end = this.length + end;
    
  if (end <= begin) {
    var swap = end;
    end = begin;
    begin = swap
  }
  
	var 
   	i = begin,
    a = this,
    b = [],
    count = 0;
  
    for (i; i<end; i++) {
        b[count] = a[i]
        count+=1;
    }
    return b;
}
 
var a = ["Banana", "Orange", "Lemon", "Apple", "Mango"];
var as = a.slice2(1, 4);
console.log(as);
