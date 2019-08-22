Array.prototype.sort = function(temp) {
    var as = this; 
    while (true) {
      for (var i=0;i<as.length-1;i++) {
        if (temp == null) {
          if (as[i] > as[i+1]) {
            var swap = as[i];
            as[i] = as[i+1];
            as[i+1] = swap;
          }
        } else { 
          if (temp(as[i], as[i+1]) <= -1) {
            var swap = as[i];
            as[i] = as[i+1];
            as[i+1] = swap;
          }
          if (temp(as[i], as[i+1]) <= 1) {
            var swap = as[i+1];
            as[i+1] = as[i];
            as[i] = swap;
          }
          if (temp(as[i], as[i+1]) == 0) {}
        }
      }
      if (end(as)) break;
    }
  return as;
}

function end(as) {
  for (var i=0;i<as.length;i++) {
    if (as[i] > as[i+1]) {
      return false;
    }
  }
  return true;
}
