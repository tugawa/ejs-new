Array.prototype.sort2 = function(compf) {
	var element = this;
	if (compf === undefined) {
	    compf = function(a, b) {
	    a = a.toString();
	    b = b.toString();
	    if (a < b) return -1;
	    if (a > b) return 1;
	    return 0;
	  }
	}
	for (var i=0;i<element.length-1;i++) {
	  for (var j=element.length-1;j>i;j--) {	
	    	 if (element[j-1] === undefined && element[j] != undefined) {
	      var swap = element[j];
	      element[j] = element[j-1];
	      element[j-1] = swap;
	    }
	    else if (element[j-1] != undefined  && element[j] != undefined) {
	    	if (compf(element[j-1], element[j]) > 0) {
	    		var swap = element[j];
	     	 	element[j] = element[j-1];
	      		element[j-1] = swap;
	    	}
	 		}
	  }
	}
	return element;
};

var a = [];
a[8] = 1;
a[5] = 5 ;
a[10] = 11;
var as = a.sort2();
console.log(as);
