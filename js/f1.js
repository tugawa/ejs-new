f = function () {
  return g(99) + 100;
}
g = function(x) {
  print("ggggggg");
  return x + 20;
}
print(g(300));
print(f());
