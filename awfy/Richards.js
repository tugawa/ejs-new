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


// The benchmark in its current state is a derivation from the SOM version,
// which is derived from Mario Wolczko's Smalltalk version of DeltaBlue.
//
// The original license details are availble here:
// http://web.archive.org/web/20050825101121/http://www.sunlabs.com/people/mario/java_benchmarking/index.html
'use strict';

var NO_TASK = null,
  NO_WORK   = null,
  IDLER     = 0,
  WORKER    = 1,
  HANDLER_A = 2,
  HANDLER_B = 3,
  DEVICE_A  = 4,
  DEVICE_B  = 5,
  NUM_TYPES = 6,

  DEVICE_PACKET_KIND = 0,
  WORK_PACKET_KIND   = 1,

  DATA_SIZE = 4,

  TRACING = false;

function Richards() {
  Benchmark.call(this);
}
Richards.prototype = Object.create(Benchmark.prototype);

Richards.prototype.benchmark = function () {
  return (new Scheduler()).start();
};

Richards.prototype.verifyResult = function (result) {
  return result;
};

function RBObject() {}

RBObject.prototype.append = function (packet, queueHead) {
  packet.link = NO_WORK;
  if (NO_WORK === queueHead) {
    return packet;
  }

  var mouse = queueHead,
    link;

  while (NO_WORK !== (link = mouse.link)) {
    mouse = link;
  }
  mouse.link = packet;
  return queueHead;
};

function Scheduler() {
  RBObject.call(this);

  // init tracing
  this.layout = 0;

  // init scheduler
  this.queuePacketCount = 0;
  this.holdCount = 0;
  this.taskTable = new Array(NUM_TYPES).fill(NO_TASK);
  this.taskList  = NO_TASK;

  this.currentTask = null;
  this.currentTaskIdentity = 0;
}
Scheduler.prototype = Object.create(RBObject.prototype);

Scheduler.prototype.createDevice = function (identity, priority, workPacket,
                                             state) {
  var data = new DeviceTaskDataRecord(),
    that = this;

  this.createTask(identity, priority, workPacket, state, data,
    function(workArg, wordArg) {
      var dataRecord = wordArg,
        functionWork = workArg;
      if (NO_WORK === functionWork) {
        if (NO_WORK === (functionWork = dataRecord.pending)) {
          return that.markWaiting();
        } else {
          dataRecord.pending = NO_WORK;
          return that.queuePacket(functionWork);
        }
      } else {
        dataRecord.pending = functionWork;
        if (TRACING) {
          that.trace(functionWork.datum);
        }
        return that.holdSelf();
      }
    });
};

Scheduler.prototype.createHandler = function (identity, priority, workPacket,
                                              state) {
  var data = new HandlerTaskDataRecord(),
    that = this;
  this.createTask(identity, priority, workPacket, state, data,
    function (work, word) {
      var dataRecord = word;
      if (NO_WORK !== work) {
        if (WORK_PACKET_KIND === work.kind) {
          dataRecord.workInAdd(work);
        } else {
          dataRecord.deviceInAdd(work);
        }
      }

      var workPacket;
      if (NO_WORK === (workPacket = dataRecord.workIn)) {
        return that.markWaiting();
      } else {
        var count = workPacket.datum;
        if (count >= DATA_SIZE) {
          dataRecord.workIn = workPacket.link;
          return that.queuePacket(workPacket);
        } else {
          var devicePacket;
          if (NO_WORK === (devicePacket = dataRecord.deviceIn)) {
            return that.markWaiting();
          } else {
            dataRecord.deviceIn = devicePacket.link;
            devicePacket.datum  = workPacket.data[count];
            workPacket.datum    = count + 1;
            return that.queuePacket(devicePacket);
          }
        }
      }
    });
};

Scheduler.prototype.createIdler = function (identity, priority, work, state) {
  var data = new IdleTaskDataRecord(),
    that = this;
  this.createTask(identity, priority, work, state, data,
    function (workArg, wordArg) {
      var dataRecord = wordArg;
      dataRecord.count -= 1;
      if (0 === dataRecord.count) {
        return that.holdSelf();
      } else {
        if (0 === (dataRecord.control & 1)) {
          dataRecord.control /= 2;
          return that.release(DEVICE_A);
        } else {
          dataRecord.control = (dataRecord.control / 2) ^ 53256;
          return that.release(DEVICE_B);
        }
      }
    });
};

Scheduler.prototype.createPacket = function (link, identity, kind) {
  return new Packet(link, identity, kind);
};

Scheduler.prototype.createTask = function (identity, priority, work, state,
                                           data, fn) {
  var t = new TaskControlBlock(this.taskList, identity, priority, work, state,
    data, fn);
  this.taskList = t;
  this.taskTable[identity] = t;
};

Scheduler.prototype.createWorker = function (identity, priority, workPacket, state) {
  var dataRecord = new WorkerTaskDataRecord(),
    that = this;
  this.createTask(identity, priority, workPacket, state, dataRecord,
    function (work, word) {
      var data = word;
      if (NO_WORK === work) {
        return that.markWaiting();
      } else {
        data.destination = (HANDLER_A === data.destination) ? HANDLER_B : HANDLER_A;
        work.identity = data.destination;
        work.datum = 0;
        for (var i = 0; i < DATA_SIZE; i++) {
          data.count += 1;
          if (data.count > 26) { data.count = 1; }
          work.data[i] = 65 + data.count - 1;
        }
        return that.queuePacket(work);
      }
    });
};

Scheduler.prototype.start = function () {
  var workQ;

  this.createIdler(IDLER, 0, NO_WORK, TaskState.createRunning());
  workQ = this.createPacket(NO_WORK, WORKER, WORK_PACKET_KIND);
  workQ = this.createPacket(workQ,   WORKER, WORK_PACKET_KIND);

  this.createWorker(WORKER, 1000, workQ, TaskState.createWaitingWithPacket());
  workQ = this.createPacket(NO_WORK, DEVICE_A, DEVICE_PACKET_KIND);
  workQ = this.createPacket(workQ,   DEVICE_A, DEVICE_PACKET_KIND);
  workQ = this.createPacket(workQ,   DEVICE_A, DEVICE_PACKET_KIND);

  this.createHandler(HANDLER_A, 2000, workQ, TaskState.createWaitingWithPacket());
  workQ = this.createPacket(NO_WORK, DEVICE_B, DEVICE_PACKET_KIND);
  workQ = this.createPacket(workQ,   DEVICE_B, DEVICE_PACKET_KIND);
  workQ = this.createPacket(workQ,   DEVICE_B, DEVICE_PACKET_KIND);

  this.createHandler(HANDLER_B, 3000,   workQ, TaskState.createWaitingWithPacket());
  this.createDevice(DEVICE_A,   4000, NO_WORK, TaskState.createWaiting());
  this.createDevice(DEVICE_B,   5000, NO_WORK, TaskState.createWaiting());

  this.schedule();

  return this.queuePacketCount == 23246 && this.holdCount == 9297;
};

Scheduler.prototype.findTask = function (identity) {
  var t = this.taskTable[identity];
  if (NO_TASK == t) { throw "findTask failed"; }
  return t;
};

Scheduler.prototype.holdSelf = function () {
  this.holdCount += 1;
  this.currentTask.setTaskHolding(true);
  return this.currentTask.link;
};

Scheduler.prototype.queuePacket = function (packet) {
  var t = this.findTask(packet.identity);
  if (NO_TASK == t) { return NO_TASK; }

  this.queuePacketCount += 1;

  packet.link = NO_WORK;
  packet.identity = this.currentTaskIdentity;
  return t.addInputAndCheckPriority(packet, this.currentTask);
};

Scheduler.prototype.release = function (identity) {
  var t = this.findTask(identity);
  if (NO_TASK == t) { return NO_TASK; }
  t.setTaskHolding(false);
  if (t.priority > this.currentTask.priority) {
    return t;
  } else {
    return this.currentTask;
  }
};

Scheduler.prototype.trace = function (id) {
  this.layout -= 1;
  if (0 >= this.layout) {
    process.stdout.write("\n");
    this.layout = 50;
  }
  process.stdout.write(id);
};

Scheduler.prototype.markWaiting = function () {
  this.currentTask.setTaskWaiting(true);
  return this.currentTask;
};

Scheduler.prototype.schedule = function () {
  this.currentTask = this.taskList;
  while (NO_TASK != this.currentTask) {
    if (this.currentTask.isTaskHoldingOrWaiting()) {
      this.currentTask = this.currentTask.link;
    } else {
      this.currentTaskIdentity = this.currentTask.identity;
      if (TRACING) { this.trace(this.currentTaskIdentity); }
      this.currentTask = this.currentTask.runTask();
    }
  }
};

function DeviceTaskDataRecord() {
  RBObject.call(this);
  this.pending = NO_WORK;
}
DeviceTaskDataRecord.prototype = Object.create(RBObject.prototype);

function HandlerTaskDataRecord() {
  RBObject.call(this);
  this.deviceIn = NO_WORK;
  this.workIn = this.deviceIn;
}
HandlerTaskDataRecord.prototype = Object.create(RBObject.prototype);

HandlerTaskDataRecord.prototype.deviceInAdd = function (packet) {
  this.deviceIn = this.append(packet, this.deviceIn);
};

HandlerTaskDataRecord.prototype.workInAdd = function (packet) {
  this.workIn = this.append(packet, this.workIn);
};

function IdleTaskDataRecord() {
  RBObject.call(this);
  this.control = 1;
  this.count   = 10000;
}

function Packet(link, identity, kind) {
  RBObject.call(this);
  this.link     = link;
  this.identity = identity;
  this.kind     = kind;
  this.datum    = 0;
  this.data     = new Array(DATA_SIZE).fill(0);
}
Packet.prototype = Object.create(RBObject.prototype);

function TaskState() {
  RBObject.call(this);

  this.packetPending_ = false;
  this.taskWaiting_   = false;
  this.taskHolding_   = false;
}
TaskState.prototype = Object.create(RBObject.prototype);

TaskState.prototype.isPacketPending = function () { return this.packetPending_; };
TaskState.prototype.isTaskHolding   = function () { return this.taskHolding_;   };
TaskState.prototype.isTaskWaiting   = function () { return this.taskWaiting_;   };

TaskState.prototype.setTaskHolding   = function (b) { this.taskHolding_ = b; };
TaskState.prototype.setTaskWaiting   = function (b) { this.taskWaiting_ = b; };
TaskState.prototype.setPacketPending = function (b) { this.packetPending_ = b; };

TaskState.prototype.packetPending = function () {
  this.packetPending_ = true;
  this.taskWaiting_   = false;
  this.taskHolding_   = false;
};

TaskState.prototype.running = function () {
  this.taskHolding_ = false;
  this.taskWaiting_ = this.taskHolding_;
  this.packetPending_ = this.taskWaiting_ ;
};

TaskState.prototype.waiting = function () {
  this.taskHolding_ = false;
  this.packetPending_ = this.taskHolding_;
  this.taskWaiting_ = true;
};

TaskState.prototype.waitingWithPacket = function () {
  this.taskHolding_ = false;
  this.packetPending_ = true;
  this.taskWaiting_ = this.packetPending_;
};

TaskState.prototype.isRunning = function () {
  return !this.packetPending_ && !this.taskWaiting_ && !this.taskHolding_;
};

TaskState.prototype.isTaskHoldingOrWaiting = function () {
  return this.taskHolding_ || (!this.packetPending_ && this.taskWaiting_);
};

TaskState.prototype.isWaiting = function () {
  return !this.packetPending_ && this.taskWaiting_ && !this.taskHolding_;
};

TaskState.prototype.isWaitingWithPacket = function () {
  return this.packetPending_ && this.taskWaiting_ && !this.taskHolding_;
};

TaskState.createPacketPending = function () {
  var t = new TaskState();
  t.packetPending();
  return t;
};

TaskState.createRunning = function () {
  var t = new TaskState();
  t.running();
  return t;
};

TaskState.createWaiting = function () {
  var t = new TaskState();
  t.waiting();
  return t;
};

TaskState.createWaitingWithPacket = function () {
  var t = new TaskState();
  t.waitingWithPacket();
  return t;
};

function TaskControlBlock(link, identity, priority, initialWorkQueue,
                          initialState, privateData, fn) {
  TaskState.call(this);
  this.link     = link;
  this.identity = identity;
  this.priority = priority;
  this.input    = initialWorkQueue;
  this.setPacketPending(initialState.isPacketPending());
  this.setTaskWaiting(initialState.isTaskWaiting());
  this.setTaskHolding(initialState.isTaskHolding());
  this.handle = privateData;
  this.function = fn;
}
TaskControlBlock.prototype = Object.create(TaskState.prototype);

TaskControlBlock.prototype.addInputAndCheckPriority = function (packet, oldTask) {
  if (NO_WORK == this.input) {
    this.input = packet;
    this.setPacketPending(true);
    if (this.priority > oldTask.priority) { return this; }
  } else {
    if (this.append === null) {
      var i = 0;
    }
    this.input = this.append(packet, this.input);
  }
  return oldTask;
};

TaskControlBlock.prototype.runTask = function () {
  var message;
  if (this.isWaitingWithPacket()) {
    message = this.input;
    this.input = message.link;
    if (NO_WORK == this.input) {
      this.running();
    } else {
      this.packetPending();
    }
  } else {
    message = NO_WORK;
  }
  return this.function(message, this.handle);
};

function WorkerTaskDataRecord() {
  RBObject.call(this);
  this.destination = HANDLER_A;
  this.count       = 0;
}
WorkerTaskDataRecord.prototype = Object.create(RBObject.prototype);

var run = new Run(Richards, "Richards", 100, 100);
run.runBenchmark();
run.printTotal();
