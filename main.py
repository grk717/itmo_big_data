import pandas as pd
import numpy as np

# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    df = pd.read_csv("AB_NYC_2019.csv")
    with open('results_pandas', 'w') as f:
        print("Count:", df.price.count(), "Mean:", df.price.mean(), "Variance:", df.price.var(), sep=" ", file=f)

