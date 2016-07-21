f = function () {
  return g(99) + 100;
}
g = function(x) {
  print("ggggggg");
  return x + 20;
}
printv(g(300));
printv(f());
