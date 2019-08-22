Array.prototype.join = function(word) {
    var aWord = this;
    var rWord = "";
    if (word === ", ") {
			for (var i=0;i<aWord.length;i++) {
				rWord = rWord + aWord[i];
        if (i < aWord.length - 1)
      		rWord = rWord + ", ";
       }
    }
    else if (word === "と") {
			for (var i=0;i<aWord.length;i++) {
	   		rWord = rWord + aWord[i];
        if (i < aWord.length - 1)
      		rWord = rWord + "と";
      }
    } 
    else if (word === "") {
			for (var i=0;i<aWord.length;i++)
	    	rWord = rWord + aWord[i]; 
 
    } 
    else {
    	for (var i=0;i<aWord.length;i++) {
	   	 rWord = rWord + aWord[i];
       if (i < aWord.length - 1)
      		rWord = rWord + ",";
      }
    }
    return rWord;




