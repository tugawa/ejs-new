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


// This benchmark is adapted to match the SOM version.
//
// Copyright © 2004-2013 Brent Fulgham
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//   * Redistributions of source code must retain the above copyright notice,
//     this list of conditions and the following disclaimer.
//
//   * Redistributions in binary form must reproduce the above copyright notice,
//     this list of conditions and the following disclaimer in the documentation
//     and/or other materials provided with the distribution.
//
//   * Neither the name of "The Computer Language Benchmarks Game" nor the name
//     of "The Computer Language Shootout Benchmarks" nor the names of its
//     contributors may be used to endorse or promote products derived from this
//     software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// The Computer Language Benchmarks Game
// http://benchmarksgame.alioth.debian.org
//
//  contributed by Karl von Laudermann
//  modified by Jeremy Echols
//  modified by Detlef Reichl
//  modified by Joseph LaFata
//  modified by Peter Zotov

// http://benchmarksgame.alioth.debian.org/u64q/program.php?test=mandelbrot&lang=yarv&id=3
'use strict';

function Mandelbrot() {
  Benchmark.call(this);

  function verifyResult(result, innerIterations) {
    if (innerIterations === 500) { return result === 191; }
    if (innerIterations === 750) { return result === 50;  }
    if (innerIterations ===   1) { return result === 128; }

    process.stdout.write("No verification result for " + innerIterations + " found\n");
    process.stdout.write("Result is: " + result + "\n");
    return false;
  }

  function mandelbrot(size) {
    var sum     = 0;
    var byteAcc = 0;
    var bitNum  = 0;

    var y = 0;

    while (y < size) {
      var ci = (2.0 * y / size) - 1.0,
        x = 0;

      while (x < size) {
        var zr   = 0.0,
          zrzr = 0.0,
          zi   = 0.0,
          zizi = 0.0,
          cr = (2.0 * x / size) - 1.5;

        var z = 0,
          notDone = true,
          escape = 0;
        while (notDone && z < 50) {
          zr = zrzr - zizi + cr;
          zi = 2.0 * zr * zi + ci;

          // preserve recalculation
          zrzr = zr * zr;
          zizi = zi * zi;

          if (zrzr + zizi > 4.0) {
            notDone = false;
            escape  = 1;
          }
          z += 1;
        }

        byteAcc = (byteAcc << 1) + escape;
        bitNum += 1;

        // Code is very similar for these cases, but using separate blocks
        // ensures we skip the shifting when it's unnecessary, which is most cases.
        if (bitNum === 8) {
          sum ^= byteAcc;
          byteAcc = 0;
          bitNum  = 0;
        } else if (x === size - 1) {
          byteAcc <<= (8 - bitNum);
          sum ^= byteAcc;
          byteAcc = 0;
          bitNum  = 0;
        }
        x += 1;
      }
      y += 1;
    }
    return sum;
  }

  this.innerBenchmarkLoop = function (innerIterations) {
    return verifyResult(mandelbrot(innerIterations), innerIterations);
  };
}

var run = new Run(Mandelbrot, "Mandelbrot", 1, 500);
run.runBenchmark();
run.printTotal();

