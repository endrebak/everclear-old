import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from pysam import VariantFile

quals = [record.qual for record in VariantFile(everclear.input[0])]
plt.hist(quals)

plt.savefig(everclear.output[0])