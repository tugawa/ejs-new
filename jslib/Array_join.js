Array.prototype.join = function(separator) {
    var aWord = this;
    var rWord = "";
    if (separator === undefined)
      separator = ",";
    for (var i=0;i<aWord.length;i++) {
      rWord = rWord + aWord[i];
      if (i < aWord.length - 1)
        rWord = rWord + separator;
    }
    return rWord;
}




