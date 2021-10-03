import re

# [[byte, type], ... ]
def readGC(file_path):
  gc_cnt = 0
  ret = [[]]
  with open(file_path, "r") as f:
    file_end = False
    during_dump = False
    while not file_end:
      line = f.readline()
      if "Total Runtime" in line:
        file_end = True
        continue

      if "call sweep" in line:
        during_dump = True # GC start
        continue

      if during_dump:
        if "bytes:" in line:
          byte, type = map(lambda x: int(x),re.findall(r"\d+", line))
          ret[gc_cnt].append((byte, type))
        else:
          ret.append([])
          gc_cnt += 1
          during_dump = False # GC end
  ret = ret[:-1]
  return ret
