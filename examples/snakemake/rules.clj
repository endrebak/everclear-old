(defrule bwa-map
  "Map DNA sequences against a reference genome with BWA."
  {:wildcards [:sample :genome]
   :external  [:genome :fastq]
   :input     "quals.svg"
   :output    "bwa-map.bam"
   :threads   8
   :params    {:rg "@RG\tID:{{wildcards.sample}}\tSM:{{wildcards.sample}}"}
   :shell     "bwa mem -R '{{params.rg}}' {{threads}} {{external.genome}} {{external.fastq}} | samtools view -Sb - > {{output.0}}"})

(defrule samtools-sort
  "Sort the bams."
  {:wildcards [:sample :genome]
   :input     "bwa-map.bam"
   :output    ["bam/sorted.bam"]
   :shell     "samtools sort -T {{wildcards.sample}} -O bam {{input.0}} > {{output.0}}"})

(defrule samtools-index
  "Index read alignments for random access."
  {:wildcards [:sample :genome]
   :input     "bam/sorted.bam"
   :output    "bam/sorted.bam.bai"
   :shell     "samtools index {{input.0}}"})

(defrule bcftools-call
  "Aggregate mapped reads from all samples and jointly call genomic variants."
  {:input     {:sorted "bam/sorted.bam" :index "bam/sorted.bam.bai"}
   :output    "all.vcf"
   :wildcards [:genome]
   :external  [:genome]
   :shell     "samtools mpileup -g -f {{external.genome}} {{input.sorted}} | bcftools call -mv - > {{output.0}}"})

(defrule plot-quals
  {:input     "all.vcf"
   :wildcards [:genome]
   :output    {:plot "quals.svg" :data "quals.tsv"}
   :shell     "plot {{input.0}} -o {{output.plot}}"
   :script    "plot-quals.py"})
