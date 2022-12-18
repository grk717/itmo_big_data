#!/usr/bin/python3
import sys

m = 0 # mean
c = 0 # count

for row in sys.stdin:
    pairs = row.rstrip("\n").split('\t')
    pairs = [float(x) for x in pairs]
    if pairs[0] != 0:
        m = (c * m + pairs[0] * pairs[1]) / (c + pairs[0])
        c += pairs[0]

print(c, m, sep="\t")
