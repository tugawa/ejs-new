/*
 * 15.5.4.15 String.prototype.substring(start, end)
 */
String.prototype.substring = function(start, end) {
    if (!(start === start))  /* is start NaN? */
	start = 0;
    if (!(end === end))
	end = 0;
    if (end < start) {
	var tmp = end;
	end = start;
	start = tmp;
    }
    if (start < 0)
	start = 0;
    if (this.length < end)
	end = this.length;
    substr = "";
    for (var i = start; i < end; i++)
	substr += this.charAt(i);
    return substr;
};
