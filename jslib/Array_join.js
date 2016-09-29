Array.prototype.join2 = function(word) {
    var aWord = this;
    var rWord = "";
    if (word === undefined) {
       for (var i=0;i<aWord.length;i++) {
	        rWord = rWord + aWord[i];
             if (i < aWord.length - 1)
                rWord = rWord + ",";
       } 
   } else {
	     for (var i=0;i<aWord.length;i++) {
	        rWord = rWord + aWord[i];
             if (i < aWord.length - 1)
      	        rWord = rWord + word;
     }
   }
    return rWord;
}



