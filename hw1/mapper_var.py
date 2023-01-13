#!/usr/bin/python3
import sys
import csv

row_count = 0
sum_value = 0
sum_squared = 0

# read whole file (if we read line by line we get extra \n characters
data = sys.stdin.readlines()
prices = []

for line in csv.reader(data):
    if (len(line) < 9 or not line[0].isnumeric()):
        continue
    row_count += 1
    sum_value += float(line[9])
    # in real life we store either running sum
    sum_squared += float(line[9])**2
    # or full list of prices to calculate variance
    prices.append(float(line[9]))


mean_value = sum_value / row_count
# option 1: calculate approx variance
approx_dispersion = (sum_squared / row_count) - (mean_value * mean_value)
# option 2: calculate accurate variance with extra loop
accurate_dispersion =  sum((x - mean_value)**2 for x in prices) / row_count

print(row_count, mean_value, approx_dispersion, accurate_dispersion, sep="\t")
