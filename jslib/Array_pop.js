Array.prototype.pop2 = function() {
    var a = this[this.length - 1];
    this.splice(this.length-1, 1);
    return a;
}
 
var a = ["a", "b", "c"];
var a2 = a.pop2();
console.log(a)
console.log(a2);
