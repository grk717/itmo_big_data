#!/usr/bin/python3
import sys

m = 0 # mean
c = 0 # count
v1 = 0 # variance accurate
v2 = 0 # variance approx

for row in sys.stdin:
    pairs = row.rstrip("\n").split('\t')
    pairs = [float(x) for x in pairs]
    if pairs[0] != 0:
        ck = pairs[0]
        mk = pairs[1]
        vk_approx = pairs[2]
        vk_accurate = pairs[3]
        m_new = (c * m + ck * mk) / (c + ck) # mean
        v_approx = (c * v1 + ck * vk_approx) / (c + ck) + c * ck * (((m - mk) / (c + ck))**2) # formula from presentation
        v_accurate = (c * v2 + ck * vk_accurate) / (c + ck) + c * ck * (((m - mk) / (c + ck))**2) # formula from presentation
        m = m_new
        c += ck
        v1 = v_approx
        v2 = v_accurate


print(v_approx, v_accurate, sep="\t")

