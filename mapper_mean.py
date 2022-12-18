#!/usr/bin/python3
import sys
import csv

row_count = 0
sum_value = 0

# read whole file (if we read line by line we get extra \n characters
# example: idx == 36071960
data = sys.stdin.readlines()
for line in csv.reader(data):
    if (len(line) < 9 or not line[0].isnumeric()):
        continue
    row_count += 1
    sum_value += float(line[9])
print(row_count, sum_value / row_count, sep="\t")