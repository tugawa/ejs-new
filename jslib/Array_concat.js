/*
*  ECMA-262 5.1 Edition
*	15.4.4.4 Array.prototype.concat ( [ item1 [ , item2 [ , … ] ] ] )
*/
Array.prototype.concat = function(){
	var o = this,
			no = [],
			len = this.length,
			count = 0;
      
	for (var i=0; i<o.length; i++)
  	no[i] = o[i]
	
	for (var i=0; i<arguments.length; i++) {
  	if (arguments[i].length != undefined) {
			for (var j=0; j<arguments[i].length; j++) {
  			no[len + count] = arguments[i][j];
    		count++;
  		}
    }
    else {
    	no[len + count] = arguments[i];
    	count++;
    }
	}

	return no;
}


var a = [1, 2, 3],
		b = [4, 5, 6],
    c = [7, 8, 9]
console.time();
for (var i=0;i<10000;i++)
var a = a.concat(b);
console.timeEnd();
console.log(a)

