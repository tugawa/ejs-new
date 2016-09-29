Array.prototype.sort = function(compf) {
	var element = this;
	if (compf === undefined) {
	  compf = function(a, b){
	    a = a.toString();
	    b = b.toString();
	    if (a < b) return -1;
	    if (a > b) return 1;
	    return 0;
	  }
	}
	for (var i=0;i<element.length-1;i++) {
	  for (var j=element.length-1;j>i;j--) {	
	    if (compf(element[j-1], element[j]) > 0) {
	      var swap = element[j];
	      element[j] = element[j-1];
	      element[j-1] = swap;
	    }
	  }
	}
	return element;
};

//var a = [3, 5, 3, 1, 8];

