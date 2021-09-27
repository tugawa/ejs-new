Object.create = function(proto) {
    var ctor = function() {};
    ctor.prototype = proto;
    return new ctor();
};

Function.prototype.call = function(tis) {
    var a = [];
    for (var i = 1; i < arguments.length; i++)
	a[i-1] = arguments[i];
    this.apply(tis, a);
};

Array.prototype.forEach = function(f) {
    for (var i = 0; i < this.length; i++)
	f(this[i]);
}
// This code is derived from the SOM benchmarks, see AUTHORS.md file.
//
// Copyright (c) 2015-2016 Stefan Marr <git@stefan-marr.de>
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the 'Software'), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
'use strict';

function Benchmark() {}

Benchmark.prototype.innerBenchmarkLoop = function (innerIterations) {
  for (var i = 0; i < innerIterations; i++) {
    if (!this.verifyResult(this.benchmark())) {
      return false;
    }
  }
  return true;
};

Benchmark.prototype.benchmark = function () {
  throw "subclass responsibility";
};

Benchmark.prototype.verifyResult = function () {
  throw "subclass responsibility";
};
// This code is derived from the SOM benchmarks, see AUTHORS.md file.
//
// Copyright (c) 2015-2016 Stefan Marr <git@stefan-marr.de>
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the 'Software'), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
'use strict';

var performance = performance || {};
performance.now = (function() {
  return performance.now       ||
         performance.mozNow    ||
         performance.msNow     ||
         performance.oNow      ||
         performance.webkitNow ||
         Date.now;
})();

function Run(benchmarkSuite, name, numIterations, innerIterations) {

  var total = 0;

  function reportBenchmark() {
    print(name + ": iterations=" + numIterations +
      " average: " + Math.round(total / numIterations) + "us total: " + Math.round(total) + "us\n\n");
  }

  function printResult(runTime) {
    print(name + ": iterations=1 runtime: " + Math.round(runTime) + "us\n");
  }

  function measure(bench) {
    var startTime = performance.now();
    if (!bench.innerBenchmarkLoop(innerIterations)) {
      throw "Benchmark failed with incorrect result";
    }
    var diff = performance.now() - startTime;
    var runTime = diff * 1000;

    printResult(runTime);
    total += runTime;
  }

  function doRuns(bench) {
    for (var i = 0; i < numIterations; i++) {
      measure(bench);
    }
  }

  this.printTotal = function () {
    print("Total Runtime: " + total + "us\n");
  };

  this.runBenchmark = function () {
    print("Starting " + name + " benchmark ...\n");

    doRuns(new benchmarkSuite());

    reportBenchmark();
    print("\n");
  };
}


// This code is derived from the SOM benchmarks, see AUTHORS.md file.
//
// Copyright (c) 2015-2016 Stefan Marr <git@stefan-marr.de>
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the 'Software'), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
'use strict';

var INITIAL_SIZE = 10,
  INITIAL_CAPACITY = 16;

function Pair(key, val) {
  this.key   = key;
  this.value = val;
}

function Vector(size) {
  this.storage  = new Array(size === undefined ? 50 : size);
  this.firstIdx = 0;
  this.lastIdx  = 0;
}

Vector.with = function (elem) {
  var v = new Vector(1);
  v.append(elem);
  return v;
};

Vector.prototype.at = function (idx) {
  if (idx >= this.storage.length) {
    return null;
  }
  return this.storage[idx];
};

Vector.prototype.atPut = function (idx, val) {
  if (this.idx >= this.storage.length) {
    var newLength = this.storage.length;
    while (newLength <= idx) {
      newLength *= 2;
    }
    this.storage = this.storage.slice();
    this.storage.length = newLength;
  }
  this.storage[idx] = val;
  if (this.lastIdx < idx + 1) {
    this.lastIdx = idx + 1;
  }
};

Vector.prototype.append = function (elem) {
  if (this.lastIdx >= this.storage.length) {
    // Copy storage to comply with rules, but don't extend storage
    var newLength = this.storage.length * 2;
    this.storage = this.storage.slice();
    this.storage.length = newLength;
  }

  this.storage[this.lastIdx] = elem;
  this.lastIdx += 1;
};

Vector.prototype.isEmpty = function () {
  return this.lastIdx === this.firstIdx;
};

Vector.prototype.forEach = function (fn) {
  for (var i = this.firstIdx; i < this.lastIdx; i++) {
    fn(this.storage[i]);
  }
};

Vector.prototype.hasSome = function(fn) {
  for (var i = this.firstIdx; i < this.lastIdx; i++) {
    if (fn(this.storage[i])) {
      return true;
    }
  }
  return false;
};

Vector.prototype.getOne = function (fn) {
  for (var i = this.firstIdx; i < this.lastIdx; i++) {
    var e = this.storage[i];
    if (fn(e)) {
      return e;
    }
  }
  return null;
};

Vector.prototype.removeFirst = function () {
  if (this.isEmpty()) {
    return null;
  }
  this.firstIdx++;
  return this.storage[this.firstIdx - 1];
};

Vector.prototype.remove = function (obj) {
  var newArray = new Array(this.capacity());
  var newLast = 0;
  var found = false;

  this.forEach(function (it) {
    if (it == obj) {
      found = true;
    } else {
      newArray[newLast] = it;
      newLast++;
    }
  });

  this.storage  = newArray;
  this.lastIdx  = newLast;
  this.firstIdx = 0;
  return found;
};

Vector.prototype.removeAll = function () {
  this.firstIdx = 0;
  this.lastIdx = 0;
  this.storage = new Array(this.storage.length);
};

Vector.prototype.size = function () {
  return this.lastIdx - this.firstIdx;
};

Vector.prototype.capacity = function () {
  return this.storage.length;
};

Vector.prototype.sortRange = function (i, j, compare) {
  if (!compare) {
    this.defaultSort(i, j);
  }

  var n = j + 1 - i;
  if (n <= 1) {
    return;
  }

  var di = this.storage[i];
  var dj = this.storage[j];

  if (compare(di, dj)) {
    this.swap(this.storage, i, j);
    var tt = di;
    di = dj;
    dj = tt;
  }

  if (n > 2) {
    var ij = (i + j) / 2;
    var dij = this.storage[ij];

    if (compare(di, dij) <= 0) {
      if (!compare(dij, dj)) {
        this.swap(this.storage, j, ij);
        dij = dj;
      }
    } else {
      this.swap(this.storage, i, ij);
      dij = di;
    }

    if (n > 3) {
      var k = i;
      var l = j - 1;

      while (true) {
        while (k <= l && compare(dij, this.storage[l])) {
          l -= 1;
        }

        k += 1;
        while (k <= l && compare(this.storage[k], dij)) {
          k += 1;
        }

        if (k > l) {
          break;
        }
        this.swap(this.storage, k, l);
      }
      var c = null; // never used
      this.sort(i, l, c);
      this.sort(k, j, c);
    }
  }
};

Vector.prototype.sort = function(compare) {
  if (this.size() > 0) {
    this.sortRange(this.firstIdx, this.lastIdx - 1, compare);
  }
};

function Set(size) {
  this.items = new Vector(size === undefined ? INITIAL_SIZE : size);
}

Set.prototype.size = function () {
  return this.items.size();
};

Set.prototype.forEach = function (fn) {
  this.items.forEach(fn);
};

Set.prototype.hasSome = function (fn) {
  return this.items.hasSome(fn);
};

Set.prototype.getOne = function (fn) {
  return this.items.getOne(fn);
};

Set.prototype.add = function (obj) {
  if (!this.contains(obj)) {
    this.items.append(obj);
  }
};

Set.prototype.contains = function (obj) {
  return this.hasSome(function (e) { return e == obj; } );
};

Set.prototype.removeAll = function () {
  this.items.removeAll();
};

Set.prototype.collect = function (fn) {
  var coll = new Vector();

  this.forEach(function (e) {
    coll.append(fn(e));
  });

  return coll;
};

function IdentitySet(size) {
  Set.call(this, size === undefined ? INITIAL_SIZE : size);
}
IdentitySet.prototype = Object.create(Set.prototype);

IdentitySet.prototype.contains = function (obj) {
  return this.hasSome(function (e) { return e == obj; });
};

function DictEntry(hash, key, value, next) {
  this.hash  = hash;
  this.key   = key;
  this.value = value;
  this.next  = next;
}

DictEntry.prototype.match = function(hash, key) {
  return this.hash == hash && key == this.key;
};

function Dictionary (size) {
  this.buckets = new Array(size === undefined ? INITIAL_CAPACITY : size);
  this.size_ = 0;
}

function hash(key) {
  if (!key) {
    return 0;
  }
  var hash = key.customHash();
  return hash ^ hash >>> 16;
}

Dictionary.prototype.size = function () {
  return this.size_;
};

Dictionary.prototype.isEmpty = function () {
  return this.size_ === 0;
};

Dictionary.prototype.getBucketIdx = function (hash) {
  return (this.buckets.length - 1) & hash;
};

Dictionary.prototype.getBucket = function (hash) {
  return this.buckets[this.getBucketIdx(hash)];
};

Dictionary.prototype.at = function (key) {
  var hash_ = hash(key),
    e = this.getBucket(hash_);

  while (e) {
    if (e.match(hash_, key)) {
      return e.value;
    }
    e = e.next;
  }
  return null;
};

Dictionary.prototype.containsKey = function (key) {
  var hash_ = hash(key),
    e = this.getBucket(hash_);

  while (e) {
    if (e.match(hash_, key)) {
      return true;
    }
    e = e.next;
  }
  return false;
};

Dictionary.prototype.atPut = function (key, value) {
  var hash_ = hash(key),
    i = this.getBucketIdx(hash_),
    current = this.buckets[i];

  if (!current) {
    this.buckets[i] = this.newEntry(key, value, hash_);
    this.size_ += 1;
  } else {
    this.insertBucketEntry(key, value, hash_, current);
  }

  if (this.size_ > this.buckets.length) {
    this.resize();
  }
};

Dictionary.prototype.newEntry = function (key, value, hash) {
  return new DictEntry(hash, key, value, null);
};

Dictionary.prototype.insertBucketEntry = function (key, value, hash, head) {
  var current = head;

  while (true) {
    if (current.match(hash, key)) {
      current.value = value;
      return;
    }
    if (!current.next) {
      this.size_ += 1;
      current.next = this.newEntry(key, value, hash);
      return;
    }
    current = current.next;
  }
};

Dictionary.prototype.resize = function () {
  var oldStorage = this.buckets;
  this.buckets = new Array(oldStorage.length * 2);
  this.transferEntries(oldStorage);
};

Dictionary.prototype.transferEntries = function (oldStorage) {
  for (var i = 0; i < oldStorage.length; ++i) {
    var current = oldStorage[i];
    if (current) {
      oldStorage[i] = null;

      if (!current.next) {
        this.buckets[current.hash & (this.buckets.length - 1)] = current;
      } else {
        this.splitBucket(oldStorage, i, current);
      }
    }
  }
};

Dictionary.prototype.splitBucket = function (oldStorage, i, head) {
  var loHead = null, loTail = null,
    hiHead = null, hiTail = null,
    current = head;

  while (current) {
    if ((current.hash & oldStorage.length) === 0) {
      if (!loTail) {
        loHead = current;
      } else {
        loTail.next = current;
      }
      loTail = current;
    } else {
      if (!hiTail) {
        hiHead = current;
      } else {
        hiTail.next = current;
      }
      hiTail = current;
    }
    current = current.next;
  }

  if (loTail) {
    loTail.next = null;
    this.buckets[i] = loHead;
  }
  if (hiTail) {
    hiTail.next = null;
    this.buckets[i + oldStorage.length] = hiHead;
  }
};

Dictionary.prototype.removeAll = function () {
  this.buckets = new Array(this.buckets.length);
  this.size_ = 0;
};

Dictionary.prototype.getKeys = function () {
  var keys = new Vector(this.size_);
  for (var i = 0; i < this.buckets.length; ++i) {
    var current = this.buckets[i];
    while (current) {
      keys.append(current.key);
      current = current.next;
    }
  }
  return keys;
};

Dictionary.prototype.getValues = function () {
  var values = new Vector(this.size_);
  for (var i = 0; i < this.buckets.length; ++i) {
    var current = this.buckets[i];
    while (current) {
      values.append(current.value);
      current = current.next;
    }
  }
  return values;
};

function DictIdEntry(hash, key, value, next) {
  DictEntry.call(this, hash, key, value, next);
}
DictIdEntry.prototype = Object.create(DictEntry.prototype);

DictIdEntry.prototype.match = function (hash, key) {
  return this.hash == hash && this.key === key;
};

function IdentityDictionary(size) {
  Dictionary.call(this, size === undefined ? INITIAL_CAPACITY : size);
}
IdentityDictionary.prototype = Object.create(Dictionary.prototype);

IdentityDictionary.prototype.newEntry = function (key, value, hash) {
  return new DictIdEntry(hash, key, value, null);
};

function Random() {
  this.seed = 74755;
}

Random.prototype.next = function () {
  this.seed = ((this.seed * 1309) + 13849) & 65535;
  return this.seed;
};

Array.prototype.fill = function (val) {
  var size = this.length;
  for (var i = 0; i < size; i += 1) {
    this[i] = val;
  }
  return this;
}

som = {
  Set: Set,
  IdentitySet: IdentitySet,
  Dictionary: Dictionary,
  IdentityDictionary: IdentityDictionary,
  Vector: Vector,
  Random: Random
}


// This code is derived from the SOM benchmark, which is itself based on the
// Java version.
//
// Copyright (c) 2015-2016 Stefan Marr <git@stefan-marr.de>
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the 'Software'), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
'use strict';

s0 = function() {
  return
"{\"head\":{\"requestCounter\":4},\"operations\":[[\"destroy\",\"w54\"]" +
",[\"set\",\"w2\",{\"activeControl\":\"w99\"}],[\"set\",\"w21\",{\"cust" +
"omVariant\":\"variant_navigation\"}],[\"set\",\"w28\",{\"customVariant" +
"\":\"variant_selected\"}],[\"set\",\"w53\",{\"children\":[\"w95\"]}],[" +
"\"create\",\"w95\",\"rwt.widgets.Composite\",{\"parent\":\"w53\",\"sty" +
"le\":[\"NONE\"],\"bounds\":[0,0,1008,586],\"children\":[\"w96\",\"w97\"" +
"],\"tabIndex\":-1,\"clientArea\":[0,0,1008,586]}],[\"create\",\"w96\"," +
"\"rwt.widgets.Label\",{\"parent\":\"w95\",\"style\":[\"NONE\"],\"bound" +
"s\":[10,30,112,26],\"tabIndex\":-1,\"customVariant\":\"variant_pageHea" +
"dline\",\"text\":\"TableViewer\"}],[\"create\",\"w97\",\"rwt.widgets.C";
}

s1 = function() {
  return
"omposite\",{\"parent\":\"w95\",\"style\":[\"NONE\"],\"bounds\":[0,61,1" +
"008,525],\"children\":[\"w98\",\"w99\",\"w226\",\"w228\"],\"tabIndex\"" +
":-1,\"clientArea\":[0,0,1008,525]}],[\"create\",\"w98\",\"rwt.widgets." +
"Text\",{\"parent\":\"w97\",\"style\":[\"LEFT\",\"SINGLE\",\"BORDER\"]," +
"\"bounds\":[10,10,988,32],\"tabIndex\":22,\"activeKeys\":[\"#13\",\"#2" +
"7\",\"#40\"]}],[\"listen\",\"w98\",{\"KeyDown\":true,\"Modify\":true}]" +
",[\"create\",\"w99\",\"rwt.widgets.Grid\",{\"parent\":\"w97\",\"style\"" +
":[\"SINGLE\",\"BORDER\"],\"appearance\":\"table\",\"indentionWidth\":0" +
",\"treeColumn\":-1,\"markupEnabled\":false}],[\"create\",\"w100\",\"rw" +
"t.widgets.ScrollBar\",{\"parent\":\"w99\",\"style\":[\"HORIZONTAL\"]}]";
}

s2 = function() {
  return
",[\"create\",\"w101\",\"rwt.widgets.ScrollBar\",{\"parent\":\"w99\",\"" +
"style\":[\"VERTICAL\"]}],[\"set\",\"w99\",{\"bounds\":[10,52,988,402]," +
"\"children\":[],\"tabIndex\":23,\"activeKeys\":[\"CTRL+#70\",\"CTRL+#7" +
"8\",\"CTRL+#82\",\"CTRL+#89\",\"CTRL+#83\",\"CTRL+#71\",\"CTRL+#69\"]," +
"\"cancelKeys\":[\"CTRL+#70\",\"CTRL+#78\",\"CTRL+#82\",\"CTRL+#89\",\"" +
"CTRL+#83\",\"CTRL+#71\",\"CTRL+#69\"]}],[\"listen\",\"w99\",{\"MouseDo" +
"wn\":true,\"MouseUp\":true,\"MouseDoubleClick\":true,\"KeyDown\":true}" +
"],[\"set\",\"w99\",{\"itemCount\":118,\"itemHeight\":28,\"itemMetrics\"" +
":[[0,0,50,3,0,3,44],[1,50,50,53,0,53,44],[2,100,140,103,0,103,134],[3," +
"240,180,243,0,243,174],[4,420,50,423,0,423,44],[5,470,50,473,0,473,44]";
}

s3 = function() {
  return
"],\"columnCount\":6,\"headerHeight\":35,\"headerVisible\":true,\"lines" +
"Visible\":true,\"focusItem\":\"w108\",\"selection\":[\"w108\"]}],[\"li" +
"sten\",\"w99\",{\"Selection\":true,\"DefaultSelection\":true}],[\"set\"" +
",\"w99\",{\"enableCellToolTip\":true}],[\"listen\",\"w100\",{\"Selecti" +
"on\":true}],[\"set\",\"w101\",{\"visibility\":true}],[\"listen\",\"w10" +
"1\",{\"Selection\":true}],[\"create\",\"w102\",\"rwt.widgets.GridColum" +
"n\",{\"parent\":\"w99\",\"text\":\"Nr.\",\"width\":50,\"moveable\":tru" +
"e}],[\"listen\",\"w102\",{\"Selection\":true}],[\"create\",\"w103\",\"" +
"rwt.widgets.GridColumn\",{\"parent\":\"w99\",\"text\":\"Sym.\",\"index" +
"\":1,\"left\":50,\"width\":50,\"moveable\":true}],[\"listen\",\"w103\"";
}

s4 = function() {
  return
",{\"Selection\":true}],[\"create\",\"w104\",\"rwt.widgets.GridColumn\"" +
",{\"parent\":\"w99\",\"text\":\"Name\",\"index\":2,\"left\":100,\"widt" +
"h\":140,\"moveable\":true}],[\"listen\",\"w104\",{\"Selection\":true}]" +
",[\"create\",\"w105\",\"rwt.widgets.GridColumn\",{\"parent\":\"w99\",\"" +
"text\":\"Series\",\"index\":3,\"left\":240,\"width\":180,\"moveable\":" +
"true}],[\"listen\",\"w105\",{\"Selection\":true}],[\"create\",\"w106\"" +
",\"rwt.widgets.GridColumn\",{\"parent\":\"w99\",\"text\":\"Group\",\"i" +
"ndex\":4,\"left\":420,\"width\":50,\"moveable\":true}],[\"listen\",\"w" +
"106\",{\"Selection\":true}],[\"create\",\"w107\",\"rwt.widgets.GridCol" +
"umn\",{\"parent\":\"w99\",\"text\":\"Period\",\"index\":5,\"left\":470";
}

s5 = function() {
  return
",\"width\":50,\"moveable\":true}],[\"listen\",\"w107\",{\"Selection\":" +
"true}],[\"create\",\"w108\",\"rwt.widgets.GridItem\",{\"parent\":\"w99" +
"\",\"index\":0,\"texts\":[\"1\",\"H\",\"Hydrogen\",\"Nonmetal\",\"1\"," +
"\"1\"],\"cellBackgrounds\":[null,null,null,[138,226,52,255],null,null]" +
"}],[\"create\",\"w109\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"" +
"index\":1,\"texts\":[\"2\",\"He\",\"Helium\",\"Noble gas\",\"18\",\"1\"" +
"],\"cellBackgrounds\":[null,null,null,[114,159,207,255],null,null]}],[" +
"\"create\",\"w110\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"ind" +
"ex\":2,\"texts\":[\"3\",\"Li\",\"Lithium\",\"Alkali metal\",\"1\",\"2\"" +
"],\"cellBackgrounds\":[null,null,null,[239,41,41,255],null,null]}],[\"";
}

s6 = function() {
  return
"create\",\"w111\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index" +
"\":3,\"texts\":[\"4\",\"Be\",\"Beryllium\",\"Alkaline earth metal\",\"" +
"2\",\"2\"],\"cellBackgrounds\":[null,null,null,[233,185,110,255],null," +
"null]}],[\"create\",\"w112\",\"rwt.widgets.GridItem\",{\"parent\":\"w9" +
"9\",\"index\":4,\"texts\":[\"5\",\"B\",\"Boron\",\"Metalloid\",\"13\"," +
"\"2\"],\"cellBackgrounds\":[null,null,null,[156,159,153,255],null,null" +
"]}],[\"create\",\"w113\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\"," +
"\"index\":5,\"texts\":[\"6\",\"C\",\"Carbon\",\"Nonmetal\",\"14\",\"2\"" +
"],\"cellBackgrounds\":[null,null,null,[138,226,52,255],null,null]}],[\"" +
"create\",\"w114\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index";
}

s7 = function() {
  return
"\":6,\"texts\":[\"7\",\"N\",\"Nitrogen\",\"Nonmetal\",\"15\",\"2\"],\"" +
"cellBackgrounds\":[null,null,null,[138,226,52,255],null,null]}],[\"cre" +
"ate\",\"w115\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":" +
"7,\"texts\":[\"8\",\"O\",\"Oxygen\",\"Nonmetal\",\"16\",\"2\"],\"cellB" +
"ackgrounds\":[null,null,null,[138,226,52,255],null,null]}],[\"create\"" +
",\"w116\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":8,\"t" +
"exts\":[\"9\",\"F\",\"Fluorine\",\"Halogen\",\"17\",\"2\"],\"cellBackg" +
"rounds\":[null,null,null,[252,233,79,255],null,null]}],[\"create\",\"w" +
"117\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":9,\"texts" +
"\":[\"10\",\"Ne\",\"Neon\",\"Noble gas\",\"18\",\"2\"],\"cellBackgroun";
}

s8 = function() {
  return
"ds\":[null,null,null,[114,159,207,255],null,null]}],[\"create\",\"w118" +
"\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":10,\"texts\"" +
":[\"11\",\"Na\",\"Sodium\",\"Alkali metal\",\"1\",\"3\"],\"cellBackgro" +
"unds\":[null,null,null,[239,41,41,255],null,null]}],[\"create\",\"w119" +
"\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":11,\"texts\"" +
":[\"12\",\"Mg\",\"Magnesium\",\"Alkaline earth metal\",\"2\",\"3\"],\"" +
"cellBackgrounds\":[null,null,null,[233,185,110,255],null,null]}],[\"cr" +
"eate\",\"w120\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\"" +
":12,\"texts\":[\"13\",\"Al\",\"Aluminium\",\"Poor metal\",\"13\",\"3\"" +
"],\"cellBackgrounds\":[null,null,null,[238,238,236,255],null,null]}],[";
}

s9 = function() {
  return
"\"create\",\"w121\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"ind" +
"ex\":13,\"texts\":[\"14\",\"Si\",\"Silicon\",\"Metalloid\",\"14\",\"3\"" +
"],\"cellBackgrounds\":[null,null,null,[156,159,153,255],null,null]}],[" +
"\"create\",\"w122\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"ind" +
"ex\":14,\"texts\":[\"15\",\"P\",\"Phosphorus\",\"Nonmetal\",\"15\",\"3" +
"\"],\"cellBackgrounds\":[null,null,null,[138,226,52,255],null,null]}]," +
"[\"create\",\"w123\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"in" +
"dex\":15,\"texts\":[\"16\",\"S\",\"Sulfur\",\"Nonmetal\",\"16\",\"3\"]" +
",\"cellBackgrounds\":[null,null,null,[138,226,52,255],null,null]}],[\"" +
"create\",\"w124\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index";
}

s10 = function() {
  return
"\":16,\"texts\":[\"17\",\"Cl\",\"Chlorine\",\"Halogen\",\"17\",\"3\"]," +
"\"cellBackgrounds\":[null,null,null,[252,233,79,255],null,null]}],[\"c" +
"reate\",\"w125\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\"" +
":17,\"texts\":[\"18\",\"Ar\",\"Argon\",\"Noble gas\",\"18\",\"3\"],\"c" +
"ellBackgrounds\":[null,null,null,[114,159,207,255],null,null]}],[\"cre" +
"ate\",\"w126\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":" +
"18,\"texts\":[\"19\",\"K\",\"Potassium\",\"Alkali metal\",\"1\",\"4\"]" +
",\"cellBackgrounds\":[null,null,null,[239,41,41,255],null,null]}],[\"c" +
"reate\",\"w127\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\"" +
":19,\"texts\":[\"20\",\"Ca\",\"Calcium\",\"Alkaline earth metal\",\"2\"";
}

s11 = function() {
  return
",\"4\"],\"cellBackgrounds\":[null,null,null,[233,185,110,255],null,nul" +
"l]}],[\"create\",\"w128\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\"" +
",\"index\":20,\"texts\":[\"21\",\"Sc\",\"Scandium\",\"Transition metal" +
"\",\"3\",\"4\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],n" +
"ull,null]}],[\"create\",\"w129\",\"rwt.widgets.GridItem\",{\"parent\":" +
"\"w99\",\"index\":21,\"texts\":[\"22\",\"Ti\",\"Titanium\",\"Transitio" +
"n metal\",\"4\",\"4\"],\"cellBackgrounds\":[null,null,null,[252,175,62" +
",255],null,null]}],[\"create\",\"w130\",\"rwt.widgets.GridItem\",{\"pa" +
"rent\":\"w99\",\"index\":22,\"texts\":[\"23\",\"V\",\"Vanadium\",\"Tra" +
"nsition metal\",\"5\",\"4\"],\"cellBackgrounds\":[null,null,null,[252,";
}

s12 = function() {
  return
"175,62,255],null,null]}],[\"create\",\"w131\",\"rwt.widgets.GridItem\"" +
",{\"parent\":\"w99\",\"index\":23,\"texts\":[\"24\",\"Cr\",\"Chromium\"" +
",\"Transition metal\",\"6\",\"4\"],\"cellBackgrounds\":[null,null,null" +
",[252,175,62,255],null,null]}],[\"create\",\"w132\",\"rwt.widgets.Grid" +
"Item\",{\"parent\":\"w99\",\"index\":24,\"texts\":[\"25\",\"Mn\",\"Man" +
"ganese\",\"Transition metal\",\"7\",\"4\"],\"cellBackgrounds\":[null,n" +
"ull,null,[252,175,62,255],null,null]}],[\"create\",\"w133\",\"rwt.widg" +
"ets.GridItem\",{\"parent\":\"w99\",\"index\":25,\"texts\":[\"26\",\"Fe" +
"\",\"Iron\",\"Transition metal\",\"8\",\"4\"],\"cellBackgrounds\":[nul" +
"l,null,null,[252,175,62,255],null,null]}],[\"create\",\"w134\",\"rwt.w";
}

s13 = function() {
  return
"idgets.GridItem\",{\"parent\":\"w99\",\"index\":26,\"texts\":[\"27\",\"" +
"Co\",\"Cobalt\",\"Transition metal\",\"9\",\"4\"],\"cellBackgrounds\":" +
"[null,null,null,[252,175,62,255],null,null]}],[\"create\",\"w135\",\"r" +
"wt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":27,\"texts\":[\"28" +
"\",\"Ni\",\"Nickel\",\"Transition metal\",\"10\",\"4\"],\"cellBackgrou" +
"nds\":[null,null,null,[252,175,62,255],null,null]}],[\"create\",\"w136" +
"\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":28,\"texts\"" +
":[\"29\",\"Cu\",\"Copper\",\"Transition metal\",\"11\",\"4\"],\"cellBa" +
"ckgrounds\":[null,null,null,[252,175,62,255],null,null]}],[\"create\"," +
"\"w137\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":29,\"t";
}

s14 = function() {
  return
"exts\":[\"30\",\"Zn\",\"Zinc\",\"Transition metal\",\"12\",\"4\"],\"ce" +
"llBackgrounds\":[null,null,null,[252,175,62,255],null,null]}],[\"creat" +
"e\",\"w138\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":30" +
",\"texts\":[\"31\",\"Ga\",\"Gallium\",\"Poor metal\",\"13\",\"4\"],\"c" +
"ellBackgrounds\":[null,null,null,[238,238,236,255],null,null]}],[\"cre" +
"ate\",\"w139\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":" +
"31,\"texts\":[\"32\",\"Ge\",\"Germanium\",\"Metalloid\",\"14\",\"4\"]," +
"\"cellBackgrounds\":[null,null,null,[156,159,153,255],null,null]}],[\"" +
"create\",\"w140\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index" +
"\":32,\"texts\":[\"33\",\"As\",\"Arsenic\",\"Metalloid\",\"15\",\"4\"]";
}

s15 = function() {
  return
",\"cellBackgrounds\":[null,null,null,[156,159,153,255],null,null]}],[\"" +
"create\",\"w141\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index" +
"\":33,\"texts\":[\"34\",\"Se\",\"Selenium\",\"Nonmetal\",\"16\",\"4\"]" +
",\"cellBackgrounds\":[null,null,null,[138,226,52,255],null,null]}],[\"" +
"create\",\"w142\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index" +
"\":34,\"texts\":[\"35\",\"Br\",\"Bromine\",\"Halogen\",\"17\",\"4\"],\"" +
"cellBackgrounds\":[null,null,null,[252,233,79,255],null,null]}],[\"cre" +
"ate\",\"w143\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":" +
"35,\"texts\":[\"36\",\"Kr\",\"Krypton\",\"Noble gas\",\"18\",\"4\"],\"" +
"cellBackgrounds\":[null,null,null,[114,159,207,255],null,null]}],[\"cr";
}

s16 = function() {
  return
"eate\",\"w144\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\"" +
":36,\"texts\":[\"37\",\"Rb\",\"Rubidium\",\"Alkali metal\",\"1\",\"5\"" +
"],\"cellBackgrounds\":[null,null,null,[239,41,41,255],null,null]}],[\"" +
"create\",\"w145\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index" +
"\":37,\"texts\":[\"38\",\"Sr\",\"Strontium\",\"Alkaline earth metal\"," +
"\"2\",\"5\"],\"cellBackgrounds\":[null,null,null,[233,185,110,255],nul" +
"l,null]}],[\"create\",\"w146\",\"rwt.widgets.GridItem\",{\"parent\":\"" +
"w99\",\"index\":38,\"texts\":[\"39\",\"Y\",\"Yttrium\",\"Transition me" +
"tal\",\"3\",\"5\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255" +
"],null,null]}],[\"create\",\"w147\",\"rwt.widgets.GridItem\",{\"parent";
}

s17 = function() {
  return
"\":\"w99\",\"index\":39,\"texts\":[\"40\",\"Zr\",\"Zirconium\",\"Trans" +
"ition metal\",\"4\",\"5\"],\"cellBackgrounds\":[null,null,null,[252,17" +
"5,62,255],null,null]}],[\"create\",\"w148\",\"rwt.widgets.GridItem\",{" +
"\"parent\":\"w99\",\"index\":40,\"texts\":[\"41\",\"Nb\",\"Niobium\",\"" +
"Transition metal\",\"5\",\"5\"],\"cellBackgrounds\":[null,null,null,[2" +
"52,175,62,255],null,null]}],[\"create\",\"w149\",\"rwt.widgets.GridIte" +
"m\",{\"parent\":\"w99\",\"index\":41,\"texts\":[\"42\",\"Mo\",\"Molybd" +
"enum\",\"Transition metal\",\"6\",\"5\"],\"cellBackgrounds\":[null,nul" +
"l,null,[252,175,62,255],null,null]}],[\"create\",\"w150\",\"rwt.widget" +
"s.GridItem\",{\"parent\":\"w99\",\"index\":42,\"texts\":[\"43\",\"Tc\"";
}

s18 = function() {
  return
",\"Technetium\",\"Transition metal\",\"7\",\"5\"],\"cellBackgrounds\":" +
"[null,null,null,[252,175,62,255],null,null]}],[\"create\",\"w151\",\"r" +
"wt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":43,\"texts\":[\"44" +
"\",\"Ru\",\"Ruthenium\",\"Transition metal\",\"8\",\"5\"],\"cellBackgr" +
"ounds\":[null,null,null,[252,175,62,255],null,null]}],[\"create\",\"w1" +
"52\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":44,\"texts" +
"\":[\"45\",\"Rh\",\"Rhodium\",\"Transition metal\",\"9\",\"5\"],\"cell" +
"Backgrounds\":[null,null,null,[252,175,62,255],null,null]}],[\"create\"" +
",\"w153\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":45,\"" +
"texts\":[\"46\",\"Pd\",\"Palladium\",\"Transition metal\",\"10\",\"5\"";
}

s19 = function() {
  return
"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null,null]}],[\"" +
"create\",\"w154\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index" +
"\":46,\"texts\":[\"47\",\"Ag\",\"Silver\",\"Transition metal\",\"11\"," +
"\"5\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null,null]" +
"}],[\"create\",\"w155\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"" +
"index\":47,\"texts\":[\"48\",\"Cd\",\"Cadmium\",\"Transition metal\",\"" +
"12\",\"5\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null," +
"null]}],[\"create\",\"w156\",\"rwt.widgets.GridItem\",{\"parent\":\"w9" +
"9\",\"index\":48,\"texts\":[\"49\",\"In\",\"Indium\",\"Poor metal\",\"" +
"13\",\"5\"],\"cellBackgrounds\":[null,null,null,[238,238,236,255],null";
}

s20 = function() {
  return
",null]}],[\"create\",\"w157\",\"rwt.widgets.GridItem\",{\"parent\":\"w" +
"99\",\"index\":49,\"texts\":[\"50\",\"Sn\",\"Tin\",\"Poor metal\",\"14" +
"\",\"5\"],\"cellBackgrounds\":[null,null,null,[238,238,236,255],null,n" +
"ull]}],[\"create\",\"w158\",\"rwt.widgets.GridItem\",{\"parent\":\"w99" +
"\",\"index\":50,\"texts\":[\"51\",\"Sb\",\"Antimony\",\"Metalloid\",\"" +
"15\",\"5\"],\"cellBackgrounds\":[null,null,null,[156,159,153,255],null" +
",null]}],[\"create\",\"w159\",\"rwt.widgets.GridItem\",{\"parent\":\"w" +
"99\",\"index\":51,\"texts\":[\"52\",\"Te\",\"Tellurium\",\"Metalloid\"" +
",\"16\",\"5\"],\"cellBackgrounds\":[null,null,null,[156,159,153,255],n" +
"ull,null]}],[\"create\",\"w160\",\"rwt.widgets.GridItem\",{\"parent\":";
}

s21 = function() {
  return
"\"w99\",\"index\":52,\"texts\":[\"53\",\"I\",\"Iodine\",\"Halogen\",\"" +
"17\",\"5\"],\"cellBackgrounds\":[null,null,null,[252,233,79,255],null," +
"null]}],[\"create\",\"w161\",\"rwt.widgets.GridItem\",{\"parent\":\"w9" +
"9\",\"index\":53,\"texts\":[\"54\",\"Xe\",\"Xenon\",\"Noble gas\",\"18" +
"\",\"5\"],\"cellBackgrounds\":[null,null,null,[114,159,207,255],null,n" +
"ull]}],[\"create\",\"w162\",\"rwt.widgets.GridItem\",{\"parent\":\"w99" +
"\",\"index\":54,\"texts\":[\"55\",\"Cs\",\"Caesium\",\"Alkali metal\"," +
"\"1\",\"6\"],\"cellBackgrounds\":[null,null,null,[239,41,41,255],null," +
"null]}],[\"create\",\"w163\",\"rwt.widgets.GridItem\",{\"parent\":\"w9" +
"9\",\"index\":55,\"texts\":[\"56\",\"Ba\",\"Barium\",\"Alkaline earth ";
}

s22 = function() {
  return
"metal\",\"2\",\"6\"],\"cellBackgrounds\":[null,null,null,[233,185,110," +
"255],null,null]}],[\"create\",\"w164\",\"rwt.widgets.GridItem\",{\"par" +
"ent\":\"w99\",\"index\":56,\"texts\":[\"57\",\"La\",\"Lanthanum\",\"La" +
"nthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null,null,null,[173,127,1" +
"68,255],null,null]}],[\"create\",\"w165\",\"rwt.widgets.GridItem\",{\"" +
"parent\":\"w99\",\"index\":57,\"texts\":[\"58\",\"Ce\",\"Cerium\",\"La" +
"nthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null,null,null,[173,127,1" +
"68,255],null,null]}],[\"create\",\"w166\",\"rwt.widgets.GridItem\",{\"" +
"parent\":\"w99\",\"index\":58,\"texts\":[\"59\",\"Pr\",\"Praseodymium\"" +
",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null,null,null,[173,";
}

s23 = function() {
  return
"127,168,255],null,null]}],[\"create\",\"w167\",\"rwt.widgets.GridItem\"" +
",{\"parent\":\"w99\",\"index\":59,\"texts\":[\"60\",\"Nd\",\"Neodymium" +
"\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null,null,null,[17" +
"3,127,168,255],null,null]}],[\"create\",\"w168\",\"rwt.widgets.GridIte" +
"m\",{\"parent\":\"w99\",\"index\":60,\"texts\":[\"61\",\"Pm\",\"Promet" +
"hium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null,null,null" +
",[173,127,168,255],null,null]}],[\"create\",\"w169\",\"rwt.widgets.Gri" +
"dItem\",{\"parent\":\"w99\",\"index\":61,\"texts\":[\"62\",\"Sm\",\"Sa" +
"marium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null,null,nu" +
"ll,[173,127,168,255],null,null]}],[\"create\",\"w170\",\"rwt.widgets.G";
}

s24 = function() {
  return
"ridItem\",{\"parent\":\"w99\",\"index\":62,\"texts\":[\"63\",\"Eu\",\"" +
"Europium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null,null," +
"null,[173,127,168,255],null,null]}],[\"create\",\"w171\",\"rwt.widgets" +
".GridItem\",{\"parent\":\"w99\",\"index\":63,\"texts\":[\"64\",\"Gd\"," +
"\"Gadolinium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null,n" +
"ull,null,[173,127,168,255],null,null]}],[\"create\",\"w172\",\"rwt.wid" +
"gets.GridItem\",{\"parent\":\"w99\",\"index\":64,\"texts\":[\"65\",\"T" +
"b\",\"Terbium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[null," +
"null,null,[173,127,168,255],null,null]}],[\"create\",\"w173\",\"rwt.wi" +
"dgets.GridItem\",{\"parent\":\"w99\",\"index\":65,\"texts\":[\"66\",\"";
}

s25 = function() {
  return
"Dy\",\"Dysprosium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[n" +
"ull,null,null,[173,127,168,255],null,null]}],[\"create\",\"w174\",\"rw" +
"t.widgets.GridItem\",{\"parent\":\"w99\",\"index\":66,\"texts\":[\"67\"" +
",\"Ho\",\"Holmium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[n" +
"ull,null,null,[173,127,168,255],null,null]}],[\"create\",\"w175\",\"rw" +
"t.widgets.GridItem\",{\"parent\":\"w99\",\"index\":67,\"texts\":[\"68\"" +
",\"Er\",\"Erbium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[nu" +
"ll,null,null,[173,127,168,255],null,null]}],[\"create\",\"w176\",\"rwt" +
".widgets.GridItem\",{\"parent\":\"w99\",\"index\":68,\"texts\":[\"69\"" +
",\"Tm\",\"Thulium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":[n";
}

s26 = function() {
  return
"ull,null,null,[173,127,168,255],null,null]}],[\"create\",\"w177\",\"rw" +
"t.widgets.GridItem\",{\"parent\":\"w99\",\"index\":69,\"texts\":[\"70\"" +
",\"Yb\",\"Ytterbium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\":" +
"[null,null,null,[173,127,168,255],null,null]}],[\"create\",\"w178\",\"" +
"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":70,\"texts\":[\"7" +
"1\",\"Lu\",\"Lutetium\",\"Lanthanide\",\"3\",\"6\"],\"cellBackgrounds\"" +
":[null,null,null,[173,127,168,255],null,null]}],[\"create\",\"w179\",\"" +
"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":71,\"texts\":[\"7" +
"2\",\"Hf\",\"Hafnium\",\"Transition metal\",\"4\",\"6\"],\"cellBackgro" +
"unds\":[null,null,null,[252,175,62,255],null,null]}],[\"create\",\"w18";
}

s27 = function() {
  return
"0\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":72,\"texts\"" +
":[\"73\",\"Ta\",\"Tantalum\",\"Transition metal\",\"5\",\"6\"],\"cellB" +
"ackgrounds\":[null,null,null,[252,175,62,255],null,null]}],[\"create\"" +
",\"w181\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":73,\"" +
"texts\":[\"74\",\"W\",\"Tungsten\",\"Transition metal\",\"6\",\"6\"],\"" +
"cellBackgrounds\":[null,null,null,[252,175,62,255],null,null]}],[\"cre" +
"ate\",\"w182\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":" +
"74,\"texts\":[\"75\",\"Re\",\"Rhenium\",\"Transition metal\",\"7\",\"6" +
"\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null,null]}]," +
"[\"create\",\"w183\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"in";
}

s28 = function() {
  return
"dex\":75,\"texts\":[\"76\",\"Os\",\"Osmium\",\"Transition metal\",\"8\"" +
",\"6\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null,null" +
"]}],[\"create\",\"w184\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\"," +
"\"index\":76,\"texts\":[\"77\",\"Ir\",\"Iridium\",\"Transition metal\"" +
",\"9\",\"6\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],nul" +
"l,null]}],[\"create\",\"w185\",\"rwt.widgets.GridItem\",{\"parent\":\"" +
"w99\",\"index\":77,\"texts\":[\"78\",\"Pt\",\"Platinum\",\"Transition " +
"metal\",\"10\",\"6\"],\"cellBackgrounds\":[null,null,null,[252,175,62," +
"255],null,null]}],[\"create\",\"w186\",\"rwt.widgets.GridItem\",{\"par" +
"ent\":\"w99\",\"index\":78,\"texts\":[\"79\",\"Au\",\"Gold\",\"Transit";
}

s29 = function() {
  return
"ion metal\",\"11\",\"6\"],\"cellBackgrounds\":[null,null,null,[252,175" +
",62,255],null,null]}],[\"create\",\"w187\",\"rwt.widgets.GridItem\",{\"" +
"parent\":\"w99\",\"index\":79,\"texts\":[\"80\",\"Hg\",\"Mercury\",\"T" +
"ransition metal\",\"12\",\"6\"],\"cellBackgrounds\":[null,null,null,[2" +
"52,175,62,255],null,null]}],[\"create\",\"w188\",\"rwt.widgets.GridIte" +
"m\",{\"parent\":\"w99\",\"index\":80,\"texts\":[\"81\",\"Tl\",\"Thalli" +
"um\",\"Poor metal\",\"13\",\"6\"],\"cellBackgrounds\":[null,null,null," +
"[238,238,236,255],null,null]}],[\"create\",\"w189\",\"rwt.widgets.Grid" +
"Item\",{\"parent\":\"w99\",\"index\":81,\"texts\":[\"82\",\"Pb\",\"Lea" +
"d\",\"Poor metal\",\"14\",\"6\"],\"cellBackgrounds\":[null,null,null,[";
}

s30 = function() {
  return
"238,238,236,255],null,null]}],[\"create\",\"w190\",\"rwt.widgets.GridI" +
"tem\",{\"parent\":\"w99\",\"index\":82,\"texts\":[\"83\",\"Bi\",\"Bism" +
"uth\",\"Poor metal\",\"15\",\"6\"],\"cellBackgrounds\":[null,null,null" +
",[238,238,236,255],null,null]}],[\"create\",\"w191\",\"rwt.widgets.Gri" +
"dItem\",{\"parent\":\"w99\",\"index\":83,\"texts\":[\"84\",\"Po\",\"Po" +
"lonium\",\"Metalloid\",\"16\",\"6\"],\"cellBackgrounds\":[null,null,nu" +
"ll,[156,159,153,255],null,null]}],[\"create\",\"w192\",\"rwt.widgets.G" +
"ridItem\",{\"parent\":\"w99\",\"index\":84,\"texts\":[\"85\",\"At\",\"" +
"Astatine\",\"Halogen\",\"17\",\"6\"],\"cellBackgrounds\":[null,null,nu" +
"ll,[252,233,79,255],null,null]}],[\"create\",\"w193\",\"rwt.widgets.Gr";
}

s31 = function() {
  return
"idItem\",{\"parent\":\"w99\",\"index\":85,\"texts\":[\"86\",\"Rn\",\"R" +
"adon\",\"Noble gas\",\"18\",\"6\"],\"cellBackgrounds\":[null,null,null" +
",[114,159,207,255],null,null]}],[\"create\",\"w194\",\"rwt.widgets.Gri" +
"dItem\",{\"parent\":\"w99\",\"index\":86,\"texts\":[\"87\",\"Fr\",\"Fr" +
"ancium\",\"Alkali metal\",\"1\",\"7\"],\"cellBackgrounds\":[null,null," +
"null,[239,41,41,255],null,null]}],[\"create\",\"w195\",\"rwt.widgets.G" +
"ridItem\",{\"parent\":\"w99\",\"index\":87,\"texts\":[\"88\",\"Ra\",\"" +
"Radium\",\"Alkaline earth metal\",\"2\",\"7\"],\"cellBackgrounds\":[nu" +
"ll,null,null,[233,185,110,255],null,null]}],[\"create\",\"w196\",\"rwt" +
".widgets.GridItem\",{\"parent\":\"w99\",\"index\":88,\"texts\":[\"89\"";
}

s32 = function() {
  return
",\"Ac\",\"Actinium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[nu" +
"ll,null,null,[173,127,168,255],null,null]}],[\"create\",\"w197\",\"rwt" +
".widgets.GridItem\",{\"parent\":\"w99\",\"index\":89,\"texts\":[\"90\"" +
",\"Th\",\"Thorium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[nul" +
"l,null,null,[173,127,168,255],null,null]}],[\"create\",\"w198\",\"rwt." +
"widgets.GridItem\",{\"parent\":\"w99\",\"index\":90,\"texts\":[\"91\"," +
"\"Pa\",\"Protactinium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":" +
"[null,null,null,[173,127,168,255],null,null]}],[\"create\",\"w199\",\"" +
"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":91,\"texts\":[\"9" +
"2\",\"U\",\"Uranium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[n";
}

s33 = function() {
  return
"ull,null,null,[173,127,168,255],null,null]}],[\"create\",\"w200\",\"rw" +
"t.widgets.GridItem\",{\"parent\":\"w99\",\"index\":92,\"texts\":[\"93\"" +
",\"Np\",\"Neptunium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[n" +
"ull,null,null,[173,127,168,255],null,null]}],[\"create\",\"w201\",\"rw" +
"t.widgets.GridItem\",{\"parent\":\"w99\",\"index\":93,\"texts\":[\"94\"" +
",\"Pu\",\"Plutonium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[n" +
"ull,null,null,[173,127,168,255],null,null]}],[\"create\",\"w202\",\"rw" +
"t.widgets.GridItem\",{\"parent\":\"w99\",\"index\":94,\"texts\":[\"95\"" +
",\"Am\",\"Americium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[n" +
"ull,null,null,[173,127,168,255],null,null]}],[\"create\",\"w203\",\"rw";
}

s34 = function() {
  return
"t.widgets.GridItem\",{\"parent\":\"w99\",\"index\":95,\"texts\":[\"96\"" +
",\"Cm\",\"Curium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[null" +
",null,null,[173,127,168,255],null,null]}],[\"create\",\"w204\",\"rwt.w" +
"idgets.GridItem\",{\"parent\":\"w99\",\"index\":96,\"texts\":[\"97\",\"" +
"Bk\",\"Berkelium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[null" +
",null,null,[173,127,168,255],null,null]}],[\"create\",\"w205\",\"rwt.w" +
"idgets.GridItem\",{\"parent\":\"w99\",\"index\":97,\"texts\":[\"98\",\"" +
"Cf\",\"Californium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":[nu" +
"ll,null,null,[173,127,168,255],null,null]}],[\"create\",\"w206\",\"rwt" +
".widgets.GridItem\",{\"parent\":\"w99\",\"index\":98,\"texts\":[\"99\"";
}

s35 = function() {
  return
",\"Es\",\"Einsteinium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":" +
"[null,null,null,[173,127,168,255],null,null]}],[\"create\",\"w207\",\"" +
"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":99,\"texts\":[\"1" +
"00\",\"Fm\",\"Fermium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrounds\":" +
"[null,null,null,[173,127,168,255],null,null]}],[\"create\",\"w208\",\"" +
"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":100,\"texts\":[\"" +
"101\",\"Md\",\"Mendelevium\",\"Actinide\",\"3\",\"7\"],\"cellBackgroun" +
"ds\":[null,null,null,[173,127,168,255],null,null]}],[\"create\",\"w209" +
"\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":101,\"texts\"" +
":[\"102\",\"No\",\"Nobelium\",\"Actinide\",\"3\",\"7\"],\"cellBackgrou";
}

s36 = function() {
  return
"nds\":[null,null,null,[173,127,168,255],null,null]}],[\"create\",\"w21" +
"0\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":102,\"texts" +
"\":[\"103\",\"Lr\",\"Lawrencium\",\"Actinide\",\"3\",\"7\"],\"cellBack" +
"grounds\":[null,null,null,[173,127,168,255],null,null]}],[\"create\",\"" +
"w211\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":103,\"te" +
"xts\":[\"104\",\"Rf\",\"Rutherfordium\",\"Transition metal\",\"4\",\"7" +
"\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null,null]}]," +
"[\"create\",\"w212\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"in" +
"dex\":104,\"texts\":[\"105\",\"Db\",\"Dubnium\",\"Transition metal\",\"" +
"5\",\"7\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null,n";
}

s37 = function() {
  return
"ull]}],[\"create\",\"w213\",\"rwt.widgets.GridItem\",{\"parent\":\"w99" +
"\",\"index\":105,\"texts\":[\"106\",\"Sg\",\"Seaborgium\",\"Transition" +
" metal\",\"6\",\"7\"],\"cellBackgrounds\":[null,null,null,[252,175,62," +
"255],null,null]}],[\"create\",\"w214\",\"rwt.widgets.GridItem\",{\"par" +
"ent\":\"w99\",\"index\":106,\"texts\":[\"107\",\"Bh\",\"Bohrium\",\"Tr" +
"ansition metal\",\"7\",\"7\"],\"cellBackgrounds\":[null,null,null,[252" +
",175,62,255],null,null]}],[\"create\",\"w215\",\"rwt.widgets.GridItem\"" +
",{\"parent\":\"w99\",\"index\":107,\"texts\":[\"108\",\"Hs\",\"Hassium" +
"\",\"Transition metal\",\"8\",\"7\"],\"cellBackgrounds\":[null,null,nu" +
"ll,[252,175,62,255],null,null]}],[\"create\",\"w216\",\"rwt.widgets.Gr";
}

s38 = function() {
  return
"idItem\",{\"parent\":\"w99\",\"index\":108,\"texts\":[\"109\",\"Mt\",\"" +
"Meitnerium\",\"Transition metal\",\"9\",\"7\"],\"cellBackgrounds\":[nu" +
"ll,null,null,[252,175,62,255],null,null]}],[\"create\",\"w217\",\"rwt." +
"widgets.GridItem\",{\"parent\":\"w99\",\"index\":109,\"texts\":[\"110\"" +
",\"Ds\",\"Darmstadtium\",\"Transition metal\",\"10\",\"7\"],\"cellBack" +
"grounds\":[null,null,null,[252,175,62,255],null,null]}],[\"create\",\"" +
"w218\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":110,\"te" +
"xts\":[\"111\",\"Rg\",\"Roentgenium\",\"Transition metal\",\"11\",\"7\"" +
"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null,null]}],[\"" +
"create\",\"w219\",\"rwt.widgets.GridItem\",{\"parent\":\"w99\",\"index";
}

s39 = function() {
  return
"\":111,\"texts\":[\"112\",\"Uub\",\"Ununbium\",\"Transition metal\",\"" +
"12\",\"7\"],\"cellBackgrounds\":[null,null,null,[252,175,62,255],null," +
"null]}],[\"create\",\"w220\",\"rwt.widgets.GridItem\",{\"parent\":\"w9" +
"9\",\"index\":112,\"texts\":[\"113\",\"Uut\",\"Ununtrium\",\"Poor meta" +
"l\",\"13\",\"7\"],\"cellBackgrounds\":[null,null,null,[238,238,236,255" +
"],null,null]}],[\"create\",\"w221\",\"rwt.widgets.GridItem\",{\"parent" +
"\":\"w99\",\"index\":113,\"texts\":[\"114\",\"Uuq\",\"Ununquadium\",\"" +
"Poor metal\",\"14\",\"7\"],\"cellBackgrounds\":[null,null,null,[238,23" +
"8,236,255],null,null]}],[\"create\",\"w222\",\"rwt.widgets.GridItem\"," +
"{\"parent\":\"w99\",\"index\":114,\"texts\":[\"115\",\"Uup\",\"Ununpen";
}

s40 = function() {
  return
"tium\",\"Poor metal\",\"15\",\"7\"],\"cellBackgrounds\":[null,null,nul" +
"l,[238,238,236,255],null,null]}],[\"create\",\"w223\",\"rwt.widgets.Gr" +
"idItem\",{\"parent\":\"w99\",\"index\":115,\"texts\":[\"116\",\"Uuh\"," +
"\"Ununhexium\",\"Poor metal\",\"16\",\"7\"],\"cellBackgrounds\":[null," +
"null,null,[238,238,236,255],null,null]}],[\"create\",\"w224\",\"rwt.wi" +
"dgets.GridItem\",{\"parent\":\"w99\",\"index\":116,\"texts\":[\"117\"," +
"\"Uus\",\"Ununseptium\",\"Halogen\",\"17\",\"7\"],\"cellBackgrounds\":" +
"[null,null,null,[252,233,79,255],null,null]}],[\"create\",\"w225\",\"r" +
"wt.widgets.GridItem\",{\"parent\":\"w99\",\"index\":117,\"texts\":[\"1" +
"18\",\"Uuo\",\"Ununoctium\",\"Noble gas\",\"18\",\"7\"],\"cellBackgrou";
}

s41 = function() {
  return
"nds\":[null,null,null,[114,159,207,255],null,null]}],[\"create\",\"w22" +
"6\",\"rwt.widgets.Composite\",{\"parent\":\"w97\",\"style\":[\"BORDER\"" +
"],\"bounds\":[10,464,988,25],\"children\":[\"w227\"],\"tabIndex\":-1,\"" +
"clientArea\":[0,0,986,23]}],[\"create\",\"w227\",\"rwt.widgets.Label\"" +
",{\"parent\":\"w226\",\"style\":[\"NONE\"],\"bounds\":[10,10,966,3],\"" +
"tabIndex\":-1,\"text\":\"Hydrogen (H)\"}],[\"create\",\"w228\",\"rwt.w" +
"idgets.Label\",{\"parent\":\"w97\",\"style\":[\"WRAP\"],\"bounds\":[10" +
",499,988,16],\"tabIndex\":-1,\"foreground\":[150,150,150,255],\"font\"" +
":[[\"Verdana\",\"Lucida Sans\",\"Arial\",\"Helvetica\",\"sans-serif\"]" +
",10,false,false],\"text\":\"Shortcuts: [CTRL+F] - Filter | Sort by: [C";
}

s42 = function() {
  return
"TRL+R] - Number, [CTRL+Y] - Symbol, [CTRL+N] - Name, [CTRL+S] - Series" +
", [CTRL+G] - Group, [CTRL+E] - Period\"}],[\"set\",\"w1\",{\"focusCont" +
"rol\":\"w99\"}],[\"call\",\"rwt.client.BrowserNavigation\",\"addToHist" +
"ory\",{\"entries\":[[\"tableviewer\",\"TableViewer\"]]}]]}";
}

var RAP_BENCHMARK_MINIFIED = 
s0() + s1() + s2() + s3() + s4() + s5() + s6() + s7() + s8() + s9() + s10() + s11() + s12() + s13() + s14() + s15() + s16() + s17() + s18() + s19() + s20() + s21() + s22() + s23() + s24() + s25() + s26() + s27() + s28() + s29() + s30() + s31() + s32() + s33() + s34() + s35() + s36() + s37() + s38() + s39() + s40() + s41() + s42();

function Json() {
  Benchmark.call(this);
}
Json.prototype = Object.create(Benchmark.prototype);

Json.prototype.benchmark = function () {
  return (new Parser(RAP_BENCHMARK_MINIFIED)).parse();
};

Json.prototype.verifyResult = function (result) {
  if (!result.isObject()) { return false; }
  if (!result.asObject().get("head").isObject())      { return false; }
  if (!result.asObject().get("operations").isArray()) { return false; }
  return result.asObject().get("operations").asArray().size() === 156;
};

function Parser(string) {
  this.input         = string;
  this.index         = -1;
  this.line          = 1;
  this.captureStart  = -1;
  this.column        = 0;
  this.current       = null;
  this.captureBuffer = "";
}

Parser.prototype.parse = function () {
  this.read();
  this.skipWhiteSpace();
  var result = this.readValue();
  this.skipWhiteSpace();
  if (!this.isEndOfText()) {
    throw this.error("Unexpected character");
  }
  return result;
};

Parser.prototype.readValue = function () {
  switch (this.current) {
  case "n":
    return this.readNull();
  case "t":
    return this.readTrue();
  case "f":
    return this.readFalse();
  case "\"":
    return this.readString();
  case "[":
    return this.readArray();
  case "{":
    return this.readObject();
  case "-":
  case "0":
  case "1":
  case "2":
  case "3":
  case "4":
  case "5":
  case "6":
  case "7":
  case "8":
  case "9":
    return this.readNumber();
  default:
    throw this.expected("value");
  }
};

Parser.prototype.readArray = function () {
  this.read();
  var array = new JsonArray();
  this.skipWhiteSpace();
  if (this.readChar("]")) {
    return array;
  }
  do {
    this.skipWhiteSpace();
    array.add(this.readValue());
    this.skipWhiteSpace();
  } while (this.readChar(","));
  if (!this.readChar("]")) {
    throw this.expected("',' or ']'");
  }
  return array;
};

Parser.prototype.readObject = function () {
  this.read();
  var object = new JsonObject();
  this.skipWhiteSpace();
  if (this.readChar("}")) {
    return object;
  }
  do {
    this.skipWhiteSpace();
    var name = this.readName();
    this.skipWhiteSpace();
    if (!this.readChar(":")) {
      throw this.expected("':'");
    }
    this.skipWhiteSpace();
    object.add(name, this.readValue());
    this.skipWhiteSpace();
  } while (this.readChar(","));

  if (!this.readChar("}")) {
    throw this.expected("',' or '}'");
  }
  return object;
};

Parser.prototype.readName = function () {
  if (this.current !== '"') {
    throw this.expected("name");
  }
  return this.readStringInternal();
};

Parser.prototype.readNull = function () {
  this.read();
  this.readRequiredChar("u");
  this.readRequiredChar("l");
  this.readRequiredChar("l");
  return JsonLiteral.NULL;
};

Parser.prototype.readTrue = function () {
  this.read();
  this.readRequiredChar("r");
  this.readRequiredChar("u");
  this.readRequiredChar("e");
  return JsonLiteral.TRUE;
};

Parser.prototype.readFalse = function () {
  this.read();
  this.readRequiredChar("a");
  this.readRequiredChar("l");
  this.readRequiredChar("s");
  this.readRequiredChar("e");
  return JsonLiteral.FALSE;
};

Parser.prototype.readRequiredChar = function (ch) {
  if (!this.readChar(ch)) {
    throw this.expected("'" + ch + "'");
  }
};

Parser.prototype.readString = function () {
  return new JsonString(this.readStringInternal());
};

Parser.prototype.readStringInternal = function () {
  this.read();
  this.startCapture();
  while (this.current !== '"') {
    if (this.current === "\\") {
      this.pauseCapture();
      this.readEscape();
      this.startCapture();
    } else {
      this.read();
    }
  }
  var string = this.endCapture();
  this.read();
  return string;
};

Parser.prototype.readEscape = function () {
  this.read();
  switch (this.current) {
  case '"':
  case "/":
  case "\\":
    this.captureBuffer += this.current;
    break;
  case "b":
    this.captureBuffer += "\b";
    break;
  case "f":
    this.captureBuffer += "\f";
    break;
  case "n":
    this.captureBuffer += "\n";
    break;
  case "r":
    this.captureBuffer += "\r";
    break;
  case "t":
    this.captureBuffer += "\t";
    break;
  default:
    throw this.expected("valid escape sequence");
  }
  this.read();
};

Parser.prototype.readNumber = function () {
  this.startCapture();
  this.readChar("-");
  var firstDigit = this.current;
  if (!this.readDigit()) {
    throw this.expected("digit");
  }
  if (firstDigit !== "0") {
    while (this.readDigit()) { }
  }
  this.readFraction();
  this.readExponent();
  return new JsonNumber(this.endCapture());
};

Parser.prototype.readFraction = function () {
  if (!this.readChar(".")) {
    return false;
  }
  if (!this.readDigit()) {
    throw this.expected("digit");
  }
  while (this.readDigit()) { }
  return true;
};

Parser.prototype.readExponent = function () {
  if (!this.readChar("e") && !this.readChar("E")) {
    return false;
  }
  if (!this.readChar("+")) {
    this.readChar("-");
  }
  if (!this.readDigit()) {
    throw this.expected("digit");
  }

  while (this.readDigit()) { }
  return true;
};

Parser.prototype.readChar = function (ch) {
  if (this.current !== ch) {
    return false;
  }
  this.read();
  return true;
};

Parser.prototype.readDigit = function () {
  if (!this.isDigit()) {
    return false;
  }
  this.read();
  return true;
};

Parser.prototype.skipWhiteSpace = function () {
  while (this.isWhiteSpace()) {
    this.read();
  }
};

Parser.prototype.read = function () {
  if ("\n" === this.current) {
    this.line += 1;
    this.column = 0;
  }
  this.index += 1;
  if (this.index < this.input.length) {
    this.current = this.input.substring(this.index, this.index + 1);
  } else {
    this.current = null;
  }
};

Parser.prototype.startCapture = function () {
  this.captureStart = this.index;
};

Parser.prototype.pauseCapture = function () {
  var end = this.current === null ? this.index : this.index - 1;
  this.captureBuffer += this.input.substring(this.captureStart, end + 1);
  this.captureStart = -1;
};

Parser.prototype.endCapture = function () {
  var end = this.current === null ? this.index : this.index - 1;
  var captured;
  if ("" === this.captureBuffer) {
    captured = this.input.substring(this.captureStart, end + 1);
  } else {
    this.captureBuffer += this.input.substring(this.captureStart, end + 1);
    captured = this.captureBuffer;
    this.captureBuffer = "";
  }
  this.captureStart = -1;
  return captured;
};

Parser.prototype.expected = function (expected) {
  if (this.isEndOfText()) {
    return this.error("Unexpected end of input");
  }
  return this.error("Expected " + expected);
};

Parser.prototype.error = function (message) {
  return new ParseException(message, this.index, this.line, this.column - 1);
};

Parser.prototype.isWhiteSpace = function () {
  return " " === this.current || "\t" === this.current ||
    "\n" === this.current || "\r" === this.current;
};

Parser.prototype.isDigit = function () {
  return "0" === this.current ||
    "1" === this.current ||
    "2" === this.current ||
    "3" === this.current ||
    "4" === this.current ||
    "5" === this.current ||
    "6" === this.current ||
    "7" === this.current ||
    "8" === this.current ||
    "9" === this.current;
};

Parser.prototype.isEndOfText = function () {
  return this.current === null;
};

function HashIndexTable() {
  this.hashTable = (new Array(32)).fill(0); // must be a power of two
}

HashIndexTable.prototype.add = function (name, index) {
  var slot = this.hashSlotFor(name);
  if (index < 0xff) {
    // increment by 1, 0 stands for empty
    this.hashTable[slot] = (index + 1) & 0xff;
  } else {
    this.hashTable[slot] = 0;
  }
};

HashIndexTable.prototype.get = function (name) {
  var slot = this.hashSlotFor(name);
  // subtract 1, 0 stands for empty
  return (this.hashTable[slot] & 0xff) - 1;
};

function stringHash(s) {
  // this is not a proper hash, but sufficient for the benchmark,
  // and very portable!
  return s.length * 1402589;
}

HashIndexTable.prototype.hashSlotFor = function (element) {
  return stringHash(element) & (this.hashTable.length - 1);
};

function ParseException(message, offset, line, column) {
  this.message = message + " at " + line + ":" + column;
  this.offset  = offset;
  this.line    = line;
  this.column  = column;
}

function JsonValue() {}

JsonValue.prototype.isObject = function () {
  return false;
};

JsonValue.prototype.isArray = function () {
  return false;
};

JsonValue.prototype.isNumber = function () {
  return false;
};

JsonValue.prototype.isString = function () {
  return false;
};

JsonValue.prototype.isBoolean = function () {
  return false;
};

JsonValue.prototype.isTrue = function () {
  return false;
};

JsonValue.prototype.isFalse = function () {
  return false;
};

JsonValue.prototype.isNull = function () {
  return false;
};

JsonValue.prototype.asObject = function () {
  throw new Error("Not an object: " + this);
};

JsonValue.prototype.asArray = function () {
  throw new Error("Not an array: " + this);
};

function JsonArray() {
  JsonValue.call(this);
  this.values = new som.Vector();
}
JsonArray.prototype = Object.create(JsonValue.prototype);

JsonArray.prototype.add = function (value) {
  if (value === null) {
    throw new Error("value is null");
  }
  this.values.append(value);
  return this;
};

JsonArray.prototype.size = function () {
  return this.values.size();
};

JsonArray.prototype.get = function (index) {
  return this.values.at(index);
};

JsonArray.prototype.isArray = function () {
  return true;
};

JsonArray.prototype.asArray = function () {
  return this;
};

function JsonLiteral(value) {
  JsonValue.call(this);
  this.value = value;
  this.null = "null" === value;
  this.true = "true" === value;
  this.false = "false" === value;

}
JsonLiteral.prototype = Object.create(JsonValue.prototype);
JsonValue.NULL  = new JsonLiteral("null");
JsonValue.TRUE  = new JsonLiteral("true");
JsonValue.FALSE = new JsonLiteral("false");

JsonLiteral.prototype.toString = function () {
  return this.value;
};

JsonLiteral.prototype.isNull = function () {
  return this.null;
};

JsonLiteral.prototype.isTrue = function () {
  return this.true;
};

JsonLiteral.prototype.isFalse = function () {
  return this.false;
};

JsonLiteral.prototype.isBoolean = function () {
  return this.true || this.false;
};

function JsonNumber(string) {
  JsonValue.call(this);
  this.string = string;

  if (string === null) {
    throw new Error("string is null");
  }
}

JsonNumber.prototype.toString = function() {
  return this.string;
};

JsonNumber.prototype.isNumber = function () {
  return true;
};

function JsonObject() {
  JsonValue.call(this);

  this.names  = new som.Vector();
  this.values = new som.Vector();
  this.table  = new HashIndexTable();
}
JsonObject.prototype = Object.create(JsonValue.prototype);

JsonObject.prototype.add = function (name, value) {
  if (name === null) {
    throw new Error("name is null");
  }
  if (value === null) {
    throw new Error("value is null");
  }
  this.table.add(name, this.names.size());
  this.names.append(name);
  this.values.append(value);
  return this;
};

JsonObject.prototype.get = function (name) {
  if (name === null) {
    throw new Error("name is null");
  }
  var index = this.indexOf(name);
  return index === -1 ? null : this.values.at(index);
};

JsonObject.prototype.size = function () {
  return this.names.size();
};

JsonObject.prototype.isEmpty = function () {
  return this.names.isEmpty();
};

JsonObject.prototype.isObject = function () {
  return true;
};

JsonObject.prototype.asObject = function () {
  return this;
};

JsonObject.prototype.indexOf = function (name) {
  var index = this.table.get(name);
  if (index !== -1 && name === this.names.at(index)) {
    return index;
  }
  throw new Error("not implemented"); // Not needed for benchmark
};

function JsonString(string) {
  JsonValue.call(this);
  this.string = string;
}
JsonString.prototype = Object.create(JsonValue.prototype);

JsonString.prototype.isString = function () {
  return true;
};

var run = new Run(Json, "Json", 3, 100);
run.runBenchmark();
run.printTotal();
