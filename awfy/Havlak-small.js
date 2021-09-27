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


// Adapted based on SOM and Java benchmark.
//  Copyright 2011 Google Inc.
//
//      Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//      You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//      See the License for the specific language governing permissions and
//          limitations under the License.
'use strict';

function Havlak() {
  Benchmark.call(this);

  this.innerBenchmarkLoop = function (innerIterations) {
    return this.verifyResult((new LoopTesterApp()).main(
      innerIterations, 50, 10 /* was 100 */, 10, 5), innerIterations);
  };

  this.verifyResult = function (result, innerIterations) {
    if (innerIterations === 15000) { return result[0] === 46602 && result[1] === 5213; }
    if (innerIterations ===  1500) { return result[0] ===  6102 && result[1] === 5213; }
    if (innerIterations ===   150) { return result[0] ===  2052 && result[1] === 5213; }
    if (innerIterations ===    15) { return result[0] ===  1647 && result[1] === 5213; }
    if (innerIterations ===     1) { return result[0] ===  1605 && result[1] === 5213; }

    process.stdout.write("No verification result for " + innerIterations + " found");
    process.stdout.write("Result is: " + result[0] + ", " + result[1]);
    return false;
  };
}

function BasicBlock(name) {
  this.name     = name;
  this.inEdges  = new som.Vector(2);
  this.outEdges = new som.Vector(2);
}

BasicBlock.prototype.getInEdges = function () {
  return this.inEdges;
};

BasicBlock.prototype.getOutEdges = function () {
  return this.outEdges;
};

BasicBlock.prototype.getNumPred = function () {
  return this.inEdges.size();
};

BasicBlock.prototype.addOutEdge = function (to) {
  return this.outEdges.append(to);
};

BasicBlock.prototype.addInEdge = function (from) {
  return this.inEdges.append(from);
};

BasicBlock.prototype.customHash = function () {
  return this.name;
};

function BasicBlockEdge(cfg, fromName, toName) {
  this.from = cfg.createNode(fromName);
  this.to   = cfg.createNode(toName);

  this.from.addOutEdge(this.to);
  this.to.addInEdge(this.from);

  cfg.addEdge(this);
}

function ControlFlowGraph() {
  this.startNode     = null;
  this.basicBlockMap = new som.Vector();
  this.edgeList      = new som.Vector();
}

ControlFlowGraph.prototype.createNode = function (name) {
  var node;
  if (this.basicBlockMap.at(name)) {
    node = this.basicBlockMap.at(name);
  } else {
    node = new BasicBlock(name);
    this.basicBlockMap.atPut(name, node);
  }

  if (this.getNumNodes() === 1) {
    this.startNode = node;
  }
  return node;
};

ControlFlowGraph.prototype.addEdge = function (edge) {
  this.edgeList.append(edge);
};

ControlFlowGraph.prototype.getNumNodes = function () {
  return this.basicBlockMap.size();
};

ControlFlowGraph.prototype.getStartBasicBlock = function () {
  return this.startNode;
};

ControlFlowGraph.prototype.getBasicBlocks = function () {
  return this.basicBlockMap;
};

function LoopStructureGraph() {
  this.loopCounter = 0;
  this.loops = new som.Vector();
  this.root  = new SimpleLoop(null, true);
  this.root.setNestingLevel(0);
  this.root.setCounter(this.loopCounter);
  this.loopCounter += 1;
  this.loops.append(this.root);
}

LoopStructureGraph.prototype.createNewLoop = function (bb, isReducible) {
  var loop = new SimpleLoop(bb, isReducible);
  loop.setCounter(this.loopCounter);
  this.loopCounter += 1;
  this.loops.append(loop);
  return loop;
};

LoopStructureGraph.prototype.calculateNestingLevel = function () {
  // link up all 1st level loops to artificial root node.
  var that = this;
  this.loops.forEach(function (liter) {
    if (!liter.isRoot()) {
      if (!liter.getParent()) {
        liter.setParent(that.root);
      }
    }
  });

  // recursively traverse the tree and assign levels.
  this.calculateNestingLevelRec(this.root, 0);
};

LoopStructureGraph.prototype.calculateNestingLevelRec = function (loop, depth) {
  var that = this;
  loop.setDepthLevel(depth);
  loop.getChildren().forEach(function (liter) {
    that.calculateNestingLevelRec(liter, depth + 1);

    loop.setNestingLevel(
      Math.max(loop.getNestingLevel(),
        1 + liter.getNestingLevel()));
  });
};

LoopStructureGraph.prototype.getNumLoops = function () {
  return this.loops.size();
};


function SimpleLoop(bb, isReducible) {
  this.isReducible = isReducible;
  this.parent = null;
  this.isRoot_ = false;
  this.nestingLevel = 0;
  this.depthLevel   = 0;
  this.basicBlocks  = new som.IdentitySet();
  this.children     = new som.IdentitySet();

  if (bb) {
    this.basicBlocks.add(bb);
  }
  this.header = bb;
}

SimpleLoop.prototype.addNode = function (bb) {
  this.basicBlocks.add(bb);
};

SimpleLoop.prototype.addChildLoop = function (loop) {
  this.children.add(loop);
};

// Getters/Setters
SimpleLoop.prototype.getChildren = function () {
  return this.children;
};

SimpleLoop.prototype.getParent = function () {
  return this.parent;
};

SimpleLoop.prototype.getNestingLevel = function (){
  return this.nestingLevel;
};

SimpleLoop.prototype.isRoot = function () {
  return this.isRoot_;
};

SimpleLoop.prototype.setParent = function (parent) {
  this.parent = parent;
  this.parent.addChildLoop(this);
};

SimpleLoop.prototype.setIsRoot = function () {
  this.isRoot_ = true;
};

SimpleLoop.prototype.setCounter = function (value) {
  this.counter = value;
};

SimpleLoop.prototype.setNestingLevel = function (level) {
  this.nestingLevel = level;
  if (level === 0) {
    this.setIsRoot();
  }
};

SimpleLoop.prototype.setDepthLevel = function (level) {
  this.depthLevel = level;
};

function UnionFindNode() { /* no op */ }

// Initialize this node.
UnionFindNode.prototype.initNode = function (bb, dfsNumber) {
  this.parent     = this;
  this.bb         = bb;
  this.dfsNumber  = dfsNumber;
  this.loop       = null;
};

// Union/Find Algorithm - The find routine.
//
// Implemented with Path Compression (inner loops are only
// visited and collapsed once, however, deep nests would still
// result in significant traversals).
//
UnionFindNode.prototype.findSet = function () {
  var nodeList = new som.Vector(),
    node = this,
    that = this;
  while (node !== node.parent) {
    if (node.parent !== node.parent.parent) {
      nodeList.append(node);
    }
    node = node.parent;
  }

  // Path Compression, all nodes' parents point to the 1st level parent.
  nodeList.forEach(function (iter) { iter.union(that.parent); });
  return node;
};

// Union/Find Algorithm - The union routine.
//
// Trivial. Assigning parent pointer is enough,
// we rely on path compression.
//
UnionFindNode.prototype.union = function (basicBlock) {
  this.parent = basicBlock;
};

// Getters/Setters
//
UnionFindNode.prototype.getBb = function () {
  return this.bb;
};

UnionFindNode.prototype.getLoop = function () {
  return this.loop;
};

UnionFindNode.prototype.getDfsNumber = function () {
  return this.dfsNumber;
};

UnionFindNode.prototype.setLoop = function (loop) {
  this.loop = loop;
};

function LoopTesterApp() {
  this.cfg = new ControlFlowGraph();
  this.lsg = new LoopStructureGraph();
  this.cfg.createNode(0);
}

// Create 4 basic blocks, corresponding to and if/then/else clause
// with a CFG that looks like a diamond
LoopTesterApp.prototype.buildDiamond = function (start) {
  var bb0 = start;
  new BasicBlockEdge(this.cfg, bb0, bb0 + 1);
  new BasicBlockEdge(this.cfg, bb0, bb0 + 2);
  new BasicBlockEdge(this.cfg, bb0 + 1, bb0 + 3);
  new BasicBlockEdge(this.cfg, bb0 + 2, bb0 + 3);

  return bb0 + 3;
};

// Connect two existing nodes
LoopTesterApp.prototype.buildConnect = function (start, end) {
  new BasicBlockEdge(this.cfg, start, end);
};

// Form a straight connected sequence of n basic blocks
LoopTesterApp.prototype.buildStraight = function (start, n) {
  for (var i = 0; i < n; i++) {
    this.buildConnect(start + i, start + i + 1);
  }
  return start + n;
};

// Construct a simple loop with two diamonds in it
LoopTesterApp.prototype.buildBaseLoop = function (from) {
  var header = this.buildStraight(from, 1),
    diamond1 = this.buildDiamond(header),
    d11      = this.buildStraight(diamond1, 1),
    diamond2 = this.buildDiamond(d11),
    footer   = this.buildStraight(diamond2, 1);
  this.buildConnect(diamond2, d11);
  this.buildConnect(diamond1, header);

  this.buildConnect(footer, from);
  footer = this.buildStraight(footer, 1);
  return footer;
};

LoopTesterApp.prototype.main = function (numDummyLoops, findLoopIterations,
                                         parLoops, pparLoops, ppparLoops) {
  this.constructSimpleCFG();
  this.addDummyLoops(numDummyLoops);
  this.constructCFG(parLoops, pparLoops, ppparLoops);

  // Performing Loop Recognition, 1 Iteration, then findLoopIteration
  this.findLoops(this.lsg);
  for (var i = 0; i < findLoopIterations; i++) {
    this.findLoops(new LoopStructureGraph());
  }

  this.lsg.calculateNestingLevel();
  return [this.lsg.getNumLoops(), this.cfg.getNumNodes()];
};

LoopTesterApp.prototype.constructCFG = function (parLoops, pparLoops, ppparLoops) {
  var n = 2;

  for (var parlooptrees = 0; parlooptrees < parLoops; parlooptrees++) {
    this.cfg.createNode(n + 1);
    this.buildConnect(2, n + 1);
    n += 1;

    for (var i = 0; i < pparLoops; i++) {
      var top = n;
      n = this.buildStraight(n, 1);
      for (var j = 0; j < ppparLoops; j++) {
        n = this.buildBaseLoop(n);
      }
      var bottom = this.buildStraight(n, 1);
      this.buildConnect(n, top);
      n = bottom;
    }
    this.buildConnect(n, 1);
  }
};

LoopTesterApp.prototype.addDummyLoops = function (numDummyLoops) {
  for (var dummyloop = 0; dummyloop < numDummyLoops; dummyloop++) {
    this.findLoops(this.lsg);
  }
};

LoopTesterApp.prototype.findLoops = function (loopStructure) {
  var finder = new HavlakLoopFinder(this.cfg, loopStructure);
  finder.findLoops();
};

LoopTesterApp.prototype.constructSimpleCFG = function () {
  this.cfg.createNode(0);
  this.buildBaseLoop(0);
  this.cfg.createNode(1);
  new BasicBlockEdge(this.cfg, 0, 2);
};

var UNVISITED = 2147483647,       // Marker for uninitialized nodes.
  MAXNONBACKPREDS = (32 * 1024);  // Safeguard against pathological algorithm behavior.

function HavlakLoopFinder(cfg, lsg) {
  this.nonBackPreds = new som.Vector();
  this.backPreds  = new som.Vector();
  this.number = new som.IdentityDictionary();
  this.maxSize = 0;

  this.cfg = cfg;
  this.lsg = lsg;
}

// As described in the paper, determine whether a node 'w' is a
// "true" ancestor for node 'v'.
//
// Dominance can be tested quickly using a pre-order trick
// for depth-first spanning trees. This is why DFS is the first
// thing we run below.
HavlakLoopFinder.prototype.isAncestor = function (w, v) {
  return w <= v && v <= this.last[w];
};

// DFS - Depth-First-Search
//
// DESCRIPTION:
// Simple depth first traversal along out edges with node numbering.
HavlakLoopFinder.prototype.doDFS = function (currentNode, current) {
  this.nodes[current].initNode(currentNode, current);
  this.number.atPut(currentNode, current);

  var lastId = current,
    outerBlocks = currentNode.getOutEdges();

  for (var i = 0; i < outerBlocks.size(); i++) {
    var target = outerBlocks.at(i);
    if (this.number.at(target) == UNVISITED) {
      lastId = this.doDFS(target, lastId + 1);
    }
  }

  this.last[current] = lastId;
  return lastId;
};

HavlakLoopFinder.prototype.initAllNodes = function () {
  // Step a:
  //   - initialize all nodes as unvisited.
  //   - depth-first traversal and numbering.
  //   - unreached BB's are marked as dead.
  //
  var that = this;
  this.cfg.getBasicBlocks().forEach(
    function (bb) { that.number.atPut(bb, UNVISITED); });

  this.doDFS(this.cfg.getStartBasicBlock(), 0);
};

HavlakLoopFinder.prototype.identifyEdges = function (size) {
  // Step b:
  //   - iterate over all nodes.
  //
  //   A backedge comes from a descendant in the DFS tree, and non-backedges
  //   from non-descendants (following Tarjan).
  //
  //   - check incoming edges 'v' and add them to either
  //     - the list of backedges (backPreds) or
  //     - the list of non-backedges (nonBackPreds)
  for (var w = 0; w < size; w++) {
    this.header[w] = 0;
    this.type[w] = "BB_NONHEADER";

    var nodeW = this.nodes[w].getBb();
    if (!nodeW) {
      this.type[w] = "BB_DEAD";
    } else {
      this.processEdges(nodeW, w);
    }
  }
};

HavlakLoopFinder.prototype.processEdges = function (nodeW, w) {
  var that = this;

  if (nodeW.getNumPred() > 0) {
    nodeW.getInEdges().forEach(function (nodeV) {
      var v = that.number.at(nodeV);
      if (v != UNVISITED) {
        if (that.isAncestor(w, v)) {
          that.backPreds.at(w).append(v);
        } else {
          that.nonBackPreds.at(w).add(v);
        }
      }
    });
  }
};

// Find loops and build loop forest using Havlak's algorithm, which
// is derived from Tarjan. Variable names and step numbering has
// been chosen to be identical to the nomenclature in Havlak's
// paper (which, in turn, is similar to the one used by Tarjan).
HavlakLoopFinder.prototype.findLoops = function () {
  if (!this.cfg.getStartBasicBlock()) {
    return;
  }

  var size = this.cfg.getNumNodes();

  this.nonBackPreds.removeAll();
  this.backPreds.removeAll();
  this.number.removeAll();
  if (size > this.maxSize) {
    this.header  = new Array(size);
    this.type    = new Array(size);
    this.last    = new Array(size);
    this.nodes   = new Array(size);
    this.maxSize = size;
  }

  for (var i = 0; i < size; ++i) {
    this.nonBackPreds.append(new som.Set());
    this.backPreds.append(new som.Vector());
    this.nodes[i] = new UnionFindNode();
  }

  this.initAllNodes();
  this.identifyEdges(size);

  // Start node is root of all other loops.
  this.header[0] = 0;

  // Step c:
  //
  // The outer loop, unchanged from Tarjan. It does nothing except
  // for those nodes which are the destinations of backedges.
  // For a header node w, we chase backward from the sources of the
  // backedges adding nodes to the set P, representing the body of
  // the loop headed by w.
  //
  // By running through the nodes in reverse of the DFST preorder,
  // we ensure that inner loop headers will be processed before the
  // headers for surrounding loops.
  //
  for (var w = size - 1; w >= 0; w--) {
    // this is 'P' in Havlak's paper
    var nodePool = new som.Vector();

    var nodeW = this.nodes[w].getBb();
    if (nodeW) {
      this.stepD(w, nodePool);

      // Copy nodePool to workList.
      var workList = new som.Vector();
      nodePool.forEach(function (niter) { workList.append(niter); });

      if (nodePool.size() !== 0) {
        this.type[w] = "BB_REDUCIBLE";
      }

      // work the list...
      while (!workList.isEmpty()) {
        var x = workList.removeFirst();

        // Step e:
        //
        // Step e represents the main difference from Tarjan's method.
        // Chasing upwards from the sources of a node w's backedges. If
        // there is a node y' that is not a descendant of w, w is marked
        // the header of an irreducible loop, there is another entry
        // into this loop that avoids w.

        // The algorithm has degenerated. Break and
        // return in this case.
        var nonBackSize = this.nonBackPreds.at(x.getDfsNumber()).size();
        if (nonBackSize > MAXNONBACKPREDS) {
          return;
        }
        this.stepEProcessNonBackPreds(w, nodePool, workList, x);
      }

      // Collapse/Unionize nodes in a SCC to a single node
      // For every SCC found, create a loop descriptor and link it in.
      //
      if ((nodePool.size() > 0) || (this.type[w] === "BB_SELF")) {
        var loop = this.lsg.createNewLoop(nodeW, this.type[w] !== "BB_IRREDUCIBLE");
        this.setLoopAttributes(w, nodePool, loop);
      }
    }
  }  // Step c
};  // findLoops

HavlakLoopFinder.prototype.stepEProcessNonBackPreds = function (w, nodePool,
                                                                workList, x) {
  var that = this;
  this.nonBackPreds.at(x.getDfsNumber()).forEach(function (iter) {
    var y = that.nodes[iter],
      ydash = y.findSet();

    if (!that.isAncestor(w, ydash.getDfsNumber())) {
      that.type[w] = "BB_IRREDUCIBLE";
      that.nonBackPreds.at(w).add(ydash.getDfsNumber());
    } else {
      if (ydash.getDfsNumber() != w) {
        if (!nodePool.hasSome(function (e) { return e == ydash; })) {
          workList.append(ydash);
          nodePool.append(ydash);
        }
      }
    }
  });
};

HavlakLoopFinder.prototype.setLoopAttributes = function (w, nodePool, loop) {
  // At this point, one can set attributes to the loop, such as:
  //
  // the bottom node:
  //    iter  = backPreds[w].begin();
  //    loop bottom is: nodes[iter].node);
  //
  // the number of backedges:
  //    backPreds[w].size()
  //
  // whether this loop is reducible:
  //    type[w] != BasicBlockClass.BB_IRREDUCIBLE
  //
  this.nodes[w].setLoop(loop);
  var that = this;

  nodePool.forEach(function (node) {
    // Add nodes to loop descriptor.
    that.header[node.getDfsNumber()] = w;
    node.union(that.nodes[w]);

    // Nested loops are not added, but linked together.
    if (node.getLoop()) {
      node.getLoop().setParent(loop);
    } else {
      loop.addNode(node.getBb());
    }
  });
};

HavlakLoopFinder.prototype.stepD = function (w, nodePool) {
  var that = this;
  this.backPreds.at(w).forEach(function (v) {
    if (v != w) {
      nodePool.append(that.nodes[v].findSet());
    } else {
      that.type[w] = "BB_SELF";
    }
  });
};

var run = new Run(Havlak, "Havlak", 1, 1500);
run.runBenchmark();
run.printTotal();
