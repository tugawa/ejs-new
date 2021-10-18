import cv2
import numpy as np
from math import floor
from random import randint

from cell_types import CellTypeDict
from ReadFile import readGC

BLACK = (0,0,0)
CELL_LEN = 8
INF = (1<<63) - 1

USED_COLORS = set([])

def num2color(x):
  r = x >> 16
  g = (x >> 8) % (1 << 8)
  b = x % (1 << 8)
  return (r, g, b)

def color2num(c):
  r, g, b = c[0], c[1], c[2]
  return (r << 16) + (g << 8) + b

for v in CellTypeDict.values():
  c = v[1]
  USED_COLORS.add(color2num(c))


def is_hidden_class(type):
  return type <= 15

def getDict(snap_shot):
  total_bytes = 0
  dict = {}
  object_nums = [0 for _ in range(33)]
  object_bytes = [0 for _ in range(33)]

  # original
  min_address = INF
  max_address = -INF

  # alignment
  alignment_height = 0

  for byte, type, address, hidden_class in snap_shot:
    total_bytes += byte
    object_nums[type] += 1
    object_bytes[type] += byte
    if is_hidden_class(type):
      key = (type, hidden_class)
      if key not in dict:
        dict[key] = [0, 0]
      dict[key][0] += 1
      dict[key][1] += byte

    # original
    min_address = min((min_address // 64) * 64, address)
    max_address = max(max_address, (address + byte + 63)// 64 * 64)

    # alignment
    alignment_height += (byte + 63) // 64

  compress_types = 0
  compress_target_bytes = 0
  for key, val in dict.items():
    if val[0] > 1:
      compress_types += 1
      compress_target_bytes += val[1]

  additional_color_candidates = min(max(5, floor(compress_types * 1.5)), (1<<23)-1)
  C = (1<<24)-1
  inc = (C+1)//additional_color_candidates
  Cs = []
  for i in range(inc, C+1, inc):
    if i > C: break
    if i not in USED_COLORS:
      Cs.append(num2color(i))
  additional_colors = len(Cs)
  idx = 0
  hiddenClassCellTypeDict = {}
  for key, val in dict.items():
    if val[0] > 1:
      if (idx >= additional_colors):
        while True:
          x = randint(0, (1<<24)-1)
          if x not in USED_COLORS:
            USED_COLORS.add(x)
            c = num2color(x)
            hiddenClassCellTypeDict[key] = c
            break
      else:
        hiddenClassCellTypeDict[key] = Cs[idx]
        idx += 1

  print("total byte: {0} ({1} KB)".format(total_bytes, floor(total_bytes / 1024 * 100) / 100))
  for key, val in CellTypeDict.items():
    print("object type: {0}, count: {1}, byte: {2}".format(val[0], object_nums[key], object_bytes[key]))
  print("compress types: {0}".format(compress_types))
  print("compress target bytes: {0} ({1} KB, {2}%)".format(compress_target_bytes, floor(compress_target_bytes/1024 * 100)/100, floor(compress_target_bytes/total_bytes*10000)/100))
  for key, val in dict.items():
    if (val[0] > 1):
      print(key, val)
  return dict, total_bytes, compress_types, hiddenClassCellTypeDict, min_address, max_address, alignment_height


def original_analyze(snap_shot, title, hiddenClassCellTypeDict, max_address, min_address):
  width = 8
  height = (max_address - min_address) // 64
  img = np.zeros((height*CELL_LEN, width*CELL_LEN, 3), dtype=np.uint8)
  img_hidden_class = np.zeros((height*CELL_LEN, width*CELL_LEN, 3), dtype=np.uint8)

  for byte, type, address, hidden_class in snap_shot:
    relative_address = address - min_address
    id = relative_address // 8
    blocks = byte // 8 # 8 byte blockの個数
    key = (type, hidden_class)
    for i in range(blocks):
      h, w = (id+i) // width, (id+i) % width
      img[h*CELL_LEN:(h+1)*CELL_LEN, w*CELL_LEN:(w+1)*CELL_LEN] = CellTypeDict[type][1]
      img[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, w*CELL_LEN:(w+1)*CELL_LEN] = BLACK
      # hidden class
      if key in hiddenClassCellTypeDict:
        img_hidden_class[h*CELL_LEN:(h+1)*CELL_LEN, w*CELL_LEN:(w+1)*CELL_LEN] = hiddenClassCellTypeDict[key]
        img_hidden_class[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, w*CELL_LEN:(w+1)*CELL_LEN] = CellTypeDict[type][1]
      else:
        img_hidden_class[h*CELL_LEN:(h+1)*CELL_LEN, w*CELL_LEN:(w+1)*CELL_LEN] = CellTypeDict[type][1]
        img_hidden_class[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, w*CELL_LEN:(w+1)*CELL_LEN] = BLACK

  cache_lines = height
  print("original cache line: {0}".format(cache_lines))
  cv2.imwrite("{0}_hidden.png".format(title), img_hidden_class)
  cv2.imwrite("{0}.png".format(title), img)


def alignment_analyze(snap_shot, title, height, hiddenClassCellTypeDict):
  width = 8

  img = np.zeros((height*CELL_LEN, width*CELL_LEN, 3), dtype=np.uint8)
  img_hidden_class = np.zeros((height*CELL_LEN, width*CELL_LEN, 3), dtype=np.uint8)

  h = 0
  for byte, type, _, hidden_class in snap_shot:
    section_num = byte // 8
    hs = section_num // 8
    re = section_num  % 8
    key = (type, hidden_class)
    for _ in range(hs):
      img[h*CELL_LEN:(h+1)*CELL_LEN, 0:width*CELL_LEN] = CellTypeDict[type][1]
      img[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, 0:width*CELL_LEN] = BLACK
      # hidden class
      if key in hiddenClassCellTypeDict:
        img_hidden_class[h*CELL_LEN:(h+1)*CELL_LEN, 0:width*CELL_LEN] = hiddenClassCellTypeDict[key]
        img_hidden_class[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, 0:width*CELL_LEN] = CellTypeDict[type][1]
      else:
        img_hidden_class[h*CELL_LEN:(h+1)*CELL_LEN, 0:width*CELL_LEN] = CellTypeDict[type][1]
        img_hidden_class[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, 0:width*CELL_LEN] = BLACK
      h+=1

    if re == 0:
      continue

    img[h*CELL_LEN:(h+1)*CELL_LEN, 0:re*CELL_LEN] = CellTypeDict[type][1]
    img[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, 0:re*CELL_LEN] = BLACK
    img[h*CELL_LEN:(h+1)*CELL_LEN, re*CELL_LEN-1:re*CELL_LEN] = BLACK
    # hidden
    if key in hiddenClassCellTypeDict:
      img_hidden_class[h*CELL_LEN:(h+1)*CELL_LEN, 0:re*CELL_LEN] = hiddenClassCellTypeDict[key]
      img_hidden_class[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, 0:re*CELL_LEN] = CellTypeDict[type][1]
      img_hidden_class[h*CELL_LEN:(h+1)*CELL_LEN, re*CELL_LEN-1:re*CELL_LEN] = BLACK
    else:
      img_hidden_class[h*CELL_LEN:(h+1)*CELL_LEN, 0:re*CELL_LEN] = CellTypeDict[type][1]
      img_hidden_class[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, 0:re*CELL_LEN] = BLACK
      img_hidden_class[h*CELL_LEN:(h+1)*CELL_LEN, re*CELL_LEN-1:re*CELL_LEN] = BLACK

    h += 1


  cache_lines = height
  print("alignment cache line: {0}".format(cache_lines))
  cv2.imwrite("{0}_hidden.png".format(title), img_hidden_class)
  cv2.imwrite("{0}.png".format(title), img)



# file = input().rstrip()
file = "Bounce-small"
result = readGC("data/{0}.txt".format(file))
for i, snap_shot in enumerate(result):
  print("===== start {0} =====".format(i+1))

  dict, total_bytes, compress_types, hiddenClassCellTypeDict, min_address, max_address, alignment_height \
  = getDict(snap_shot)

  # original_analyze(snap_shot, "original{0}".format(i+1), hiddenClassCellTypeDict, max_address, min_address)

  alignment_analyze(snap_shot, "alignment{0}".format(i+1), alignment_height,hiddenClassCellTypeDict)

  print("===== end {0} =====".format(i+1))
  print()
